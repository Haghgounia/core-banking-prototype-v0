package com.behsazan.corebanking.databaseexport;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseExportRequestTest {
    @Test
    void normalizesSchemaAndPrefix() {
        var request = new DatabaseExportRequest(" dps ", " ref_ ", Path.of("out"));

        assertThat(request.schemaName()).isEqualTo("DPS");
        assertThat(request.tablePrefix()).isEqualTo("REF_");
        assertThat(request.outputRoot()).isAbsolute();
    }

    @Test
    void rejectsUnsafeSchemaName() {
        assertThatThrownBy(() -> new DatabaseExportRequest(
                "DPS; DROP USER X",
                "REF_",
                Path.of("out")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
