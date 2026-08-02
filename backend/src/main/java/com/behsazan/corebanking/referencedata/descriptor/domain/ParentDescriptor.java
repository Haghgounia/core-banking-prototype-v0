package com.behsazan.corebanking.referencedata.descriptor.domain;

public record ParentDescriptor(
        String resource,
        String apiField,
        String columnName,
        String label
) {
}
