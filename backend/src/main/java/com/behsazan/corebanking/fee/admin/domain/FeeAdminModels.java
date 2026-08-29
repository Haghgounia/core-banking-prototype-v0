package com.behsazan.corebanking.fee.admin.domain;

import java.util.List;
import java.util.Map;

public final class FeeAdminModels {
    private FeeAdminModels() {}

    public record TableCatalogItem(
            String tableName,
            String title,
            String groupCode,
            String groupTitle,
            int baselineRows,
            long rowCount,
            boolean available,
            boolean editable
    ) {}

    public record GroupCatalogItem(
            String code,
            String title,
            long rowCount,
            int baselineRows,
            int availableTables,
            List<TableCatalogItem> tables
    ) {}

    public record CatalogResponse(
            String schemaName,
            int tableCount,
            int availableTableCount,
            long totalRows,
            int baselineRows,
            List<GroupCatalogItem> groups
    ) {}

    public record SelectOption(Object value, String code, String label) {}

    public record ColumnDescriptor(
            String name,
            String label,
            String dataType,
            Integer length,
            Integer precision,
            Integer scale,
            boolean nullable,
            boolean primaryKey,
            boolean foreignKey,
            String parentTable,
            String parentColumn,
            boolean readOnly,
            String defaultValue,
            String domainCode,
            List<SelectOption> options
    ) {}

    public record TableDescriptor(
            String schemaName,
            String tableName,
            String title,
            String groupCode,
            String groupTitle,
            boolean editable,
            int baselineRows,
            String primaryKeyColumn,
            List<ColumnDescriptor> columns
    ) {}

    public record TablePage(
            List<Map<String, Object>> items,
            long totalElements,
            int page,
            int size
    ) {}
}
