package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.CatalogResponse;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.EligibilityRuleSaveRequest;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.EligibilityRuleSaveResponse;
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
import java.util.Set;

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
        if ("PRODUCT_ELIGIBILITY_CRITERION".equals(normalizeTable(table))) {
            referenceOptionService.validateEligibilityCriterion(prepared);
            validateGenderCriterionScope(prepared);
        }
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
        if ("PRODUCT_ELIGIBILITY_CRITERION".equals(normalizeTable(table))) {
            referenceOptionService.validateEligibilityCriterion(merged);
            validateGenderCriterionScope(merged);
        }
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

    public EligibilityRuleSaveResponse eligibilityRule(long id) {
        Map<String, Object> rule = findById("PRODUCT_ELIGIBILITY_RULE", id);
        TablePage page = repository.search("PRODUCT_ELIGIBILITY_CRITERION", null, 0, 500,
                "ELIGIBILITY_RULE_ID", String.valueOf(id));
        return new EligibilityRuleSaveResponse(rule, page.items());
    }

    @Transactional
    public EligibilityRuleSaveResponse saveEligibilityRule(Long id, EligibilityRuleSaveRequest request, String actor) {
        if (request == null || request.rule() == null) {
            throw new ProductBuilderValidationException("اطلاعات قاعده اهلیت الزامی است.");
        }
        String resolvedActor = actorName(actor);
        List<Map<String, Object>> requestedCriteria = request.criteria() == null ? List.of() : request.criteria();
        Map<String, Object> existing = id == null ? null : findById("PRODUCT_ELIGIBILITY_RULE", id);
        Map<String, Object> prepared = existing == null
                ? new LinkedHashMap<>(request.rule())
                : new LinkedHashMap<>(request.rule());
        applyEligibilityLegacyProjection(prepared, existing, requestedCriteria);

        long ruleId;
        if (existing == null) {
            referenceOptionService.validateChangedValues(descriptor("PRODUCT_ELIGIBILITY_RULE"), prepared, null);
            businessValidator.validate("PRODUCT_ELIGIBILITY_RULE", prepared);
            ruleId = repository.insert("PRODUCT_ELIGIBILITY_RULE", prepared, resolvedActor);
        } else {
            referenceOptionService.validateChangedValues(descriptor("PRODUCT_ELIGIBILITY_RULE"), prepared, existing);
            Map<String, Object> merged = new LinkedHashMap<>(existing);
            merged.putAll(prepared);
            businessValidator.validate("PRODUCT_ELIGIBILITY_RULE", merged);
            if (!repository.update("PRODUCT_ELIGIBILITY_RULE", id, prepared, resolvedActor)) {
                throw new ProductBuilderValidationException("قاعده اهلیت یافت نشد: " + id);
            }
            ruleId = id;
        }

        TablePage current = repository.search("PRODUCT_ELIGIBILITY_CRITERION", null, 0, 1000,
                "ELIGIBILITY_RULE_ID", String.valueOf(ruleId));
        for (Map<String, Object> row : current.items()) {
            long criterionId = number(row.get("ELIGIBILITY_CRITERION_ID"));
            if (criterionId > 0) repository.delete("PRODUCT_ELIGIBILITY_CRITERION", criterionId, resolvedActor);
        }

        List<Map<String, Object>> normalized = requestedCriteria.stream()
                .<Map<String, Object>>map(row -> new LinkedHashMap<>(row))
                .peek(row -> row.put("ELIGIBILITY_RULE_ID", ruleId))
                .sorted((left, right) -> Integer.compare(criterionSort(left), criterionSort(right)))
                .toList();

        for (Map<String, Object> criterion : normalized) {
            referenceOptionService.validateEligibilityCriterion(criterion);
            validateGenderCriterionScopeAgainstRequest(criterion, normalized);
            businessValidator.validate("PRODUCT_ELIGIBILITY_CRITERION", criterion);
            repository.insert("PRODUCT_ELIGIBILITY_CRITERION", criterion, resolvedActor);
        }
        return eligibilityRule(ruleId);
    }

    private void applyEligibilityLegacyProjection(Map<String, Object> values, Map<String, Object> existing,
                                                   List<Map<String, Object>> criteria) {
        TableDescriptor descriptor = descriptor("PRODUCT_ELIGIBILITY_RULE");
        Map<String, String> projectionColumns = Map.of(
                "CUSTOMER_TYPE", "CUSTOMER_TYPE_CODE",
                "CUSTOMER_SEGMENT", "CUSTOMER_SEGMENT_CODE",
                "CUSTOMER_STATUS", "CUSTOMER_STATUS_CODE",
                "GENDER", "GENDER_CODE",
                "RESIDENCY_STATUS", "RESIDENCY_STATUS_CODE",
                "NATIONALITY_SCOPE", "NATIONALITY_SCOPE_CODE"
        );
        for (Map.Entry<String, String> projection : projectionColumns.entrySet()) {
            var column = descriptor.columns().stream().filter(c -> projection.getValue().equals(c.name())).findFirst().orElse(null);
            if (column == null) continue;
            List<String> selected = criteria.stream()
                    .filter(row -> projection.getKey().equals(text(row.get("CRITERION_TYPE_CODE"))))
                    .filter(row -> "IN".equals(text(row.get("OPERATOR_CODE"))) || "EQ".equals(text(row.get("OPERATOR_CODE"))))
                    .map(row -> row.get("CRITERION_VALUE_CODE") == null ? "" : row.get("CRITERION_VALUE_CODE").toString().trim())
                    .filter(v -> !v.isBlank())
                    .toList();
            if (!selected.isEmpty()) {
                values.put(projection.getValue(), selected.get(0));
                continue;
            }
            if (column.nullable()) {
                values.put(projection.getValue(), null);
                continue;
            }
            if (existing != null && existing.get(projection.getValue()) != null) {
                values.put(projection.getValue(), existing.get(projection.getValue()));
                continue;
            }
            String unrestricted = column.options().stream()
                    .filter(o -> Set.of("ALL", "ANY", "3").contains(text(o.code())) || Set.of("ALL", "ANY", "3").contains(text(o.value())))
                    .map(o -> o.value().toString()).findFirst().orElse(null);
            if (unrestricted != null) {
                values.put(projection.getValue(), unrestricted);
            } else if (!column.options().isEmpty()) {
                values.put(projection.getValue(), column.options().get(0).value());
            }
        }

        descriptor.columns().stream()
                .filter(c -> Set.of("PRIORITY_NO", "RULE_PRIORITY", "PRIORITY").contains(c.name()))
                .findFirst()
                .ifPresent(column -> {
                    if (!values.containsKey(column.name()) || values.get(column.name()) == null || values.get(column.name()).toString().isBlank()) {
                        values.put(column.name(), 100);
                    }
                });
    }

    private void validateGenderCriterionScopeAgainstRequest(Map<String, Object> criterion, List<Map<String, Object>> allCriteria) {
        if (!"GENDER".equals(text(criterion.get("CRITERION_TYPE_CODE")))) return;
        List<String> customerTypes = allCriteria.stream()
                .filter(row -> "CUSTOMER_TYPE".equals(text(row.get("CRITERION_TYPE_CODE"))))
                .map(row -> text(row.get("CRITERION_VALUE_CODE")))
                .filter(v -> !v.isBlank())
                .distinct().toList();
        if (customerTypes.isEmpty() || customerTypes.stream().anyMatch(v -> !Set.of("PERSON", "INDIVIDUAL").contains(v))) {
            throw new ProductBuilderValidationException("معیار جنسیت فقط برای دامنه صرفاً «شخص حقیقی» قابل ثبت است.");
        }
    }

    private static int criterionSort(Map<String, Object> row) {
        return switch (text(row.get("CRITERION_TYPE_CODE"))) {
            case "CUSTOMER_TYPE" -> 10;
            case "CUSTOMER_SEGMENT" -> 20;
            case "CUSTOMER_STATUS" -> 30;
            case "GENDER" -> 40;
            case "RESIDENCY_STATUS" -> 50;
            case "NATIONALITY_SCOPE" -> 60;
            case "CUSTOMER_TENURE" -> 70;
            case "ACCOUNT_TENURE" -> 80;
            default -> 100;
        };
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

    private void validateGenderCriterionScope(Map<String, Object> criterion) {
        if (!"GENDER".equals(text(criterion.get("CRITERION_TYPE_CODE")))) return;
        long ruleId = number(criterion.get("ELIGIBILITY_RULE_ID"));
        if (ruleId <= 0) throw new ProductBuilderValidationException("قاعده اهلیت والد برای معیار جنسیت الزامی است.");
        TablePage criteria = repository.search("PRODUCT_ELIGIBILITY_CRITERION", null, 0, 500,
                "ELIGIBILITY_RULE_ID", String.valueOf(ruleId));
        List<String> customerTypes = criteria.items().stream()
                .filter(row -> "CUSTOMER_TYPE".equals(text(row.get("CRITERION_TYPE_CODE"))))
                .filter(row -> !row.containsKey("IS_ACTIVE") || number(row.get("IS_ACTIVE")) != 0)
                .map(row -> text(row.get("CRITERION_VALUE_CODE")))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (customerTypes.isEmpty() || customerTypes.stream().anyMatch(value -> !Set.of("PERSON", "INDIVIDUAL").contains(value))) {
            throw new ProductBuilderValidationException(
                    "معیار جنسیت فقط زمانی مجاز است که دامنه نوع مشتری صرفاً «شخص حقیقی» باشد."
            );
        }
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
