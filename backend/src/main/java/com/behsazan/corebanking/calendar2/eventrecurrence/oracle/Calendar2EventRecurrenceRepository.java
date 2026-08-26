package com.behsazan.corebanking.calendar2.eventrecurrence.oracle;

import com.behsazan.corebanking.calendar2.eventrecurrence.domain.Calendar2EventRecurrenceModels.RuleDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
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

    public Optional<RuleDefinition> findRule(long ruleId) {
        String sql = """
                SELECT R.EVENT_RULE_ID, R.EVENT_ID, E.NAME_FA AS EVENT_NAME, R.RULE_TYPE,
                       R.CALENDAR_VARIANT_ID, R.YEAR_NO, R.MONTH_NO, R.DAY_NO,
                       R.START_YEAR_NO, R.END_YEAR_NO, R.SOURCE_ID, R.DESCRIPTION,
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
        StringBuilder where = new StringBuilder("CD.CALENDAR_VARIANT_ID = :variantId AND CD.MONTH_NO = :monthNo AND CD.DAY_NO = :dayNo");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("variantId", rule.calendarVariantId());
        params.put("monthNo", rule.monthNo());
        params.put("dayNo", rule.dayNo());

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
