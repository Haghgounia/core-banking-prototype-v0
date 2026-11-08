package com.behsazan.corebanking.collateral.introduction;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

/** No new Oracle credentials or Hikari pool in the local Unified R2 draft writer. */
class CollateralIntroductionSharedDatasourceTest {
    @Test void sharedWriterNeverClosesOrReconfiguresAppDataSource() {
        DataSource appDataSource = mock(DataSource.class);
        try (var writer = new CollateralIntroductionWriterPool(appDataSource)) {
            assertSame(appDataSource, writer.jdbc().getDataSource());
        }
        verifyNoInteractions(appDataSource);
    }
}
