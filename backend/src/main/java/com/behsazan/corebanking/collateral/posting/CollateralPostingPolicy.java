package com.behsazan.corebanking.collateral.posting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pure posting invariants. Bank attestation is a *persisted independently issued permit*,
 * never a checkbox or amount supplied in the commit request. No FX or approved coverage
 * factor is inferred from CLT reference percentages.
 */
public final class CollateralPostingPolicy {
    private CollateralPostingPolicy() {}
    public static final Set<String> OPERATIONS = Set.of("ALLOCATE", "UNALLOCATE");
    public static final Set<String> RELEASE_MODES = Set.of("FULL", "PARTIAL");

    public static String id(String raw) {
        if (raw == null || !raw.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_POSTING_POSITIVE_NUMBER19_REQUIRED");
        return raw;
    }
    public static String key(String raw) {
        try {
            if (raw == null || !UUID.fromString(raw).toString().equals(raw))
                throw new IllegalArgumentException("CLT_POSTING_IDEMPOTENCY_UUID_REQUIRED");
            return raw;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("CLT_POSTING_IDEMPOTENCY_UUID_REQUIRED");
        }
    }
    public static String sequence(String raw) {
        if (raw == null || !raw.matches("[A-Z][A-Z0-9_]{0,29}"))
            throw new IllegalArgumentException("CLT_POSTING_APPROVED_SEQUENCE_MISSING");
        return raw;
    }
    public static String referenceCode(String raw) {
        if (raw == null || !raw.matches("[A-Z][A-Z0-9_]{0,29}"))
            throw new IllegalArgumentException("CLT_POSTING_BANK_REFERENCE_CODE_MISSING");
        return raw;
    }
    public static BigDecimal positiveMoney(String raw) {
        if (raw == null || !raw.matches("(?:0|[1-9][0-9]{0,15})(?:\\.[0-9]{1,4})?"))
            throw new IllegalArgumentException("CLT_POSTING_NUMBER20_4_REQUIRED");
        BigDecimal amount = new BigDecimal(raw);
        if (amount.signum() <= 0 || amount.precision() > 20 || amount.scale() > 4)
            throw new IllegalArgumentException("CLT_POSTING_POSITIVE_MONEY_REQUIRED");
        return amount;
    }
    public record Attestation(String operation, String status, String maker, String checker,
            String poster, String issuer, LocalDateTime expiresAt, LocalDateTime now,
            boolean obligationAttested, boolean debtorsAttested, boolean policyAttested,
            boolean sameCollateral, boolean sameBranch, boolean sameClient,
            boolean sameModule, boolean parentVersionMatched, boolean stagingMatched,
            boolean originalTransactionMatched) {}

    public static List<String> blockers(Attestation p) {
        List<String> problems = new ArrayList<>();
        if (!OPERATIONS.contains(p.operation())) problems.add("POSTING_OPERATION_INVALID");
        if (!"APPROVED".equals(p.status())) problems.add("POSTING_PERMIT_NOT_APPROVED");
        if (p.maker()==null || p.checker()==null || p.poster()==null || p.issuer()==null
                || p.maker().isBlank() || p.checker().isBlank() || p.issuer().isBlank()
                || p.maker().equals(p.checker()) || p.checker().equals(p.poster())
                || p.maker().equals(p.poster()) || p.issuer().equals(p.poster()))
            problems.add("POSTING_DUTIES_NOT_SEPARATED");
        if (p.expiresAt()==null || p.now()==null || !p.expiresAt().isAfter(p.now()))
            problems.add("POSTING_PERMIT_EXPIRED");
        if (!p.obligationAttested() || !p.debtorsAttested() || !p.policyAttested())
            problems.add("POSTING_BANK_ATTESTATIONS_INCOMPLETE");
        if (!p.sameCollateral() || !p.sameBranch()) problems.add("POSTING_COLLATERAL_BRANCH_MISMATCH");
        if (!p.sameClient() || !p.sameModule()) problems.add("POSTING_CONSUMER_BINDING_MISMATCH");
        if (!p.parentVersionMatched()) problems.add("POSTING_COLLATERAL_VERSION_CHANGED");
        if (!p.stagingMatched()) problems.add("POSTING_REVIEWED_STAGE_NOT_MATCHED");
        if ("UNALLOCATE".equals(p.operation()) && !p.originalTransactionMatched())
            problems.add("POSTING_ORIGINAL_TRANSACTION_NOT_VERIFIED");
        return List.copyOf(problems);
    }
    /** Only same-currency, bank-attested approved limit is usable. Never an FX conversion. */
    public static BigDecimal allocationRemaining(BigDecimal approvedLimit,
            BigDecimal recordedEligibleValue, BigDecimal currentlyAllocated, BigDecimal request,
            boolean allActiveSameCurrency) {
        if (!allActiveSameCurrency) throw new IllegalArgumentException("POSTING_ACTIVE_ALLOCATION_FX_UNRESOLVED");
        if (approvedLimit==null || recordedEligibleValue==null || currentlyAllocated==null || request==null
                || approvedLimit.signum()<=0 || recordedEligibleValue.signum()<=0
                || currentlyAllocated.signum()<0 || request.signum()<=0)
            throw new IllegalArgumentException("POSTING_APPROVED_CAPACITY_EVIDENCE_MISSING");
        if (approvedLimit.compareTo(recordedEligibleValue)>0)
            throw new IllegalArgumentException("POSTING_PERMIT_EXCEEDS_RECORDED_ELIGIBLE_VALUE");
        BigDecimal residual=approvedLimit.subtract(currentlyAllocated).subtract(request);
        if (residual.signum()<0) throw new IllegalArgumentException("POSTING_APPROVED_CAPACITY_EXCEEDED");
        return residual;
    }
    /** FULL marks USED=NO; PARTIAL retains USED=YES and reduces the current outstanding amount. */
    public static BigDecimal unallocationRemaining(BigDecimal outstanding,BigDecimal release,String mode){
        if (outstanding==null || release==null || outstanding.signum()<=0 || release.signum()<=0)
            throw new IllegalArgumentException("POSTING_UNALLOCATION_AMOUNT_INVALID");
        if (!RELEASE_MODES.contains(mode)) throw new IllegalArgumentException("POSTING_RELEASE_MODE_INVALID");
        if ("FULL".equals(mode) && release.compareTo(outstanding)!=0)
            throw new IllegalArgumentException("POSTING_FULL_RELEASE_MUST_EQUAL_OUTSTANDING");
        if ("PARTIAL".equals(mode) && release.compareTo(outstanding)>=0)
            throw new IllegalArgumentException("POSTING_PARTIAL_RELEASE_MUST_BE_LESS_THAN_OUTSTANDING");
        return outstanding.subtract(release);
    }
    public static boolean sameKey(String requested, String committed) {
        return requested!=null && requested.equals(committed);
    }
}
