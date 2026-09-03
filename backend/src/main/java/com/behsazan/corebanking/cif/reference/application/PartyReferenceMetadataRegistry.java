package com.behsazan.corebanking.cif.reference.application;

import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.CatalogResponse;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.ColumnDefinition;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.ColumnDescriptor;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.Model;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.PackageCatalog;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.PackageDefinition;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.RelationDefinition;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.RelationDescriptor;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.TableCatalogItem;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.TableDefinition;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.TableDescriptor;
import com.behsazan.corebanking.shared.error.ReferenceNotFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PartyReferenceMetadataRegistry {
    private final Model model;
    private final Map<String, TableDefinition> byResource;
    private final Map<String, String> resourceByTable;
    private final String schemaName;

    public PartyReferenceMetadataRegistry(
            JsonMapper objectMapper,
            @Value("${core-banking.schemas.party-reference:CIF}") String schemaName
    ) {
        this.schemaName = schemaName;
        try (InputStream input = new ClassPathResource("cif/party-reference/party-reference-model.json").getInputStream()) {
            this.model = objectMapper.readValue(input, Model.class);
        } catch (IOException | JacksonException exception) {
            throw new IllegalStateException("Cannot load CIF party reference metadata", exception);
        }
        Map<String, TableDefinition> resources = new LinkedHashMap<>();
        Map<String, String> tables = new LinkedHashMap<>();
        model.tables().values().forEach(table -> {
            String resource = resourceFor(table.name());
            if (resources.put(resource, table) != null) {
                throw new IllegalStateException("Duplicate party reference resource: " + resource);
            }
            tables.put(table.name(), resource);
        });
        this.byResource = Map.copyOf(resources);
        this.resourceByTable = Map.copyOf(tables);
    }

    public TableDefinition require(String resource) {
        TableDefinition table = byResource.get(resource);
        if (table == null) {
            throw new ReferenceNotFoundException("فرم اطلاعات پایه Party با شناسه " + resource + " یافت نشد.");
        }
        return table;
    }

    public String schemaName() {
        return schemaName;
    }

    public String resourceForTable(String tableName) {
        String resource = resourceByTable.get(tableName);
        if (resource == null) {
            throw new ReferenceNotFoundException("جدول مرجع " + tableName + " در این فاز فعال نیست.");
        }
        return resource;
    }

    public CatalogResponse catalog() {
        List<PackageCatalog> packages = model.packages().stream()
                .map(this::packageCatalog)
                .toList();
        return new CatalogResponse(packages, byResource.size());
    }

    public TableDescriptor descriptor(String resource) {
        TableDefinition table = require(resource);
        RelationDescriptor relation = relationDescriptor(table.relation());
        List<ColumnDescriptor> columns = table.columns().stream()
                .map(column -> columnDescriptor(table, column))
                .toList();
        return new TableDescriptor(
                resource,
                schemaName,
                table.name(),
                table.title(),
                table.packageName(),
                table.packageFa(),
                table.documentation(),
                table.pk(),
                table.checks(),
                relation,
                columns
        );
    }

    private PackageCatalog packageCatalog(PackageDefinition pack) {
        List<TableCatalogItem> tables = pack.tables().stream()
                .map(model.tables()::get)
                .filter(java.util.Objects::nonNull)
                .map(table -> new TableCatalogItem(
                        resourceFor(table.name()),
                        table.name(),
                        table.title(),
                        table.packageName(),
                        table.packageFa(),
                        iconFor(table),
                        table.pk(),
                        table.columns().size()
                ))
                .toList();
        return new PackageCatalog(pack.name(), pack.title(), pack.icon(), tables);
    }

    private RelationDescriptor relationDescriptor(RelationDefinition relation) {
        if (relation == null) {
            return null;
        }
        return new RelationDescriptor(
                relation.field(),
                resourceForTable(relation.target()),
                relation.target(),
                relation.label()
        );
    }

    private ColumnDescriptor columnDescriptor(TableDefinition table, ColumnDefinition column) {
        boolean keyPart = table.pk().contains(column.name());
        boolean recordVersion = "RECORD_VERSION".equals(column.name());
        String lookupResource = null;
        if (table.relation() != null && table.relation().field().equals(column.name())) {
            lookupResource = resourceForTable(table.relation().target());
        } else if ("PARENT_CODE".equals(column.name())) {
            lookupResource = resourceFor(table.name());
        }
        boolean relationField = table.relation() != null && table.relation().field().equals(column.name());
        boolean grid = keyPart
                || relationField
                || "NAME_FA".equals(column.name())
                || "NAME_EN".equals(column.name())
                || "IS_ACTIVE".equals(column.name());
        return new ColumnDescriptor(
                column.name(),
                labelFor(table, column),
                column.type(),
                column.maxLength() == 0 ? null : column.maxLength(),
                column.required(),
                parseDefault(column),
                keyPart,
                keyPart || recordVersion,
                grid,
                keyPart || "NAME_FA".equals(column.name()) || "NAME_EN".equals(column.name()) || "DESCRIPTION_FA".equals(column.name()),
                lookupResource,
                column.description()
        );
    }

    private static Object parseDefault(ColumnDefinition column) {
        if (column.defaultValue() == null) {
            return null;
        }
        if ("NUMBER".equals(column.type())) {
            return new BigDecimal(column.defaultValue());
        }
        return column.defaultValue();
    }

    private static String labelFor(TableDefinition table, ColumnDefinition column) {
        if (table.relation() != null && table.relation().field().equals(column.name())) {
            return table.relation().label();
        }
        if ("PARENT_CODE".equals(column.name())) {
            return "کد والد در سلسله‌مراتب";
        }
        return column.label();
    }

    private static String iconFor(TableDefinition table) {
        String name = table.name();
        if (name.contains("GENDER") || name.contains("PARTY_TYPE")) return "badge";
        if (name.contains("RELIGION") || name.contains("DENOMINATION")) return "diversity_3";
        if (name.contains("DOCUMENT") || name.contains("IDENTIFIER")) return "fingerprint";
        if (name.contains("CONSENT")) return "verified_user";
        if (name.contains("ROLE") || name.contains("AUTHORITY")) return "manage_accounts";
        if (name.contains("CLASSIFICATION")) return "category";
        if (name.contains("STATUS")) return "rule";
        return "list_alt";
    }

    public static String resourceFor(String tableName) {
        return tableName.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
