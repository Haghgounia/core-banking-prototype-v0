package com.behsazan.corebanking.posting.cash;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * R1-D8I DEV_ONLY risk boundary. No Spring / Oracle dependency so it can be
 * unit-tested without activating the posting infrastructure.
 * This is NOT a replacement for real bank approval or an isolated UAT.
 */
public final class DevOnlyCashPostingGuard {
    private DevOnlyCashPostingGuard() {}

    public record Settings(boolean postingDevProfile, boolean productionProfile,
            boolean syntheticAccountConfirmed, String actualDb, String actualPdb,
            String expectedDb, String expectedPdb, long pilotAccountId,
            String cashDeskCode, String currency, String productVersion,
            String bankUnit, String externalReferencePrefix, BigDecimal maxAmount,
            String bookCode, String legalEntity, String ledgerCode, String chartCode,
            String depositRule, String withdrawalRule,
            String depositMapping, String withdrawalMapping) {}

    public static void validate(Settings s, CashPostingPolicy.Instruction r) {
        Objects.requireNonNull(s, "DEV_ONLY settings");
        Objects.requireNonNull(r, "cash posting instruction");
        if (!s.postingDevProfile() || s.productionProfile())
            fail("dedicated posting-dev profile is required; production profiles are forbidden");
        if (!s.syntheticAccountConfirmed())
            fail("the selected pilot account must be verified as synthetic before DEV_ONLY writes");
        eq(s.expectedDb(), "FREEPDB1", "expected DB identity");
        eq(s.expectedPdb(), "FREEPDB1", "expected PDB identity");
        eq(s.actualDb(), s.expectedDb(), "actual DB identity");
        eq(s.actualPdb(), s.expectedPdb(), "actual PDB identity");
        if (s.pilotAccountId() != 1L || r.depositAccountId() != s.pilotAccountId())
            fail("only the configured DPS2 pilot account ID 1 is permitted");
        eq(s.cashDeskCode(), "UAT1405-DESK-01", "configured cash desk");
        eq(r.cashDeskCode(), s.cashDeskCode(), "instruction cash desk");
        eq(s.currency(), "IRR", "configured currency");
        eq(r.currency(), s.currency(), "instruction currency");
        eq(s.productVersion(), "9", "configured product version");
        eq(r.productVersion(), s.productVersion(), "instruction product version");
        eq(s.bankUnit(), "0101", "configured bank unit");
        eq(r.bankUnit(), s.bankUnit(), "instruction bank unit");
        eq(s.bookCode(), "UAT1405-IRR", "book code");
        eq(s.legalEntity(), "UAT_ENTITY_1405", "legal entity");
        eq(s.ledgerCode(), "UAT1405-LEDGER", "ledger code");
        eq(s.chartCode(), "UAT1405-CHART", "chart code");
        eq(s.depositRule(), "UAT1405-CASH-DEP", "deposit rule code");
        eq(s.withdrawalRule(), "UAT1405-CASH-WDR", "withdrawal rule code");
        eq(s.depositMapping(), "UAT1405-MAP-DEP", "deposit GL mapping code");
        eq(s.withdrawalMapping(), "UAT1405-MAP-WDR", "withdrawal GL mapping code");
        eq(s.externalReferencePrefix(), "UAT1405-", "cash-management reference prefix");
        if (r.externalCashReference() == null ||
                !r.externalCashReference().startsWith(s.externalReferencePrefix()) ||
                r.externalCashReference().length() > 160)
            fail("cash-management reference must start with UAT1405-");
        if (!"CASH_DEPOSIT".equals(r.transactionType()) &&
                !"CASH_WITHDRAWAL".equals(r.transactionType()))
            fail("only a cash deposit or cash withdrawal is allowed");
        if (s.maxAmount() == null || s.maxAmount().compareTo(new BigDecimal("100000")) != 0)
            fail("the configured maximum amount must be exactly 100000 IRR");
        if (r.amount() == null || r.amount().signum() <= 0 ||
                r.amount().compareTo(s.maxAmount()) > 0)
            fail("DEV_ONLY amount is missing or exceeds the 100000 IRR cap");
        LocalDate start = LocalDate.of(2026,3,21), end = LocalDate.of(2027,3,20);
        if (r.bookingDate() == null || r.valueDate() == null ||
                r.bookingDate().isBefore(start) || r.bookingDate().isAfter(end) ||
                r.valueDate().isBefore(start) || r.valueDate().isAfter(end))
            fail("booking and value dates must both fall in FY1405");
    }

    private static void eq(String value, String wanted, String field) {
        if (!Objects.equals(value,wanted)) fail("unexpected " + field);
    }
    private static void fail(String message) {
        throw new IllegalStateException("R1-D8I DEV_ONLY denied: " + message);
    }
}
