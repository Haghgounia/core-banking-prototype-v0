package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * R3-B2: Single-table organizational contact implementation. It NEVER addresses the staging table;
 * ORG.CONTACT_POINTS must become canonical through a separately authorized DBA cutover.
 * Activated exclusively via storage-mode=canonical. Writes require a second explicit opt-in.
 * The former association ID is the canonical CONTACT_POINT_ID for all 6844 migrated rows.
 * Audit actor PROTOTYPE is a transparent non-authenticated demo placeholder, NOT an identity claim.
 */
@Service
@ConditionalOnProperty(prefix = "core-banking.organization.contacts", name = "storage-mode",
        havingValue = "canonical")
public class CanonicalOrganizationContactService implements OrganizationContactOperations {
    private static final String AUDIT_ACTOR = "PROTOTYPE";
    private static final Set<String> TYPES = Set.of(
            "PHONE", "FAX", "MOBILE", "EMAIL", "WEBSITE", "CALL_CENTER", "SOCIAL", "OTHER");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9() ./#xX-]+$");
    private static final Pattern URL = Pattern.compile("^https?://[^\\s]+$", Pattern.CASE_INSENSITIVE);

    private static final String SELECT_ROWS = """
            SELECT c.CONTACT_POINT_ID AS ORGANIZATION_UNIT_CONTACT_POINT_ID,
                   c.CONTACT_POINT_ID, c.ORGANIZATION_UNIT_ID,
                   u.UNIT_CODE, u.NAME_FA AS UNIT_NAME_FA,
                   c.CONTACT_TYPE_CODE, c.CONTACT_VALUE, c.DISPLAY_NAME_FA, c.SOURCE_CODE,
                   c.CONTACT_ROLE_ID, r.ROLE_CODE, r.NAME_FA AS ROLE_NAME_FA,
                   c.PRIMARY_FLAG, c.EFFECTIVE_FROM, c.EFFECTIVE_TO, c.ROW_VERSION
              FROM ORG.CONTACT_POINTS c
              JOIN ORG.CONTACT_ROLES r ON r.CONTACT_ROLE_ID=c.CONTACT_ROLE_ID
              JOIN ORG.ORGANIZATION_UNITS u ON u.ORGANIZATION_UNIT_ID=c.ORGANIZATION_UNIT_ID
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final boolean allowWrites;

    public CanonicalOrganizationContactService(
            NamedParameterJdbcTemplate jdbc,
            @Value("${core-banking.organization.contacts.canonical-writes-enabled:false}") boolean allowWrites) {
        this.jdbc = jdbc;
        this.allowWrites = allowWrites;
    }

    @Override
    @Transactional(readOnly = true)
    public ContactReadiness readiness() {
        Long table = count("""
                SELECT COUNT(*) FROM ALL_TABLES
                 WHERE OWNER='ORG' AND TABLE_NAME='CONTACT_POINTS'
                """);
        Long columns = count("""
                SELECT COUNT(DISTINCT COLUMN_NAME) FROM ALL_TAB_COLUMNS
                 WHERE OWNER='ORG' AND TABLE_NAME='CONTACT_POINTS'
                   AND COLUMN_NAME IN
                     ('CONTACT_POINT_ID','ORGANIZATION_UNIT_ID','CONTACT_TYPE_CODE','CONTACT_VALUE',
                      'DISPLAY_NAME_FA','CONTACT_ROLE_ID','PRIMARY_FLAG','EFFECTIVE_FROM','EFFECTIVE_TO',
                      'SOURCE_CODE','CREATED_AT','CREATED_BY','UPDATED_AT','UPDATED_BY','ROW_VERSION')
                """);
        Long unitFk = fkCount("ORGANIZATION_UNIT_ID", "ORGANIZATION_UNITS");
        Long roleFk = fkCount("CONTACT_ROLE_ID", "CONTACT_ROLES");
        Long pk = count("""
                SELECT COUNT(*) FROM ALL_CONSTRAINTS pk
                  JOIN ALL_CONS_COLUMNS pc ON pc.OWNER=pk.OWNER
                     AND pc.CONSTRAINT_NAME=pk.CONSTRAINT_NAME
                 WHERE pk.OWNER='ORG' AND pk.TABLE_NAME='CONTACT_POINTS'
                   AND pk.CONSTRAINT_TYPE='P' AND pk.STATUS='ENABLED'
                   AND pk.VALIDATED='VALIDATED' AND pc.COLUMN_NAME='CONTACT_POINT_ID'
                """);
        Long version = count("""
                SELECT COUNT(*) FROM ALL_TAB_COLUMNS
                 WHERE OWNER='ORG' AND TABLE_NAME='CONTACT_POINTS' AND COLUMN_NAME='ROW_VERSION'
                """);
        boolean ready = equal(table, 1) && equal(columns, 15)
                && equal(unitFk, 1) && equal(roleFk, 1) && equal(pk, 1) && equal(version, 1);
        return new ContactReadiness(equal(table, 1), equal(unitFk, 1), false,
                equal(version, 1), ready, "CANONICAL", ready && allowWrites);
    }

    private Long fkCount(String column, String referencedTable) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM ALL_CONSTRAINTS fk
                  JOIN ALL_CONS_COLUMNS fc ON fc.OWNER=fk.OWNER
                    AND fc.CONSTRAINT_NAME=fk.CONSTRAINT_NAME
                  JOIN ALL_CONSTRAINTS target ON target.OWNER=fk.R_OWNER
                    AND target.CONSTRAINT_NAME=fk.R_CONSTRAINT_NAME
                 WHERE fk.OWNER='ORG' AND fk.TABLE_NAME='CONTACT_POINTS'
                   AND fk.CONSTRAINT_TYPE='R' AND fk.STATUS='ENABLED'
                   AND fk.VALIDATED='VALIDATED' AND fc.COLUMN_NAME=:fkColumn
                   AND target.OWNER='ORG' AND target.TABLE_NAME=:targetTable
                """, Map.of("fkColumn", column, "targetTable", referencedTable), Long.class);
    }

    private Long count(String sql) { return jdbc.queryForObject(sql, Map.of(), Long.class); }
    private static boolean equal(Long actual, long expected) { return actual != null && actual == expected; }

    @Override
    @Transactional(readOnly = true)
    public List<ContactRole> roles() {
        return jdbc.query("""
                SELECT CONTACT_ROLE_ID, ROLE_CODE, NAME_FA FROM ORG.CONTACT_ROLES
                 WHERE ACTIVE_FLAG=1 ORDER BY CONTACT_ROLE_ID
                """, Map.of(), (rs, i) -> new ContactRole(
                rs.getLong("CONTACT_ROLE_ID"), rs.getString("ROLE_CODE"), rs.getString("NAME_FA")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactRow> list(Long organizationUnitId) {
        long unitId = requirePositive(organizationUnitId, "واحد سازمانی را انتخاب کنید.");
        requireCanonicalSchema();
        assertUnitExists(unitId);
        return queryRows(unitId, null);
    }

    @Override
    @Transactional
    public ContactRow create(ContactInput raw) {
        requireWriteAccess();
        Validated input = validate(raw);
        requireCanonicalSchema();
        lockUnit(input.unitId());
        requireActiveRole(input.roleId());
        validateUniqueness(input, null);
        KeyHolder key = new GeneratedKeyHolder();
        int inserted = jdbc.update("""
                INSERT INTO ORG.CONTACT_POINTS
                  (ORGANIZATION_UNIT_ID, CONTACT_TYPE_CODE, CONTACT_VALUE, DISPLAY_NAME_FA,
                   CONTACT_ROLE_ID, PRIMARY_FLAG, EFFECTIVE_FROM, EFFECTIVE_TO,
                   SOURCE_CODE, CREATED_BY)
                VALUES (:unitId, :type, :value, :label, :roleId, :primaryFlag,
                        :effectiveFrom, :effectiveTo, 'MANUAL', :actor)
                """, params(input).addValue("actor", AUDIT_ACTOR), key,
                new String[]{"CONTACT_POINT_ID"});
        if (inserted != 1 || key.getKey() == null || key.getKey().longValue() <= 0)
            throw OrganizationContactException.conflict("شناسه تماس جدید از Oracle دریافت نشد.");
        return requireRow(input.unitId(), key.getKey().longValue());
    }

    @Override
    @Transactional
    public ContactRow update(long recordId, ContactInput raw) {
        if (recordId <= 0) throw OrganizationContactException.invalid("شناسه راه تماس معتبر نیست.");
        requireWriteAccess();
        Validated input = validate(raw);
        requireCanonicalSchema();
        lockUnit(input.unitId());
        List<ContactRow> existing = jdbc.query(SELECT_ROWS + """
                WHERE c.CONTACT_POINT_ID=:id AND c.ORGANIZATION_UNIT_ID=:unitId
                  FOR UPDATE OF c.CONTACT_ROLE_ID
                """, new MapSqlParameterSource().addValue("id", recordId)
                    .addValue("unitId", input.unitId()), (rs, i) -> toRow(rs));
        if (existing.size() != 1) throw OrganizationContactException.notFound(
                "راه تماس در واحد انتخاب‌شده یافت نشد. انتقال راه تماس به واحد دیگر مجاز نیست.");
        ContactRow old = existing.get(0);
        if (raw.expectedRowVersion() == null || raw.expectedRowVersion() != old.rowVersion())
            throw OrganizationContactException.conflict(
                    "نسخه راه تماس تغییر کرده است؛ اطلاعات را دوباره بارگذاری کنید.");
        requireActiveRole(input.roleId());
        validateUniqueness(input, recordId);
        int updated = jdbc.update("""
                UPDATE ORG.CONTACT_POINTS
                   SET CONTACT_TYPE_CODE=:type, CONTACT_VALUE=:value, DISPLAY_NAME_FA=:label,
                       CONTACT_ROLE_ID=:roleId, PRIMARY_FLAG=:primaryFlag,
                       EFFECTIVE_FROM=:effectiveFrom, EFFECTIVE_TO=:effectiveTo,
                       UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor,
                       ROW_VERSION=ROW_VERSION+1
                 WHERE CONTACT_POINT_ID=:id AND ORGANIZATION_UNIT_ID=:unitId
                   AND ROW_VERSION=:expectedVersion
                """, params(input).addValue("id", recordId)
                   .addValue("actor", AUDIT_ACTOR)
                   .addValue("expectedVersion", raw.expectedRowVersion()));
        if (updated != 1) throw OrganizationContactException.conflict(
                "رکورد راه تماس هم‌زمان تغییر کرده است؛ اطلاعات را دوباره بارگذاری کنید.");
        return requireRow(input.unitId(), recordId);
    }

    private void requireWriteAccess() {
        if (!allowWrites) throw OrganizationContactException.writesDisabled();
    }
    private void requireCanonicalSchema() {
        if (!readiness().ready()) throw OrganizationContactException.schemaMissing();
    }
    private void assertUnitExists(long id) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ORG.ORGANIZATION_UNITS WHERE ORGANIZATION_UNIT_ID=:id
                """, Map.of("id", id), Long.class);
        if (!equal(count, 1)) throw OrganizationContactException.notFound("واحد سازمانی یافت نشد.");
    }
    private void lockUnit(long id) {
        List<Long> found = jdbc.query("""
                SELECT ORGANIZATION_UNIT_ID FROM ORG.ORGANIZATION_UNITS
                 WHERE ORGANIZATION_UNIT_ID=:id FOR UPDATE
                """, Map.of("id", id), (rs, i) -> rs.getLong(1));
        if (found.size() != 1) throw OrganizationContactException.notFound("واحد سازمانی یافت نشد.");
    }
    private void requireActiveRole(long roleId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ORG.CONTACT_ROLES
                 WHERE CONTACT_ROLE_ID=:id AND ACTIVE_FLAG=1
                """, Map.of("id", roleId), Long.class);
        if (!equal(count, 1)) throw OrganizationContactException.invalid(
                "نقش انتخاب‌شده برای راه تماس فعال یا معتبر نیست.");
    }

    private List<ContactRow> queryRows(long unitId, Long recordId) {
        String sql = SELECT_ROWS + " WHERE c.ORGANIZATION_UNIT_ID=:unitId "
                + (recordId == null ? "" : " AND c.CONTACT_POINT_ID=:id ")
                + " ORDER BY c.PRIMARY_FLAG DESC, r.CONTACT_ROLE_ID, c.CONTACT_TYPE_CODE, "
                + "c.EFFECTIVE_FROM DESC NULLS LAST";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("unitId", unitId);
        if (recordId != null) p.addValue("id", recordId);
        return jdbc.query(sql, p, (rs, i) -> toRow(rs));
    }
    private ContactRow requireRow(long unitId, long recordId) {
        List<ContactRow> rows = queryRows(unitId, recordId);
        if (rows.size() != 1) throw OrganizationContactException.notFound("راه تماس موردنظر یافت نشد.");
        return rows.get(0);
    }
    private static ContactRow toRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("CONTACT_POINT_ID");
        // Stable, backwards-compatible association ID alias. These are intentionally identical.
        return new ContactRow(id, id, rs.getLong("ORGANIZATION_UNIT_ID"),
                rs.getString("UNIT_CODE"), rs.getString("UNIT_NAME_FA"),
                rs.getString("CONTACT_TYPE_CODE"), rs.getString("CONTACT_VALUE"),
                rs.getString("DISPLAY_NAME_FA"), rs.getLong("CONTACT_ROLE_ID"),
                rs.getString("ROLE_CODE"), rs.getString("ROLE_NAME_FA"),
                rs.getInt("PRIMARY_FLAG") == 1,
                readDate(rs, "EFFECTIVE_FROM"), readDate(rs, "EFFECTIVE_TO"),
                rs.getString("SOURCE_CODE"), rs.getLong("ROW_VERSION"));
    }
    private static LocalDate readDate(ResultSet rs, String name) throws SQLException {
        Date date = rs.getDate(name);
        return date == null ? null : date.toLocalDate();
    }

    private void validateUniqueness(Validated input, Long excludedId) {
        MapSqlParameterSource p = params(input)
                .addValue("valueKey", phoneType(input.type())
                        ? input.value().replaceAll("[^0-9]", "")
                        : input.value().toUpperCase(Locale.ROOT))
                .addValue("excludedId", excludedId, Types.NUMERIC);
        String base = """
                FROM ORG.CONTACT_POINTS c
                WHERE c.ORGANIZATION_UNIT_ID=:unitId AND c.CONTACT_ROLE_ID=:roleId
                  AND c.CONTACT_TYPE_CODE=:type
                  AND (:excludedId IS NULL OR c.CONTACT_POINT_ID<>:excludedId)
                  AND NVL(c.EFFECTIVE_FROM, DATE '0001-01-01') <= NVL(:effectiveTo, DATE '9999-12-31')
                  AND NVL(:effectiveFrom, DATE '0001-01-01') <= NVL(c.EFFECTIVE_TO, DATE '9999-12-31')
                """;
        String normalizedValue = phoneType(input.type())
                ? " AND REGEXP_REPLACE(c.CONTACT_VALUE, '[^0-9]', '')=:valueKey"
                : " AND UPPER(TRIM(c.CONTACT_VALUE))=:valueKey";
        Long duplicates = jdbc.queryForObject("SELECT COUNT(*) " + base + normalizedValue, p, Long.class);
        if (duplicates != null && duplicates > 0) throw OrganizationContactException.conflict(
                "این راه تماس با همین نقش و دوره اعتبار برای واحد قبلاً ثبت شده است.");
        if (input.primary()) {
            Long others = jdbc.queryForObject("SELECT COUNT(*) " + base +
                    " AND c.PRIMARY_FLAG=1", p, Long.class);
            if (others != null && others > 0) throw OrganizationContactException.conflict(
                    "برای این واحد، نقش و نوع تماس در دوره انتخاب‌شده راه تماس اصلی دیگری وجود دارد.");
        }
    }

    private static MapSqlParameterSource params(Validated input) {
        return new MapSqlParameterSource().addValue("unitId", input.unitId())
                .addValue("type", input.type()).addValue("value", input.value())
                .addValue("label", input.label()).addValue("roleId", input.roleId())
                .addValue("primaryFlag", input.primary() ? 1 : 0)
                .addValue("effectiveFrom", sqlDate(input.from()), Types.DATE)
                .addValue("effectiveTo", sqlDate(input.to()), Types.DATE);
    }
    private static Date sqlDate(LocalDate value) { return value == null ? null : Date.valueOf(value); }
    private record Validated(long unitId, String type, String value, long roleId,
                             boolean primary, LocalDate from, LocalDate to, String label) {}

    private static Validated validate(ContactInput input) {
        if (input == null) throw OrganizationContactException.invalid("اطلاعات راه تماس ارسال نشده است.");
        long unitId = requirePositive(input.organizationUnitId(), "واحد سازمانی معتبر نیست.");
        long roleId = requirePositive(input.contactRoleId(), "نقش راه تماس معتبر نیست.");
        String type = input.contactTypeCode() == null ? "" : input.contactTypeCode().trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw OrganizationContactException.invalid("نوع راه تماس انتخاب‌شده معتبر نیست.");
        String value = input.contactValue() == null ? "" : input.contactValue().trim();
        if (value.isBlank() || value.length() > 320)
            throw OrganizationContactException.invalid("مقدار راه تماس باید بین ۱ تا ۳۲۰ نویسه باشد.");
        if (type.equals("EMAIL")) {
            if (!EMAIL.matcher(value).matches()) throw OrganizationContactException.invalid("نشانی ایمیل معتبر نیست.");
            value = value.toLowerCase(Locale.ROOT);
        } else if (type.equals("WEBSITE") || type.equals("SOCIAL")) {
            if (!URL.matcher(value).matches())
                throw OrganizationContactException.invalid("نشانی وب باید با http:// یا https:// آغاز شود.");
        } else if (phoneType(type)) {
            value = asciiDigits(value);
            if (value.length() > 64 || !PHONE.matcher(value).matches()
                    || value.chars().filter(Character::isDigit).count() < 3)
                throw OrganizationContactException.invalid("شماره تماس واردشده معتبر نیست.");
        }
        String label = input.displayNameFa() == null ? "" : input.displayNameFa().trim();
        if (label.length() > 150) throw OrganizationContactException.invalid("عنوان راه تماس بیش از ۱۵۰ نویسه است.");
        LocalDate from = input.effectiveFrom();
        LocalDate to = input.effectiveTo();
        if (from != null && to != null && to.isBefore(from))
            throw OrganizationContactException.invalid("تاریخ پایان اعتبار نمی‌تواند پیش از تاریخ شروع باشد.");
        return new Validated(unitId, type, value, roleId, Boolean.TRUE.equals(input.primaryFlag()),
                from, to, label.isEmpty() ? null : label);
    }
    private static boolean phoneType(String type) {
        return type.equals("PHONE") || type.equals("FAX") || type.equals("MOBILE")
                || type.equals("CALL_CENTER");
    }
    private static long requirePositive(Long value, String message) {
        if (value == null || value < 1 || value >= 100_000_000L)
            throw OrganizationContactException.invalid(message);
        return value;
    }
    private static String asciiDigits(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            int digit = Character.digit(c, 10);
            result.append(digit >= 0 ? (char) ('0' + digit) : c);
        }
        return result.toString();
    }
}
