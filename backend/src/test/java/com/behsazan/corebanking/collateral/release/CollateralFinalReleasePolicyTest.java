package com.behsazan.corebanking.collateral.release;

import com.behsazan.corebanking.collateral.release.CollateralFinalReleasePolicy.Facts;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralFinalReleasePolicyTest {
    private static final Facts NO_ALLOC=new Facts(0,0,0,0,0,false,false,null,"1");
    @Test void absenceOfAllocationNeverGrantsRelease(){assertFalse(CollateralFinalReleasePolicy.finalReleaseAllowed(NO_ALLOC));}
    @Test void activeAllocationIsBlocker(){assertTrue(CollateralFinalReleasePolicy.blockers(new Facts(1,1,0,0,0,false,false,null,"1")).contains("CLT_R5C_ACTIVE_ALLOCATIONS_REMAIN"));}
    @Test void foreignBranchIsBlocker(){assertTrue(CollateralFinalReleasePolicy.blockers(new Facts(0,1,1,0,0,false,false,null,"1")).contains("CLT_R5C_CROSS_BRANCH_ALLOCATION_REVIEW_REQUIRED"));}
    @Test void inquiryRequiredIsBlocker(){assertTrue(CollateralFinalReleasePolicy.blockers(new Facts(0,0,0,0,0,true,false,null,"1")).contains("CLT_R5C_REQUIRED_RELEASE_INQUIRY_NOT_ATTESTED"));}
    @Test void terminalDoesNotProveRelease(){assertTrue(CollateralFinalReleasePolicy.blockers(new Facts(0,0,0,0,0,false,true,null,"1")).contains("CLT_R5C_TERMINAL_STATUS_IS_NOT_RELEASE_PROOF"));}
    @Test void versionMismatchFails(){assertTrue(CollateralFinalReleasePolicy.blockers(new Facts(0,0,0,0,0,false,false,"2","1")).contains("CLT_R5C_COLLATERAL_VERSION_MISMATCH"));}
    @Test void etagRequiresStrongCanonical(){assertThrows(IllegalArgumentException.class,()->CollateralFinalReleasePolicy.strongEtag("W/\"1\""));}
    @Test void limitBounded(){assertThrows(IllegalArgumentException.class,()->CollateralFinalReleasePolicy.limit(51));}
    @Test void oracleIdIsNotJavascriptNumber(){assertEquals("9999999999999999999",CollateralFinalReleasePolicy.positiveId("9999999999999999999"));}
    @Test void missingAttestationsAlwaysBlock(){assertTrue(CollateralFinalReleasePolicy.blockers(NO_ALLOC).contains("G-21_LEGAL_HOLDS_DPS2_PLEDGES_AND_INQUIRIES_NOT_VERIFIED"));}
}
