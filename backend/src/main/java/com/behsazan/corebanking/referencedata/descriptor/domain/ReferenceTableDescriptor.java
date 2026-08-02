package com.behsazan.corebanking.referencedata.descriptor.domain;

import java.util.List;
import java.util.Optional;

public record ReferenceTableDescriptor(
        String resource,
        String category,
        String title,
        String icon,
        String schemaName,
        String tableName,
        String sequenceName,
        String idApiName,
        String idColumnName,
        String codeApiName,
        String nameApiName,
        ParentDescriptor parent,
        List<ReferenceFieldDescriptor> fields
) {
    public ReferenceTableDescriptor {
        fields = List.copyOf(fields);
    }

    public ReferenceFieldDescriptor field(String apiName) {
        return fields.stream()
                .filter(field -> field.apiName().equals(apiName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown field: " + apiName));
    }

    public Optional<ReferenceFieldDescriptor> optionalField(String apiName) {
        return fields.stream().filter(field -> field.apiName().equals(apiName)).findFirst();
    }

    public List<ReferenceFieldDescriptor> editableFields() {
        return fields.stream().filter(ReferenceFieldDescriptor::writable).toList();
    }

    public List<ReferenceFieldDescriptor> gridFields() {
        return fields.stream().filter(ReferenceFieldDescriptor::grid).toList();
    }

    public List<ReferenceFieldDescriptor> searchableFields() {
        return fields.stream().filter(ReferenceFieldDescriptor::searchable).toList();
    }
}
