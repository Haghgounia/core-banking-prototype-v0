package com.behsazan.corebanking.calendar.datasetimport.oracle;

import com.behsazan.corebanking.calendar.datasetimport.application.CalendarDatasetCsvParser;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.CalendarDateCsvRow;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.CalendarDayCsvRow;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.DatasetStatus;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.DatasetVerification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Repository
public class CalendarDatasetImportRepository {
    private static final int BATCH_SIZE = 1000;
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private static final List<String> REQUIRED_SYSTEMS = List.of("GREGORIAN", "SOLAR_HIJRI_IR", "HIJRI_CIVIL");
    private static final List<String> REQUIRED_ALGORITHMS = List.of("PROLEPTIC_GREGORIAN", "PERSIAN_KHAYYAM_33", "HIJRI_CIVIL_TYPE_II");

    private final JdbcTemplate jdbcTemplate;
    private final String schemaName;

    public CalendarDatasetImportRepository(JdbcTemplate jdbcTemplate,
                                           @Value("${core-banking.schemas.calendar:CAL}") String schemaName) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaName = identifier(schemaName.trim().toUpperCase());
    }

    public DatasetStatus status() {
        long dayRows = count("CALENDAR_DAY");
        long dateRows = count("CALENDAR_DATE");
        long systemRows = count("CALENDAR_SYSTEM");
        long algorithmRows = count("CALENDAR_ALGORITHM");
        long weekdayRows = count("WEEKDAY");
        long monthRows = count("CALENDAR_MONTH");
        List<String> missing = missingSeedItems(weekdayRows, monthRows);
        return new DatasetStatus(
                schemaName, dayRows, dateRows, systemRows, algorithmRows, weekdayRows, monthRows,
                missing.isEmpty(), dayRows == 0 && dateRows == 0, missing
        );
    }

    public void lockDatasetTables() {
        jdbcTemplate.execute("LOCK TABLE " + table("CALENDAR_DAY") + " IN EXCLUSIVE MODE");
        jdbcTemplate.execute("LOCK TABLE " + table("CALENDAR_DATE") + " IN EXCLUSIVE MODE");
    }

    public long loadCalendarDays(MultipartFile file) {
        String sql = "INSERT INTO " + table("CALENDAR_DAY")
                + " (DAY_ID, CANONICAL_DATE, EPOCH_DAY, JULIAN_DAY_NUMBER, WEEKDAY_ID, ISO_WEEKDAY_NO, IR_WEEKDAY_NO)"
                + " VALUES (?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?)";
        try (BufferedReader reader = reader(file)) {
            CalendarDatasetCsvParser.requireHeader(reader.readLine(), CalendarDatasetCsvParser.DAY_HEADER, "calendar_day.csv");
            return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    String line;
                    long lineNumber = 1;
                    long rows = 0;
                    int pending = 0;
                    while ((line = readLine(reader)) != null) {
                        lineNumber++;
                        if (line.isBlank()) continue;
                        CalendarDayCsvRow row = CalendarDatasetCsvParser.parseDay(line, lineNumber);
                        statement.setLong(1, row.dayId());
                        statement.setString(2, row.canonicalDate().toString());
                        statement.setLong(3, row.epochDay());
                        statement.setLong(4, row.julianDayNumber());
                        statement.setInt(5, row.weekdayId());
                        statement.setInt(6, row.isoWeekdayNo());
                        statement.setInt(7, row.irWeekdayNo());
                        statement.addBatch();
                        rows++;
                        pending++;
                        if (pending == BATCH_SIZE) {
                            statement.executeBatch();
                            statement.clearBatch();
                            pending = 0;
                        }
                    }
                    if (pending > 0) statement.executeBatch();
                    return rows;
                }
            });
        } catch (IOException exception) {
            throw new UncheckedIOException("خواندن calendar_day.csv ناموفق بود.", exception);
        }
    }

    public long loadCalendarDates(MultipartFile file) {
        String sql = "INSERT INTO " + table("CALENDAR_DATE")
                + " (CALENDAR_DATE_ID, DAY_ID, CALENDAR_SYSTEM_CODE, YEAR_NO, MONTH_NO, DAY_NO, DAY_OF_YEAR, FORMATTED_DATE, IS_LEAP_YEAR, ALGORITHM_CODE)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (BufferedReader reader = reader(file)) {
            CalendarDatasetCsvParser.requireHeader(reader.readLine(), CalendarDatasetCsvParser.DATE_HEADER, "calendar_date.csv");
            return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    String line;
                    long lineNumber = 1;
                    long rows = 0;
                    int pending = 0;
                    while ((line = readLine(reader)) != null) {
                        lineNumber++;
                        if (line.isBlank()) continue;
                        CalendarDateCsvRow row = CalendarDatasetCsvParser.parseDate(line, lineNumber);
                        statement.setLong(1, row.calendarDateId());
                        statement.setLong(2, row.dayId());
                        statement.setString(3, row.calendarSystemCode());
                        statement.setInt(4, row.yearNo());
                        statement.setInt(5, row.monthNo());
                        statement.setInt(6, row.dayNo());
                        statement.setInt(7, row.dayOfYear());
                        statement.setString(8, row.formattedDate());
                        statement.setString(9, row.isLeapYear());
                        statement.setString(10, row.algorithmCode());
                        statement.addBatch();
                        rows++;
                        pending++;
                        if (pending == BATCH_SIZE) {
                            statement.executeBatch();
                            statement.clearBatch();
                            pending = 0;
                        }
                    }
                    if (pending > 0) statement.executeBatch();
                    return rows;
                }
            });
        } catch (IOException exception) {
            throw new UncheckedIOException("خواندن calendar_date.csv ناموفق بود.", exception);
        }
    }

    public DatasetVerification verify() {
        long badRepresentations = scalar("SELECT COUNT(*) FROM (SELECT DAY_ID FROM " + table("CALENDAR_DATE")
                + " GROUP BY DAY_ID HAVING COUNT(*) <> 3)");
        long gaps = scalar("SELECT COUNT(*) FROM (SELECT CANONICAL_DATE, LAG(CANONICAL_DATE) OVER (ORDER BY CANONICAL_DATE) PREV_DATE FROM "
                + table("CALENDAR_DAY") + ") WHERE PREV_DATE IS NOT NULL AND CANONICAL_DATE <> PREV_DATE + 1");
        long dayIdGap = scalar("SELECT CASE WHEN COUNT(*) = 0 THEN 0 WHEN MAX(DAY_ID)-MIN(DAY_ID)+1 = COUNT(*) THEN 0 ELSE 1 END FROM "
                + table("CALENDAR_DAY"));
        long badJdn = scalar("SELECT COUNT(*) FROM " + table("CALENDAR_DAY")
                + " WHERE JULIAN_DAY_NUMBER <> EPOCH_DAY + 2440588");
        long badWeekday = scalar("SELECT COUNT(*) FROM " + table("CALENDAR_DAY")
                + " WHERE ISO_WEEKDAY_NO <> (CANONICAL_DATE - TRUNC(CANONICAL_DATE,'IW') + 1)");
        long unknownSystems = scalar("SELECT COUNT(*) FROM " + table("CALENDAR_DATE")
                + " WHERE CALENDAR_SYSTEM_CODE NOT IN ('GREGORIAN','SOLAR_HIJRI_IR','HIJRI_CIVIL')");
        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT CALENDAR_SYSTEM_CODE, COUNT(*) CNT FROM " + table("CALENDAR_DATE")
                        + " GROUP BY CALENDAR_SYSTEM_CODE ORDER BY CALENDAR_SYSTEM_CODE",
                rs -> {
                    while (rs.next()) counts.put(rs.getString("CALENDAR_SYSTEM_CODE"), rs.getLong("CNT"));
                });
        return new DatasetVerification(badRepresentations, gaps, dayIdGap, badJdn, badWeekday, unknownSystems, counts);
    }

    public String[] canonicalRange() {
        return jdbcTemplate.queryForObject("SELECT MIN(CANONICAL_DATE) MIN_DATE, MAX(CANONICAL_DATE) MAX_DATE FROM " + table("CALENDAR_DAY"),
                (rs, rowNum) -> new String[]{toIso(rs.getDate("MIN_DATE")), toIso(rs.getDate("MAX_DATE"))});
    }

    public long countDays() { return count("CALENDAR_DAY"); }
    public long countDates() { return count("CALENDAR_DATE"); }

    private List<String> missingSeedItems(long weekdayRows, long monthRows) {
        ArrayList<String> missing = new ArrayList<>();
        Set<String> systems = new LinkedHashSet<>(jdbcTemplate.queryForList(
                "SELECT CALENDAR_SYSTEM_CODE FROM " + table("CALENDAR_SYSTEM")
                        + " WHERE CALENDAR_SYSTEM_CODE IN ('GREGORIAN','SOLAR_HIJRI_IR','HIJRI_CIVIL')",
                String.class));
        for (String required : REQUIRED_SYSTEMS) {
            if (!systems.contains(required)) missing.add("CALENDAR_SYSTEM." + required);
        }

        Set<String> algorithms = new LinkedHashSet<>(jdbcTemplate.queryForList(
                "SELECT ALGORITHM_CODE FROM " + table("CALENDAR_ALGORITHM")
                        + " WHERE ALGORITHM_CODE IN ('PROLEPTIC_GREGORIAN','PERSIAN_KHAYYAM_33','HIJRI_CIVIL_TYPE_II')",
                String.class));
        for (String required : REQUIRED_ALGORITHMS) {
            if (!algorithms.contains(required)) missing.add("CALENDAR_ALGORITHM." + required);
        }
        long requiredWeekdays = scalar("SELECT COUNT(*) FROM " + table("WEEKDAY") + " WHERE WEEKDAY_ID BETWEEN 1 AND 7");
        if (requiredWeekdays < 7 || weekdayRows < 7) missing.add("WEEKDAY (IDs 1..7 required)");

        long requiredMonths = scalar("SELECT COUNT(*) FROM " + table("CALENDAR_MONTH")
                + " WHERE CALENDAR_SYSTEM_CODE IN ('GREGORIAN','SOLAR_HIJRI_IR','HIJRI_CIVIL')");
        if (requiredMonths < 36 || monthRows < 36) missing.add("CALENDAR_MONTH (36 rows required for the three calendar systems)");
        return missing;
    }

    private BufferedReader reader(MultipartFile file) throws IOException {
        return new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8), 128 * 1024);
    }

    private static String readLine(BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private long count(String tableName) {
        return scalar("SELECT COUNT(*) FROM " + table(tableName));
    }

    private long scalar(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private String table(String tableName) {
        return schemaName + "." + identifier(tableName);
    }

    private static String identifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        }
        return value;
    }

    private static String toIso(Date value) {
        if (value == null) return null;
        LocalDate localDate = value.toLocalDate();
        return localDate.toString();
    }
}
