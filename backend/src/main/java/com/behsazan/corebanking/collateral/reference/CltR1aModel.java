package com.behsazan.corebanking.collateral.reference;

import java.util.List;

/** Physical descriptors derived from the 17 Reference Data tables in the CLT EA/Oracle baseline. */
public final class CltR1aModel {
    private CltR1aModel() { }

    public record Column(String name, String label, String dataType, boolean required, boolean primaryKey) { }

    public record Table(String table, String title, String description, String category,
                        String primaryKey, String codeColumn, String nameColumn,
                        List<Column> columns, int seedCount) { }

    public record Catalog(String source, String schema, String phase, int totalTables,
                          int technicalSeedPreviewCount, List<Table> tables) { }

    public record Rows(List<java.util.Map<String, Object>> items, long totalElements,
                       int page, int size, String source) { }

    public record Readiness(boolean ready, int expectedTables, int visibleTables,
                            List<String> missingTables, String source) { }
}
