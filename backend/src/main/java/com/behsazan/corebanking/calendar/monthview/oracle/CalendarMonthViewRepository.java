package com.behsazan.corebanking.calendar.monthview.oracle;

import com.behsazan.corebanking.calendar.monthview.domain.CalendarMonthViewModels.CalendarContext;
import com.behsazan.corebanking.calendar.monthview.domain.CalendarMonthViewModels.DayRow;
import com.behsazan.corebanking.calendar.monthview.domain.CalendarMonthViewModels.EventRow;
import com.behsazan.corebanking.calendar.monthview.domain.CalendarMonthViewModels.MonthOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class CalendarMonthViewRepository {
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");

    private final JdbcClient jdbcClient;
    private final String schema;

    public CalendarMonthViewRepository(
            JdbcClient jdbcClient,
            @Value("${core-banking.schemas.calendar:CAL}") String schemaName
    ) {
        this.jdbcClient = jdbcClient;
        this.schema = safeIdentifier(schemaName == null ? null : schemaName.trim().toUpperCase());
    }

    public Optional<CalendarContext> calendarContext(String systemCode) {
        String sql = """
                SELECT S.CALENDAR_SYSTEM_CODE,
                       S.CALENDAR_SYSTEM_NAME_FA AS CALENDAR_NAME,
                       MAX(CASE WHEN D.CANONICAL_DATE = TRUNC(SYSDATE) THEN CD.YEAR_NO END) AS CURRENT_YEAR,
                       MAX(CASE WHEN D.CANONICAL_DATE = TRUNC(SYSDATE) THEN CD.MONTH_NO END) AS CURRENT_MONTH,
                       MIN(CD.YEAR_NO) AS MINIMUM_YEAR,
                       MAX(CD.YEAR_NO) AS MAXIMUM_YEAR
                  FROM %s S
                  JOIN %s CD ON CD.CALENDAR_SYSTEM_CODE = S.CALENDAR_SYSTEM_CODE
                  JOIN %s D ON D.DAY_ID = CD.DAY_ID
                 WHERE S.CALENDAR_SYSTEM_CODE = :systemCode
                   AND S.ACTIVE_FLAG = 'Y'
                 GROUP BY S.CALENDAR_SYSTEM_CODE, S.CALENDAR_SYSTEM_NAME_FA
                """.formatted(table("CALENDAR_SYSTEM"), table("CALENDAR_DATE"), table("CALENDAR_DAY"));
        return jdbcClient.sql(sql)
                .param("systemCode", systemCode)
                .query((rs, rowNum) -> new CalendarContext(
                        rs.getString("CALENDAR_SYSTEM_CODE"),
                        rs.getString("CALENDAR_NAME"),
                        nullableInteger(rs, "CURRENT_YEAR"),
                        nullableInteger(rs, "CURRENT_MONTH"),
                        nullableInteger(rs, "MINIMUM_YEAR"),
                        nullableInteger(rs, "MAXIMUM_YEAR")
                )).optional();
    }

    public List<MonthOption> months(String systemCode) {
        String sql = "SELECT MONTH_NO, MONTH_NAME_FA FROM " + table("CALENDAR_MONTH")
                + " WHERE CALENDAR_SYSTEM_CODE = :systemCode ORDER BY MONTH_NO";
        return jdbcClient.sql(sql)
                .param("systemCode", systemCode)
                .query((rs, rowNum) -> new MonthOption(rs.getInt("MONTH_NO"), rs.getString("MONTH_NAME_FA")))
                .list();
    }

    public List<DayRow> monthDays(String systemCode, int year, int monthNo) {
        String sql = """
                SELECT D.DAY_ID,
                       TO_CHAR(D.CANONICAL_DATE, 'YYYY-MM-DD') AS CANONICAL_ISO_DATE,
                       D.ISO_WEEKDAY_NO,
                       D.IR_WEEKDAY_NO AS IR_DISPLAY_ORDER,
                       W.WEEKDAY_NAME_FA AS WEEKDAY_NAME,
                       CD.YEAR_NO AS PRIMARY_YEAR,
                       CD.MONTH_NO AS PRIMARY_MONTH_NO,
                       CD.DAY_NO AS PRIMARY_DAY_NO,
                       PCM.MONTH_NAME_FA AS PRIMARY_MONTH_NAME,
                       PCD.YEAR_NO AS PERSIAN_YEAR,
                       PCD.MONTH_NO AS PERSIAN_MONTH_NO,
                       PCD.DAY_NO AS PERSIAN_DAY_NO,
                       PM.MONTH_NAME_FA AS PERSIAN_MONTH_NAME,
                       GCD.YEAR_NO AS GREGORIAN_YEAR,
                       GCD.MONTH_NO AS GREGORIAN_MONTH_NO,
                       GCD.DAY_NO AS GREGORIAN_DAY_NO,
                       GM.MONTH_NAME_FA AS GREGORIAN_MONTH_NAME,
                       HCD.YEAR_NO AS ISLAMIC_YEAR,
                       HCD.MONTH_NO AS ISLAMIC_MONTH_NO,
                       HCD.DAY_NO AS ISLAMIC_DAY_NO,
                       HM.MONTH_NAME_FA AS ISLAMIC_MONTH_NAME,
                       CASE WHEN D.CANONICAL_DATE = TRUNC(SYSDATE) THEN 'Y' ELSE 'N' END AS TODAY_FLAG,
                       CASE WHEN EXISTS (
                           SELECT 1
                             FROM %s BCD
                             JOIN %s BC ON BC.BUSINESS_CALENDAR_ID = BCD.BUSINESS_CALENDAR_ID
                            WHERE BCD.DAY_ID = D.DAY_ID
                              AND BC.ACTIVE_FLAG = 'Y'
                              AND BCD.IS_BANK_HOLIDAY = 'Y'
                       ) THEN 'Y' ELSE 'N' END AS BANK_HOLIDAY_FLAG
                  FROM %s CD
                  JOIN %s D ON D.DAY_ID = CD.DAY_ID
                  JOIN %s W ON W.WEEKDAY_ID = D.WEEKDAY_ID
                  LEFT JOIN %s PCM ON PCM.CALENDAR_SYSTEM_CODE = CD.CALENDAR_SYSTEM_CODE AND PCM.MONTH_NO = CD.MONTH_NO
                  LEFT JOIN %s PCD ON PCD.DAY_ID = D.DAY_ID AND PCD.CALENDAR_SYSTEM_CODE = 'SOLAR_HIJRI_IR'
                  LEFT JOIN %s PM ON PM.CALENDAR_SYSTEM_CODE = 'SOLAR_HIJRI_IR' AND PM.MONTH_NO = PCD.MONTH_NO
                  LEFT JOIN %s GCD ON GCD.DAY_ID = D.DAY_ID AND GCD.CALENDAR_SYSTEM_CODE = 'GREGORIAN'
                  LEFT JOIN %s GM ON GM.CALENDAR_SYSTEM_CODE = 'GREGORIAN' AND GM.MONTH_NO = GCD.MONTH_NO
                  LEFT JOIN %s HCD ON HCD.DAY_ID = D.DAY_ID AND HCD.CALENDAR_SYSTEM_CODE = 'HIJRI_CIVIL'
                  LEFT JOIN %s HM ON HM.CALENDAR_SYSTEM_CODE = 'HIJRI_CIVIL' AND HM.MONTH_NO = HCD.MONTH_NO
                 WHERE CD.CALENDAR_SYSTEM_CODE = :systemCode
                   AND CD.YEAR_NO = :year
                   AND CD.MONTH_NO = :monthNo
                 ORDER BY D.CANONICAL_DATE
                """.formatted(
                table("BUSINESS_CALENDAR_DAY"), table("BUSINESS_CALENDAR"),
                table("CALENDAR_DATE"), table("CALENDAR_DAY"), table("WEEKDAY"), table("CALENDAR_MONTH"),
                table("CALENDAR_DATE"), table("CALENDAR_MONTH"),
                table("CALENDAR_DATE"), table("CALENDAR_MONTH"),
                table("CALENDAR_DATE"), table("CALENDAR_MONTH")
        );

        return jdbcClient.sql(sql)
                .param("systemCode", systemCode)
                .param("year", year)
                .param("monthNo", monthNo)
                .query((rs, rowNum) -> new DayRow(
                        rs.getLong("DAY_ID"), rs.getString("CANONICAL_ISO_DATE"),
                        rs.getInt("ISO_WEEKDAY_NO"), rs.getInt("IR_DISPLAY_ORDER"), rs.getString("WEEKDAY_NAME"),
                        rs.getInt("PRIMARY_YEAR"), rs.getInt("PRIMARY_MONTH_NO"), rs.getInt("PRIMARY_DAY_NO"), rs.getString("PRIMARY_MONTH_NAME"),
                        nullableInteger(rs, "PERSIAN_YEAR"), nullableInteger(rs, "PERSIAN_MONTH_NO"), nullableInteger(rs, "PERSIAN_DAY_NO"), rs.getString("PERSIAN_MONTH_NAME"),
                        nullableInteger(rs, "GREGORIAN_YEAR"), nullableInteger(rs, "GREGORIAN_MONTH_NO"), nullableInteger(rs, "GREGORIAN_DAY_NO"), rs.getString("GREGORIAN_MONTH_NAME"),
                        nullableInteger(rs, "ISLAMIC_YEAR"), nullableInteger(rs, "ISLAMIC_MONTH_NO"), nullableInteger(rs, "ISLAMIC_DAY_NO"), rs.getString("ISLAMIC_MONTH_NAME"),
                        "Y".equalsIgnoreCase(rs.getString("TODAY_FLAG")),
                        "Y".equalsIgnoreCase(rs.getString("BANK_HOLIDAY_FLAG"))
                )).list();
    }

    public List<EventRow> monthEvents(String systemCode, int year, int monthNo) {
        String sql = """
                SELECT OO.OCCASION_OCCURRENCE_ID,
                       O.OCCASION_ID,
                       CDO.DAY_ID,
                       O.OCCASION_CODE AS EVENT_CODE,
                       O.OCCASION_NAME_FA AS EVENT_NAME,
                       OC.CATEGORY_CODE AS EVENT_TYPE_CODE,
                       OC.CATEGORY_NAME_FA AS EVENT_TYPE_NAME,
                       OO.IS_OFFICIAL,
                       CASE WHEN EXISTS (
                           SELECT 1
                             FROM %s BCD
                             JOIN %s BC ON BC.BUSINESS_CALENDAR_ID = BCD.BUSINESS_CALENDAR_ID
                            WHERE BCD.DAY_ID = CDO.DAY_ID
                              AND BC.ACTIVE_FLAG = 'Y'
                              AND BCD.IS_BANK_HOLIDAY = 'Y'
                       ) THEN 'Y' ELSE 'N' END AS HOLIDAY_FLAG,
                       CASE
                         WHEN OO.OCCURRENCE_STATUS_CODE = 'GENERATED' THEN 'GENERATED'
                         WHEN OO.IS_OFFICIAL = 'Y' THEN 'OFFICIAL'
                         ELSE 'MANUAL'
                       END AS OCCURRENCE_SOURCE,
                       OO.OCCURRENCE_STATUS_CODE AS DATA_STATUS,
                       O.DESCRIPTION
                  FROM %s CDO
                  JOIN %s OO ON OO.OCCASION_OCCURRENCE_ID = CDO.OCCASION_OCCURRENCE_ID
                  JOIN %s O ON O.OCCASION_ID = OO.OCCASION_ID
                  JOIN %s OC ON OC.OCCASION_CATEGORY_ID = O.OCCASION_CATEGORY_ID
                  JOIN %s CD ON CD.DAY_ID = CDO.DAY_ID AND CD.CALENDAR_SYSTEM_CODE = :systemCode
                 WHERE CD.YEAR_NO = :year
                   AND CD.MONTH_NO = :monthNo
                   AND O.ACTIVE_FLAG = 'Y'
                   AND OO.OCCURRENCE_STATUS_CODE <> 'CANCELLED'
                 ORDER BY CD.DAY_NO,
                          CDO.DISPLAY_PRIORITY,
                          O.OCCASION_NAME_FA,
                          OO.OCCASION_OCCURRENCE_ID
                """.formatted(
                table("BUSINESS_CALENDAR_DAY"), table("BUSINESS_CALENDAR"),
                table("CALENDAR_DAY_OCCASION"), table("OCCASION_OCCURRENCE"), table("OCCASION"),
                table("OCCASION_CATEGORY"), table("CALENDAR_DATE")
        );

        return jdbcClient.sql(sql)
                .param("systemCode", systemCode)
                .param("year", year)
                .param("monthNo", monthNo)
                .query((rs, rowNum) -> new EventRow(
                        rs.getLong("OCCASION_OCCURRENCE_ID"), rs.getLong("OCCASION_ID"), rs.getLong("DAY_ID"),
                        rs.getString("EVENT_CODE"), rs.getString("EVENT_NAME"), rs.getString("EVENT_TYPE_CODE"), rs.getString("EVENT_TYPE_NAME"),
                        "Y".equalsIgnoreCase(rs.getString("IS_OFFICIAL")),
                        "Y".equalsIgnoreCase(rs.getString("HOLIDAY_FLAG")),
                        rs.getString("OCCURRENCE_SOURCE"), rs.getString("DATA_STATUS"), rs.getString("DESCRIPTION")
                )).list();
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? null : value.intValueExact();
    }

    private String table(String objectName) { return schema + "." + safeIdentifier(objectName); }

    private static String safeIdentifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        }
        return value;
    }
}
