package com.behsazan.corebanking.collateral.sponsor;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.List;

class CollateralSponsorPolicyTest {
    private static List<String> issues(String owner, String candidate,
            String status, String applicable, boolean duplicate, boolean cif) {
        return CollateralSponsorPolicy.blockers(owner, candidate, status, applicable, "ACTIVE", duplicate, cif);
    }
    @Test void oneValidSponsorIsNotStoppedByAnInventedGlobalMaximum() {
        assertTrue(issues("10", "11", "ACTIVE", "Y", false, true).isEmpty());
        assertTrue(issues("10", "11", "PLEDGED", "Y", false, true).isEmpty());
        assertTrue(issues("10", "11", "PARTIAL_RELEASE", "Y", false, true).isEmpty());
    }
    @Test void ownerDuplicateAndInvalidPartyAreIndependentBlockers() {
        var x = issues("10", "10", "ACTIVE", "Y", true, false);
        assertTrue(x.contains("CLT_SPONSOR_SAME_AS_COLLATERAL_OWNER"));
        assertTrue(x.contains("CLT_SPONSOR_ALREADY_REGISTERED"));
        assertTrue(x.contains("CLT_SPONSOR_PARTY_NOT_VERIFIED_BY_CIF"));
    }
    @Test void releasedSuspendedAndInapplicableAreClosed() {
        for (String status : List.of("RELEASED", "CLOSED", "SUSPENDED", "DRAFT"))
            assertTrue(issues("10", "11", status, "Y", false, true)
                    .contains("CLT_SPONSOR_COLLATERAL_STATUS_NOT_ALLOWED"));
        assertTrue(issues("10", "11", "ACTIVE", "N", false, true)
                .contains("CLT_SPONSOR_NOT_APPLICABLE_TO_TYPE"));
    }
    @Test void canonicalIdsAreDecimalStringsWithNoFloatingPoint() {
        assertEquals("9999999999999999999", CollateralSponsorPolicy.id("9999999999999999999"));
        assertThrows(IllegalArgumentException.class, () -> CollateralSponsorPolicy.id("1e9"));
        assertThrows(IllegalArgumentException.class, () -> CollateralSponsorPolicy.id("01"));
        assertThrows(IllegalArgumentException.class, () -> CollateralSponsorPolicy.id("0"));
        assertThrows(IllegalArgumentException.class, () -> CollateralSponsorPolicy.id("10000000000000000000"));
    }
    @Test void inactiveCollateralTypeIsNotEligibleForNewSponsors() {
        assertTrue(CollateralSponsorPolicy.blockers("10", "11", "ACTIVE", "Y", "RETIRED", false, true)
                .contains("CLT_SPONSOR_TYPE_NOT_ACTIVE"));
    }
    @Test void reasonDepositAndReplayContractAreStrict() {
        var value = CollateralSponsorPolicy.candidate("12345", " ACC-1 ", " معرفی ضامن مستقل ");
        assertEquals("ACC-1", value.depositNumber());
        assertEquals(CollateralSponsorPolicy.digest(value.canonical()),
                CollateralSponsorPolicy.digest(value.canonical()));
        assertNotEquals(CollateralSponsorPolicy.digest(value.canonical()),
                CollateralSponsorPolicy.digest(value.canonical() + "changed"));
        assertThrows(IllegalArgumentException.class, () -> CollateralSponsorPolicy.candidate("1", "a'.b", "reason code"));
        assertThrows(IllegalArgumentException.class, () -> CollateralSponsorPolicy.candidate("1", null, "short"));
        assertThrows(IllegalArgumentException.class, () -> CollateralSponsorPolicy.key("x"));
        assertEquals("sponsor-12345678", CollateralSponsorPolicy.key("sponsor-12345678"));
    }
}
