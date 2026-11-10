package com.behsazan.corebanking.collateral.insurance.cancellation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CLT R3 / G-06: cancellation is an append-only, bank-gated lifecycle EVENT.
 * No hard delete, no alteration of the historical insurance contract, no inferred
 * cancellation from renewal or expiration, and no automatic R4 deallocation.
 */
public final class CollateralR3InsuranceCancellationPolicy {
    private CollateralR3InsuranceCancellationPolicy() {}

    public record Check(String code, String result, String explanation) {}
    public record Facts(boolean matchedPolicy, boolean matchedCollateral, boolean matchedBranch,
            String collateralStatus, String validFrom, String validTo, String asOf,
            long allocationRowCount, long successorCount, boolean anyExistingCancellation,
            boolean eventLedgerKnown, boolean currentVersionKnown, boolean insurerStatusKnown) {}
    public record Assessment(boolean requestCandidate, boolean automaticCancellationAllowed,
            boolean knownAllocationFree, boolean historyPreserved, List<Check> checks,
            List<String> mandatoryDecisionGates) {}
    public record Command(String expectedVersion, String idempotencyKey, String reason) {}
    public record Stage(String status, String maker, String reviewer,
            String poster, String requestHash, String persistedHash) {}
    public record Release(String policyRevision, String businessRef, String dataRef,
            String coverageRef, String allocationRef, String integrationRef, String downstreamRef,
            String approvedBy, boolean currentlyApproved) {}
    public record Impact(String decisionRef, String coverageDecisionRef,
            String allocationDecisionRef, String approvedBy, String replacementInsuranceId,
            boolean independentlyApproved) {}
    public record Ledger(String requestId, String insuranceId, String collateralId,
            String eventId, String key, String hash, String reason, String version,
            String postingSubject, String policyRevision) {}

    private static Check check(String code, boolean pass, String explanation) {
        return new Check(code, pass ? "PASS" : "BLOCKED", explanation);
    }
    private static Check unknown(String code, String explanation) {
        return new Check(code, "UNKNOWN", explanation);
    }
    public static Assessment assess(Facts f) {
        if (f == null) throw new IllegalArgumentException("CLT_R3_CANCEL_FACTS_REQUIRED");
        List<Check> checks = new ArrayList<>();
        checks.add(check("POLICY_SCOPED", f.matchedPolicy() && f.matchedCollateral() && f.matchedBranch(),
                "The policy must belong to the selected collateral and authenticated branch"));
        checks.add(check("POLICY_NOT_ALREADY_CANCELLED", !f.anyExistingCancellation() && f.eventLedgerKnown(),
                "No prior final cancellation event may exist; missing event ledger is not proof"));
        checks.add(check("NO_RENEWAL_SUCCESSOR", f.successorCount()==0,
                "An existing renewal successor requires independent lifecycle resolution"));
        checks.add(check("NO_ALLOCATION_ROWS", f.allocationRowCount()==0,
                "All allocation rows conservatively block cancellation; USED does not prove release"));
        checks.add(check("COLLATERAL_NOT_TERMINATED", ("ACTIVE".equals(f.collateralStatus()) || "PLEDGED".equals(f.collateralStatus())
                        || "PARTIAL_RELEASE".equals(f.collateralStatus())), "A terminated collateral requires separate approved policy"));
        checks.add(check("POLICY_ROW_VERSION_KNOWN", f.currentVersionKnown(),
                "The policy record version is needed for compare-and-set cancellation"));
        checks.add(check("INSURER_STATUS_KNOWN", f.insurerStatusKnown(),
                "Insurance-company acceptance and external verification are not inferred"));
        checks.add(unknown("G04_COVERAGE_REPLACEMENT_POLICY", "Minimum coverage and replacement must be bank-approved"));
        checks.add(unknown("G06_CANCELLATION_IMPACT_POLICY", "Cancellation impact, effective date, and approvals require bank decisions"));
        checks.add(unknown("G09_DOWNSTREAM_BINDING", "R4 and external insurance reconciliation not proven"));
        boolean scoped=f.matchedPolicy() && f.matchedCollateral() && f.matchedBranch();
        boolean statusAllowed="ACTIVE".equals(f.collateralStatus())
                || "PLEDGED".equals(f.collateralStatus())
                || "PARTIAL_RELEASE".equals(f.collateralStatus());
        boolean candidate=scoped && !f.anyExistingCancellation() && f.successorCount()==0
                && f.currentVersionKnown() && f.eventLedgerKnown() && statusAllowed
                && f.insurerStatusKnown() && f.allocationRowCount()>=0 && f.asOf()!=null;
        return new Assessment(candidate, false, f.allocationRowCount()==0, true,
                List.copyOf(checks), List.of("G-04_INSURANCE_COVERAGE_POLICY_APPROVAL",
                    "G-06_CANCELLATION_WORKFLOW_IMPACT_AND_EVENT_MODEL",
                    "G-09_R4_AND_INSURER_CONSUMER_CONTRACT",
                    "D-07_ORACLE_CANCELLATION_LEDGER_SCHEMA_APPROVAL"));
    }
    private static boolean ref(String s) {
        return s!=null && s.trim().length()>=8 && s.trim().length()<=120;
    }
    public static String key(String s) {
        if (s==null || !s.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,99}"))
            throw new IllegalArgumentException("CLT_R3_CANCEL_IDEMPOTENCY_INVALID");
        return s;
    }
    public static String reason(String s) {
        if (s==null || s.trim().length()<8 || s.trim().length()>500)
            throw new IllegalArgumentException("CLT_R3_CANCEL_REASON_INVALID");
        return s.trim();
    }
    public static String id(String s) {
        if (s==null || !s.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_R3_CANCEL_ID_INVALID");
        return s;
    }
    public static String version(String s) {
        if (s==null || !s.matches("[1-9][0-9]{0,9}"))
            throw new IllegalArgumentException("CLT_R3_CANCEL_VERSION_INVALID");
        return s;
    }
    public static void requireStageTransition(String oldStatus, String action,
            String maker, String reviewer, String actor, boolean permitted) {
        if (!permitted || actor==null || actor.isBlank() || maker==null || maker.isBlank())
            throw new SecurityException("CLT_R3_CANCEL_ACTOR_SCOPE_REQUIRED");
        String expected = switch (action) {
            case "SUBMIT", "WITHDRAW" -> "DRAFT";
            case "REVIEW", "REJECT" -> "SUBMITTED";
            default -> throw new IllegalArgumentException("CLT_R3_CANCEL_ACTION_INVALID");
        };
        if ("WITHDRAW".equals(action) && "SUBMITTED".equals(oldStatus)) expected="SUBMITTED";
        if (!expected.equals(oldStatus))
            throw new IllegalStateException("CLT_R3_CANCEL_STAGE_TRANSITION_INVALID");
        if ("SUBMIT".equals(action) || "WITHDRAW".equals(action)) {
            if (!maker.equals(actor))
                throw new SecurityException("CLT_R3_CANCEL_MAKER_REQUIRED");
        } else if (maker.equals(actor) || (reviewer!=null && !reviewer.isBlank()))
            throw new SecurityException("CLT_R3_CANCEL_INDEPENDENT_REVIEWER_REQUIRED");
    }
    public static void requirePosting(Command cmd, Stage stage, Release release, Impact impact,
            Assessment current, boolean featureEnabled, boolean schemaVerified,
            boolean permitted, boolean replacementVerified, boolean noAllocationRows) {
        if (cmd==null || stage==null || current==null)
            throw new IllegalArgumentException("CLT_R3_CANCEL_POST_INPUT_REQUIRED");
        version(cmd.expectedVersion()); key(cmd.idempotencyKey()); reason(cmd.reason());
        if (!featureEnabled || !schemaVerified)
            throw new IllegalStateException("CLT_R3_CANCEL_POST_GATE_DISABLED");
        if (!permitted || !"REVIEWED".equals(stage.status())
                || !ref(stage.maker()) || !ref(stage.reviewer()) || !ref(stage.poster())
                || stage.maker().equals(stage.reviewer()) || stage.maker().equals(stage.poster())
                || stage.reviewer().equals(stage.poster()))
            throw new SecurityException("CLT_R3_CANCEL_THREE_PARTY_SEPARATION_REQUIRED");
        if (!Objects.equals(stage.requestHash(),stage.persistedHash()) || !ref(stage.requestHash()))
            throw new IllegalStateException("CLT_R3_CANCEL_STAGE_INTEGRITY_INVALID");
        if (release==null || !release.currentlyApproved() || !ref(release.policyRevision())
                || !ref(release.businessRef()) || !ref(release.dataRef())
                || !ref(release.coverageRef()) || !ref(release.allocationRef())
                || !ref(release.integrationRef()) || !ref(release.downstreamRef())
                || !ref(release.approvedBy()) || release.approvedBy().equals(stage.poster())
                || release.approvedBy().equals(stage.maker())
                || release.approvedBy().equals(stage.reviewer()))
            throw new IllegalStateException("CLT_R3_CANCEL_BANK_RELEASE_MISSING");
        if (impact==null || !impact.independentlyApproved() || !ref(impact.decisionRef())
                || !ref(impact.coverageDecisionRef()) || !ref(impact.allocationDecisionRef())
                || !ref(impact.approvedBy()) || impact.approvedBy().equals(stage.poster())
                || impact.approvedBy().equals(stage.maker()) || impact.approvedBy().equals(stage.reviewer()))
            throw new IllegalStateException("CLT_R3_CANCEL_INDEPENDENT_IMPACT_DECISION_MISSING");
        if (!current.requestCandidate() || !current.knownAllocationFree() || !noAllocationRows
                || current.checks().stream().anyMatch(c -> !c.code().startsWith("G") && !"PASS".equals(c.result()))
                || !replacementVerified)
            throw new IllegalStateException("CLT_R3_CANCEL_COVERAGE_OR_ALLOCATION_RISK");
    }
    public static void requireReplay(String expectedId, Command cmd, Stage stage, Ledger stored,
            String currentHash, String revision) {
        if (stored==null || !Objects.equals(stored.requestId(), expectedId)
                || !Objects.equals(stored.key(), cmd.idempotencyKey())
                || !Objects.equals(stored.hash(), currentHash)
                || !Objects.equals(stored.reason(), cmd.reason().trim())
                || !Objects.equals(stored.version(), cmd.expectedVersion())
                || !Objects.equals(stored.postingSubject(),stage.poster())
                || !Objects.equals(stored.policyRevision(),revision))
            throw new IllegalStateException("CLT_R3_CANCEL_REPLAY_CONFLICT");
    }
    public static String digest(String canonical) {
        return com.behsazan.corebanking.collateral.insurance.CollateralInsurancePolicy.digest(canonical);
    }
    public static BigDecimal numberId(String value) { return new BigDecimal(id(value)); }
    public static String asOf(LocalDate d) { return Objects.requireNonNull(d).toString(); }
}
