package com.behsazan.corebanking.deposit.account.operations.oracle;

import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.AccountSummary;
import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.LifecycleEvent;
import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.OwnerParty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class DepositAccountOperationsRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String accountSchema;
    private final String openingSchema;
    private final String productSchema;

    public DepositAccountOperationsRepository(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.deposit-account:DPS2}") String accountSchema,
            @Value("${core-banking.schemas.deposit-opening:DPS2}") String openingSchema,
            @Value("${core-banking.schemas.product-definition:PDL}") String productSchema
    ) {
        this.jdbc = jdbc;
        this.accountSchema = requireIdentifier(accountSchema);
        this.openingSchema = requireIdentifier(openingSchema);
        this.productSchema = requireIdentifier(productSchema);
    }

    public List<AccountSummary> search(
            String accountNo,
            String status,
            Long openingRequestId,
            Long partyId,
            String productFamilyCode,
            int offset,
            int limit
    ) {
        QueryParts query = queryParts(accountNo, status, openingRequestId, partyId, productFamilyCode);
        String sql = baseSelect() + query.whereClause()
                + " ORDER BY A.ACCOUNT_ID DESC OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
        query.params().addValue("offset", offset).addValue("limit", limit);
        return jdbc.query(sql, query.params(), (rs, rowNum) -> mapSummary(rs));
    }

    public long count(
            String accountNo,
            String status,
            Long openingRequestId,
            Long partyId,
            String productFamilyCode
    ) {
        QueryParts query = queryParts(accountNo, status, openingRequestId, partyId, productFamilyCode);
        String sql = "SELECT COUNT(*) FROM " + accountSchema + ".DEPOSIT_ACCOUNT A "
                + "JOIN " + openingSchema + ".DEPOSIT_OPENING_REQUEST R ON R.OPENING_REQUEST_ID=A.OPENING_REQUEST_ID "
                + "LEFT JOIN " + productSchema + ".PRODUCT_VERSION PV ON PV.PRODUCT_VERSION_ID=A.PRODUCT_VERSION_ID "
                + "LEFT JOIN " + productSchema + ".PRODUCT P ON P.PRODUCT_ID=PV.PRODUCT_ID "
                + query.whereClause();
        Long value = jdbc.queryForObject(sql, query.params(), Long.class);
        return value == null ? 0L : value;
    }

    public Optional<AccountSummary> find(long accountId) {
        String sql = baseSelect() + " WHERE A.ACCOUNT_ID=:accountId";
        List<AccountSummary> rows = jdbc.query(sql, new MapSqlParameterSource("accountId", accountId),
                (rs, rowNum) -> mapSummary(rs));
        return rows.stream().findFirst();
    }

    public List<OwnerParty> owners(long openingRequestId) {
        String sql = """
                SELECT PARTY_ID, ROLE_CODE, IS_PRIMARY, OWNERSHIP_PERCENT, SEQUENCE_NO
                  FROM %s.DEPOSIT_OPENING_PARTY
                 WHERE OPENING_REQUEST_ID=:openingRequestId
                 ORDER BY IS_PRIMARY DESC, SEQUENCE_NO, OPENING_PARTY_ID
                """.formatted(openingSchema);
        return jdbc.query(sql, new MapSqlParameterSource("openingRequestId", openingRequestId), (rs, rowNum) ->
                new OwnerParty(
                        rs.getLong("PARTY_ID"),
                        rs.getString("ROLE_CODE"),
                        rs.getInt("IS_PRIMARY") == 1,
                        rs.getBigDecimal("OWNERSHIP_PERCENT"),
                        rs.getInt("SEQUENCE_NO")
                ));
    }

    public List<LifecycleEvent> events(long accountId) {
        String sql = """
                SELECT LIFECYCLE_EVENT_ID, EVENT_TYPE_CODE, FROM_STATUS_CODE, TO_STATUS_CODE,
                       CORRELATION_ID, EVENT_AT, EVENT_BY
                  FROM %s.DEPOSIT_ACCOUNT_LIFECYCLE_EVENT
                 WHERE ACCOUNT_ID=:accountId
                 ORDER BY EVENT_AT, LIFECYCLE_EVENT_ID
                """.formatted(accountSchema);
        return jdbc.query(sql, new MapSqlParameterSource("accountId", accountId), (rs, rowNum) ->
                new LifecycleEvent(
                        rs.getLong("LIFECYCLE_EVENT_ID"),
                        rs.getString("EVENT_TYPE_CODE"),
                        rs.getString("FROM_STATUS_CODE"),
                        rs.getString("TO_STATUS_CODE"),
                        rs.getString("CORRELATION_ID"),
                        toOffsetDateTime(rs.getTimestamp("EVENT_AT")),
                        rs.getString("EVENT_BY")
                ));
    }

    private String baseSelect() {
        return "SELECT A.ACCOUNT_ID, A.ACCOUNT_NO, A.OPENING_REQUEST_ID, R.REQUEST_NO, "
                + "(SELECT MAX(OP.PARTY_ID) KEEP (DENSE_RANK FIRST ORDER BY OP.IS_PRIMARY DESC, OP.SEQUENCE_NO, OP.OPENING_PARTY_ID) "
                + "   FROM " + openingSchema + ".DEPOSIT_OPENING_PARTY OP WHERE OP.OPENING_REQUEST_ID=A.OPENING_REQUEST_ID) PRIMARY_PARTY_ID, "
                + "A.PRODUCT_VERSION_ID, P.PRODUCT_FAMILY_CODE, A.CURRENCY_CODE, A.OPENING_AMOUNT, "
                + "A.ACCOUNT_STATUS_CODE, A.CREATED_AT, A.ACTIVATED_AT, A.RECORD_VERSION "
                + "FROM " + accountSchema + ".DEPOSIT_ACCOUNT A "
                + "JOIN " + openingSchema + ".DEPOSIT_OPENING_REQUEST R ON R.OPENING_REQUEST_ID=A.OPENING_REQUEST_ID "
                + "LEFT JOIN " + productSchema + ".PRODUCT_VERSION PV ON PV.PRODUCT_VERSION_ID=A.PRODUCT_VERSION_ID "
                + "LEFT JOIN " + productSchema + ".PRODUCT P ON P.PRODUCT_ID=PV.PRODUCT_ID ";
    }

    private QueryParts queryParts(
            String accountNo,
            String status,
            Long openingRequestId,
            Long partyId,
            String productFamilyCode
    ) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (accountNo != null && !accountNo.isBlank()) {
            where.append(" AND UPPER(A.ACCOUNT_NO) LIKE :accountNo");
            params.addValue("accountNo", "%" + accountNo.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND A.ACCOUNT_STATUS_CODE=:status");
            params.addValue("status", status.trim().toUpperCase(Locale.ROOT));
        }
        if (openingRequestId != null) {
            where.append(" AND A.OPENING_REQUEST_ID=:openingRequestId");
            params.addValue("openingRequestId", openingRequestId);
        }
        if (partyId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM ").append(openingSchema)
                    .append(".DEPOSIT_OPENING_PARTY OPF WHERE OPF.OPENING_REQUEST_ID=A.OPENING_REQUEST_ID AND OPF.PARTY_ID=:partyId)");
            params.addValue("partyId", partyId);
        }
        if (productFamilyCode != null && !productFamilyCode.isBlank()) {
            where.append(" AND P.PRODUCT_FAMILY_CODE=:productFamilyCode");
            params.addValue("productFamilyCode", productFamilyCode.trim().toUpperCase(Locale.ROOT));
        }
        return new QueryParts(where.toString(), params);
    }

    private static AccountSummary mapSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AccountSummary(
                rs.getLong("ACCOUNT_ID"),
                rs.getString("ACCOUNT_NO"),
                rs.getLong("OPENING_REQUEST_ID"),
                rs.getString("REQUEST_NO"),
                rs.getObject("PRIMARY_PARTY_ID", Long.class),
                rs.getObject("PRODUCT_VERSION_ID", Long.class),
                rs.getString("PRODUCT_FAMILY_CODE"),
                rs.getString("CURRENCY_CODE"),
                rs.getBigDecimal("OPENING_AMOUNT"),
                rs.getString("ACCOUNT_STATUS_CODE"),
                toOffsetDateTime(rs.getTimestamp("CREATED_AT")),
                toOffsetDateTime(rs.getTimestamp("ACTIVATED_AT")),
                rs.getLong("RECORD_VERSION")
        );
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

    private record QueryParts(String whereClause, MapSqlParameterSource params) {
    }
}
