package com.behsazan.corebanking.organization.location.manage;

import com.behsazan.corebanking.organization.location.manage.OrganizationUnitLocationModels.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical-only location/branch address management. Legacy locations & postal addresses are never
 * overwritten, including the 4 distinct assignments referencing legacy LOCATION_ID=1327.
 * All modifications are user-triggered, unit-serialized and audited in the same transaction.
 */
@Service
public class OrganizationUnitLocationService {
    private static final Set<String> ROLES=Set.of("PRIMARY","REGISTERED","OPERATIONAL");
    private static final Set<String> ADDRESS_TYPES=Set.of("PHYSICAL","POSTAL","MAILING");
    private static final Set<String> PRESENCES=Set.of("PHYSICAL","VIRTUAL");
    private static final Set<String> USAGES=Set.of("PHYSICAL","REFERENCE");
    private static final Set<String> GEO_TYPES=Set.of("CITY","RURAL_DISTRICT");
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper mapper;

    public OrganizationUnitLocationService(NamedParameterJdbcTemplate jdbc, JsonMapper mapper) {
        this.jdbc=jdbc; this.mapper=mapper;
    }

    @Transactional(readOnly=true)
    public Readiness readiness() {
        String sql="""
                SELECT
                   (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='ORG' AND TABLE_NAME='ORGANIZATION_UNIT_LOCATIONS' AND COLUMN_NAME='LOCATION_ID' AND NULLABLE='Y') AS LOCATION_NULLABLE,
                   (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='ORG' AND TABLE_NAME='ORGANIZATION_UNIT_LOCATIONS' AND COLUMN_NAME='CANONICAL_MANAGED_FLAG') AS MANAGED_COL,
                   (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='ORG' AND TABLE_NAME='ORGANIZATION_UNIT_LOCATIONS' AND COLUMN_NAME IN ('UNIT_PRESENCE_CODE','ADDRESS_USAGE_CODE')) AS USAGE_COLS,
                   (SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='ORG' AND TABLE_NAME='OU_LOCATION_CHANGE_AUDIT') AS AUDIT_TABLE,
                   (SELECT COUNT(*) FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='ORG' AND SEQUENCE_NAME='SEQ_OU_LOCATION_AUDIT') AS AUDIT_SEQ,
                   (SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='ORG' AND TABLE_NAME='ORGANIZATION_UNIT_LOCATIONS' AND COLUMN_NAME='GEO_ENTITY_TYPE_CODE') AS GEO_TYPE_COL,
                   (SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='ORG'
                    AND TABLE_NAME='ORGANIZATION_UNIT_LOCATIONS' AND CONSTRAINT_NAME='CK_OUL_R2D_GEO_TYPE'
                    AND CONSTRAINT_TYPE='C' AND STATUS='ENABLED') AS GEO_TYPE_CHECK
                 FROM DUAL
                """;
        return jdbc.queryForObject(sql,new MapSqlParameterSource(),(rs,i)-> {
            boolean n=rs.getInt("LOCATION_NULLABLE")==1;
            boolean m=rs.getInt("MANAGED_COL")==1;
            boolean u=rs.getInt("USAGE_COLS")==2;
            boolean a=rs.getInt("AUDIT_TABLE")==1 && rs.getInt("AUDIT_SEQ")==1;
            boolean g=rs.getInt("GEO_TYPE_COL")==1 && rs.getInt("GEO_TYPE_CHECK")==1;
            return new Readiness(n,m,u,a,g,n&&m&&u&&a&&g);
        });
    }

    @Transactional(readOnly=true)
    public LocationDetails get(long id) { requireReady(); return find(id); }

    @Transactional(readOnly=true)
    public List<AuditEntry> history(long id) {
        requireReady();
        find(id);
        return jdbc.query("""
                SELECT AUDIT_ID, ORGANIZATION_UNIT_LOCATION_ID,ACTION_CODE,
                       CHANGE_REFERENCE, CHANGED_AT,CHANGED_BY
                  FROM ORG.OU_LOCATION_CHANGE_AUDIT
                 WHERE ORGANIZATION_UNIT_LOCATION_ID=:id ORDER BY AUDIT_ID DESC
                """, new MapSqlParameterSource("id",id), (rs,i)->new AuditEntry(
                rs.getLong("AUDIT_ID"),rs.getLong("ORGANIZATION_UNIT_LOCATION_ID"),
                rs.getString("ACTION_CODE"),rs.getString("CHANGE_REFERENCE"),
                rs.getString("CHANGED_AT"),rs.getString("CHANGED_BY")));
    }

    @Transactional
    public LocationDetails create(EditInput raw) {
        requireReady();
        EditInput body=validate(raw,true);
        validateGeoReference(body);
        long unitId=body.organizationUnitId();
        lockUnit(unitId);
        ensureNoPrimaryOverlap(body,null);
        Long id=jdbc.queryForObject("SELECT ORG.SEQ_ORGANIZATION_UNIT_LOCATIONS.NEXTVAL FROM DUAL",
                new MapSqlParameterSource(),Long.class);
        if (id==null || id<=0 || id>=100_000_000L)
            throw OrganizationUnitLocationException.conflict("شناسه مکان خارج از محدوده مجاز است؛ وضعیت Sequence را بررسی کنید.");
        MapSqlParameterSource p=params(body).addValue("id",id);
        jdbc.update("""
                INSERT INTO ORG.ORGANIZATION_UNIT_LOCATIONS (
                  ORGANIZATION_UNIT_LOCATION_ID, ORGANIZATION_UNIT_ID, LOCATION_ID,
                  LOCATION_ROLE_CODE,EFFECTIVE_FROM,EFFECTIVE_TO,
                  GEO_ENTITY_ID,GEO_ENTITY_TYPE_CODE,LATITUDE,LONGITUDE,COORDINATE_REFERENCE_SYSTEM_CODE,
                  LOCATION_DESCRIPTION_FA,LOCATION_DESCRIPTION_EN,
                  ADDRESS_TYPE_CODE,MAIN_STREET_FA,MAIN_STREET_EN,SECONDARY_STREET_FA,SECONDARY_STREET_EN,
                  SQUARE_CROSSROAD_FA,SQUARE_CROSSROAD_EN,BUILDING_NO,POSTAL_CODE,
                  ADDRESS_FA,ADDRESS_EN,ADDITIONAL_ADDRESS_FA,ADDITIONAL_ADDRESS_EN,
                  RELOCATION_REASON_CODE,DECISION_REFERENCE,DECISION_DATE,
                  LEGACY_BACKFILL_FLAG,CANONICAL_MANAGED_FLAG,UNIT_PRESENCE_CODE,ADDRESS_USAGE_CODE)
                VALUES (
                  :id,:unitId,NULL,:role,:fromDate,:toDate,:geoId,:geoType,:latitude,:longitude,:crs,
                  :locationDescriptionFa,:locationDescriptionEn,:addressType,:mainStreetFa,:mainStreetEn,
                  :secondaryStreetFa,:secondaryStreetEn,:squareCrossroadFa,:squareCrossroadEn,:buildingNo,
                  :postalCode,:addressFa,:addressEn,:additionalAddressFa,:additionalAddressEn,
                  :reason,:reference,:decisionDate,0,1,:presence,:usage)
                """,p);
        LocationDetails after=find(id);
        audit(id,unitId,"CREATE",null,after,body.decisionReference());
        return after;
    }

    @Transactional
    public LocationDetails update(long id, EditInput raw) {
        if (id<=0) throw OrganizationUnitLocationException.invalid("شناسه استقرار معتبر نیست.");
        requireReady();
        EditInput body=validate(raw,false);
        validateGeoReference(body);
        lockUnit(body.organizationUnitId());
        // Same lock order for all writes: unit -> location (prevents lost updates and deadlocks).
        List<Long> locks=jdbc.query("""
                SELECT ORGANIZATION_UNIT_LOCATION_ID FROM ORG.ORGANIZATION_UNIT_LOCATIONS
                WHERE ORGANIZATION_UNIT_LOCATION_ID=:id FOR UPDATE
                """, new MapSqlParameterSource("id",id),(rs,i)->rs.getLong(1));
        if (locks.isEmpty()) throw OrganizationUnitLocationException.missing("استقرار انتخاب‌شده یافت نشد.");
        LocationDetails old=find(id);
        if (old.organizationUnitId()!=body.organizationUnitId())
            throw OrganizationUnitLocationException.conflict("انتقال رکورد استقرار به واحد دیگر مجاز نیست.");
        if (raw.expectedRowVersion()==null || raw.expectedRowVersion()!=old.rowVersion())
            throw OrganizationUnitLocationException.conflict("این استقرار توسط کاربر دیگری تغییر کرده است؛ مجدداً بارگذاری کنید.");
        ensureNoPrimaryOverlap(body,id);
        MapSqlParameterSource p=params(body).addValue("id",id).addValue("expectedVersion",body.expectedRowVersion());
        int updated=jdbc.update("""
                UPDATE ORG.ORGANIZATION_UNIT_LOCATIONS SET
                  LOCATION_ROLE_CODE=:role,EFFECTIVE_FROM=:fromDate,EFFECTIVE_TO=:toDate,
                  GEO_ENTITY_ID=:geoId,GEO_ENTITY_TYPE_CODE=:geoType,LATITUDE=:latitude,LONGITUDE=:longitude,
                  COORDINATE_REFERENCE_SYSTEM_CODE=:crs,
                  LOCATION_DESCRIPTION_FA=:locationDescriptionFa,LOCATION_DESCRIPTION_EN=:locationDescriptionEn,
                  ADDRESS_TYPE_CODE=:addressType,MAIN_STREET_FA=:mainStreetFa,MAIN_STREET_EN=:mainStreetEn,
                  SECONDARY_STREET_FA=:secondaryStreetFa,SECONDARY_STREET_EN=:secondaryStreetEn,
                  SQUARE_CROSSROAD_FA=:squareCrossroadFa,SQUARE_CROSSROAD_EN=:squareCrossroadEn,
                  BUILDING_NO=:buildingNo,POSTAL_CODE=:postalCode,ADDRESS_FA=:addressFa,ADDRESS_EN=:addressEn,
                  ADDITIONAL_ADDRESS_FA=:additionalAddressFa,ADDITIONAL_ADDRESS_EN=:additionalAddressEn,
                  RELOCATION_REASON_CODE=:reason,DECISION_REFERENCE=:reference,DECISION_DATE=:decisionDate,
                  UNIT_PRESENCE_CODE=:presence,ADDRESS_USAGE_CODE=:usage,
                  CANONICAL_MANAGED_FLAG=1,ROW_VERSION=ROW_VERSION+1
                WHERE ORGANIZATION_UNIT_LOCATION_ID=:id AND ORGANIZATION_UNIT_ID=:unitId
                  AND ROW_VERSION=:expectedVersion
                """,p);
        if (updated!=1) throw OrganizationUnitLocationException.conflict("ویرایش ذخیره نشد؛ نسخه رکورد را مجدداً بررسی کنید.");
        LocationDetails after=find(id);
        audit(id,body.organizationUnitId(),"UPDATE",old,after,body.decisionReference());
        return after;
    }

    private void audit(long id,long unitId,String action,LocationDetails before,LocationDetails after,String reference) {
        jdbc.update("""
                INSERT INTO ORG.OU_LOCATION_CHANGE_AUDIT
                (ORGANIZATION_UNIT_LOCATION_ID,ORGANIZATION_UNIT_ID,ACTION_CODE,OLD_STATE,NEW_STATE,CHANGE_REFERENCE,CHANGED_BY)
                VALUES (:id,:unitId,:action,:oldState,:newState,:reference,SYS_CONTEXT('USERENV','SESSION_USER'))
                """,new MapSqlParameterSource().addValue("id",id).addValue("unitId",unitId)
                .addValue("action",action).addValue("oldState",before==null?null:json(before),java.sql.Types.CLOB)
                .addValue("newState",json(after),java.sql.Types.CLOB).addValue("reference",reference));
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JacksonException ex) {throw new IllegalStateException("Location audit snapshot could not be written",ex);}
    }

    private void requireReady() {
        Readiness r=readiness();
        if (r==null || !r.ready()) throw OrganizationUnitLocationException.schema();
    }

    private void lockUnit(long id) {
        List<Long> results=jdbc.query("""
                SELECT ORGANIZATION_UNIT_ID FROM ORG.ORGANIZATION_UNITS
                 WHERE ORGANIZATION_UNIT_ID=:id FOR UPDATE
                """,new MapSqlParameterSource("id",id),(rs,i)->rs.getLong(1));
        if (results.isEmpty()) throw OrganizationUnitLocationException.missing("واحد سازمانی انتخاب‌شده وجود ندارد.");
    }

    private void ensureNoPrimaryOverlap(EditInput input,Long exclusionId) {
        if (!"PRIMARY".equals(input.locationRoleCode())) return;
        String exclusion=exclusionId==null?"":" AND ORGANIZATION_UNIT_LOCATION_ID<>:excludedId ";
        MapSqlParameterSource p=new MapSqlParameterSource()
                .addValue("unitId",input.organizationUnitId())
                .addValue("fromDate",sqlDate(input.effectiveFrom()),java.sql.Types.DATE)
                .addValue("toDate",sqlDate(input.effectiveTo()),java.sql.Types.DATE);
        if (exclusionId!=null) p.addValue("excludedId",exclusionId);
        Long count=jdbc.queryForObject("""
                SELECT COUNT(*) FROM ORG.ORGANIZATION_UNIT_LOCATIONS
                 WHERE ORGANIZATION_UNIT_ID=:unitId AND LOCATION_ROLE_CODE='PRIMARY'
                   AND NVL(EFFECTIVE_FROM,DATE '0001-01-01') <= NVL(:toDate,DATE '9999-12-31')
                   AND NVL(:fromDate,DATE '0001-01-01') <= NVL(EFFECTIVE_TO,DATE '9999-12-31')
                """+exclusion,p,Long.class);
        if (count!=null && count>0) throw OrganizationUnitLocationException.conflict(
                "یک استقرار اصلی با بازه زمانی هم‌پوشان برای این واحد وجود دارد. ابتدا اعتبار استقرار قبلی را خاتمه دهید.");
    }

    private LocationDetails find(long id) {
        List<LocationDetails> list=jdbc.query("""
                SELECT ul.*,
                       CASE WHEN ul.GEO_ENTITY_TYPE_CODE IN ('CITY','RURAL_DISTRICT')
                           THEN ul.GEO_ENTITY_TYPE_CODE
                           WHEN ul.GEO_ENTITY_TYPE_CODE IS NULL AND ul.LEGACY_BACKFILL_FLAG=1
                           THEN 'CITY' END AS EFFECTIVE_GEO_TYPE_CODE,
                       u.UNIT_CODE, u.NAME_FA AS UNIT_NAME_FA
                  FROM ORG.ORGANIZATION_UNIT_LOCATIONS ul
                  JOIN ORG.ORGANIZATION_UNITS u ON u.ORGANIZATION_UNIT_ID=ul.ORGANIZATION_UNIT_ID
                 WHERE ul.ORGANIZATION_UNIT_LOCATION_ID=:id
                """,new MapSqlParameterSource("id",id), (rs,i)->readRow(rs));
        if (list.isEmpty()) throw OrganizationUnitLocationException.missing("استقرار انتخاب‌شده یافت نشد.");
        return list.get(0);
    }

    private static LocationDetails readRow(ResultSet rs) throws SQLException {
        long version=rs.getLong("ROW_VERSION");
        EditInput fields=new EditInput(rs.getLong("ORGANIZATION_UNIT_ID"),rs.getString("LOCATION_ROLE_CODE"),
                date(rs,"EFFECTIVE_FROM"),date(rs,"EFFECTIVE_TO"),
                nullableLong(rs,"GEO_ENTITY_ID"),rs.getString("EFFECTIVE_GEO_TYPE_CODE"),rs.getBigDecimal("LATITUDE"),rs.getBigDecimal("LONGITUDE"),
                rs.getString("COORDINATE_REFERENCE_SYSTEM_CODE"),
                rs.getString("LOCATION_DESCRIPTION_FA"),rs.getString("LOCATION_DESCRIPTION_EN"),
                rs.getString("ADDRESS_TYPE_CODE"),rs.getString("MAIN_STREET_FA"),rs.getString("MAIN_STREET_EN"),
                rs.getString("SECONDARY_STREET_FA"),rs.getString("SECONDARY_STREET_EN"),
                rs.getString("SQUARE_CROSSROAD_FA"),rs.getString("SQUARE_CROSSROAD_EN"),
                rs.getString("BUILDING_NO"),rs.getString("POSTAL_CODE"),rs.getString("ADDRESS_FA"),
                rs.getString("ADDRESS_EN"),rs.getString("ADDITIONAL_ADDRESS_FA"),
                rs.getString("ADDITIONAL_ADDRESS_EN"),rs.getString("UNIT_PRESENCE_CODE"),
                rs.getString("ADDRESS_USAGE_CODE"),rs.getString("RELOCATION_REASON_CODE"),
                rs.getString("DECISION_REFERENCE"),date(rs,"DECISION_DATE"),version);
        return new LocationDetails(rs.getLong("ORGANIZATION_UNIT_LOCATION_ID"),rs.getLong("ORGANIZATION_UNIT_ID"),
                rs.getString("UNIT_CODE"),rs.getString("UNIT_NAME_FA"),
                nullableLong(rs,"LOCATION_ID"),nullableLong(rs,"LEGACY_POSTAL_ADDRESS_ID"),
                rs.getInt("LEGACY_BACKFILL_FLAG"),rs.getInt("CANONICAL_MANAGED_FLAG"),version,fields);
    }

    private static LocalDate date(ResultSet rs,String name) throws SQLException {
        java.sql.Date result=rs.getDate(name);return result==null?null:result.toLocalDate();
    }
    private static Long nullableLong(ResultSet rs,String name) throws SQLException {
        long val=rs.getLong(name);return rs.wasNull()?null:val;
    }
    private static java.sql.Date sqlDate(LocalDate date) {return date==null?null:java.sql.Date.valueOf(date);}

    private static MapSqlParameterSource params(EditInput x) {
        return new MapSqlParameterSource()
                .addValue("unitId",x.organizationUnitId()).addValue("role",x.locationRoleCode())
                .addValue("fromDate",sqlDate(x.effectiveFrom()),java.sql.Types.DATE)
                .addValue("toDate",sqlDate(x.effectiveTo()),java.sql.Types.DATE)
                .addValue("geoId",x.geoEntityId()).addValue("geoType",x.geoEntityTypeCode())
                .addValue("latitude",x.latitude()).addValue("longitude",x.longitude())
                .addValue("crs",x.coordinateReferenceSystemCode())
                .addValue("locationDescriptionFa",x.locationDescriptionFa()).addValue("locationDescriptionEn",x.locationDescriptionEn())
                .addValue("addressType",x.addressTypeCode()).addValue("mainStreetFa",x.mainStreetFa())
                .addValue("mainStreetEn",x.mainStreetEn()).addValue("secondaryStreetFa",x.secondaryStreetFa())
                .addValue("secondaryStreetEn",x.secondaryStreetEn()).addValue("squareCrossroadFa",x.squareCrossroadFa())
                .addValue("squareCrossroadEn",x.squareCrossroadEn()).addValue("buildingNo",x.buildingNo())
                .addValue("postalCode",x.postalCode()).addValue("addressFa",x.addressFa()).addValue("addressEn",x.addressEn())
                .addValue("additionalAddressFa",x.additionalAddressFa()).addValue("additionalAddressEn",x.additionalAddressEn())
                .addValue("presence",x.unitPresenceCode()).addValue("usage",x.addressUsageCode())
                .addValue("reason",x.relocationReasonCode()).addValue("reference",x.decisionReference())
                .addValue("decisionDate",sqlDate(x.decisionDate()),java.sql.Types.DATE);
    }

    private static EditInput validate(EditInput in,boolean creating) {
        if (in==null || in.organizationUnitId()==null || in.organizationUnitId()<=0)
            throw OrganizationUnitLocationException.invalid("واحد سازمانی الزامی است.");
        if (!ROLES.contains(in.locationRoleCode()))
            throw OrganizationUnitLocationException.invalid("نقش استقرار معتبر نیست.");
        if (!ADDRESS_TYPES.contains(in.addressTypeCode()))
            throw OrganizationUnitLocationException.invalid("نوع نشانی معتبر نیست.");
        if (in.effectiveFrom()!=null && in.effectiveTo()!=null && in.effectiveTo().isBefore(in.effectiveFrom()))
            throw OrganizationUnitLocationException.invalid("پایان اعتبار پیش از شروع اعتبار است.");
        if (in.geoEntityId()!=null && (in.geoEntityId()<=0 || in.geoEntityId()>99_999_999L))
            throw OrganizationUnitLocationException.invalid("شناسه GEO معتبر نیست.");
        boolean hasGeoType=in.geoEntityTypeCode()!=null && !in.geoEntityTypeCode().isBlank();
        if ((in.geoEntityId()!=null)!=hasGeoType)
            throw OrganizationUnitLocationException.invalid("نوع و شناسه مرجع جغرافیایی باید با هم تعیین شوند.");
        if (hasGeoType && !GEO_TYPES.contains(in.geoEntityTypeCode()))
            throw OrganizationUnitLocationException.invalid("نوع مرجع جغرافیایی باید شهر یا دهستان باشد.");
        if (creating && in.geoEntityId()==null)
            throw OrganizationUnitLocationException.invalid("برای استقرار جدید، شهر یا دهستان را از فهرست مرجع انتخاب کنید.");
        if ((in.latitude()==null)!=(in.longitude()==null))
            throw OrganizationUnitLocationException.invalid("طول و عرض جغرافیایی باید هر دو درج شوند یا هر دو خالی باشند.");
        if (in.latitude()!=null && (in.latitude().compareTo(BigDecimal.valueOf(-90))<0 ||
                in.latitude().compareTo(BigDecimal.valueOf(90))>0 || in.latitude().scale()>7))
            throw OrganizationUnitLocationException.invalid("عرض جغرافیایی باید بین ۹۰- تا ۹۰ با حداکثر ۷ رقم اعشار باشد.");
        if (in.longitude()!=null && (in.longitude().compareTo(BigDecimal.valueOf(-180))<0 ||
                in.longitude().compareTo(BigDecimal.valueOf(180))>0 || in.longitude().scale()>7))
            throw OrganizationUnitLocationException.invalid("طول جغرافیایی باید بین ۱۸۰- تا ۱۸۰ با حداکثر ۷ رقم اعشار باشد.");
        if (in.addressFa()==null || in.addressFa().isBlank())
            throw OrganizationUnitLocationException.invalid("نشانی کامل فارسی الزامی است.");
        requireMax(in.addressFa(),1000,"نشانی فارسی");
        requireMax(in.addressEn(),1000,"نشانی انگلیسی");
        requireMax(in.postalCode(),20,"کدپستی");
        requireMax(in.coordinateReferenceSystemCode(),20,"سامانه مختصات");
        requireMax(in.locationDescriptionFa(),500,"شرح مکان فارسی");
        requireMax(in.locationDescriptionEn(),500,"شرح مکان انگلیسی");
        requireMax(in.mainStreetFa(),300,"خیابان اصلی فارسی");
        requireMax(in.mainStreetEn(),300,"خیابان اصلی انگلیسی");
        requireMax(in.secondaryStreetFa(),300,"خیابان فرعی فارسی");
        requireMax(in.secondaryStreetEn(),300,"خیابان فرعی انگلیسی");
        requireMax(in.squareCrossroadFa(),300,"تقاطع فارسی");
        requireMax(in.squareCrossroadEn(),300,"تقاطع انگلیسی");
        requireMax(in.buildingNo(),30,"شماره ساختمان");
        requireMax(in.additionalAddressFa(),500,"توضیحات تکمیلی فارسی");
        requireMax(in.additionalAddressEn(),500,"توضیحات تکمیلی انگلیسی");
        requireMax(in.relocationReasonCode(),50,"علت تغییر");
        if (in.decisionReference()==null || in.decisionReference().isBlank())
            throw OrganizationUnitLocationException.invalid("ثبت مرجع مستند/مجوز تغییر برای حفظ ردپای حسابرسی الزامی است.");
        requireMax(in.decisionReference(),160,"مرجع تصمیم");
        if (in.unitPresenceCode()!=null && !PRESENCES.contains(in.unitPresenceCode()))
            throw OrganizationUnitLocationException.invalid("ماهیت واحد فیزیکی/مجازی معتبر نیست.");
        if (in.addressUsageCode()!=null && !USAGES.contains(in.addressUsageCode()))
            throw OrganizationUnitLocationException.invalid("کاربرد نشانی معتبر نیست.");
        if (creating && (in.unitPresenceCode()==null || in.addressUsageCode()==null))
            throw OrganizationUnitLocationException.invalid("برای استقرار جدید ماهیت فیزیکی/مجازی و کاربرد نشانی باید مشخص شود.");
        if (creating && in.expectedRowVersion()!=null)
            throw OrganizationUnitLocationException.invalid("در ثبت جدید، شماره نسخه نباید ارسال شود.");
        if (!creating && in.expectedRowVersion()==null)
            throw OrganizationUnitLocationException.invalid("در ویرایش، شماره نسخه رکورد الزامی است.");
        return in;
    }
    private void validateGeoReference(EditInput input) {
        if(input.geoEntityId()==null)return;
        // The type is supplied by the governed selector. A numeric ID from another GEO table is invalid.
        String sql="CITY".equals(input.geoEntityTypeCode())
                ? "SELECT COUNT(*) FROM GEO.CITIES WHERE CITY_ID=:id AND IS_ACTIVE=1"
                : "SELECT COUNT(*) FROM GEO.RURAL_DISTRICTS WHERE RURAL_DISTRICT_ID=:id AND IS_ACTIVE=1";
        Long matches=jdbc.queryForObject(sql,new MapSqlParameterSource("id",input.geoEntityId()),Long.class);
        if(matches==null||matches!=1L)
            throw OrganizationUnitLocationException.invalid("شهر یا دهستان انتخاب‌شده در مرجع GEO فعال نیست.");
    }

    private static void requireMax(String value,int n,String name) {
        if (value!=null && value.length()>n)
            throw OrganizationUnitLocationException.invalid(name+" حداکثر "+n+" کاراکتر می‌پذیرد.");
    }
}
