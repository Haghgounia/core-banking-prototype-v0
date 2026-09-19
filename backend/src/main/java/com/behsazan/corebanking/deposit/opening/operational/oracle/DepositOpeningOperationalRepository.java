package com.behsazan.corebanking.deposit.opening.operational.oracle;

import com.behsazan.corebanking.deposit.opening.operational.domain.DepositOpeningOperationalModels.ReadinessCheckView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Repository
public class DepositOpeningOperationalRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;

    public DepositOpeningOperationalRepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-opening:DPS2}") String schema
    ) {
        this.jdbc = jdbc;
        this.schema = requireIdentifier(schema);
    }

    public List<FundingRow> listFundings(long openingRequestId) {
        String sql = """
                SELECT OPENING_FUNDING_ID, FUNDING_METHOD_CODE, FUNDING_AMOUNT,
                       SOURCE_PARTY_ID, SOURCE_ACCOUNT_ID, SOURCE_REFERENCE,
                       FUNDING_PURPOSE_CODE, SOURCE_OWNERSHIP_VERIFIED_FLAG,
                       FUNDING_STATUS_CODE, TRANSACTION_REFERENCE
                  FROM %s.DEPOSIT_OPENING_FUNDING
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                 ORDER BY OPENING_FUNDING_ID
                """.formatted(schema);
        return jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> new FundingRow(
                        rs.getLong("OPENING_FUNDING_ID"),
                        rs.getString("FUNDING_METHOD_CODE"),
                        rs.getBigDecimal("FUNDING_AMOUNT"),
                        rs.getObject("SOURCE_PARTY_ID", Long.class),
                        rs.getObject("SOURCE_ACCOUNT_ID", Long.class),
                        rs.getString("SOURCE_REFERENCE"),
                        rs.getString("FUNDING_PURPOSE_CODE"),
                        rs.getInt("SOURCE_OWNERSHIP_VERIFIED_FLAG"),
                        rs.getString("FUNDING_STATUS_CODE"),
                        rs.getString("TRANSACTION_REFERENCE")
                ));
    }

    public List<ObligationRow> listObligations(long openingRequestId) {
        String sql = """
                SELECT OPENING_OBLIGATION_ID, OBLIGATION_TYPE_CODE, FINAL_AMOUNT,
                       MANDATORY_FOR_ACTIVATION_FLAG, SETTLEMENT_STATUS_CODE, SETTLEMENT_REFERENCE
                  FROM %s.DEPOSIT_OPENING_OBLIGATION
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                 ORDER BY OPENING_OBLIGATION_ID
                """.formatted(schema);
        return jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> new ObligationRow(
                        rs.getLong("OPENING_OBLIGATION_ID"),
                        rs.getString("OBLIGATION_TYPE_CODE"),
                        rs.getBigDecimal("FINAL_AMOUNT"),
                        rs.getInt("MANDATORY_FOR_ACTIVATION_FLAG"),
                        rs.getString("SETTLEMENT_STATUS_CODE"),
                        rs.getString("SETTLEMENT_REFERENCE")
                ));
    }

    public int countAllocations(long openingRequestId) {
        String sql = """
                SELECT COUNT(*)
                  FROM %s.DEPOSIT_OPENING_FUND_ALLOC a
                  JOIN %s.DEPOSIT_OPENING_OBLIGATION o
                    ON o.OPENING_OBLIGATION_ID = a.OPENING_OBLIGATION_ID
                 WHERE o.OPENING_REQUEST_ID = :openingRequestId
                """.formatted(schema, schema);
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                Integer.class);
        return count == null ? 0 : count;
    }

    public long nextFundAllocationId() {
        Long value = jdbc.getJdbcOperations().queryForObject(
                "SELECT " + schema + ".SEQ_DEP_OPEN_FUND_ALLOC.NEXTVAL FROM DUAL",
                Long.class
        );
        if (value == null) throw new IllegalStateException("SEQ_DEP_OPEN_FUND_ALLOC returned null.");
        return value;
    }

    public int settleFunding(long fundingId, String settlementReference, String actor) {
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_FUNDING
                   SET FUNDING_STATUS_CODE = 'SUCCESS',
                       TRANSACTION_REFERENCE = COALESCE(TRANSACTION_REFERENCE, :settlementReference),
                       ATTEMPT_AT = COALESCE(ATTEMPT_AT, SYSTIMESTAMP),
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION,0) + 1
                 WHERE OPENING_FUNDING_ID = :fundingId
                   AND FUNDING_STATUS_CODE <> 'SUCCESS'
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("fundingId", fundingId, Types.NUMERIC)
                .addValue("settlementReference", settlementReference, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int settleObligation(long obligationId, boolean waived, String settlementReference, String actor) {
        String status = waived ? "WAIVED" : "SETTLED";
        String sql = """
                UPDATE %s.DEPOSIT_OPENING_OBLIGATION
                   SET SETTLEMENT_STATUS_CODE = :status,
                       SETTLEMENT_REFERENCE = COALESCE(SETTLEMENT_REFERENCE, :settlementReference),
                       WAIVER_REFERENCE = CASE WHEN :status = 'WAIVED'
                                               THEN COALESCE(WAIVER_REFERENCE, :settlementReference)
                                               ELSE WAIVER_REFERENCE END,
                       SETTLED_AT = COALESCE(SETTLED_AT, SYSTIMESTAMP),
                       UPDATED_AT = SYSTIMESTAMP,
                       UPDATED_BY = :actor,
                       RECORD_VERSION = NVL(RECORD_VERSION,0) + 1
                 WHERE OPENING_OBLIGATION_ID = :obligationId
                   AND SETTLEMENT_STATUS_CODE NOT IN ('SETTLED','WAIVED')
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("obligationId", obligationId, Types.NUMERIC)
                .addValue("status", status, Types.VARCHAR)
                .addValue("settlementReference", settlementReference, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int insertAllocation(
            long allocationId,
            long fundingId,
            long obligationId,
            BigDecimal amount,
            String settlementReference,
            String actor
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_FUND_ALLOC (
                    OPENING_FUND_ALLOC_ID, OPENING_FUNDING_ID, OPENING_OBLIGATION_ID,
                    ALLOCATED_AMOUNT, ALLOCATION_STATUS_CODE, SETTLEMENT_REFERENCE, CREATED_BY
                ) VALUES (
                    :allocationId, :fundingId, :obligationId,
                    :amount, 'POSTED', :settlementReference, :actor
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("allocationId", allocationId, Types.NUMERIC)
                .addValue("fundingId", fundingId, Types.NUMERIC)
                .addValue("obligationId", obligationId, Types.NUMERIC)
                .addValue("amount", amount, Types.NUMERIC)
                .addValue("settlementReference", settlementReference, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public int unresolvedRequiredAccountCreationChecks(long openingRequestId) {
        String sql = """
                SELECT COUNT(*)
                  FROM %s.REF_DEP_OPEN_CHECK r
                 WHERE r.IS_ACTIVE = 1
                   AND r.DEFAULT_REQUIRED_FLAG = 1
                   AND r.DEFAULT_BLOCKING_SCOPE_CODE = 'ACCOUNT_CREATION'
                   AND NOT EXISTS (
                       SELECT 1
                         FROM %s.DEPOSIT_OPENING_CHECK c
                        WHERE c.OPENING_REQUEST_ID = :openingRequestId
                          AND c.CHECK_CODE = r.CHECK_CODE
                          AND c.ATTEMPT_NO = (
                              SELECT MAX(x.ATTEMPT_NO)
                                FROM %s.DEPOSIT_OPENING_CHECK x
                               WHERE x.OPENING_REQUEST_ID = c.OPENING_REQUEST_ID
                                 AND x.CHECK_CODE = c.CHECK_CODE
                          )
                          AND c.RESULT_STATUS_CODE IN ('PASS','WAIVED','NOT_APPLICABLE')
                   )
                """.formatted(schema, schema, schema);
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                Integer.class);
        return count == null ? 0 : count;
    }

    public boolean allMandatoryObligationsSettled(long openingRequestId) {
        String sql = """
                SELECT COUNT(*)
                  FROM %s.DEPOSIT_OPENING_OBLIGATION
                 WHERE OPENING_REQUEST_ID = :openingRequestId
                   AND MANDATORY_FOR_ACTIVATION_FLAG = 1
                   AND SETTLEMENT_STATUS_CODE NOT IN ('SETTLED','WAIVED')
                """.formatted(schema);
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                Integer.class);
        return count != null && count == 0;
    }

    public boolean fundingSourcesValid(long openingRequestId) {
        String sql = """
                SELECT COUNT(*)
                  FROM %s.DEPOSIT_OPENING_FUNDING f
                  JOIN %s.DEPOSIT_OPENING_REQUEST r
                    ON r.OPENING_REQUEST_ID = f.OPENING_REQUEST_ID
                 WHERE f.OPENING_REQUEST_ID = :openingRequestId
                   AND (
                        f.FUNDING_STATUS_CODE <> 'SUCCESS'
                        OR (f.SOURCE_ACCOUNT_ID IS NOT NULL AND NVL(f.SOURCE_OWNERSHIP_VERIFIED_FLAG,0) <> 1)
                        OR (r.CUSTOMER_RISK_LEVEL_CODE = 'HIGH' AND
                            (f.SOURCE_ACCOUNT_ID IS NULL OR NVL(f.SOURCE_OWNERSHIP_VERIFIED_FLAG,0) <> 1))
                   )
                """.formatted(schema, schema);
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                Integer.class);
        return count != null && count == 0;
    }

    public boolean signatoryMandateReady(long openingRequestId, String ownershipTypeCode) {
        if ("INDIVIDUAL".equalsIgnoreCase(ownershipTypeCode)) return true;
        String sql = """
                SELECT COUNT(*)
                  FROM %s.DEPOSIT_OPENING_SIGNATORY_AUTHORITY a
                  JOIN %s.DEPOSIT_OPENING_SIGNATORY s
                    ON s.OPENING_SIGNATORY_ID = a.OPENING_SIGNATORY_ID
                 WHERE s.OPENING_REQUEST_ID = :openingRequestId
                """.formatted(schema, schema);
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                Integer.class);
        return count != null && count > 0;
    }

    public TermReadiness termReadiness(long openingRequestId) {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM %s.DEPOSIT_OPENING_TERM t WHERE t.OPENING_REQUEST_ID=:openingRequestId) TERM_COUNT,
                  (SELECT COUNT(*) FROM %s.DEPOSIT_OPENING_PROFIT_INSTRUCTION p WHERE p.OPENING_REQUEST_ID=:openingRequestId) PROFIT_COUNT,
                  (SELECT COUNT(*) FROM %s.DEPOSIT_OPENING_MATURITY_INSTRUCTION m
                     JOIN %s.DEPOSIT_OPENING_TERM t ON t.OPENING_TERM_ID=m.OPENING_TERM_ID
                    WHERE t.OPENING_REQUEST_ID=:openingRequestId) MATURITY_COUNT
                FROM dual
                """.formatted(schema, schema, schema, schema);
        return jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> new TermReadiness(
                        rs.getInt("TERM_COUNT"), rs.getInt("PROFIT_COUNT"), rs.getInt("MATURITY_COUNT")
                ));
    }

    public List<CheckDefinition> activationDefinitions() {
        String sql = """
                SELECT CHECK_CODE, TITLE_FA, CHECK_TYPE_CODE, DEFAULT_REQUIRED_FLAG,
                       DEFAULT_PHASE_CODE, DEFAULT_BLOCKING_SCOPE_CODE, DEFAULT_RECHECK_FLAG,
                       RESULT_VALIDITY_MINUTES
                  FROM %s.REF_DEP_OPEN_CHECK
                 WHERE IS_ACTIVE = 1
                   AND DEFAULT_PHASE_CODE IN ('PRE_ACTIVATION','POST_ACTIVATION')
                 ORDER BY EXECUTION_ORDER, DISPLAY_ORDER, CHECK_CODE
                """.formatted(schema);
        return jdbc.query(sql, (rs, rowNum) -> new CheckDefinition(
                rs.getString("CHECK_CODE"), rs.getString("TITLE_FA"), rs.getString("CHECK_TYPE_CODE"),
                rs.getInt("DEFAULT_REQUIRED_FLAG"), rs.getString("DEFAULT_PHASE_CODE"),
                rs.getString("DEFAULT_BLOCKING_SCOPE_CODE"), rs.getInt("DEFAULT_RECHECK_FLAG"),
                rs.getObject("RESULT_VALIDITY_MINUTES", Integer.class)
        ));
    }

    public int nextAttemptNo(long openingRequestId, String checkCode) {
        String sql = """
                SELECT NVL(MAX(ATTEMPT_NO),0)+1
                  FROM %s.DEPOSIT_OPENING_CHECK
                 WHERE OPENING_REQUEST_ID=:openingRequestId AND CHECK_CODE=:checkCode
                """.formatted(schema);
        Integer value = jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("checkCode", checkCode, Types.VARCHAR), Integer.class);
        return value == null ? 1 : value;
    }

    public int insertReadinessCheck(
            long openingRequestId,
            CheckDefinition definition,
            int attemptNo,
            String resultStatusCode,
            String resultReference,
            OffsetDateTime validUntil,
            String sourceEvaluationReference,
            String waiverReason,
            String actor
    ) {
        String sql = """
                INSERT INTO %s.DEPOSIT_OPENING_CHECK (
                    OPENING_REQUEST_ID, CHECK_CODE, CHECK_TYPE_CODE, ATTEMPT_NO,
                    CHECK_PHASE_CODE, BLOCKING_SCOPE_CODE, REQUIRED_FLAG, RECHECK_REQUIRED_FLAG,
                    RESULT_STATUS_CODE, RESULT_REFERENCE, CHECKED_AT, VALID_UNTIL,
                    SOURCE_EVALUATION_REFERENCE, WAIVER_REASON, CREATED_BY
                ) VALUES (
                    :openingRequestId, :checkCode, :checkTypeCode, :attemptNo,
                    :checkPhaseCode, :blockingScopeCode, :requiredFlag, :recheckRequiredFlag,
                    :resultStatusCode, :resultReference, SYSTIMESTAMP, :validUntil,
                    :sourceEvaluationReference, :waiverReason, :actor
                )
                """.formatted(schema);
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("openingRequestId", openingRequestId, Types.NUMERIC)
                .addValue("checkCode", definition.checkCode(), Types.VARCHAR)
                .addValue("checkTypeCode", definition.checkTypeCode(), Types.VARCHAR)
                .addValue("attemptNo", attemptNo, Types.NUMERIC)
                .addValue("checkPhaseCode", definition.checkPhaseCode(), Types.VARCHAR)
                .addValue("blockingScopeCode", definition.blockingScopeCode(), Types.VARCHAR)
                .addValue("requiredFlag", definition.requiredFlag(), Types.NUMERIC)
                .addValue("recheckRequiredFlag", definition.recheckRequiredFlag(), Types.NUMERIC)
                .addValue("resultStatusCode", resultStatusCode, Types.VARCHAR)
                .addValue("resultReference", resultReference, Types.VARCHAR)
                .addValue("validUntil", toTimestamp(validUntil), Types.TIMESTAMP)
                .addValue("sourceEvaluationReference", sourceEvaluationReference, Types.VARCHAR)
                .addValue("waiverReason", waiverReason, Types.VARCHAR)
                .addValue("actor", actor, Types.VARCHAR));
    }

    public List<ReadinessCheckView> latestActivationChecks(long openingRequestId) {
        String sql = """
                SELECT c.CHECK_CODE, r.TITLE_FA, c.CHECK_TYPE_CODE, c.CHECK_PHASE_CODE,
                       c.BLOCKING_SCOPE_CODE, c.REQUIRED_FLAG, c.RESULT_STATUS_CODE,
                       c.RESULT_REFERENCE, c.CHECKED_AT, c.VALID_UNTIL,
                       c.SOURCE_EVALUATION_REFERENCE, c.WAIVER_REASON
                  FROM %s.DEPOSIT_OPENING_CHECK c
                  JOIN %s.REF_DEP_OPEN_CHECK r ON r.CHECK_CODE=c.CHECK_CODE
                 WHERE c.OPENING_REQUEST_ID=:openingRequestId
                   AND c.CHECK_PHASE_CODE IN ('PRE_ACTIVATION','POST_ACTIVATION')
                   AND c.ATTEMPT_NO=(
                       SELECT MAX(x.ATTEMPT_NO)
                         FROM %s.DEPOSIT_OPENING_CHECK x
                        WHERE x.OPENING_REQUEST_ID=c.OPENING_REQUEST_ID
                          AND x.CHECK_CODE=c.CHECK_CODE
                   )
                 ORDER BY r.EXECUTION_ORDER, r.DISPLAY_ORDER, c.CHECK_CODE
                """.formatted(schema, schema, schema);
        return jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> new ReadinessCheckView(
                        rs.getString("CHECK_CODE"), rs.getString("TITLE_FA"), rs.getString("CHECK_TYPE_CODE"),
                        rs.getString("CHECK_PHASE_CODE"), rs.getString("BLOCKING_SCOPE_CODE"),
                        rs.getInt("REQUIRED_FLAG") == 1, rs.getString("RESULT_STATUS_CODE"),
                        rs.getString("RESULT_REFERENCE"), toOffsetDateTime(rs.getTimestamp("CHECKED_AT")),
                        toOffsetDateTime(rs.getTimestamp("VALID_UNTIL")),
                        rs.getString("SOURCE_EVALUATION_REFERENCE"), rs.getString("WAIVER_REASON")
                ));
    }

    public String jointBasisCode(long openingRequestId) {
        String sql = "SELECT JOINT_ACCOUNT_BASIS_CODE FROM " + schema + ".DEPOSIT_OPENING_REQUEST WHERE OPENING_REQUEST_ID=:openingRequestId";
        List<String> values = jdbc.query(sql,
                new MapSqlParameterSource().addValue("openingRequestId", openingRequestId, Types.NUMERIC),
                (rs, rowNum) -> rs.getString(1));
        return values.stream().findFirst().orElse(null);
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static String requireIdentifier(String raw) {
        if (raw == null) throw new IllegalArgumentException("Oracle identifier is required.");
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_$#]{0,127}")) {
            throw new IllegalArgumentException("Invalid Oracle identifier: " + raw);
        }
        return normalized;
    }

    public record FundingRow(
            long fundingId,
            String fundingMethodCode,
            BigDecimal amount,
            Long sourcePartyId,
            Long sourceAccountId,
            String sourceReference,
            String fundingPurposeCode,
            int sourceOwnershipVerifiedFlag,
            String fundingStatusCode,
            String transactionReference
    ) {
    }

    public record ObligationRow(
            long obligationId,
            String obligationTypeCode,
            BigDecimal finalAmount,
            int mandatoryForActivationFlag,
            String settlementStatusCode,
            String settlementReference
    ) {
    }

    public record CheckDefinition(
            String checkCode,
            String titleFa,
            String checkTypeCode,
            int requiredFlag,
            String checkPhaseCode,
            String blockingScopeCode,
            int recheckRequiredFlag,
            Integer resultValidityMinutes
    ) {
    }

    public record TermReadiness(int termCount, int profitCount, int maturityCount) {
    }
}
