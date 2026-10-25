package com.behsazan.corebanking.organization.location.map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Read-only ORG branch map. GEO IDs are always qualified by their geographic entity type. */
@Service
public class OrganizationBranchMapService {
    private static final Set<String> LEVELS=Set.of("province","county","city","ruralDistrict","unitType");
    private static final Set<String> GEO_TYPES=Set.of("CITY","RURAL_DISTRICT");
    private static final int MAX_ROWS=6000;
    private final NamedParameterJdbcTemplate jdbc;
    public OrganizationBranchMapService(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    public record GeoOption(long id, String code, String name, Long unitCount) {}
    public record GeoContext(String geoEntityTypeCode, long geoEntityId,
                             long provinceId, long countyId, String name) {}
    public record MapPoint(long locationId, long organizationUnitId, String unitCode,
                           String unitNameFa, String unitTypeCode, String unitTypeNameFa,
                           String addressFa, String postalCode, BigDecimal latitude, BigDecimal longitude,
                           Long provinceId, String provinceName, Long countyId, String countyName,
                           Long cityId, String cityName, Long ruralDistrictId, String ruralDistrictName) {}
    public record MapResult(long totalUnits, long withCoordinates, long withoutCoordinates,
                            boolean truncated, List<MapPoint> points) {}

    /** Before opt-in DDL the proven original city mapping remains readable.
     *  An explicitly typed rural-district placement is NEVER interpreted as a city.
     *  Historical LEGACY_BACKFILL_FLAG=1 values originate in the recorded GEO_CITY_ID import contract.
     *  New untyped rows never receive a guessed geographic type.
     */
    static String effectiveGeoType(String alias,boolean typedColumnExists) {
        String legacy="WHEN "+alias+".LEGACY_BACKFILL_FLAG=1 THEN 'CITY' ";
        if (!typedColumnExists) return "CASE "+legacy+"END";
        return "CASE WHEN "+alias+".GEO_ENTITY_TYPE_CODE IN ('CITY','RURAL_DISTRICT') THEN "
                +alias+".GEO_ENTITY_TYPE_CODE "+legacy+"END";
    }

    private boolean geoTypeColumnInstalled() {
        Integer count=jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='ORG'
                 AND TABLE_NAME='ORGANIZATION_UNIT_LOCATIONS' AND COLUMN_NAME='GEO_ENTITY_TYPE_CODE'
                """,new MapSqlParameterSource(),Integer.class);
        return count!=null && count==1;
    }

    private static final String VALID_CURRENT="""
                (ul.EFFECTIVE_FROM IS NULL OR ul.EFFECTIVE_FROM <= TRUNC(SYSDATE))
            AND (ul.EFFECTIVE_TO IS NULL OR ul.EFFECTIVE_TO >= TRUNC(SYSDATE))
            """;

    @Transactional(readOnly=true)
    public List<GeoOption> options(String level, Long provinceId, Long countyId) {
        if(!LEVELS.contains(level))throw new IllegalArgumentException("سطح جغرافیایی معتبر نیست.");
        if("county".equals(level) && invalid(provinceId)) return List.of();
        if(("city".equals(level)||"ruralDistrict".equals(level))&&invalid(countyId))return List.of();
        var p=new MapSqlParameterSource().addValue("provinceId",provinceId,Types.NUMERIC)
                .addValue("countyId",countyId,Types.NUMERIC);
        String sql;
        if("city".equals(level)||"ruralDistrict".equals(level)) {
            // A rural district and a city may have the same numeric identifier in different GEO tables.
            String table="city".equals(level)?"GEO.CITIES":"GEO.RURAL_DISTRICTS";
            String key="city".equals(level)?"CITY":"RURAL_DISTRICT";
            String prefix="city".equals(level)?"c":"r";
            String kind="city".equals(level)?"CITY":"RURAL_DISTRICT";
            String typeExpr=effectiveGeoType("ul",geoTypeColumnInstalled());
            sql="SELECT "+prefix+"."+key+"_ID AS ID, "+prefix+"."+key+"_CODE AS CODE, "
              +prefix+"."+key+"_NAME AS NAME_FA, "
              +"COUNT(DISTINCT CASE WHEN u.STATUS_CODE='ACTIVE' THEN ul.ORGANIZATION_UNIT_ID END) AS UNIT_COUNT "
              +"FROM "+table+" "+prefix+" JOIN GEO.DISTRICTS d ON d.DISTRICT_ID="+prefix+".DISTRICT_ID "
              +"LEFT JOIN ORG.ORGANIZATION_UNIT_LOCATIONS ul ON ul.GEO_ENTITY_ID="+prefix+"."+key+"_ID "
              +"AND ("+typeExpr+")='"+kind+"' AND "+VALID_CURRENT+" "
              +"LEFT JOIN ORG.ORGANIZATION_UNITS u ON u.ORGANIZATION_UNIT_ID=ul.ORGANIZATION_UNIT_ID "
              +"WHERE "+prefix+".IS_ACTIVE=1 AND d.COUNTY_ID=:countyId "
              +"GROUP BY "+prefix+"."+key+"_ID,"+prefix+"."+key+"_CODE,"+prefix+"."+key+"_NAME "
              +"ORDER BY "+prefix+"."+key+"_NAME";
        } else {
            sql=switch(level) {
                case "unitType" -> """
                    SELECT ORGANIZATION_UNIT_TYPE_ID AS ID, TYPE_CODE AS CODE, NAME_FA,
                           CAST(NULL AS NUMBER) AS UNIT_COUNT
                      FROM ORG.ORGANIZATION_UNIT_TYPES ORDER BY NAME_FA, TYPE_CODE
                    """;
                case "province" -> """
                    SELECT PROVINCE_ID AS ID, PROVINCE_CODE AS CODE, PROVINCE_NAME AS NAME_FA,
                           CAST(NULL AS NUMBER) AS UNIT_COUNT
                      FROM GEO.PROVINCES WHERE IS_ACTIVE=1 AND GEO_TYPE=1 ORDER BY PROVINCE_NAME
                    """;
                default -> """
                    SELECT COUNTY_ID AS ID, COUNTY_CODE AS CODE, COUNTY_NAME AS NAME_FA,
                           CAST(NULL AS NUMBER) AS UNIT_COUNT
                      FROM GEO.COUNTIES WHERE IS_ACTIVE=1 AND PROVINCE_ID=:provinceId ORDER BY COUNTY_NAME
                    """;
            };
        }
        return jdbc.query(sql,p,(rs,i)->new GeoOption(rs.getLong("ID"),rs.getString("CODE"),
                rs.getString("NAME_FA"),nullable(rs,"UNIT_COUNT")));
    }

    /** Resolves the exact typed GEO record for an address editor; never uses coordinates to guess. */
    @Transactional(readOnly=true)
    public GeoContext geoContext(String type,long id) {
        if(!GEO_TYPES.contains(type)||id<=0)throw new IllegalArgumentException("نوع یا شناسه GEO معتبر نیست.");
        String table="CITY".equals(type)?"GEO.CITIES":"GEO.RURAL_DISTRICTS";
        String key="CITY".equals(type)?"CITY":"RURAL_DISTRICT";
        String sql="SELECT co.PROVINCE_ID,d.COUNTY_ID,g."+key+"_NAME AS GEO_NAME "
                +"FROM "+table+" g JOIN GEO.DISTRICTS d ON d.DISTRICT_ID=g.DISTRICT_ID "
                +"JOIN GEO.COUNTIES co ON co.COUNTY_ID=d.COUNTY_ID "
                +"WHERE g.IS_ACTIVE=1 AND g."+key+"_ID=:id";
        List<GeoContext> result=jdbc.query(sql,new MapSqlParameterSource("id",id),(rs,i)->new GeoContext(
                type,id,rs.getLong("PROVINCE_ID"),rs.getLong("COUNTY_ID"),rs.getString("GEO_NAME")));
        if(result.size()!=1)throw new IllegalArgumentException("مرجع جغرافیایی انتخاب‌شده یافت نشد.");
        return result.get(0);
    }
    private static boolean invalid(Long value) {return value==null||value<=0;}

    private String baseSql() {
        String geoType=effectiveGeoType("l",geoTypeColumnInstalled());
        return """
            WITH CURRENT_LOCATIONS AS (
               SELECT l.ORGANIZATION_UNIT_LOCATION_ID, l.ORGANIZATION_UNIT_ID,l.GEO_ENTITY_ID,
                      """+geoType+"""
                       AS GEO_ENTITY_TYPE_CODE_EFFECTIVE,
                      l.LATITUDE,l.LONGITUDE,l.ADDRESS_FA,l.POSTAL_CODE,
                      ROW_NUMBER() OVER (PARTITION BY l.ORGANIZATION_UNIT_ID
                          ORDER BY CASE WHEN l.LOCATION_ROLE_CODE='PRIMARY' THEN 0 ELSE 1 END,
                          l.EFFECTIVE_FROM DESC NULLS LAST,l.ORGANIZATION_UNIT_LOCATION_ID DESC) AS RN
                 FROM ORG.ORGANIZATION_UNIT_LOCATIONS l
                WHERE (l.EFFECTIVE_FROM IS NULL OR l.EFFECTIVE_FROM<=TRUNC(SYSDATE))
                  AND (l.EFFECTIVE_TO IS NULL OR l.EFFECTIVE_TO>=TRUNC(SYSDATE))
            ), ENRICHED AS (
               SELECT l.ORGANIZATION_UNIT_LOCATION_ID,l.ORGANIZATION_UNIT_ID,
                      u.UNIT_CODE,u.NAME_FA AS UNIT_NAME_FA,
                      t.TYPE_CODE AS UNIT_TYPE_CODE,t.NAME_FA AS UNIT_TYPE_NAME_FA,
                      l.LATITUDE,l.LONGITUDE,l.ADDRESS_FA,l.POSTAL_CODE,
                      p.PROVINCE_ID,p.PROVINCE_NAME,co.COUNTY_ID,co.COUNTY_NAME,
                      c.CITY_ID,c.CITY_NAME,r.RURAL_DISTRICT_ID,r.RURAL_DISTRICT_NAME
                 FROM CURRENT_LOCATIONS l
                 JOIN ORG.ORGANIZATION_UNITS u ON u.ORGANIZATION_UNIT_ID=l.ORGANIZATION_UNIT_ID
                 JOIN ORG.ORGANIZATION_UNIT_TYPES t ON t.ORGANIZATION_UNIT_TYPE_ID=u.ORGANIZATION_UNIT_TYPE_ID
                 LEFT JOIN GEO.CITIES c ON c.CITY_ID=l.GEO_ENTITY_ID
                     AND l.GEO_ENTITY_TYPE_CODE_EFFECTIVE='CITY' AND c.IS_ACTIVE=1
                 LEFT JOIN GEO.RURAL_DISTRICTS r ON r.RURAL_DISTRICT_ID=l.GEO_ENTITY_ID
                     AND l.GEO_ENTITY_TYPE_CODE_EFFECTIVE='RURAL_DISTRICT' AND r.IS_ACTIVE=1
                 LEFT JOIN GEO.DISTRICTS d ON d.DISTRICT_ID=COALESCE(c.DISTRICT_ID,r.DISTRICT_ID)
                 LEFT JOIN GEO.COUNTIES co ON co.COUNTY_ID=d.COUNTY_ID
                 LEFT JOIN GEO.PROVINCES p ON p.PROVINCE_ID=co.PROVINCE_ID
                WHERE l.RN=1 AND u.STATUS_CODE='ACTIVE'
            )
            """;
    }
    private static final String FILTER="""
         WHERE (:provinceId IS NULL OR PROVINCE_ID=:provinceId)
           AND (:countyId IS NULL OR COUNTY_ID=:countyId)
           AND (:cityId IS NULL OR CITY_ID=:cityId)
           AND (:ruralDistrictId IS NULL OR RURAL_DISTRICT_ID=:ruralDistrictId)
           AND (:typeCode IS NULL OR UNIT_TYPE_CODE=:typeCode)
           AND (:text IS NULL OR UPPER(UNIT_CODE) LIKE :text OR UPPER(UNIT_NAME_FA) LIKE :text)
        """;

    @Transactional(readOnly=true)
    public MapResult points(Long provinceId,Long countyId,Long cityId,Long ruralDistrictId,
                            String typeCode,String text) {
        if(cityId!=null && ruralDistrictId!=null)
            throw new IllegalArgumentException("شهر و دهستان هم‌زمان قابل انتخاب نیستند.");
        for(Long id:List.of(provinceId==null?0L:provinceId,countyId==null?0L:countyId,
                            cityId==null?0L:cityId,ruralDistrictId==null?0L:ruralDistrictId))
            if(id<0)throw new IllegalArgumentException("شناسه فیلتر معتبر نیست.");
        String t=text==null?"":text.strip();if(t.length()>100)t=t.substring(0,100);
        String like=t.isBlank()?null:"%"+t.toUpperCase(Locale.ROOT)+"%";
        MapSqlParameterSource p=new MapSqlParameterSource()
                .addValue("provinceId",provinceId,Types.NUMERIC)
                .addValue("countyId",countyId,Types.NUMERIC)
                .addValue("cityId",cityId,Types.NUMERIC)
                .addValue("ruralDistrictId",ruralDistrictId,Types.NUMERIC)
                .addValue("typeCode",typeCode==null||typeCode.isBlank()?null:typeCode,Types.VARCHAR)
                .addValue("text",like,Types.VARCHAR).addValue("maxRows",MAX_ROWS);
        String base=baseSql();
        String countSql=base+"SELECT COUNT(*) TOTAL_COUNT, "
                +"NVL(SUM(CASE WHEN LATITUDE BETWEEN 24 AND 41 AND LONGITUDE BETWEEN 44 AND 64 "
                +"THEN 1 ELSE 0 END),0) AS HAS_COORD FROM ENRICHED "+FILTER;
        long[] counts=jdbc.queryForObject(countSql,p,(rs,i)->new long[]{rs.getLong("TOTAL_COUNT"),rs.getLong("HAS_COORD")});
        if(counts==null)counts=new long[]{0,0};
        String sql=base+"""
                SELECT ORGANIZATION_UNIT_LOCATION_ID,ORGANIZATION_UNIT_ID,UNIT_CODE,UNIT_NAME_FA,
                       UNIT_TYPE_CODE,UNIT_TYPE_NAME_FA,ADDRESS_FA,POSTAL_CODE,LATITUDE,LONGITUDE,
                       PROVINCE_ID,PROVINCE_NAME,COUNTY_ID,COUNTY_NAME,CITY_ID,CITY_NAME,
                       RURAL_DISTRICT_ID,RURAL_DISTRICT_NAME FROM ENRICHED
                """+FILTER+"""
                  ORDER BY UNIT_NAME_FA,ORGANIZATION_UNIT_ID
                  FETCH FIRST :maxRows ROWS ONLY
                """;
        List<MapPoint> rows=jdbc.query(sql,p,(rs,i)->new MapPoint(
                rs.getLong("ORGANIZATION_UNIT_LOCATION_ID"),rs.getLong("ORGANIZATION_UNIT_ID"),
                rs.getString("UNIT_CODE"),rs.getString("UNIT_NAME_FA"),rs.getString("UNIT_TYPE_CODE"),
                rs.getString("UNIT_TYPE_NAME_FA"),rs.getString("ADDRESS_FA"),rs.getString("POSTAL_CODE"),
                rs.getBigDecimal("LATITUDE"),rs.getBigDecimal("LONGITUDE"),
                nullable(rs,"PROVINCE_ID"),rs.getString("PROVINCE_NAME"),
                nullable(rs,"COUNTY_ID"),rs.getString("COUNTY_NAME"),
                nullable(rs,"CITY_ID"),rs.getString("CITY_NAME"),
                nullable(rs,"RURAL_DISTRICT_ID"),rs.getString("RURAL_DISTRICT_NAME")));
        return new MapResult(counts[0],counts[1],counts[0]-counts[1],counts[0]>MAX_ROWS,rows);
    }
    private static Long nullable(ResultSet rs,String name) throws SQLException {
        long value=rs.getLong(name);return rs.wasNull()?null:value;
    }
}
