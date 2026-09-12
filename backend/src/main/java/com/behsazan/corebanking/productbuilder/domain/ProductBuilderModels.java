package com.behsazan.corebanking.productbuilder.domain;

import java.util.List;
import java.util.Map;

public final class ProductBuilderModels {
    private ProductBuilderModels() {}

    public record TableCatalogItem(
            String tableName,
            String title,
            String packageCode,
            String packageTitle,
            long rowCount
    ) {}

    public record PackageCatalogItem(
            String code,
            String title,
            long rowCount,
            List<TableCatalogItem> tables
    ) {}

    public record CatalogResponse(
            String schemaName,
            long totalRows,
            List<PackageCatalogItem> packages
    ) {}

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
            List<SelectOption> options
    ) {}

    public record TableDescriptor(
            String schemaName,
            String tableName,
            String title,
            String packageCode,
            String packageTitle,
            String primaryKeyColumn,
            List<ColumnDescriptor> columns
    ) {}

    public record SelectOption(Object value, String code, String label) {}

    public record TablePage(
            List<Map<String, Object>> items,
            long totalElements,
            int page,
            int size
    ) {}


    public record ProductReadinessItem(
            String code,
            String label,
            boolean ok,
            String detail
    ) {}

    public record ProductModuleValidation(
            String moduleCode,
            String label,
            String domain,
            boolean required,
            boolean enabled,
            boolean configured,
            boolean dataValid,
            boolean validationRecorded,
            String configurationStatus,
            String validationStatus,
            List<String> missingTables,
            Map<String, Long> ruleCounts
    ) {}

    public record Product360(
            Map<String, Object> product,
            Map<String, Object> version,
            List<Map<String, Object>> modules,
            Map<String, Long> ruleCounts,
            List<ProductModuleValidation> moduleValidations,
            List<ProductReadinessItem> readiness,
            int readinessScore,
            boolean validationPassed,
            boolean canApprove,
            boolean canPublish,
            String lifecycleStatus
    ) {}

    public record ProductWorkspace(
            Map<String, Object> product,
            List<Map<String, Object>> versions,
            Map<String, Long> relatedCounts
    ) {}
}
