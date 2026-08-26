package com.behsazan.corebanking.calendar2.datasetimport.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Calendar2DatasetImportModels {
    private Calendar2DatasetImportModels() {}

    public record DatasetImportResult(
            String schemaName,
            String packageFileName,
            Map<String, Long> tableRows,
            long totalRows,
            long elapsedMillis
    ) {
        public DatasetImportResult {
            tableRows = Collections.unmodifiableMap(new LinkedHashMap<>(tableRows));
        }
    }
}
