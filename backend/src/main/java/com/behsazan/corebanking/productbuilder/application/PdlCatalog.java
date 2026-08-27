package com.behsazan.corebanking.productbuilder.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PdlCatalog {
    private PdlCatalog() {}

    public record Entry(String tableName, String title, String packageCode, String packageTitle) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        registerGeneratedEntries();
    }

    private static void register(String tableName, String title, String packageCode, String packageTitle) {
        ENTRIES.put(tableName, new Entry(tableName, title, packageCode, packageTitle));
    }

    private static void registerGeneratedEntries() {
        register("PRODUCT", "\u0645\u062d\u0635\u0648\u0644 \u0628\u0627\u0646\u06a9\u06cc", "01", "Core");
        register("PRODUCT_VERSION", "\u0646\u0633\u062e\u0647 \u0645\u062d\u0635\u0648\u0644", "01", "Core");
        register("PRODUCT_VERSION_MODULE", "\u0645\u0627\u0698\u0648\u0644\u200c\u0647\u0627\u06cc \u0646\u0633\u062e\u0647 \u0645\u062d\u0635\u0648\u0644", "01", "Core");
        register("PRODUCT_LEGACY_MAPPING", "\u0646\u06af\u0627\u0634\u062a \u0645\u06cc\u0631\u0627\u062b\u06cc \u0645\u062d\u0635\u0648\u0644", "01", "Core");
        register("PRODUCT_RELATIONSHIP", "\u0631\u0627\u0628\u0637\u0647 \u0628\u06cc\u0646 \u0645\u062d\u0635\u0648\u0644\u0627\u062a", "01", "Core");
        register("PRODUCT_ORG_SCOPE", "\u0645\u062d\u062f\u0648\u062f\u0647 \u0633\u0627\u0632\u0645\u0627\u0646\u06cc \u0645\u062d\u0635\u0648\u0644", "02", "Common Rules");
        register("PRODUCT_CHANNEL_OPERATION", "\u0639\u0645\u0644\u06cc\u0627\u062a \u0645\u062c\u0627\u0632 \u06a9\u0627\u0646\u0627\u0644", "02", "Common Rules");
        register("PRODUCT_CHANNEL_RULE", "\u0642\u0627\u0639\u062f\u0647 \u0645\u0634\u062a\u0631\u06a9 \u06a9\u0627\u0646\u0627\u0644", "02", "Common Rules");
        register("PRODUCT_RATE_TIER", "\u067e\u0644\u0647\u200c\u0647\u0627\u06cc \u0646\u0631\u062e", "02", "Common Rules");
        register("PRODUCT_ELIGIBILITY_RULE", "\u0642\u0627\u0639\u062f\u0647 \u0645\u0634\u062a\u0631\u06a9 \u0627\u0647\u0644\u06cc\u062a", "02", "Common Rules");
        register("PRODUCT_REQUIRED_INQUIRY", "\u0627\u0633\u062a\u0639\u0644\u0627\u0645\u200c\u0647\u0627\u06cc \u0627\u0644\u0632\u0627\u0645\u06cc \u0645\u062d\u0635\u0648\u0644", "02", "Common Rules");
        register("PRODUCT_PRICING_COMPONENT", "\u0627\u062c\u0632\u0627\u06cc \u0642\u06cc\u0645\u062a\u200c\u06af\u0630\u0627\u0631\u06cc", "02", "Common Rules");
        register("PRODUCT_PRICING_RULE", "\u0642\u0627\u0639\u062f\u0647 \u0642\u06cc\u0645\u062a\u200c\u06af\u0630\u0627\u0631\u06cc \u0645\u0634\u062a\u0631\u06a9", "02", "Common Rules");
        register("PRODUCT_REQUIRED_DOCUMENT", "\u0645\u062f\u0627\u0631\u06a9 \u0627\u0644\u0632\u0627\u0645\u06cc \u0645\u062d\u0635\u0648\u0644", "02", "Common Rules");
        register("DEPOSIT_PRODUCT_JOINT_RULE", "\u0642\u0648\u0627\u0639\u062f \u062d\u0633\u0627\u0628 \u0645\u0634\u062a\u0631\u06a9 \u0645\u062d\u0635\u0648\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_HOLD_RULE", "\u0642\u0648\u0627\u0639\u062f \u0645\u0633\u062f\u0648\u062f\u06cc \u0645\u062d\u0635\u0648\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_ALLOWED_TERM", "\u0645\u062f\u062a\u200c\u0647\u0627\u06cc \u0645\u062c\u0627\u0632 \u0645\u062d\u0635\u0648\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE", "\u0642\u0648\u0627\u0639\u062f \u062a\u0633\u0648\u06cc\u0647 \u062e\u0627\u062a\u0645\u0647 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_PROFILE", "\u067e\u0631\u0648\u0641\u0627\u06cc\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA", "\u0627\u0628\u0632\u0627\u0631\u0647\u0627\u06cc \u0628\u0631\u062f\u0627\u0634\u062a \u0645\u062d\u0635\u0648\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE", "\u0642\u0648\u0627\u0639\u062f \u062a\u0623\u06cc\u06cc\u062f \u062e\u0627\u062a\u0645\u0647 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_CLOSURE_PRECHECK", "\u06a9\u0646\u062a\u0631\u0644\u200c\u0647\u0627\u06cc \u067e\u06cc\u0634 \u0627\u0632 \u062e\u0627\u062a\u0645\u0647 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_TRANSACTION_RULE", "\u0642\u0648\u0627\u0639\u062f \u062a\u0631\u0627\u06a9\u0646\u0634 \u0645\u062d\u0635\u0648\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_OPENING_RULE", "\u0642\u0648\u0627\u0639\u062f \u0627\u0641\u062a\u062a\u0627\u062d \u0645\u062d\u0635\u0648\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_CLOSURE_RULE", "\u0642\u0648\u0627\u0639\u062f \u062e\u0627\u062a\u0645\u0647 \u0645\u062d\u0635\u0648\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_TERM_RULE", "\u0642\u0648\u0627\u0639\u062f \u0645\u062f\u062a \u0648 \u0633\u0631\u0631\u0633\u06cc\u062f \u0645\u062d\u0635\u0648\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("DEPOSIT_PRODUCT_DORMANCY_RULE", "\u0642\u0648\u0627\u0639\u062f \u0631\u0627\u06a9\u062f\u06cc \u0645\u062d\u0635\u0648\u0644 \u0633\u067e\u0631\u062f\u0647", "03", "Deposit Module");
        register("LOAN_PRODUCT_PROCESS_RULE", "\u0642\u0648\u0627\u0639\u062f \u0641\u0631\u0627\u06cc\u0646\u062f\u06cc \u0645\u062d\u0635\u0648\u0644 \u062a\u0633\u0647\u06cc\u0644\u0627\u062a", "04", "Loan Module");
        register("LOAN_PRODUCT_PROFILE", "\u067e\u0631\u0648\u0641\u0627\u06cc\u0644 \u062a\u0633\u0647\u06cc\u0644\u0627\u062a", "04", "Loan Module");
        register("LOAN_FINANCIAL_EXTENSION", "\u0627\u0641\u0632\u0648\u0646\u0647 \u0645\u0627\u0644\u06cc \u062a\u0633\u0647\u06cc\u0644\u0627\u062a", "04", "Loan Module");
        register("LOAN_ELIGIBILITY_EXTENSION", "\u0627\u0641\u0632\u0648\u0646\u0647 \u0627\u0647\u0644\u06cc\u062a \u062a\u0633\u0647\u06cc\u0644\u0627\u062a", "04", "Loan Module");
        register("LOAN_PRODUCT_REPAYMENT_RULE", "\u0642\u0648\u0627\u0639\u062f \u0628\u0627\u0632\u067e\u0631\u062f\u0627\u062e\u062a \u0645\u062d\u0635\u0648\u0644 \u062a\u0633\u0647\u06cc\u0644\u0627\u062a", "04", "Loan Module");
        register("LOAN_PRODUCT_COLLATERAL_RULE", "\u0642\u0648\u0627\u0639\u062f \u0648\u062b\u06cc\u0642\u0647 \u0645\u062d\u0635\u0648\u0644 \u062a\u0633\u0647\u06cc\u0644\u0627\u062a", "04", "Loan Module");
        register("FACILITY_OPERATION_PARAMETER", "\u067e\u0627\u0631\u0627\u0645\u062a\u0631\u0647\u0627\u06cc \u0639\u0645\u0644\u06cc\u0627\u062a \u062a\u0633\u0647\u06cc\u0644\u0627\u062a", "05", "Reference Data");
        register("PLAN_TYPE", "\u0627\u0646\u0648\u0627\u0639 \u0637\u0631\u062d \u0647\u0627\u06cc \u062a\u0633\u0647\u06cc\u0644\u0627\u062a", "05", "Reference Data");
        register("LOAN_TYPE_PLAN_TYPE", "\u0631\u0627\u0628\u0637 \u0646\u0648\u0639 \u0639\u0642\u062f \u0648 \u0646\u0648\u0639 \u0637\u0631\u062d", "05", "Reference Data");
        register("LOAN_TYPE", "\u062c\u062f\u0648\u0644 \u0627\u0646\u0648\u0627\u0639 \u0639\u0642\u062f", "05", "Reference Data");
        register("PATTERN_OPERATION_LOAN_TYPE", "\u0627\u0644\u06af\u0648\u06cc \u0639\u0645\u0644\u06cc\u0627\u062a \u0639\u0642\u062f", "05", "Reference Data");
        register("PATTERN_OPERATION_DETAIL", "\u062c\u0632\u0626\u06cc\u0627\u062a \u0639\u0645\u0644\u06cc\u0627\u062a \u062a\u0633\u0647\u06cc\u0644\u0627\u062a", "05", "Reference Data");
        register("ECONOMIC_SECTION", "\u062c\u062f\u0648\u0644 \u0628\u062e\u0634 \u0627\u0642\u062a\u0635\u0627\u062f\u06cc", "05", "Reference Data");
        register("LOAN_USAGE", "\u0646\u062d\u0648\u0647 \u0627\u0633\u062a\u0641\u0627\u062f\u0647", "05", "Reference Data");
        register("CODE_SET", "\u0645\u062c\u0645\u0648\u0639\u0647 \u06a9\u062f \u0645\u0631\u062c\u0639", "05", "Reference Data");
        register("ECONOMIC_SUB_SECTION", "\u062c\u062f\u0648\u0644 \u0632\u06cc\u0631 \u0628\u062e\u0634 \u0627\u0642\u062a\u0635\u0627\u062f\u06cc", "05", "Reference Data");
        register("LOAN_PRODUCT_COLLATERAL", "\u0627\u0642\u0644\u0627\u0645 \u0627\u0644\u06af\u0648\u06cc \u0648\u062b\u0627\u06cc\u0642 \u0645\u062d\u0635\u0648\u0644", "05", "Reference Data");
        register("DOCUMENT_TYPE", "\u0646\u0648\u0639 \u0645\u062f\u0631\u06a9", "05", "Reference Data");
        register("PATTERN_OPERATION", "\u0627\u0644\u06af\u0648\u06cc \u0639\u0645\u0644\u06cc\u0627\u062a", "05", "Reference Data");
        register("CODE_VALUE_TRANSITION", "\u0642\u0648\u0627\u0639\u062f \u06af\u0630\u0627\u0631 \u0645\u0642\u0627\u062f\u06cc\u0631 \u0648\u0636\u0639\u06cc\u062a", "05", "Reference Data");
        register("OPERATION", "\u0627\u0646\u0648\u0627\u0639 \u0639\u0645\u0644\u06cc\u0627\u062a", "05", "Reference Data");
        register("CODE_VALUE", "\u0645\u0642\u0627\u062f\u06cc\u0631 \u06a9\u062f \u0645\u0631\u062c\u0639", "05", "Reference Data");
        register("SUB_OPERATION", "\u0631\u06cc\u0632 \u0639\u0645\u0644\u06cc\u0627\u062a", "05", "Reference Data");
    }

    public static Entry require(String tableName) {
        Entry entry = ENTRIES.get(normalize(tableName));
        if (entry == null) throw new ProductBuilderValidationException("Unsupported PDL table: " + tableName);
        return entry;
    }

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES.values());
    }

    public static boolean contains(String tableName) {
        return ENTRIES.containsKey(normalize(tableName));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
