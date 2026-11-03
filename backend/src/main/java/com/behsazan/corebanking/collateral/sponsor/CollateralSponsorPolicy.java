package com.behsazan.corebanking.collateral.sponsor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.ArrayList;

/** Domain rules for BUC-06 / SUC-07. No consumer debtor is involved at sponsor intake. */
public final class CollateralSponsorPolicy {
    private CollateralSponsorPolicy() {}

    public record Candidate(String partyId, String depositNumber, String justification) {
        public String canonical() {
            return partyId + "\u001f" + (depositNumber == null ? "" : depositNumber)
                    + "\u001f" + justification;
        }
    }

    public static String id(String input) {
        if (input == null || !input.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_SPONSOR_ID_INVALID");
        // Oracle NUMBER(19) is a decimal identifier. Never convert to float/double.
        return new BigDecimal(input).toPlainString();
    }

    public static Candidate candidate(String partyId, String deposit, String justification) {
        String party = id(partyId);
        String d = deposit == null || deposit.isBlank() ? null : deposit.trim();
        if (d != null && !d.matches("[A-Za-z0-9/_-]{1,15}"))
            throw new IllegalArgumentException("CLT_SPONSOR_DEPOSIT_NUMBER_INVALID");
        String reason = justification == null ? "" : justification.trim();
        if (reason.length() < 8 || reason.length() > 500 || reason.chars().anyMatch(c -> c < 32))
            throw new IllegalArgumentException("CLT_SPONSOR_JUSTIFICATION_INVALID");
        return new Candidate(party, d, reason);
    }

    public static String key(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,99}"))
            throw new IllegalArgumentException("CLT_SPONSOR_IDEMPOTENCY_KEY_INVALID");
        return value;
    }

    public static String correlation(String value) {
        if (value == null || value.isBlank()) return null;
        if (!value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}"))
            throw new IllegalArgumentException("CLT_SPONSOR_CORRELATION_INVALID");
        return value;
    }

    public static String digest(String canonical) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA256_UNAVAILABLE", e); }
    }

    /** Registrations are independent of debtor/obligor, tested only at allocation (SUC-08). */
    public static List<String> blockers(String ownerPartyId, String sponsorPartyId,
            String collateralStatus, String sponsorRequired, String typeStatus, boolean duplicate,
            boolean eligibleParty) {
        var errors = new ArrayList<String>();
        if (!"ACTIVE".equals(typeStatus)) errors.add("CLT_SPONSOR_TYPE_NOT_ACTIVE");
        if (!"Y".equals(sponsorRequired)) errors.add("CLT_SPONSOR_NOT_APPLICABLE_TO_TYPE");
        if (!("ACTIVE".equals(collateralStatus) || "PLEDGED".equals(collateralStatus)
                || "PARTIAL_RELEASE".equals(collateralStatus)))
            errors.add("CLT_SPONSOR_COLLATERAL_STATUS_NOT_ALLOWED");
        if (ownerPartyId != null && ownerPartyId.equals(sponsorPartyId))
            errors.add("CLT_SPONSOR_SAME_AS_COLLATERAL_OWNER");
        if (duplicate) errors.add("CLT_SPONSOR_ALREADY_REGISTERED");
        if (!eligibleParty) errors.add("CLT_SPONSOR_PARTY_NOT_VERIFIED_BY_CIF");
        // Deliberately no global maximum; minimum is unresolved in physical CLT (G-11).
        return List.copyOf(errors);
    }
}
