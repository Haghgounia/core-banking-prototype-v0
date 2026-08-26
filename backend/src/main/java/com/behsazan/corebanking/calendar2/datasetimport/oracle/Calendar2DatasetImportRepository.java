package com.behsazan.corebanking.calendar2.datasetimport.oracle;

import com.behsazan.corebanking.calendar2.datasetimport.application.Calendar2Csv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.regex.Pattern;

@Repository
public class Calendar2DatasetImportRepository {
    private static final int BATCH_SIZE = 1000;
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");

    private final JdbcTemplate jdbcTemplate;
    private final String schemaName;

    public Calendar2DatasetImportRepository(JdbcTemplate jdbcTemplate,
                                            @Value("${core-banking.schemas.calendar2:CAL2}") String schemaName) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaName = identifier(schemaName.trim().toUpperCase());
    }

    public String schemaName() { return schemaName; }

    public long loadCalendarSystem(Path file) {
        return load(file, "01_calendar_system.csv",
                List.of("CALENDAR_SYSTEM_ID","CALENDAR_CODE","NAME_FA","NAME_EN","CALENDAR_TYPE","ACTIVE_FLAG","CREATED_AT"),
                insert("CALENDAR_SYSTEM", "CALENDAR_SYSTEM_ID,CALENDAR_CODE,NAME_FA,NAME_EN,CALENDAR_TYPE,ACTIVE_FLAG,CREATED_AT"),
                (ps, v, line) -> {
                    number(ps,1,v.get(0)); text(ps,2,v.get(1)); text(ps,3,v.get(2)); text(ps,4,v.get(3));
                    text(ps,5,v.get(4)); text(ps,6,v.get(5)); timestamp(ps,7,v.get(6));
                });
    }

    public long loadSourceAuthority(Path file) {
        return load(file, "02_source_authority.csv",
                List.of("SOURCE_ID","SOURCE_CODE","NAME_FA","NAME_EN","SOURCE_TYPE","SOURCE_URI","COUNTRY_CODE","AUTHORITY_LEVEL","ACTIVE_FLAG"),
                insert("SOURCE_AUTHORITY", "SOURCE_ID,SOURCE_CODE,NAME_FA,NAME_EN,SOURCE_TYPE,SOURCE_URI,COUNTRY_CODE,AUTHORITY_LEVEL,ACTIVE_FLAG"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); text(ps,2,v.get(1)); text(ps,3,v.get(2)); text(ps,4,v.get(3)); text(ps,5,v.get(4));
                    text(ps,6,v.get(5)); text(ps,7,v.get(6)); text(ps,8,v.get(7)); text(ps,9,v.get(8));
                });
    }

    public long loadDatasetVersion(Path file) {
        return load(file, "03_dataset_version.csv",
                List.of("DATASET_VERSION_ID","VERSION_CODE","GENERATOR_VERSION","ALGORITHM_VERSION","SOURCE_VERSION","RANGE_START_DATE","RANGE_END_DATE","STATUS","CHECKSUM_SHA256","GENERATED_AT","APPROVED_BY","APPROVED_AT"),
                insert("DATASET_VERSION", "DATASET_VERSION_ID,VERSION_CODE,GENERATOR_VERSION,ALGORITHM_VERSION,SOURCE_VERSION,RANGE_START_DATE,RANGE_END_DATE,STATUS,CHECKSUM_SHA256,GENERATED_AT,APPROVED_BY,APPROVED_AT"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); text(ps,2,v.get(1)); text(ps,3,v.get(2)); text(ps,4,v.get(3)); text(ps,5,v.get(4));
                    date(ps,6,v.get(5)); date(ps,7,v.get(6)); text(ps,8,v.get(7)); text(ps,9,v.get(8)); timestamp(ps,10,v.get(9));
                    text(ps,11,v.get(10)); timestamp(ps,12,v.get(11));
                });
    }

    public long loadCalendarVariant(Path file) {
        return load(file, "04_calendar_variant.csv",
                List.of("CALENDAR_VARIANT_ID","CALENDAR_SYSTEM_ID","VARIANT_CODE","METHOD_TYPE","ALGORITHM_CODE","AUTHORITY_ID","VALID_FROM","VALID_TO","IS_DEFAULT","VERSION_NO","ACTIVE_FLAG"),
                insert("CALENDAR_VARIANT", "CALENDAR_VARIANT_ID,CALENDAR_SYSTEM_ID,VARIANT_CODE,METHOD_TYPE,ALGORITHM_CODE,AUTHORITY_ID,VALID_FROM,VALID_TO,IS_DEFAULT,VERSION_NO,ACTIVE_FLAG"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); number(ps,2,v.get(1)); text(ps,3,v.get(2)); text(ps,4,v.get(3)); text(ps,5,v.get(4));
                    number(ps,6,v.get(5)); date(ps,7,v.get(6)); date(ps,8,v.get(7)); text(ps,9,v.get(8)); number(ps,10,v.get(9)); text(ps,11,v.get(10));
                });
    }

    public long loadCalendarMonth(Path file) {
        return load(file, "05_calendar_month.csv",
                List.of("CALENDAR_MONTH_ID","CALENDAR_SYSTEM_ID","MONTH_NO","NAME_FA","NAME_EN","SHORT_NAME_FA","SHORT_NAME_EN","DISPLAY_ORDER"),
                insert("CALENDAR_MONTH", "CALENDAR_MONTH_ID,CALENDAR_SYSTEM_ID,MONTH_NO,NAME_FA,NAME_EN,SHORT_NAME_FA,SHORT_NAME_EN,DISPLAY_ORDER"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); number(ps,2,v.get(1)); number(ps,3,v.get(2)); text(ps,4,v.get(3)); text(ps,5,v.get(4));
                    text(ps,6,v.get(5)); text(ps,7,v.get(6)); number(ps,8,v.get(7));
                });
    }

    public long loadWeekday(Path file) {
        return load(file, "06_weekday.csv",
                List.of("WEEKDAY_ID","ISO_WEEKDAY_NO","NAME_FA","NAME_EN","SHORT_NAME_FA","SHORT_NAME_EN","IR_DISPLAY_ORDER"),
                insert("WEEKDAY", "WEEKDAY_ID,ISO_WEEKDAY_NO,NAME_FA,NAME_EN,SHORT_NAME_FA,SHORT_NAME_EN,IR_DISPLAY_ORDER"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); number(ps,2,v.get(1)); text(ps,3,v.get(2)); text(ps,4,v.get(3)); text(ps,5,v.get(4));
                    text(ps,6,v.get(5)); number(ps,7,v.get(6));
                });
    }

    public long loadCanonicalDay(Path file) {
        return load(file, "07_canonical_day.csv",
                List.of("DAY_ID","EPOCH_DAY","CANONICAL_DATE","ISO_DATE_TEXT","WEEKDAY_ID","ISO_WEEK_NO","ISO_WEEK_YEAR","CREATED_AT"),
                insert("CANONICAL_DAY", "DAY_ID,EPOCH_DAY,CANONICAL_DATE,ISO_DATE_TEXT,WEEKDAY_ID,ISO_WEEK_NO,ISO_WEEK_YEAR,CREATED_AT"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); number(ps,2,v.get(1)); date(ps,3,v.get(2)); text(ps,4,v.get(3)); number(ps,5,v.get(4));
                    number(ps,6,v.get(5)); number(ps,7,v.get(6)); timestamp(ps,8,v.get(7));
                });
    }

    public long loadCalendarDate(Path file) {
        return load(file, "08_calendar_date.csv",
                List.of("CALENDAR_DATE_ID","DAY_ID","CALENDAR_VARIANT_ID","YEAR_NO","MONTH_NO","DAY_NO","DAY_OF_YEAR","WEEKDAY_ID","IS_LEAP_YEAR","IS_LEAP_MONTH","MONTH_LENGTH","DATA_STATUS","DATASET_VERSION_ID"),
                insert("CALENDAR_DATE", "CALENDAR_DATE_ID,DAY_ID,CALENDAR_VARIANT_ID,YEAR_NO,MONTH_NO,DAY_NO,DAY_OF_YEAR,WEEKDAY_ID,IS_LEAP_YEAR,IS_LEAP_MONTH,MONTH_LENGTH,DATA_STATUS,DATASET_VERSION_ID"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); number(ps,2,v.get(1)); number(ps,3,v.get(2)); number(ps,4,v.get(3)); number(ps,5,v.get(4));
                    number(ps,6,v.get(5)); number(ps,7,v.get(6)); number(ps,8,v.get(7)); text(ps,9,v.get(8)); text(ps,10,v.get(9));
                    number(ps,11,v.get(10)); text(ps,12,v.get(11)); number(ps,13,v.get(12));
                });
    }

    public long loadEventType(Path file) {
        return load(file, "09_event_type.csv",
                List.of("EVENT_TYPE_ID","EVENT_TYPE_CODE","NAME_FA","NAME_EN","ACTIVE_FLAG"),
                insert("EVENT_TYPE", "EVENT_TYPE_ID,EVENT_TYPE_CODE,NAME_FA,NAME_EN,ACTIVE_FLAG"),
                (ps,v,line) -> { number(ps,1,v.get(0)); text(ps,2,v.get(1)); text(ps,3,v.get(2)); text(ps,4,v.get(3)); text(ps,5,v.get(4)); });
    }

    public long loadEvent(Path file) {
        return load(file, "10_event.csv",
                List.of("EVENT_ID","EVENT_CODE","EVENT_TYPE_ID","NAME_FA","NAME_EN","DESCRIPTION","RECURRENCE_TYPE","BASE_CALENDAR_SYSTEM_ID","OFFICIAL_FLAG","DEFAULT_HOLIDAY_FLAG","ACTIVE_FLAG"),
                insert("EVENT", "EVENT_ID,EVENT_CODE,EVENT_TYPE_ID,NAME_FA,NAME_EN,DESCRIPTION,RECURRENCE_TYPE,BASE_CALENDAR_SYSTEM_ID,OFFICIAL_FLAG,DEFAULT_HOLIDAY_FLAG,ACTIVE_FLAG"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); text(ps,2,v.get(1)); number(ps,3,v.get(2)); text(ps,4,v.get(3)); text(ps,5,v.get(4));
                    text(ps,6,v.get(5)); text(ps,7,v.get(6)); number(ps,8,v.get(7)); text(ps,9,v.get(8)); text(ps,10,v.get(9)); text(ps,11,v.get(10));
                });
    }

    public long loadEventOccurrence(Path file) {
        return load(file, "11_event_occurrence.csv",
                List.of("EVENT_OCCURRENCE_ID","EVENT_ID","DAY_ID","SOURCE_ID","DATA_STATUS","HOLIDAY_FLAG","START_TIME","END_TIME","DESCRIPTION","DATASET_VERSION_ID"),
                insert("EVENT_OCCURRENCE", "EVENT_OCCURRENCE_ID,EVENT_ID,DAY_ID,SOURCE_ID,DATA_STATUS,HOLIDAY_FLAG,START_TIME,END_TIME,DESCRIPTION,DATASET_VERSION_ID"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); number(ps,2,v.get(1)); number(ps,3,v.get(2)); number(ps,4,v.get(3)); text(ps,5,v.get(4));
                    text(ps,6,v.get(5)); timestamp(ps,7,v.get(6)); timestamp(ps,8,v.get(7)); text(ps,9,v.get(8)); number(ps,10,v.get(9));
                });
    }

    public long loadBusinessCalendar(Path file) {
        return load(file, "12_business_calendar.csv",
                List.of("BUSINESS_CALENDAR_ID","CALENDAR_CODE","NAME_FA","NAME_EN","COUNTRY_CODE","TIME_ZONE","ORGANIZATION_ID","VALID_FROM","VALID_TO","ACTIVE_FLAG"),
                insert("BUSINESS_CALENDAR", "BUSINESS_CALENDAR_ID,CALENDAR_CODE,NAME_FA,NAME_EN,COUNTRY_CODE,TIME_ZONE,ORGANIZATION_ID,VALID_FROM,VALID_TO,ACTIVE_FLAG"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); text(ps,2,v.get(1)); text(ps,3,v.get(2)); text(ps,4,v.get(3)); text(ps,5,v.get(4));
                    text(ps,6,v.get(5)); text(ps,7,v.get(6)); date(ps,8,v.get(7)); date(ps,9,v.get(8)); text(ps,10,v.get(9));
                });
    }

    public long loadBusinessCalendarDay(Path file) {
        return load(file, "13_business_calendar_day.csv",
                List.of("BUSINESS_CALENDAR_DAY_ID","BUSINESS_CALENDAR_ID","DAY_ID","DAY_STATUS","OPEN_TIME","CLOSE_TIME","IS_BUSINESS_DAY","IS_SETTLEMENT_DAY","IS_CLEARING_DAY","IS_PROCESSING_DAY","REASON_CODE","SOURCE_ID"),
                insert("BUSINESS_CALENDAR_DAY", "BUSINESS_CALENDAR_DAY_ID,BUSINESS_CALENDAR_ID,DAY_ID,DAY_STATUS,OPEN_TIME,CLOSE_TIME,IS_BUSINESS_DAY,IS_SETTLEMENT_DAY,IS_CLEARING_DAY,IS_PROCESSING_DAY,REASON_CODE,SOURCE_ID"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); number(ps,2,v.get(1)); number(ps,3,v.get(2)); text(ps,4,v.get(3)); timestamp(ps,5,v.get(4)); timestamp(ps,6,v.get(5));
                    text(ps,7,v.get(6)); text(ps,8,v.get(7)); text(ps,9,v.get(8)); text(ps,10,v.get(9)); text(ps,11,v.get(10)); number(ps,12,v.get(11));
                });
    }

    public long loadValidationRun(Path file) {
        return load(file, "14_validation_run.csv",
                List.of("VALIDATION_RUN_ID","DATASET_VERSION_ID","RUN_STARTED_AT","RUN_FINISHED_AT","VALIDATOR_NAME","VALIDATOR_VERSION","TOTAL_ASSERTIONS","PASSED_ASSERTIONS","FAILED_ASSERTIONS","RUN_STATUS","EVIDENCE_CHECKSUM"),
                insert("VALIDATION_RUN", "VALIDATION_RUN_ID,DATASET_VERSION_ID,RUN_STARTED_AT,RUN_FINISHED_AT,VALIDATOR_NAME,VALIDATOR_VERSION,TOTAL_ASSERTIONS,PASSED_ASSERTIONS,FAILED_ASSERTIONS,RUN_STATUS,EVIDENCE_CHECKSUM"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); number(ps,2,v.get(1)); timestamp(ps,3,v.get(2)); timestamp(ps,4,v.get(3)); text(ps,5,v.get(4));
                    text(ps,6,v.get(5)); number(ps,7,v.get(6)); number(ps,8,v.get(7)); number(ps,9,v.get(8)); text(ps,10,v.get(9)); text(ps,11,v.get(10));
                });
    }

    public long loadValidationResult(Path file) {
        return load(file, "15_validation_result.csv",
                List.of("VALIDATION_RESULT_ID","VALIDATION_RUN_ID","TEST_CODE","DAY_ID","CALENDAR_VARIANT_ID","RESULT_STATUS","EXPECTED_VALUE","ACTUAL_VALUE","ERROR_CODE","DETAILS"),
                insert("VALIDATION_RESULT", "VALIDATION_RESULT_ID,VALIDATION_RUN_ID,TEST_CODE,DAY_ID,CALENDAR_VARIANT_ID,RESULT_STATUS,EXPECTED_VALUE,ACTUAL_VALUE,ERROR_CODE,DETAILS"),
                (ps,v,line) -> {
                    number(ps,1,v.get(0)); number(ps,2,v.get(1)); text(ps,3,v.get(2)); number(ps,4,v.get(3)); number(ps,5,v.get(4));
                    text(ps,6,v.get(5)); text(ps,7,v.get(6)); text(ps,8,v.get(7)); text(ps,9,v.get(8)); text(ps,10,v.get(9));
                });
    }

    private long load(Path file, String fileName, List<String> expectedHeader, String sql, RowBinder binder) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Calendar2Csv.requireHeader(reader.readLine(), expectedHeader, fileName);
            return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    String line;
                    long lineNumber = 1;
                    long rows = 0;
                    int pending = 0;
                    try {
                        while ((line = reader.readLine()) != null) {
                            lineNumber++;
                            if (line.isBlank()) continue;
                            List<String> values = Calendar2Csv.split(line);
                            if (values.size() != expectedHeader.size()) {
                                throw new IllegalArgumentException("تعداد ستون‌های " + fileName + " در سطر " + lineNumber + " باید "
                                        + expectedHeader.size() + " باشد؛ مقدار فعلی " + values.size() + " است.");
                            }
                            try {
                                binder.bind(statement, values, lineNumber);
                            } catch (RuntimeException | SQLException exception) {
                                throw new IllegalArgumentException("خطا در " + fileName + " سطر " + lineNumber + ": " + exception.getMessage(), exception);
                            }
                            statement.addBatch();
                            rows++;
                            pending++;
                            if (pending == BATCH_SIZE) {
                                statement.executeBatch();
                                statement.clearBatch();
                                pending = 0;
                            }
                        }
                    } catch (IOException exception) {
                        throw new UncheckedIOException("خواندن " + fileName + " ناموفق بود.", exception);
                    }
                    if (pending > 0) statement.executeBatch();
                    return rows;
                }
            });
        } catch (IOException exception) {
            throw new UncheckedIOException("خواندن " + fileName + " ناموفق بود.", exception);
        }
    }

    private String insert(String table, String columnsCsv) {
        String[] columns = columnsCsv.split(",");
        return "INSERT INTO " + schemaName + "." + identifier(table) + " (" + columnsCsv + ") VALUES ("
                + String.join(",", java.util.Collections.nCopies(columns.length, "?")) + ")";
    }

    private static void text(PreparedStatement ps, int index, String value) throws SQLException {
        String v = clean(value);
        if (v == null) ps.setNull(index, Types.VARCHAR); else ps.setString(index, v);
    }

    private static void number(PreparedStatement ps, int index, String value) throws SQLException {
        String v = clean(value);
        if (v == null) ps.setNull(index, Types.NUMERIC); else ps.setBigDecimal(index, new BigDecimal(v));
    }

    private static void date(PreparedStatement ps, int index, String value) throws SQLException {
        String v = clean(value);
        if (v == null) ps.setNull(index, Types.DATE); else ps.setDate(index, Date.valueOf(v));
    }

    private static void timestamp(PreparedStatement ps, int index, String value) throws SQLException {
        String v = clean(value);
        if (v == null) ps.setNull(index, Types.TIMESTAMP); else ps.setTimestamp(index, Timestamp.valueOf(v.replace('T',' ')));
    }

    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String identifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        return value;
    }

    @FunctionalInterface
    private interface RowBinder {
        void bind(PreparedStatement statement, List<String> values, long lineNumber) throws SQLException;
    }
}
