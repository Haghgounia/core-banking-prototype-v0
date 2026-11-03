package com.behsazan.corebanking.collateral.valuation;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Pure R3C policy. G-07/G-18 require formal bank decisions before finalization. */
public final class CollateralValuationPolicy {
    private CollateralValuationPolicy() {}

    public record Candidate(String typeCode, String methodCode, String valuationDate,
            String valuationAmount, String currencyCode, String forcedSaleValue,
            String eligibleValue, String haircutPercent, String valuerPartyId,
            String valuerName, String valuationReference, String effectiveFrom,
            String effectiveTo, String supersedesValuationId, String justification) {
        public String canonical() { return String.join("\u001f", typeCode, methodCode, valuationDate,
                valuationAmount, currencyCode, nil(forcedSaleValue), nil(eligibleValue), nil(haircutPercent),
                nil(valuerPartyId), nil(valuerName), nil(valuationReference), effectiveFrom,
                nil(effectiveTo), nil(supersedesValuationId), justification); }
    }

    public record RecordedPeriod(String state, boolean withinRecordedPeriod, List<String> blockers) {}

    private static String nil(String value) { return value == null ? "" : value; }
    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String required(String text, String code, int max) {
        String val = trim(text);
        if (val == null || val.isBlank() || val.length() > max) throw new IllegalArgumentException(code);
        return val;
    }
    private static String optional(String text, int max, String code) {
        String val = trim(text);
        if (val == null || val.isBlank()) return null;
        if (val.length() > max) throw new IllegalArgumentException(code);
        return val;
    }
    public static String id(String val) {
        if (val == null || !val.matches("[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("CLT_VALUATION_ID_INVALID");
        return val;
    }
    public static String requestKey(String val) {
        if (val == null || !val.matches("[A-Za-z0-9._-]{8,128}"))
            throw new IllegalArgumentException("CLT_VALUATION_IDEMPOTENCY_KEY_INVALID");
        return val;
    }
    public static String correlation(String val) {
        if (val == null || val.isBlank()) return "NO_CORRELATION";
        if (!val.matches("[A-Za-z0-9._:-]{1,100}"))
            throw new IllegalArgumentException("CLT_VALUATION_CORRELATION_INVALID");
        return val;
    }
    private static String code(String val, String problem) {
        String text = required(val, problem, 30);
        if (!text.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException(problem);
        return text;
    }
    public static LocalDate date(String val) {
        if (val == null || !val.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
            throw new IllegalArgumentException("CLT_VALUATION_DATE_INVALID");
        try { return LocalDate.parse(val); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("CLT_VALUATION_DATE_INVALID"); }
    }
    private static String amount(String val, boolean nullable, String code) {
        String text = trim(val);
        if (nullable && (text == null || text.isBlank())) return null;
        if (text == null || !text.matches("[0-9]{1,16}(\\.[0-9]{1,4})?"))
            throw new IllegalArgumentException(code);
        BigDecimal parsed = new BigDecimal(text);
        if (parsed.precision() > 20 || parsed.scale() > 4)
            throw new IllegalArgumentException(code);
        return parsed.stripTrailingZeros().toPlainString();
    }
    private static String haircut(String value) {
        String v = trim(value);
        if (v == null || v.isBlank()) return null;
        if (!v.matches("[0-9]{1,3}(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("CLT_VALUATION_HAIRCUT_INVALID");
        BigDecimal number = new BigDecimal(v);
        if (number.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("CLT_VALUATION_HAIRCUT_INVALID");
        return number.stripTrailingZeros().toPlainString();
    }
    public static Candidate normalize(CollateralValuationModels.CandidateRequest request) {
        if (request == null) throw new IllegalArgumentException("CLT_VALUATION_REQUEST_REQUIRED");
        String type = code(request.typeCode(), "CLT_VALUATION_TYPE_CODE_INVALID");
        String method = code(request.methodCode(), "CLT_VALUATION_METHOD_CODE_INVALID");
        String day = date(request.valuationDate()).toString();
        String from = date(request.effectiveFrom()).toString();
        String to = optional(request.effectiveTo(), 10, "CLT_VALUATION_EFFECTIVE_TO_INVALID");
        if (to != null) to = date(to).toString();
        if (date(from).isBefore(date(day)) || (to != null && date(to).isBefore(date(from))))
            throw new IllegalArgumentException("CLT_VALUATION_PERIOD_INVALID");
        String currency = required(request.currencyCode(), "CLT_VALUATION_CURRENCY_INVALID", 3);
        if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("CLT_VALUATION_CURRENCY_INVALID");
        String party = optional(request.valuerPartyId(), 19, "CLT_VALUATION_VALUER_PARTY_INVALID");
        if (party != null) party = id(party);
        String name = optional(request.valuerName(), 200, "CLT_VALUATION_VALUER_NAME_INVALID");
        if (party == null && name == null) throw new IllegalArgumentException("CLT_VALUATION_VALUER_REQUIRED");
        String previous = optional(request.supersedesValuationId(), 19, "CLT_VALUATION_PREVIOUS_INVALID");
        if (previous != null) previous = id(previous);
        return new Candidate(type, method, day,
                amount(request.valuationAmount(), false, "CLT_VALUATION_AMOUNT_INVALID"), currency,
                amount(request.forcedSaleValue(), true, "CLT_VALUATION_FORCED_SALE_INVALID"),
                amount(request.eligibleValue(), true, "CLT_VALUATION_ELIGIBLE_VALUE_INVALID"),
                haircut(request.haircutPercent()), party, name,
                optional(request.valuationReference(), 100, "CLT_VALUATION_REFERENCE_INVALID"),
                from, to, previous, required(request.justification(), "CLT_VALUATION_REASON_INVALID", 500));
    }
    public static RecordedPeriod recordedPeriod(String pointer, String rowId, String status,
            String effectiveFrom, String effectiveTo, String asOf, boolean sameCurrency) {
        List<String> blockers = new ArrayList<>();
        if (pointer == null || pointer.isBlank()) blockers.add("CLT_VALUATION_CURRENT_POINTER_MISSING");
        else if (rowId == null || !pointer.equals(rowId)) blockers.add("CLT_VALUATION_CURRENT_POINTER_UNRESOLVED");
        if (rowId != null && !"FINAL".equals(status)) blockers.add("CLT_VALUATION_CURRENT_NOT_FINAL");
        if (rowId != null && !sameCurrency) blockers.add("CLT_VALUATION_CURRENT_CURRENCY_DIFFERENT");
        if (rowId != null) {
            LocalDate day = date(asOf);
            if (effectiveFrom == null || day.isBefore(date(effectiveFrom)))
                blockers.add("CLT_VALUATION_EFFECTIVE_FROM_IN_FUTURE");
            if (effectiveTo == null) blockers.add("CLT_VALUATION_EFFECTIVE_TO_UNSPECIFIED");
            else if (day.isAfter(date(effectiveTo))) blockers.add("CLT_VALUATION_RECORDED_PERIOD_EXPIRED");
        }
        String state = blockers.isEmpty() ? "WITHIN_RECORDED_EFFECTIVE_PERIOD" :
                (blockers.contains("CLT_VALUATION_RECORDED_PERIOD_EXPIRED") ? "EXPIRED" : "NOT_PROVEN");
        // Even a final, in-period value is not a new-allocation decision: interval-unit policy missing.
        return new RecordedPeriod(state, blockers.isEmpty(), List.copyOf(blockers));
    }
    public static List<String> candidateBlockers(boolean typeActive, boolean methodActive,
            boolean currencyActive, boolean ownerEligible, boolean collateralTypeActive,
            String collateralStatus, String currentValuationId, String supersedesId,
            boolean supersedesIsFinalCurrent) {
        List<String> problems = new ArrayList<>();
        if (!typeActive) problems.add("CLT_VALUATION_TYPE_INACTIVE");
        if (!methodActive) problems.add("CLT_VALUATION_METHOD_INACTIVE");
        if (!currencyActive) problems.add("CLT_VALUATION_CURRENCY_INACTIVE");
        if (!ownerEligible) problems.add("CLT_VALUATION_OWNER_NOT_VERIFIED_BY_CIF");
        if (!collateralTypeActive) problems.add("CLT_VALUATION_COLLATERAL_TYPE_INACTIVE");
        if (!List.of("ACTIVE", "PLEDGED", "PARTIAL_RELEASE").contains(collateralStatus))
            problems.add("CLT_VALUATION_COLLATERAL_STATUS_NOT_ELIGIBLE_FOR_REQUEST");
        if (currentValuationId == null && supersedesId != null)
            problems.add("CLT_VALUATION_SUPERSEDES_WITHOUT_CURRENT");
        if (currentValuationId != null && supersedesId == null)
            problems.add("CLT_VALUATION_CURRENT_MUST_BE_REFERENCED");
        if (currentValuationId != null && supersedesId != null && !supersedesIsFinalCurrent)
            problems.add("CLT_VALUATION_SUPERSEDES_NOT_CURRENT_FINAL");
        return List.copyOf(problems);
    }
    public static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
