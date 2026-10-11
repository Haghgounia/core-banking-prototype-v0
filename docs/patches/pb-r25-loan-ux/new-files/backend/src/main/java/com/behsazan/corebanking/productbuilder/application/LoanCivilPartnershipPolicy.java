package com.behsazan.corebanking.productbuilder.application;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Distinguishes the civil partnership contract from the broader participatory loan family.
 * Source of truth is the persisted PDL.LOAN_TYPE row, not a UI family enum or a fabricated ID.
 */
final class LoanCivilPartnershipPolicy {
    private static final Set<String> CODES = Set.of(
        "CIVIL_PARTNERSHIP", "MOSHAREKAT_MADANI", "MOSHAREKAT-MADANI"
    );

    private LoanCivilPartnershipPolicy() {}

    static boolean matches(Map<String, ?> loanType) {
        if (loanType == null) return false;
        Object group = loanType.get("LOAN_GROUP");
        if (group == null) return false;
        try {
            if (new BigDecimal(group.toString()).intValueExact() != 3) return false;
        } catch (NumberFormatException | ArithmeticException e) {
            return false;
        }
        String name = String.valueOf(loanType.get("LOAN_TYPE_NAME") == null ? "" : loanType.get("LOAN_TYPE_NAME"))
            .replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ')
            .trim().replaceAll("\\s+", " ");
        Object code = loanType.get("LOAN_TYPE_CODE");
        String normalizedCode = code == null ? "" : code.toString().trim().toUpperCase(Locale.ROOT);
        return "مشارکت مدنی".equals(name) || CODES.contains(normalizedCode);
    }
}
