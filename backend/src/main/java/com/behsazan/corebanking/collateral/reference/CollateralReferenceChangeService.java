package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceChanges.*;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Column;
import com.behsazan.corebanking.collateral.reference.CollateralReferenceModels.Table;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** No database write without opt-in, an authenticated principal and a distinct checker. */
@Service
public final class CollateralReferenceChangeService {
    private final CollateralReferenceCatalog catalog;
    private final CollateralReferenceProperties config;
    private final ObjectProvider<CollateralReferenceWriterPool> writer;
    private final JsonMapper json;

    public CollateralReferenceChangeService(CollateralReferenceCatalog catalog,
            CollateralReferenceProperties config, ObjectProvider<CollateralReferenceWriterPool> writer,
            JsonMapper json) {
        this.catalog = catalog;
        this.config = config;
        this.writer = writer;
        this.json = json;
    }
    private CollateralReferenceWriterPool writer() {
        if (!config.writesEnabled()) throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "CLT_REFERENCE_WRITES_DISABLED");
        return writer.getObject();
    }
    private static String authenticatedActor(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token) || !authentication.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CLT_REFERENCE_JWT_REQUIRED");
        return CollateralReferenceChangePolicy.safeActor(token.getToken().getSubject());
    }
    private static String canonicalId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_REFERENCE_ID_INVALID");
        return value;
    }
    private String toJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalArgumentException("CLT_REFERENCE_JSON_INVALID"); }
    }
    private Map<String, Object> fromJson(String value) {
        try {
            @SuppressWarnings("unchecked") Map<String, Object> result = json.readValue(value, Map.class);
            if (result == null) throw new IllegalArgumentException("CLT_REFERENCE_JSON_EMPTY");
            return result;
        } catch (Exception e) { throw new IllegalStateException("CLT_REFERENCE_JSON_UNAVAILABLE", e); }
    }
    public Changes list(String status) {
        if (!Set.of("PENDING_APPROVAL", "APPLIED", "REJECTED").contains(status))
            throw new IllegalArgumentException("CLT_REFERENCE_CHANGE_STATUS_INVALID");
        return new Changes(writer().jdbc().query("""
                SELECT CHANGE_ID, TABLE_NAME, OPERATION, TARGET_ID, EXPECTED_VERSION,
                       PROPOSED_JSON, REASON, STATUS, MAKER_SUBJECT, CHECKER_SUBJECT,
                       CREATED_AT, DECIDED_AT, CORRELATION_ID
                  FROM CLT.CLT_REFERENCE_CHANGE_REQUEST WHERE STATUS = ?
                 ORDER BY CHANGE_ID DESC FETCH FIRST 40 ROWS ONLY
                """, (rs, idx) -> row(rs), status), "ORACLE_CLT_GOVERNANCE");
    }
    public Change propose(Proposal proposal, Authentication authentication,
                          String idempotencyKey, String correlation) {
        String maker = authenticatedActor(authentication);
        String key = CollateralReferenceChangePolicy.idempotencyKey(idempotencyKey);
        String trace = CollateralReferenceChangePolicy.correlationId(correlation);
        if (proposal == null) throw new IllegalArgumentException("CLT_REFERENCE_PROPOSAL_MISSING");
        CollateralReferenceChangePolicy.requireTarget(proposal.operation(), proposal.targetId(), proposal.expectedVersion());
        String reason = CollateralReferenceChangePolicy.reason(proposal.reason());
        Table table = catalog.require(proposal.table());
        Map<String, String> types = new LinkedHashMap<>();
        Set<String> required = new java.util.HashSet<>();
        for (Column column : table.columns()) {
            types.put(column.name(), column.dataType());
            if (column.required()) required.add(column.name());
        }
        Map<String, Object> values = CollateralReferenceChangePolicy.normalize(table.table(),
                proposal.operation(), table.primaryKey(), types, required, proposal.values());
        // Sort the proposal fields for a replay-stable body fingerprint, independent of caller map order.
        Map<String, Object> stable = new java.util.TreeMap<>(values);
        String proposedJson = toJson(stable);
        String fingerprint = sha256(toJson(List.of(table.table(), proposal.operation(),
                proposal.targetId() == null ? "" : proposal.targetId(),
                proposal.expectedVersion() == null ? "" : proposal.expectedVersion().toString(), proposedJson, reason)));
        CollateralReferenceWriterPool pool = writer();
        return pool.transaction().execute(tx -> {
            List<Change> previous = pool.jdbc().query("""
                    SELECT CHANGE_ID, TABLE_NAME, OPERATION, TARGET_ID, EXPECTED_VERSION,
                           PROPOSED_JSON, REASON, STATUS, MAKER_SUBJECT, CHECKER_SUBJECT,
                           CREATED_AT, DECIDED_AT, CORRELATION_ID
                      FROM CLT.CLT_REFERENCE_CHANGE_REQUEST
                     WHERE MAKER_SUBJECT = ? AND IDEMPOTENCY_KEY = ?
                    """, (rs, index) -> row(rs), maker, key);
            if (!previous.isEmpty()) {
                String priorFingerprint = pool.jdbc().queryForObject(
                        "SELECT REQUEST_FINGERPRINT FROM CLT.CLT_REFERENCE_CHANGE_REQUEST WHERE CHANGE_ID = ?",
                        String.class, new BigDecimal(previous.getFirst().changeId()));
                if (!fingerprint.equals(priorFingerprint)) throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "CLT_REFERENCE_IDEMPOTENCY_REUSE_MISMATCH");
                return previous.getFirst();
            }
            BigDecimal id = pool.jdbc().queryForObject(
                    "SELECT CLT.SEQ_CLT_REFERENCE_CHANGE_REQUEST.NEXTVAL FROM DUAL", BigDecimal.class);
            pool.jdbc().update("""
                    INSERT INTO CLT.CLT_REFERENCE_CHANGE_REQUEST
                     (CHANGE_ID, TABLE_NAME, OPERATION, TARGET_ID, EXPECTED_VERSION,
                      PROPOSED_JSON, REASON, STATUS, MAKER_SUBJECT, IDEMPOTENCY_KEY,
                      REQUEST_FINGERPRINT, CORRELATION_ID)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING_APPROVAL', ?, ?, ?, ?)
                    """, id, table.table(), proposal.operation(),
                    proposal.targetId() == null ? null : new BigDecimal(proposal.targetId()),
                    proposal.expectedVersion(), proposedJson, reason, maker, key, fingerprint, trace);
            audit(pool, id, "SUBMITTED", maker, null, proposedJson, reason, trace);
            return getById(pool, id, false);
        });
    }
    public Change decide(String changeId, Decision decision,
                         Authentication authentication, String correlation) {
        String checker = authenticatedActor(authentication);
        String trace = CollateralReferenceChangePolicy.correlationId(correlation);
        if (decision == null || !Set.of("APPROVE", "REJECT").contains(decision.decision()))
            throw new IllegalArgumentException("CLT_REFERENCE_DECISION_INVALID");
        String reason = CollateralReferenceChangePolicy.reason(decision.reason());
        BigDecimal id = new BigDecimal(canonicalId(changeId));
        CollateralReferenceWriterPool pool = writer();
        return pool.transaction().execute(tx -> {
            Change request = getById(pool, id, true);
            if (!"PENDING_APPROVAL".equals(request.status())) throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "CLT_REFERENCE_DECISION_ALREADY_RECORDED");
            try {
                CollateralReferenceChangePolicy.requireDistinctActors(request.maker(), checker);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_REFERENCE_SELF_APPROVAL_DENIED");
            }
            if ("REJECT".equals(decision.decision())) {
                pool.jdbc().update("""
                      UPDATE CLT.CLT_REFERENCE_CHANGE_REQUEST
                         SET STATUS = 'REJECTED', CHECKER_SUBJECT = ?, DECIDED_AT = SYSTIMESTAMP,
                             DECISION_REASON = ?, RECORD_VERSION = RECORD_VERSION + 1
                       WHERE CHANGE_ID = ? AND STATUS = 'PENDING_APPROVAL'
                      """, checker, reason, id);
                audit(pool, id, "REJECTED", checker, null, null, reason, trace);
            } else {
                Table table = catalog.require(request.table());
                Map<String, String> types = columnTypes(table);
                Set<String> required = new java.util.HashSet<>();
                for (Column col : table.columns()) if (col.required()) required.add(col.name());
                Map<String, Object> safeValues = CollateralReferenceChangePolicy.normalize(table.table(),
                        request.operation(), table.primaryKey(), types, required, request.values());
                Map<String, Object> before = null;
                Map<String, Object> after;
                BigDecimal target;
                if ("CREATE".equals(request.operation())) {
                    target = createReference(pool, table, safeValues, request.maker());
                    after = snapshot(pool, table, target, false);
                } else {
                    target = new BigDecimal(canonicalId(request.targetId()));
                    before = snapshot(pool, table, target, true);
                    if (before == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "CLT_REFERENCE_TARGET_NOT_FOUND");
                    Object version = before.get("RECORD_VERSION");
                    if (version == null || new BigDecimal(version.toString()).longValueExact()
                            != request.expectedVersion()) throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "CLT_REFERENCE_VERSION_CONFLICT");
                    updateReference(pool, table, target, safeValues, checker, request.expectedVersion());
                    after = snapshot(pool, table, target, false);
                }
                pool.jdbc().update("""
                      UPDATE CLT.CLT_REFERENCE_CHANGE_REQUEST
                         SET STATUS = 'APPLIED', TARGET_ID = ?, CHECKER_SUBJECT = ?,
                             DECIDED_AT = SYSTIMESTAMP, DECISION_REASON = ?,
                             RECORD_VERSION = RECORD_VERSION + 1
                       WHERE CHANGE_ID = ? AND STATUS = 'PENDING_APPROVAL'
                      """, target, checker, reason, id);
                audit(pool, id, "APPLIED", checker, before == null ? null : toJson(before), toJson(after), reason, trace);
            }
            return getById(pool, id, false);
        });
    }
    private BigDecimal createReference(CollateralReferenceWriterPool pool, Table table,
                                        Map<String, Object> values, String maker) {
        // Physical CLT baseline uses existing SEQ_<table> rather than XMI IDENTITY.
        BigDecimal id = pool.jdbc().queryForObject(
                "SELECT CLT.SEQ_" + table.table() + ".NEXTVAL FROM DUAL", BigDecimal.class);
        if (id == null || id.precision() > 19) throw new IllegalStateException("CLT_REFERENCE_SEQUENCE_EXHAUSTED");
        StringBuilder names = new StringBuilder(table.primaryKey());
        StringBuilder params = new StringBuilder("?");
        List<Object> args = new java.util.ArrayList<>();
        args.add(id);
        Map<String, String> types = columnTypes(table);
        for (var field : values.entrySet()) {
            names.append(", ").append(field.getKey());
            params.append(", ?");
            args.add(CollateralReferenceChangePolicy.jdbcValue(types.get(field.getKey()), field.getValue()));
        }
        names.append(", CREATED_BY");
        params.append(", ?");
        args.add(maker);
        pool.jdbc().update("INSERT INTO CLT." + table.table() + " (" + names + ") VALUES (" + params + ")",
                args.toArray());
        return id;
    }
    private void updateReference(CollateralReferenceWriterPool pool, Table table, BigDecimal id,
                                  Map<String, Object> values, String checker, Long expected) {
        Map<String, String> types = columnTypes(table);
        StringBuilder sql = new StringBuilder("UPDATE CLT.").append(table.table()).append(" SET ");
        List<Object> args = new java.util.ArrayList<>();
        for (var field : values.entrySet()) {
            if (!args.isEmpty()) sql.append(", ");
            sql.append(field.getKey()).append(" = ?");
            args.add(CollateralReferenceChangePolicy.jdbcValue(types.get(field.getKey()), field.getValue()));
        }
        sql.append(", UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = ?, RECORD_VERSION = RECORD_VERSION + 1")
                .append(" WHERE ").append(table.primaryKey()).append(" = ? AND RECORD_VERSION = ?");
        args.add(checker);
        args.add(id);
        args.add(expected);
        if (pool.jdbc().update(sql.toString(), args.toArray()) != 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_REFERENCE_VERSION_CONFLICT");
    }
    private Map<String, Object> snapshot(CollateralReferenceWriterPool pool, Table table,
                                          BigDecimal id, boolean lock) {
        List<Map<String, Object>> rows = pool.jdbc().queryForList(
                "SELECT " + CollateralReferenceRepository.projection(table) + " FROM CLT." + table.table()
                + " WHERE " + table.primaryKey() + " = ?" + (lock ? " FOR UPDATE" : ""), id);
        return rows.isEmpty() ? null : CollateralReferenceRepository.normalized(rows.getFirst());
    }
    private static Map<String, String> columnTypes(Table table) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Column col : table.columns()) result.put(col.name(), col.dataType());
        return result;
    }
    private void audit(CollateralReferenceWriterPool pool, BigDecimal id, String event,
                       String actor, String before, String after, String reason, String correlation) {
        pool.jdbc().update("""
                INSERT INTO CLT.CLT_REFERENCE_CHANGE_AUDIT
                  (CHANGE_ID, EVENT_TYPE, ACTOR_SUBJECT, BEFORE_JSON, AFTER_JSON, REASON, CORRELATION_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, event, actor, before, after, reason, correlation);
    }
    private Change getById(CollateralReferenceWriterPool pool, BigDecimal id, boolean lock) {
        List<Change> found = pool.jdbc().query("""
                SELECT CHANGE_ID, TABLE_NAME, OPERATION, TARGET_ID, EXPECTED_VERSION,
                       PROPOSED_JSON, REASON, STATUS, MAKER_SUBJECT, CHECKER_SUBJECT,
                       CREATED_AT, DECIDED_AT, CORRELATION_ID
                  FROM CLT.CLT_REFERENCE_CHANGE_REQUEST WHERE CHANGE_ID = ?
                """ + (lock ? " FOR UPDATE" : ""), (rs, index) -> row(rs), id);
        if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CLT_REFERENCE_CHANGE_NOT_FOUND");
        return found.getFirst();
    }
    private Change row(java.sql.ResultSet rs) throws java.sql.SQLException {
        BigDecimal version = rs.getBigDecimal("EXPECTED_VERSION");
        BigDecimal target = rs.getBigDecimal("TARGET_ID");
        java.sql.Timestamp decided = rs.getTimestamp("DECIDED_AT");
        return new Change(rs.getBigDecimal("CHANGE_ID").toPlainString(), rs.getString("TABLE_NAME"),
                rs.getString("OPERATION"), target == null ? null : target.toPlainString(),
                version == null ? null : version.longValueExact(), fromJson(rs.getString("PROPOSED_JSON")),
                rs.getString("REASON"), rs.getString("STATUS"), rs.getString("MAKER_SUBJECT"),
                rs.getString("CHECKER_SUBJECT"), rs.getTimestamp("CREATED_AT").toLocalDateTime().toString(),
                decided == null ? null : decided.toLocalDateTime().toString(), rs.getString("CORRELATION_ID"));
    }
    private static String sha256(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
