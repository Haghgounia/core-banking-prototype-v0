package com.behsazan.corebanking.system.modelcomparison;

import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.SchemaOption;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves selectable Oracle schemas from the live Oracle data dictionary.
 *
 * <p>The schema list is intentionally metadata-driven: ALL_TABLES is used as the
 * source of truth, so only owners that are actually visible to the current Oracle
 * connection, contain at least one visible table, and are not Oracle-maintained
 * system schemas are offered to the UI. Project
 * configuration is used only to enrich known schemas with friendly labels and to
 * select a preferred default; it no longer determines the selectable schema list.</p>
 */
@Component
class ConfiguredDatabaseSchemas {
    static final String VISIBLE_SCHEMAS_SQL = """
            SELECT T.OWNER, COUNT(*) AS TABLE_COUNT
              FROM ALL_TABLES T
              JOIN ALL_USERS U ON U.USERNAME = T.OWNER
             WHERE COALESCE(U.ORACLE_MAINTAINED, 'N') = 'N'
             GROUP BY T.OWNER
             ORDER BY T.OWNER
            """;

    private final JdbcClient jdbc;
    private final Map<String, String> friendlyLabels;
    private final String preferredDefaultSchema;

    ConfiguredDatabaseSchemas(JdbcClient jdbc, Environment environment) {
        this.jdbc = jdbc;
        Map<String, String> labels = new LinkedHashMap<>();
        addLabel(labels, environment.getProperty("core-banking.schemas.cif"), "Party / Customer (CIF)");
        addLabel(labels, environment.getProperty("core-banking.schemas.reference-data"), "اطلاعات پایه عمومی (GEO)");
        addLabel(labels, environment.getProperty("core-banking.schemas.deposit-product-factory"), "محصول‌ساز سپرده (DPS)");
        addLabel(labels, environment.getProperty("core-banking.schemas.fee"), "مدیریت کارمزد (FEE)");
        addLabel(labels, environment.getProperty("core-banking.schemas.calendar"), "تقویم یک (CAL)");
        addLabel(labels, environment.getProperty("core-banking.schemas.calendar2"), "تقویم دو (CAL2)");
        addLabel(labels, environment.getProperty("core-banking.schemas.party-reference"), "اطلاعات پایه Party");
        this.friendlyLabels = Map.copyOf(labels);
        this.preferredDefaultSchema = normalize(environment.getProperty("core-banking.schemas.cif"));
    }

    List<SchemaOption> options() {
        return visibleSchemas().stream()
                .map(schema -> new SchemaOption(schema.name(), label(schema)))
                .toList();
    }

    String defaultSchema() {
        List<VisibleSchema> visible = visibleSchemas();
        if (visible.isEmpty()) {
            throw new IllegalStateException("هیچ Schema دارای جدول قابل مشاهده‌ای از Oracle Metadata دریافت نشد.");
        }
        if (preferredDefaultSchema != null && visible.stream().anyMatch(schema -> schema.name().equals(preferredDefaultSchema))) {
            return preferredDefaultSchema;
        }
        String currentUser = currentUser();
        if (currentUser != null && visible.stream().anyMatch(schema -> schema.name().equals(currentUser))) {
            return currentUser;
        }
        return visible.getFirst().name();
    }

    String require(String requested) {
        String normalized = normalize(requested);
        if (normalized == null) {
            return defaultSchema();
        }
        boolean visible = visibleSchemas().stream().anyMatch(schema -> schema.name().equals(normalized));
        if (!visible) {
            throw new ModelComparisonValidationException(
                    "Schema انتخاب‌شده در Oracle Metadata قابل مشاهده نیست یا جدول قابل مشاهده‌ای ندارد: " + normalized
            );
        }
        return normalized;
    }

    private List<VisibleSchema> visibleSchemas() {
        return jdbc.sql(VISIBLE_SCHEMAS_SQL)
                .query((rs, rowNum) -> new VisibleSchema(
                        normalize(rs.getString("OWNER")),
                        rs.getLong("TABLE_COUNT")
                ))
                .list()
                .stream()
                .filter(schema -> schema.name() != null)
                .toList();
    }

    private String currentUser() {
        try {
            return normalize(jdbc.sql("SELECT USER FROM DUAL").query(String.class).single());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String label(VisibleSchema schema) {
        String friendly = friendlyLabels.get(schema.name());
        String tableCount = schema.tableCount() + " جدول";
        return friendly == null
                ? schema.name() + " — " + tableCount
                : friendly + " — " + schema.name() + " — " + tableCount;
    }

    private static void addLabel(Map<String, String> labels, String value, String label) {
        String normalized = normalize(value);
        if (normalized != null) labels.putIfAbsent(normalized, label);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private record VisibleSchema(String name, long tableCount) {
    }
}
