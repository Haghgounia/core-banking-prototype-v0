package com.behsazan.corebanking.organization.location.manage;

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
 * Operational ORG location list. The only location/address source is
 * ORG.ORGANIZATION_UNIT_LOCATIONS; ORGANIZATION_UNITS supplies unit labels.
 * Historical source reconciliation remains in a separate, explicitly legacy
 * diagnostic endpoint. No Oracle DML or legacy-table lookup occurs here.
 */
@Service
public class OrganizationUnitLocationBrowseService {
    private static final Pattern SAFE_SCHEMA=Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private final NamedParameterJdbcTemplate jdbc;
    private final String schema;

    public OrganizationUnitLocationBrowseService(NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.schemas.organization:ORG}") String schema) {
        this.jdbc=jdbc;
        String normalized=schema==null?"ORG":schema.trim().toUpperCase(Locale.ROOT);
        if (!SAFE_SCHEMA.matcher(normalized).matches())
            throw new IllegalArgumentException("Invalid ORG schema name");
        this.schema=normalized;
    }

    public record BrowseSummary(long unitLocationRows,long backfilledSnapshotRows,
                                long managedCanonicalRows,long unbackfilledRows) {}
    public record BrowseRow(long organizationUnitLocationId,long organizationUnitId,
                            String unitCode,String unitNameFa,String locationRoleCode,
                            String effectiveFrom,String effectiveTo,Long geoEntityId,
                            BigDecimal latitude,BigDecimal longitude,String postalCode,
                            String addressFa,String addressEn,String unitPresenceCode,
                            String addressUsageCode,long rowVersion,Long legacyLocationId,
                            Long legacyPostalAddressId,String sourceStatus) {}
    public record BrowseSnapshot(BrowseSummary summary,List<BrowseRow> rows,
                                 String reviewMode,int rowLimit,int page,long filteredRows) {}

    @Transactional(readOnly=true)
    public BrowseSnapshot browse(Long unitId,String text,Integer requestedLimit,Integer requestedPage) {
        int limit=requestedLimit==null?150:Math.max(1,Math.min(300,requestedLimit));
        int page=requestedPage==null?0:Math.max(0,Math.min(10000,requestedPage));
        String search=text==null?"":text.trim();
        if(search.length()>100) search=search.substring(0,100);
        String term=search.isBlank()?null:"%"+search.toUpperCase(Locale.ROOT)+"%";
        boolean managed=managedColumnsReady();
        String managedExpr=managed?"UL.CANONICAL_MANAGED_FLAG":"0";
        String cte=canonicalCte(schema,managed);

        BrowseSummary summary=jdbc.queryForObject("""
                SELECT COUNT(*) AS TOTAL_ROWS,
                       NVL(SUM(CASE WHEN LEGACY_BACKFILL_FLAG=1 THEN 1 ELSE 0 END),0) AS BACKFILLED_ROWS,
                       NVL(SUM(CASE WHEN %s=1 THEN 1 ELSE 0 END),0) AS MANAGED_ROWS,
                       NVL(SUM(CASE WHEN LEGACY_BACKFILL_FLAG<>1 THEN 1 ELSE 0 END),0) AS UNBACKFILLED_ROWS
                  FROM %s.ORGANIZATION_UNIT_LOCATIONS UL
                """.formatted(managedExpr,schema),new MapSqlParameterSource(),(rs,i)->new BrowseSummary(
                rs.getLong("TOTAL_ROWS"),rs.getLong("BACKFILLED_ROWS"),
                rs.getLong("MANAGED_ROWS"),rs.getLong("UNBACKFILLED_ROWS")));
        if(summary==null) throw new IllegalStateException("Cannot count canonical ORG locations");

        MapSqlParameterSource params=new MapSqlParameterSource()
                .addValue("unitId",unitId,java.sql.Types.NUMERIC)
                .addValue("term",term,java.sql.Types.VARCHAR)
                .addValue("limit",limit,java.sql.Types.INTEGER)
                .addValue("offset",page*limit,java.sql.Types.INTEGER);
        String filter="""
                 WHERE (:unitId IS NULL OR ORGANIZATION_UNIT_ID=:unitId)
                   AND (:term IS NULL OR UPPER(UNIT_CODE) LIKE :term OR UPPER(UNIT_NAME_FA) LIKE :term)
                """;
        Long filtered=jdbc.queryForObject(cte+"SELECT COUNT(*) FROM CANONICAL_ROWS"+filter,
                params,Long.class);
        List<BrowseRow> rows=jdbc.query(cte+"""
                SELECT ORGANIZATION_UNIT_LOCATION_ID, ORGANIZATION_UNIT_ID, UNIT_CODE, UNIT_NAME_FA,
                       LOCATION_ROLE_CODE, EFFECTIVE_FROM, EFFECTIVE_TO,
                       GEO_ENTITY_ID, LATITUDE, LONGITUDE, POSTAL_CODE, ADDRESS_FA, ADDRESS_EN,
                       UNIT_PRESENCE_CODE, ADDRESS_USAGE_CODE, ROW_VERSION,
                       LEGACY_LOCATION_ID, LEGACY_POSTAL_ADDRESS_ID, SOURCE_STATUS
                  FROM CANONICAL_ROWS
                """+filter+"""
                 ORDER BY UNIT_CODE,ORGANIZATION_UNIT_LOCATION_ID
                 OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """,params,(rs,i)->readRow(rs));
        return new BrowseSnapshot(summary,rows,"CANONICAL_SINGLE_TABLE",limit,page,
                filtered==null?0L:filtered);
    }

    private boolean managedColumnsReady(){
        Long count=jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER=:schema AND TABLE_NAME='ORGANIZATION_UNIT_LOCATIONS'
                   AND COLUMN_NAME IN ('CANONICAL_MANAGED_FLAG','UNIT_PRESENCE_CODE','ADDRESS_USAGE_CODE')
                """,new MapSqlParameterSource("schema",schema),Long.class);
        // R1-only Oracle remains readable before the opt-in cutover; management
        // stays blocked independently by OrganizationUnitLocationService.readiness().
        return count!=null && count==3;
    }

    static String canonicalCte(String schema,boolean managedColumnsReady){
        String managed=managedColumnsReady?"UL.CANONICAL_MANAGED_FLAG":"0";
        String presence=managedColumnsReady?"UL.UNIT_PRESENCE_CODE":"CAST(NULL AS VARCHAR2(20))";
        String usage=managedColumnsReady?"UL.ADDRESS_USAGE_CODE":"CAST(NULL AS VARCHAR2(20))";
        return """
                WITH CANONICAL_ROWS AS (
                  SELECT UL.ORGANIZATION_UNIT_LOCATION_ID,UL.ORGANIZATION_UNIT_ID,
                         OU.UNIT_CODE,OU.NAME_FA AS UNIT_NAME_FA,UL.LOCATION_ROLE_CODE,
                         UL.EFFECTIVE_FROM,UL.EFFECTIVE_TO,UL.GEO_ENTITY_ID,
                         UL.LATITUDE,UL.LONGITUDE,UL.POSTAL_CODE,UL.ADDRESS_FA,UL.ADDRESS_EN,
                         %s AS UNIT_PRESENCE_CODE,%s AS ADDRESS_USAGE_CODE,
                         UL.ROW_VERSION,UL.LOCATION_ID AS LEGACY_LOCATION_ID,
                         UL.LEGACY_POSTAL_ADDRESS_ID,
                         CASE WHEN %s=1 AND UL.LOCATION_ID IS NULL THEN 'NATIVE_RECORD'
                              WHEN %s=1 THEN 'CANONICAL_EDITED'
                              WHEN UL.LEGACY_BACKFILL_FLAG=1 THEN 'BACKFILLED_SNAPSHOT'
                              ELSE 'NOT_BACKFILLED' END AS SOURCE_STATUS
                    FROM %s.ORGANIZATION_UNIT_LOCATIONS UL
                    JOIN %s.ORGANIZATION_UNITS OU ON OU.ORGANIZATION_UNIT_ID=UL.ORGANIZATION_UNIT_ID
                )
                """.formatted(presence,usage,managed,managed,schema,schema);
    }

    private static BrowseRow readRow(ResultSet rs) throws SQLException{
        return new BrowseRow(rs.getLong("ORGANIZATION_UNIT_LOCATION_ID"),
                rs.getLong("ORGANIZATION_UNIT_ID"),rs.getString("UNIT_CODE"),rs.getString("UNIT_NAME_FA"),
                rs.getString("LOCATION_ROLE_CODE"),dateText(rs,"EFFECTIVE_FROM"),dateText(rs,"EFFECTIVE_TO"),
                nullableLong(rs,"GEO_ENTITY_ID"),rs.getBigDecimal("LATITUDE"),rs.getBigDecimal("LONGITUDE"),
                rs.getString("POSTAL_CODE"),rs.getString("ADDRESS_FA"),rs.getString("ADDRESS_EN"),
                rs.getString("UNIT_PRESENCE_CODE"),rs.getString("ADDRESS_USAGE_CODE"),rs.getLong("ROW_VERSION"),
                nullableLong(rs,"LEGACY_LOCATION_ID"),nullableLong(rs,"LEGACY_POSTAL_ADDRESS_ID"),
                rs.getString("SOURCE_STATUS"));
    }
    private static Long nullableLong(ResultSet rs,String column) throws SQLException{
        long value=rs.getLong(column);return rs.wasNull()?null:value;
    }
    private static String dateText(ResultSet rs,String column) throws SQLException{
        java.sql.Timestamp value=rs.getTimestamp(column);
        return value==null?null:value.toLocalDateTime().toString();
    }
}
