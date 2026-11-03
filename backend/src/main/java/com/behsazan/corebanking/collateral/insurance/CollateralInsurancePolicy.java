package com.behsazan.corebanking.collateral.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** BUC-07/SUC-09; staging eligibility is NOT insurance validity for allocation (G-04/G-06). */
public final class CollateralInsurancePolicy {
    private CollateralInsurancePolicy() {}

    public record Candidate(String insurerCode, String companyId, String insuranceTypeId,
            String depositNumber, String insuranceNumber, String insuranceAmount,
            String receivedAmount, String taxAmount, String validFrom, String validTo,
            String previousInsuranceId, String currencyCode, String justification) {
        public String canonical() {
            return String.join("\u001f", insurerCode, companyId, insuranceTypeId, depositNumber,
                    insuranceNumber, insuranceAmount, receivedAmount,
                    taxAmount == null ? "" : taxAmount, validFrom, validTo,
                    previousInsuranceId == null ? "" : previousInsuranceId,
                    currencyCode, justification);
        }
    }

    public static String id(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_INSURANCE_ID_INVALID");
        return new BigDecimal(value).toPlainString();
    }
    public static String optionalId(String value) {
        return value == null || value.isBlank() ? null : id(value);
    }
    public static String key(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,99}"))
            throw new IllegalArgumentException("CLT_INSURANCE_IDEMPOTENCY_KEY_INVALID");
        return value;
    }
    public static String correlation(String value) {
        if (value == null || value.isBlank()) return null;
        if (!value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}"))
            throw new IllegalArgumentException("CLT_INSURANCE_CORRELATION_INVALID");
        return value;
    }
    private static String field(String text, int max, String code) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty() || value.length() > max || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(code);
        return value;
    }
    private static String date(String text) {
        try {
            if (text == null || !text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
                throw new IllegalArgumentException("CLT_INSURANCE_DATE_INVALID");
            return LocalDate.parse(text).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("CLT_INSURANCE_DATE_INVALID", e);
        }
    }
    public static String amount(String value, boolean positive, boolean nullable) {
        if (nullable && (value == null || value.isBlank())) return null;
        if (value == null || !value.matches("[0-9]{1,16}(\\.[0-9]{1,4})?"))
            throw new IllegalArgumentException("CLT_INSURANCE_AMOUNT_FORMAT_INVALID");
        BigDecimal n = new BigDecimal(value);
        if (n.signum() < 0 || (positive && n.signum() == 0) || n.precision() > 20 || n.scale() > 4)
            throw new IllegalArgumentException("CLT_INSURANCE_AMOUNT_RANGE_INVALID");
        return n.stripTrailingZeros().toPlainString();
    }
    public static Candidate candidate(CollateralInsuranceModels.CandidateRequest input) {
        if (input == null) throw new IllegalArgumentException("CLT_INSURANCE_BODY_REQUIRED");
        String insurer = field(input.insurerCode(), 10, "CLT_INSURANCE_INSURER_INVALID");
        // In V1.42, bank-issued policy (code 1) has no contracted insurer integration.
        // Only customer-provided policy (code 2) can enter unapproved staging.
        if (!"2".equals(insurer))
            throw new IllegalArgumentException("CLT_INSURANCE_BANK_INSURER_INTEGRATION_UNAVAILABLE");
        String company = id(input.companyId());
        String type = id(input.insuranceTypeId());
        String deposit = field(input.depositNumber(), 15, "CLT_INSURANCE_DEPOSIT_NUMBER_INVALID");
        String number = field(input.insuranceNumber(), 20, "CLT_INSURANCE_NUMBER_INVALID");
        if (!number.matches("[^\\s]{1,20}"))
            throw new IllegalArgumentException("CLT_INSURANCE_NUMBER_INVALID");
        String currency = field(input.currencyCode(), 3, "CLT_INSURANCE_CURRENCY_INVALID");
        if (!currency.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("CLT_INSURANCE_CURRENCY_INVALID");
        String from = date(input.validFrom());
        String to = date(input.validTo());
        if (!LocalDate.parse(to).isAfter(LocalDate.parse(from)))
            throw new IllegalArgumentException("CLT_INSURANCE_DATE_RANGE_INVALID");
        String reason = field(input.justification(), 500, "CLT_INSURANCE_JUSTIFICATION_INVALID");
        if (reason.length() < 8) throw new IllegalArgumentException("CLT_INSURANCE_JUSTIFICATION_INVALID");
        return new Candidate(insurer, company, type, deposit, number,
                amount(input.insuranceAmount(), true, false),
                amount(input.receivedAmount(), false, false),
                amount(input.taxAmount(), false, true), from, to,
                optionalId(input.previousInsuranceId()), currency, reason);
    }

    public static List<String> blockers(boolean insurable, boolean typeActive,
            String collateralStatus, boolean companyActive, boolean insuranceTypeActive,
            boolean currencyActive, boolean ownerPartyEligible, boolean numberDuplicate, boolean periodOverlap,
            boolean renewalValid) {
        var errors = new ArrayList<String>();
        if (!typeActive) errors.add("CLT_INSURANCE_COLLATERAL_TYPE_INACTIVE");
        if (!insurable) errors.add("CLT_INSURANCE_COLLATERAL_NOT_INSURABLE");
        if (!("ACTIVE".equals(collateralStatus) || "PLEDGED".equals(collateralStatus)
                || "PARTIAL_RELEASE".equals(collateralStatus)))
            errors.add("CLT_INSURANCE_COLLATERAL_STATUS_NOT_ALLOWED");
        if (!companyActive) errors.add("CLT_INSURANCE_COMPANY_INACTIVE_OR_UNKNOWN");
        if (!insuranceTypeActive) errors.add("CLT_INSURANCE_TYPE_INACTIVE_OR_UNKNOWN");
        if (!currencyActive) errors.add("CLT_INSURANCE_CURRENCY_INACTIVE_OR_UNKNOWN");
        if (!ownerPartyEligible) errors.add("CLT_INSURANCE_OWNER_PARTY_NOT_VERIFIED_BY_CIF");
        if (numberDuplicate) errors.add("CLT_INSURANCE_COMPANY_NUMBER_DUPLICATE");
        if (periodOverlap) errors.add("CLT_INSURANCE_SAME_TYPE_PERIOD_OVERLAP");
        if (!renewalValid) errors.add("CLT_INSURANCE_RENEWAL_CHAIN_INVALID");
        return List.copyOf(errors);
    }
    public static boolean overlaps(String aFrom, String aTo, String bFrom, String bTo) {
        return aFrom.compareTo(bTo) <= 0 && bFrom.compareTo(aTo) <= 0;
    }
    public static boolean validRenewal(String parentCollateralId, String parentTypeId,
            String previousCollateralId, String previousTypeId,
            String previousTo, String proposedFrom, boolean previousAlreadyRenewed) {
        if (previousAlreadyRenewed || !parentCollateralId.equals(previousCollateralId)
                || !parentTypeId.equals(previousTypeId)) return false;
        try { return LocalDate.parse(previousTo).plusDays(1).equals(LocalDate.parse(proposedFrom)); }
        catch (DateTimeException e) { return false; }
    }
    public static String digest(String data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(data.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA256_UNAVAILABLE", e); }
    }
}
