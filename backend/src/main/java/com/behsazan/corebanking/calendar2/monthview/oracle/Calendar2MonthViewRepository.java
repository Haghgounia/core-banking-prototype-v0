package com.behsazan.corebanking.calendar2.monthview.oracle;

import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.CalendarContext;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.DayRow;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.EventRow;
import com.behsazan.corebanking.calendar2.monthview.domain.Calendar2MonthViewModels.MonthOption;
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
public class Calendar2MonthViewRepository {
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");

    private final JdbcClient jdbcClient;
    private final String schema;

    public Calendar2MonthViewRepository(
            JdbcClient jdbcClient,
            @Value("${core-banking.schemas.calendar2:CAL2}") String schemaName
    ) {
        this.jdbcClient = jdbcClient;
        this.schema = safeIdentifier(schemaName == null ? null : schemaName.trim().toUpperCase());
    }

    public Optional<CalendarContext> calendarContext(String calendarCode) {
        String sql = """
                WITH SELECTED_VARIANT AS (
                    SELECT S.CALENDAR_SYSTEM_ID, S.CALENDAR_CODE, S.NAME_FA AS CALENDAR_NAME,
                           V.CALENDAR_VARIANT_ID, V.VARIANT_CODE,
                           ROW_NUMBER() OVER (ORDER BY V.CALENDAR_VARIANT_ID) AS RN
                      FROM %s S
                      JOIN %s V ON V.CALENDAR_SYSTEM_ID = S.CALENDAR_SYSTEM_ID
                     WHERE S.CALENDAR_CODE = :calendarCode
                       AND S.ACTIVE_FLAG = 'Y'
                       AND V.ACTIVE_FLAG = 'Y'
                       AND V.IS_DEFAULT = 'Y'
                )
                SELECT SV.CALENDAR_SYSTEM_ID, SV.CALENDAR_VARIANT_ID, SV.CALENDAR_CODE,
                       SV.CALENDAR_NAME, SV.VARIANT_CODE,
                       MAX(CASE WHEN D.CANONICAL_DATE = TRUNC(SYSDATE) THEN CD.YEAR_NO END) AS CURRENT_YEAR,
                       MAX(CASE WHEN D.CANONICAL_DATE = TRUNC(SYSDATE) THEN CD.MONTH_NO END) AS CURRENT_MONTH,
                       MIN(CD.YEAR_NO) AS MINIMUM_YEAR,
                       MAX(CD.YEAR_NO) AS MAXIMUM_YEAR
                  FROM SELECTED_VARIANT SV
                  JOIN %s CD ON CD.CALENDAR_VARIANT_ID = SV.CALENDAR_VARIANT_ID
                  JOIN %s D ON D.DAY_ID = CD.DAY_ID
                 WHERE SV.RN = 1
                 GROUP BY SV.CALENDAR_SYSTEM_ID, SV.CALENDAR_VARIANT_ID, SV.CALENDAR_CODE,
                          SV.CALENDAR_NAME, SV.VARIANT_CODE
                """.formatted(table("CALENDAR_SYSTEM"), table("CALENDAR_VARIANT"), table("CALENDAR_DATE"), table("CANONICAL_DAY"));

        return jdbcClient.sql(sql)
                .param("calendarCode", calendarCode)
                .query((rs, rowNum) -> mapCalendarContext(rs))
                .optional();
    }

    public List<MonthOption> months(long calendarSystemId) {
        String sql = "SELECT MONTH_NO, NAME_FA FROM " + table("CALENDAR_MONTH")
                + " WHERE CALENDAR_SYSTEM_ID = :calendarSystemId ORDER BY DISPLAY_ORDER NULLS LAST, MONTH_NO";
        return jdbcClient.sql(sql)
                .param("calendarSystemId", calendarSystemId)
                .query((rs, rowNum) -> new MonthOption(rs.getInt("MONTH_NO"), rs.getString("NAME_FA")))
                .list();
    }

    public List<DayRow> monthDays(long calendarVariantId, int year, int monthNo) {
        String sql = defaultVariantsCte() + """
                SELECT D.DAY_ID, D.ISO_DATE_TEXT AS CANONICAL_ISO_DATE,
                       W.ISO_WEEKDAY_NO,
                       NVL(W.IR_DISPLAY_ORDER, CASE WHEN W.ISO_WEEKDAY_NO >= 6 THEN W.ISO_WEEKDAY_NO - 5 ELSE W.ISO_WEEKDAY_NO + 2 END) AS IR_DISPLAY_ORDER,
                       W.NAME_FA AS WEEKDAY_NAME,
                       CD.YEAR_NO AS PRIMARY_YEAR, CD.MONTH_NO AS PRIMARY_MONTH_NO, CD.DAY_NO AS PRIMARY_DAY_NO,
                       PCM.NAME_FA AS PRIMARY_MONTH_NAME,
                       PCD.YEAR_NO AS PERSIAN_YEAR, PCD.MONTH_NO AS PERSIAN_MONTH_NO, PCD.DAY_NO AS PERSIAN_DAY_NO, PM.NAME_FA AS PERSIAN_MONTH_NAME,
                       GCD.YEAR_NO AS GREGORIAN_YEAR, GCD.MONTH_NO AS GREGORIAN_MONTH_NO, GCD.DAY_NO AS GREGORIAN_DAY_NO, GM.NAME_FA AS GREGORIAN_MONTH_NAME,
                       ICD.YEAR_NO AS ISLAMIC_YEAR, ICD.MONTH_NO AS ISLAMIC_MONTH_NO, ICD.DAY_NO AS ISLAMIC_DAY_NO, IM.NAME_FA AS ISLAMIC_MONTH_NAME,
                       CASE WHEN D.CANONICAL_DATE = TRUNC(SYSDATE) THEN 'Y' ELSE 'N' END AS TODAY_FLAG
                  FROM %s CD
                  JOIN %s D ON D.DAY_ID = CD.DAY_ID
                  JOIN %s W ON W.WEEKDAY_ID = D.WEEKDAY_ID
                  JOIN %s PV ON PV.CALENDAR_VARIANT_ID = CD.CALENDAR_VARIANT_ID
                  JOIN %s PS ON PS.CALENDAR_SYSTEM_ID = PV.CALENDAR_SYSTEM_ID
                  LEFT JOIN %s PCM ON PCM.CALENDAR_SYSTEM_ID = PS.CALENDAR_SYSTEM_ID AND PCM.MONTH_NO = CD.MONTH_NO
                  CROSS JOIN DEFAULT_VARIANTS VX
                  LEFT JOIN %s PCD ON PCD.DAY_ID = D.DAY_ID AND PCD.CALENDAR_VARIANT_ID = VX.PERSIAN_VARIANT_ID
                  LEFT JOIN %s PM ON PM.CALENDAR_SYSTEM_ID = VX.PERSIAN_SYSTEM_ID AND PM.MONTH_NO = PCD.MONTH_NO
                  LEFT JOIN %s GCD ON GCD.DAY_ID = D.DAY_ID AND GCD.CALENDAR_VARIANT_ID = VX.GREGORIAN_VARIANT_ID
                  LEFT JOIN %s GM ON GM.CALENDAR_SYSTEM_ID = VX.GREGORIAN_SYSTEM_ID AND GM.MONTH_NO = GCD.MONTH_NO
                  LEFT JOIN %s ICD ON ICD.DAY_ID = D.DAY_ID AND ICD.CALENDAR_VARIANT_ID = VX.ISLAMIC_VARIANT_ID
                  LEFT JOIN %s IM ON IM.CALENDAR_SYSTEM_ID = VX.ISLAMIC_SYSTEM_ID AND IM.MONTH_NO = ICD.MONTH_NO
                 WHERE CD.CALENDAR_VARIANT_ID = :calendarVariantId
                   AND CD.YEAR_NO = :year
                   AND CD.MONTH_NO = :monthNo
                 ORDER BY D.CANONICAL_DATE
                """.formatted(
                table("CALENDAR_DATE"), table("CANONICAL_DAY"), table("WEEKDAY"),
                table("CALENDAR_VARIANT"), table("CALENDAR_SYSTEM"), table("CALENDAR_MONTH"),
                table("CALENDAR_DATE"), table("CALENDAR_MONTH"),
                table("CALENDAR_DATE"), table("CALENDAR_MONTH"),
                table("CALENDAR_DATE"), table("CALENDAR_MONTH")
        );

        return jdbcClient.sql(sql)
                .param("calendarVariantId", calendarVariantId)
                .param("year", year)
                .param("monthNo", monthNo)
                .query((rs, rowNum) -> mapDay(rs))
                .list();
    }

    public List<EventRow> monthEvents(long calendarVariantId, int year, int monthNo) {
        String sql = """
                SELECT EO.EVENT_OCCURRENCE_ID, EO.EVENT_ID, EO.DAY_ID,
                       E.EVENT_CODE, E.NAME_FA AS EVENT_NAME,
                       ET.EVENT_TYPE_CODE, ET.NAME_FA AS EVENT_TYPE_NAME,
                       E.OFFICIAL_FLAG, EO.HOLIDAY_FLAG, EO.OCCURRENCE_SOURCE,
                       EO.DATA_STATUS, EO.DESCRIPTION
                  FROM %s EO
                  JOIN %s E ON E.EVENT_ID = EO.EVENT_ID
                  JOIN %s ET ON ET.EVENT_TYPE_ID = E.EVENT_TYPE_ID
                  JOIN %s CD ON CD.DAY_ID = EO.DAY_ID
                             AND CD.CALENDAR_VARIANT_ID = :calendarVariantId
                 WHERE CD.YEAR_NO = :year
                   AND CD.MONTH_NO = :monthNo
                   AND E.ACTIVE_FLAG = 'Y'
                 ORDER BY CD.DAY_NO,
                          CASE WHEN EO.HOLIDAY_FLAG = 'Y' THEN 0 ELSE 1 END,
                          E.NAME_FA,
                          EO.EVENT_OCCURRENCE_ID
                """.formatted(table("EVENT_OCCURRENCE"), table("EVENT"), table("EVENT_TYPE"), table("CALENDAR_DATE"));

        return jdbcClient.sql(sql)
                .param("calendarVariantId", calendarVariantId)
                .param("year", year)
                .param("monthNo", monthNo)
                .query((rs, rowNum) -> mapEvent(rs))
                .list();
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
                     WHERE V.ACTIVE_FLAG = 'Y'
                       AND S.ACTIVE_FLAG = 'Y'
                )
                """.formatted(table("CALENDAR_VARIANT"), table("CALENDAR_SYSTEM"));
    }

    private CalendarContext mapCalendarContext(ResultSet rs) throws SQLException {
        return new CalendarContext(
                rs.getLong("CALENDAR_SYSTEM_ID"),
                rs.getLong("CALENDAR_VARIANT_ID"),
                rs.getString("CALENDAR_CODE"),
                rs.getString("CALENDAR_NAME"),
                rs.getString("VARIANT_CODE"),
                nullableInteger(rs, "CURRENT_YEAR"),
                nullableInteger(rs, "CURRENT_MONTH"),
                nullableInteger(rs, "MINIMUM_YEAR"),
                nullableInteger(rs, "MAXIMUM_YEAR")
        );
    }

    private DayRow mapDay(ResultSet rs) throws SQLException {
        return new DayRow(
                rs.getLong("DAY_ID"), rs.getString("CANONICAL_ISO_DATE"),
                rs.getInt("ISO_WEEKDAY_NO"), rs.getInt("IR_DISPLAY_ORDER"), rs.getString("WEEKDAY_NAME"),
                rs.getInt("PRIMARY_YEAR"), rs.getInt("PRIMARY_MONTH_NO"), rs.getInt("PRIMARY_DAY_NO"), rs.getString("PRIMARY_MONTH_NAME"),
                nullableInteger(rs, "PERSIAN_YEAR"), nullableInteger(rs, "PERSIAN_MONTH_NO"), nullableInteger(rs, "PERSIAN_DAY_NO"), rs.getString("PERSIAN_MONTH_NAME"),
                nullableInteger(rs, "GREGORIAN_YEAR"), nullableInteger(rs, "GREGORIAN_MONTH_NO"), nullableInteger(rs, "GREGORIAN_DAY_NO"), rs.getString("GREGORIAN_MONTH_NAME"),
                nullableInteger(rs, "ISLAMIC_YEAR"), nullableInteger(rs, "ISLAMIC_MONTH_NO"), nullableInteger(rs, "ISLAMIC_DAY_NO"), rs.getString("ISLAMIC_MONTH_NAME"),
                "Y".equalsIgnoreCase(rs.getString("TODAY_FLAG"))
        );
    }

    private EventRow mapEvent(ResultSet rs) throws SQLException {
        return new EventRow(
                rs.getLong("EVENT_OCCURRENCE_ID"), rs.getLong("EVENT_ID"), rs.getLong("DAY_ID"),
                rs.getString("EVENT_CODE"), rs.getString("EVENT_NAME"), rs.getString("EVENT_TYPE_CODE"), rs.getString("EVENT_TYPE_NAME"),
                "Y".equalsIgnoreCase(rs.getString("OFFICIAL_FLAG")),
                "Y".equalsIgnoreCase(rs.getString("HOLIDAY_FLAG")),
                rs.getString("OCCURRENCE_SOURCE"), rs.getString("DATA_STATUS"), rs.getString("DESCRIPTION")
        );
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
