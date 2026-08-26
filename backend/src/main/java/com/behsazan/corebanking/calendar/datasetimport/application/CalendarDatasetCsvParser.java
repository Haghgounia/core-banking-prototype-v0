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
        long dayId = positiveLong(values.get(0), "DAY_ID", lineNumber);
        LocalDate canonicalDate;
        try {
            canonicalDate = LocalDate.parse(values.get(1));
        } catch (DateTimeParseException exception) {
            throw lineError(lineNumber, "CANONICAL_DATE باید با قالب YYYY-MM-DD باشد.");
        }
        long epochDay = longValue(values.get(2), "EPOCH_DAY", lineNumber);
        long jdn = longValue(values.get(3), "JULIAN_DAY_NUMBER", lineNumber);
        int weekdayId = rangeInt(values.get(4), "WEEKDAY_ID", 1, 7, lineNumber);
        int iso = rangeInt(values.get(5), "ISO_WEEKDAY_NO", 1, 7, lineNumber);
        int ir = rangeInt(values.get(6), "IR_WEEKDAY_NO", 1, 7, lineNumber);
        return new CalendarDayCsvRow(dayId, canonicalDate, epochDay, jdn, weekdayId, iso, ir);
    }

    public static CalendarDateCsvRow parseDate(String line, long lineNumber) {
        List<String> values = requireColumnCount(line, DATE_HEADER.size(), "calendar_date.csv", lineNumber);
        long id = positiveLong(values.get(0), "CALENDAR_DATE_ID", lineNumber);
        long dayId = positiveLong(values.get(1), "DAY_ID", lineNumber);
        String system = required(values.get(2), "CALENDAR_SYSTEM_CODE", lineNumber);
        if (!CALENDAR_SYSTEMS.contains(system)) {
            throw lineError(lineNumber, "CALENDAR_SYSTEM_CODE نامعتبر است: " + system);
        }
        int year = intValue(values.get(3), "YEAR_NO", lineNumber);
        int month = rangeInt(values.get(4), "MONTH_NO", 1, 12, lineNumber);
        int day = rangeInt(values.get(5), "DAY_NO", 1, 31, lineNumber);
        int dayOfYear = rangeInt(values.get(6), "DAY_OF_YEAR", 1, 366, lineNumber);
        String formattedDate = required(values.get(7), "FORMATTED_DATE", lineNumber);
        if (!formattedDate.matches("-?\\d{1,6}/\\d{2}/\\d{2}")) {
            throw lineError(lineNumber, "FORMATTED_DATE باید با قالب YYYY/MM/DD باشد.");
        }
        String leap = required(values.get(8), "IS_LEAP_YEAR", lineNumber);
        if (!"Y".equals(leap) && !"N".equals(leap)) {
            throw lineError(lineNumber, "IS_LEAP_YEAR فقط می‌تواند Y یا N باشد.");
        }
        String algorithm = required(values.get(9), "ALGORITHM_CODE", lineNumber);
        return new CalendarDateCsvRow(id, dayId, system, year, month, day, dayOfYear, formattedDate, leap, algorithm);
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
