package com.behsazan.corebanking.calendar2.reference.oracle;

import java.util.regex.Pattern;

public final class Calendar2SqlNames {
    private static final Pattern SAFE = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private Calendar2SqlNames() {}

    public static String identifier(String value) {
        if (value == null || !SAFE.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe Oracle identifier: " + value);
        }
        return value;
    }

    public static String qualified(String schema, String object) {
        return identifier(schema) + "." + identifier(object);
    }
}
