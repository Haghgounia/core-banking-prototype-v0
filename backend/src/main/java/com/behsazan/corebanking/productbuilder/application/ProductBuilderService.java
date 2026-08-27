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

    public ProductBuilderService(PdlProductBuilderRepository repository) {
        this.repository = repository;
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
        long id = repository.insert(table, values, actorName(actor));
        return findById(table, id);
    }

    @Transactional
    public Map<String, Object> update(String table, long id, Map<String, Object> values, String actor) {
        if (!repository.update(table, id, values, actorName(actor))) {
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

    private static String actorName(String actor) {
        return actor == null || actor.isBlank() ? "prototype-ui" : actor.trim();
    }
}
