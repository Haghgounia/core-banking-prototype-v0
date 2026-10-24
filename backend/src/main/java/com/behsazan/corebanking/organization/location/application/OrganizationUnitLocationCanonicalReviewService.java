package com.behsazan.corebanking.organization.location.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * R2-A: read-only canonical snapshot review AFTER the verified R1 backfill.
 * The stored unit-location row is the display source; legacy tables are read only
 * to flag subsequent drift. No writes or assumed physical/virtual classification.
 * Requires all 26 R1 additive columns to exist in the ORG schema.
 */
@Service
public class OrganizationUnitLocationCanonicalReviewService {
    private static final Pattern SAFE_SCHEMA = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;

    public OrganizationUnitLocationCanonicalReviewService(NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.organization:ORG}") String schema) {
        this.jdbc = jdbc;
        String normalized = schema == null ? "ORG" : schema.trim().toUpperCase(Locale.ROOT);
        if (!SAFE_SCHEMA.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid ORG schema name");
        }
        this.schema = normalized;
    }

    public record Summary(long unitLocationRows, long verifiedSnapshots,
                          long unbackfilledRows, long sourceDriftRows,
                          long missingLegacySources, long unlinkedLegacyAddresses,
                          long ambiguousLegacyAddresses, long openPrimaryDuplicateUnitGroups) {}

    public record Row(long organizationUnitLocationId, long organizationUnitId,
                      String unitCode, String unitNameFa, Long legacyLocationId,
                      String locationRoleCode, String effectiveFrom, String effectiveTo,
                      Long geoEntityId, BigDecimal latitude, BigDecimal longitude,
                      String addressTypeCode, String postalCode, String addressFa,
                      String addressEn, Long legacyPostalAddressId,
                      int legacyBackfillFlag, long rowVersion, String sourceStatus) {}

    public record Snapshot(Summary summary, List<Row> rows, String reviewMode,
                           int rowLimit, int page, long filteredRows) {}

    @Transactional(readOnly = true)
    public Snapshot review(Long organizationUnitId, String text, Integer requestedLimit, Integer requestedPage) {
        int limit = requestedLimit == null ? 150 : Math.max(1, Math.min(300, requestedLimit));
        int page = requestedPage == null ? 0 : Math.max(0, Math.min(10000, requestedPage));
        String query = text == null ? "" : text.trim();
        if (query.length() > 100) query = query.substring(0, 100);
        String like = query.isEmpty() ? null : "%" + query.toUpperCase(Locale.ROOT) + "%";

        String cte = canonicalCte();
        String summarySql = cte + """
                SELECT COUNT(*) AS TOTAL_ROWS,
                       NVL(SUM(CASE WHEN SOURCE_STATUS='MATCH' THEN 1 ELSE 0 END),0) AS VERIFIED_ROWS,
                       NVL(SUM(CASE WHEN SOURCE_STATUS='NOT_BACKFILLED' THEN 1 ELSE 0 END),0) AS UNBACKFILLED_ROWS,
                       NVL(SUM(CASE WHEN SOURCE_STATUS='DRIFT' THEN 1 ELSE 0 END),0) AS DRIFT_ROWS,
                       NVL(SUM(CASE WHEN SOURCE_STATUS IN ('MISSING_LOCATION','MISSING_ADDRESS') THEN 1 ELSE 0 END),0) AS MISSING_SOURCES,
                       NVL(SUM(CASE WHEN SOURCE_STATUS='UNLINKED_ADDRESS' THEN 1 ELSE 0 END),0) AS UNLINKED_ADDRESSES,
                       NVL(SUM(CASE WHEN SOURCE_STATUS='AMBIGUOUS_ADDRESS' THEN 1 ELSE 0 END),0) AS AMBIGUOUS_ADDRESSES
                  FROM CANONICAL_ROWS
                """;
        Summary partial = jdbc.queryForObject(summarySql, new MapSqlParameterSource(), (rs, rowNum) ->
                new Summary(rs.getLong("TOTAL_ROWS"), rs.getLong("VERIFIED_ROWS"),
                        rs.getLong("UNBACKFILLED_ROWS"), rs.getLong("DRIFT_ROWS"),
                        rs.getLong("MISSING_SOURCES"), rs.getLong("UNLINKED_ADDRESSES"),
                        rs.getLong("AMBIGUOUS_ADDRESSES"), 0));
        if (partial == null) {
            throw new IllegalStateException("Could not read canonical ORG location summary");
        }

        String overlapSql = """
                SELECT COUNT(*) FROM (
                    SELECT ORGANIZATION_UNIT_ID
                      FROM %s.ORGANIZATION_UNIT_LOCATIONS
                     WHERE LOCATION_ROLE_CODE='PRIMARY' AND EFFECTIVE_TO IS NULL
                     GROUP BY ORGANIZATION_UNIT_ID HAVING COUNT(*) > 1
                )
                """.formatted(schema);
        Long duplicateCount = jdbc.getJdbcTemplate().queryForObject(overlapSql, Long.class);
        Summary summary = new Summary(partial.unitLocationRows(), partial.verifiedSnapshots(),
                partial.unbackfilledRows(), partial.sourceDriftRows(),
                partial.missingLegacySources(), partial.unlinkedLegacyAddresses(),
                partial.ambiguousLegacyAddresses(), duplicateCount == null ? 0 : duplicateCount);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationUnitId", organizationUnitId, java.sql.Types.NUMERIC)
                .addValue("text", like, java.sql.Types.VARCHAR)
                .addValue("rowLimit", limit, java.sql.Types.INTEGER)
                .addValue("rowOffset", page * limit, java.sql.Types.INTEGER);
        String filteredSql = cte + """
                SELECT COUNT(*) FROM CANONICAL_ROWS
                 WHERE (:organizationUnitId IS NULL OR ORGANIZATION_UNIT_ID=:organizationUnitId)
                   AND (:text IS NULL OR UPPER(UNIT_CODE) LIKE :text OR UPPER(UNIT_NAME_FA) LIKE :text)
                """;
        Long filteredResult = jdbc.queryForObject(filteredSql, params, (rs, rowNum) -> rs.getLong(1));
        long filteredRows = filteredResult == null ? 0L : filteredResult;

        String sql = cte + """
                SELECT ORGANIZATION_UNIT_LOCATION_ID, ORGANIZATION_UNIT_ID, UNIT_CODE,
                       UNIT_NAME_FA, LEGACY_LOCATION_ID, LOCATION_ROLE_CODE,
                       EFFECTIVE_FROM, EFFECTIVE_TO, GEO_ENTITY_ID, LATITUDE, LONGITUDE,
                       ADDRESS_TYPE_CODE, POSTAL_CODE, ADDRESS_FA, ADDRESS_EN,
                       LEGACY_POSTAL_ADDRESS_ID, LEGACY_BACKFILL_FLAG, ROW_VERSION, SOURCE_STATUS
                  FROM CANONICAL_ROWS
                 WHERE (:organizationUnitId IS NULL OR ORGANIZATION_UNIT_ID=:organizationUnitId)
                   AND (:text IS NULL OR UPPER(UNIT_CODE) LIKE :text OR UPPER(UNIT_NAME_FA) LIKE :text)
                 ORDER BY UNIT_CODE, ORGANIZATION_UNIT_LOCATION_ID
                 OFFSET :rowOffset ROWS FETCH NEXT :rowLimit ROWS ONLY
                """;
        List<Row> rows = jdbc.query(sql, params, (rs, rowNum) -> new Row(
                rs.getLong("ORGANIZATION_UNIT_LOCATION_ID"), rs.getLong("ORGANIZATION_UNIT_ID"),
                rs.getString("UNIT_CODE"), rs.getString("UNIT_NAME_FA"),
                nullableLong(rs, "LEGACY_LOCATION_ID"), rs.getString("LOCATION_ROLE_CODE"),
                dateText(rs, "EFFECTIVE_FROM"), dateText(rs, "EFFECTIVE_TO"),
                nullableLong(rs, "GEO_ENTITY_ID"), rs.getBigDecimal("LATITUDE"),
                rs.getBigDecimal("LONGITUDE"), rs.getString("ADDRESS_TYPE_CODE"),
                rs.getString("POSTAL_CODE"), rs.getString("ADDRESS_FA"),
                rs.getString("ADDRESS_EN"), nullableLong(rs, "LEGACY_POSTAL_ADDRESS_ID"),
                rs.getInt("LEGACY_BACKFILL_FLAG"), rs.getLong("ROW_VERSION"),
                rs.getString("SOURCE_STATUS")));
        return new Snapshot(summary, rows, "CANONICAL_R2A_READ_ONLY", limit, page, filteredRows);
    }

    private String canonicalCte() {
        // DECODE treats two NULL values as equal. Compare all 19 content fields
        // plus source postal-address ID, exactly as the approved R1 verifier did.
        String differences = String.join(" + ",
                "DECODE(UL.GEO_ENTITY_ID,L.GEO_ENTITY_ID,0,1)",
                "DECODE(UL.LATITUDE,L.LATITUDE,0,1)",
                "DECODE(UL.LONGITUDE,L.LONGITUDE,0,1)",
                "DECODE(UL.COORDINATE_REFERENCE_SYSTEM_CODE,L.COORDINATE_REFERENCE_SYSTEM_CODE,0,1)",
                "DECODE(UL.LOCATION_DESCRIPTION_FA,L.DESCRIPTION_FA,0,1)",
                "DECODE(UL.LOCATION_DESCRIPTION_EN,L.DESCRIPTION_EN,0,1)",
                "DECODE(UL.ADDRESS_TYPE_CODE,P.ADDRESS_TYPE_CODE,0,1)",
                "DECODE(UL.MAIN_STREET_FA,P.MAIN_STREET_FA,0,1)",
                "DECODE(UL.MAIN_STREET_EN,P.MAIN_STREET_EN,0,1)",
                "DECODE(UL.SECONDARY_STREET_FA,P.SECONDARY_STREET_FA,0,1)",
                "DECODE(UL.SECONDARY_STREET_EN,P.SECONDARY_STREET_EN,0,1)",
                "DECODE(UL.SQUARE_CROSSROAD_FA,P.SQUARE_CROSSROAD_FA,0,1)",
                "DECODE(UL.SQUARE_CROSSROAD_EN,P.SQUARE_CROSSROAD_EN,0,1)",
                "DECODE(UL.BUILDING_NO,P.BUILDING_NO,0,1)",
                "DECODE(UL.POSTAL_CODE,P.POSTAL_CODE,0,1)",
                "DECODE(UL.ADDRESS_FA,P.ADDRESS_FA,0,1)",
                "DECODE(UL.ADDRESS_EN,P.ADDRESS_EN,0,1)",
                "DECODE(UL.ADDITIONAL_ADDRESS_FA,P.ADDITIONAL_ADDRESS_FA,0,1)",
                "DECODE(UL.ADDITIONAL_ADDRESS_EN,P.ADDITIONAL_ADDRESS_EN,0,1)",
                "DECODE(UL.LEGACY_POSTAL_ADDRESS_ID,P.POSTAL_ADDRESS_ID,0,1)");
        return """
                WITH CANONICAL_ROWS AS (
                    SELECT UL.ORGANIZATION_UNIT_LOCATION_ID, UL.ORGANIZATION_UNIT_ID,
                           OU.UNIT_CODE, OU.NAME_FA AS UNIT_NAME_FA,
                           UL.LOCATION_ID AS LEGACY_LOCATION_ID, UL.LOCATION_ROLE_CODE,
                           UL.EFFECTIVE_FROM, UL.EFFECTIVE_TO,
                           UL.GEO_ENTITY_ID, UL.LATITUDE, UL.LONGITUDE,
                           UL.ADDRESS_TYPE_CODE, UL.POSTAL_CODE, UL.ADDRESS_FA,
                           UL.ADDRESS_EN, UL.LEGACY_POSTAL_ADDRESS_ID,
                           UL.LEGACY_BACKFILL_FLAG, UL.ROW_VERSION,
                           CASE
                             WHEN UL.LEGACY_BACKFILL_FLAG<>1 THEN 'NOT_BACKFILLED'
                             WHEN L.LOCATION_ID IS NULL THEN 'MISSING_LOCATION'
                             WHEN UL.LEGACY_POSTAL_ADDRESS_ID IS NOT NULL
                                  AND P.POSTAL_ADDRESS_ID IS NULL THEN 'MISSING_ADDRESS'
                             WHEN P.POSTAL_ADDRESS_ID IS NOT NULL
                                  AND P.LOCATION_ID<>UL.LOCATION_ID THEN 'UNLINKED_ADDRESS'
                             WHEN UL.LEGACY_POSTAL_ADDRESS_ID IS NULL AND EXISTS (
                                  SELECT 1 FROM %1$s.POSTAL_ADDRESSES P2
                                   WHERE P2.LOCATION_ID=UL.LOCATION_ID) THEN 'UNLINKED_ADDRESS'
                             WHEN (SELECT COUNT(*) FROM %1$s.POSTAL_ADDRESSES P2
                                    WHERE P2.LOCATION_ID=UL.LOCATION_ID)>1 THEN 'AMBIGUOUS_ADDRESS'
                             WHEN (%2$s)<>0 THEN 'DRIFT'
                             ELSE 'MATCH'
                           END AS SOURCE_STATUS
                      FROM %1$s.ORGANIZATION_UNIT_LOCATIONS UL
                      JOIN %1$s.ORGANIZATION_UNITS OU
                        ON OU.ORGANIZATION_UNIT_ID=UL.ORGANIZATION_UNIT_ID
                      LEFT JOIN %1$s.LOCATIONS L ON L.LOCATION_ID=UL.LOCATION_ID
                      LEFT JOIN %1$s.POSTAL_ADDRESSES P
                        ON P.POSTAL_ADDRESS_ID=UL.LEGACY_POSTAL_ADDRESS_ID
                )
                """.formatted(schema, differences);
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static String dateText(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime().toString();
    }
}
