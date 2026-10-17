package com.behsazan.corebanking.posting.cash;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Pure, deterministic Formance-like source/destination policy for the cash principal slice.
 * Ledger position flow and formal GL debit/credit are different concepts.
 */
public final class CashPostingPolicy {
    private CashPostingPolicy() {}
    public static final String PURPOSE = "CASH_PRINCIPAL";
    public static final String CUSTOMER_POSITION = "CUSTOMER_DEPOSIT";
    public static final String CASH_POSITION = "CASH_CLEARING";
    public static final String CUSTOMER_GL = "CUSTOMER_DEPOSIT_LIABILITY";
    public static final String CASH_GL = "TELLER_CASH";

    public record Instruction(long depositTransactionId, long depositAccountId, String accountNumber,
                              String cashDeskCode, String transactionType, BigDecimal amount,
                              String currency, LocalDate bookingDate, LocalDate valueDate,
                              String productVersion, String bankUnit, String externalCashReference) {}
    public record GlLine(int sequence, String role, String direction, BigDecimal amountUnits) {}
    public record Plan(String sourcePositionRole, String destinationPositionRole, BigDecimal amountUnits,
                       List<GlLine> entries, String payloadHash, String reference) {}

    public static Plan make(Instruction r, int currencyScale) {
        if (r == null || r.depositTransactionId() <= 0 || r.depositAccountId() <= 0)
            throw new IllegalArgumentException("A positive, persisted DPS2 transaction and account are required");
        if (currencyScale < 0 || currencyScale > 8)
            throw new IllegalArgumentException("Unsupported monetary precision");
        require(r.accountNumber(), "accountNumber", 160);
        require(r.cashDeskCode(), "cashDeskCode", 160);
        require(r.productVersion(), "productVersion", 160);
        require(r.bankUnit(), "bankUnit", 160);
        require(r.externalCashReference(), "externalCashReference", 160);
        if (r.currency() == null || !r.currency().matches("[A-Z]{3}"))
            throw new IllegalArgumentException("ISO currency code is required");
        if (r.bookingDate() == null || r.valueDate() == null)
            throw new IllegalArgumentException("Booking date and value date are required");
        boolean deposit = "CASH_DEPOSIT".equals(r.transactionType());
        if (!deposit && !"CASH_WITHDRAWAL".equals(r.transactionType()))
            throw new IllegalArgumentException("Only authorized cash deposit/withdrawal can be journalled");
        if (r.amount() == null || r.amount().signum() <= 0)
            throw new IllegalArgumentException("The cash amount must be positive");
        BigDecimal units;
        try {
            units = r.amount().movePointRight(currencyScale).setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount has a fractional minor unit", exception);
        }
        if (units.precision() > 24)
            throw new IllegalArgumentException("Amount exceeds Oracle NUMBER(24,0)");
        List<GlLine> entries = deposit
                ? List.of(new GlLine(1, CASH_GL, "DEBIT", units), new GlLine(2, CUSTOMER_GL, "CREDIT", units))
                : List.of(new GlLine(1, CUSTOMER_GL, "DEBIT", units), new GlLine(2, CASH_GL, "CREDIT", units));
        if (entries.stream().filter(x -> "DEBIT".equals(x.direction())).map(GlLine::amountUnits)
                .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(
                entries.stream().filter(x -> "CREDIT".equals(x.direction())).map(GlLine::amountUnits)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)) != 0)
            throw new IllegalStateException("Unbalanced journal");
        String canonical = String.join("|", "DPS2", Long.toString(r.depositTransactionId()),
                Long.toString(r.depositAccountId()), r.accountNumber(), r.cashDeskCode(), r.transactionType(),
                units.toPlainString(), r.currency(), r.bookingDate().toString(), r.valueDate().toString(),
                r.productVersion(), r.bankUnit(), r.externalCashReference(), PURPOSE);
        return new Plan(deposit ? CASH_POSITION : CUSTOMER_POSITION,
                deposit ? CUSTOMER_POSITION : CASH_POSITION, units, entries,
                sha256(canonical), "DPS2-CASH-" + r.depositTransactionId());
    }

    private static void require(String value, String name, int max) {
        if (value == null || value.isBlank() || value.length() > max || value.indexOf('|') >= 0)
            throw new IllegalArgumentException(name + " is missing or invalid");
    }
    static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
