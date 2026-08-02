package com.behsazan.corebanking.referencedata.descriptor.application;

import com.behsazan.corebanking.referencedata.descriptor.domain.FieldType;
import com.behsazan.corebanking.referencedata.descriptor.domain.ParentDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceFieldDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.ReferenceTableDescriptor;
import com.behsazan.corebanking.referencedata.descriptor.domain.SelectOption;

import java.util.List;

public final class ReferenceDescriptorSupport {
    private ReferenceDescriptorSupport() {
    }

    public static ReferenceTableDescriptor descriptor(
            String resource,
            String category,
            String title,
            String icon,
            String schema,
            String table,
            String sequence,
            String idApi,
            String idColumn,
            String codeApi,
            String nameApi,
            ParentDescriptor parent,
            List<ReferenceFieldDescriptor> fields
    ) {
        return new ReferenceTableDescriptor(
                resource, category, title, icon, schema, table, sequence,
                idApi, idColumn, codeApi, nameApi, parent, fields
        );
    }

    public static ReferenceFieldDescriptor id(String api, String column, String label) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.NUMBER,
                false, true, true, false, null, null, null, List.of());
    }

    public static ReferenceFieldDescriptor text(
            String api,
            String column,
            String label,
            boolean required,
            boolean grid,
            boolean searchable,
            int maxLength
    ) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.TEXT,
                required, false, grid, searchable, maxLength, null, null, List.of());
    }

    public static ReferenceFieldDescriptor number(
            String api,
            String column,
            String label,
            boolean required,
            boolean grid,
            Object defaultValue
    ) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.NUMBER,
                required, false, grid, false, null, defaultValue, null, List.of());
    }

    public static ReferenceFieldDescriptor bool(
            String api,
            String column,
            String label,
            boolean required,
            boolean grid,
            boolean defaultValue
    ) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.BOOLEAN,
                required, false, grid, false, null, defaultValue, null, List.of());
    }

    public static ReferenceFieldDescriptor date(
            String api,
            String column,
            String label,
            boolean required,
            boolean grid
    ) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.DATE,
                required, false, grid, false, null, null, null, List.of());
    }

    public static ReferenceFieldDescriptor select(
            String api,
            String column,
            String label,
            boolean required,
            boolean grid,
            Object defaultValue,
            List<SelectOption> options
    ) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.SELECT,
                required, false, grid, false, null, defaultValue, null, options);
    }

    public static ReferenceFieldDescriptor lookup(
            String api,
            String column,
            String label,
            String resource,
            boolean required,
            boolean grid
    ) {
        return new ReferenceFieldDescriptor(api, column, label, FieldType.LOOKUP,
                required, false, grid, false, null, null, resource, List.of());
    }

    public static ReferenceFieldDescriptor audit(
            String api,
            String column,
            String label,
            FieldType type
    ) {
        return new ReferenceFieldDescriptor(api, column, label, type,
                false, true, false, false, null, null, null, List.of());
    }

    public static List<ReferenceFieldDescriptor> standardAudits() {
        return List.of(
                audit("createdBy", "CREATED_BY", "ایجادکننده", FieldType.NUMBER),
                audit("createdDate", "CREATED_DATE", "تاریخ ایجاد", FieldType.TIMESTAMP),
                audit("lastModifiedBy", "LAST_MODIFIED_BY", "ویرایش‌کننده", FieldType.NUMBER),
                audit("lastModifiedDate", "LAST_MODIFIED_DATE", "تاریخ ویرایش", FieldType.TIMESTAMP)
        );
    }

    public static List<ReferenceFieldDescriptor> employmentAudits() {
        return List.of(
                audit("createdBy", "CREATE_USER_ID", "ایجادکننده", FieldType.NUMBER),
                audit("createdDate", "CREATE_DATE", "تاریخ ایجاد", FieldType.TIMESTAMP),
                audit("lastModifiedBy", "LAST_MODIFIED_BY", "ویرایش‌کننده", FieldType.NUMBER),
                audit("lastModifiedDate", "LAST_MODIFIED_DATE", "تاریخ ویرایش", FieldType.TIMESTAMP)
        );
    }
}
