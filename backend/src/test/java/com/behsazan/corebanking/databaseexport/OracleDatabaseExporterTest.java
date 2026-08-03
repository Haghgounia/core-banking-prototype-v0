package com.behsazan.corebanking.databaseexport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OracleDatabaseExporterTest {
    @Test
    void escapesQuotesWithoutChangingPersianTextOrAmpersand() {
        assertThat(OracleDatabaseExporter.sqlString("O'Reilly & تهران"))
                .isEqualTo("'O''Reilly & تهران'");
    }
}
