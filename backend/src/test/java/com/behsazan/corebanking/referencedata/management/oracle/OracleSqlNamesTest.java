package com.behsazan.corebanking.referencedata.management.oracle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OracleSqlNamesTest {
    @Test
    void acceptsRegistryStyleIdentifiers() {
        assertThat(OracleSqlNames.qualified("GEO", "RURAL_DISTRICTS")).isEqualTo("GEO.RURAL_DISTRICTS");
    }

    @Test
    void rejectsInjectedIdentifiers() {
        assertThatThrownBy(() -> OracleSqlNames.identifier("PROVINCES; DROP TABLE X"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
