package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy.Kind;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CollateralPostingReadinessPolicyTest {
    private static final String H = "a".repeat(64);
    private static CollateralPostingReadinessPolicy.Facts valid(Kind kind) {
        return new CollateralPostingReadinessPolicy.Facts(kind, "71", "12", "REVIEWED", "3", H, H,
                1,1,1,3,0,"ACTIVE","ACTIVE",null,null,true);
    }
    private static boolean blocked(CollateralPostingReadinessPolicy.Readiness r, String code) {
        return r.integrityBlockers().contains(code);
    }
    @Test void validStageIsNotBusinessPermissionForAllThreeKinds() {
        for (Kind kind : Kind.values()) {
            var r=CollateralPostingReadinessPolicy.evaluate(valid(kind));
            assertTrue(r.stageIntegrityVerified());
            assertFalse(r.finalPostingEnabled());
            assertFalse(r.finalPostingEligible());
            assertFalse(r.policyApproved());
            assertTrue(r.mandatoryApprovalGates().stream().anyMatch(g -> g.startsWith("G-09")));
        }
    }
    @Test void missingIndependentReviewCannotAdvance() {
        var f=valid(Kind.SPONSOR);
        var r=CollateralPostingReadinessPolicy.evaluate(new CollateralPostingReadinessPolicy.Facts(
                f.kind(),f.requestId(),f.collateralId(),f.stageStatus(),f.recordVersion(),
                f.storedHash(),f.calculatedHash(),1,1,0,2,0,"ACTIVE","ACTIVE",null,null,true));
        assertTrue(blocked(r,"INDEPENDENT_REVIEW_AUDIT"));
        assertTrue(blocked(r,"AUDIT_EVENT_COUNT"));
        assertFalse(r.stageIntegrityVerified());
    }
    @Test void noDuplicateOrFingerprintFalsePositive() {
        var f=valid(Kind.INSURANCE);
        var r=CollateralPostingReadinessPolicy.evaluate(new CollateralPostingReadinessPolicy.Facts(
                f.kind(),f.requestId(),f.collateralId(),"SUBMITTED","2", H,"b".repeat(64),
                1,1,1,3,1,"ACTIVE","INACTIVE",null,null,true));
        assertTrue(blocked(r,"STAGE_REVIEWED"));
        assertTrue(blocked(r,"STAGE_VERSION"));
        assertTrue(blocked(r,"PAYLOAD_FINGERPRINT"));
        assertTrue(blocked(r,"NO_PHYSICAL_DUPLICATE"));
        assertTrue(blocked(r,"COLLATERAL_TYPE_ACTIVE"));
    }
    @Test void valuationPointerMustRemainStableAndReferenceIsUnknownWhenAbsent() {
        var f=valid(Kind.VALUATION);
        var r=CollateralPostingReadinessPolicy.evaluate(new CollateralPostingReadinessPolicy.Facts(
                f.kind(),f.requestId(),f.collateralId(),f.stageStatus(),f.recordVersion(),
                f.storedHash(),f.calculatedHash(),1,1,1,3,0,"ACTIVE","ACTIVE","5","4",false));
        assertTrue(blocked(r,"VALUATION_SUPERSESSION_STABLE"));
        assertTrue(r.checks().stream().anyMatch(c -> c.code().equals("VALUATION_REFERENCE_DUPLICATION")
                && c.result().equals("UNKNOWN")));
        assertFalse(r.stageIntegrityVerified());
    }
    @Test void rejectedTerminalAndMissingAuditAreNotAccepted() {
        var f=valid(Kind.SPONSOR);
        var r=CollateralPostingReadinessPolicy.evaluate(new CollateralPostingReadinessPolicy.Facts(
                f.kind(),f.requestId(),f.collateralId(),"REJECTED","3",f.storedHash(),f.calculatedHash(),
                0,1,1,4,0,"RELEASED","ACTIVE",null,null,true));
        assertTrue(blocked(r,"STAGE_REVIEWED"));
        assertTrue(blocked(r,"CREATED_AUDIT"));
        assertTrue(blocked(r,"AUDIT_EVENT_COUNT"));
        assertTrue(blocked(r,"COLLATERAL_NOT_RELEASED"));
    }
    @Test void negativeFactsFailClosed() {
        var f=valid(Kind.SPONSOR);
        assertThrows(IllegalArgumentException.class, () -> CollateralPostingReadinessPolicy.evaluate(
                new CollateralPostingReadinessPolicy.Facts(f.kind(),f.requestId(),f.collateralId(),
                f.stageStatus(),f.recordVersion(),f.storedHash(),f.calculatedHash(),-1,1,1,3,0,
                "ACTIVE","ACTIVE",null,null,true)));
    }
}
