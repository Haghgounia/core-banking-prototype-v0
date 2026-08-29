package com.behsazan.corebanking.cif.isic.oracle;

import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityDetail;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityLookup;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityRequest;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ActivityRow;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ReleaseLookup;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ReleaseRequest;
import com.behsazan.corebanking.cif.isic.domain.IsicModels.ReleaseRow;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Repository
public class IsicRepository {
    private static final Pattern SQL_NAME = Pattern.compile("^[A-Z][A-Z0-9_$#]*$");
    private static final Set<String> RELEASE_SORTS = Set.of("ISIC_RELEASE_ID", "REVISION_CODE", "VARIANT_CODE", "NAME_FA", "NAME_EN", "DATASET_STATUS_CODE", "IS_ACTIVE");
    private static final Set<String> ACTIVITY_SORTS = Set.of("ISIC_CODE", "LEVEL_CODE", "SECTION_CODE", "NAME_FA", "NAME_EN", "TRANSLATION_STATUS", "IS_SELECTABLE", "IS_ACTIVE", "SORT_ORDER");

    private final JdbcClient jdbc;
    private final String schema;

    public IsicRepository(JdbcClient jdbc, @Value("${core-banking.schemas.cif:CIF}") String schema) {
        this.jdbc = jdbc;
        String normalized = schema == null ? "CIF" : schema.trim().toUpperCase();
        if (!SQL_NAME.matcher(normalized).matches()) throw new IllegalArgumentException("Invalid CIF schema name");
        this.schema = normalized;
    }

    public PageResponse<ReleaseRow> searchReleases(String text, Boolean active, int page, int size, String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (text != null && !text.isBlank()) {
            where.add("(UPPER(CLASSIFICATION_CODE) LIKE :text OR UPPER(REVISION_CODE) LIKE :text OR UPPER(VARIANT_CODE) LIKE :text OR UPPER(NAME_FA) LIKE :text OR UPPER(NAME_EN) LIKE :text OR UPPER(SOURCE_AUTHORITY) LIKE :text)");
            params.put("text", "%" + text.trim().toUpperCase() + "%");
        }
        if (active != null) {
            where.add("IS_ACTIVE = :active");
            params.put("active", active ? 1 : 0);
        }
        String whereSql = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);
        long total = jdbc.sql("SELECT COUNT(*) FROM " + table("REF_ISIC_RELEASE") + whereSql).params(params).query(Long.class).single();
        String order = RELEASE_SORTS.contains(normalizeSort(sortBy)) ? normalizeSort(sortBy) : "ISIC_RELEASE_ID";
        String dir = "desc".equalsIgnoreCase(direction) ? "DESC" : "ASC";
        Map<String, Object> pageParams = new LinkedHashMap<>(params);
        pageParams.put("offset", safePage * safeSize);
        pageParams.put("size", safeSize);
        String sql = "SELECT * FROM " + table("REF_ISIC_RELEASE") + whereSql + " ORDER BY " + order + " " + dir + " OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY";
        List<ReleaseRow> rows = jdbc.sql(sql).params(pageParams).query((rs, n) -> mapRelease(rs)).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public List<ReleaseLookup> releaseLookup(boolean includeInactive) {
        String sql = "SELECT ISIC_RELEASE_ID, REVISION_CODE, VARIANT_CODE, NVL(NAME_FA, NAME_EN) DISPLAY_NAME, IS_ACTIVE, DATASET_STATUS_CODE FROM " + table("REF_ISIC_RELEASE")
                + (includeInactive ? "" : " WHERE IS_ACTIVE=1") + " ORDER BY CLASSIFICATION_CODE, REVISION_CODE, VARIANT_CODE";
        return jdbc.sql(sql).query((rs, n) -> new ReleaseLookup(
                rs.getLong("ISIC_RELEASE_ID"),
                "ISIC Rev." + rs.getString("REVISION_CODE") + " / " + rs.getString("VARIANT_CODE"),
                rs.getString("DISPLAY_NAME"),
                rs.getInt("IS_ACTIVE") == 1,
                rs.getString("DATASET_STATUS_CODE")
        )).list();
    }

    public Optional<ReleaseRow> findRelease(long id) {
        return jdbc.sql("SELECT * FROM " + table("REF_ISIC_RELEASE") + " WHERE ISIC_RELEASE_ID=:id")
                .param("id", id).query((rs, n) -> mapRelease(rs)).optional();
    }

    public long insertRelease(ReleaseRequest r, String actor) {
        String sql = """
                INSERT INTO %s (CLASSIFICATION_CODE, REVISION_CODE, VARIANT_CODE, COUNTRY_CODE, NAME_FA, NAME_EN,
                    SOURCE_AUTHORITY, SOURCE_URI, PUBLICATION_DATE, DATASET_STATUS_CODE, IS_CURRENT, IS_ACTIVE,
                    VALID_FROM, VALID_TO, RECORD_VERSION, CREATED_BY, CREATED_DATE)
                VALUES (:classificationCode, :revisionCode, :variantCode, :countryCode, :nameFa, :nameEn,
                    :sourceAuthority, :sourceUri, :publicationDate, :datasetStatusCode, :currentFlag, :activeFlag,
                    :validFrom, :validTo, 1, :actor, SYSTIMESTAMP)
                """.formatted(table("REF_ISIC_RELEASE"));
        jdbc.sql(sql).params(releaseParams(r, actor, false)).update();
        return jdbc.sql("SELECT ISIC_RELEASE_ID FROM " + table("REF_ISIC_RELEASE") + " WHERE CLASSIFICATION_CODE=:c AND REVISION_CODE=:r AND VARIANT_CODE=:v")
                .param("c", r.classificationCode()).param("r", r.revisionCode()).param("v", r.variantCode())
                .query(Long.class).single();
    }

    public boolean updateRelease(long id, ReleaseRequest r, String actor) {
        String sql = """
                UPDATE %s SET CLASSIFICATION_CODE=:classificationCode, REVISION_CODE=:revisionCode, VARIANT_CODE=:variantCode,
                    COUNTRY_CODE=:countryCode, NAME_FA=:nameFa, NAME_EN=:nameEn, SOURCE_AUTHORITY=:sourceAuthority,
                    SOURCE_URI=:sourceUri, PUBLICATION_DATE=:publicationDate, DATASET_STATUS_CODE=:datasetStatusCode,
                    IS_CURRENT=:currentFlag, IS_ACTIVE=:activeFlag, VALID_FROM=:validFrom, VALID_TO=:validTo,
                    RECORD_VERSION=RECORD_VERSION+1, LAST_MODIFIED_BY=:actor, LAST_MODIFIED_DATE=SYSTIMESTAMP
                WHERE ISIC_RELEASE_ID=:id AND RECORD_VERSION=:recordVersion
                """.formatted(table("REF_ISIC_RELEASE"));
        Map<String, Object> params = releaseParams(r, actor, true);
        params.put("id", id);
        return jdbc.sql(sql).params(params).update() == 1;
    }

    public boolean deleteRelease(long id) {
        return jdbc.sql("DELETE FROM " + table("REF_ISIC_RELEASE") + " WHERE ISIC_RELEASE_ID=:id").param("id", id).update() == 1;
    }

    public boolean releaseExists(long id) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table("REF_ISIC_RELEASE") + " WHERE ISIC_RELEASE_ID=:id").param("id", id).query(Long.class).single() > 0;
    }

    public PageResponse<ActivityRow> searchActivities(Long releaseId, String parentCode, String levelCode, String text, Boolean active, Boolean selectable,
                                                      int page, int size, String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 500);
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (releaseId != null) { where.add("A.ISIC_RELEASE_ID=:releaseId"); params.put("releaseId", releaseId); }
        if (parentCode != null) {
            if (parentCode.isBlank()) where.add("A.PARENT_ISIC_CODE IS NULL");
            else { where.add("A.PARENT_ISIC_CODE=:parentCode"); params.put("parentCode", parentCode.trim()); }
        }
        if (levelCode != null && !levelCode.isBlank()) { where.add("A.LEVEL_CODE=:levelCode"); params.put("levelCode", levelCode.trim().toUpperCase()); }
        if (active != null) { where.add("A.IS_ACTIVE=:active"); params.put("active", active ? 1 : 0); }
        if (selectable != null) { where.add("A.IS_SELECTABLE=:selectable"); params.put("selectable", selectable ? 1 : 0); }
        if (text != null && !text.isBlank()) {
            where.add("(UPPER(A.ISIC_CODE) LIKE :text OR UPPER(A.NAME_FA) LIKE :text OR UPPER(A.NAME_EN) LIKE :text)");
            params.put("text", "%" + text.trim().toUpperCase() + "%");
        }
        String from = " FROM " + table("REF_ISIC_ACTIVITY2") + " A JOIN " + table("REF_ISIC_RELEASE") + " R ON R.ISIC_RELEASE_ID=A.ISIC_RELEASE_ID";
        String whereSql = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);
        long total = jdbc.sql("SELECT COUNT(*)" + from + whereSql).params(params).query(Long.class).single();
        String order = ACTIVITY_SORTS.contains(normalizeSort(sortBy)) ? normalizeSort(sortBy) : "SORT_ORDER";
        String dir = "desc".equalsIgnoreCase(direction) ? "DESC" : "ASC";
        Map<String, Object> pageParams = new LinkedHashMap<>(params);
        pageParams.put("offset", safePage * safeSize);
        pageParams.put("size", safeSize);
        String sql = "SELECT A.*, NVL(R.NAME_FA,R.NAME_EN) RELEASE_LABEL, NVL(A.NAME_FA,A.NAME_EN) DISPLAY_NAME, "
                + "CASE WHEN EXISTS (SELECT 1 FROM " + table("REF_ISIC_ACTIVITY2") + " C WHERE C.ISIC_RELEASE_ID=A.ISIC_RELEASE_ID AND C.PARENT_ISIC_CODE=A.ISIC_CODE) THEN 1 ELSE 0 END HAS_CHILDREN"
                + from + whereSql + " ORDER BY A." + order + " " + dir + " OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY";
        List<ActivityRow> rows = jdbc.sql(sql).params(pageParams).query((rs, n) -> mapActivityRow(rs)).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public Optional<ActivityDetail> findActivity(long id) {
        return jdbc.sql("SELECT * FROM " + table("REF_ISIC_ACTIVITY2") + " WHERE ISIC_ACTIVITY_ID=:id")
                .param("id", id).query((rs, n) -> mapActivityDetail(rs)).optional();
    }

    public Optional<ActivityDetail> findActivity(long releaseId, String code) {
        return jdbc.sql("SELECT * FROM " + table("REF_ISIC_ACTIVITY2") + " WHERE ISIC_RELEASE_ID=:releaseId AND ISIC_CODE=:code")
                .param("releaseId", releaseId).param("code", code).query((rs, n) -> mapActivityDetail(rs)).optional();
    }

    public long insertActivity(ActivityRequest r, String actor) {
        String sql = """
                INSERT INTO %s (ISIC_RELEASE_ID, ISIC_CODE, BASE_ISIC_CODE, LEVEL_CODE, PARENT_ISIC_CODE, SECTION_CODE,
                    NAME_FA, NAME_EN, DESCRIPTION_FA, DESCRIPTION_EN, INCLUSIONS_FA, INCLUSIONS_EN, EXCLUSIONS_FA, EXCLUSIONS_EN,
                    TRANSLATION_STATUS, IS_SELECTABLE, IS_ACTIVE, VALID_FROM, VALID_TO, SORT_ORDER,
                    RECORD_VERSION, CREATED_BY, CREATED_DATE)
                VALUES (:releaseId, :code, :baseCode, :levelCode, :parentCode, :sectionCode,
                    :nameFa, :nameEn, :descriptionFa, :descriptionEn, :inclusionsFa, :inclusionsEn, :exclusionsFa, :exclusionsEn,
                    :translationStatus, :selectableFlag, :activeFlag, :validFrom, :validTo, :sortOrder,
                    1, :actor, SYSTIMESTAMP)
                """.formatted(table("REF_ISIC_ACTIVITY2"));
        jdbc.sql(sql).params(activityParams(r, actor, false)).update();
        return jdbc.sql("SELECT ISIC_ACTIVITY_ID FROM " + table("REF_ISIC_ACTIVITY2") + " WHERE ISIC_RELEASE_ID=:releaseId AND ISIC_CODE=:code")
                .param("releaseId", r.isicReleaseId()).param("code", r.isicCode()).query(Long.class).single();
    }

    public boolean updateActivity(long id, ActivityRequest r, String actor) {
        String sql = """
                UPDATE %s SET ISIC_RELEASE_ID=:releaseId, ISIC_CODE=:code, BASE_ISIC_CODE=:baseCode, LEVEL_CODE=:levelCode,
                    PARENT_ISIC_CODE=:parentCode, SECTION_CODE=:sectionCode, NAME_FA=:nameFa, NAME_EN=:nameEn,
                    DESCRIPTION_FA=:descriptionFa, DESCRIPTION_EN=:descriptionEn, INCLUSIONS_FA=:inclusionsFa, INCLUSIONS_EN=:inclusionsEn,
                    EXCLUSIONS_FA=:exclusionsFa, EXCLUSIONS_EN=:exclusionsEn, TRANSLATION_STATUS=:translationStatus,
                    IS_SELECTABLE=:selectableFlag, IS_ACTIVE=:activeFlag, VALID_FROM=:validFrom, VALID_TO=:validTo, SORT_ORDER=:sortOrder,
                    RECORD_VERSION=RECORD_VERSION+1, LAST_MODIFIED_BY=:actor, LAST_MODIFIED_DATE=SYSTIMESTAMP
                WHERE ISIC_ACTIVITY_ID=:id AND RECORD_VERSION=:recordVersion
                """.formatted(table("REF_ISIC_ACTIVITY2"));
        Map<String, Object> params = activityParams(r, actor, true);
        params.put("id", id);
        return jdbc.sql(sql).params(params).update() == 1;
    }

    public boolean deleteActivity(long id) {
        return jdbc.sql("DELETE FROM " + table("REF_ISIC_ACTIVITY2") + " WHERE ISIC_ACTIVITY_ID=:id").param("id", id).update() == 1;
    }

    public List<ActivityLookup> activityLookup(long releaseId, String text, boolean selectableOnly, int limit) {
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        where.add("ISIC_RELEASE_ID=:releaseId");
        params.put("releaseId", releaseId);
        where.add("IS_ACTIVE=1");
        if (selectableOnly) where.add("IS_SELECTABLE=1");
        if (text != null && !text.isBlank()) {
            where.add("(UPPER(ISIC_CODE) LIKE :text OR UPPER(NAME_FA) LIKE :text OR UPPER(NAME_EN) LIKE :text)");
            params.put("text", "%" + text.trim().toUpperCase() + "%");
        }
        params.put("limit", Math.min(Math.max(limit, 1), 5000));
        String sql = "SELECT ISIC_ACTIVITY_ID, ISIC_CODE, NVL(NAME_FA,NAME_EN) DISPLAY_NAME, LEVEL_CODE, PARENT_ISIC_CODE FROM "
                + table("REF_ISIC_ACTIVITY2") + " WHERE " + String.join(" AND ", where) + " ORDER BY SORT_ORDER, ISIC_CODE FETCH FIRST :limit ROWS ONLY";
        return jdbc.sql(sql).params(params).query((rs, n) -> new ActivityLookup(
                rs.getLong("ISIC_ACTIVITY_ID"), rs.getString("ISIC_CODE"), rs.getString("DISPLAY_NAME"), rs.getString("LEVEL_CODE"), rs.getString("PARENT_ISIC_CODE")
        )).list();
    }

    private Map<String, Object> releaseParams(ReleaseRequest r, String actor, boolean update) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("classificationCode", r.classificationCode()); p.put("revisionCode", r.revisionCode()); p.put("variantCode", r.variantCode());
        p.put("countryCode", r.countryCode()); p.put("nameFa", r.nameFa()); p.put("nameEn", r.nameEn()); p.put("sourceAuthority", r.sourceAuthority());
        p.put("sourceUri", r.sourceUri()); p.put("publicationDate", sqlDate(r.publicationDate())); p.put("datasetStatusCode", r.datasetStatusCode());
        p.put("currentFlag", Boolean.TRUE.equals(r.current()) ? 1 : 0); p.put("activeFlag", Boolean.TRUE.equals(r.active()) ? 1 : 0);
        p.put("validFrom", sqlDate(r.validFrom())); p.put("validTo", sqlDate(r.validTo())); p.put("actor", actor);
        if (update) p.put("recordVersion", r.recordVersion());
        return p;
    }

    private Map<String, Object> activityParams(ActivityRequest r, String actor, boolean update) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("releaseId", r.isicReleaseId()); p.put("code", r.isicCode()); p.put("baseCode", r.baseIsicCode()); p.put("levelCode", r.levelCode());
        p.put("parentCode", r.parentIsicCode()); p.put("sectionCode", r.sectionCode()); p.put("nameFa", r.nameFa()); p.put("nameEn", r.nameEn());
        p.put("descriptionFa", r.descriptionFa()); p.put("descriptionEn", r.descriptionEn()); p.put("inclusionsFa", r.inclusionsFa());
        p.put("inclusionsEn", r.inclusionsEn()); p.put("exclusionsFa", r.exclusionsFa()); p.put("exclusionsEn", r.exclusionsEn());
        p.put("translationStatus", r.translationStatus()); p.put("selectableFlag", Boolean.TRUE.equals(r.selectable()) ? 1 : 0);
        p.put("activeFlag", Boolean.TRUE.equals(r.active()) ? 1 : 0); p.put("validFrom", sqlDate(r.validFrom())); p.put("validTo", sqlDate(r.validTo()));
        p.put("sortOrder", r.sortOrder()); p.put("actor", actor); if (update) p.put("recordVersion", r.recordVersion());
        return p;
    }

    private ReleaseRow mapRelease(ResultSet rs) throws SQLException {
        return new ReleaseRow(rs.getLong("ISIC_RELEASE_ID"), rs.getString("CLASSIFICATION_CODE"), rs.getString("REVISION_CODE"), rs.getString("VARIANT_CODE"),
                rs.getString("COUNTRY_CODE"), rs.getString("NAME_FA"), rs.getString("NAME_EN"), rs.getString("SOURCE_AUTHORITY"), rs.getString("SOURCE_URI"),
                localDate(rs.getDate("PUBLICATION_DATE")), rs.getString("DATASET_STATUS_CODE"), rs.getInt("IS_CURRENT") == 1, rs.getInt("IS_ACTIVE") == 1,
                localDate(rs.getDate("VALID_FROM")), localDate(rs.getDate("VALID_TO")), rs.getInt("RECORD_VERSION"), rs.getString("CREATED_BY"),
                localDateTime(rs.getTimestamp("CREATED_DATE")), rs.getString("LAST_MODIFIED_BY"), localDateTime(rs.getTimestamp("LAST_MODIFIED_DATE")));
    }

    private ActivityRow mapActivityRow(ResultSet rs) throws SQLException {
        return new ActivityRow(rs.getLong("ISIC_ACTIVITY_ID"), rs.getLong("ISIC_RELEASE_ID"), rs.getString("RELEASE_LABEL"), rs.getString("ISIC_CODE"),
                rs.getString("BASE_ISIC_CODE"), rs.getString("LEVEL_CODE"), rs.getString("PARENT_ISIC_CODE"), rs.getString("SECTION_CODE"), rs.getString("NAME_FA"),
                rs.getString("NAME_EN"), rs.getString("DISPLAY_NAME"), rs.getString("TRANSLATION_STATUS"), rs.getInt("IS_SELECTABLE") == 1, rs.getInt("IS_ACTIVE") == 1,
                localDate(rs.getDate("VALID_FROM")), localDate(rs.getDate("VALID_TO")), rs.getInt("SORT_ORDER"), rs.getInt("RECORD_VERSION"), rs.getInt("HAS_CHILDREN") == 1);
    }

    private ActivityDetail mapActivityDetail(ResultSet rs) throws SQLException {
        return new ActivityDetail(rs.getLong("ISIC_ACTIVITY_ID"), rs.getLong("ISIC_RELEASE_ID"), rs.getString("ISIC_CODE"), rs.getString("BASE_ISIC_CODE"),
                rs.getString("LEVEL_CODE"), rs.getString("PARENT_ISIC_CODE"), rs.getString("SECTION_CODE"), rs.getString("NAME_FA"), rs.getString("NAME_EN"),
                clob(rs, "DESCRIPTION_FA"), clob(rs, "DESCRIPTION_EN"), clob(rs, "INCLUSIONS_FA"), clob(rs, "INCLUSIONS_EN"), clob(rs, "EXCLUSIONS_FA"), clob(rs, "EXCLUSIONS_EN"),
                rs.getString("TRANSLATION_STATUS"), rs.getInt("IS_SELECTABLE") == 1, rs.getInt("IS_ACTIVE") == 1, localDate(rs.getDate("VALID_FROM")),
                localDate(rs.getDate("VALID_TO")), rs.getInt("SORT_ORDER"), rs.getInt("RECORD_VERSION"), rs.getString("CREATED_BY"),
                localDateTime(rs.getTimestamp("CREATED_DATE")), rs.getString("LAST_MODIFIED_BY"), localDateTime(rs.getTimestamp("LAST_MODIFIED_DATE")));
    }

    private static String clob(ResultSet rs, String column) throws SQLException {
        Clob value = rs.getClob(column);
        return value == null ? null : value.getSubString(1, (int) value.length());
    }

    private String table(String name) { return schema + "." + name; }
    private static String normalizeSort(String value) { return value == null ? "" : value.trim().toUpperCase(); }
    private static Date sqlDate(LocalDate d) { return d == null ? null : Date.valueOf(d); }
    private static LocalDate localDate(Date d) { return d == null ? null : d.toLocalDate(); }
    private static LocalDateTime localDateTime(Timestamp t) { return t == null ? null : t.toLocalDateTime(); }
}
