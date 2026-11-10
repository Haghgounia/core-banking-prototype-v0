package com.behsazan.corebanking.collateral.reconciliation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Non-writing fixed catalog and fail-closed status semantics for BUC-01/03/04/13. */
public final class CollateralOracleReconciliationPolicy {
    private CollateralOracleReconciliationPolicy() {}

    // The same 17 tables as the governed R1 catalog; deliberately not supplied by HTTP callers.
    public static final List<String> REFERENCE_TABLES = List.of(
            "COLLATERAL_TYPE_ATTRIBUTE", "REF_COLLATERAL_ATTRIBUTE_DATA_TYPE", "REF_COLL_TXN_STATUS",
            "REF_CURRENCY", "REF_VALUATION_TYPE", "REF_COLL_REVERSAL_REASON",
            "COLLATERAL_ATTRIBUTE_DEFINITION", "COLLATERAL_TYPE", "COLLATERAL_GROUP",
            "REF_SOURCE_SYSTEM", "REF_COLLATERAL_STATUS", "COLLATERAL_INSURANCE_TYPE",
            "REF_CHANNEL", "INSURANCE_COMPANY", "REF_VALUATION_METHOD",
            "REF_VALUATION_STATUS", "REF_COLL_TXN_TYPE");

    public record ReferenceRow(String table, String visibility, String dataState,
            Long totalRows, Long activeRows) {}
    public record CheckRow(String id, String state, String observation) {}

    public static void validateCatalog() {
        if (REFERENCE_TABLES.size() != 17 || new LinkedHashSet<>(REFERENCE_TABLES).size() != 17)
            throw new IllegalStateException("CLT_RECONCILIATION_REFERENCE_CATALOG_INVALID");
        for (String table : REFERENCE_TABLES) {
            if (!table.matches("[A-Z][A-Z0-9_]{1,60}"))
                throw new IllegalStateException("CLT_RECONCILIATION_TABLE_IDENTIFIER_INVALID");
        }
    }

    /** ALL_TABLES is privilege-filtered: invisible is not asserted to be physically missing. */
    public static String visibility(boolean dictionaryAvailable, Set<String> visible, String table) {
        if (!dictionaryAvailable || visible == null) return "UNKNOWN";
        return visible.contains(table) ? "VISIBLE" : "NOT_VISIBLE";
    }

    public static ReferenceRow unknownReference(String table, String visibility) {
        return new ReferenceRow(table, visibility, "UNKNOWN", null, null);
    }

    public static ReferenceRow observedReference(String table, long total, long active) {
        if (total < 0 || active < 0 || active > total)
            throw new IllegalArgumentException("CLT_RECONCILIATION_INVALID_COUNTS");
        return new ReferenceRow(table, "VISIBLE", total == 0 ? "OBSERVED_EMPTY" : "OBSERVED_ROWS",
                total, active);
    }

    /** No policy or bank acceptance is inferred, even when the technical references match. */
    public static CheckRow transactionDefault(Long pending, Long pendingApproval, String defaultCode) {
        if (pending == null || pendingApproval == null || defaultCode == null)
            return new CheckRow("G16_STATUS_DEFAULT", "UNKNOWN", "DEFAULT_OR_REFERENCE_NOT_OBSERVED");
        if (!Set.of("PENDING", "PENDING_APPROVAL").contains(defaultCode))
            return new CheckRow("G16_STATUS_DEFAULT", "UNKNOWN", "DEFAULT_NOT_IN_KNOWN_REFERENCE_SET");
        boolean matched = "PENDING".equals(defaultCode) ? pending > 0 : pendingApproval > 0;
        return new CheckRow("G16_STATUS_DEFAULT", matched ? "OBSERVED" : "BLOCKED",
                matched ? "DEFAULT_HAS_ACTIVE_REFERENCE" : "DEFAULT_REFERENCE_MISMATCH");
    }

    public static String canonicalDefault(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim().toUpperCase(Locale.ROOT).replace("'", "").replace("\"", "");
        if ("PENDING".equals(trimmed) || "PENDING_APPROVAL".equals(trimmed)) return trimmed;
        return "OTHER";
    }

    public static CheckRow binary(String id, Boolean observed) {
        return new CheckRow(id, observed == null ? "UNKNOWN" : observed ? "OBSERVED" : "BLOCKED",
                observed == null ? "NOT_VERIFIED" : observed ? "TECHNICAL_CONDITION_OBSERVED" : "TECHNICAL_CONDITION_NOT_OBSERVED");
    }
}
