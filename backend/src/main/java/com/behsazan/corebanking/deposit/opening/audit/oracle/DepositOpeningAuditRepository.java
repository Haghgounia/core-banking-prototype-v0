package com.behsazan.corebanking.deposit.opening.audit.oracle;

import com.behsazan.corebanking.deposit.opening.audit.domain.DepositOpeningAuditModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class DepositOpeningAuditRepository {
    private static final List<String> MUTABLE_REQUEST_FIELDS = List.of(
            "PRODUCT_VERSION_ID",
            "REQUEST_TYPE_CODE",
            "OWNERSHIP_TYPE_CODE",
            "CURRENCY_CODE",
            "OPENING_CHANNEL_CODE",
            "ORG_UNIT_CODE",
            "REQUESTED_OPENING_DATE",
            "OPENING_AMOUNT",
            "SOURCE_OF_FUNDS_CODE",
            "PURPOSE_CODE"
    );

    private static final Map<String, EntitySpec> CHANGEABLE_ENTITIES = Map.ofEntries(
            Map.entry("DEPOSIT_OPENING_REQUEST", new EntitySpec("OPENING_REQUEST_ID", OwnershipMode.ROOT)),
            Map.entry("DEPOSIT_OPENING_PARTY", new EntitySpec("OPENING_PARTY_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_SIGNATURE_RULE", new EntitySpec("OPENING_SIGNATURE_RULE_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_SIGNATORY", new EntitySpec("OPENING_SIGNATORY_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_SIGNATORY_AUTHORITY", new EntitySpec("OPENING_SIGNATORY_AUTHORITY_ID", OwnershipMode.SIGNATORY)),
            Map.entry("DEPOSIT_OPENING_AUTHORIZED_USER", new EntitySpec("OPENING_AUTHORIZED_USER_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_DELEGATION", new EntitySpec("OPENING_DELEGATION_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_BENEFICIARY", new EntitySpec("OPENING_BENEFICIARY_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_TERM", new EntitySpec("OPENING_TERM_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_MATURITY_INSTRUCTION", new EntitySpec("OPENING_MATURITY_INSTRUCTION_ID", OwnershipMode.TERM)),
            Map.entry("DEPOSIT_OPENING_PROFIT_INSTRUCTION", new EntitySpec("OPENING_PROFIT_INSTRUCTION_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_WITHDRAWAL_MEDIA", new EntitySpec("OPENING_WITHDRAWAL_MEDIA_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_SERVICE_SELECTION", new EntitySpec("OPENING_SERVICE_SELECTION_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_PAYMENT_INSTRUMENT", new EntitySpec("OPENING_PAYMENT_INSTRUMENT_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST", new EntitySpec("OPENING_PRICING_REQUEST_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_TAX_STATUS", new EntitySpec("OPENING_TAX_STATUS_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_REWARD_ENROLLMENT", new EntitySpec("OPENING_REWARD_ENROLLMENT_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_FUNDING", new EntitySpec("OPENING_FUNDING_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_CHECK", new EntitySpec("OPENING_CHECK_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_DOCUMENT", new EntitySpec("OPENING_DOCUMENT_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_TERMS_ACCEPTANCE", new EntitySpec("OPENING_TERMS_ACCEPTANCE_ID", OwnershipMode.DIRECT)),
            Map.entry("DEPOSIT_OPENING_DECISION", new EntitySpec("OPENING_DECISION_ID", OwnershipMode.DIRECT))
    );

    private static final Set<String> SYSTEM_COLUMNS = Set.of(
            "CREATED_AT", "CREATED_BY", "UPDATED_AT", "UPDATED_BY", "RECORD_VERSION"
    );

    private static final List<String> SNAPSHOT_DIRECT_TABLES = List.of(
            "DEPOSIT_OPENING_PARTY",
            "DEPOSIT_OPENING_SIGNATURE_RULE",
            "DEPOSIT_OPENING_SIGNATORY",
            "DEPOSIT_OPENING_AUTHORIZED_USER",
            "DEPOSIT_OPENING_DELEGATION",
            "DEPOSIT_OPENING_BENEFICIARY",
            "DEPOSIT_OPENING_TERM",
            "DEPOSIT_OPENING_PROFIT_INSTRUCTION",
            "DEPOSIT_OPENING_WITHDRAWAL_MEDIA",
            "DEPOSIT_OPENING_SERVICE_SELECTION",
            "DEPOSIT_OPENING_PAYMENT_INSTRUMENT",
            "DEPOSIT_OPENING_PRICING_OVERRIDE_REQUEST",
            "DEPOSIT_OPENING_TAX_STATUS",
            "DEPOSIT_OPENING_REWARD_ENROLLMENT",
            "DEPOSIT_OPENING_FUNDING",
            "DEPOSIT_OPENING_CHECK",
            "DEPOSIT_OPENING_DOCUMENT",
            "DEPOSIT_OPENING_TERMS_ACCEPTANCE",
            "DEPOSIT_OPENING_DECISION"
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;

    public DepositOpeningAuditRepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-opening:DPS2}") String schema
    ) {
        this.jdbc = jdbc;
        this.schema = requireIdentifier(schema);
    }

    public Optional<OpeningAuditHeader> findOpening(long openingRequestId) {
        return queryOpening(openingRequestId, false);
    }

    public Optional<OpeningAuditHeader> lockOpening(long openingRequestId) {
        return queryOpening(openingRequestId, true);
    }

    private Optional<OpeningAuditHeader> queryOpening(long openingRequestId, boolean lock) {
        String sql = """
                SELECT OPENING_REQUEST_ID, REQUEST_NO, REQUEST_STATUS_CODE,
                       NVL(RECORD_VERSION, 1) RECORD_VERSION, CREATED_ACCOUNT_ID
                  FROM %s.DEPOSIT_OPENING_REQUEST
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                """.formatted(schema) + (lock ? " FOR UPDATE" : "");
        List<OpeningAuditHeader> rows = jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> new OpeningAuditHeader(
                        rs.getLong("OPENING_REQUEST_ID"),
                        rs.getString("REQUEST_NO"),
                        rs.getString("REQUEST_STATUS_CODE"),
                        rs.getLong("RECORD_VERSION"),
                        rs.getObject("CREATED_ACCOUNT_ID", Long.class)
                ));
        return rows.stream().findFirst();
    }

    public Optional<MutableFieldState> lockMutableFieldState(long openingRequestId) {
        String fields = String.join(", ", MUTABLE_REQUEST_FIELDS);
        String sql = "SELECT OPENING_REQUEST_ID, REQUEST_NO, REQUEST_STATUS_CODE, " +
                "NVL(RECORD_VERSION,1) RECORD_VERSION, CREATED_ACCOUNT_ID, " + fields +
                " FROM " + schema + ".DEPOSIT_OPENING_REQUEST " +
                "WHERE OPENING_REQUEST_ID = :openingRequestId FOR UPDATE";
        List<MutableFieldState> rows = jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> {
                    OpeningAuditHeader header = new OpeningAuditHeader(
                            rs.getLong("OPENING_REQUEST_ID"), rs.getString("REQUEST_NO"),
                            rs.getString("REQUEST_STATUS_CODE"), rs.getLong("RECORD_VERSION"),
                            rs.getObject("CREATED_ACCOUNT_ID", Long.class));
                    Map<String, Object> values = new LinkedHashMap<>();
                    for (String field : MUTABLE_REQUEST_FIELDS) {
                        Object value = rs.getObject(field);
                        if (value instanceof Date date) value = date.toLocalDate();
                        values.put(field, value);
                    }
                    return new MutableFieldState(header, java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values)));
                });
        return rows.stream().findFirst();
    }

    public Optional<EntityFieldMetadata> mutableFieldMetadata(String entityName, String fieldName) {
        String entity = requireIdentifier(entityName);
        String field = requireIdentifier(fieldName);
        EntitySpec spec = CHANGEABLE_ENTITIES.get(entity);
        if (spec == null || !isMutableBusinessField(entity, field, spec)) return Optional.empty();
        String sql = "SELECT DATA_TYPE, CHAR_LENGTH, DATA_PRECISION, DATA_SCALE, NULLABLE " +
                "FROM ALL_TAB_COLUMNS WHERE OWNER = :owner AND TABLE_NAME = :tableName AND COLUMN_NAME = :columnName";
        List<EntityFieldMetadata> rows = jdbc.query(sql, new MapSqlParameterSource()
                        .addValue("owner", schema, Types.VARCHAR)
                        .addValue("tableName", entity, Types.VARCHAR)
                        .addValue("columnName", field, Types.VARCHAR),
                (rs, rowNum) -> new EntityFieldMetadata(
                        entity, field, rs.getString("DATA_TYPE"),
                        rs.getObject("CHAR_LENGTH", Integer.class),
                        rs.getObject("DATA_PRECISION", Integer.class),
                        rs.getObject("DATA_SCALE", Integer.class),
                        "Y".equalsIgnoreCase(rs.getString("NULLABLE"))
                ));
        return rows.stream().findFirst().filter(meta -> supportedMutationType(meta.dataType()));
    }

    public Optional<EntityRowState> lockEntityFields(
            long openingRequestId, String entityName, long entityId, List<String> fields
    ) {
        String entity = requireIdentifier(entityName);
        EntitySpec spec = CHANGEABLE_ENTITIES.get(entity);
        if (spec == null) throw new IllegalArgumentException("Unsupported Opening aggregate entity: " + entity);
        if (fields == null || fields.isEmpty()) throw new IllegalArgumentException("At least one field is required.");
        List<String> safeFields = fields.stream().map(DepositOpeningAuditRepository::requireIdentifier).distinct().toList();
        for (String field : safeFields) {
            if (!isMutableBusinessField(entity, field, spec)) {
                throw new IllegalArgumentException("Protected or unsupported mutable field: " + entity + "." + field);
            }
        }
        String sql = "SELECT " + spec.keyColumn() + ", NVL(RECORD_VERSION,1) RECORD_VERSION, " +
                String.join(", ", safeFields) + " FROM " + schema + "." + entity + " t WHERE " +
                ownershipPredicate(spec) + " FOR UPDATE";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("entityId", entityId, Types.NUMERIC);
        List<EntityRowState> rows = jdbc.query(sql, params, (rs, rowNum) -> {
            LinkedHashMap<String, Object> values = new LinkedHashMap<>();
            for (String field : safeFields) {
                Object value = rs.getObject(field);
                if (value instanceof Date date) value = date.toLocalDate();
                values.put(field, value);
            }
            return new EntityRowState(entity, entityId, rs.getLong("RECORD_VERSION"),
                    java.util.Collections.unmodifiableMap(values));
        });
        return rows.stream().findFirst();
    }

    public int updateEntityFields(
            String entityName, long entityId, long expectedVersion, Map<String, Object> values, String actor
    ) {
        String entity = requireIdentifier(entityName);
        EntitySpec spec = CHANGEABLE_ENTITIES.get(entity);
        if (spec == null) throw new IllegalArgumentException("Unsupported Opening aggregate entity: " + entity);
        if (values.isEmpty()) return 0;
        List<String> assignments = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("entityId", entityId, Types.NUMERIC)
                .addValue("expectedVersion", expectedVersion, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR);
        int index = 0;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String field = requireIdentifier(entry.getKey());
            if (!isMutableBusinessField(entity, field, spec)) {
                throw new IllegalArgumentException("Protected or unsupported mutable field: " + entity + "." + field);
            }
            String parameter = "v" + index++;
            assignments.add(field + " = :" + parameter);
            addTypedValue(params, parameter, entry.getValue());
        }
        String sql = "UPDATE " + schema + "." + entity + " SET " + String.join(", ", assignments) +
                ", UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = :actor, RECORD_VERSION = NVL(RECORD_VERSION,0) + 1 " +
                "WHERE " + spec.keyColumn() + " = :entityId AND NVL(RECORD_VERSION,1) = :expectedVersion";
        return jdbc.update(sql, params);
    }

    public int bumpOpeningVersion(long openingRequestId, long expectedVersion, String actor) {
        String sql = "UPDATE " + schema + ".DEPOSIT_OPENING_REQUEST SET UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, " +
                "RECORD_VERSION=NVL(RECORD_VERSION,0)+1 WHERE OPENING_REQUEST_ID=:openingRequestId " +
                "AND NVL(RECORD_VERSION,1)=:expectedVersion";
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("expectedVersion", expectedVersion, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public boolean changeableEntity(String entityName) {
        if (entityName == null) return false;
        try {
            return CHANGEABLE_ENTITIES.containsKey(requireIdentifier(entityName));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public long nextChangeSetId() {
        return nextValue("SEQ_DEPOSIT_OPENING_CHANGE_SET");
    }

    public long nextAuditEventId() {
        return nextValue("SEQ_DEPOSIT_OPENING_AUDIT_EVENT");
    }

    public long nextFieldChangeId() {
        return nextValue("SEQ_DEPOSIT_OPENING_AUDIT_FIELD_CHANGE");
    }

    public long nextSnapshotId() {
        return nextValue("SEQ_DEPOSIT_OPENING_SNAPSHOT");
    }

    public long nextChangeNo(long openingRequestId) {
        String sql = "SELECT NVL(MAX(CHANGE_NO), 0) + 1 FROM " + schema +
                ".DEPOSIT_OPENING_CHANGE_SET WHERE OPENING_REQUEST_ID = :openingRequestId";
        Long value = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC), Long.class);
        return value == null ? 1L : value;
    }

    public int insertChangeSet(
            long changeSetId,
            long openingRequestId,
            long changeNo,
            String changeTypeCode,
            String changeReasonCode,
            String changeReasonNote,
            long baseRequestVersion,
            String actor,
            String correlationId
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_CHANGE_SET (
                    CHANGE_SET_ID, OPENING_REQUEST_ID, CHANGE_NO,
                    CHANGE_TYPE_CODE, CHANGE_STATUS_CODE,
                    CHANGE_REASON_CODE, CHANGE_REASON_NOTE,
                    REQUESTED_BY, REQUESTED_AT, BASE_REQUEST_VERSION,
                    CORRELATION_ID, CREATED_BY, RECORD_VERSION
                ) VALUES (
                    :changeSetId, :openingRequestId, :changeNo,
                    :changeTypeCode, 'DRAFT',
                    :changeReasonCode, :changeReasonNote,
                    :actor, SYSTIMESTAMP, :baseRequestVersion,
                    :correlationId, :actor, 1
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("changeSetId", changeSetId, Types.NUMERIC)
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("changeNo", changeNo, Types.NUMERIC)
                .addValue("changeTypeCode", changeTypeCode, Types.VARCHAR)
                .addValue("changeReasonCode", changeReasonCode, Types.VARCHAR)
                .addValue("changeReasonNote", changeReasonNote, Types.VARCHAR)
                .addValue("baseRequestVersion", baseRequestVersion, Types.NUMERIC)
                .addValue("correlationId", correlationId, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public Optional<ChangeSetView> findChangeSet(long openingRequestId, long changeSetId, boolean lock) {
        String sql = """
                SELECT CHANGE_SET_ID, OPENING_REQUEST_ID, CHANGE_NO,
                       CHANGE_TYPE_CODE, CHANGE_STATUS_CODE, CHANGE_REASON_CODE,
                       CHANGE_REASON_NOTE, REQUESTED_BY, REQUESTED_AT,
                       APPROVED_BY, APPROVED_AT, BASE_REQUEST_VERSION,
                       RESULT_REQUEST_VERSION, CORRELATION_ID
                  FROM %s.DEPOSIT_OPENING_CHANGE_SET
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   AND CHANGE_SET_ID = :changeSetId
                """.formatted(schema) + (lock ? " FOR UPDATE" : "");
        List<ChangeSetView> rows = jdbc.query(sql, new MapSqlParameterSource()
                        .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                        .addValue("changeSetId", changeSetId, Types.NUMERIC),
                (rs, rowNum) -> mapChangeSet(rs));
        return rows.stream().findFirst();
    }

    public List<ChangeSetView> listChangeSets(long openingRequestId) {
        String sql = """
                SELECT CHANGE_SET_ID, OPENING_REQUEST_ID, CHANGE_NO,
                       CHANGE_TYPE_CODE, CHANGE_STATUS_CODE, CHANGE_REASON_CODE,
                       CHANGE_REASON_NOTE, REQUESTED_BY, REQUESTED_AT,
                       APPROVED_BY, APPROVED_AT, BASE_REQUEST_VERSION,
                       RESULT_REQUEST_VERSION, CORRELATION_ID
                  FROM %s.DEPOSIT_OPENING_CHANGE_SET
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                 ORDER BY CHANGE_NO DESC, CHANGE_SET_ID DESC
                """.formatted(schema);
        return jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> mapChangeSet(rs));
    }

    public int approveChangeSet(long openingRequestId, long changeSetId, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_CHANGE_SET
                   SET CHANGE_STATUS_CODE = 'APPROVED',
                       APPROVED_BY = :actor,
                       APPROVED_AT = SYSTIMESTAMP,
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   AND CHANGE_SET_ID = :changeSetId
                   AND CHANGE_STATUS_CODE = 'DRAFT'
                """.formatted(schema);
        return jdbc.update(sql, params(openingRequestId, changeSetId, actor));
    }

    public int rejectChangeSet(long openingRequestId, long changeSetId, String actor, String note) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_CHANGE_SET
                   SET CHANGE_STATUS_CODE = 'REJECTED',
                       CHANGE_REASON_NOTE = CASE
                           WHEN :note IS NULL THEN CHANGE_REASON_NOTE
                           WHEN CHANGE_REASON_NOTE IS NULL THEN :note
                           ELSE CHANGE_REASON_NOTE || CHR(10) || :note
                       END,
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   AND CHANGE_SET_ID = :changeSetId
                   AND CHANGE_STATUS_CODE IN ('DRAFT', 'APPROVED')
                """.formatted(schema);
        return jdbc.update(sql, params(openingRequestId, changeSetId, actor).addValue("note", note, Types.VARCHAR));
    }

    public int markChangeSetApplied(
            long openingRequestId,
            long changeSetId,
            long resultRequestVersion,
            String actor
    ) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_CHANGE_SET
                   SET CHANGE_STATUS_CODE = 'APPLIED',
                       RESULT_REQUEST_VERSION = :resultRequestVersion,
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   AND CHANGE_SET_ID = :changeSetId
                   AND CHANGE_STATUS_CODE = 'APPROVED'
                """.formatted(schema);
        return jdbc.update(sql, params(openingRequestId, changeSetId, actor)
                .addValue("resultRequestVersion", resultRequestVersion, Types.NUMERIC));
    }

    public int updateRequestFields(
            long openingRequestId,
            long expectedVersion,
            Map<String, Object> values,
            String actor
    ) {
        if (values.isEmpty()) return 0;
        List<String> assignments = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("expectedVersion", expectedVersion, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR);
        int index = 0;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String field = requireIdentifier(entry.getKey());
            if (!MUTABLE_REQUEST_FIELDS.contains(field)) {
                throw new IllegalArgumentException("Unsupported mutable opening field: " + field);
            }
            String parameter = "v" + index++;
            assignments.add(field + " = :" + parameter);
            Object value = entry.getValue();
            if (value instanceof java.time.LocalDate date) {
                params.addValue(parameter, Date.valueOf(date), Types.DATE);
            } else if (value instanceof BigDecimal) {
                params.addValue(parameter, value, Types.NUMERIC);
            } else if (value instanceof Number) {
                params.addValue(parameter, value, Types.NUMERIC);
            } else {
                params.addValue(parameter, value, Types.VARCHAR);
            }
        }
        String sql = "UPDATE " + schema + ".DEPOSIT_OPENING_REQUEST SET " +
                String.join(", ", assignments) +
                ", UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = :actor, " +
                "RECORD_VERSION = NVL(RECORD_VERSION, 0) + 1 " +
                "WHERE OPENING_REQUEST_ID = :openingRequestId " +
                "AND NVL(RECORD_VERSION, 1) = :expectedVersion";
        return jdbc.update(sql, params);
    }

    public int insertAuditEvent(
            long auditEventId,
            long openingRequestId,
            Long changeSetId,
            String entityName,
            String entityKey,
            String eventTypeCode,
            String actorTypeCode,
            String actorId,
            String actorRoleCode,
            String channelCode,
            String orgUnitCode,
            String sourceSystemCode,
            String operationName,
            String reasonCode,
            String reasonNote,
            String approvalReference,
            String correlationId,
            Long requestVersionBefore,
            Long requestVersionAfter,
            String actor
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_AUDIT_EVENT (
                    AUDIT_EVENT_ID, OPENING_REQUEST_ID, CHANGE_SET_ID,
                    ENTITY_NAME, ENTITY_KEY, EVENT_TYPE_CODE,
                    ACTOR_TYPE_CODE, ACTOR_ID, ACTOR_ROLE_CODE,
                    CHANNEL_CODE, ORG_UNIT_CODE, SOURCE_SYSTEM_CODE,
                    OPERATION_NAME, EVENT_AT, REASON_CODE, REASON_NOTE,
                    APPROVAL_REFERENCE, CORRELATION_ID,
                    REQUEST_VERSION_BEFORE, REQUEST_VERSION_AFTER,
                    CREATED_BY
                ) VALUES (
                    :auditEventId, :openingRequestId, :changeSetId,
                    :entityName, :entityKey, :eventTypeCode,
                    :actorTypeCode, :actorId, :actorRoleCode,
                    :channelCode, :orgUnitCode, :sourceSystemCode,
                    :operationName, SYSTIMESTAMP, :reasonCode, :reasonNote,
                    :approvalReference, :correlationId,
                    :requestVersionBefore, :requestVersionAfter,
                    :actor
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("auditEventId", auditEventId, Types.NUMERIC)
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("changeSetId", changeSetId, Types.NUMERIC)
                .addValue("entityName", entityName, Types.VARCHAR)
                .addValue("entityKey", entityKey, Types.VARCHAR)
                .addValue("eventTypeCode", eventTypeCode, Types.VARCHAR)
                .addValue("actorTypeCode", actorTypeCode, Types.VARCHAR)
                .addValue("actorId", actorId, Types.VARCHAR)
                .addValue("actorRoleCode", actorRoleCode, Types.VARCHAR)
                .addValue("channelCode", channelCode, Types.VARCHAR)
                .addValue("orgUnitCode", orgUnitCode, Types.VARCHAR)
                .addValue("sourceSystemCode", sourceSystemCode, Types.VARCHAR)
                .addValue("operationName", operationName, Types.VARCHAR)
                .addValue("reasonCode", reasonCode, Types.VARCHAR)
                .addValue("reasonNote", reasonNote, Types.VARCHAR)
                .addValue("approvalReference", approvalReference, Types.VARCHAR)
                .addValue("correlationId", correlationId, Types.VARCHAR)
                .addValue("requestVersionBefore", requestVersionBefore, Types.NUMERIC)
                .addValue("requestVersionAfter", requestVersionAfter, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int insertFieldChange(
            long fieldChangeId,
            long auditEventId,
            String fieldName,
            String fieldChangeTypeCode,
            String oldValue,
            String newValue,
            String oldValueHash,
            String newValueHash,
            String dataTypeCode,
            boolean sensitive,
            boolean valueMasked
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_AUDIT_FIELD_CHANGE (
                    FIELD_CHANGE_ID, AUDIT_EVENT_ID, FIELD_NAME,
                    FIELD_CHANGE_TYPE_CODE, OLD_VALUE, NEW_VALUE,
                    OLD_VALUE_HASH, NEW_VALUE_HASH, DATA_TYPE_CODE,
                    IS_SENSITIVE, VALUE_MASKED_FLAG
                ) VALUES (
                    :fieldChangeId, :auditEventId, :fieldName,
                    :fieldChangeTypeCode, :oldValue, :newValue,
                    :oldValueHash, :newValueHash, :dataTypeCode,
                    :isSensitive, :valueMasked
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("fieldChangeId", fieldChangeId, Types.NUMERIC)
                .addValue("auditEventId", auditEventId, Types.NUMERIC)
                .addValue("fieldName", fieldName, Types.VARCHAR)
                .addValue("fieldChangeTypeCode", fieldChangeTypeCode, Types.VARCHAR)
                .addValue("oldValue", oldValue, Types.CLOB)
                .addValue("newValue", newValue, Types.CLOB)
                .addValue("oldValueHash", oldValueHash, Types.VARCHAR)
                .addValue("newValueHash", newValueHash, Types.VARCHAR)
                .addValue("dataTypeCode", dataTypeCode, Types.VARCHAR)
                .addValue("isSensitive", sensitive ? 1 : 0, Types.NUMERIC)
                .addValue("valueMasked", valueMasked ? 1 : 0, Types.NUMERIC));
    }

    public int insertStatusHistory(
            long openingRequestId,
            String fromStatusCode,
            String toStatusCode,
            Long changeSetId,
            Long auditEventId,
            String changeReasonCode,
            String changeNote,
            String actor,
            String correlationId
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_STATUS_HISTORY (
                    OPENING_REQUEST_ID, FROM_STATUS_CODE, TO_STATUS_CODE,
                    CHANGE_SET_ID, AUDIT_EVENT_ID, CHANGE_REASON_CODE,
                    CHANGE_NOTE, CHANGED_BY, CHANGED_AT,
                    CORRELATION_ID, CREATED_BY
                ) VALUES (
                    :openingRequestId, :fromStatusCode, :toStatusCode,
                    :changeSetId, :auditEventId, :changeReasonCode,
                    :changeNote, :actor, SYSTIMESTAMP,
                    :correlationId, :actor
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("fromStatusCode", fromStatusCode, Types.VARCHAR)
                .addValue("toStatusCode", toStatusCode, Types.VARCHAR)
                .addValue("changeSetId", changeSetId, Types.NUMERIC)
                .addValue("auditEventId", auditEventId, Types.NUMERIC)
                .addValue("changeReasonCode", changeReasonCode, Types.VARCHAR)
                .addValue("changeNote", changeNote, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR)
                .addValue("correlationId", correlationId, Types.VARCHAR));
    }

    public int insertSnapshot(
            long snapshotId,
            long openingRequestId,
            Long changeSetId,
            String snapshotTypeCode,
            long requestVersionNo,
            String snapshotData,
            String snapshotHash,
            String correlationId,
            String actor
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_SNAPSHOT (
                    SNAPSHOT_ID, OPENING_REQUEST_ID, CHANGE_SET_ID,
                    SNAPSHOT_TYPE_CODE, REQUEST_VERSION_NO,
                    SNAPSHOT_FORMAT_CODE, SNAPSHOT_DATA, SNAPSHOT_HASH,
                    HASH_ALGORITHM_CODE, CORRELATION_ID, CREATED_BY
                ) VALUES (
                    :snapshotId, :openingRequestId, :changeSetId,
                    :snapshotTypeCode, :requestVersionNo,
                    'JSON', :snapshotData, :snapshotHash,
                    'SHA256', :correlationId, :actor
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("snapshotId", snapshotId, Types.NUMERIC)
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("changeSetId", changeSetId, Types.NUMERIC)
                .addValue("snapshotTypeCode", snapshotTypeCode, Types.VARCHAR)
                .addValue("requestVersionNo", requestVersionNo, Types.NUMERIC)
                .addValue("snapshotData", snapshotData, Types.CLOB)
                .addValue("snapshotHash", snapshotHash, Types.VARCHAR)
                .addValue("correlationId", correlationId, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public Map<String, Object> canonicalOpeningSnapshot(long openingRequestId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC);
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("DEPOSIT_OPENING_REQUEST", canonicalRows(
                "SELECT * FROM " + schema + ".DEPOSIT_OPENING_REQUEST WHERE OPENING_REQUEST_ID = :openingRequestId ORDER BY 1",
                params));
        for (String table : SNAPSHOT_DIRECT_TABLES) {
            snapshot.put(table, canonicalRows(
                    "SELECT * FROM " + schema + "." + table + " WHERE OPENING_REQUEST_ID = :openingRequestId ORDER BY 1",
                    params));
        }
        snapshot.put("DEPOSIT_OPENING_SIGNATORY_AUTHORITY", canonicalRows(
                "SELECT a.* FROM " + schema + ".DEPOSIT_OPENING_SIGNATORY_AUTHORITY a " +
                        "JOIN " + schema + ".DEPOSIT_OPENING_SIGNATORY s ON s.OPENING_SIGNATORY_ID = a.OPENING_SIGNATORY_ID " +
                        "WHERE s.OPENING_REQUEST_ID = :openingRequestId ORDER BY a.OPENING_SIGNATORY_AUTHORITY_ID",
                params));
        snapshot.put("DEPOSIT_OPENING_MATURITY_INSTRUCTION", canonicalRows(
                "SELECT m.* FROM " + schema + ".DEPOSIT_OPENING_MATURITY_INSTRUCTION m " +
                        "JOIN " + schema + ".DEPOSIT_OPENING_TERM t ON t.OPENING_TERM_ID = m.OPENING_TERM_ID " +
                        "WHERE t.OPENING_REQUEST_ID = :openingRequestId ORDER BY m.OPENING_MATURITY_INSTRUCTION_ID",
                params));
        return snapshot;
    }

    public List<AuditEventView> listAuditEvents(long openingRequestId) {
        String sql = """
                SELECT AUDIT_EVENT_ID, OPENING_REQUEST_ID, CHANGE_SET_ID,
                       ENTITY_NAME, ENTITY_KEY, EVENT_TYPE_CODE, ACTOR_TYPE_CODE,
                       ACTOR_ID, ACTOR_ROLE_CODE, CHANNEL_CODE, ORG_UNIT_CODE,
                       SOURCE_SYSTEM_CODE, OPERATION_NAME, EVENT_AT,
                       REASON_CODE, REASON_NOTE, APPROVAL_REFERENCE,
                       CORRELATION_ID, REQUEST_VERSION_BEFORE, REQUEST_VERSION_AFTER
                  FROM %s.DEPOSIT_OPENING_AUDIT_EVENT
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                 ORDER BY EVENT_AT DESC, AUDIT_EVENT_ID DESC
                """.formatted(schema);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC);
        List<AuditFieldChangeView> fieldChanges = listFieldChanges(openingRequestId);
        Map<Long, List<AuditFieldChangeView>> byEvent = new LinkedHashMap<>();
        for (AuditFieldChangeView item : fieldChanges) {
            byEvent.computeIfAbsent(item.auditEventId(), ignored -> new ArrayList<>()).add(item);
        }
        return jdbc.query(sql, params, (rs, rowNum) -> new AuditEventView(
                rs.getLong("AUDIT_EVENT_ID"), rs.getLong("OPENING_REQUEST_ID"),
                rs.getObject("CHANGE_SET_ID", Long.class), rs.getString("ENTITY_NAME"),
                rs.getString("ENTITY_KEY"), rs.getString("EVENT_TYPE_CODE"),
                rs.getString("ACTOR_TYPE_CODE"), rs.getString("ACTOR_ID"),
                rs.getString("ACTOR_ROLE_CODE"), rs.getString("CHANNEL_CODE"),
                rs.getString("ORG_UNIT_CODE"), rs.getString("SOURCE_SYSTEM_CODE"),
                rs.getString("OPERATION_NAME"), toOffsetDateTime(rs.getTimestamp("EVENT_AT")),
                rs.getString("REASON_CODE"), rs.getString("REASON_NOTE"),
                rs.getString("APPROVAL_REFERENCE"), rs.getString("CORRELATION_ID"),
                rs.getObject("REQUEST_VERSION_BEFORE", Long.class),
                rs.getObject("REQUEST_VERSION_AFTER", Long.class),
                List.copyOf(byEvent.getOrDefault(rs.getLong("AUDIT_EVENT_ID"), List.of()))
        ));
    }

    private List<AuditFieldChangeView> listFieldChanges(long openingRequestId) {
        String sql = """
                SELECT f.FIELD_CHANGE_ID, f.AUDIT_EVENT_ID, f.FIELD_NAME,
                       f.FIELD_CHANGE_TYPE_CODE, f.OLD_VALUE, f.NEW_VALUE,
                       f.OLD_VALUE_HASH, f.NEW_VALUE_HASH, f.DATA_TYPE_CODE,
                       f.IS_SENSITIVE, f.VALUE_MASKED_FLAG
                  FROM %s.DEPOSIT_OPENING_AUDIT_FIELD_CHANGE f
                  JOIN %s.DEPOSIT_OPENING_AUDIT_EVENT e
                    ON e.AUDIT_EVENT_ID = f.AUDIT_EVENT_ID
                 WHERE e.OPENING_REQUEST_ID = :openingRequestId
                 ORDER BY f.FIELD_CHANGE_ID
                """.formatted(schema, schema);
        return jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> new AuditFieldChangeView(
                        rs.getLong("FIELD_CHANGE_ID"), rs.getLong("AUDIT_EVENT_ID"),
                        rs.getString("FIELD_NAME"), rs.getString("FIELD_CHANGE_TYPE_CODE"),
                        rs.getString("OLD_VALUE"), rs.getString("NEW_VALUE"),
                        rs.getString("OLD_VALUE_HASH"), rs.getString("NEW_VALUE_HASH"),
                        rs.getString("DATA_TYPE_CODE"), rs.getInt("IS_SENSITIVE") == 1,
                        rs.getInt("VALUE_MASKED_FLAG") == 1
                ));
    }

    public List<StatusHistoryView> listStatusHistory(long openingRequestId) {
        String sql = """
                SELECT STATUS_HISTORY_ID, OPENING_REQUEST_ID, FROM_STATUS_CODE,
                       TO_STATUS_CODE, CHANGE_SET_ID, AUDIT_EVENT_ID,
                       CHANGE_REASON_CODE, CHANGE_NOTE, CHANGED_BY,
                       CHANGED_AT, CORRELATION_ID
                  FROM %s.DEPOSIT_OPENING_STATUS_HISTORY
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                 ORDER BY CHANGED_AT DESC, STATUS_HISTORY_ID DESC
                """.formatted(schema);
        return jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> new StatusHistoryView(
                        rs.getLong("STATUS_HISTORY_ID"), rs.getLong("OPENING_REQUEST_ID"),
                        rs.getString("FROM_STATUS_CODE"), rs.getString("TO_STATUS_CODE"),
                        rs.getObject("CHANGE_SET_ID", Long.class), rs.getObject("AUDIT_EVENT_ID", Long.class),
                        rs.getString("CHANGE_REASON_CODE"), rs.getString("CHANGE_NOTE"),
                        rs.getString("CHANGED_BY"), toOffsetDateTime(rs.getTimestamp("CHANGED_AT")),
                        rs.getString("CORRELATION_ID")
                ));
    }

    public List<SnapshotView> listSnapshots(long openingRequestId) {
        String sql = """
                SELECT SNAPSHOT_ID, OPENING_REQUEST_ID, CHANGE_SET_ID,
                       SNAPSHOT_TYPE_CODE, REQUEST_VERSION_NO,
                       SNAPSHOT_FORMAT_CODE, SNAPSHOT_HASH, HASH_ALGORITHM_CODE,
                       CORRELATION_ID, CREATED_AT, CREATED_BY
                  FROM %s.DEPOSIT_OPENING_SNAPSHOT
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                 ORDER BY CREATED_AT DESC, SNAPSHOT_ID DESC
                """.formatted(schema);
        return jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> mapSnapshot(rs));
    }

    public Optional<SnapshotDocument> findSnapshot(long openingRequestId, long snapshotId) {
        String sql = """
                SELECT SNAPSHOT_ID, OPENING_REQUEST_ID, CHANGE_SET_ID,
                       SNAPSHOT_TYPE_CODE, REQUEST_VERSION_NO,
                       SNAPSHOT_FORMAT_CODE, SNAPSHOT_HASH, HASH_ALGORITHM_CODE,
                       CORRELATION_ID, CREATED_AT, CREATED_BY, SNAPSHOT_DATA
                  FROM %s.DEPOSIT_OPENING_SNAPSHOT
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   AND SNAPSHOT_ID = :snapshotId
                """.formatted(schema);
        List<SnapshotDocument> rows = jdbc.query(sql, new MapSqlParameterSource()
                        .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                        .addValue("snapshotId", snapshotId, Types.NUMERIC),
                (rs, rowNum) -> new SnapshotDocument(mapSnapshot(rs), rs.getString("SNAPSHOT_DATA")));
        return rows.stream().findFirst();
    }

    public boolean activeReferenceCode(String table, String codeColumn, String code) {
        String safeTable = requireIdentifier(table);
        String safeColumn = requireIdentifier(codeColumn);
        String sql = "SELECT COUNT(*) FROM " + schema + "." + safeTable +
                " WHERE " + safeColumn + " = :code AND NVL(IS_ACTIVE, 1) = 1 " +
                "AND (VALID_FROM IS NULL OR VALID_FROM <= SYSDATE) " +
                "AND (VALID_TO IS NULL OR VALID_TO >= SYSDATE)";
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("code", code, Types.VARCHAR), Integer.class);
        return count != null && count > 0;
    }

    private boolean isMutableBusinessField(String entity, String field, EntitySpec spec) {
        if (SYSTEM_COLUMNS.contains(field) || field.equals(spec.keyColumn())) return false;
        if ("DEPOSIT_OPENING_REQUEST".equals(entity)) return MUTABLE_REQUEST_FIELDS.contains(field);
        if ("OPENING_REQUEST_ID".equals(field) || "OPENING_SIGNATORY_ID".equals(field) || "OPENING_TERM_ID".equals(field)) return false;
        return true;
    }

    private String ownershipPredicate(EntitySpec spec) {
        return switch (spec.ownershipMode()) {
            case ROOT -> "t.OPENING_REQUEST_ID = :openingRequestId AND t.OPENING_REQUEST_ID = :entityId";
            case DIRECT -> "t.OPENING_REQUEST_ID = :openingRequestId AND t." + spec.keyColumn() + " = :entityId";
            case SIGNATORY -> "t." + spec.keyColumn() + " = :entityId AND EXISTS (SELECT 1 FROM " + schema +
                    ".DEPOSIT_OPENING_SIGNATORY s WHERE s.OPENING_SIGNATORY_ID=t.OPENING_SIGNATORY_ID " +
                    "AND s.OPENING_REQUEST_ID=:openingRequestId)";
            case TERM -> "t." + spec.keyColumn() + " = :entityId AND EXISTS (SELECT 1 FROM " + schema +
                    ".DEPOSIT_OPENING_TERM d WHERE d.OPENING_TERM_ID=t.OPENING_TERM_ID " +
                    "AND d.OPENING_REQUEST_ID=:openingRequestId)";
        };
    }

    private static boolean supportedMutationType(String dataType) {
        if (dataType == null) return false;
        String type = dataType.toUpperCase(Locale.ROOT);
        return type.startsWith("VARCHAR") || type.startsWith("NVARCHAR") || type.equals("CHAR") || type.equals("NCHAR") ||
                type.equals("NUMBER") || type.equals("DECIMAL") || type.equals("NUMERIC") ||
                type.equals("DATE") || type.startsWith("TIMESTAMP");
    }

    private static void addTypedValue(MapSqlParameterSource params, String parameter, Object value) {
        if (value instanceof java.time.LocalDate date) params.addValue(parameter, Date.valueOf(date), Types.DATE);
        else if (value instanceof java.time.OffsetDateTime dateTime) params.addValue(parameter, Timestamp.from(dateTime.toInstant()), Types.TIMESTAMP);
        else if (value instanceof java.time.LocalDateTime dateTime) params.addValue(parameter, Timestamp.valueOf(dateTime), Types.TIMESTAMP);
        else if (value instanceof Timestamp timestamp) params.addValue(parameter, timestamp, Types.TIMESTAMP);
        else if (value instanceof BigDecimal || value instanceof Number) params.addValue(parameter, value, Types.NUMERIC);
        else params.addValue(parameter, value, Types.VARCHAR);
    }

    private List<Map<String, Object>> canonicalRows(String sql, MapSqlParameterSource params) {
        ResultSetExtractor<List<Map<String, Object>>> extractor = rs -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();
            while (rs.next()) {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String column = metadata.getColumnLabel(i).toUpperCase(Locale.ROOT);
                    row.put(column, canonicalValue(rs.getObject(i)));
                }
                rows.add(row);
            }
            return rows;
        };
        List<Map<String, Object>> rows = jdbc.query(sql, params, extractor);
        return rows == null ? List.of() : rows;
    }

    private static Object canonicalValue(Object value) throws SQLException {
        if (value == null) return null;
        if (value instanceof Timestamp timestamp) return timestamp.toInstant().toString();
        if (value instanceof Date date) return date.toLocalDate().toString();
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros();
        if (value instanceof Clob clob) return clob.getSubString(1, Math.toIntExact(clob.length()));
        return value;
    }

    private long nextValue(String sequenceName) {
        String safeSequence = requireIdentifier(sequenceName);
        return jdbc.getJdbcOperations().queryForObject(
                "SELECT " + schema + "." + safeSequence + ".NEXTVAL FROM DUAL",
                Long.class
        );
    }

    private static ChangeSetView mapChangeSet(java.sql.ResultSet rs) throws SQLException {
        return new ChangeSetView(
                rs.getLong("CHANGE_SET_ID"), rs.getLong("OPENING_REQUEST_ID"),
                rs.getLong("CHANGE_NO"), rs.getString("CHANGE_TYPE_CODE"),
                rs.getString("CHANGE_STATUS_CODE"), rs.getString("CHANGE_REASON_CODE"),
                rs.getString("CHANGE_REASON_NOTE"), rs.getString("REQUESTED_BY"),
                toOffsetDateTime(rs.getTimestamp("REQUESTED_AT")), rs.getString("APPROVED_BY"),
                toOffsetDateTime(rs.getTimestamp("APPROVED_AT")),
                rs.getObject("BASE_REQUEST_VERSION", Long.class),
                rs.getObject("RESULT_REQUEST_VERSION", Long.class),
                rs.getString("CORRELATION_ID")
        );
    }

    private static SnapshotView mapSnapshot(java.sql.ResultSet rs) throws SQLException {
        return new SnapshotView(
                rs.getLong("SNAPSHOT_ID"), rs.getLong("OPENING_REQUEST_ID"),
                rs.getObject("CHANGE_SET_ID", Long.class), rs.getString("SNAPSHOT_TYPE_CODE"),
                rs.getLong("REQUEST_VERSION_NO"), rs.getString("SNAPSHOT_FORMAT_CODE"),
                rs.getString("SNAPSHOT_HASH"), rs.getString("HASH_ALGORITHM_CODE"),
                rs.getString("CORRELATION_ID"), toOffsetDateTime(rs.getTimestamp("CREATED_AT")),
                rs.getString("CREATED_BY")
        );
    }

    private static MapSqlParameterSource params(long openingRequestId, long changeSetId, String actor) {
        return new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("changeSetId", changeSetId, Types.NUMERIC)
                .addValue("actor", actor, Types.VARCHAR);
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static String requireIdentifier(String raw) {
        if (raw == null) throw new IllegalArgumentException("Oracle identifier is required.");
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_$#]{0,127}")) {
            throw new IllegalArgumentException("Invalid Oracle identifier: " + raw);
        }
        return normalized;
    }
    private record EntitySpec(String keyColumn, OwnershipMode ownershipMode) {
    }

    private enum OwnershipMode {
        ROOT, DIRECT, SIGNATORY, TERM
    }

}
