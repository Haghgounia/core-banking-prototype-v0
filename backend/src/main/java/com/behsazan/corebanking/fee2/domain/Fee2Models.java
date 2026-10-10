package com.behsazan.corebanking.fee2.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class Fee2Models {
    private Fee2Models() {}

    public record TableCatalogItem(String tableName, String title, String groupCode, String groupTitle,
                                   String description, long rowCount, boolean available, boolean editable) {}
    public record GroupCatalogItem(String code, String title, long rowCount, int availableTables,
                                   List<TableCatalogItem> tables) {}
    public record CatalogResponse(String schemaName, int tableCount, int availableTableCount,
                                  long totalRows, List<GroupCatalogItem> groups) {}
    public record SelectOption(Object value, String code, String label) {}
    public record ColumnDescriptor(String name, String label, String dataType, Integer length,
                                   Integer precision, Integer scale, boolean nullable, boolean primaryKey,
                                   boolean foreignKey, String parentTable, String parentColumn,
                                   boolean readOnly, String defaultValue, List<SelectOption> options) {}
    public record TableDescriptor(String schemaName, String tableName, String title, String description,
                                  String groupCode, String groupTitle, boolean editable,
                                  String primaryKeyColumn, List<ColumnDescriptor> columns) {}
    public record TablePage(List<Map<String, Object>> items, long totalElements, int page, int size) {}
    public record VersionTransitionRequest(String targetStatus, String comment) {}
    public record CalculationConfigRequest(
            Long rowVersion,
            String calculationType,
            String basisCode,
            String basisUnit,
            BigDecimal fixedAmount,
            BigDecimal rateValue,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String roundingMode,
            BigDecimal roundingQuantum,
            String periodPolicy,
            String dayBasis,
            Map<String,Object> operatorConfig
    ) {}

    public record StudioSummary(
            long feeDefinitions,
            long draftVersions,
            long readyVersions,
            long approvedVersions,
            long activeVersions,
            int calculationTypesCovered,
            long simulationRuns,
            long calculationLogs
    ) {}

    public record StudioCatalogItem(
            String feeId,
            String feeCode,
            String nameFa,
            String nameEn,
            String feeTypeCode,
            String categoryCode,
            String ownerUnit,
            String definitionStatus,
            String latestVersionId,
            Long latestVersionNo,
            String versionStatus,
            String calculationType,
            String currency,
            String effectiveFrom,
            String effectiveTo
    ) {}

    public record StudioCreateRequest(
            String scopeId,
            String feeCode,
            String nameFa,
            String nameEn,
            String feeTypeCode,
            String categoryCode,
            String ownerUnit,
            String descriptionFa,
            String calculationType,
            String currency,
            String basisCode,
            String basisUnit,
            BigDecimal fixedAmount,
            BigDecimal rateValue,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String roundingMode,
            BigDecimal roundingQuantum,
            String periodPolicy,
            String dayBasis,
            Map<String,Object> operatorConfig,
            String effectiveFrom,
            String bindingLevel,
            Integer priority,
            String serviceCode,
            String eventType,
            String operationCode,
            Boolean taxEnabled,
            String taxCode,
            String taxType,
            BigDecimal taxRate,
            String taxBasisType
    ) {}

    public record StudioCreateResponse(
            String definitionId,
            String versionId,
            String bindingId,
            String taxId
    ) {}
}
