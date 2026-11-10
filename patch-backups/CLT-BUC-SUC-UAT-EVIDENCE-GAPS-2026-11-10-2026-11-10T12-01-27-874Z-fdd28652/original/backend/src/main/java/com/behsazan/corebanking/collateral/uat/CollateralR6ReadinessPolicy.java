package com.behsazan.corebanking.collateral.uat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * R6: Oracle dictionary evidence only. Table presence DOES NOT prove a live business flow,
 * bank policy, a usable record, permission to post or analyst acceptance.
 * The canonical 16 BUCs stay separate from the 10 official UI UAT cases.
 */
public final class CollateralR6ReadinessPolicy {
    private CollateralR6ReadinessPolicy() {}

    private static final Map<String, List<String>> TABLES = catalog();

    private static Map<String,List<String>> catalog() {
        LinkedHashMap<String,List<String>> items = new LinkedHashMap<>();
        items.put("BUC-01", List.of("COLLATERAL_GROUP","COLLATERAL_TYPE","COLLATERAL_TYPE_ATTRIBUTE","INSURANCE_COMPANY"));
        items.put("BUC-02", List.of("COLLATERAL"));
        items.put("BUC-03", List.of("COLLATERAL","CLT_INTRODUCTION_DRAFT","CLT_INTRODUCTION_REVIEW","CLT_REGISTRATION_LEDGER"));
        items.put("BUC-04", List.of("CLT_INTRODUCTION_DRAFT_SERIAL_RANGE","CLT_COLLATERAL_SERIAL_RANGE"));
        items.put("BUC-05", List.of("CLT_COLLATERAL_AMENDMENT","CLT_COLLATERAL_AMENDMENT_AUDIT"));
        items.put("BUC-06", List.of("COLLATERAL_SPONSOR","COLLATERAL_SPONSOR_REQUEST"));
        items.put("BUC-07", List.of("COLLATERAL_INSURANCE","COLLATERAL_INSURANCE_REQUEST"));
        items.put("BUC-08", List.of("COLLATERAL_VALUATION_HISTORY","COLLATERAL_VALUATION_REQUEST"));
        items.put("BUC-09", List.of("COLLATERAL_ALLOCATION","CLT_ALLOCATION_REQUEST","CLT_COVERAGE_POST_PERMIT","COLLATERAL_TRANSACTION"));
        items.put("BUC-10", List.of("COLLATERAL_ALLOCATION","CLT_UNALLOC_REQ","CLT_COVERAGE_POST_PERMIT","COLLATERAL_TRANSACTION"));
        items.put("BUC-11", List.of("COLLATERAL_STATUS_HISTORY","CLT_FINAL_RELEASE_PERMIT"));
        items.put("BUC-12", List.of("CLT_ASSET_IDENTITY","CLT_ASSET_COLLATERAL_LINK","CLT_REINTRODUCTION_PERMIT"));
        items.put("BUC-13", List.of("COLLATERAL","COLLATERAL_TYPE"));
        items.put("BUC-14", List.of("COLLATERAL","COLLATERAL_ALLOCATION","COLLATERAL_VALUATION_HISTORY"));
        items.put("BUC-15", List.of("COLLATERAL_TYPE","CLT_CROSS_UNIT_GRANT","CLT_CROSS_UNIT_INTENT"));
        items.put("BUC-16", List.of("COLLATERAL_TRANSACTION","COLLATERAL_TXN_STATUS_HISTORY","COLLATERAL_STATUS_HISTORY"));
        return java.util.Collections.unmodifiableMap(items);
    }

    public record CaseEvidence(String buc, String schemaState,
            List<String> requiredTables, List<String> missingTables) {}

    public static List<CaseEvidence> evaluate(Set<String> accessibleCltTables, boolean dictionaryAvailable) {
        Set<String> tables = accessibleCltTables == null ? Set.of() :
                new LinkedHashSet<>(accessibleCltTables);
        List<CaseEvidence> rows = new ArrayList<>(16);
        for (Map.Entry<String,List<String>> entry : TABLES.entrySet()) {
            List<String> missing = dictionaryAvailable
                    ? entry.getValue().stream().filter(t -> !tables.contains(t)).toList()
                    : List.of();
            String state = !dictionaryAvailable ? "UNKNOWN" : missing.isEmpty() ? "PRESENT" : "MISSING";
            rows.add(new CaseEvidence(entry.getKey(), state, entry.getValue(), missing));
        }
        return List.copyOf(rows);
    }

    public static Set<String> allRequiredTables() {
        Set<String> result = new LinkedHashSet<>();
        TABLES.values().forEach(result::addAll);
        return java.util.Collections.unmodifiableSet(result);
    }
}
