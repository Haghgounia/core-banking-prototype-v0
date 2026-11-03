package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.SerialRangeInput;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draft-stage rule from Collateral V1.42: serials are exact decimal strings;
 * one row is 1..20 consecutive items, inclusive, with no overlapping rows.
 * This is NOT a bank-approved operational asset identity or final DDL model.
 */
public final class CollateralSerialRangePolicy {
    private CollateralSerialRangePolicy() {}
    private static final int MAX_ROWS = 80;
    private static final BigInteger MAX_ITEMS_PER_ROW = BigInteger.valueOf(20);
    public record Assessment(List<SerialRangeInput> rows, int itemCount, String totalAmountIRR) {}

    public static List<SerialRangeInput> normalize(List<SerialRangeInput> source) {
        if (source == null || source.isEmpty()) return List.of();
        if (source.size() > MAX_ROWS) return invalid("CLT_R2C_TOO_MANY_SERIAL_ROWS");
        List<SerialRangeInput> rows = new ArrayList<>();
        for (SerialRangeInput input : source) {
            if (input == null) return invalid("CLT_R2C_SERIAL_ROW_MISSING");
            String from = digits(input.serialFrom());
            String to = digits(input.serialTo());
            BigInteger start = new BigInteger(from), end = new BigInteger(to);
            if (end.compareTo(start) < 0) return invalid("CLT_R2C_SERIAL_END_BEFORE_START");
            BigInteger count = end.subtract(start).add(BigInteger.ONE);
            if (count.compareTo(MAX_ITEMS_PER_ROW) > 0) return invalid("CLT_R2C_SERIAL_ROW_OVER_20");
            String amount = input.totalAmount();
            if (amount == null || !amount.matches("[1-9][0-9]{0,15}"))
                return invalid("CLT_R2C_ROW_AMOUNT_IRR_INVALID");
            rows.add(new SerialRangeInput(from, to, new BigInteger(amount).toString()));
        }
        rows.sort(Comparator.comparing((SerialRangeInput r) -> new BigInteger(r.serialFrom()))
                .thenComparing(r -> new BigInteger(r.serialTo())));
        BigInteger previousEnd = null;
        for (SerialRangeInput row : rows) {
            BigInteger start = new BigInteger(row.serialFrom());
            if (previousEnd != null && start.compareTo(previousEnd) <= 0)
                return invalid("CLT_R2C_SERIAL_RANGE_OVERLAP");
            previousEnd = new BigInteger(row.serialTo());
        }
        return List.copyOf(rows);
    }

    public static Assessment assess(boolean countable, String currency, String estimatedValue,
                                    List<SerialRangeInput> source) {
        List<SerialRangeInput> rows = normalize(source);
        if (!countable && !rows.isEmpty()) return invalid("CLT_R2C_TYPE_NOT_COUNTABLE");
        if (countable && rows.isEmpty()) return invalid("CLT_R2C_SERIAL_RANGES_REQUIRED");
        if (countable && !"IRR".equals(currency))
            return invalid("CLT_R2C_COUNTABLE_FOREIGN_CURRENCY_POLICY_OPEN");
        BigInteger total = BigInteger.ZERO;
        int quantity = 0;
        for (SerialRangeInput row : rows) {
            total = total.add(new BigInteger(row.totalAmount()));
            quantity += new BigInteger(row.serialTo()).subtract(new BigInteger(row.serialFrom()))
                    .intValueExact() + 1;
        }
        if (countable && new BigDecimal(estimatedValue).compareTo(new BigDecimal(total)) != 0)
            return invalid("CLT_R2C_ITEM_AMOUNT_SUM_MISMATCH");
        return new Assessment(rows, quantity, total.toString());
    }

    private static String digits(String raw) {
        if (raw == null) return invalid("CLT_R2C_SERIAL_INVALID");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch >= '\u06f0' && ch <= '\u06f9') ch = (char) ('0' + ch - '\u06f0');
            if (ch >= '\u0660' && ch <= '\u0669') ch = (char) ('0' + ch - '\u0660');
            result.append(ch);
        }
        String value = result.toString().strip();
        if (!value.matches("[0-9]{1,100}")) return invalid("CLT_R2C_SERIAL_INVALID");
        return value;
    }
    private static <T> T invalid(String code) { throw new IllegalArgumentException(code); }
}
