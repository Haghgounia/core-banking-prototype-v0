package com.behsazan.corebanking.referencedata.descriptor.domain;

import java.util.List;

public record ReferenceFieldDescriptor(
        String apiName,
        String columnName,
        String label,
        FieldType type,
        boolean required,
        boolean readOnly,
        boolean grid,
        boolean searchable,
        Integer maxLength,
        Object defaultValue,
        String lookupResource,
        List<SelectOption> options
) {
    public ReferenceFieldDescriptor {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public boolean writable() {
        return !readOnly;
    }
}
