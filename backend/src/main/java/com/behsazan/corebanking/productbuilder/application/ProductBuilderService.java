package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.CatalogResponse;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.PackageCatalogItem;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductWorkspace;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableCatalogItem;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableDescriptor;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductBuilderService {
    private final PdlProductBuilderRepository repository;
    private final ProductBuilderBusinessValidator businessValidator;
    private final ProductGovernanceWriteGuard governanceWriteGuard;

    public ProductBuilderService(PdlProductBuilderRepository repository,
                                 ProductBuilderBusinessValidator businessValidator,
                                 ProductGovernanceWriteGuard governanceWriteGuard) {
        this.repository = repository;
        this.businessValidator = businessValidator;
        this.governanceWriteGuard = governanceWriteGuard;
    }

    public CatalogResponse catalog() {
        Map<String, List<TableCatalogItem>> grouped = new LinkedHashMap<>();
        Map<String, String> packageTitles = new LinkedHashMap<>();
        long totalRows = 0;
        for (PdlCatalog.Entry entry : PdlCatalog.entries()) {
            long count = repository.count(entry.tableName());
            totalRows += count;
            grouped.computeIfAbsent(entry.packageCode(), k -> new ArrayList<>())
                    .add(new TableCatalogItem(entry.tableName(), entry.title(), entry.packageCode(), entry.packageTitle(), count));
            packageTitles.put(entry.packageCode(), entry.packageTitle());
        }
        List<PackageCatalogItem> packages = grouped.entrySet().stream().map(group -> {
            long rows = group.getValue().stream().mapToLong(TableCatalogItem::rowCount).sum();
            return new PackageCatalogItem(group.getKey(), packageTitles.get(group.getKey()), rows, List.copyOf(group.getValue()));
        }).toList();
        return new CatalogResponse(repository.schemaName(), totalRows, packages);
    }

    public TableDescriptor descriptor(String table) {
        return repository.descriptor(table);
    }

    public TablePage search(String table, String text, int page, int size, String filterColumn, String filterValue) {
        return repository.search(table, text, page, size, filterColumn, filterValue);
    }

    public Map<String, Object> findById(String table, long id) {
        return repository.findById(table, id)
                .orElseThrow(() -> new ProductBuilderValidationException("PDL row not found: " + table + "/" + id));
    }

    @Transactional
    public Map<String, Object> create(String table, Map<String, Object> values, String actor) {
        guardGovernedFieldsOnCreate(table, values);
        governanceWriteGuard.assertCreateAllowed(table, values);
        businessValidator.validate(table, values);
        long id = repository.insert(table, values, actorName(actor));
        return findById(table, id);
    }

    @Transactional
    public Map<String, Object> update(String table, long id, Map<String, Object> values, String actor) {
        guardGovernedFieldsOnUpdate(table, values);
        Map<String, Object> existing = new LinkedHashMap<>(findById(table, id));
        governanceWriteGuard.assertUpdateAllowed(table, existing, values);
        Map<String, Object> merged = new LinkedHashMap<>(existing);
        merged.putAll(values);
        businessValidator.validate(table, merged);
        if (!repository.update(table, id, values, actorName(actor))) {
            throw new ProductBuilderValidationException("PDL row not found: " + table + "/" + id);
        }
        return findById(table, id);
    }

    @Transactional
    public void delete(String table, long id, String actor) {
        Map<String, Object> existing = findById(table, id);
        governanceWriteGuard.assertDeleteAllowed(table, existing);
        if (!repository.delete(table, id, actorName(actor))) {
            throw new ProductBuilderValidationException("PDL row not found: " + table + "/" + id);
        }
    }

    public List<SelectOption> lookup(String table, String column, String text, int limit) {
        return repository.lookup(table, column, text, limit);
    }

    public ProductWorkspace productWorkspace(long productId) {
        Map<String, Object> product = findById("PRODUCT", productId);
        TablePage versionPage = repository.search("PRODUCT_VERSION", null, 0, 200, "PRODUCT_ID", String.valueOf(productId));
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("PRODUCT_VERSION", versionPage.totalElements());
        counts.put("PRODUCT_LEGACY_MAPPING", repository.search("PRODUCT_LEGACY_MAPPING", null, 0, 1, "PRODUCT_ID", String.valueOf(productId)).totalElements());
        return new ProductWorkspace(product, versionPage.items(), counts);
    }

    private static void guardGovernedFieldsOnCreate(String table, Map<String, Object> values) {
        String normalized = normalizeTable(table);
        if ("PRODUCT".equals(normalized)) {
            requireInitialValue(values, "PRODUCT_STATUS_CODE", "DRAFT");
        } else if ("PRODUCT_VERSION".equals(normalized)) {
            requireInitialValue(values, "VERSION_STATUS_CODE", "DRAFT");
            requireFalseOrEmpty(values, "IS_CURRENT");
            requireEmpty(values, "APPROVED_AT");
            requireEmpty(values, "APPROVED_BY");
        } else if ("PRODUCT_VERSION_MODULE".equals(normalized)) {
            requireInitialValue(values, "VALIDATION_STATUS_CODE", "NOT_VALIDATED");
        }
    }

    private static void guardGovernedFieldsOnUpdate(String table, Map<String, Object> values) {
        String normalized = normalizeTable(table);
        if ("PRODUCT".equals(normalized) && values.containsKey("PRODUCT_STATUS_CODE")) {
            governedField("PRODUCT_STATUS_CODE", "Validate/Approve/Publish");
        }
        if ("PRODUCT_VERSION".equals(normalized)) {
            for (String field : List.of("VERSION_STATUS_CODE", "IS_CURRENT", "APPROVED_AT", "APPROVED_BY")) {
                if (values.containsKey(field)) governedField(field, "Validate/Approve/Publish/Return-to-Draft");
            }
        }
        if ("PRODUCT_VERSION_MODULE".equals(normalized) && values.containsKey("VALIDATION_STATUS_CODE")) {
            String value = textValue(values.get("VALIDATION_STATUS_CODE"));
            if (!value.isBlank() && !"NOT_VALIDATED".equals(value)) {
                governedField("VALIDATION_STATUS_CODE", "Validate");
            }
        }
    }

    private static void requireInitialValue(Map<String, Object> values, String field, String expected) {
        if (!values.containsKey(field)) return;
        String actual = textValue(values.get(field));
        if (!actual.isBlank() && !expected.equals(actual)) governedField(field, "governed lifecycle action");
    }

    private static void requireFalseOrEmpty(Map<String, Object> values, String field) {
        if (!values.containsKey(field)) return;
        Object value = values.get(field);
        if (value == null) return;
        String text = value.toString().trim();
        if (!(text.isBlank() || "0".equals(text) || "FALSE".equalsIgnoreCase(text) || "N".equalsIgnoreCase(text))) {
            governedField(field, "Publish");
        }
    }

    private static void requireEmpty(Map<String, Object> values, String field) {
        if (!values.containsKey(field)) return;
        Object value = values.get(field);
        if (value != null && !value.toString().isBlank()) governedField(field, "Approve");
    }

    private static String normalizeTable(String table) {
        return table == null ? "" : table.trim().toUpperCase();
    }

    private static String textValue(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase();
    }

    private static void governedField(String field, String action) {
        throw new ProductBuilderValidationException(field + " یک فیلد حاکمیتی است و فقط از مسیر " + action + " قابل تغییر است.");
    }

    private static String actorName(String actor) {
        return actor == null || actor.isBlank() ? "prototype-ui" : actor.trim();
    }
}
