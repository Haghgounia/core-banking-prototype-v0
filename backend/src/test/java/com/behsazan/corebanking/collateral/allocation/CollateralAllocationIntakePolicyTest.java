package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralAllocationIntakePolicyTest {
    @Test void neverClaimsAuthorizationOrCapacityLock() {
        assertFalse(CollateralAllocationIntakePolicy.everAuthorizesAllocation());
        assertEquals("RECORDED_NOT_ALLOCATED",CollateralAllocationIntakePolicy.STATUS);
        assertTrue(CollateralAllocationIntakePolicy.OPERATIONAL_BLOCKERS.contains("CLT_ALLOCATION_CAPACITY_NOT_LOCKED"));
    }
    @Test void exactIdempotencyReplayAndConflict() {
        assertDoesNotThrow(()->CollateralAllocationIntakePolicy.requireReplayMatch("digest","digest",CollateralAllocationIntakePolicy.STATUS));
        assertThrows(IllegalArgumentException.class,()->CollateralAllocationIntakePolicy.requireReplayMatch("digest","modified",CollateralAllocationIntakePolicy.STATUS));
        assertThrows(IllegalStateException.class,()->CollateralAllocationIntakePolicy.requireReplayMatch("digest","digest","ALLOCATED"));
    }
    @Test void branchAndClientAffectRequestDigest() {
        var normalized=CollateralAllocationRequestPolicy.normalize(new Request("LOAN","FAC-5",
                "1234567890123456789","100000.0000","IRR",List.of(new Debtor("9000000000000000001","PRIMARY"))));
        String a=CollateralAllocationRequestPolicy.digest("001","service-a","99",normalized);
        assertNotEquals(a,CollateralAllocationRequestPolicy.digest("002","service-a","99",normalized));
        assertNotEquals(a,CollateralAllocationRequestPolicy.digest("001","service-b","99",normalized));
    }
    @Test void validationIsBounded() {
        assertThrows(IllegalArgumentException.class,()->CollateralAllocationIntakePolicy.clientId("bad client"));
        assertThrows(IllegalArgumentException.class,()->CollateralAllocationIntakePolicy.actor(""));
        assertThrows(IllegalArgumentException.class,()->CollateralAllocationIntakePolicy.limit(1000));
        assertEquals(20,CollateralAllocationIntakePolicy.limit(20));
    }
}
