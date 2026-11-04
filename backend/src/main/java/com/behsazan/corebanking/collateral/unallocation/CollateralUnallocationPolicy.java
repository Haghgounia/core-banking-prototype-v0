package com.behsazan.corebanking.collateral.unallocation;

import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationModels.*;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestPolicy;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Pure R5A preflight contract. A positive snapshot NEVER authorizes a financial release. */
public final class CollateralUnallocationPolicy {
    private CollateralUnallocationPolicy() {}
    public record Normalized(String consumerDomain, String fileNumber, String requestedAmount,
            String currencyCode, String releaseMode, String reasonReference, String expectedVersion) {}
    public static String id(String raw) {
        return CollateralAllocationRequestPolicy.id(raw);
    }
    public static String idempotencyKey(String raw) {
        return CollateralAllocationRequestPolicy.key(raw);
    }
    public static int limit(int value) {
        if (value < 1 || value > 50) throw new IllegalArgumentException("CLT_R5A_HISTORY_LIMIT_INVALID");
        return value;
    }
    public static String version(String ifMatch) {
        if (ifMatch == null || !ifMatch.matches("\"[1-9][0-9]{0,9}\""))
            throw new IllegalArgumentException("CLT_R5A_IF_MATCH_REQUIRED_STRONG_VERSION");
        return new BigDecimal(ifMatch.substring(1, ifMatch.length() - 1)).toPlainString();
    }
    public static Normalized normalize(PreviewRequest request, String ifMatch) {
        if (request == null) throw new IllegalArgumentException("CLT_R5A_REQUEST_REQUIRED");
        String domain = CollateralCapacityPolicy.domain(request.consumerDomain());
        String file = id(request.fileNumber());
        String amount = CollateralCapacityPolicy.positiveMoney(request.requestedAmount())
                .stripTrailingZeros().toPlainString();
        String currency = CollateralCapacityPolicy.currency(request.currencyCode());
        if (request.releaseMode() == null || !(request.releaseMode().equals("FULL")
                || request.releaseMode().equals("PARTIAL")))
            throw new IllegalArgumentException("CLT_R5A_RELEASE_MODE_INVALID");
        if (request.reasonReference() == null || !request.reasonReference().matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,99}"))
            throw new IllegalArgumentException("CLT_R5A_REASON_REFERENCE_INVALID");
        return new Normalized(domain, file, amount, currency,
                request.releaseMode(), request.reasonReference(), version(ifMatch));
    }
    public static String digest(String collateralId, String allocationId, String branch,
            String caller, Normalized n) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            for (String value : List.of(collateralId, allocationId, branch, caller,
                    n.consumerDomain(), n.fileNumber(), n.requestedAmount(),
                    n.currencyCode(), n.releaseMode(), n.reasonReference(), n.expectedVersion())) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                d.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                d.update(bytes);
            }
            return HexFormat.of().formatHex(d.digest());
        } catch (NoSuchAlgorithmException e) {throw new IllegalStateException("CLT_R5A_SHA256_UNAVAILABLE",e);}
    }
    public static List<String> blockers(AllocationLine stored, Normalized n,
            String approvedModuleId, boolean verifiedServiceClient) {
        List<String> reasons = new ArrayList<>();
        if (!stored.used()) reasons.add("CLT_R5A_ALLOCATION_ALREADY_NOT_USED");
        if (!stored.recordVersion().equals(n.expectedVersion()))
            reasons.add("CLT_R5A_ALLOCATION_VERSION_MISMATCH");
        if (!stored.fileNumber().equals(n.fileNumber()))
            reasons.add("CLT_R5A_CONSUMER_FILE_NUMBER_MISMATCH");
        if (approvedModuleId == null || !approvedModuleId.equals(stored.moduleId()))
            reasons.add("CLT_R5A_DOMAIN_MODULE_ID_NOT_APPROVED_OR_MISMATCH");
        if (!stored.currencyCode().equals(n.currencyCode()))
            reasons.add("CLT_R5A_CURRENCY_MISMATCH_NO_FX_ASSUMPTION");
        int compare = new BigDecimal(n.requestedAmount()).compareTo(new BigDecimal(stored.allocatedAmount()));
        if (compare > 0) reasons.add("CLT_R5A_REQUEST_EXCEEDS_ALLOCATION");
        if (compare < 0 || n.releaseMode().equals("PARTIAL"))
            reasons.add("CLT_R5A_PARTIAL_UNALLOCATION_POLICY_NOT_APPROVED");
        if (!verifiedServiceClient) reasons.add("CLT_R5A_CONSUMER_CLIENT_NOT_VERIFIED");
        // These decisions are NOT inferable from the physical CLT DDL or the analyst UI.
        reasons.add("G-09_CONSUMER_OBLIGATION_STATUS_NOT_ATTESTED");
        reasons.add("G-13_BRANCH_AND_CROSS_BRANCH_RELEASE_AUTHORIZATION_NOT_APPROVED");
        reasons.add("CLT_R5A_DURABLE_UNALLOCATION_IDEMPOTENCY_NOT_IMPLEMENTED");
        reasons.add("CLT_R5A_CONSUMER_RELEASE_CONFIRMATION_NOT_VERIFIED");
        reasons.add("CLT_R5A_FINANCIAL_UNALLOCATION_AND_AUDIT_WRITES_DISABLED");
        return List.copyOf(reasons);
    }
    public static boolean everAuthorizesUnallocation() {return false;}
}
