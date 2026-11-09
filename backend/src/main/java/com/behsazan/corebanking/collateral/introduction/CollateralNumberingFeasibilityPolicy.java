package com.behsazan.corebanking.collateral.introduction;

import java.util.ArrayList;
import java.util.List;

/**
 * G-17: exact-string, READ-ONLY capacity assessment of the UI V1.42 number layout.
 * No sequence is reserved, no number is generated, and no proposed scheme is approved.
 * The physical CLT.COLLATERAL.COLLATERAL_NUMBER is NUMBER(19,0).
 */
public final class CollateralNumberingFeasibilityPolicy {
    public static final int PHYSICAL_DIGITS = 19;
    public static final int JALALI_YEAR_DIGITS = 4;
    public static final int SEQUENCE_DIGITS = 3;
    public static final int CHECK_DIGITS = 1;
    public static final String PROPOSED_SCHEME = "TYPE_JALALI_YEAR_CUSTOMER_SEQ3_CHECK1_V1_UNAPPROVED";
    private CollateralNumberingFeasibilityPolicy() {}

    /** Deliberately excludes the real customer number and proposed number from API output. */
    public record Assessment(String proposedScheme, boolean policyApproved,
                             boolean physicallyRepresentable, boolean physicalColumnVerified,
                             int physicalMaxDigits, int observedOraclePrecision,
                             int typeCodeDigits, int customerNoDigits, int fixedDigits,
                             int requiredDigits, int spareDigits, String customerReferenceSource,
                             List<String> blockers) {}

    public static Assessment assess(String oracleTypeCode, String cifCustomerNumber,
                                    boolean cifCustomerReady) {
        return assess(oracleTypeCode, cifCustomerNumber, cifCustomerReady, 19);
    }
    public static Assessment assess(String oracleTypeCode, String cifCustomerNumber,
                                    boolean cifCustomerReady, int observedOraclePrecision) {
        String type = oracleTypeCode == null ? null : oracleTypeCode.strip();
        String customer = cifCustomerNumber == null ? null : cifCustomerNumber.strip();
        List<String> blockers = new ArrayList<>();
        int typeLen = type == null ? 0 : type.length();
        int customerLen = customer == null ? 0 : customer.length();
        boolean typeNumeric = type != null && type.matches("[0-9]{1,50}");
        boolean customerNumeric = customer != null && customer.matches("[0-9]{1,100}");
        if (!typeNumeric) blockers.add("G17_ORACLE_TYPE_CODE_MISSING_OR_NON_NUMERIC");
        if (!cifCustomerReady || !customerNumeric)
            blockers.add("G17_CIF_CUSTOMER_NUMBER_NOT_VERIFIED_NUMERIC");
        if (typeNumeric && type.startsWith("0"))
            blockers.add("G17_NUMBER19_LEADING_ZERO_LOSS");
        if (observedOraclePrecision != PHYSICAL_DIGITS)
            blockers.add("G17_ORACLE_COLLATERAL_NUMBER_COLUMN_MISMATCH");
        int fixed = JALALI_YEAR_DIGITS + SEQUENCE_DIGITS + CHECK_DIGITS;
        int required = typeLen + customerLen + fixed;
        if (typeNumeric && customerNumeric && required > PHYSICAL_DIGITS)
            blockers.add("G17_NUMBER19_PHYSICAL_OVERFLOW");
        if (typeNumeric && customerNumeric && required < 1)
            blockers.add("G17_EMPTY_COMPOSITE");
        boolean fits = blockers.isEmpty();
        // Even a geometrically fitting candidate cannot be issued until the G-17 decision.
        blockers.add("G17_SCHEME_NOT_APPROVED_NO_FINAL_ISSUANCE");
        return new Assessment(PROPOSED_SCHEME, false, fits,
                observedOraclePrecision == PHYSICAL_DIGITS, PHYSICAL_DIGITS,
                observedOraclePrecision, typeLen, customerLen, fixed, required,
                PHYSICAL_DIGITS - required, "CIF_PARTY_READINESS_CUSTOMER_NO",
                List.copyOf(blockers));
    }
}
