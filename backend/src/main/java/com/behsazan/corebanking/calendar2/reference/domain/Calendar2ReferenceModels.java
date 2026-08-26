package com.behsazan.corebanking.calendar2.reference.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Calendar2ReferenceModels {
    private Calendar2ReferenceModels() {}

    public enum FieldType { TEXT, NUMBER, DATE, TIMESTAMP, BOOLEAN, SELECT, LOOKUP }

    public record SelectOption(Object value, String label) {}

    public record FieldDescriptor(
            String apiName,
            String columnName,
            String label,
            FieldType type,
            boolean required,
            boolean key,
            boolean readOnly,
            boolean grid,
            boolean searchable,
            Integer maxLength,
            Object defaultValue,
            String lookupResource,
            List<SelectOption> options
    ) {
        public FieldDescriptor {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

    public record TableDescriptor(
            String resource,
            String group,
            String title,
            String description,
            String icon,
            String schemaName,
            String tableName,
            boolean allowCreate,
            boolean allowUpdate,
            boolean allowDelete,
            boolean autoNumericPrimaryKey,
            String lookupCodeApiName,
            String lookupNameApiName,
            List<FieldDescriptor> fields
    ) {
        public TableDescriptor { fields = List.copyOf(fields); }
        public List<FieldDescriptor> keyFields() { return fields.stream().filter(FieldDescriptor::key).toList(); }
        public List<FieldDescriptor> gridFields() { return fields.stream().filter(FieldDescriptor::grid).toList(); }
        public List<FieldDescriptor> searchableFields() { return fields.stream().filter(FieldDescriptor::searchable).toList(); }
        public FieldDescriptor field(String apiName) {
            return fields.stream().filter(field -> field.apiName().equals(apiName)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown CAL2 field: " + apiName));
        }
    }

    public record CatalogItem(String resource, String title, String description, String icon, String tableName, boolean readOnly) {}

    public record CatalogGroup(String code, String title, String description, String icon, List<CatalogItem> tables) {
        public CatalogGroup { tables = List.copyOf(tables); }
    }

    public record CatalogResponse(String schemaName, int tableCount, List<CatalogGroup> groups) {
        public CatalogResponse { groups = List.copyOf(groups); }
    }

    public record RecordResponse(String key, Map<String, Object> values) {
        public RecordResponse { values = Collections.unmodifiableMap(new LinkedHashMap<>(values)); }
    }

    public record LookupOption(Object value, String code, String label) {}
}
