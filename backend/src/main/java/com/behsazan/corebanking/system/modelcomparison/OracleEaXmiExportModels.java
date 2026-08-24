package com.behsazan.corebanking.system.modelcomparison;

import java.time.Instant;
import java.util.List;

public final class OracleEaXmiExportModels {
    private OracleEaXmiExportModels() {
    }

    public record ExportPreview(
            String schema,
            String tablePattern,
            int tableCount,
            int externalReferenceTableCount,
            int columnCount,
            int primaryKeyCount,
            int uniqueConstraintCount,
            int foreignKeyCount,
            int indexCount,
            int checkConstraintCount,
            Instant inspectedAt,
            List<String> sampleTables,
            List<String> warnings
    ) {
    }
}
