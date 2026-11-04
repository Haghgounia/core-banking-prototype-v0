package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.localuat.CltLocalUatProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Pin the exact R6G failure path: the local branch MUST not check missing JWT/reader credentials. */
class CollateralReferenceLocalReaderBootstrapTest {
    private static CollateralReferenceProperties unconfiguredJwtReader() {
        return new CollateralReferenceProperties(true, null, null, null, 3600,
                null, null, null, false, null, null, null);
    }

    @Test void localBranchSelectsApplicationDatasourceWithoutJwtOrSeparateDbCredentials() {
        DataSource applicationDatasource = mock(DataSource.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.address", "127.0.0.1")
                .withProperty("CLT_LOCAL_UAT_READONLY_ENABLED", "true");
        var pool = new CollateralReferenceConfiguration().collateralReferenceReaderPool(
                unconfiguredJwtReader(), new CltLocalUatProperties(true, "1"),
                new CollateralRegistryProperties(true), environment, applicationDatasource);
        assertNotNull(pool.jdbc());
        assertDoesNotThrow(pool::close); // borrowed application DataSource must never be closed here
    }

    @Test void missingLauncherFlagCannotEnableSyntheticLocalPrincipal() {
        DataSource applicationDatasource = mock(DataSource.class);
        MockEnvironment environment = new MockEnvironment().withProperty("server.address", "127.0.0.1");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new CollateralReferenceConfiguration().collateralReferenceReaderPool(
                        unconfiguredJwtReader(), new CltLocalUatProperties(true, "1"),
                        new CollateralRegistryProperties(true), environment, applicationDatasource));
        assertEquals("CLT_R6H_LOCAL_MODE_BINDING_MISMATCH", ex.getMessage());
    }

    @Test void enabledLauncherFlagCannotBeOverriddenByDisabledLocalObject() {
        DataSource applicationDatasource = mock(DataSource.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.address", "127.0.0.1")
                .withProperty("CLT_LOCAL_UAT_READONLY_ENABLED", "true");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new CollateralReferenceConfiguration().collateralReferenceReaderPool(
                        unconfiguredJwtReader(), new CltLocalUatProperties(false, "1"),
                        new CollateralRegistryProperties(true), environment, applicationDatasource));
        assertEquals("CLT_R6H_LOCAL_MODE_BINDING_MISMATCH", ex.getMessage());
    }

    @Test void nonLocalBranchPreservesOriginalFailClosedPolicy() {
        DataSource applicationDatasource = mock(DataSource.class);
        MockEnvironment environment = new MockEnvironment().withProperty("server.address", "127.0.0.1");
        assertThrows(IllegalStateException.class,
                () -> new CollateralReferenceConfiguration().collateralReferenceReaderPool(
                        unconfiguredJwtReader(), new CltLocalUatProperties(false, "1"),
                        new CollateralRegistryProperties(true), environment, applicationDatasource));
    }
}
