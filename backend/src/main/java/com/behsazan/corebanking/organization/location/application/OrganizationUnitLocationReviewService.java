package com.behsazan.corebanking.organization.location.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Types;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Phase R1: an evidence-preserving, read-only projection of the THREE legacy ORG tables.
 * Every postal address is returned; we deliberately do not guess which address is canonical.
 * No schema migration or new columns are needed to use this endpoint.
 */
@Service
public class OrganizationUnitLocationReviewService {
    private static final Pattern SAFE_SCHEMA = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;

    public OrganizationUnitLocationReviewService(NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.organization:ORG}") String schema) {
        this.jdbc = jdbc;
        String normalized = schema == null ? "ORG" : schema.trim().toUpperCase(Locale.ROOT);
        if (!SAFE_SCHEMA.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid organization Oracle schema name");
        }
        this.schema = normalized;
    }

    public record Summary(long organizationUnitLocationRows, long physicalLocationRows,
                          long postalAddressRows, long servicePointsReferencingLocations,
                          long assignmentsWithoutAddress, long assignmentsWithMultipleAddresses,
                          long locationsWithoutUnitAssignment) {}

    public record ReviewRow(long organizationUnitLocationId, long organizationUnitId,
                            String unitCode, String unitNameFa, long legacyLocationId,
                            String locationRoleCode, String effectiveFrom, String effectiveTo,
                            Long geoEntityId, BigDecimal latitude, BigDecimal longitude,
                            String coordinateReferenceSystemCode, String locationDescriptionFa,
                            Long legacyPostalAddressId, long postalAddressCount,
                            String addressTypeCode, String postalCode, String addressFa,
                            String addressEn, String mainStreetFa, String secondaryStreetFa,
                            String buildingNo) {}

    public record ReviewSnapshot(Summary summary, List<ReviewRow> rows,
                                 String reviewMode, int rowLimit) {}

    @Transactional(readOnly = true)
    public ReviewSnapshot review(Long organizationUnitId, String text, Integer requestedLimit) {
        int limit = requestedLimit == null ? 150 : Math.max(1, Math.min(300, requestedLimit));
        String query = text == null ? "" : text.trim();
        if (query.length() > 100) query = query.substring(0, 100);
        String like = query.isEmpty() ? null : "%" + query.toUpperCase(Locale.ROOT) + "%";
        Summary summary = new Summary(
                count("SELECT COUNT(*) FROM " + schema + ".ORGANIZATION_UNIT_LOCATIONS"),
                count("SELECT COUNT(*) FROM " + schema + ".LOCATIONS"),
                count("SELECT COUNT(*) FROM " + schema + ".POSTAL_ADDRESSES"),
                count("SELECT COUNT(*) FROM " + schema + ".SERVICE_POINTS" +
                        " WHERE LOCATION_ID IS NOT NULL"),
                count("SELECT COUNT(*) FROM " + schema + ".ORGANIZATION_UNIT_LOCATIONS UL" +
                        " WHERE NOT EXISTS (SELECT 1 FROM " + schema + ".POSTAL_ADDRESSES P" +
                        " WHERE P.LOCATION_ID = UL.LOCATION_ID)"),
                count("SELECT COUNT(*) FROM " + schema + ".ORGANIZATION_UNIT_LOCATIONS UL" +
                        " WHERE (SELECT COUNT(*) FROM " + schema + ".POSTAL_ADDRESSES P" +
                        " WHERE P.LOCATION_ID = UL.LOCATION_ID) > 1"),
                count("SELECT COUNT(*) FROM " + schema + ".LOCATIONS L" +
                        " WHERE NOT EXISTS (SELECT 1 FROM " + schema + ".ORGANIZATION_UNIT_LOCATIONS UL" +
                        " WHERE UL.LOCATION_ID = L.LOCATION_ID)"));

        String sql = """
                SELECT UL.ORGANIZATION_UNIT_LOCATION_ID, UL.ORGANIZATION_UNIT_ID,
                       OU.UNIT_CODE, OU.NAME_FA AS UNIT_NAME_FA,
                       UL.LOCATION_ID AS LEGACY_LOCATION_ID, UL.LOCATION_ROLE_CODE,
                       UL.EFFECTIVE_FROM, UL.EFFECTIVE_TO,
                       L.GEO_ENTITY_ID, L.LATITUDE, L.LONGITUDE,
                       L.COORDINATE_REFERENCE_SYSTEM_CODE, L.DESCRIPTION_FA AS LOCATION_DESCRIPTION_FA,
                       P.POSTAL_ADDRESS_ID AS LEGACY_POSTAL_ADDRESS_ID,
                       (SELECT COUNT(*) FROM %1$s.POSTAL_ADDRESSES P2
                         WHERE P2.LOCATION_ID = UL.LOCATION_ID) AS POSTAL_ADDRESS_COUNT,
                       P.ADDRESS_TYPE_CODE, P.POSTAL_CODE, P.ADDRESS_FA, P.ADDRESS_EN,
                       P.MAIN_STREET_FA, P.SECONDARY_STREET_FA, P.BUILDING_NO
                FROM %1$s.ORGANIZATION_UNIT_LOCATIONS UL
                JOIN %1$s.ORGANIZATION_UNITS OU ON OU.ORGANIZATION_UNIT_ID = UL.ORGANIZATION_UNIT_ID
                LEFT JOIN %1$s.LOCATIONS L ON L.LOCATION_ID = UL.LOCATION_ID
                LEFT JOIN %1$s.POSTAL_ADDRESSES P ON P.LOCATION_ID = UL.LOCATION_ID
                WHERE (:organizationUnitId IS NULL OR UL.ORGANIZATION_UNIT_ID = :organizationUnitId)
                  AND (:text IS NULL OR UPPER(OU.UNIT_CODE) LIKE :text OR UPPER(OU.NAME_FA) LIKE :text)
                ORDER BY OU.UNIT_CODE, UL.ORGANIZATION_UNIT_LOCATION_ID, P.POSTAL_ADDRESS_ID
                FETCH FIRST :rowLimit ROWS ONLY
                """.formatted(schema);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationUnitId", organizationUnitId, Types.NUMERIC)
                .addValue("text", like, Types.VARCHAR)
                .addValue("rowLimit", limit, Types.INTEGER);
        List<ReviewRow> rows = jdbc.query(sql, params, (rs, rowNum) -> new ReviewRow(
                rs.getLong("ORGANIZATION_UNIT_LOCATION_ID"),
                rs.getLong("ORGANIZATION_UNIT_ID"),
                rs.getString("UNIT_CODE"), rs.getString("UNIT_NAME_FA"),
                rs.getLong("LEGACY_LOCATION_ID"), rs.getString("LOCATION_ROLE_CODE"),
                dateText(rs, "EFFECTIVE_FROM"), dateText(rs, "EFFECTIVE_TO"),
                nullableLong(rs, "GEO_ENTITY_ID"), rs.getBigDecimal("LATITUDE"),
                rs.getBigDecimal("LONGITUDE"), rs.getString("COORDINATE_REFERENCE_SYSTEM_CODE"),
                rs.getString("LOCATION_DESCRIPTION_FA"), nullableLong(rs, "LEGACY_POSTAL_ADDRESS_ID"),
                rs.getLong("POSTAL_ADDRESS_COUNT"), rs.getString("ADDRESS_TYPE_CODE"),
                rs.getString("POSTAL_CODE"), rs.getString("ADDRESS_FA"),
                rs.getString("ADDRESS_EN"), rs.getString("MAIN_STREET_FA"),
                rs.getString("SECONDARY_STREET_FA"), rs.getString("BUILDING_NO")
        ));
        return new ReviewSnapshot(summary, rows, "LEGACY_READ_ONLY", limit);
    }

    private long count(String sql) {
        Long value = jdbc.getJdbcTemplate().queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private static Long nullableLong(ResultSet rs, String field) throws SQLException {
        long value = rs.getLong(field);
        return rs.wasNull() ? null : value;
    }

    private static String dateText(ResultSet rs, String field) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(field);
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }
}
