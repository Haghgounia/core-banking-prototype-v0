package com.behsazan.corebanking.calendar2.eventrecurrence.oracle;

import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.CalendarMonthOption;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.OccurrenceFilterMeta;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.OccurrenceSummary;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.RuleDefinition;
import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.RuleSummary;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class Calendar2EventRecurrenceRepository {
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");

    private final JdbcClient jdbcClient;
    private final String schema;

    public Calendar2EventRecurrenceRepository(
            JdbcClient jdbcClient,
            @Value("${core-banking.schemas.calendar2:CAL2}") String schemaName
    ) {
        this.jdbcClient = jdbcClient;
        this.schema = safeIdentifier(schemaName == null ? null : schemaName.trim().toUpperCase());
    }

    public PageResponse<RuleSummary> searchRuleSummaries(String text, int page, int size, String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String search = text == null ? "" : text.trim();
        Map<String, Object> params = new LinkedHashMap<>();
        String where = "";
        if (!search.isBlank()) {
            where = """
                     WHERE (UPPER(E.EVENT_CODE) LIKE :text
                        OR UPPER(E.NAME_FA) LIKE :text
                        OR UPPER(NVL(E.NAME_EN, '')) LIKE :text
                        OR UPPER(V.VARIANT_CODE) LIKE :text
                        OR UPPER(S.CALENDAR_CODE) LIKE :text
                        OR UPPER(S.NAME_FA) LIKE :text
                        OR UPPER(NVL(M.NAME_FA, '')) LIKE :text
                        OR UPPER(R.RULE_TYPE) LIKE :text
                        OR TO_CHAR(R.MONTH_NO) LIKE :textRaw
                        OR TO_CHAR(R.DAY_NO) LIKE :textRaw
                        OR TO_CHAR(R.YEAR_NO) LIKE :textRaw)
                    """;
            params.put("text", "%" + search.toUpperCase(Locale.ROOT) + "%");
            params.put("textRaw", "%" + search + "%");
        }

        String from = " FROM " + table("EVENT_RECURRENCE_RULE") + " R"
                + " JOIN " + table("EVENT") + " E ON E.EVENT_ID = R.EVENT_ID"
                + " JOIN " + table("CALENDAR_VARIANT") + " V ON V.CALENDAR_VARIANT_ID = R.CALENDAR_VARIANT_ID"
                + " JOIN " + table("CALENDAR_SYSTEM") + " S ON S.CALENDAR_SYSTEM_ID = V.CALENDAR_SYSTEM_ID"
                + " LEFT JOIN " + table("CALENDAR_MONTH") + " M ON M.CALENDAR_SYSTEM_ID = S.CALENDAR_SYSTEM_ID AND M.MONTH_NO = R.MONTH_NO";

        long total = jdbcClient.sql("SELECT COUNT(*)" + from + where).params(params).query(Long.class).single();

        String occurrenceCount = "NVL(O.GENERATED_OCCURRENCES, 0)";
        String sql = """
                SELECT R.EVENT_RULE_ID, R.EVENT_ID, E.EVENT_CODE, E.NAME_FA AS EVENT_NAME,
                       R.RULE_TYPE, R.CALENDAR_VARIANT_ID, V.VARIANT_CODE,
                       S.CALENDAR_CODE, S.NAME_FA AS CALENDAR_NAME, M.NAME_FA AS MONTH_NAME,
                       R.YEAR_NO, R.MONTH_NO, R.DAY_NO, R.START_YEAR_NO, R.END_YEAR_NO,
                       R.DAY_RESOLUTION_POLICY, R.ACTIVE_FLAG, %s AS GENERATED_OCCURRENCES
                %s
                LEFT JOIN (
                    SELECT EVENT_RULE_ID, COUNT(*) AS GENERATED_OCCURRENCES
                      FROM %s
                     WHERE OCCURRENCE_SOURCE = 'GENERATED'
                     GROUP BY EVENT_RULE_ID
                ) O ON O.EVENT_RULE_ID = R.EVENT_RULE_ID
                %s
                ORDER BY %s %s
                OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY
                """.formatted(
                occurrenceCount, from, table("EVENT_OCCURRENCE"), where,
                summarySort(sortBy, occurrenceCount), normalizedDirection(direction)
        );
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);
        List<RuleSummary> rows = jdbcClient.sql(sql).params(params).query((rs, rowNum) -> mapRuleSummary(rs)).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public PageResponse<OccurrenceSummary> searchOccurrenceSummaries(
            String text, Integer solarYear, Long eventId, String occurrenceSource, Boolean holiday,
            int page, int size, String sortBy, String direction
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String search = text == null ? "" : text.trim();
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");

        if (!search.isBlank()) {
            where.append("""
                     AND (UPPER(E.EVENT_CODE) LIKE :text
                       OR UPPER(E.NAME_FA) LIKE :text
                       OR UPPER(NVL(E.NAME_EN, '')) LIKE :text
                       OR UPPER(NVL(ET.NAME_FA, '')) LIKE :text
                       OR UPPER(NVL(SA.NAME_FA, '')) LIKE :text
                       OR UPPER(NVL(SA.SOURCE_CODE, '')) LIKE :text
                       OR UPPER(NVL(DV.VERSION_CODE, '')) LIKE :text
                       OR UPPER(NVL(D.ISO_DATE_TEXT, '')) LIKE :text
                       OR UPPER(NVL(PM.NAME_FA, '')) LIKE :text
                       OR UPPER(NVL(GM.NAME_FA, '')) LIKE :text
                       OR UPPER(NVL(IM.NAME_FA, '')) LIKE :text
                       OR TO_CHAR(PCD.YEAR_NO) LIKE :textRaw
                       OR TO_CHAR(GCD.YEAR_NO) LIKE :textRaw
                       OR TO_CHAR(ICD.YEAR_NO) LIKE :textRaw)
                    """);
            params.put("text", "%" + search.toUpperCase(Locale.ROOT) + "%");
            params.put("textRaw", "%" + search + "%");
        }
        if (solarYear != null) {
            where.append(" AND PCD.YEAR_NO = :solarYear");
            params.put("solarYear", solarYear);
        }
        if (eventId != null) {
            where.append(" AND EO.EVENT_ID = :eventId");
            params.put("eventId", eventId);
        }
        if (occurrenceSource != null && !occurrenceSource.isBlank()) {
            where.append(" AND EO.OCCURRENCE_SOURCE = :occurrenceSource");
            params.put("occurrenceSource", occurrenceSource);
        }
        if (holiday != null) {
            where.append(" AND EO.HOLIDAY_FLAG = :holidayFlag");
            params.put("holidayFlag", holiday ? "Y" : "N");
        }

        String variantsCte = defaultVariantsCte();
        String from = occurrenceSummaryFrom();
        long total = jdbcClient.sql(variantsCte + " SELECT COUNT(*) " + from + where)
                .params(params).query(Long.class).single();

        String sql = variantsCte + """
                SELECT EO.EVENT_OCCURRENCE_ID, EO.EVENT_ID, EO.EVENT_RULE_ID,
                       E.EVENT_CODE, E.NAME_FA AS EVENT_NAME, ET.NAME_FA AS EVENT_TYPE_NAME,
                       EO.DAY_ID, D.ISO_DATE_TEXT AS CANONICAL_ISO_DATE, W.NAME_FA AS WEEKDAY_NAME,
                       PCD.YEAR_NO AS SOLAR_YEAR, PCD.MONTH_NO AS SOLAR_MONTH_NO, PCD.DAY_NO AS SOLAR_DAY_NO, PM.NAME_FA AS SOLAR_MONTH_NAME,
                       GCD.YEAR_NO AS GREGORIAN_YEAR, GCD.MONTH_NO AS GREGORIAN_MONTH_NO, GCD.DAY_NO AS GREGORIAN_DAY_NO, GM.NAME_FA AS GREGORIAN_MONTH_NAME,
                       ICD.YEAR_NO AS HIJRI_YEAR, ICD.MONTH_NO AS HIJRI_MONTH_NO, ICD.DAY_NO AS HIJRI_DAY_NO, IM.NAME_FA AS HIJRI_MONTH_NAME,
                       R.RULE_TYPE, RS.NAME_FA AS RULE_CALENDAR_NAME, RV.VARIANT_CODE AS RULE_VARIANT_CODE,
                       RM.NAME_FA AS RULE_MONTH_NAME, R.YEAR_NO AS RULE_YEAR_NO, R.MONTH_NO AS RULE_MONTH_NO, R.DAY_NO AS RULE_DAY_NO,
                       EO.OCCURRENCE_SOURCE, EO.DATA_STATUS, EO.HOLIDAY_FLAG,
                       EO.SOURCE_ID, SA.SOURCE_CODE, SA.NAME_FA AS SOURCE_NAME,
                       DV.VERSION_CODE AS DATASET_VERSION_CODE, EO.DESCRIPTION
                """ + from + where + " ORDER BY " + occurrenceSort(sortBy) + " " + normalizedDirection(direction)
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);
        List<OccurrenceSummary> rows = jdbcClient.sql(sql).params(params)
                .query((rs, rowNum) -> mapOccurrenceSummary(rs)).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public OccurrenceFilterMeta occurrenceFilterMeta() {
        String sql = defaultVariantsCte() + """
                SELECT MAX(CASE WHEN D.CANONICAL_DATE = TRUNC(SYSDATE) THEN PCD.YEAR_NO END) AS CURRENT_SOLAR_YEAR,
                       MIN(PCD.YEAR_NO) AS MIN_SOLAR_YEAR,
                       MAX(PCD.YEAR_NO) AS MAX_SOLAR_YEAR
                  FROM %s D
                  CROSS JOIN DEFAULT_VARIANTS VX
                  JOIN %s PCD ON PCD.DAY_ID = D.DAY_ID AND PCD.CALENDAR_VARIANT_ID = VX.PERSIAN_VARIANT_ID
                """.formatted(table("CANONICAL_DAY"), table("CALENDAR_DATE"));
        return jdbcClient.sql(sql).query((rs, rowNum) -> new OccurrenceFilterMeta(
                nullableInteger(rs, "CURRENT_SOLAR_YEAR"),
                nullableInteger(rs, "MIN_SOLAR_YEAR"),
                nullableInteger(rs, "MAX_SOLAR_YEAR")
        )).single();
    }

    private String defaultVariantsCte() {
        return """
                WITH DEFAULT_VARIANTS AS (
                    SELECT MAX(CASE WHEN S.CALENDAR_CODE = 'PERSIAN' AND V.IS_DEFAULT = 'Y' THEN V.CALENDAR_VARIANT_ID END) AS PERSIAN_VARIANT_ID,
                           MAX(CASE WHEN S.CALENDAR_CODE = 'GREGORIAN' AND V.IS_DEFAULT = 'Y' THEN V.CALENDAR_VARIANT_ID END) AS GREGORIAN_VARIANT_ID,
                           MAX(CASE WHEN S.CALENDAR_CODE = 'ISLAMIC' AND V.IS_DEFAULT = 'Y' THEN V.CALENDAR_VARIANT_ID END) AS ISLAMIC_VARIANT_ID,
                           MAX(CASE WHEN S.CALENDAR_CODE = 'PERSIAN' THEN S.CALENDAR_SYSTEM_ID END) AS PERSIAN_SYSTEM_ID,
                           MAX(CASE WHEN S.CALENDAR_CODE = 'GREGORIAN' THEN S.CALENDAR_SYSTEM_ID END) AS GREGORIAN_SYSTEM_ID,
                           MAX(CASE WHEN S.CALENDAR_CODE = 'ISLAMIC' THEN S.CALENDAR_SYSTEM_ID END) AS ISLAMIC_SYSTEM_ID
                      FROM %s V
                      JOIN %s S ON S.CALENDAR_SYSTEM_ID = V.CALENDAR_SYSTEM_ID
                )
                """.formatted(table("CALENDAR_VARIANT"), table("CALENDAR_SYSTEM"));
    }

    private String occurrenceSummaryFrom() {
        return " FROM " + table("EVENT_OCCURRENCE") + " EO"
                + " JOIN " + table("EVENT") + " E ON E.EVENT_ID = EO.EVENT_ID"
                + " LEFT JOIN " + table("EVENT_TYPE") + " ET ON ET.EVENT_TYPE_ID = E.EVENT_TYPE_ID"
                + " JOIN " + table("CANONICAL_DAY") + " D ON D.DAY_ID = EO.DAY_ID"
                + " LEFT JOIN " + table("WEEKDAY") + " W ON W.WEEKDAY_ID = D.WEEKDAY_ID"
                + " LEFT JOIN " + table("SOURCE_AUTHORITY") + " SA ON SA.SOURCE_ID = EO.SOURCE_ID"
                + " LEFT JOIN " + table("DATASET_VERSION") + " DV ON DV.DATASET_VERSION_ID = EO.DATASET_VERSION_ID"
                + " LEFT JOIN " + table("EVENT_RECURRENCE_RULE") + " R ON R.EVENT_RULE_ID = EO.EVENT_RULE_ID"
                + " LEFT JOIN " + table("CALENDAR_VARIANT") + " RV ON RV.CALENDAR_VARIANT_ID = R.CALENDAR_VARIANT_ID"
                + " LEFT JOIN " + table("CALENDAR_SYSTEM") + " RS ON RS.CALENDAR_SYSTEM_ID = RV.CALENDAR_SYSTEM_ID"
                + " LEFT JOIN " + table("CALENDAR_MONTH") + " RM ON RM.CALENDAR_SYSTEM_ID = RS.CALENDAR_SYSTEM_ID AND RM.MONTH_NO = R.MONTH_NO"
                + " CROSS JOIN DEFAULT_VARIANTS VX"
                + " LEFT JOIN " + table("CALENDAR_DATE") + " PCD ON PCD.DAY_ID = EO.DAY_ID AND PCD.CALENDAR_VARIANT_ID = VX.PERSIAN_VARIANT_ID"
                + " LEFT JOIN " + table("CALENDAR_MONTH") + " PM ON PM.CALENDAR_SYSTEM_ID = VX.PERSIAN_SYSTEM_ID AND PM.MONTH_NO = PCD.MONTH_NO"
                + " LEFT JOIN " + table("CALENDAR_DATE") + " GCD ON GCD.DAY_ID = EO.DAY_ID AND GCD.CALENDAR_VARIANT_ID = VX.GREGORIAN_VARIANT_ID"
                + " LEFT JOIN " + table("CALENDAR_MONTH") + " GM ON GM.CALENDAR_SYSTEM_ID = VX.GREGORIAN_SYSTEM_ID AND GM.MONTH_NO = GCD.MONTH_NO"
                + " LEFT JOIN " + table("CALENDAR_DATE") + " ICD ON ICD.DAY_ID = EO.DAY_ID AND ICD.CALENDAR_VARIANT_ID = VX.ISLAMIC_VARIANT_ID"
                + " LEFT JOIN " + table("CALENDAR_MONTH") + " IM ON IM.CALENDAR_SYSTEM_ID = VX.ISLAMIC_SYSTEM_ID AND IM.MONTH_NO = ICD.MONTH_NO";
    }

    private OccurrenceSummary mapOccurrenceSummary(ResultSet rs) throws SQLException {
        return new OccurrenceSummary(
                rs.getLong("EVENT_OCCURRENCE_ID"), rs.getLong("EVENT_ID"), nullableLong(rs, "EVENT_RULE_ID"),
                rs.getString("EVENT_CODE"), rs.getString("EVENT_NAME"), rs.getString("EVENT_TYPE_NAME"),
                rs.getLong("DAY_ID"), rs.getString("CANONICAL_ISO_DATE"), rs.getString("WEEKDAY_NAME"),
                nullableInteger(rs, "SOLAR_YEAR"), nullableInteger(rs, "SOLAR_MONTH_NO"), nullableInteger(rs, "SOLAR_DAY_NO"), rs.getString("SOLAR_MONTH_NAME"),
                nullableInteger(rs, "GREGORIAN_YEAR"), nullableInteger(rs, "GREGORIAN_MONTH_NO"), nullableInteger(rs, "GREGORIAN_DAY_NO"), rs.getString("GREGORIAN_MONTH_NAME"),
                nullableInteger(rs, "HIJRI_YEAR"), nullableInteger(rs, "HIJRI_MONTH_NO"), nullableInteger(rs, "HIJRI_DAY_NO"), rs.getString("HIJRI_MONTH_NAME"),
                rs.getString("RULE_TYPE"), rs.getString("RULE_CALENDAR_NAME"), rs.getString("RULE_VARIANT_CODE"), rs.getString("RULE_MONTH_NAME"),
                nullableInteger(rs, "RULE_YEAR_NO"), nullableInteger(rs, "RULE_MONTH_NO"), nullableInteger(rs, "RULE_DAY_NO"),
                rs.getString("OCCURRENCE_SOURCE"), rs.getString("DATA_STATUS"), "Y".equalsIgnoreCase(rs.getString("HOLIDAY_FLAG")),
                nullableLong(rs, "SOURCE_ID"), rs.getString("SOURCE_CODE"), rs.getString("SOURCE_NAME"), rs.getString("DATASET_VERSION_CODE"), rs.getString("DESCRIPTION")
        );
    }

    private static String occurrenceSort(String requested) {
        if (requested == null || requested.isBlank()) return "D.CANONICAL_DATE";
        return switch (requested) {
            case "eventName" -> "E.NAME_FA";
            case "solarDate", "gregorianDate", "hijriDate" -> "D.CANONICAL_DATE";
            case "ruleType" -> "R.RULE_TYPE";
            case "occurrenceSource" -> "EO.OCCURRENCE_SOURCE";
            case "holiday" -> "EO.HOLIDAY_FLAG";
            case "dataStatus" -> "EO.DATA_STATUS";
            case "sourceName" -> "SA.NAME_FA";
            default -> "D.CANONICAL_DATE";
        };
    }

    public List<CalendarMonthOption> monthsForVariant(long variantId) {
        String sql = """
                SELECT M.MONTH_NO, M.NAME_FA
                  FROM %s V
                  JOIN %s M ON M.CALENDAR_SYSTEM_ID = V.CALENDAR_SYSTEM_ID
                 WHERE V.CALENDAR_VARIANT_ID = :variantId
                 ORDER BY M.DISPLAY_ORDER NULLS LAST, M.MONTH_NO
                """.formatted(table("CALENDAR_VARIANT"), table("CALENDAR_MONTH"));
        return jdbcClient.sql(sql).param("variantId", variantId)
                .query((rs, rowNum) -> new CalendarMonthOption(rs.getInt("MONTH_NO"), rs.getString("NAME_FA"))).list();
    }

    public Optional<RuleDefinition> findRule(long ruleId) {
        String sql = """
                SELECT R.EVENT_RULE_ID, R.EVENT_ID, E.NAME_FA AS EVENT_NAME, R.RULE_TYPE,
                       R.CALENDAR_VARIANT_ID, R.YEAR_NO, R.MONTH_NO, R.DAY_NO,
                       R.START_YEAR_NO, R.END_YEAR_NO, R.DAY_RESOLUTION_POLICY, R.SOURCE_ID, R.DESCRIPTION,
                       R.ACTIVE_FLAG AS RULE_ACTIVE, E.ACTIVE_FLAG AS EVENT_ACTIVE,
                       E.DEFAULT_HOLIDAY_FLAG
                  FROM %s R
                  JOIN %s E ON E.EVENT_ID = R.EVENT_ID
                 WHERE R.EVENT_RULE_ID = :ruleId
                """.formatted(table("EVENT_RECURRENCE_RULE"), table("EVENT"));
        return jdbcClient.sql(sql).param("ruleId", ruleId).query((rs, rowNum) -> mapRule(rs)).optional();
    }

    public List<Long> activeRuleIds() {
        String sql = """
                SELECT R.EVENT_RULE_ID
                  FROM %s R
                  JOIN %s E ON E.EVENT_ID = R.EVENT_ID
                 WHERE R.ACTIVE_FLAG = 'Y'
                   AND E.ACTIVE_FLAG = 'Y'
                 ORDER BY R.EVENT_RULE_ID
                """.formatted(table("EVENT_RECURRENCE_RULE"), table("EVENT"));
        return jdbcClient.sql(sql).query(Long.class).list();
    }

    public int deleteGenerated(long ruleId) {
        return jdbcClient.sql("DELETE FROM " + table("EVENT_OCCURRENCE")
                        + " WHERE EVENT_RULE_ID = :ruleId AND OCCURRENCE_SOURCE = 'GENERATED'")
                .param("ruleId", ruleId).update();
    }

    public int countCalendarMatches(RuleDefinition rule) {
        RuleFilter filter = filter(rule);
        long count = jdbcClient.sql("SELECT COUNT(*) FROM " + table("CALENDAR_DATE") + " CD WHERE " + filter.where())
                .params(filter.params()).query(Long.class).single();
        return Math.toIntExact(count);
    }

    public int insertGenerated(RuleDefinition rule) {
        jdbcClient.sql("LOCK TABLE " + table("EVENT_OCCURRENCE") + " IN SHARE ROW EXCLUSIVE MODE").update();
        long nextId = jdbcClient.sql("SELECT NVL(MAX(EVENT_OCCURRENCE_ID), 0) + 1 FROM " + table("EVENT_OCCURRENCE"))
                .query(Long.class).single();

        RuleFilter filter = filter(rule);
        Map<String, Object> params = new LinkedHashMap<>(filter.params());
        params.put("nextId", nextId);
        params.put("eventId", rule.eventId());
        params.put("ruleId", rule.eventRuleId());
        params.put("sourceId", rule.sourceId());
        params.put("holidayFlag", rule.defaultHoliday() ? "Y" : "N");
        params.put("description", rule.description());

        String sql = """
                INSERT INTO %s (
                    EVENT_OCCURRENCE_ID, EVENT_ID, EVENT_RULE_ID, DAY_ID, SOURCE_ID,
                    OCCURRENCE_SOURCE, DATA_STATUS, HOLIDAY_FLAG, START_TIME, END_TIME,
                    DESCRIPTION, DATASET_VERSION_ID
                )
                SELECT :nextId + ROW_NUMBER() OVER (ORDER BY CD.DAY_ID) - 1,
                       :eventId, :ruleId, CD.DAY_ID, :sourceId,
                       'GENERATED', 'CALCULATED', :holidayFlag, NULL, NULL,
                       :description, CD.DATASET_VERSION_ID
                  FROM %s CD
                 WHERE %s
                   AND NOT EXISTS (
                       SELECT 1 FROM %s X
                        WHERE X.EVENT_ID = :eventId
                          AND X.DAY_ID = CD.DAY_ID
                   )
                """.formatted(
                table("EVENT_OCCURRENCE"), table("CALENDAR_DATE"), filter.where(), table("EVENT_OCCURRENCE")
        );
        return jdbcClient.sql(sql).params(params).update();
    }

    private RuleFilter filter(RuleDefinition rule) {
        StringBuilder where = new StringBuilder("CD.CALENDAR_VARIANT_ID = :variantId AND CD.MONTH_NO = :monthNo");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("variantId", rule.calendarVariantId());
        params.put("monthNo", rule.monthNo());
        params.put("dayNo", rule.dayNo());

        String calendarDate = table("CALENDAR_DATE");
        String policy = rule.dayResolutionPolicy() == null
                ? "EXACT" : rule.dayResolutionPolicy().trim().toUpperCase(Locale.ROOT);
        params.put("dayResolutionPolicy", policy);

        // FIX89: keep day-resolution policy inside Oracle SQL instead of selecting a Java branch.
        // This makes the policy actually used by the same statement that counts/inserts candidate dates
        // and gives one deterministic expression for both EXACT and LAST_DAY_IF_INVALID.
        where.append(" AND CD.DAY_NO = CASE ")
                .append("WHEN :dayResolutionPolicy = 'LAST_DAY_IF_INVALID' THEN (")
                .append("SELECT NVL(MAX(CASE WHEN RX.DAY_NO = :dayNo THEN RX.DAY_NO END), MAX(RX.DAY_NO)) ")
                .append("FROM ").append(calendarDate).append(" RX ")
                .append("WHERE RX.CALENDAR_VARIANT_ID = CD.CALENDAR_VARIANT_ID ")
                .append("AND RX.YEAR_NO = CD.YEAR_NO AND RX.MONTH_NO = CD.MONTH_NO) ")
                .append("ELSE :dayNo END");

        if ("ONE_TIME_DATE".equals(rule.ruleType())) {
            where.append(" AND CD.YEAR_NO = :yearNo");
            params.put("yearNo", rule.yearNo());
        } else {
            if (rule.startYearNo() != null) {
                where.append(" AND CD.YEAR_NO >= :startYearNo");
                params.put("startYearNo", rule.startYearNo());
            }
            if (rule.endYearNo() != null) {
                where.append(" AND CD.YEAR_NO <= :endYearNo");
                params.put("endYearNo", rule.endYearNo());
            }
        }
        return new RuleFilter(where.toString(), params);
    }

    private RuleSummary mapRuleSummary(ResultSet rs) throws SQLException {
        return new RuleSummary(
                rs.getLong("EVENT_RULE_ID"),
                rs.getLong("EVENT_ID"),
                rs.getString("EVENT_CODE"),
                rs.getString("EVENT_NAME"),
                rs.getString("RULE_TYPE"),
                rs.getLong("CALENDAR_VARIANT_ID"),
                rs.getString("VARIANT_CODE"),
                rs.getString("CALENDAR_CODE"),
                rs.getString("CALENDAR_NAME"),
                rs.getString("MONTH_NAME"),
                nullableInteger(rs, "YEAR_NO"),
                rs.getInt("MONTH_NO"),
                rs.getInt("DAY_NO"),
                nullableInteger(rs, "START_YEAR_NO"),
                nullableInteger(rs, "END_YEAR_NO"),
                rs.getString("DAY_RESOLUTION_POLICY"),
                "Y".equalsIgnoreCase(rs.getString("ACTIVE_FLAG")),
                rs.getInt("GENERATED_OCCURRENCES")
        );
    }

    private static String summarySort(String requested, String occurrenceCount) {
        if (requested == null || requested.isBlank()) return "E.NAME_FA";
        return switch (requested) {
            case "eventName" -> "E.NAME_FA";
            case "calendarName" -> "S.NAME_FA";
            case "dateLabel" -> "R.MONTH_NO, R.DAY_NO";
            case "ruleType" -> "R.RULE_TYPE";
            case "rangeLabel" -> "NVL(R.START_YEAR_NO, -999999), NVL(R.END_YEAR_NO, 999999)";
            case "dayResolutionPolicy" -> "R.DAY_RESOLUTION_POLICY";
            case "generatedOccurrences" -> occurrenceCount;
            case "active" -> "R.ACTIVE_FLAG";
            default -> "E.NAME_FA";
        };
    }

    private static String normalizedDirection(String direction) {
        return "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC";
    }

    private RuleDefinition mapRule(ResultSet rs) throws SQLException {
        return new RuleDefinition(
                rs.getLong("EVENT_RULE_ID"),
                rs.getLong("EVENT_ID"),
                rs.getString("EVENT_NAME"),
                rs.getString("RULE_TYPE"),
                rs.getLong("CALENDAR_VARIANT_ID"),
                nullableInteger(rs, "YEAR_NO"),
                rs.getInt("MONTH_NO"),
                rs.getInt("DAY_NO"),
                nullableInteger(rs, "START_YEAR_NO"),
                nullableInteger(rs, "END_YEAR_NO"),
                rs.getString("DAY_RESOLUTION_POLICY"),
                nullableLong(rs, "SOURCE_ID"),
                rs.getString("DESCRIPTION"),
                "Y".equalsIgnoreCase(rs.getString("RULE_ACTIVE")),
                "Y".equalsIgnoreCase(rs.getString("EVENT_ACTIVE")),
                "Y".equalsIgnoreCase(rs.getString("DEFAULT_HOLIDAY_FLAG"))
        );
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.intValueExact();
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.longValueExact();
    }

    private String table(String objectName) { return schema + "." + safeIdentifier(objectName); }

    private static String safeIdentifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        }
        return value;
    }

    private record RuleFilter(String where, Map<String, Object> params) {}
}
