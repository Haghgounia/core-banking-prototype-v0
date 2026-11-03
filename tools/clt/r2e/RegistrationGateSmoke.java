package com.behsazan.corebanking.collateral.registration;

import com.behsazan.corebanking.collateral.registration.CollateralRegistrationGatePolicy.Evidence;
import java.util.List;
import java.util.Set;

/** No frameworks or database required: executable deny-by-default decision regression. */
public final class RegistrationGateSmoke {
    private static int passed;
    private static void ok(boolean condition, String title) {
        if (!condition) throw new AssertionError(title);
        passed++;
    }
    private static Evidence evidence(String status, String maker, String checker, boolean immutable,
            boolean current, boolean countable, boolean rows, boolean oracle, String candidate) {
        return new Evidence("12","33",status,maker,checker,"071","a".repeat(64),
                true,immutable,immutable,current,List.of("CLT_EVIDENCE_UNAVAILABLE"),oracle,
                true,true,countable,rows,countable ? "13" : "0",candidate,"1000000","IRR");
    }
    private static boolean hasStatus(CollateralRegistrationReadinessModels.Report report, String code, String status) {
        return report.checks().stream().anyMatch(c -> code.equals(c.code()) && status.equals(c.status()));
    }
    public static void main(String[] args) {
        var good=CollateralRegistrationGatePolicy.assess(evidence("REVIEWED","maker","checker",
                true,true,true,true,true,"CAR-001"));
        ok(!good.finalRegistrationEnabled(), "hard off");
        ok(!good.eligibleForFinalRegistration(), "not registrable");
        ok(good.registrationNumber()==null, "no issued number");
        ok("NOT_REGISTERED".equals(good.registrationStatus()), "draft not collateral");
        ok(hasStatus(good,"REVIEW_DECISION","VERIFIED"), "independent reviewer");
        ok(hasStatus(good,"SNAPSHOT_INTEGRITY","VERIFIED"), "exact digest");
        ok(hasStatus(good,"LIVE_CIF_CLT_RECHECK","VERIFIED"), "current evidence");
        ok(hasStatus(good,"NUMBER19_STORAGE","VERIFIED"), "Oracle column compatible");
        ok(hasStatus(good,"TRANSACTION_REFERENCES","VERIFIED"), "reference code evidence");
        ok(hasStatus(good,"COUNTABLE_ITEM_VALIDATION","VERIFIED"), "serial ranges valid");
        ok(hasStatus(good,"ASSET_CANDIDATE","INFORMATIONAL"), "candidate, not certified");
        ok(good.blockingCodes().containsAll(Set.of("G-01","G-03","G-12","G-14","G-16","G-17","FINAL_WRITER")),"bank gaps");
        ok("13".equals(good.countableItemCount()), "count as string");
        var returned=CollateralRegistrationGatePolicy.assess(evidence("RETURNED","maker","checker",
                true,true,false,false,true,null));
        ok(hasStatus(returned,"REVIEW_DECISION","BLOCKED"), "returned is blocked");
        var pending=CollateralRegistrationGatePolicy.assess(evidence("PENDING_REVIEW","maker","checker",
                true,true,false,false,true,null));
        ok(hasStatus(pending,"REVIEW_DECISION","BLOCKED"), "pending is blocked");
        var same=CollateralRegistrationGatePolicy.assess(evidence("REVIEWED","maker","maker",
                true,true,false,false,true,null));
        ok(hasStatus(same,"REVIEW_DECISION","BLOCKED"), "self review rejected");
        var damaged=CollateralRegistrationGatePolicy.assess(evidence("REVIEWED","maker","checker",
                false,true,false,false,true,null));
        ok(hasStatus(damaged,"SNAPSHOT_INTEGRITY","BLOCKED"), "digest mismatch");
        ok(hasStatus(damaged,"LIVE_CIF_CLT_RECHECK","BLOCKED"), "invalid hash denies live proof");
        var outage=CollateralRegistrationGatePolicy.assess(evidence("REVIEWED","maker","checker",
                true,false,true,true,true,"ASSET"));
        ok(hasStatus(outage,"LIVE_CIF_CLT_RECHECK","BLOCKED"), "CIF unavailable denies");
        var brokenDb=CollateralRegistrationGatePolicy.assess(evidence("REVIEWED","maker","checker",
                true,true,true,true,false,"ASSET"));
        ok(hasStatus(brokenDb,"NUMBER19_STORAGE","BLOCKED"), "unexpected Oracle shape");
        var nonCountable=CollateralRegistrationGatePolicy.assess(evidence("REVIEWED","maker","checker",
                true,true,false,false,true,"LAND"));
        ok(hasStatus(nonCountable,"COUNTABLE_ITEM_VALIDATION","NOT_APPLICABLE"),"non-countable");
        ok(!nonCountable.blockingCodes().contains("G-03"),"non-countable no G03 blocker");
        ok(!nonCountable.blockingCodes().contains("G-12"),"non-countable no G12 blocker");
        var overlap=CollateralRegistrationGatePolicy.assess(evidence("REVIEWED","maker","checker",
                true,true,true,false,true,"ASSET"));
        ok(hasStatus(overlap,"COUNTABLE_ITEM_VALIDATION","BLOCKED"),"bad serial blocked");
        var missingAsset=CollateralRegistrationGatePolicy.assess(evidence("REVIEWED","maker","checker",
                true,true,false,false,true,null));
        ok(hasStatus(missingAsset,"ASSET_CANDIDATE","BLOCKED"),"missing candidate");
        ok(hasStatus(good,"G-14","DECISION_REQUIRED"),"candidate cannot approve identity");
        ok(hasStatus(good,"G-17","DECISION_REQUIRED"),"format cannot approve itself");
        ok(hasStatus(good,"G-16","DECISION_REQUIRED"),"default mismatch cannot disappear");
        ok(hasStatus(good,"FINAL_WRITER","BLOCKED"),"no main DML");
        ok(good.checks().stream().allMatch(c->c.labelFa()!=null&&!c.labelFa().isBlank()),"Persian check labels");
        try { CollateralRegistrationGatePolicy.assess(null); throw new AssertionError("null"); }
        catch (IllegalArgumentException expected) { passed++; }
        System.out.println("CLT_R2E_REGISTRATION_DECISION_SMOKE_PASS="+passed);
    }
}
