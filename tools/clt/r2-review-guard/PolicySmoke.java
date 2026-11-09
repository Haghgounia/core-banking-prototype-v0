import com.behsazan.corebanking.collateral.introduction.CollateralReviewDecisionGuard;
import com.behsazan.corebanking.collateral.registration.CollateralRegistrationGatePolicy;
import com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadinessModels;
import java.util.List;

public class PolicySmoke {
    static int n;
    static void ok(boolean v, String message) {
        if (!v) throw new AssertionError(message); n++;
    }
    public static void main(String[] args) {
        var valid = new CollateralReviewDecisionGuard.Evidence(true,true,true,true,true,true,false,true);
        ok(CollateralReviewDecisionGuard.assess(valid).reviewAllowed(), "valid reviewed candidate");
        ok(!CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
            true,false,true,true,true,true,false,true)).reviewAllowed(), "tampered snapshot");
        ok(!CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
            true,true,false,true,true,true,false,true)).reviewAllowed(), "missing creation audit");
        ok(!CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
            true,true,true,false,true,true,false,true)).reviewAllowed(), "mismatched child ranges");
        ok(!CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
            true,true,true,true,false,true,false,true)).reviewAllowed(), "stale CIF rules");
        ok(!CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
            true,true,true,true,true,true,true,true)).reviewAllowed(), "cross owner conflict");
        ok(!CollateralReviewDecisionGuard.assess(new CollateralReviewDecisionGuard.Evidence(
            true,true,true,true,true,false,false,true)).reviewAllowed(), "unavailable candidate lookup");
        var sample = new CollateralRegistrationGatePolicy.Evidence(
                "77","20","REVIEWED","maker","checker","001","a".repeat(64),
                true,true,true,true,List.of(),true,true,true,true,true,"2","ASSET-7","1000","IRR",
                true,true,true,false);
        var report = CollateralRegistrationGatePolicy.assess(sample);
        ok(!report.eligibleForFinalRegistration() && !report.finalRegistrationEnabled(), "finalization disabled");
        ok(report.blockingCodes().contains("G-17"), "G17 still open");
        ok(report.blockingCodes().contains("G-14"), "G14 still open");
        ok(report.checks().stream().anyMatch(c -> c.code().equals("CREATION_AUDIT") && c.status().equals("VERIFIED")), "audit verified");
        ok(report.checks().stream().anyMatch(c -> c.code().equals("SERIAL_CHILD_READBACK") && c.status().equals("VERIFIED")), "child evidence verified");
        var problem = new CollateralRegistrationGatePolicy.Evidence(
                "77","20","REVIEWED","maker","checker","001","a".repeat(64),
                true,true,true,true,List.of(),true,true,true,true,true,"2","ASSET-7","1000","IRR",
                false,false,true,true);
        var blocked=CollateralRegistrationGatePolicy.assess(problem);
        ok(blocked.blockingCodes().contains("CREATION_AUDIT"), "audit mismatch blocks");
        ok(blocked.blockingCodes().contains("SERIAL_CHILD_READBACK"), "range mismatch blocks");
        ok(blocked.blockingCodes().contains("ASSET_STAGE_COLLISION"), "collision blocks");
        System.out.println("CLT_R2_REVIEW_GUARD_POLICY_SMOKE_PASS="+n);
    }
}
