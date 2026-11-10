package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * R3 pre-posting read model. Technical integrity of a reviewed Stage request is NOT
 * business approval, not a final-domain transaction, and never a write permit.
 * This projection deliberately fails closed while policy/schema gates G-04/05/06/07/11/18 remain open.
 */
public final class CollateralPostingReadinessPolicy {
    private CollateralPostingReadinessPolicy() {}

    public record Facts(Kind kind, String requestId, String collateralId, String stageStatus,
            String recordVersion, String storedHash, String calculatedHash,
            long createdAuditCount, long submittedAuditCount, long independentReviewAuditCount,
            long totalAuditCount, long physicalConflictCount,
            String collateralStatus, String collateralTypeStatus,
            String currentValuationId, String supersedesValuationId,
            boolean valuationReferenceProvided) {}

    public record Check(String code, String result, String explanation) {}

    public record Readiness(String kind, String requestId, String collateralId,
            String stageStatus, String recordVersion, boolean stageIntegrityVerified,
            boolean physicalConflictScreenPassed, boolean policyApproved,
            boolean finalPostingEnabled, boolean finalPostingEligible,
            List<Check> checks, List<String> integrityBlockers,
            List<String> mandatoryApprovalGates, String evidenceKind) {}

    private static Check check(String code, boolean success, String detail) {
        return new Check(code, success ? "PASS" : "BLOCKED", detail);
    }
    private static Check unknown(String code, String detail) {
        return new Check(code, "UNKNOWN", detail);
    }
    private static void add(List<Check> checks, String code, boolean success, String detail) {
        checks.add(check(code, success, detail));
    }

    public static Readiness evaluate(Facts f) {
        if (f == null || f.kind() == null || f.requestId() == null || f.collateralId() == null
                || f.recordVersion() == null || f.createdAuditCount() < 0 || f.submittedAuditCount() < 0
                || f.independentReviewAuditCount() < 0 || f.totalAuditCount() < 0
                || f.physicalConflictCount() < 0) {
            throw new IllegalArgumentException("CLT_R3_POSTING_FACTS_INVALID");
        }
        List<Check> checks = new ArrayList<>();
        add(checks, "STAGE_REVIEWED", "REVIEWED".equals(f.stageStatus()),
                "REVIEWED is internal Stage screening only");
        add(checks, "STAGE_VERSION", "3".equals(f.recordVersion()),
                "Current R3 transition model is DRAFT(1) > SUBMITTED(2) > REVIEWED(3)");
        boolean hashWellFormed = f.storedHash() != null && f.storedHash().matches("[0-9a-f]{64}");
        add(checks, "PAYLOAD_FINGERPRINT", hashWellFormed
                && Objects.equals(f.storedHash(), f.calculatedHash()),
                "Recomputed canonical payload from Oracle Stage columns");
        add(checks, "CREATED_AUDIT", f.createdAuditCount() == 1,
                "Exact creation event by maker with matching snapshot hash");
        add(checks, "SUBMITTED_AUDIT", f.submittedAuditCount() == 1,
                "Exact submit event by maker with reason and matching snapshot hash");
        add(checks, "INDEPENDENT_REVIEW_AUDIT", f.independentReviewAuditCount() == 1,
                "Review event by non-maker with reason and matching snapshot hash");
        add(checks, "AUDIT_EVENT_COUNT", f.totalAuditCount() == 3,
                "Stage REVIEWED currently requires exactly CREATED/SUBMITTED/REVIEWED events");
        add(checks, "COLLATERAL_NOT_RELEASED", f.collateralStatus() != null
                && !List.of("RELEASED", "CANCELLED", "CANCELED").contains(f.collateralStatus()),
                "Physical collateral not found in known terminal states; other statuses need bank policy");
        add(checks, "COLLATERAL_TYPE_ACTIVE", "ACTIVE".equals(f.collateralTypeStatus()),
                "Type is active in Oracle reference data");
        add(checks, "NO_PHYSICAL_DUPLICATE", f.physicalConflictCount() == 0,
                "Matching physical relation/insurance number/valuation reference does not already exist");
        if (f.kind() == Kind.VALUATION) {
            add(checks, "VALUATION_SUPERSESSION_STABLE",
                    Objects.equals(f.currentValuationId(), f.supersedesValuationId()),
                    "Current valuation pointer must match proposed superseded row (or both absent)");
            if (!f.valuationReferenceProvided()) {
                checks.add(unknown("VALUATION_REFERENCE_DUPLICATION",
                        "No reference provided; manual cross-reference and final idempotency check are required"));
            }
        }
        // No policy should be inferred from the existence of a database row or an internal REVIEWED event.
        List<String> approvals = switch (f.kind()) {
            case SPONSOR -> List.of("G-05_SPONSOR_APPROVAL_CANCELLATION_AUDIT",
                    "G-11_MINIMUM_VALID_SPONSOR_POLICY", "G-09_PARTY_AND_DEBTOR_INTEGRATION");
            case INSURANCE -> List.of("G-04_APPROVED_COVERAGE_BY_COLLATERAL_TYPE",
                    "G-06_RENEWAL_CANCELLATION_LIFECYCLE", "G-09_INSURANCE_EXTERNAL_VERIFICATION");
            case VALUATION -> List.of("G-07_FINAL_VALUATION_APPROVAL_CONTRACT",
                    "G-18_APPRAISABILITY_REVALUATION_INTERVAL_AND_BUSINESS_DATE",
                    "G-09_VALUER_AND_EXTERNAL_VERIFICATION");
        };
        // All kinds also depend on an approved, atomic final posting contract and documented migration.
        List<String> required = new ArrayList<>(approvals);
        required.add("R3_APPROVED_POSTING_SCHEMA_TRANSACTION_AUDIT_AND_ROLLBACK");
        List<String> blockers = checks.stream()
                .filter(c -> "BLOCKED".equals(c.result()))
                .map(Check::code).toList();
        boolean stable = blockers.isEmpty()
                && checks.stream().noneMatch(c -> "UNKNOWN".equals(c.result()));
        boolean conflictFree = f.physicalConflictCount() == 0;
        return new Readiness(f.kind().name(), f.requestId(), f.collateralId(),
                f.stageStatus(), f.recordVersion(), stable, conflictFree,
                false, false, false, List.copyOf(checks), List.copyOf(blockers),
                List.copyOf(required),
                "ORACLE_BRANCH_SCOPED_SINGLE_STATEMENT_STAGE_PREPOSTING_DRY_RUN_NO_DOMAIN_DML");
    }
}
