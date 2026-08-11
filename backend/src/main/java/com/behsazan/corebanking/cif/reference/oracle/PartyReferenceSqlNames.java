package com.behsazan.corebanking.cif.reference.oracle;

import java.util.regex.Pattern;

final class PartyReferenceSqlNames {
    private static final Pattern SAFE = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");

    private PartyReferenceSqlNames() {
    }

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
