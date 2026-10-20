package com.behsazan.corebanking.organization.grade;

import com.behsazan.corebanking.organization.grade.GradeModels.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Explicit, parameterized SQL. No ORG-GRADE tables are exposed to generic reference CRUD. */
@Repository
public class GradeRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public GradeRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<Level> LEVEL_ROW = (rs, n) -> new Level(
            rs.getLong("ORGANIZATION_UNIT_GRADE_LEVEL_ID"), rs.getLong("ORGANIZATION_ID"),
            rs.getLong("ORGANIZATION_UNIT_TYPE_ID"), rs.getString("GRADE_SCHEME_CODE"),
            rs.getString("GRADE_CODE"), rs.getString("NAME_FA"), rs.getString("NAME_EN"),
            rs.getInt("DISPLAY_ORDER"), rs.getInt("ACTIVE_FLAG") == 1, rs.getLong("ROW_VERSION"));

    private static LocalDate date(ResultSet rs, String field) throws SQLException {
        Date d = rs.getDate(field);
        return d == null ? null : d.toLocalDate();
    }

    private static final String PERIOD_SELECT = """
        SELECT p.ORGANIZATION_UNIT_GRADE_PERIOD_ID, p.ORGANIZATION_UNIT_ID,
               p.ORGANIZATION_UNIT_GRADE_LEVEL_ID, g.GRADE_CODE, g.NAME_FA AS GRADE_NAME_FA,
               p.EFFECTIVE_FROM, p.EFFECTIVE_TO, p.CHANGE_REASON_CODE,
               p.RECORD_STATE, p.DESCRIPTION_FA, p.CREATED_BY, p.ROW_VERSION
          FROM ORG.ORGANIZATION_UNIT_GRADE_PERIODS p
          JOIN ORG.ORGANIZATION_UNIT_GRADE_LEVELS g
            ON g.ORGANIZATION_UNIT_GRADE_LEVEL_ID = p.ORGANIZATION_UNIT_GRADE_LEVEL_ID
        """;
    private static final RowMapper<Period> PERIOD_ROW = (rs, n) -> new Period(
            rs.getLong("ORGANIZATION_UNIT_GRADE_PERIOD_ID"), rs.getLong("ORGANIZATION_UNIT_ID"),
            rs.getLong("ORGANIZATION_UNIT_GRADE_LEVEL_ID"), rs.getString("GRADE_CODE"),
            rs.getString("GRADE_NAME_FA"), date(rs, "EFFECTIVE_FROM"), date(rs, "EFFECTIVE_TO"),
            rs.getString("CHANGE_REASON_CODE"), rs.getString("RECORD_STATE"),
            rs.getString("DESCRIPTION_FA"), rs.getString("CREATED_BY"), rs.getLong("ROW_VERSION"));

    public List<Organization> organizations() {
        return jdbc.query("SELECT ORGANIZATION_ID, ORGANIZATION_CODE, NAME_FA FROM ORG.ORGANIZATIONS ORDER BY NAME_FA",
                Map.of(), (rs, n) -> new Organization(rs.getLong(1), rs.getString(2), rs.getString(3)));
    }
    public Map<String, Object> branchType() {
        List<Map<String, Object>> types = jdbc.query("SELECT ORGANIZATION_UNIT_TYPE_ID, TYPE_CODE FROM ORG.ORGANIZATION_UNIT_TYPES WHERE TYPE_CODE='BRANCH'",
                Map.of(), (rs,n) -> Map.<String,Object>of("id", rs.getLong(1), "code", rs.getString(2)));
        if (types.size() != 1) throw GradeException.conflict("نوع واحد BRANCH در ORG یکتا و معتبر نیست.");
        return types.get(0);
    }
    public List<Unit> branchSearch(String text, int limit) {
        String term = text == null ? null : text.strip().toLowerCase();
        if (term != null && term.isBlank()) term = null;
        return jdbc.query("""
                SELECT u.ORGANIZATION_UNIT_ID, u.ORGANIZATION_ID, u.ORGANIZATION_UNIT_TYPE_ID,
                       u.UNIT_CODE, u.NAME_FA, u.STATUS_CODE,
                       (SELECT g.NAME_FA FROM ORG.ORGANIZATION_UNIT_GRADE_PERIODS p
                        JOIN ORG.ORGANIZATION_UNIT_GRADE_LEVELS g
                          ON g.ORGANIZATION_UNIT_GRADE_LEVEL_ID = p.ORGANIZATION_UNIT_GRADE_LEVEL_ID
                        WHERE p.ORGANIZATION_UNIT_ID = u.ORGANIZATION_UNIT_ID
                          AND p.RECORD_STATE IN ('ACTIVE','APPROVED')
                          AND p.EFFECTIVE_FROM <= TRUNC(CURRENT_DATE)
                          AND (p.EFFECTIVE_TO IS NULL OR p.EFFECTIVE_TO > TRUNC(CURRENT_DATE))
                        FETCH FIRST 1 ROW ONLY) AS CURRENT_GRADE
                  FROM ORG.ORGANIZATION_UNITS u
                  JOIN ORG.ORGANIZATION_UNIT_TYPES t ON t.ORGANIZATION_UNIT_TYPE_ID = u.ORGANIZATION_UNIT_TYPE_ID
                 WHERE t.TYPE_CODE = 'BRANCH'
                   AND (:term IS NULL OR LOWER(u.UNIT_CODE) LIKE :pattern OR LOWER(u.NAME_FA) LIKE :pattern OR LOWER(u.NAME_EN) LIKE :pattern)
                 ORDER BY CASE WHEN u.STATUS_CODE = 'ACTIVE' THEN 0 ELSE 1 END, u.UNIT_CODE
                 FETCH FIRST :maxRows ROWS ONLY
                """, new MapSqlParameterSource()
                        .addValue("term", term)
                        .addValue("pattern", term == null ? null : "%" + term + "%")
                        .addValue("maxRows", Math.max(1, Math.min(limit, 150))),
                (rs, n) -> new Unit(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)));
    }
    public Unit branchById(long id) {
        Unit unit = requireUnit(id);
        if (!"BRANCH".equals(unitTypeCode(unit.organizationUnitTypeId())))
            throw GradeException.notFound("واحد انتخابی شعبه نیست.");
        return unit;
    }
    public Unit requireUnit(long id) {
        List<Unit> units = jdbc.query("""
                SELECT u.ORGANIZATION_UNIT_ID, u.ORGANIZATION_ID, u.ORGANIZATION_UNIT_TYPE_ID,
                       u.UNIT_CODE, u.NAME_FA, u.STATUS_CODE, CAST(NULL AS VARCHAR2(150)) AS CURRENT_GRADE
                  FROM ORG.ORGANIZATION_UNITS u WHERE u.ORGANIZATION_UNIT_ID=:id
                """, Map.of("id", id), (rs,n) -> new Unit(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                    rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)));
        if (units.isEmpty()) throw GradeException.notFound("واحد سازمانی یافت نشد.");
        return units.get(0);
    }
    public void lockUnit(long id) {
        List<Long> found = jdbc.query("SELECT ORGANIZATION_UNIT_ID FROM ORG.ORGANIZATION_UNITS WHERE ORGANIZATION_UNIT_ID=:id FOR UPDATE",
                Map.of("id", id), (rs,n) -> rs.getLong(1));
        if (found.isEmpty()) throw GradeException.notFound("واحد سازمانی یافت نشد.");
    }
    public String unitTypeCode(long id) {
        List<String> found = jdbc.query("SELECT TYPE_CODE FROM ORG.ORGANIZATION_UNIT_TYPES WHERE ORGANIZATION_UNIT_TYPE_ID=:id",
                Map.of("id", id), (rs,n) -> rs.getString(1));
        if (found.isEmpty()) throw GradeException.notFound("نوع واحد سازمانی یافت نشد.");
        return found.get(0);
    }
    public boolean organizationExists(long id) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM ORG.ORGANIZATIONS WHERE ORGANIZATION_ID=:id", Map.of("id", id), Long.class);
        return count != null && count != 0;
    }
    public List<Level> levels(Long organizationId, boolean onlyActive) {
        return jdbc.query("""
                SELECT * FROM ORG.ORGANIZATION_UNIT_GRADE_LEVELS
                 WHERE (:organizationId IS NULL OR ORGANIZATION_ID=:organizationId)
                   AND (:onlyActive=0 OR ACTIVE_FLAG=1)
                 ORDER BY ORGANIZATION_ID, GRADE_SCHEME_CODE, DISPLAY_ORDER, GRADE_CODE
                """, new MapSqlParameterSource().addValue("organizationId", organizationId)
                .addValue("onlyActive", onlyActive ? 1 : 0), LEVEL_ROW);
    }
    public Level level(long id) {
        List<Level> values = jdbc.query("SELECT * FROM ORG.ORGANIZATION_UNIT_GRADE_LEVELS WHERE ORGANIZATION_UNIT_GRADE_LEVEL_ID=:id",
                Map.of("id", id), LEVEL_ROW);
        if (values.isEmpty()) throw GradeException.notFound("درجه انتخاب‌شده وجود ندارد.");
        return values.get(0);
    }
    public long nextLevelId() {
        return jdbc.queryForObject("SELECT ORG.SEQ_ORGANIZATION_UNIT_GRADE_LEVELS.NEXTVAL FROM DUAL", Map.of(), Long.class);
    }
    public void insertLevel(long id, LevelInput value, String actor) {
        jdbc.update("""
            INSERT INTO ORG.ORGANIZATION_UNIT_GRADE_LEVELS
              (ORGANIZATION_UNIT_GRADE_LEVEL_ID, ORGANIZATION_ID, ORGANIZATION_UNIT_TYPE_ID,
               GRADE_SCHEME_CODE, GRADE_CODE, NAME_FA, NAME_EN, DISPLAY_ORDER, ACTIVE_FLAG, CREATED_BY)
            VALUES (:id,:organizationId,:unitTypeId,:scheme,:code,:fa,:en,:orderNo,:active,:actor)
            """, new MapSqlParameterSource().addValue("id", id)
                .addValue("organizationId", value.organizationId()).addValue("unitTypeId", value.organizationUnitTypeId())
                .addValue("scheme", value.gradeSchemeCode()).addValue("code", value.gradeCode())
                .addValue("fa", value.nameFa()).addValue("en", value.nameEn())
                .addValue("orderNo", value.displayOrder()).addValue("active", value.active() ? 1 : 0)
                .addValue("actor", actor));
    }
    public int updateLevel(long id, LevelInput value, String actor) {
        return jdbc.update("""
            UPDATE ORG.ORGANIZATION_UNIT_GRADE_LEVELS
               SET NAME_FA=:fa, NAME_EN=:en, DISPLAY_ORDER=:orderNo, ACTIVE_FLAG=:active,
                   UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, ROW_VERSION=ROW_VERSION+1
             WHERE ORGANIZATION_UNIT_GRADE_LEVEL_ID=:id AND ROW_VERSION=:expected
            """, new MapSqlParameterSource().addValue("id", id).addValue("expected", value.rowVersion())
                .addValue("fa", value.nameFa()).addValue("en", value.nameEn())
                .addValue("orderNo", value.displayOrder()).addValue("active", value.active() ? 1 : 0)
                .addValue("actor", actor));
    }
    public List<Period> periods(long unitId) {
        return jdbc.query(PERIOD_SELECT + " WHERE p.ORGANIZATION_UNIT_ID=:id ORDER BY p.EFFECTIVE_FROM DESC, p.ORGANIZATION_UNIT_GRADE_PERIOD_ID DESC",
                Map.of("id", unitId), PERIOD_ROW);
    }
    public List<Period> effective(long unitId) {
        return jdbc.query(PERIOD_SELECT + " WHERE p.ORGANIZATION_UNIT_ID=:id AND p.RECORD_STATE IN ('ACTIVE','APPROVED') ORDER BY p.EFFECTIVE_FROM",
                Map.of("id", unitId), PERIOD_ROW);
    }
    public Period period(long id) {
        List<Period> values = jdbc.query(PERIOD_SELECT + " WHERE p.ORGANIZATION_UNIT_GRADE_PERIOD_ID=:id",
                Map.of("id", id), PERIOD_ROW);
        if (values.isEmpty()) throw GradeException.notFound("رکورد درجه‌بندی یافت نشد.");
        return values.get(0);
    }
    public long nextPeriodId() {
        return jdbc.queryForObject("SELECT ORG.SEQ_ORGANIZATION_UNIT_GRADE_PERIODS.NEXTVAL FROM DUAL", Map.of(), Long.class);
    }
    /** Store directly-effective grade without inventing approval metadata. */
    public void insertActivePeriod(long id, AssignmentInput value, String reason, String actor) {
        jdbc.update("""
            INSERT INTO ORG.ORGANIZATION_UNIT_GRADE_PERIODS
               (ORGANIZATION_UNIT_GRADE_PERIOD_ID, ORGANIZATION_UNIT_ID,
                ORGANIZATION_UNIT_GRADE_LEVEL_ID, EFFECTIVE_FROM, EFFECTIVE_TO,
                CHANGE_REASON_CODE, DESCRIPTION_FA, RECORD_STATE, CREATED_BY)
            VALUES (:id, :unitId, :levelId, :fromDate, :toDate,
                    :reason, :description, 'ACTIVE', :actor)
            """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("unitId", value.organizationUnitId())
                .addValue("levelId", value.gradeLevelId())
                .addValue("fromDate", Date.valueOf(value.effectiveFrom()), Types.DATE)
                .addValue("toDate", value.effectiveTo() == null ? null : Date.valueOf(value.effectiveTo()), Types.DATE)
                .addValue("reason", reason)
                .addValue("description", value.descriptionFa())
                .addValue("actor", actor));
    }
    public int closePrevious(long id, long expected, LocalDate endDate, String actor) {
        return jdbc.update("""
            UPDATE ORG.ORGANIZATION_UNIT_GRADE_PERIODS
               SET EFFECTIVE_TO=:toDate, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, ROW_VERSION=ROW_VERSION+1
             WHERE ORGANIZATION_UNIT_GRADE_PERIOD_ID=:id AND ROW_VERSION=:expected
               AND RECORD_STATE IN ('ACTIVE','APPROVED')
               AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO>:toDate)
            """, new MapSqlParameterSource().addValue("id", id).addValue("expected", expected)
                .addValue("toDate", Date.valueOf(endDate), Types.DATE).addValue("actor", actor));
    }
    public Coverage coverage() {
        return jdbc.queryForObject("""
            SELECT COUNT(*) AS TOTAL,
                   SUM(CASE WHEN EXISTS (
                        SELECT 1 FROM ORG.ORGANIZATION_UNIT_GRADE_PERIODS p
                        WHERE p.ORGANIZATION_UNIT_ID=u.ORGANIZATION_UNIT_ID
                          AND p.RECORD_STATE IN ('ACTIVE','APPROVED')
                          AND p.EFFECTIVE_FROM<=TRUNC(CURRENT_DATE)
                          AND (p.EFFECTIVE_TO IS NULL OR p.EFFECTIVE_TO>TRUNC(CURRENT_DATE)))
                       THEN 1 ELSE 0 END) AS GRADED
              FROM ORG.ORGANIZATION_UNITS u
              JOIN ORG.ORGANIZATION_UNIT_TYPES t ON t.ORGANIZATION_UNIT_TYPE_ID=u.ORGANIZATION_UNIT_TYPE_ID
             WHERE t.TYPE_CODE='BRANCH' AND u.STATUS_CODE='ACTIVE'
            """, Map.of(), (rs, n) -> {
            long total = rs.getLong("TOTAL"), graded = rs.getLong("GRADED");
            return new Coverage(total, graded, total - graded);
        });
    }
}
