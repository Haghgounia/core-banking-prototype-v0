package com.behsazan.corebanking.databaseexport;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record DatabaseExportRequest(
        String schemaName,
        String tablePrefix,
        Path outputRoot
) {
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_$#]{0,127}");
    private static final Pattern SAFE_PREFIX = Pattern.compile("[A-Z0-9_$#_]{0,128}");

    public DatabaseExportRequest {
        schemaName = normalizeSchema(schemaName);
        tablePrefix = normalizePrefix(tablePrefix);
        outputRoot = Objects.requireNonNull(outputRoot, "outputRoot").toAbsolutePath().normalize();
    }

    private static String normalizeSchema(String value) {
        String normalized = Objects.requireNonNull(value, "schemaName").trim().toUpperCase(Locale.ROOT);
        if (!SAFE_IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid Oracle schema name: " + value);
        }
        return normalized;
    }

    private static String normalizePrefix(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!SAFE_PREFIX.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid Oracle table prefix: " + value);
        }
        return normalized;
    }
}
