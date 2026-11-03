package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralAllocationRequestPolicyTest {
    private static Request r(List<Debtor> debtors, String amount) {
        return new Request("LOAN", "CASE-1", "9000000000000000001", amount, "IRR", debtors);
    }
    @Test void exact19DigitsAndDecimalNormalization() {
        var n = CollateralAllocationRequestPolicy.normalize(r(
                List.of(new Debtor("9000000000000000002", "PRIMARY")),"2000000.0000"));
        assertEquals("9000000000000000001",n.fileNumber());
        assertEquals("2000000",n.amount());
    }
    @Test void fullDebtorRosterMustHaveExactlyOnePrimaryAndNoDuplicates() {
        assertThrows(IllegalArgumentException.class,()->CollateralAllocationRequestPolicy.normalize(r(
                List.of(new Debtor("1","JOINT")),"1")));
        assertThrows(IllegalArgumentException.class,()->CollateralAllocationRequestPolicy.normalize(r(
                List.of(new Debtor("1","PRIMARY"),new Debtor("1","JOINT")),"1")));
    }
    @Test void bothPrimaryAndJointDebtorsCanConflictWithSponsor() {
        var list = List.of(new Debtor("1","PRIMARY"),new Debtor("2","JOINT"));
        assertTrue(CollateralAllocationRequestPolicy.conflict(list,Set.of("1")));
        assertTrue(CollateralAllocationRequestPolicy.conflict(list,Set.of("2")));
        assertFalse(CollateralAllocationRequestPolicy.conflict(list,Set.of("3")));
    }
    @Test void digestBindsClientBranchCaseFileAmountAndDebtors() {
        var n = CollateralAllocationRequestPolicy.normalize(r(List.of(new Debtor("1","PRIMARY")),"1"));
        var alt = CollateralAllocationRequestPolicy.normalize(r(List.of(new Debtor("1","PRIMARY")),"2"));
        assertNotEquals(CollateralAllocationRequestPolicy.digest("B1","loan-client","99",n),
                CollateralAllocationRequestPolicy.digest("B2","loan-client","99",n));
        assertNotEquals(CollateralAllocationRequestPolicy.digest("B1","loan-client","99",n),
                CollateralAllocationRequestPolicy.digest("B1","loan-client","99",alt));
    }
    @Test void requestNeverHasAuthorizationOnAnyPolicyOutcome() {
        for (boolean match : List.of(false,true)) {
            var blockers = CollateralAllocationRequestPolicy.blockers(true,match,"IRR","IRR","0");
            assertTrue(blockers.contains("CLT_ALLOCATION_SERIALIZABLE_CAPACITY_LOCK_NOT_IMPLEMENTED"));
            assertTrue(blockers.contains("G-02_DURABLE_IDEMPOTENCY_REQUEST_REGISTRY_MISSING"));
        }
    }
    @Test void idempotencyKeyMustBeCanonicalUuid() {
        assertThrows(IllegalArgumentException.class,()->CollateralAllocationRequestPolicy.key("not-a-uuid"));
        assertEquals("68b8fcb4-89eb-40d4-a326-d77fe2a500a1",
                CollateralAllocationRequestPolicy.key("68b8fcb4-89eb-40d4-a326-d77fe2a500a1"));
    }
}
