package com.behsazan.corebanking.collateral.localuat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/** Regression for R6G startup CLT_REFERENCE_READ_CONFIG_DENIED. */
class CltLocalUatEnvironmentBootstrapTest {
    private final CltLocalUatConfiguration configuration = new CltLocalUatConfiguration();

    @Test void launcherFlagAndBranchAreTheSingleSourceOfTruth() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("CLT_LOCAL_UAT_READONLY_ENABLED", "true")
                .withProperty("CLT_LOCAL_UAT_BRANCH_CODE", "1")
                .withProperty("core-banking.collateral.local-uat.enabled", "false")
                .withProperty("core-banking.collateral.local-uat.branch-code", "IGNORED");
        CltLocalUatProperties local = configuration.cltLocalUatProperties(env);
        assertTrue(local.enabled());
        assertEquals("1", local.branchCode());
    }

    @Test void disabledByDefaultAndWithExplicitFalse() {
        assertFalse(configuration.cltLocalUatProperties(new MockEnvironment()).enabled());
        assertFalse(configuration.cltLocalUatProperties(new MockEnvironment()
                .withProperty("CLT_LOCAL_UAT_READONLY_ENABLED", "false")
                .withProperty("CLT_LOCAL_UAT_BRANCH_CODE", "100")).enabled());
    }

    @Test void invalidModeFlagIsRejectedRatherThanCoerced() {
        assertThrows(IllegalStateException.class, () -> configuration.cltLocalUatProperties(
                new MockEnvironment().withProperty("CLT_LOCAL_UAT_READONLY_ENABLED", "yes")));
    }

    @Test void branchIsNeverTakenFromARequestOrImplicitDefault() {
        CltLocalUatProperties local = configuration.cltLocalUatProperties(new MockEnvironment()
                .withProperty("CLT_LOCAL_UAT_READONLY_ENABLED", "true"));
        assertNull(local.branchCode());
        assertFalse(local.enabled() && local.branchCode() != null);
    }
}
