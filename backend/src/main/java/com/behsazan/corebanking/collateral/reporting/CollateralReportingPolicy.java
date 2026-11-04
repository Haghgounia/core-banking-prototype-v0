package com.behsazan.corebanking.collateral.reporting;

import java.math.BigDecimal;

/** R6B: no financial authorization or cross-currency summation. */
public final class CollateralReportingPolicy {
    private CollateralReportingPolicy() {}
    public static int eventLimit(int limit) {
        if (limit < 1 || limit > 50) throw new IllegalArgumentException("CLT_REPORT_EVENT_LIMIT_1_TO_50");
        return limit;
    }
    public static String amount(BigDecimal n) {
        if (n == null) throw new IllegalArgumentException("CLT_REPORT_AMOUNT_REQUIRED");
        return n.toPlainString();
    }
    public static long count(Long n) {
        if (n == null || n < 0) throw new IllegalArgumentException("CLT_REPORT_COUNT_INVALID");
        return n;
    }
    public static boolean hasTotals(boolean fromOracle, boolean branchScoped) {
        return fromOracle && branchScoped;
    }
}
