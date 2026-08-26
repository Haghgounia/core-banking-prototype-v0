package com.behsazan.corebanking.calendar.datasetimport.application;

import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.CalendarDateCsvRow;
import com.behsazan.corebanking.calendar.datasetimport.domain.CalendarDatasetModels.CalendarDayCsvRow;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CalendarDatasetCsvParser {
    public static final List<String> DAY_HEADER = List.of(
            "DAY_ID", "CANONICAL_DATE", "EPOCH_DAY", "JULIAN_DAY_NUMBER", "WEEKDAY_ID", "ISO_WEEKDAY_NO", "IR_WEEKDAY_NO"
    );
    public static final List<String> DATE_HEADER = List.of(
            "CALENDAR_DATE_ID", "DAY_ID", "CALENDAR_SYSTEM_CODE", "YEAR_NO", "MONTH_NO", "DAY_NO", "DAY_OF_YEAR",
            "FORMATTED_DATE", "IS_LEAP_YEAR", "ALGORITHM_CODE"
    );
    public static final Set<String> CALENDAR_SYSTEMS = Set.of("GREGORIAN", "SOLAR_HIJRI_IR", "HIJRI_CIVIL");

    private CalendarDatasetCsvParser() {}

    public static void requireHeader(String line, List<String> expected, String fileLabel) {
        if (line == null) throw error(fileLabel + " فاقد Header است.");
        List<String> actual = split(line);
        if (!actual.isEmpty()) actual.set(0, stripBom(actual.getFirst()));
        if (!actual.equals(expected)) {
            throw error("Header فایل " + fileLabel + " معتبر نیست. ستون‌های مورد انتظار: " + String.join(",", expected));
        }
    }

    public static CalendarDayCsvRow parseDay(String line, long lineNumber) {
        List<String> values = requireColumnCount(line, DAY_HEADER.size(), "calendar_day.csv", lineNumber);
        try {
            return new CalendarDayCsvRow(
                    Long.parseLong(values.get(0).trim()),
                    LocalDate.parse(values.get(1).trim()),
                    Long.parseLong(values.get(2).trim()),
                    Long.parseLong(values.get(3).trim()),
                    Integer.parseInt(values.get(4).trim()),
                    Integer.parseInt(values.get(5).trim()),
                    Integer.parseInt(values.get(6).trim())
            );
        } catch (RuntimeException exception) {
            throw lineError(lineNumber, "مقادیر سطر برای تبدیل فنی به نوع داده مقصد قابل خواندن نیستند: " + exception.getMessage());
        }
    }

    public static CalendarDateCsvRow parseDate(String line, long lineNumber) {
        List<String> values = requireColumnCount(line, DATE_HEADER.size(), "calendar_date.csv", lineNumber);
        try {
            return new CalendarDateCsvRow(
                    Long.parseLong(values.get(0).trim()),
                    Long.parseLong(values.get(1).trim()),
                    values.get(2).trim(),
                    Integer.parseInt(values.get(3).trim()),
                    Integer.parseInt(values.get(4).trim()),
                    Integer.parseInt(values.get(5).trim()),
                    Integer.parseInt(values.get(6).trim()),
                    values.get(7).trim(),
                    values.get(8).trim(),
                    values.get(9).trim()
            );
        } catch (RuntimeException exception) {
            throw lineError(lineNumber, "مقادیر سطر برای تبدیل فنی به نوع داده مقصد قابل خواندن نیستند: " + exception.getMessage());
        }
    }

    public static List<String> split(String line) {
        ArrayList<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (quoted) throw error("ساختار CSV معتبر نیست؛ علامت نقل‌قول بسته نشده است.");
        values.add(current.toString().trim());
        return values;
    }

    private static List<String> requireColumnCount(String line, int expected, String file, long lineNumber) {
        if (line == null || line.isBlank()) throw lineError(lineNumber, "سطر خالی در " + file + " مجاز نیست.");
        List<String> values = split(line);
        if (values.size() != expected) {
            throw lineError(lineNumber, "تعداد ستون‌ها در " + file + " باید " + expected + " باشد؛ مقدار فعلی " + values.size() + " است.");
        }
        return values;
    }

    private static long positiveLong(String value, String field, long lineNumber) {
        long parsed = longValue(value, field, lineNumber);
        if (parsed <= 0) throw lineError(lineNumber, field + " باید بزرگ‌تر از صفر باشد.");
        return parsed;
    }

    private static long longValue(String value, String field, long lineNumber) {
        try {
            return Long.parseLong(required(value, field, lineNumber));
        } catch (NumberFormatException exception) {
            throw lineError(lineNumber, field + " باید عدد صحیح باشد.");
        }
    }

    private static int intValue(String value, String field, long lineNumber) {
        try {
            return Integer.parseInt(required(value, field, lineNumber));
        } catch (NumberFormatException exception) {
            throw lineError(lineNumber, field + " باید عدد صحیح باشد.");
        }
    }

    private static int rangeInt(String value, String field, int min, int max, long lineNumber) {
        int parsed = intValue(value, field, lineNumber);
        if (parsed < min || parsed > max) throw lineError(lineNumber, field + " باید بین " + min + " و " + max + " باشد.");
        return parsed;
    }

    private static String required(String value, String field, long lineNumber) {
        if (value == null || value.isBlank()) throw lineError(lineNumber, field + " اجباری است.");
        return value.trim();
    }

    private static String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\ufeff' ? value.substring(1) : value;
    }

    static IllegalArgumentException lineError(long lineNumber, String detail) {
        return error("خطا در سطر " + lineNumber + ": " + detail);
    }

    static IllegalArgumentException error(String detail) {
        return new IllegalArgumentException(detail);
    }
}
