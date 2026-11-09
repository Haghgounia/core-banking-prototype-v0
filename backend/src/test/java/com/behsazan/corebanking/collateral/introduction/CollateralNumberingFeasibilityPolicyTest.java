package com.behsazan.corebanking.collateral.introduction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralNumberingFeasibilityPolicyTest {
    @Test void exactNineteenDigitsStillRequiresApproval() {
        var out = CollateralNumberingFeasibilityPolicy.assess("12", "123456789", true);
        assertEquals(19, out.requiredDigits());
        assertTrue(out.physicallyRepresentable());
        assertFalse(out.policyApproved());
        assertEquals(0, out.spareDigits());
        assertTrue(out.blockers().contains("G17_SCHEME_NOT_APPROVED_NO_FINAL_ISSUANCE"));
    }
    @Test void detectsOverflowWithoutLongOrDouble() {
        var out = CollateralNumberingFeasibilityPolicy.assess("1234", "12345678", true);
        assertEquals(20, out.requiredDigits());
        assertFalse(out.physicallyRepresentable());
        assertTrue(out.blockers().contains("G17_NUMBER19_PHYSICAL_OVERFLOW"));
    }
    @Test void refusesNonNumericOracleCodeAndCifNumber() {
        var out = CollateralNumberingFeasibilityPolicy.assess("RLE", "C-100", true);
        assertFalse(out.physicallyRepresentable());
        assertTrue(out.blockers().contains("G17_ORACLE_TYPE_CODE_MISSING_OR_NON_NUMERIC"));
        assertTrue(out.blockers().contains("G17_CIF_CUSTOMER_NUMBER_NOT_VERIFIED_NUMERIC"));
    }
    @Test void neverSubstitutesPartyIdForUnknownCustomerNo() {
        var out = CollateralNumberingFeasibilityPolicy.assess("11", null, false);
        assertFalse(out.physicallyRepresentable());
        assertEquals(0, out.customerNoDigits());
    }
    @Test void detectsLeadingZeroLossInOracleNumberStorage() {
        var out = CollateralNumberingFeasibilityPolicy.assess("012", "1234", true);
        assertFalse(out.physicallyRepresentable());
        assertTrue(out.blockers().contains("G17_NUMBER19_LEADING_ZERO_LOSS"));
    }
    @Test void refusesUnexpectedPhysicalOracleSchema() {
        var out = CollateralNumberingFeasibilityPolicy.assess("1", "123", true, 20);
        assertFalse(out.physicallyRepresentable());
        assertFalse(out.physicalColumnVerified());
        assertEquals(20, out.observedOraclePrecision());
        assertTrue(out.blockers().contains("G17_ORACLE_COLLATERAL_NUMBER_COLUMN_MISMATCH"));
    }
    @Test void sourceIsCifAndNoNumberIsEverGenerated() {
        var out = CollateralNumberingFeasibilityPolicy.assess("1", "001", true);
        assertEquals("CIF_PARTY_READINESS_CUSTOMER_NO", out.customerReferenceSource());
        assertEquals("TYPE_JALALI_YEAR_CUSTOMER_SEQ3_CHECK1_V1_UNAPPROVED",out.proposedScheme());
        assertFalse(out.policyApproved());
    }
}
