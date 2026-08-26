package com.behsazan.corebanking.calendar.reference.oracle;

import java.util.regex.Pattern;

final class CalendarSqlNames {
    private static final Pattern SAFE = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");

    private CalendarSqlNames() {}

    static String identifier(String value) {
        if (value == null || !SAFE.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        }
        return value;
    }

    static String qualified(String schema, String object) {
        return identifier(schema) + "." + identifier(object);
    }
}
