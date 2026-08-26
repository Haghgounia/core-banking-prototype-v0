package com.behsazan.corebanking.system.modelcomparison;

import com.behsazan.corebanking.system.modelcomparison.EaOracleComparisonModels.SchemaOption;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
class ConfiguredDatabaseSchemas {
    private final List<SchemaOption> options;
    private final String defaultSchema;

    ConfiguredDatabaseSchemas(Environment environment) {
        Map<String, String> configured = new LinkedHashMap<>();
        add(configured, environment.getProperty("core-banking.schemas.cif"), "Party / Customer (CIF)");
        add(configured, environment.getProperty("core-banking.schemas.reference-data"), "اطلاعات پایه عمومی (GEO)");
        add(configured, environment.getProperty("core-banking.schemas.deposit-product-factory"), "محصول‌ساز سپرده (DPS)");
        add(configured, environment.getProperty("core-banking.schemas.fee"), "مدیریت کارمزد (FEE)");
        add(configured, environment.getProperty("core-banking.schemas.calendar"), "تقویم یک (CAL)");
        add(configured, environment.getProperty("core-banking.schemas.calendar2"), "تقویم دو (CAL2)");
        add(configured, environment.getProperty("core-banking.schemas.party-reference"), "اطلاعات پایه Party");
        if (configured.isEmpty()) {
            throw new IllegalStateException("No configured Oracle schemas found under core-banking.schemas");
        }
        this.options = configured.entrySet().stream().map(entry -> new SchemaOption(entry.getKey(), entry.getValue())).toList();
        String cif = normalize(environment.getProperty("core-banking.schemas.cif"));
        this.defaultSchema = cif != null && configured.containsKey(cif) ? cif : options.getFirst().code();
    }

    List<SchemaOption> options() {
        return options;
    }

    String defaultSchema() {
        return defaultSchema;
    }

    String require(String requested) {
        String normalized = normalize(requested);
        if (normalized == null) normalized = defaultSchema;
        final String target = normalized;
        return options.stream().map(SchemaOption::code).filter(target::equals).findFirst()
                .orElseThrow(() -> new ModelComparisonValidationException(
                        "Schema انتخاب‌شده در تنظیمات برنامه تعریف نشده است: " + target
                ));
    }

    private static void add(Map<String, String> configured, String value, String label) {
        String normalized = normalize(value);
        if (normalized != null) configured.putIfAbsent(normalized, label + " — " + normalized);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
