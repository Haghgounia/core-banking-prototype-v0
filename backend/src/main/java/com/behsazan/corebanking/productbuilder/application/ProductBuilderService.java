package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.CatalogResponse;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.PackageCatalogItem;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductWorkspace;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductVersionDefaults;
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
    private final PdlReferenceOptionService referenceOptionService;

    public ProductBuilderService(PdlProductBuilderRepository repository,
                                 ProductBuilderBusinessValidator businessValidator,
                                 PdlReferenceOptionService referenceOptionService) {
        this.repository = repository;
        this.businessValidator = businessValidator;
        this.referenceOptionService = referenceOptionService;
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
        return referenceOptionService.enrich(repository.descriptor(table));
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
        String resolvedActor = actorName(actor);
        Map<String, Object> prepared = prepareCreateValues(table, values, resolvedActor);
        referenceOptionService.validateChangedValues(descriptor(table), prepared, null);
        businessValidator.validate(table, prepared);
        long id = repository.insert(table, prepared, resolvedActor);
        return findById(table, id);
    }

    @Transactional
    public Map<String, Object> update(String table, long id, Map<String, Object> values, String actor) {
        String resolvedActor = actorName(actor);
        Map<String, Object> existing = findById(table, id);
        Map<String, Object> prepared = prepareUpdateValues(table, existing, values, resolvedActor);
        Map<String, Object> merged = new LinkedHashMap<>(existing);
        merged.putAll(prepared);
        referenceOptionService.validateChangedValues(descriptor(table), prepared, existing);
        businessValidator.validate(table, merged);
        if (!repository.update(table, id, prepared, resolvedActor)) {
            throw new ProductBuilderValidationException("PDL row not found: " + table + "/" + id);
        }
        return findById(table, id);
    }

    @Transactional
    public void delete(String table, long id, String actor) {
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

    public ProductVersionDefaults productVersionDefaults(long productId, String actor) {
        findById("PRODUCT", productId);
        return new ProductVersionDefaults(
                repository.previewNextProductVersionNo(productId),
                repository.currentDatabaseDate().toString(),
                repository.currentDatabaseDateTime().toString(),
                actorName(actor)
        );
    }

    private Map<String, Object> prepareCreateValues(String table, Map<String, Object> values, String actor) {
        Map<String, Object> prepared = new LinkedHashMap<>(values);
        if (!"PRODUCT_VERSION".equals(normalizeTable(table))) return prepared;

        long productId = number(prepared.get("PRODUCT_ID"));
        if (productId <= 0) throw new ProductBuilderValidationException("PRODUCT_ID برای ثبت نسخه محصول الزامی است.");
        prepared.remove("APPROVED_AT");
        prepared.remove("APPROVED_BY");
        prepared.put("VERSION_NO", repository.lockAndNextProductVersionNo(productId));
        applyApprovalAudit(prepared, null, actor);
        return prepared;
    }

    private Map<String, Object> prepareUpdateValues(String table, Map<String, Object> existing,
                                                     Map<String, Object> values, String actor) {
        Map<String, Object> prepared = new LinkedHashMap<>(values);
        if (!"PRODUCT_VERSION".equals(normalizeTable(table))) return prepared;

        prepared.remove("VERSION_NO");
        prepared.remove("APPROVED_AT");
        prepared.remove("APPROVED_BY");
        applyApprovalAudit(prepared, text(existing.get("VERSION_STATUS_CODE")), actor);
        return prepared;
    }

    private void applyApprovalAudit(Map<String, Object> values, String previousStatus, String actor) {
        String requestedStatus = values.containsKey("VERSION_STATUS_CODE")
                ? text(values.get("VERSION_STATUS_CODE")) : previousStatus;
        if ("APPROVED".equals(requestedStatus) && !"APPROVED".equals(previousStatus)) {
            values.put("APPROVED_AT", repository.currentDatabaseDateTime());
            values.put("APPROVED_BY", actor);
        } else if ("DRAFT".equals(requestedStatus) && "APPROVED".equals(previousStatus)) {
            values.put("APPROVED_AT", null);
            values.put("APPROVED_BY", null);
        }
    }

    private static String normalizeTable(String table) {
        return table == null ? "" : table.trim().toUpperCase();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase();
    }

    private static long number(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString().trim()); }
        catch (NumberFormatException ex) { return 0; }
    }

    private static String actorName(String actor) {
        return actor == null || actor.isBlank() ? "prototype-ui" : actor.trim();
    }
}
