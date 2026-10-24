package com.behsazan.corebanking.organization.location.map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Read-only, bounded, real-Oracle branch/organizational-unit distribution for the Iran map. */
@Service
public class OrganizationBranchMapService {
    private final NamedParameterJdbcTemplate jdbc;
    public OrganizationBranchMapService(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    public record GeoOption(long id, String code, String name) {}
    public record MapPoint(long locationId, long organizationUnitId, String unitCode,
                           String unitNameFa, String unitTypeCode, String unitTypeNameFa,
                           String addressFa, String postalCode, BigDecimal latitude, BigDecimal longitude,
                           Long provinceId, String provinceName, Long countyId, String countyName,
                           Long cityId, String cityName, Long ruralDistrictId, String ruralDistrictName) {}
    public record MapResult(long totalUnits, long withCoordinates, long withoutCoordinates,
                            boolean truncated, List<MapPoint> points) {}

    @Transactional(readOnly=true)
    public List<GeoOption> options(String level, Long provinceId, Long countyId) {
        if (!Set.of("province","county","city","ruralDistrict","unitType").contains(level))
            throw new IllegalArgumentException("سطح جغرافیایی معتبر نیست.");
        if ("county".equals(level) && invalid(provinceId)) return List.of();
        if (("city".equals(level) || "ruralDistrict".equals(level)) && invalid(countyId)) return List.of();
        var p = new MapSqlParameterSource().addValue("provinceId",provinceId)
                .addValue("countyId",countyId);
        String sql = switch(level) {
            case "unitType" -> """
                SELECT ORGANIZATION_UNIT_TYPE_ID AS ID, TYPE_CODE AS CODE, NAME_FA
                  FROM ORG.ORGANIZATION_UNIT_TYPES ORDER BY NAME_FA, TYPE_CODE
                """;
            case "province" -> """
                SELECT PROVINCE_ID AS ID, PROVINCE_CODE AS CODE, PROVINCE_NAME AS NAME_FA
                  FROM GEO.PROVINCES WHERE IS_ACTIVE=1 AND GEO_TYPE=1 ORDER BY PROVINCE_NAME
                """;
            case "county" -> """
                SELECT COUNTY_ID AS ID, COUNTY_CODE AS CODE, COUNTY_NAME AS NAME_FA
                  FROM GEO.COUNTIES WHERE IS_ACTIVE=1 AND PROVINCE_ID=:provinceId ORDER BY COUNTY_NAME
                """;
            case "city" -> """
                SELECT c.CITY_ID AS ID, c.CITY_CODE AS CODE, c.CITY_NAME AS NAME_FA
                  FROM GEO.CITIES c JOIN GEO.DISTRICTS d ON d.DISTRICT_ID=c.DISTRICT_ID
                 WHERE c.IS_ACTIVE=1 AND d.COUNTY_ID=:countyId ORDER BY c.CITY_NAME
                """;
            default -> """
                SELECT r.RURAL_DISTRICT_ID AS ID, r.RURAL_DISTRICT_CODE AS CODE,
                       r.RURAL_DISTRICT_NAME AS NAME_FA
                  FROM GEO.RURAL_DISTRICTS r JOIN GEO.DISTRICTS d ON d.DISTRICT_ID=r.DISTRICT_ID
                 WHERE r.IS_ACTIVE=1 AND d.COUNTY_ID=:countyId ORDER BY r.RURAL_DISTRICT_NAME
                """;
        };
        return jdbc.query(sql,p,(rs,i)->new GeoOption(rs.getLong("ID"),rs.getString("CODE"),rs.getString("NAME_FA")));
    }

    private static boolean invalid(Long value) { return value==null || value<=0; }

    private static final String BASE = """
        WITH CURRENT_LOCATIONS AS (
           SELECT l.ORGANIZATION_UNIT_LOCATION_ID, l.ORGANIZATION_UNIT_ID, l.GEO_ENTITY_ID,
                  l.LATITUDE, l.LONGITUDE, l.ADDRESS_FA, l.POSTAL_CODE,
                  ROW_NUMBER() OVER (PARTITION BY l.ORGANIZATION_UNIT_ID
                      ORDER BY CASE WHEN l.LOCATION_ROLE_CODE='PRIMARY' THEN 0 ELSE 1 END,
                      l.EFFECTIVE_FROM DESC NULLS LAST, l.ORGANIZATION_UNIT_LOCATION_ID DESC) AS RN
             FROM ORG.ORGANIZATION_UNIT_LOCATIONS l
            WHERE (l.EFFECTIVE_FROM IS NULL OR l.EFFECTIVE_FROM <= TRUNC(SYSDATE))
              AND (l.EFFECTIVE_TO IS NULL OR l.EFFECTIVE_TO >= TRUNC(SYSDATE))
        ), ENRICHED AS (
          SELECT l.ORGANIZATION_UNIT_LOCATION_ID, l.ORGANIZATION_UNIT_ID,
                 u.UNIT_CODE, u.NAME_FA AS UNIT_NAME_FA,
                 t.TYPE_CODE AS UNIT_TYPE_CODE, t.NAME_FA AS UNIT_TYPE_NAME_FA,
                 l.LATITUDE,l.LONGITUDE,l.ADDRESS_FA,l.POSTAL_CODE,
                 p.PROVINCE_ID,p.PROVINCE_NAME,co.COUNTY_ID,co.COUNTY_NAME,
                 c.CITY_ID,c.CITY_NAME,r.RURAL_DISTRICT_ID,r.RURAL_DISTRICT_NAME
            FROM CURRENT_LOCATIONS l
            JOIN ORG.ORGANIZATION_UNITS u ON u.ORGANIZATION_UNIT_ID=l.ORGANIZATION_UNIT_ID
            JOIN ORG.ORGANIZATION_UNIT_TYPES t ON t.ORGANIZATION_UNIT_TYPE_ID=u.ORGANIZATION_UNIT_TYPE_ID
            LEFT JOIN GEO.CITIES c ON c.CITY_ID=l.GEO_ENTITY_ID AND c.IS_ACTIVE=1
            LEFT JOIN GEO.RURAL_DISTRICTS r ON r.RURAL_DISTRICT_ID=l.GEO_ENTITY_ID
                            AND r.IS_ACTIVE=1 AND c.CITY_ID IS NULL
            LEFT JOIN GEO.DISTRICTS d ON d.DISTRICT_ID=COALESCE(c.DISTRICT_ID,r.DISTRICT_ID)
            LEFT JOIN GEO.COUNTIES co ON co.COUNTY_ID=d.COUNTY_ID
            LEFT JOIN GEO.PROVINCES p ON p.PROVINCE_ID=co.PROVINCE_ID
           WHERE l.RN=1
        )
        """;
    private static final String FILTER = """
           WHERE (:provinceId IS NULL OR PROVINCE_ID=:provinceId)
             AND (:countyId IS NULL OR COUNTY_ID=:countyId)
             AND (:cityId IS NULL OR CITY_ID=:cityId)
             AND (:ruralDistrictId IS NULL OR RURAL_DISTRICT_ID=:ruralDistrictId)
             AND (:typeCode IS NULL OR UNIT_TYPE_CODE=:typeCode)
             AND (:text IS NULL OR UPPER(UNIT_CODE) LIKE :text OR UPPER(UNIT_NAME_FA) LIKE :text)
        """;
    private static final int MAX_ROWS=6000;

    @Transactional(readOnly=true)
    public MapResult points(Long provinceId,Long countyId,Long cityId,Long ruralDistrictId,
                            String typeCode,String text) {
        String t=text==null?"":text.strip();
        if(t.length()>100) t=t.substring(0,100);
        String like=t.isBlank()?null:"%"+t.toUpperCase(Locale.ROOT)+"%";
        MapSqlParameterSource p=new MapSqlParameterSource()
                .addValue("provinceId",provinceId,java.sql.Types.NUMERIC)
                .addValue("countyId",countyId,java.sql.Types.NUMERIC)
                .addValue("cityId",cityId,java.sql.Types.NUMERIC)
                .addValue("ruralDistrictId",ruralDistrictId,java.sql.Types.NUMERIC)
                .addValue("typeCode",typeCode==null||typeCode.isBlank()?null:typeCode,java.sql.Types.VARCHAR)
                .addValue("text",like,java.sql.Types.VARCHAR).addValue("maxRows",MAX_ROWS);
        String countSql=BASE+"SELECT COUNT(*) AS TOTAL_COUNT, "+
                "NVL(SUM(CASE WHEN LATITUDE BETWEEN 24 AND 41 AND LONGITUDE BETWEEN 44 AND 64 THEN 1 ELSE 0 END),0) AS HAS_COORD " +
                "FROM ENRICHED " + FILTER;
        long[] counts=jdbc.queryForObject(countSql,p,(rs,i)->new long[]{rs.getLong("TOTAL_COUNT"),rs.getLong("HAS_COORD")});
        if(counts==null) counts=new long[]{0,0};
        String sql=BASE+"""
                SELECT ORGANIZATION_UNIT_LOCATION_ID, ORGANIZATION_UNIT_ID, UNIT_CODE,UNIT_NAME_FA,
                       UNIT_TYPE_CODE,UNIT_TYPE_NAME_FA,ADDRESS_FA,POSTAL_CODE,LATITUDE,LONGITUDE,
                       PROVINCE_ID,PROVINCE_NAME,COUNTY_ID,COUNTY_NAME,CITY_ID,CITY_NAME,
                       RURAL_DISTRICT_ID,RURAL_DISTRICT_NAME
                  FROM ENRICHED
                """+FILTER+"""
                  AND LATITUDE BETWEEN 24 AND 41 AND LONGITUDE BETWEEN 44 AND 64
                  ORDER BY UNIT_NAME_FA,ORGANIZATION_UNIT_ID
                  FETCH FIRST :maxRows ROWS ONLY
                """;
        List<MapPoint> points=jdbc.query(sql,p,(rs,i)->new MapPoint(
                rs.getLong("ORGANIZATION_UNIT_LOCATION_ID"),rs.getLong("ORGANIZATION_UNIT_ID"),
                rs.getString("UNIT_CODE"),rs.getString("UNIT_NAME_FA"),rs.getString("UNIT_TYPE_CODE"),
                rs.getString("UNIT_TYPE_NAME_FA"),rs.getString("ADDRESS_FA"),rs.getString("POSTAL_CODE"),
                rs.getBigDecimal("LATITUDE"),rs.getBigDecimal("LONGITUDE"),
                nullable(rs,"PROVINCE_ID"),rs.getString("PROVINCE_NAME"),
                nullable(rs,"COUNTY_ID"),rs.getString("COUNTY_NAME"),
                nullable(rs,"CITY_ID"),rs.getString("CITY_NAME"),
                nullable(rs,"RURAL_DISTRICT_ID"),rs.getString("RURAL_DISTRICT_NAME")));
        return new MapResult(counts[0],counts[1],counts[0]-counts[1],counts[1]>MAX_ROWS,points);
    }
    private static Long nullable(java.sql.ResultSet rs,String name) throws java.sql.SQLException {
        long value=rs.getLong(name);return rs.wasNull()?null:value;
    }
}
