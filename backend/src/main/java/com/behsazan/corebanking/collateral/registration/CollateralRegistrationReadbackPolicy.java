package com.behsazan.corebanking.collateral.registration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackModels.Check;

/** Pure BUC-03/13 integrity conditions. Never turn missing evidence into PASS. */
public final class CollateralRegistrationReadbackPolicy {
    private CollateralRegistrationReadbackPolicy() {}
    public static boolean equalId(Object first, Object second) {
        if (first == null || second == null) return false;
        try {
            BigDecimal a=new BigDecimal(first.toString()),b=new BigDecimal(second.toString());
            return a.stripTrailingZeros().scale() <= 0 && b.stripTrailingZeros().scale() <= 0
                    && a.compareTo(b)==0;
        } catch (NumberFormatException ex) { return false; }
    }
    public static boolean equalMoney(Object first, Object second) {
        if (first == null || second == null) return false;
        try { return new BigDecimal(first.toString()).compareTo(new BigDecimal(second.toString())) == 0; }
        catch (NumberFormatException ex) { return false; }
    }
    public static boolean matchesCore(Map<String,Object> core, Map<String,Object> ledger) {
        return core != null && ledger != null
                && equalId(core.get("COLLATERAL_ID"), ledger.get("COLLATERAL_ID"))
                && equalId(core.get("COLLATERAL_NUMBER"), ledger.get("COLLATERAL_NUMBER"))
                && Objects.equals(core.get("BRANCH_CODE"), ledger.get("BRANCH_CODE"))
                && "REGISTERED".equals(ledger.get("REGISTRATION_STATUS"));
    }
    public static boolean matchesDraft(Map<String,Object> core, Map<String,Object> ledger,
            Map<String,Object> draft) {
        return matchesCore(core, ledger) && draft != null
                && equalId(draft.get("DRAFT_ID"),ledger.get("DRAFT_ID"))
                && Objects.equals(draft.get("REQUEST_HASH"), ledger.get("DRAFT_HASH"))
                && equalId(core.get("PARTY_ID"),draft.get("PARTY_ID"))
                && equalId(core.get("COLLATERAL_TYPE_ID"),draft.get("COLLATERAL_TYPE_ID"))
                && Objects.equals(core.get("CURRENCY_CODE"),draft.get("CURRENCY_CODE"))
                && equalMoney(core.get("ESTIMATE_AMOUNT"),draft.get("ESTIMATE_AMOUNT"))
                && Objects.equals(core.get("VALID_FROM"),draft.get("VALID_FROM"))
                && Objects.equals(core.get("VALID_TO"),draft.get("VALID_TO"))
                && Objects.equals(core.get("DEPOSIT_NUMBER"),draft.get("DEPOSIT_NUMBER"));
    }
    private static final Set<String> REQUIRED_EVIDENCE = Set.of(
            "CORE_BRANCH_ROW", "REGISTRATION_LEDGER", "LEDGER_TO_CORE_BINDING",
            "INDEPENDENT_REVIEW_DECISION", "BANK_NUMBER_ISSUANCE_ROW",
            "VERIFIED_ASSET_CURRENT_LINK", "IMMUTABLE_DRAFT_TO_CORE",
            "PERSISTED_ATTRIBUTE_VALUES", "PERSISTED_SERIAL_RANGES");
    public static String state(String mode, boolean corePresent, boolean ledgerSchema,
            boolean ledgerPresent, List<Check> checks) {
        if (!ledgerSchema) return "LEDGER_SCHEMA_NOT_READY";
        if (!ledgerPresent) return corePresent ? "HISTORICAL_CORE_WITHOUT_LEDGER" : "NOT_REGISTERED";
        if (!corePresent) return "CORE_READBACK_MISSING";
        boolean failed = checks.stream().anyMatch(c -> "MISMATCH".equals(c.state()));
        if (failed) return "READBACK_MISMATCH";
        // Neither an empty check list nor a partial report can claim technical lineage.
        boolean complete = checks.stream().map(Check::code).collect(java.util.stream.Collectors.toSet())
                .containsAll(REQUIRED_EVIDENCE)
                && checks.stream().allMatch(c -> "VERIFIED".equals(c.state())
                || "NOT_APPLICABLE".equals(c.state())
                || ("OBSERVED".equals(c.state())
                    && ("PERSISTED_ATTRIBUTE_VALUES".equals(c.code())
                     || "PERSISTED_SERIAL_RANGES".equals(c.code()))));
        return complete ? "TECHNICAL_LINEAGE_OBSERVED" : "READBACK_EVIDENCE_INCOMPLETE";
    }
    public static boolean technical(String status) {
        return "TECHNICAL_LINEAGE_OBSERVED".equals(status);
    }
}
