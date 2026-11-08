package com.behsazan.corebanking.collateral.registry;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CollateralRegistryPolicyTest {
    @Test void registrationIsExactDecimalString() {
        assertEquals("9999999999999999999", CollateralRegistryPolicy.id("9999999999999999999").toPlainString());
        for (String invalid : new String[]{"0", "01", "1.5", "-3", "10000000000000000000", "1 OR 1=1"}) {
            assertThrows(IllegalArgumentException.class, () -> CollateralRegistryPolicy.id(invalid));
        }
    }
    @Test void branchClaimIsStrict() {
        assertEquals("1010", CollateralRegistryPolicy.branch("1010"));
        for (String invalid : new String[]{"", " 1010", "a.b", "X'--", "123/4"}) {
            assertThrows(IllegalArgumentException.class, () -> CollateralRegistryPolicy.branch(invalid));
        }
    }
    @Test void groupCodeFilterAcceptsOnlyExactBusinessCode() {
        for (String valid : new String[]{"70", "44", "G_1", "ABCDE"}) {
            assertEquals(valid, CollateralRegistryPolicy.groupCode(valid));
        }
        for (String invalid : new String[]{"", " 70", "70 ", "ABCDEF", "%", "\u06f7\u06f0", "1' OR 1=1", "a/b"}) {
            assertThrows(IllegalArgumentException.class, () -> CollateralRegistryPolicy.groupCode(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> CollateralRegistryPolicy.groupCode(null));
    }
    @Test void lifecycleStatusIsStrictBusinessCode() {
        for (String valid : new String[]{"ACTIVE", "PENDING_APPROVAL", "RELEASED", "S-1"}) {
            assertEquals(valid, CollateralRegistryPolicy.statusCode(valid));
        }
        for (String invalid : new String[]{"", " ACTIVE", "A%", "a'b", "1 OR 1=1",
                "1234567890123456789012345678901"}) {
            assertThrows(IllegalArgumentException.class, () -> CollateralRegistryPolicy.statusCode(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> CollateralRegistryPolicy.statusCode(null));
    }
    @Test void boundedPagination() {
        CollateralRegistryPolicy.pagination(1000, 50);
        assertThrows(IllegalArgumentException.class, () -> CollateralRegistryPolicy.pagination(-1, 20));
        assertThrows(IllegalArgumentException.class, () -> CollateralRegistryPolicy.pagination(0, 51));
    }
}
