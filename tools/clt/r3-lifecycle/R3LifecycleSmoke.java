import com.behsazan.corebanking.collateral.lifecycle.CollateralLifecycleEvidencePolicy;
import java.time.LocalDate;

/** Standalone Java 21 policy smoke, never touches Oracle or Spring. */
public class R3LifecycleSmoke {
    private static int passes;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passes++;
    }
    private static CollateralLifecycleEvidencePolicy.Facts facts(String pointer, String row,
            String status, String currency, LocalDate from, LocalDate to, long sponsors, long insurance) {
        return new CollateralLifecycleEvidencePolicy.Facts("712", "1405123000001", "ACTIVE", "ACTIVE", "IRR",
                true, true, sponsors, insurance, 10, pointer, row, status, currency,
                from, to, LocalDate.of(2026,11,9));
    }
    public static void main(String[] args) {
        var now = LocalDate.of(2026,11,9);
        var good = CollateralLifecycleEvidencePolicy.evaluate(facts("8", "8", "FINAL", "IRR",
                now.minusDays(1), now.plusDays(1), 10, 12));
        check(good.sponsor().recordedRows()==10, "sponsor recorded");
        check(good.insurance().recordedRows()==12, "insurance recorded");
        check(good.valuation().recordedRows()==10, "valuation recorded");
        check("NOT_VERIFIED".equals(good.sponsor().validity()), "sponsor not approved");
        check("NOT_VERIFIED".equals(good.insurance().validity()), "insurance not approved");
        check(good.valuation().withinRecordedEffectivePeriod(), "recorded period only");
        check(!good.valuation().eligibleForNewAllocation(), "not allocation approval");
        check(!good.eligibleForNewAllocation(), "composite not allocation approval");
        check(!good.finalRegistrationEnabled(), "final registration off");
        check(good.unresolvedApprovalGates().contains("G-18"), "revaluation gap open");
        var absent=CollateralLifecycleEvidencePolicy.evaluate(facts(null,null,null,null,null,null,0,0));
        check("NOT_PROVEN".equals(absent.valuation().recordedPeriodState()), "no pointer");
        check(!absent.valuation().withinRecordedEffectivePeriod(), "no pointer not period");
        var stale=CollateralLifecycleEvidencePolicy.evaluate(facts("8","9","FINAL","IRR",
                now.minusDays(1),now.plusDays(1),1,1));
        check(stale.valuation().evidenceBlockers().contains("CLT_R3_CURRENT_VALUATION_POINTER_UNRESOLVED"), "stale pointer");
        var badStatus=CollateralLifecycleEvidencePolicy.evaluate(facts("8","8","APPROVED","IRR",
                now.minusDays(1),now.plusDays(1),1,1));
        check(badStatus.valuation().evidenceBlockers().contains("CLT_R3_CURRENT_VALUATION_NOT_FINAL"), "physical FINAL");
        var expired=CollateralLifecycleEvidencePolicy.evaluate(facts("8","8","FINAL","IRR",
                now.minusDays(10),now.minusDays(1),1,1));
        check("EXPIRED_RECORDED_PERIOD".equals(expired.valuation().recordedPeriodState()), "expired");
        var unknown=CollateralLifecycleEvidencePolicy.evaluate(facts("8","8","FINAL","IRR",
                now.minusDays(10),null,1,1));
        check(unknown.valuation().evidenceBlockers().contains("CLT_R3_VALUATION_EFFECTIVE_TO_UNKNOWN"), "unknown to");
        var future=CollateralLifecycleEvidencePolicy.evaluate(facts("8","8","FINAL","IRR",
                now.plusDays(1),now.plusDays(10),1,1));
        check(future.valuation().evidenceBlockers().contains("CLT_R3_VALUATION_NOT_YET_EFFECTIVE"), "future");
        var fx=CollateralLifecycleEvidencePolicy.evaluate(facts("8","8","FINAL","USD",
                now.minusDays(1),now.plusDays(1),1,1));
        check(fx.valuation().evidenceBlockers().contains("CLT_R3_CURRENT_VALUATION_CURRENCY_MISMATCH"), "fx mismatch");
        check(good.valuation().policyGaps().size()==2, "no fabricated policy");
        check(!good.evidenceKind().contains("FINAL_DECISION_APPROVED"), "evidence-only label");
        try {
            CollateralLifecycleEvidencePolicy.evaluate(facts("8","8","FINAL","IRR",
                now.minusDays(1),now.plusDays(1),-1,1));
            throw new AssertionError("negative count accepted");
        } catch (IllegalArgumentException expected) { passes++; }
        System.out.println("CLT_R3_LIFECYCLE_POLICY_PASS="+passes+"/"+passes);
    }
}
