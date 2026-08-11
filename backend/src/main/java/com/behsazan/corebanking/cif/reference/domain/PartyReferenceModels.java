package com.behsazan.corebanking.cif.reference.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public final class PartyReferenceModels {
    private PartyReferenceModels() {
    }

    public record Model(
            List<PackageDefinition> packages,
            Map<String, TableDefinition> tables,
            Map<String, RelationDefinition> relations,
            Map<String, Object> sourceStats
    ) {
    }

    public record PackageDefinition(
            String name,
            String title,
            String icon,
            List<String> tables
    ) {
    }

    public record TableDefinition(
            String name,
            String title,
            @JsonProperty("package") String packageName,
            String packageFa,
            String documentation,
            List<ColumnDefinition> columns,
            List<String> pk,
            List<String> checks,
            List<Map<String, Object>> seedRows,
            RelationDefinition relation
    ) {
        public ColumnDefinition requireColumn(String columnName) {
            return columns.stream()
                    .filter(column -> column.name().equals(columnName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown column " + columnName + " for " + name));
        }

        public boolean hasColumn(String columnName) {
            return columns.stream().anyMatch(column -> column.name().equals(columnName));
        }
    }

    public record ColumnDefinition(
            String name,
            String label,
            String type,
            String length,
            String precision,
            String scale,
            boolean required,
            @JsonProperty("default") String defaultValue,
            String description
    ) {
        public int maxLength() {
            if (!"VARCHAR2".equals(type) || length == null || length.isBlank()) {
                return 0;
            }
            return Integer.parseInt(length);
        }
    }

    public record RelationDefinition(
            String field,
            String target,
            String label
    ) {
    }

    public record CatalogResponse(
            List<PackageCatalog> packages,
            int tableCount
    ) {
    }

    public record PackageCatalog(
            String name,
            String title,
            String icon,
            List<TableCatalogItem> tables
    ) {
    }

    public record TableCatalogItem(
            String resource,
            String tableName,
            String title,
            String packageName,
            String packageFa,
            String icon,
            List<String> primaryKey,
            int fieldCount
    ) {
    }

    public record TableDescriptor(
            String resource,
            String schemaName,
            String tableName,
            String title,
            String packageName,
            String packageFa,
            String documentation,
            List<String> primaryKey,
            List<String> checks,
            RelationDescriptor relation,
            List<ColumnDescriptor> columns
    ) {
    }

    public record RelationDescriptor(
            String field,
            String targetResource,
            String targetTable,
            String label
    ) {
    }

    public record ColumnDescriptor(
            String name,
            String label,
            String type,
            Integer maxLength,
            boolean required,
            Object defaultValue,
            boolean keyPart,
            boolean readOnlyOnEdit,
            boolean grid,
            boolean searchable,
            String lookupResource,
            String description
    ) {
    }

    public record RowResponse(
            String key,
            Map<String, Object> values
    ) {
    }

    public record RecordResponse(
            String key,
            Map<String, Object> values
    ) {
    }

    public record LookupOption(
            String value,
            String code,
            String label
    ) {
    }
}
