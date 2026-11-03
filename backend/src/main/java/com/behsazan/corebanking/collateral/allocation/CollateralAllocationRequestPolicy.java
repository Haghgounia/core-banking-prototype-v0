package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.*;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** R4B: pure, bounded contract validation. No provisional capacity is authorization. */
public final class CollateralAllocationRequestPolicy {
    private CollateralAllocationRequestPolicy() {}
    private static final Set<String> ROLES = Set.of("PRIMARY", "JOINT");
    public record Normalized(String consumerDomain, String caseReference, String fileNumber,
            String amount, String currencyCode, List<Debtor> debtors) {}

    public static String id(String raw) {
        if (raw == null || !raw.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_ALLOCATION_PARTY_OR_FILE_ID_INVALID");
        return raw;
    }
    public static String key(String raw) {
        try {
            if (raw == null || !raw.equals(UUID.fromString(raw).toString()))
                throw new IllegalArgumentException("CLT_ALLOCATION_IDEMPOTENCY_UUID_REQUIRED");
            return raw;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("CLT_ALLOCATION_IDEMPOTENCY_UUID_REQUIRED");
        }
    }
    public static Normalized normalize(Request request) {
        if (request == null) throw new IllegalArgumentException("CLT_ALLOCATION_REQUEST_REQUIRED");
        String domain = CollateralCapacityPolicy.domain(request.consumerDomain());
        if (request.caseReference() == null || !request.caseReference().matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}"))
            throw new IllegalArgumentException("CLT_ALLOCATION_CASE_REFERENCE_INVALID");
        String file = id(request.fileNumber());
        BigDecimal amount = CollateralCapacityPolicy.positiveMoney(request.amount());
        String currency = CollateralCapacityPolicy.currency(request.currencyCode());
        if (request.debtors() == null || request.debtors().isEmpty() || request.debtors().size() > 20)
            throw new IllegalArgumentException("CLT_ALLOCATION_DEBTOR_ROSTER_SIZE_INVALID");
        int primaryCount = 0;
        Set<String> seen = new HashSet<>();
        List<Debtor> debtors = new ArrayList<>();
        for (Debtor debtor : request.debtors()) {
            if (debtor == null || !ROLES.contains(debtor.role()))
                throw new IllegalArgumentException("CLT_ALLOCATION_DEBTOR_ROLE_INVALID");
            String party = id(debtor.partyId());
            if (!seen.add(party)) throw new IllegalArgumentException("CLT_ALLOCATION_DEBTOR_DUPLICATE");
            if ("PRIMARY".equals(debtor.role())) primaryCount++;
            debtors.add(new Debtor(party, debtor.role()));
        }
        if (primaryCount != 1) throw new IllegalArgumentException("CLT_ALLOCATION_ONE_PRIMARY_REQUIRED");
        debtors.sort(Comparator.comparing(Debtor::partyId));
        return new Normalized(domain, request.caseReference(), file,
                amount.stripTrailingZeros().toPlainString(), currency, List.copyOf(debtors));
    }

    /** Key is deliberately NOT part of payload digest: replay comparison needs payload identity. */
    public static String digest(String branch, String clientId, String collateralId, Normalized n) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            List<String> tokens = new ArrayList<>(List.of(branch, clientId, collateralId,
                    n.consumerDomain(), n.caseReference(), n.fileNumber(), n.amount(), n.currencyCode()));
            for (Debtor debtor : n.debtors()) {
                tokens.add(debtor.partyId());
                tokens.add(debtor.role());
            }
            for (String token : tokens) {
                byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
                md.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                md.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {throw new IllegalStateException("CLT_ALLOCATION_HASH_UNAVAILABLE", e);}
    }
    public static boolean conflict(List<Debtor> debtors, Set<String> sponsorIds) {
        for (Debtor d : debtors) if (sponsorIds.contains(d.partyId())) return true;
        return false;
    }
    public static List<String> blockers(boolean serviceClientVerified, boolean sponsorConflict,
            String requestCurrency, String collateralCurrency, String hypotheticalShortfall) {
        List<String> result = new ArrayList<>();
        if (!serviceClientVerified) result.add("CLT_ALLOCATION_CONSUMER_CLIENT_NOT_VERIFIED");
        if (sponsorConflict) result.add("CLT_ALLOCATION_DEBTOR_IS_COLLATERAL_SPONSOR");
        if (!requestCurrency.equals(collateralCurrency)) result.add("CLT_ALLOCATION_FX_POLICY_NOT_APPROVED");
        if (hypotheticalShortfall != null && new BigDecimal(hypotheticalShortfall).signum() > 0)
            result.add("CLT_ALLOCATION_ARITHMETIC_SHORTFALL");
        result.add("G-02_DURABLE_IDEMPOTENCY_REQUEST_REGISTRY_MISSING");
        result.add("G-09_CONSUMER_OBLIGATION_AND_COMPLETE_DEBTOR_ROSTER_NOT_ATTESTED");
        result.add("G-13_PER_INTRODUCTION_CROSS_BRANCH_PERMISSION_NOT_PROVEN");
        result.add("G-04_G-05_G-07_G-10_G-11_G-18_OPERATIONAL_POLICY_NOT_APPROVED");
        result.add("CLT_ALLOCATION_SERIALIZABLE_CAPACITY_LOCK_NOT_IMPLEMENTED");
        result.add("CLT_ALLOCATION_DOMAIN_MODULE_ID_NOT_APPROVED");
        return List.copyOf(result);
    }
}
