package com.behsazan.corebanking.collateral.amendment;

import com.behsazan.corebanking.collateral.amendment.CollateralAmendmentModels.Record;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Fixed SQL over CLT's existing shared application DataSource. No auto-migrations. */
@Repository
public class CollateralAmendmentRepository {
    public record Source(String collateralId, String branch, String status,
            String version, Map<String, String> values) {}
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CollateralAmendmentRepository(DataSource dataSource, JsonMapper json) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(15);
        this.json = json;
    }
    public boolean schemaReady() {
        Integer tables = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='CLT' AND TABLE_NAME IN
                ('CLT_COLLATERAL_AMENDMENT','CLT_COLLATERAL_AMENDMENT_AUDIT')
                """, Integer.class);
        Integer cols = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
                  AND TABLE_NAME='CLT_COLLATERAL_AMENDMENT' AND COLUMN_NAME IN
                  ('AMENDMENT_ID','COLLATERAL_ID','BRANCH_CODE','MAKER_SUBJECT',
                   'BASE_RECORD_VERSION','BEFORE_JSON','AFTER_JSON','CHANGES_JSON',
                   'REQUEST_HASH','REQUEST_REASON','IDEMPOTENCY_KEY','CORRELATION_ID',
                   'STATUS','CHECKER_SUBJECT','DECISION_REASON','DECISION_KEY',
                   'DECIDED_AT','APPLIED_RECORD_VERSION','CREATED_AT','RECORD_VERSION')
                """, Integer.class);
        Integer auditCols = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='CLT'
                  AND TABLE_NAME='CLT_COLLATERAL_AMENDMENT_AUDIT'
                  AND COLUMN_NAME IN ('AUDIT_ID','AMENDMENT_ID','EVENT_CODE','EVENT_SUBJECT',
                                      'CORRELATION_ID','REQUEST_HASH','EVENT_AT')
                """, Integer.class);
        Integer seq = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='CLT'
                  AND SEQUENCE_NAME='SEQ_CLT_COLLATERAL_AMENDMENT'
                """, Integer.class);
        return Integer.valueOf(2).equals(tables) && Integer.valueOf(20).equals(cols)
                && Integer.valueOf(7).equals(auditCols) && Integer.valueOf(1).equals(seq);
    }
    private Source toSource(ResultSet rs) throws SQLException {
        Map<String,String> value = new LinkedHashMap<>();
        value.put("VALID_FROM", rs.getDate("VALID_FROM").toLocalDate().toString());
        value.put("ESTIMATE_AMOUNT", rs.getBigDecimal("ESTIMATE_AMOUNT").stripTrailingZeros().toPlainString());
        Date to = rs.getDate("VALID_TO"); value.put("VALID_TO", to == null ? null : to.toLocalDate().toString());
        value.put("DEPOSIT_NUMBER", rs.getString("DEPOSIT_NUMBER"));
        value.put("REMARKS", rs.getString("REMARKS"));
        return new Source(rs.getBigDecimal("COLLATERAL_ID").toPlainString(),
                rs.getString("BRANCH_CODE"), rs.getString("COLLATERAL_STATUS_CODE"),
                rs.getBigDecimal("RECORD_VERSION").toPlainString(), value);
    }
    /** Locking the parent serializes proposals/decisions with other CLT parent writers. */
    public Source sourceForUpdate(String id, String branch) {
        List<Source> rows = jdbc.query("""
                SELECT COLLATERAL_ID, BRANCH_CODE, COLLATERAL_STATUS_CODE, RECORD_VERSION,
                       VALID_FROM, ESTIMATE_AMOUNT, VALID_TO, DEPOSIT_NUMBER, REMARKS
                  FROM CLT.COLLATERAL
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=? FOR UPDATE
                """, (rs,n) -> toSource(rs), new BigDecimal(id), branch);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public Source source(String id, String branch) {
        List<Source> rows = jdbc.query("""
                SELECT COLLATERAL_ID, BRANCH_CODE, COLLATERAL_STATUS_CODE, RECORD_VERSION,
                       VALID_FROM, ESTIMATE_AMOUNT, VALID_TO, DEPOSIT_NUMBER, REMARKS
                  FROM CLT.COLLATERAL WHERE COLLATERAL_ID=? AND BRANCH_CODE=?
                """, (rs,n) -> toSource(rs), new BigDecimal(id), branch);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public boolean hasAnyDependency(String collateralId) {
        Long count = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM CLT.COLLATERAL_SPONSOR WHERE COLLATERAL_ID=?)
                     + (SELECT COUNT(*) FROM CLT.COLLATERAL_INSURANCE WHERE COLLATERAL_ID=?)
                     + (SELECT COUNT(*) FROM CLT.COLLATERAL_VALUATION_HISTORY WHERE COLLATERAL_ID=?)
                     + (SELECT COUNT(*) FROM CLT.COLLATERAL_ALLOCATION WHERE COLLATERAL_ID=?) AS TOTAL
                  FROM DUAL
                """, Long.class, new BigDecimal(collateralId), new BigDecimal(collateralId),
                new BigDecimal(collateralId), new BigDecimal(collateralId));
        return count == null || count > 0L;
    }
    public String json(Map<String,String> values) {
        try { return json.writeValueAsString(values); }
        catch (Exception ex) { throw new IllegalStateException("CLT_G01_JSON_WRITE_FAILED", ex); }
    }
    @SuppressWarnings("unchecked")
    private Map<String,String> values(String text) {
        try {
            Map<?,?> parsed=json.readValue(text, Map.class);
            Map<String,String> result=new LinkedHashMap<>();
            for (var entry : parsed.entrySet()) {
                if (!(entry.getKey() instanceof String key)
                        || (entry.getValue()!=null && !(entry.getValue() instanceof String)))
                    throw new IllegalStateException("CLT_G01_STORED_JSON_INVALID");
                result.put(key, (String) entry.getValue());
            }
            return result;
        } catch (Exception e) { throw new IllegalStateException("CLT_G01_STORED_JSON_INVALID", e); }
    }
    private Record record(ResultSet rs) throws SQLException {
        Timestamp decided=rs.getTimestamp("DECIDED_AT");
        BigDecimal applied=rs.getBigDecimal("APPLIED_RECORD_VERSION");
        return new Record(rs.getBigDecimal("AMENDMENT_ID").toPlainString(),
                rs.getBigDecimal("COLLATERAL_ID").toPlainString(), rs.getString("STATUS"),
                rs.getString("MAKER_SUBJECT"), rs.getString("CHECKER_SUBJECT"),
                rs.getString("REQUEST_REASON"), rs.getString("DECISION_REASON"),
                rs.getBigDecimal("BASE_RECORD_VERSION").toPlainString(),
                applied==null?null:applied.toPlainString(), rs.getBigDecimal("RECORD_VERSION").toPlainString(),
                rs.getTimestamp("CREATED_AT").toLocalDateTime().toString(),
                decided==null?null:decided.toLocalDateTime().toString(),
                values(rs.getString("BEFORE_JSON")), values(rs.getString("AFTER_JSON")),
                values(rs.getString("CHANGES_JSON")));
    }
    private static final String COLUMNS = """
            SELECT AMENDMENT_ID,COLLATERAL_ID,STATUS,MAKER_SUBJECT,CHECKER_SUBJECT,
                   REQUEST_REASON,DECISION_REASON,BASE_RECORD_VERSION,APPLIED_RECORD_VERSION,
                   RECORD_VERSION,CREATED_AT,DECIDED_AT,BEFORE_JSON,AFTER_JSON,CHANGES_JSON
            FROM CLT.CLT_COLLATERAL_AMENDMENT
            """;
    public Record byId(String id, String collateralId, String branch, boolean lock) {
        List<Record> records=jdbc.query(COLUMNS +
                " WHERE AMENDMENT_ID=? AND COLLATERAL_ID=? AND BRANCH_CODE=?" + (lock?" FOR UPDATE":""),
                (rs,n)->record(rs), new BigDecimal(id), new BigDecimal(collateralId), branch);
        return records.isEmpty()?null:records.getFirst();
    }
    public List<Record> list(String collateralId,String branch) {
        return jdbc.query(COLUMNS + " WHERE COLLATERAL_ID=? AND BRANCH_CODE=? "
                + " ORDER BY CREATED_AT DESC, AMENDMENT_ID DESC FETCH FIRST 50 ROWS ONLY",
                (rs,n)->record(rs), new BigDecimal(collateralId),branch);
    }
    public Record byIdempotency(String maker,String branch,String key) {
        List<Record> records=jdbc.query(COLUMNS +
                " WHERE MAKER_SUBJECT=? AND BRANCH_CODE=? AND IDEMPOTENCY_KEY=?",
                (rs,n)->record(rs),maker,branch,key);
        return records.isEmpty()?null:records.getFirst();
    }
    public String requestHash(String id) {
        return jdbc.queryForObject("SELECT REQUEST_HASH FROM CLT.CLT_COLLATERAL_AMENDMENT WHERE AMENDMENT_ID=?",
                String.class, new BigDecimal(id));
    }
    public boolean hasPending(String collateralId) {
        Integer c=jdbc.queryForObject("SELECT COUNT(*) FROM CLT.CLT_COLLATERAL_AMENDMENT "
                +" WHERE COLLATERAL_ID=? AND STATUS='PENDING_REVIEW'",Integer.class,new BigDecimal(collateralId));
        return c==null || c>0;
    }
    public Record insert(String collateralId,String branch,String maker,String baseVersion,
            Map<String,String> before,Map<String,String> after,Map<String,String> changes,
            String reason,String key,String hash,String trace) {
        BigDecimal id=jdbc.queryForObject("SELECT CLT.SEQ_CLT_COLLATERAL_AMENDMENT.NEXTVAL FROM DUAL",BigDecimal.class);
        if(id==null)throw new IllegalStateException("CLT_G01_SEQUENCE_UNAVAILABLE");
        String beforeJson=json(before),afterJson=json(after),changeJson=json(changes);
        jdbc.update(connection->{
            var ps=connection.prepareStatement("""
                    INSERT INTO CLT.CLT_COLLATERAL_AMENDMENT
                     (AMENDMENT_ID,COLLATERAL_ID,BRANCH_CODE,MAKER_SUBJECT,BASE_RECORD_VERSION,
                      BEFORE_JSON,AFTER_JSON,CHANGES_JSON,REQUEST_HASH,REQUEST_REASON,
                      IDEMPOTENCY_KEY,CORRELATION_ID,STATUS,RECORD_VERSION)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'PENDING_REVIEW',1)
                    """);
            ps.setBigDecimal(1,id);ps.setBigDecimal(2,new BigDecimal(collateralId));
            ps.setString(3,branch);ps.setString(4,maker);ps.setBigDecimal(5,new BigDecimal(baseVersion));
            ps.setCharacterStream(6,new StringReader(beforeJson),beforeJson.length());
            ps.setCharacterStream(7,new StringReader(afterJson),afterJson.length());
            ps.setCharacterStream(8,new StringReader(changeJson),changeJson.length());
            ps.setString(9,hash);ps.setString(10,reason);ps.setString(11,key);ps.setString(12,trace);
            return ps;
        });
        audit(id.toPlainString(),"AMENDMENT_SUBMITTED",maker,trace,hash);
        return byId(id.toPlainString(),collateralId,branch,false);
    }
    public void audit(String amendmentId,String event,String actor,String trace,String hash) {
        jdbc.update("""
                INSERT INTO CLT.CLT_COLLATERAL_AMENDMENT_AUDIT
                  (AMENDMENT_ID,EVENT_CODE,EVENT_SUBJECT,CORRELATION_ID,REQUEST_HASH)
                VALUES (?,?,?,?,?)
                """,new BigDecimal(amendmentId),event,actor,trace,hash);
    }
    public int apply(Source expected, Map<String,String> after,String actor) {
        String end=after.get("VALID_TO");
        return jdbc.update("""
                UPDATE CLT.COLLATERAL SET ESTIMATE_AMOUNT=?,VALID_TO=?,DEPOSIT_NUMBER=?,
                    REMARKS=?,UPDATED_AT=SYSTIMESTAMP,UPDATED_BY=?,RECORD_VERSION=RECORD_VERSION+1
                 WHERE COLLATERAL_ID=? AND BRANCH_CODE=? AND RECORD_VERSION=?
                   AND COLLATERAL_STATUS_CODE='ACTIVE'
                """,new BigDecimal(after.get("ESTIMATE_AMOUNT")),
                end==null?null:Date.valueOf(end),after.get("DEPOSIT_NUMBER"),after.get("REMARKS"),
                actor,new BigDecimal(expected.collateralId()),expected.branch(),new BigDecimal(expected.version()));
    }
    public int decide(String amendmentId,String branch,String actor,String newStatus,String reason,
            String key,String appliedVersion) {
        return jdbc.update("""
                UPDATE CLT.CLT_COLLATERAL_AMENDMENT SET STATUS=?, CHECKER_SUBJECT=?,
                    DECISION_REASON=?,DECISION_KEY=?,DECIDED_AT=SYSTIMESTAMP,
                    APPLIED_RECORD_VERSION=?,RECORD_VERSION=RECORD_VERSION+1
                 WHERE AMENDMENT_ID=? AND BRANCH_CODE=? AND STATUS='PENDING_REVIEW' AND RECORD_VERSION=1
                """,newStatus,actor,reason,key,
                appliedVersion==null?null:new BigDecimal(appliedVersion),new BigDecimal(amendmentId),branch);
    }
    public boolean exactDecisionReplay(String amendmentId,String actor,String decisionKey) {
        Integer count=jdbc.queryForObject("""
                SELECT COUNT(*) FROM CLT.CLT_COLLATERAL_AMENDMENT
                 WHERE AMENDMENT_ID=? AND CHECKER_SUBJECT=? AND DECISION_KEY=?
                """,Integer.class,new BigDecimal(amendmentId),actor,decisionKey);
        return Integer.valueOf(1).equals(count);
    }
}
