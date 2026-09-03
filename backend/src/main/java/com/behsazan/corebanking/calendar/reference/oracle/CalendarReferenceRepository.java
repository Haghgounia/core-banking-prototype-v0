package com.behsazan.corebanking.calendar.reference.oracle;

import com.behsazan.corebanking.calendar.reference.application.CalendarReferenceRegistry;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.FieldDescriptor;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.FieldType;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.LookupOption;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.RecordResponse;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.SolarYearContext;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.TableDescriptor;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@Repository
public class CalendarReferenceRepository {
    private final JdbcClient jdbcClient;
    private final CalendarReferenceRegistry registry;

    public CalendarReferenceRepository(JdbcClient jdbcClient, CalendarReferenceRegistry registry) {
        this.jdbcClient = jdbcClient;
        this.registry = registry;
    }

    public PageResponse<Map<String, Object>> search(TableDescriptor descriptor, String text, Integer solarYear, int page, int size,
                                                     String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        StringBuilder where = new StringBuilder(searchWhere(descriptor, text));
        Map<String, Object> params = new LinkedHashMap<>();
        if (text != null && !text.isBlank() && !descriptor.searchableFields().isEmpty()) {
            params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (solarYear != null && hasDayId(descriptor)) {
            appendCondition(where, "EXISTS (SELECT 1 FROM " + solarDateTable()
                    + " SY WHERE SY.DAY_ID=T.DAY_ID AND SY.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR' AND SY.YEAR_NO=:solarYear)");
            params.put("solarYear", solarYear);
        }

        long total = jdbcClient.sql("SELECT COUNT(*) FROM " + table(descriptor) + " T" + where)
                .params(params).query(Long.class).single();

        List<FieldDescriptor> selected = descriptor.gridFields();
        String sql = "SELECT " + selectList(selected, "T") + " FROM " + table(descriptor) + " T" + where
                + " ORDER BY " + orderBy(descriptor, sortBy) + " " + normalizedDirection(direction)
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);

        List<Map<String, Object>> rows = jdbcClient.sql(sql).params(params)
                .query((rs, rowNum) -> {
                    Map<String, Object> row = mapFields(rs, selected);
                    row.put("_key", keyFromValues(descriptor, row));
                    return row;
                }).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public SolarYearContext solarYearContext() {
        String schema = CalendarSqlNames.identifier(registry.schemaName());
        String sql = "SELECT MIN(P.YEAR_NO) AS MIN_YEAR, MAX(P.YEAR_NO) AS MAX_YEAR, "
                + "NVL(MAX(CASE WHEN D.CANONICAL_DATE=TRUNC(SYSDATE) THEN P.YEAR_NO END), "
                + "MAX(P.YEAR_NO) KEEP (DENSE_RANK FIRST ORDER BY ABS(D.CANONICAL_DATE-TRUNC(SYSDATE)))) AS CURRENT_YEAR "
                + "FROM " + schema + ".CALENDAR_DATE P JOIN " + schema + ".CALENDAR_DAY D ON D.DAY_ID=P.DAY_ID "
                + "WHERE P.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR'";
        return jdbcClient.sql(sql).query((rs, rowNum) -> new SolarYearContext(
                rs.getInt("CURRENT_YEAR"), rs.getInt("MIN_YEAR"), rs.getInt("MAX_YEAR")
        )).single();
    }


    public PageResponse<Map<String, Object>> searchCalendarDays(String text, Integer solarYear, int page, int size,
                                                                 String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (text != null && !text.isBlank()) {
            where.append(" AND (UPPER(TO_CHAR(D.DAY_ID)) LIKE :searchText")
                    .append(" OR UPPER(TO_CHAR(D.CANONICAL_DATE,'YYYY-MM-DD')) LIKE :searchText")
                    .append(" OR UPPER(NVL(P.FORMATTED_DATE,'')) LIKE :searchText")
                    .append(" OR UPPER(NVL(H.FORMATTED_DATE,'')) LIKE :searchText")
                    .append(" OR UPPER(W.WEEKDAY_NAME_FA) LIKE :searchText)");
            params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (solarYear != null) {
            where.append(" AND P.YEAR_NO=:solarYear");
            params.put("solarYear", solarYear);
        }
        String schema = CalendarSqlNames.identifier(registry.schemaName());
        String from = " FROM " + schema + ".CALENDAR_DAY D"
                + " JOIN " + schema + ".WEEKDAY W ON W.WEEKDAY_ID=D.WEEKDAY_ID"
                + " LEFT JOIN " + schema + ".CALENDAR_DATE P ON P.DAY_ID=D.DAY_ID AND P.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR'"
                + " LEFT JOIN " + schema + ".CALENDAR_DATE H ON H.DAY_ID=D.DAY_ID AND H.CALENDAR_SYSTEM_CODE='HIJRI_CIVIL'";
        long total = jdbcClient.sql("SELECT COUNT(*)" + from + where).params(params).query(Long.class).single();
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);
        String order = switch (sortBy == null ? "" : sortBy) {
            case "canonicalDate" -> "D.CANONICAL_DATE";
            case "solarDate" -> "P.YEAR_NO, P.MONTH_NO, P.DAY_NO";
            case "hijriDate" -> "H.YEAR_NO, H.MONTH_NO, H.DAY_NO";
            case "weekdayName" -> "W.IR_WEEKDAY_NO";
            case "epochDay" -> "D.EPOCH_DAY";
            case "julianDayNumber" -> "D.JULIAN_DAY_NUMBER";
            default -> "D.DAY_ID";
        };
        String sql = "SELECT D.DAY_ID, D.CANONICAL_DATE, D.EPOCH_DAY, D.JULIAN_DAY_NUMBER, D.WEEKDAY_ID, "
                + "D.ISO_WEEKDAY_NO, D.IR_WEEKDAY_NO, W.WEEKDAY_NAME_FA, P.FORMATTED_DATE AS SOLAR_DATE, "
                + "H.FORMATTED_DATE AS HIJRI_DATE" + from + where + " ORDER BY " + order + " "
                + normalizedDirection(direction) + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        List<Map<String, Object>> rows = jdbcClient.sql(sql).params(params).query((rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            BigDecimal id = rs.getBigDecimal("DAY_ID");
            Date canonical = rs.getDate("CANONICAL_DATE");
            row.put("dayId", id);
            row.put("canonicalDate", canonical == null ? null : canonical.toLocalDate());
            row.put("epochDay", rs.getBigDecimal("EPOCH_DAY"));
            row.put("julianDayNumber", rs.getBigDecimal("JULIAN_DAY_NUMBER"));
            row.put("weekdayId", rs.getBigDecimal("WEEKDAY_ID"));
            row.put("isoWeekdayNo", rs.getBigDecimal("ISO_WEEKDAY_NO"));
            row.put("irWeekdayNo", rs.getBigDecimal("IR_WEEKDAY_NO"));
            row.put("weekdayName", rs.getString("WEEKDAY_NAME_FA"));
            row.put("solarDate", rs.getString("SOLAR_DATE"));
            row.put("hijriDate", rs.getString("HIJRI_DATE"));
            row.put("_key", id == null ? "" : id.stripTrailingZeros().toPlainString());
            return row;
        }).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public PageResponse<Map<String, Object>> searchBusinessCalendarDays(String text, Integer solarYear, int page, int size,
                                                                         String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (text != null && !text.isBlank()) {
            where.append(" AND (UPPER(TO_CHAR(B.BUSINESS_CALENDAR_DAY_ID)) LIKE :searchText")
                    .append(" OR UPPER(BC.CALENDAR_CODE) LIKE :searchText")
                    .append(" OR UPPER(BC.CALENDAR_NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(TO_CHAR(B.DAY_ID)) LIKE :searchText")
                    .append(" OR UPPER(NVL(P.FORMATTED_DATE,'')) LIKE :searchText")
                    .append(" OR UPPER(W.WEEKDAY_NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(NVL(B.STATUS_SOURCE_CODE,'')) LIKE :searchText)");
            params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (solarYear != null) {
            where.append(" AND P.YEAR_NO=:solarYear");
            params.put("solarYear", solarYear);
        }
        String schema = CalendarSqlNames.identifier(registry.schemaName());
        String from = " FROM " + schema + ".BUSINESS_CALENDAR_DAY B"
                + " JOIN " + schema + ".BUSINESS_CALENDAR BC ON BC.BUSINESS_CALENDAR_ID=B.BUSINESS_CALENDAR_ID"
                + " JOIN " + schema + ".CALENDAR_DAY D ON D.DAY_ID=B.DAY_ID"
                + " JOIN " + schema + ".WEEKDAY W ON W.WEEKDAY_ID=D.WEEKDAY_ID"
                + " LEFT JOIN " + schema + ".CALENDAR_DATE P ON P.DAY_ID=D.DAY_ID AND P.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR'";
        long total = jdbcClient.sql("SELECT COUNT(*)" + from + where).params(params).query(Long.class).single();
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);
        String order = switch (sortBy == null ? "" : sortBy) {
            case "businessCalendarName" -> "BC.CALENDAR_NAME_FA";
            case "solarDate" -> "P.YEAR_NO, P.MONTH_NO, P.DAY_NO";
            case "weekdayName" -> "W.IR_WEEKDAY_NO";
            case "workingDay" -> "B.IS_WORKING_DAY";
            case "bankHoliday" -> "B.IS_BANK_HOLIDAY";
            case "statusSourceCode" -> "B.STATUS_SOURCE_CODE";
            default -> "D.CANONICAL_DATE";
        };
        String sql = "SELECT B.BUSINESS_CALENDAR_DAY_ID, B.BUSINESS_CALENDAR_ID, B.DAY_ID, B.IS_WORKING_DAY, "
                + "B.IS_BANK_HOLIDAY, B.IS_SETTLEMENT_DAY, B.IS_CLEARING_DAY, B.IS_POSTING_DAY, B.OPEN_TIME, B.CLOSE_TIME, "
                + "B.STATUS_SOURCE_CODE, BC.CALENDAR_CODE, BC.CALENDAR_NAME_FA, D.CANONICAL_DATE, W.WEEKDAY_NAME_FA, "
                + "P.FORMATTED_DATE AS SOLAR_DATE" + from + where + " ORDER BY " + order + " "
                + normalizedDirection(direction) + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        List<Map<String, Object>> rows = jdbcClient.sql(sql).params(params).query((rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            BigDecimal id = rs.getBigDecimal("BUSINESS_CALENDAR_DAY_ID");
            Date canonical = rs.getDate("CANONICAL_DATE");
            row.put("businessCalendarDayId", id);
            row.put("businessCalendarId", rs.getBigDecimal("BUSINESS_CALENDAR_ID"));
            row.put("dayId", rs.getBigDecimal("DAY_ID"));
            row.put("workingDay", "Y".equalsIgnoreCase(rs.getString("IS_WORKING_DAY")));
            row.put("bankHoliday", "Y".equalsIgnoreCase(rs.getString("IS_BANK_HOLIDAY")));
            row.put("settlementDay", "Y".equalsIgnoreCase(rs.getString("IS_SETTLEMENT_DAY")));
            row.put("clearingDay", "Y".equalsIgnoreCase(rs.getString("IS_CLEARING_DAY")));
            row.put("postingDay", "Y".equalsIgnoreCase(rs.getString("IS_POSTING_DAY")));
            row.put("openTime", rs.getString("OPEN_TIME"));
            row.put("closeTime", rs.getString("CLOSE_TIME"));
            row.put("statusSourceCode", rs.getString("STATUS_SOURCE_CODE"));
            row.put("businessCalendarCode", rs.getString("CALENDAR_CODE"));
            row.put("businessCalendarName", rs.getString("CALENDAR_NAME_FA"));
            row.put("canonicalDate", canonical == null ? null : canonical.toLocalDate());
            row.put("weekdayName", rs.getString("WEEKDAY_NAME_FA"));
            row.put("solarDate", rs.getString("SOLAR_DATE"));
            row.put("_key", id == null ? "" : id.stripTrailingZeros().toPlainString());
            return row;
        }).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public PageResponse<Map<String, Object>> searchOccasionRules(String text, int page, int size,
                                                                  String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (text != null && !text.isBlank()) {
            where.append(" AND (UPPER(O.OCCASION_NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(O.OCCASION_CODE) LIKE :searchText")
                    .append(" OR UPPER(R.RULE_TYPE_CODE) LIKE :searchText")
                    .append(" OR UPPER(S.CALENDAR_SYSTEM_NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(NVL(M.MONTH_NAME_FA,'')) LIKE :searchText)");
            params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        String schema = CalendarSqlNames.identifier(registry.schemaName());
        String from = " FROM " + schema + ".OCCASION_RULE R"
                + " JOIN " + schema + ".OCCASION O ON O.OCCASION_ID=R.OCCASION_ID"
                + " JOIN " + schema + ".CALENDAR_SYSTEM S ON S.CALENDAR_SYSTEM_CODE=R.DATE_SYSTEM_CODE"
                + " LEFT JOIN " + schema + ".CALENDAR_MONTH M ON M.CALENDAR_SYSTEM_CODE=R.DATE_SYSTEM_CODE AND M.MONTH_NO=R.MONTH_NO";
        long total = jdbcClient.sql("SELECT COUNT(*)" + from + where).params(params).query(Long.class).single();
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);
        String order = switch (sortBy == null ? "" : sortBy) {
            case "occasionName" -> "O.OCCASION_NAME_FA";
            case "ruleTypeCode" -> "R.RULE_TYPE_CODE";
            case "dateSystemName" -> "S.CALENDAR_SYSTEM_NAME_FA";
            case "monthNo" -> "R.MONTH_NO, R.DAY_NO";
            case "effectiveFromYear" -> "R.EFFECTIVE_FROM_YEAR";
            case "priorityNo" -> "R.PRIORITY_NO";
            default -> "R.OCCASION_RULE_ID";
        };
        String sql = "SELECT R.OCCASION_RULE_ID, R.OCCASION_ID, R.RULE_TYPE_CODE, R.DATE_SYSTEM_CODE, R.MONTH_NO, R.DAY_NO, "
                + "R.DURATION_DAYS, R.EFFECTIVE_FROM_YEAR, R.EFFECTIVE_TO_YEAR, R.PRIORITY_NO, R.ACTIVE_FLAG, "
                + "O.OCCASION_CODE, O.OCCASION_NAME_FA, S.CALENDAR_SYSTEM_NAME_FA, M.MONTH_NAME_FA" + from + where
                + " ORDER BY " + order + " " + normalizedDirection(direction)
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        List<Map<String, Object>> rows = jdbcClient.sql(sql).params(params).query((rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            BigDecimal id = rs.getBigDecimal("OCCASION_RULE_ID");
            row.put("occasionRuleId", id);
            row.put("occasionId", rs.getBigDecimal("OCCASION_ID"));
            row.put("ruleTypeCode", rs.getString("RULE_TYPE_CODE"));
            row.put("dateSystemCode", rs.getString("DATE_SYSTEM_CODE"));
            row.put("monthNo", rs.getBigDecimal("MONTH_NO"));
            row.put("dayNo", rs.getBigDecimal("DAY_NO"));
            row.put("durationDays", rs.getBigDecimal("DURATION_DAYS"));
            row.put("effectiveFromYear", rs.getBigDecimal("EFFECTIVE_FROM_YEAR"));
            row.put("effectiveToYear", rs.getBigDecimal("EFFECTIVE_TO_YEAR"));
            row.put("priorityNo", rs.getBigDecimal("PRIORITY_NO"));
            row.put("activeFlag", "Y".equalsIgnoreCase(rs.getString("ACTIVE_FLAG")));
            row.put("occasionCode", rs.getString("OCCASION_CODE"));
            row.put("occasionName", rs.getString("OCCASION_NAME_FA"));
            row.put("dateSystemName", rs.getString("CALENDAR_SYSTEM_NAME_FA"));
            row.put("monthName", rs.getString("MONTH_NAME_FA"));
            row.put("_key", id == null ? "" : id.stripTrailingZeros().toPlainString());
            return row;
        }).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public PageResponse<Map<String, Object>> searchOccasionOccurrences(String text, Integer solarYear, int page, int size,
                                                                        String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (text != null && !text.isBlank()) {
            where.append(" AND (UPPER(O.OCCASION_NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(O.OCCASION_CODE) LIKE :searchText")
                    .append(" OR UPPER(C.CATEGORY_NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(X.OCCURRENCE_STATUS_CODE) LIKE :searchText")
                    .append(" OR UPPER(NVL(SP.FORMATTED_DATE,'')) LIKE :searchText")
                    .append(" OR UPPER(TO_CHAR(SD.CANONICAL_DATE,'YYYY-MM-DD')) LIKE :searchText")
                    .append(" OR UPPER(NVL(X.SOURCE_AUTHORITY_CODE,'')) LIKE :searchText)");
            params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (solarYear != null) {
            where.append(" AND SP.YEAR_NO=:solarYear");
            params.put("solarYear", solarYear);
        }
        String schema = CalendarSqlNames.identifier(registry.schemaName());
        String from = " FROM " + schema + ".OCCASION_OCCURRENCE X"
                + " JOIN " + schema + ".OCCASION O ON O.OCCASION_ID=X.OCCASION_ID"
                + " JOIN " + schema + ".OCCASION_CATEGORY C ON C.OCCASION_CATEGORY_ID=O.OCCASION_CATEGORY_ID"
                + " JOIN " + schema + ".CALENDAR_DAY SD ON SD.DAY_ID=X.START_DAY_ID"
                + " JOIN " + schema + ".CALENDAR_DAY ED ON ED.DAY_ID=X.END_DAY_ID"
                + " LEFT JOIN " + schema + ".CALENDAR_DATE SP ON SP.DAY_ID=SD.DAY_ID AND SP.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR'"
                + " LEFT JOIN " + schema + ".CALENDAR_DATE EP ON EP.DAY_ID=ED.DAY_ID AND EP.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR'";
        long total = jdbcClient.sql("SELECT COUNT(*)" + from + where).params(params).query(Long.class).single();
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);
        String order = switch (sortBy == null ? "" : sortBy) {
            case "occasionName" -> "O.OCCASION_NAME_FA";
            case "categoryName" -> "C.CATEGORY_NAME_FA";
            case "startSolarDate" -> "SD.CANONICAL_DATE";
            case "occurrenceStatusCode" -> "X.OCCURRENCE_STATUS_CODE";
            case "sourceAuthorityCode" -> "X.SOURCE_AUTHORITY_CODE";
            default -> "SD.CANONICAL_DATE";
        };
        String sql = "SELECT X.OCCASION_OCCURRENCE_ID, X.OCCASION_ID, X.START_DAY_ID, X.END_DAY_ID, X.OCCURRENCE_STATUS_CODE, "
                + "X.SOURCE_AUTHORITY_CODE, X.SOURCE_REFERENCE, X.IS_OFFICIAL, X.IS_CONFIRMED, O.OCCASION_CODE, O.OCCASION_NAME_FA, "
                + "C.CATEGORY_CODE, C.CATEGORY_NAME_FA, SD.CANONICAL_DATE AS START_CANONICAL_DATE, ED.CANONICAL_DATE AS END_CANONICAL_DATE, "
                + "SP.FORMATTED_DATE AS START_SOLAR_DATE, EP.FORMATTED_DATE AS END_SOLAR_DATE" + from + where
                + " ORDER BY " + order + " " + normalizedDirection(direction)
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        List<Map<String, Object>> rows = jdbcClient.sql(sql).params(params).query((rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            BigDecimal id = rs.getBigDecimal("OCCASION_OCCURRENCE_ID");
            Date startDate = rs.getDate("START_CANONICAL_DATE");
            Date endDate = rs.getDate("END_CANONICAL_DATE");
            row.put("occasionOccurrenceId", id);
            row.put("occasionId", rs.getBigDecimal("OCCASION_ID"));
            row.put("startDayId", rs.getBigDecimal("START_DAY_ID"));
            row.put("endDayId", rs.getBigDecimal("END_DAY_ID"));
            row.put("occurrenceStatusCode", rs.getString("OCCURRENCE_STATUS_CODE"));
            row.put("sourceAuthorityCode", rs.getString("SOURCE_AUTHORITY_CODE"));
            row.put("sourceReference", rs.getString("SOURCE_REFERENCE"));
            row.put("official", "Y".equalsIgnoreCase(rs.getString("IS_OFFICIAL")));
            row.put("confirmed", "Y".equalsIgnoreCase(rs.getString("IS_CONFIRMED")));
            row.put("occasionCode", rs.getString("OCCASION_CODE"));
            row.put("occasionName", rs.getString("OCCASION_NAME_FA"));
            row.put("categoryCode", rs.getString("CATEGORY_CODE"));
            row.put("categoryName", rs.getString("CATEGORY_NAME_FA"));
            row.put("startCanonicalDate", startDate == null ? null : startDate.toLocalDate());
            row.put("endCanonicalDate", endDate == null ? null : endDate.toLocalDate());
            row.put("startSolarDate", rs.getString("START_SOLAR_DATE"));
            row.put("endSolarDate", rs.getString("END_SOLAR_DATE"));
            row.put("_key", id == null ? "" : id.stripTrailingZeros().toPlainString());
            return row;
        }).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public PageResponse<Map<String, Object>> searchCalendarDayOccasions(String text, Integer solarYear, int page, int size,
                                                                         String sortBy, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (text != null && !text.isBlank()) {
            where.append(" AND (UPPER(O.OCCASION_NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(O.OCCASION_CODE) LIKE :searchText")
                    .append(" OR UPPER(NVL(P.FORMATTED_DATE,'')) LIKE :searchText")
                    .append(" OR UPPER(W.WEEKDAY_NAME_FA) LIKE :searchText")
                    .append(" OR UPPER(TO_CHAR(CDO.DAY_ID)) LIKE :searchText)");
            params.put("searchText", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        if (solarYear != null) {
            where.append(" AND P.YEAR_NO=:solarYear");
            params.put("solarYear", solarYear);
        }
        String schema = CalendarSqlNames.identifier(registry.schemaName());
        String from = " FROM " + schema + ".CALENDAR_DAY_OCCASION CDO"
                + " JOIN " + schema + ".CALENDAR_DAY D ON D.DAY_ID=CDO.DAY_ID"
                + " JOIN " + schema + ".WEEKDAY W ON W.WEEKDAY_ID=D.WEEKDAY_ID"
                + " JOIN " + schema + ".OCCASION_OCCURRENCE X ON X.OCCASION_OCCURRENCE_ID=CDO.OCCASION_OCCURRENCE_ID"
                + " JOIN " + schema + ".OCCASION O ON O.OCCASION_ID=X.OCCASION_ID"
                + " LEFT JOIN " + schema + ".CALENDAR_DATE P ON P.DAY_ID=D.DAY_ID AND P.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR'";
        long total = jdbcClient.sql("SELECT COUNT(*)" + from + where).params(params).query(Long.class).single();
        params.put("offset", safePage * safeSize);
        params.put("pageSize", safeSize);
        String order = switch (sortBy == null ? "" : sortBy) {
            case "solarDate" -> "D.CANONICAL_DATE";
            case "weekdayName" -> "W.IR_WEEKDAY_NO";
            case "occasionName" -> "O.OCCASION_NAME_FA";
            case "displayPriority" -> "CDO.DISPLAY_PRIORITY";
            case "primaryOccasion" -> "CDO.PRIMARY_OCCASION_FLAG";
            default -> "D.CANONICAL_DATE, CDO.DISPLAY_PRIORITY";
        };
        String sql = "SELECT CDO.CALENDAR_DAY_OCCASION_ID, CDO.DAY_ID, CDO.OCCASION_OCCURRENCE_ID, CDO.DISPLAY_PRIORITY, "
                + "CDO.PRIMARY_OCCASION_FLAG, D.CANONICAL_DATE, W.WEEKDAY_NAME_FA, P.FORMATTED_DATE AS SOLAR_DATE, "
                + "O.OCCASION_CODE, O.OCCASION_NAME_FA" + from + where + " ORDER BY " + order + " "
                + normalizedDirection(direction) + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        List<Map<String, Object>> rows = jdbcClient.sql(sql).params(params).query((rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            BigDecimal id = rs.getBigDecimal("CALENDAR_DAY_OCCASION_ID");
            Date canonical = rs.getDate("CANONICAL_DATE");
            row.put("calendarDayOccasionId", id);
            row.put("dayId", rs.getBigDecimal("DAY_ID"));
            row.put("occasionOccurrenceId", rs.getBigDecimal("OCCASION_OCCURRENCE_ID"));
            row.put("displayPriority", rs.getBigDecimal("DISPLAY_PRIORITY"));
            row.put("primaryOccasion", "Y".equalsIgnoreCase(rs.getString("PRIMARY_OCCASION_FLAG")));
            row.put("canonicalDate", canonical == null ? null : canonical.toLocalDate());
            row.put("weekdayName", rs.getString("WEEKDAY_NAME_FA"));
            row.put("solarDate", rs.getString("SOLAR_DATE"));
            row.put("occasionCode", rs.getString("OCCASION_CODE"));
            row.put("occasionName", rs.getString("OCCASION_NAME_FA"));
            row.put("_key", id == null ? "" : id.stripTrailingZeros().toPlainString());
            return row;
        }).list();
        return new PageResponse<>(rows, total, safePage, safeSize);
    }

    public Optional<RecordResponse> find(TableDescriptor descriptor, String encodedKey) {
        ParsedKey key = parseKey(descriptor, encodedKey);
        String sql = "SELECT " + selectList(descriptor.fields(), "T") + " FROM " + table(descriptor) + " T WHERE " + key.where();
        return jdbcClient.sql(sql).paramSource(key.params())
                .query((rs, rowNum) -> {
                    Map<String, Object> values = mapFields(rs, descriptor.fields());
                    return new RecordResponse(keyFromValues(descriptor, values), values);
                }).optional();
    }

    public RecordResponse insert(TableDescriptor descriptor, Map<String, Object> values) {
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>(values);
        if (descriptor.autoNumericPrimaryKey()) {
            FieldDescriptor key = descriptor.keyFields().getFirst();
            jdbcClient.sql("LOCK TABLE " + table(descriptor) + " IN SHARE ROW EXCLUSIVE MODE").update();
            long nextId = jdbcClient.sql("SELECT NVL(MAX(" + CalendarSqlNames.identifier(key.columnName()) + "), 0) + 1 FROM " + table(descriptor))
                    .query(Long.class).single();
            normalized.put(key.apiName(), nextId);
        }

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (FieldDescriptor field : descriptor.fields()) {
            if (field.readOnly() && !(field.key() && descriptor.autoNumericPrimaryKey())) continue;
            Object value = normalized.get(field.apiName());
            if (value == null && field.defaultValue() != null) value = field.defaultValue();
            columns.add(CalendarSqlNames.identifier(field.columnName()));
            placeholders.add(":" + field.apiName());
            params.addValue(field.apiName(), databaseValue(field, value), sqlType(field));
        }
        String sql = "INSERT INTO " + table(descriptor) + " (" + String.join(", ", columns) + ") VALUES ("
                + String.join(", ", placeholders) + ")";
        jdbcClient.sql(sql).paramSource(params).update();
        return find(descriptor, keyFromValues(descriptor, normalized)).orElseThrow();
    }

    public Optional<RecordResponse> update(TableDescriptor descriptor, String encodedKey, Map<String, Object> values) {
        ParsedKey key = parseKey(descriptor, encodedKey);
        List<String> assignments = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (FieldDescriptor field : descriptor.fields()) {
            if (field.readOnly() || field.key()) continue;
            assignments.add(CalendarSqlNames.identifier(field.columnName()) + " = :" + field.apiName());
            Object value = values.get(field.apiName());
            if (value == null && field.defaultValue() != null) value = field.defaultValue();
            params.addValue(field.apiName(), databaseValue(field, value), sqlType(field));
        }
        key.params().getValues().forEach(params::addValue);
        if (assignments.isEmpty()) return find(descriptor, encodedKey);
        int changed = jdbcClient.sql("UPDATE " + table(descriptor) + " SET " + String.join(", ", assignments) + " WHERE " + key.where())
                .paramSource(params).update();
        return changed == 1 ? find(descriptor, encodedKey) : Optional.empty();
    }

    public boolean delete(TableDescriptor descriptor, String encodedKey) {
        ParsedKey key = parseKey(descriptor, encodedKey);
        return jdbcClient.sql("DELETE FROM " + table(descriptor) + " WHERE " + key.where())
                .paramSource(key.params()).update() == 1;
    }

    public List<LookupOption> lookup(String resource, String text, int limit) {
        if ("calendar-days".equals(resource)) return dayLookup(text, limit);
        if ("occasion-occurrences".equals(resource)) return occasionOccurrenceLookup(text, limit);
        TableDescriptor descriptor = registry.require(resource);
        if (descriptor.keyFields().size() != 1) return List.of();
        FieldDescriptor valueField = descriptor.keyFields().getFirst();
        FieldDescriptor codeField = descriptor.field(descriptor.lookupCodeApiName());
        FieldDescriptor nameField = descriptor.field(descriptor.lookupNameApiName());
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (text != null && !text.isBlank()) {
            String codeExpr = textExpression(codeField, "T." + CalendarSqlNames.identifier(codeField.columnName()));
            String nameExpr = textExpression(nameField, "T." + CalendarSqlNames.identifier(nameField.columnName()));
            where.add("(UPPER(" + codeExpr + ") LIKE :text OR UPPER(" + nameExpr + ") LIKE :text)");
            params.put("text", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        descriptor.fields().stream().filter(f -> f.apiName().equals("activeFlag")).findFirst()
                .ifPresent(field -> where.add("T." + CalendarSqlNames.identifier(field.columnName()) + " = 'Y'"));
        params.put("limit", safeLimit);
        String codeSelect = textExpression(codeField, "T." + CalendarSqlNames.identifier(codeField.columnName()));
        String nameSelect = textExpression(nameField, "T." + CalendarSqlNames.identifier(nameField.columnName()));
        String sql = "SELECT T." + CalendarSqlNames.identifier(valueField.columnName()) + " AS VALUE_COL, "
                + codeSelect + " AS CODE_COL, " + nameSelect + " AS LABEL_COL "
                + "FROM " + table(descriptor) + " T"
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY T." + CalendarSqlNames.identifier(nameField.columnName()) + " FETCH FIRST :limit ROWS ONLY";
        return jdbcClient.sql(sql).params(params).query((rs, rowNum) -> new LookupOption(
                readKeyValue(rs, "VALUE_COL", valueField), rs.getString("CODE_COL"), rs.getString("LABEL_COL")
        )).list();
    }

    private List<LookupOption> dayLookup(String text, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        boolean hasText = text != null && !text.isBlank();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", safeLimit);
        if (hasText) params.put("text", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        String schema = CalendarSqlNames.identifier(registry.schemaName());
        String filter = hasText
                ? " AND (UPPER(TO_CHAR(D.DAY_ID)) LIKE :text OR UPPER(TO_CHAR(D.CANONICAL_DATE,'YYYY-MM-DD')) LIKE :text "
                  + "OR EXISTS (SELECT 1 FROM " + schema + ".CALENDAR_DATE SX WHERE SX.DAY_ID=D.DAY_ID AND UPPER(SX.FORMATTED_DATE) LIKE :text))"
                : " AND D.CANONICAL_DATE BETWEEN TRUNC(SYSDATE)-31 AND TRUNC(SYSDATE)+31";
        String sql = """
                SELECT D.DAY_ID,
                       TO_CHAR(D.CANONICAL_DATE,'YYYY-MM-DD') AS GREGORIAN_DATE,
                       MAX(CASE WHEN CD.CALENDAR_SYSTEM_CODE='SOLAR_HIJRI_IR' THEN CD.FORMATTED_DATE END) AS SOLAR_DATE,
                       MAX(CASE WHEN CD.CALENDAR_SYSTEM_CODE='HIJRI_CIVIL' THEN CD.FORMATTED_DATE END) AS HIJRI_DATE,
                       W.WEEKDAY_NAME_FA
                  FROM %s.CALENDAR_DAY D
                  JOIN %s.WEEKDAY W ON W.WEEKDAY_ID=D.WEEKDAY_ID
                  JOIN %s.CALENDAR_DATE CD ON CD.DAY_ID=D.DAY_ID
                 WHERE 1=1 %s
                 GROUP BY D.DAY_ID, D.CANONICAL_DATE, W.WEEKDAY_NAME_FA
                 ORDER BY D.CANONICAL_DATE
                 FETCH FIRST :limit ROWS ONLY
                """.formatted(schema, schema, schema, filter);
        return jdbcClient.sql(sql).params(params).query((rs, rowNum) -> {
            long dayId = rs.getLong("DAY_ID");
            String gregorian = rs.getString("GREGORIAN_DATE");
            String solar = rs.getString("SOLAR_DATE");
            String hijri = rs.getString("HIJRI_DATE");
            String weekday = rs.getString("WEEKDAY_NAME_FA");
            String label = "%s · %s · میلادی %s · قمری %s".formatted(weekday, solar, gregorian, hijri);
            return new LookupOption(dayId, gregorian, label);
        }).list();
    }

    private List<LookupOption> occasionOccurrenceLookup(String text, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", safeLimit);
        String where = "";
        if (text != null && !text.isBlank()) {
            where = " WHERE UPPER(O.OCCASION_NAME_FA) LIKE :text OR UPPER(O.OCCASION_CODE) LIKE :text OR TO_CHAR(X.OCCASION_OCCURRENCE_ID) LIKE :text";
            params.put("text", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        String schema = CalendarSqlNames.identifier(registry.schemaName());
        String sql = "SELECT X.OCCASION_OCCURRENCE_ID, O.OCCASION_CODE, O.OCCASION_NAME_FA, "
                + "TO_CHAR(S.CANONICAL_DATE,'YYYY-MM-DD') START_DATE, TO_CHAR(E.CANONICAL_DATE,'YYYY-MM-DD') END_DATE "
                + "FROM " + schema + ".OCCASION_OCCURRENCE X "
                + "JOIN " + schema + ".OCCASION O ON O.OCCASION_ID=X.OCCASION_ID "
                + "JOIN " + schema + ".CALENDAR_DAY S ON S.DAY_ID=X.START_DAY_ID "
                + "JOIN " + schema + ".CALENDAR_DAY E ON E.DAY_ID=X.END_DAY_ID"
                + where + " ORDER BY S.CANONICAL_DATE DESC FETCH FIRST :limit ROWS ONLY";
        return jdbcClient.sql(sql).params(params).query((rs, rowNum) -> {
            long id = rs.getLong("OCCASION_OCCURRENCE_ID");
            String code = rs.getString("OCCASION_CODE");
            String label = rs.getString("OCCASION_NAME_FA") + " · " + rs.getString("START_DATE")
                    + (rs.getString("START_DATE").equals(rs.getString("END_DATE")) ? "" : " تا " + rs.getString("END_DATE"));
            return new LookupOption(id, code, label);
        }).list();
    }

    private boolean hasDayId(TableDescriptor descriptor) {
        return descriptor.fields().stream().anyMatch(field -> "DAY_ID".equals(field.columnName()));
    }

    private String solarDateTable() {
        return CalendarSqlNames.qualified(registry.schemaName(), "CALENDAR_DATE");
    }

    private static void appendCondition(StringBuilder where, String condition) {
        if (where.length() == 0) where.append(" WHERE ").append(condition);
        else where.append(" AND ").append(condition);
    }

    private String searchWhere(TableDescriptor descriptor, String text) {
        if (text == null || text.isBlank() || descriptor.searchableFields().isEmpty()) return "";
        StringJoiner joiner = new StringJoiner(" OR ", " WHERE (", ")");
        for (FieldDescriptor field : descriptor.searchableFields()) {
            String col = "T." + CalendarSqlNames.identifier(field.columnName());
            String expr = "UPPER(" + textExpression(field, col) + ")";
            joiner.add(expr + " LIKE :searchText");
        }
        return joiner.toString();
    }

    private String orderBy(TableDescriptor descriptor, String requested) {
        if (requested != null && !requested.isBlank()) {
            Optional<FieldDescriptor> field = descriptor.gridFields().stream()
                    .filter(item -> item.apiName().equals(requested)).findFirst();
            if (field.isPresent()) return "T." + CalendarSqlNames.identifier(field.get().columnName());
        }
        return "T." + CalendarSqlNames.identifier(descriptor.keyFields().getFirst().columnName());
    }

    private static String textExpression(FieldDescriptor field, String qualifiedColumn) {
        if (field.type() == FieldType.DATE) return "TO_CHAR(" + qualifiedColumn + ",'YYYY-MM-DD')";
        if (field.type() == FieldType.NUMBER || isNumericLookup(field)) return "TO_CHAR(" + qualifiedColumn + ")";
        return qualifiedColumn;
    }

    private static String normalizedDirection(String direction) {
        return "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC";
    }

    private static String table(TableDescriptor descriptor) {
        return CalendarSqlNames.qualified(descriptor.schemaName(), descriptor.tableName());
    }

    private static String selectList(List<FieldDescriptor> fields, String alias) {
        return fields.stream().map(field -> alias + "." + CalendarSqlNames.identifier(field.columnName())
                        + " AS \"" + field.apiName() + "\"")
                .reduce((left, right) -> left + ", " + right).orElseThrow();
    }

    private static Map<String, Object> mapFields(ResultSet rs, List<FieldDescriptor> fields) throws SQLException {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (FieldDescriptor field : fields) values.put(field.apiName(), readValue(rs, field));
        return values;
    }

    private static Object readValue(ResultSet rs, FieldDescriptor field) throws SQLException {
        return switch (field.type()) {
            case BOOLEAN -> {
                String value = rs.getString(field.apiName());
                yield value == null ? null : "Y".equalsIgnoreCase(value);
            }
            case NUMBER -> rs.getBigDecimal(field.apiName());
            case DATE -> {
                Date date = rs.getDate(field.apiName());
                yield date == null ? null : date.toLocalDate();
            }
            case LOOKUP -> isNumericLookup(field) ? rs.getBigDecimal(field.apiName()) : rs.getString(field.apiName());
            case TEXT, TIME, SELECT -> rs.getString(field.apiName());
        };
    }

    private static Object readKeyValue(ResultSet rs, String columnAlias, FieldDescriptor field) throws SQLException {
        if (field.type() == FieldType.NUMBER || isNumericLookup(field)) return rs.getBigDecimal(columnAlias);
        return rs.getString(columnAlias);
    }

    private static int sqlType(FieldDescriptor field) {
        return switch (field.type()) {
            case NUMBER -> Types.NUMERIC;
            case DATE -> Types.DATE;
            case BOOLEAN, TEXT, TIME, SELECT -> Types.VARCHAR;
            case LOOKUP -> isNumericLookup(field) ? Types.NUMERIC : Types.VARCHAR;
        };
    }

    private static Object databaseValue(FieldDescriptor field, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) return null;
        return switch (field.type()) {
            case BOOLEAN -> (value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString())) ? "Y" : "N";
            case NUMBER -> value instanceof BigDecimal ? value : new BigDecimal(value.toString());
            case DATE -> value instanceof LocalDate d ? Date.valueOf(d) : Date.valueOf(value.toString());
            case LOOKUP -> isNumericLookup(field)
                    ? (value instanceof BigDecimal ? value : new BigDecimal(value.toString()))
                    : value.toString();
            case TEXT, TIME, SELECT -> value.toString();
        };
    }

    private static boolean isNumericLookup(FieldDescriptor field) {
        return field.type() == FieldType.LOOKUP && field.columnName().endsWith("_ID");
    }

    private static String keyFromValues(TableDescriptor descriptor, Map<String, Object> values) {
        return descriptor.keyFields().stream().map(field -> {
            Object value = values.get(field.apiName());
            if (value instanceof BigDecimal bd) return bd.stripTrailingZeros().toPlainString();
            return value == null ? "" : value.toString();
        }).reduce((left, right) -> left + "~" + right).orElseThrow();
    }

    private static ParsedKey parseKey(TableDescriptor descriptor, String encodedKey) {
        String[] parts = encodedKey.split("~", -1);
        List<FieldDescriptor> keys = descriptor.keyFields();
        if (parts.length != keys.size()) throw new IllegalArgumentException("Invalid calendar record key");
        StringJoiner where = new StringJoiner(" AND ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (int i = 0; i < keys.size(); i++) {
            FieldDescriptor field = keys.get(i);
            String name = "key" + i;
            where.add(CalendarSqlNames.identifier(field.columnName()) + " = :" + name);
            params.addValue(name, databaseValue(field, parts[i]), sqlType(field));
        }
        return new ParsedKey(where.toString(), params);
    }

    private record ParsedKey(String where, MapSqlParameterSource params) {}
}
