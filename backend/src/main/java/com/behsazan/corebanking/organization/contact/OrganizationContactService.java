package com.behsazan.corebanking.organization.contact;

import com.behsazan.corebanking.organization.contact.OrganizationContactModels.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Organizational (not CIF/party) contact endpoint management.
 * A new immutable contact-point master row is created when its value changes,
 * so two units sharing an endpoint never inadvertently change one another's details.
 */
@Service
@ConditionalOnProperty(prefix = "core-banking.organization.contacts", name = "storage-mode",
        havingValue = "legacy", matchIfMissing = true)
public class OrganizationContactService implements OrganizationContactOperations {
    private static final Set<String> TYPES = Set.of(
            "PHONE", "FAX", "MOBILE", "EMAIL", "WEBSITE", "CALL_CENTER", "SOCIAL", "OTHER");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9() ./#xX-]+$");
    private static final Pattern URL = Pattern.compile("^https?://[^\\s]+$", Pattern.CASE_INSENSITIVE);

    private final NamedParameterJdbcTemplate jdbc;

    public OrganizationContactService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public ContactReadiness readiness() {
        Long master = jdbc.queryForObject("SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER='ORG' AND TABLE_NAME='CONTACT_POINTS'",
                Map.of(), Long.class);
        Long unitFk = jdbc.queryForObject("SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='ORG' AND TABLE_NAME='ORGANIZATION_UNIT_CONTACT_POINTS' AND CONSTRAINT_NAME='FK_OUCP_CONTACT_MASTER' AND CONSTRAINT_TYPE='R' AND STATUS='ENABLED'",
                Map.of(), Long.class);
        Long serviceFk = jdbc.queryForObject("SELECT COUNT(*) FROM ALL_CONSTRAINTS WHERE OWNER='ORG' AND TABLE_NAME='SERVICE_POINT_CONTACT_POINTS' AND CONSTRAINT_NAME='FK_SPCP_CONTACT_MASTER' AND CONSTRAINT_TYPE='R' AND STATUS='ENABLED'",
                Map.of(), Long.class);
        Long rowVersion = jdbc.queryForObject("SELECT COUNT(*) FROM ALL_TAB_COLUMNS WHERE OWNER='ORG' AND TABLE_NAME='ORGANIZATION_UNIT_CONTACT_POINTS' AND COLUMN_NAME='ROW_VERSION'",
                Map.of(), Long.class);
        boolean hasRowVersion = rowVersion != null && rowVersion == 1;
        boolean hasMaster = master != null && master == 1;
        boolean hasUnitFk = unitFk != null && unitFk == 1;
        boolean hasServiceFk = serviceFk != null && serviceFk == 1;
        return new ContactReadiness(hasMaster, hasUnitFk, hasServiceFk, hasRowVersion,
                hasMaster && hasUnitFk && hasServiceFk && hasRowVersion, "LEGACY", true);
    }

    @Transactional(readOnly = true)
    public List<ContactRole> roles() {
        return jdbc.query("""
                SELECT CONTACT_ROLE_ID, ROLE_CODE, NAME_FA
                  FROM ORG.CONTACT_ROLES
                 WHERE ACTIVE_FLAG=1 ORDER BY CONTACT_ROLE_ID
                """, Map.of(), (rs, i) -> new ContactRole(
                rs.getLong("CONTACT_ROLE_ID"), rs.getString("ROLE_CODE"), rs.getString("NAME_FA")));
    }

    @Transactional(readOnly = true)
    public List<ContactRow> list(Long organizationUnitId) {
        long id = requirePositive(organizationUnitId, "واحد سازمانی را انتخاب کنید.");
        requireSchema();
        assertUnitExists(id);
        return queryRows(id, null);
    }

    @Transactional
    public ContactRow create(ContactInput raw) {
        Validated input = validate(raw);
        requireSchema();
        lockUnit(input.unitId());
        requireActiveRole(input.roleId());
        validateAssignmentUniqueness(input, null);
        long masterId = nextId("ORG.SEQ_CONTACT_POINTS");
        insertMaster(masterId, input);
        long assignmentId = nextId("ORG.SEQ_ORGANIZATION_UNIT_CONTACT_POINTS");
        jdbc.update("""
                INSERT INTO ORG.ORGANIZATION_UNIT_CONTACT_POINTS
                  (ORGANIZATION_UNIT_CONTACT_POINT_ID, ORGANIZATION_UNIT_ID, CONTACT_POINT_ID,
                   CONTACT_ROLE_ID, PRIMARY_FLAG, EFFECTIVE_FROM, EFFECTIVE_TO)
                VALUES (:id,:unitId,:contactPointId,:roleId,:primaryFlag,:effectiveFrom,:effectiveTo)
                """, assignmentParams(assignmentId, input, masterId));
        return requireRow(input.unitId(), assignmentId);
    }

    @Transactional
    public ContactRow update(long assignmentId, ContactInput raw) {
        if (assignmentId <= 0) throw OrganizationContactException.invalid("شناسه راه تماس معتبر نیست.");
        Validated input = validate(raw);
        requireSchema();
        lockUnit(input.unitId());
        // Lock existing assignment after parent lock to serialize writes for the same unit.
        List<ContactRow> existing = jdbc.query(SELECT_ROWS + """
                WHERE a.ORGANIZATION_UNIT_CONTACT_POINT_ID=:assignmentId AND a.ORGANIZATION_UNIT_ID=:unitId
                  FOR UPDATE OF a.CONTACT_ROLE_ID
                """, new MapSqlParameterSource().addValue("assignmentId", assignmentId)
                .addValue("unitId", input.unitId()), (rs, i) -> toRow(rs));
        if (existing.isEmpty()) throw OrganizationContactException.notFound(
                "راه تماس در واحد انتخاب‌شده یافت نشد. جابه‌جایی انتساب بین واحدها مجاز نیست.");
        ContactRow old = existing.get(0);
        if (raw.expectedRowVersion() == null || raw.expectedRowVersion() != old.rowVersion())
            throw OrganizationContactException.conflict("راه تماس در همین فاصله تغییر کرده است. اطلاعات را دوباره بارگذاری کنید.");
        requireActiveRole(input.roleId());
        validateAssignmentUniqueness(input, assignmentId);
        boolean endpointChanged = !old.contactTypeCode().equals(input.type())
                || !old.contactValue().equals(input.value())
                || !normalizeLabel(old.displayNameFa()).equals(normalizeLabel(input.label()));
        long masterId = old.contactPointId();
        if (endpointChanged) {
            masterId = nextId("ORG.SEQ_CONTACT_POINTS");
            insertMaster(masterId, input); // Never UPDATE a contact point used by other units.
        }
        int count = jdbc.update("""
                UPDATE ORG.ORGANIZATION_UNIT_CONTACT_POINTS
                   SET CONTACT_POINT_ID=:contactPointId, CONTACT_ROLE_ID=:roleId,
                       PRIMARY_FLAG=:primaryFlag, EFFECTIVE_FROM=:effectiveFrom,
                       EFFECTIVE_TO=:effectiveTo, ROW_VERSION=ROW_VERSION+1
                 WHERE ORGANIZATION_UNIT_CONTACT_POINT_ID=:id AND ORGANIZATION_UNIT_ID=:unitId
                   AND ROW_VERSION=:expectedVersion
                """, assignmentParams(assignmentId, input, masterId)
                  .addValue("expectedVersion", raw.expectedRowVersion()));
        if (count != 1) throw OrganizationContactException.conflict("رکورد راه تماس به‌روزرسانی نشد.");
        return requireRow(input.unitId(), assignmentId);
    }

    private static final String SELECT_ROWS = """
            SELECT a.ORGANIZATION_UNIT_CONTACT_POINT_ID, a.CONTACT_POINT_ID,
                   a.ORGANIZATION_UNIT_ID, u.UNIT_CODE, u.NAME_FA AS UNIT_NAME_FA,
                   m.CONTACT_TYPE_CODE, m.CONTACT_VALUE, m.DISPLAY_NAME_FA, m.SOURCE_CODE,
                   a.CONTACT_ROLE_ID, r.ROLE_CODE, r.NAME_FA AS ROLE_NAME_FA,
                   a.PRIMARY_FLAG, a.EFFECTIVE_FROM, a.EFFECTIVE_TO, a.ROW_VERSION
              FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS a
              JOIN ORG.CONTACT_POINTS m ON m.CONTACT_POINT_ID=a.CONTACT_POINT_ID
              JOIN ORG.CONTACT_ROLES r ON r.CONTACT_ROLE_ID=a.CONTACT_ROLE_ID
              JOIN ORG.ORGANIZATION_UNITS u ON u.ORGANIZATION_UNIT_ID=a.ORGANIZATION_UNIT_ID
            """;

    private List<ContactRow> queryRows(long unitId, Long assignmentId) {
        // Avoid an untyped NULL JDBC bind in Oracle's `:assignmentId IS NULL` predicate.
        String sql = SELECT_ROWS
                + " WHERE a.ORGANIZATION_UNIT_ID=:unitId "
                + (assignmentId == null ? "" : " AND a.ORGANIZATION_UNIT_CONTACT_POINT_ID=:assignmentId ")
                + " ORDER BY a.PRIMARY_FLAG DESC, r.CONTACT_ROLE_ID, m.CONTACT_TYPE_CODE, "
                + "a.EFFECTIVE_FROM DESC NULLS LAST";
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("unitId", unitId);
        if (assignmentId != null) parameters.addValue("assignmentId", assignmentId);
        return jdbc.query(sql, parameters, (rs, i) -> toRow(rs));
    }

    private ContactRow requireRow(long unitId, long id) {
        List<ContactRow> rows = queryRows(unitId, id);
        if (rows.size() != 1) throw OrganizationContactException.notFound("راه تماس موردنظر یافت نشد.");
        return rows.get(0);
    }

    private static ContactRow toRow(ResultSet rs) throws SQLException {
        return new ContactRow(
                rs.getLong("ORGANIZATION_UNIT_CONTACT_POINT_ID"), rs.getLong("CONTACT_POINT_ID"),
                rs.getLong("ORGANIZATION_UNIT_ID"), rs.getString("UNIT_CODE"), rs.getString("UNIT_NAME_FA"),
                rs.getString("CONTACT_TYPE_CODE"), rs.getString("CONTACT_VALUE"), rs.getString("DISPLAY_NAME_FA"),
                rs.getLong("CONTACT_ROLE_ID"), rs.getString("ROLE_CODE"), rs.getString("ROLE_NAME_FA"),
                rs.getInt("PRIMARY_FLAG") == 1, readDate(rs, "EFFECTIVE_FROM"),
                readDate(rs, "EFFECTIVE_TO"), rs.getString("SOURCE_CODE"), rs.getLong("ROW_VERSION"));
    }

    private static LocalDate readDate(ResultSet rs, String col) throws SQLException {
        Date date = rs.getDate(col);
        return date == null ? null : date.toLocalDate();
    }

    private void validateAssignmentUniqueness(Validated input, Long excludedId) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("unitId", input.unitId()).addValue("roleId", input.roleId())
                .addValue("contactType", input.type()).addValue("contactValueKey",
                        phoneType(input.type()) ? input.value().replaceAll("[^0-9]", "")
                                : input.value().toUpperCase(Locale.ROOT))
                .addValue("effectiveFrom", sqlDate(input.from()), java.sql.Types.DATE)
                .addValue("effectiveTo", sqlDate(input.to()), java.sql.Types.DATE)
                .addValue("excludedId", excludedId, java.sql.Types.NUMERIC);
        // Inclusive effective-date endpoints (same as existing ORG CHECK constraints).
        String conditions = """
                FROM ORG.ORGANIZATION_UNIT_CONTACT_POINTS a
                JOIN ORG.CONTACT_POINTS m ON m.CONTACT_POINT_ID=a.CONTACT_POINT_ID
                WHERE a.ORGANIZATION_UNIT_ID=:unitId AND a.CONTACT_ROLE_ID=:roleId
                  AND m.CONTACT_TYPE_CODE=:contactType
                  AND (:excludedId IS NULL OR a.ORGANIZATION_UNIT_CONTACT_POINT_ID<>:excludedId)
                  AND NVL(a.EFFECTIVE_FROM, DATE '0001-01-01') <= NVL(:effectiveTo, DATE '9999-12-31')
                  AND NVL(:effectiveFrom, DATE '0001-01-01') <= NVL(a.EFFECTIVE_TO, DATE '9999-12-31')
                """;
        String valueComparison = phoneType(input.type())
                ? " AND REGEXP_REPLACE(m.CONTACT_VALUE, '[^0-9]', '')=:contactValueKey"
                : " AND UPPER(TRIM(m.CONTACT_VALUE))=:contactValueKey";
        Long exactDuplicates = jdbc.queryForObject("SELECT COUNT(*) " + conditions
                + valueComparison, p, Long.class);
        if (exactDuplicates != null && exactDuplicates > 0)
            throw OrganizationContactException.conflict("این راه تماس با همین نقش و دوره اعتبار برای واحد قبلاً ثبت شده است.");
        if (input.primary()) {
            Long otherPrimaries = jdbc.queryForObject("SELECT COUNT(*) " + conditions
                    + " AND a.PRIMARY_FLAG=1", p, Long.class);
            if (otherPrimaries != null && otherPrimaries > 0)
                throw OrganizationContactException.conflict(
                        "برای این واحد، نقش و نوع تماس در دوره انتخاب‌شده راه تماس اصلی دیگری وجود دارد.");
        }
    }

    private void insertMaster(long id, Validated input) {
        jdbc.update("""
                INSERT INTO ORG.CONTACT_POINTS
                  (CONTACT_POINT_ID, CONTACT_TYPE_CODE, CONTACT_VALUE, DISPLAY_NAME_FA, SOURCE_CODE)
                VALUES (:id,:type,:value,:label,'MANUAL')
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("type", input.type()).addValue("value", input.value())
                .addValue("label", input.label()));
    }

    private MapSqlParameterSource assignmentParams(long id, Validated input, long masterId) {
        return new MapSqlParameterSource().addValue("id", id).addValue("unitId", input.unitId())
                .addValue("contactPointId", masterId).addValue("roleId", input.roleId())
                .addValue("primaryFlag", input.primary() ? 1 : 0)
                .addValue("effectiveFrom", sqlDate(input.from()), java.sql.Types.DATE)
                .addValue("effectiveTo", sqlDate(input.to()), java.sql.Types.DATE);
    }

    private void requireSchema() {
        if (!readiness().ready()) throw OrganizationContactException.schemaMissing();
    }

    private void assertUnitExists(long id) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM ORG.ORGANIZATION_UNITS WHERE ORGANIZATION_UNIT_ID=:id",
                Map.of("id", id), Long.class);
        if (count == null || count != 1) throw OrganizationContactException.notFound("واحد سازمانی یافت نشد.");
    }

    private void lockUnit(long id) {
        List<Long> found = jdbc.query("""
                SELECT ORGANIZATION_UNIT_ID FROM ORG.ORGANIZATION_UNITS
                 WHERE ORGANIZATION_UNIT_ID=:id FOR UPDATE
                """, Map.of("id", id), (rs, i) -> rs.getLong(1));
        if (found.isEmpty()) throw OrganizationContactException.notFound("واحد سازمانی یافت نشد.");
    }

    private void requireActiveRole(long roleId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM ORG.CONTACT_ROLES WHERE CONTACT_ROLE_ID=:id AND ACTIVE_FLAG=1",
                Map.of("id", roleId), Long.class);
        if (count == null || count != 1)
            throw OrganizationContactException.invalid("نقش انتخاب‌شده برای راه تماس فعال یا معتبر نیست.");
    }

    private long nextId(String sequence) {
        // Only constants provided by this class; never sourced from HTTP/user input.
        if (!sequence.equals("ORG.SEQ_CONTACT_POINTS")
                && !sequence.equals("ORG.SEQ_ORGANIZATION_UNIT_CONTACT_POINTS"))
            throw new IllegalArgumentException("Unexpected sequence");
        Long id = jdbc.queryForObject("SELECT " + sequence + ".NEXTVAL FROM DUAL", Map.of(), Long.class);
        if (id == null || id < 1 || id >= 100_000_000L)
            throw OrganizationContactException.conflict("دامنه شناسه راه تماس به پایان رسیده است.");
        return id;
    }

    private static boolean phoneType(String type) {
        return type.equals("PHONE") || type.equals("FAX") || type.equals("MOBILE")
                || type.equals("CALL_CENTER");
    }

    private static Date sqlDate(LocalDate value) { return value == null ? null : Date.valueOf(value); }
    private static long requirePositive(Long value, String message) {
        if (value == null || value < 1 || value >= 100_000_000L)
            throw OrganizationContactException.invalid(message);
        return value;
    }
    private static String normalizeLabel(String value) { return value == null ? "" : value.trim(); }

    private record Validated(long unitId, String type, String value, long roleId,
                             boolean primary, LocalDate from, LocalDate to, String label) {}

    private static Validated validate(ContactInput input) {
        if (input == null) throw OrganizationContactException.invalid("اطلاعات راه تماس ارسال نشده است.");
        long unitId = requirePositive(input.organizationUnitId(), "واحد سازمانی معتبر نیست.");
        long roleId = requirePositive(input.contactRoleId(), "نقش راه تماس معتبر نیست.");
        String type = input.contactTypeCode() == null ? "" : input.contactTypeCode().trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type))
            throw OrganizationContactException.invalid("نوع راه تماس انتخاب‌شده معتبر نیست.");
        String value = input.contactValue() == null ? "" : input.contactValue().trim();
        if (value.isBlank() || value.length() > 320)
            throw OrganizationContactException.invalid("مقدار راه تماس باید بین ۱ تا ۳۲۰ نویسه باشد.");
        if (type.equals("EMAIL")) {
            if (!EMAIL.matcher(value).matches())
                throw OrganizationContactException.invalid("نشانی ایمیل معتبر نیست.");
            value = value.toLowerCase(Locale.ROOT);
        } else if (type.equals("WEBSITE") || type.equals("SOCIAL")) {
            if (!URL.matcher(value).matches())
                throw OrganizationContactException.invalid("نشانی وب باید با http:// یا https:// آغاز شود.");
        } else if (type.equals("PHONE") || type.equals("FAX") || type.equals("MOBILE") || type.equals("CALL_CENTER")) {
            value = asciiDigits(value);
            if (value.length() > 64 || !PHONE.matcher(value).matches()
                    || value.chars().filter(Character::isDigit).count() < 3)
                throw OrganizationContactException.invalid("شماره تماس واردشده معتبر نیست.");
        }
        String label = normalizeLabel(input.displayNameFa());
        if (label.length() > 150)
            throw OrganizationContactException.invalid("عنوان راه تماس بیش از ۱۵۰ نویسه است.");
        LocalDate from = input.effectiveFrom();
        LocalDate to = input.effectiveTo();
        if (from != null && to != null && to.isBefore(from))
            throw OrganizationContactException.invalid("تاریخ پایان اعتبار نمی‌تواند پیش از تاریخ شروع باشد.");
        return new Validated(unitId, type, value, roleId,
                Boolean.TRUE.equals(input.primaryFlag()), from, to, label.isEmpty() ? null : label);
    }

    private static String asciiDigits(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            int digit = Character.digit(character, 10);
            out.append(digit >= 0 ? (char) ('0' + digit) : character);
        }
        return out.toString();
    }
}
