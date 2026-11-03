package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.AttributeInput;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.Request;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** Pure, deterministic validation; identifiers and monetary amounts never pass through double. */
public final class CollateralIntroductionPolicy {
    private CollateralIntroductionPolicy() {}

    public record Normalized(String partyId, String collateralTypeId, String estimateAmount,
                             String currencyCode, String validFrom, String validTo,
                             String depositNumber, String remarks, List<AttributeInput> attributes) {
        public String canonical() {
            StringBuilder out = new StringBuilder();
            for (String field : new String[] {partyId, collateralTypeId, estimateAmount, currencyCode,
                    validFrom, validTo, depositNumber, remarks}) append(out, field);
            for (AttributeInput a : attributes) {
                append(out, a.attributeId());
                append(out, a.value());
            }
            return out.toString();
        }
    }
    private static void append(StringBuilder out, String v) {
        String value = v == null ? "" : v;
        out.append(value.length()).append(':').append(value);
    }
    public static String id(String s) {
        if (s == null || !s.matches("[1-9][0-9]{0,18}")) invalid("CLT_INTRODUCTION_ID_INVALID");
        return s;
    }
    public static String actor(String s) {
        if (s == null || s.isBlank() || s.length() > 100 || s.chars().anyMatch(c -> c < 32 || c == 127))
            invalid("CLT_INTRODUCTION_ACTOR_INVALID");
        return s;
    }
    public static String key(String s) {
        if (s == null || !s.matches("[A-Za-z0-9_-]{12,100}")) invalid("CLT_INTRODUCTION_IDEMPOTENCY_INVALID");
        return s;
    }
    public static String correlation(String s) {
        if (s == null || s.isBlank()) return java.util.UUID.randomUUID().toString();
        if (!s.matches("[A-Za-z0-9_-]{8,100}")) invalid("CLT_INTRODUCTION_CORRELATION_INVALID");
        return s;
    }
    public static String amount(String raw) {
        if (raw == null || !raw.matches("[0-9]{1,16}(\\.[0-9]{1,4})?"))
            invalid("CLT_INTRODUCTION_AMOUNT_INVALID");
        BigDecimal n = new BigDecimal(raw);
        if (n.signum() <= 0) invalid("CLT_INTRODUCTION_AMOUNT_MUST_BE_POSITIVE");
        return n.stripTrailingZeros().toPlainString();
    }
    public static String date(String raw, boolean optional) {
        if (optional && (raw == null || raw.isBlank())) return null;
        if (raw == null || !raw.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
            invalid("CLT_INTRODUCTION_DATE_INVALID");
        try {
            LocalDate day = LocalDate.parse(raw);
            if (day.getYear() < 1900 || day.getYear() > 2200) invalid("CLT_INTRODUCTION_DATE_RANGE_INVALID");
            return day.toString();
        } catch (DateTimeParseException e) { return invalid("CLT_INTRODUCTION_DATE_INVALID"); }
    }
    public static Normalized normalize(Request req) {
        if (req == null) return invalid("CLT_INTRODUCTION_REQUEST_MISSING");
        String party = id(req.partyId()), type = id(req.collateralTypeId());
        String estimate = amount(req.estimateAmount());
        String currency = req.currencyCode();
        if (currency == null || !currency.matches("[A-Z]{3}")) invalid("CLT_INTRODUCTION_CURRENCY_INVALID");
        String from = date(req.validFrom(), false), to = date(req.validTo(), true);
        if (to != null && LocalDate.parse(to).isBefore(LocalDate.parse(from)))
            invalid("CLT_INTRODUCTION_DATE_ORDER_INVALID");
        String deposit = trim(req.depositNumber()), remarks = trim(req.remarks());
        if (deposit != null && (deposit.length() > 15 || !deposit.matches("[A-Za-z0-9_/-]+")))
            invalid("CLT_INTRODUCTION_DEPOSIT_INVALID");
        if (remarks != null && (remarks.length() > 150 || hasControl(remarks)))
            invalid("CLT_INTRODUCTION_REMARKS_INVALID");
        List<AttributeInput> attributes = req.attributes() == null ? List.of() : req.attributes();
        if (attributes.size() > 80) invalid("CLT_INTRODUCTION_TOO_MANY_ATTRIBUTES");
        List<AttributeInput> clean = new ArrayList<>();
        for (AttributeInput a : attributes) {
            if (a == null || a.value() == null || a.value().isBlank() || hasControl(a.value()))
                invalid("CLT_INTRODUCTION_ATTRIBUTE_VALUE_INVALID");
            String value = a.value().strip();
            if (value.getBytes(StandardCharsets.UTF_8).length > 4000)
                invalid("CLT_INTRODUCTION_ATTRIBUTE_TOO_LONG");
            clean.add(new AttributeInput(id(a.attributeId()), value));
        }
        clean.sort(Comparator.comparing((AttributeInput a) -> new BigDecimal(a.attributeId()))
                .thenComparing(AttributeInput::value));
        Normalized result = new Normalized(party, type, estimate, currency, from, to,
                deposit, remarks, List.copyOf(clean));
        if (result.canonical().getBytes(StandardCharsets.UTF_8).length > 12000)
            invalid("CLT_INTRODUCTION_PAYLOAD_TOO_LARGE");
        return result;
    }
    private static String trim(String s) { return s == null || s.isBlank() ? null : s.strip(); }
    private static boolean hasControl(String s) { return s.chars().anyMatch(c -> c < 32 || c == 127); }
    public static String validateValue(String kind, String value, String permittedValues) {
        if (value == null || value.isBlank()) return invalid("CLT_INTRODUCTION_ATTRIBUTE_EMPTY");
        return switch (kind) {
            case "TEXT" -> {
                if (value.getBytes(StandardCharsets.UTF_8).length > 4000)
                    invalid("CLT_INTRODUCTION_TEXT_TOO_LONG");
                yield value;
            }
            case "NUMBER" -> {
                if (!value.matches("-?[0-9]{1,18}(\\.[0-9]{1,4})?"))
                    invalid("CLT_INTRODUCTION_ATTRIBUTE_NUMBER_INVALID");
                BigDecimal n = new BigDecimal(value);
                if (n.precision() > 22 || n.scale() > 4)
                    invalid("CLT_INTRODUCTION_ATTRIBUTE_NUMBER_PRECISION");
                yield n.stripTrailingZeros().toPlainString();
            }
            case "DATE" -> date(value, false);
            case "BOOLEAN" -> {
                if (!Set.of("Y", "N").contains(value)) invalid("CLT_INTRODUCTION_BOOLEAN_INVALID");
                yield value;
            }
            case "LIST" -> {
                // The physical column has no structured child option table. A single literal value
                // is safe; multi-choice strings require an approved parser and are not guessed.
                if (permittedValues == null || !permittedValues.equals(value))
                    invalid("CLT_INTRODUCTION_LIST_MAPPING_PENDING");
                yield value;
            }
            default -> invalid("CLT_INTRODUCTION_ATTRIBUTE_TYPE_UNSUPPORTED");
        };
    }
    public static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA256_UNAVAILABLE", e); }
    }
    private static <T> T invalid(String code) { throw new IllegalArgumentException(code); }
}
