package com.behsazan.corebanking.productbuilder.web;

import com.behsazan.corebanking.productbuilder.application.ProductBuilderService;
import com.behsazan.corebanking.productbuilder.application.ProductRuleGovernanceService;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.CatalogResponse;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.EligibilityRuleSaveRequest;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.EligibilityRuleSaveResponse;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductWorkspace;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductVersionDefaults;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableDescriptor;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/product-builder")
public class ProductBuilderController {
    private final ProductBuilderService service;
    private final ProductRuleGovernanceService governance;

    public ProductBuilderController(ProductBuilderService service, ProductRuleGovernanceService governance) {
        this.service = service;
        this.governance = governance;
    }

    @GetMapping("/versions/{versionId}/rule-governance")
    ProductRuleGovernanceService.GovernanceReport ruleGovernance(@PathVariable long versionId) {
        return governance.report(versionId);
    }

    @GetMapping("/loan-product-families")
    List<SelectOption> loanProductFamilies() {
        return service.loanProductFamilies();
    }

    @GetMapping("/loan-code-types")
    List<Map<String, Object>> loanCodeTypes() {
        return service.activeLoanTypesForCode();
    }

    @GetMapping("/loan-eligibility-extensions/{id}/employment-types")
    List<String> loanEligibilityEmploymentTypes(@PathVariable long id) {
        return service.loanEligibilityEmploymentTypes(id);
    }

    @PostMapping("/loan-eligibility-extensions/employment-types")
    Map<String, Object> saveLoanEligibilityEmploymentTypes(
            @RequestBody Map<String, Object> body,
            @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        if (!(body.get("values") instanceof Map<?, ?> raw) || !(body.get("employmentTypes") instanceof List<?> types))
            throw new com.behsazan.corebanking.productbuilder.application.ProductBuilderValidationException(
                    "اطلاعات افزونه اهلیت و انواع اشتغال الزامی است.");
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        raw.forEach((key, value) -> { if (key instanceof String k) values.put(k, value); });
        Long id = body.get("id") == null ? null : ((Number) body.get("id")).longValue();
        return service.saveLoanEligibilityEmployment(id, values, types, actor);
    }

    /** Explicit stage-six command; never issue a product code from generic CRUD. */
    @PostMapping("/products/{productId}/finalize-loan")
    Map<String, Object> finalizeLoanProduct(
            @PathVariable long productId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        Object requestedVersion = request.get("versionId");
        if (requestedVersion == null)
            throw new com.behsazan.corebanking.productbuilder.application.ProductBuilderValidationException(
                    "نسخه مصوب برای صدور کد محصول ضروری است.");
        long versionId;
        try { versionId = new java.math.BigDecimal(String.valueOf(requestedVersion)).longValueExact(); }
        catch (RuntimeException ex) {
            throw new com.behsazan.corebanking.productbuilder.application.ProductBuilderValidationException(
                    "شناسه نسخه برای نهایی‌سازی معتبر نیست.");
        }
        if (versionId <= 0)
            throw new com.behsazan.corebanking.productbuilder.application.ProductBuilderValidationException(
                    "شناسه نسخه برای نهایی‌سازی معتبر نیست.");
        return service.finalizeLoanProduct(productId, versionId, actor);
    }

    @GetMapping("/catalog")
    CatalogResponse catalog() {
        return service.catalog();
    }

    @GetMapping("/products/{productId}/workspace")
    ProductWorkspace productWorkspace(@PathVariable long productId) {
        return service.productWorkspace(productId);
    }

    @GetMapping("/products/{productId}/versions/new-defaults")
    ProductVersionDefaults productVersionDefaults(
            @PathVariable long productId,
            @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        return service.productVersionDefaults(productId, actor);
    }

    @GetMapping("/eligibility-rules/{id}")
    EligibilityRuleSaveResponse eligibilityRule(@PathVariable long id) {
        return service.eligibilityRule(id);
    }

    @PostMapping("/eligibility-rules")
    EligibilityRuleSaveResponse createEligibilityRule(
            @RequestBody EligibilityRuleSaveRequest request,
            @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        return service.saveEligibilityRule(null, request, actor);
    }

    @PutMapping("/eligibility-rules/{id}")
    EligibilityRuleSaveResponse updateEligibilityRule(
            @PathVariable long id,
            @RequestBody EligibilityRuleSaveRequest request,
            @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        return service.saveEligibilityRule(id, request, actor);
    }

    @GetMapping("/tables/{table}/descriptor")
    TableDescriptor descriptor(@PathVariable String table) {
        return service.descriptor(table);
    }

    @GetMapping("/tables/{table}/rows")
    TablePage rows(@PathVariable String table,
                   @RequestParam(required = false) String text,
                   @RequestParam(defaultValue = "0") int page,
                   @RequestParam(defaultValue = "25") int size,
                   @RequestParam(required = false) String filterColumn,
                   @RequestParam(required = false) String filterValue) {
        return service.search(table, text, page, size, filterColumn, filterValue);
    }

    @GetMapping("/tables/{table}/rows/{id}")
    Map<String, Object> row(@PathVariable String table, @PathVariable long id) {
        return service.findById(table, id);
    }

    @GetMapping("/tables/{table}/lookups/{column}")
    List<SelectOption> lookup(@PathVariable String table,
                              @PathVariable String column,
                              @RequestParam(required = false) String text,
                              @RequestParam(defaultValue = "200") int limit) {
        return service.lookup(table, column, text, limit);
    }

    /** Persist one normalized PDL scope per selected organization, atomically. */
    @PostMapping("/org-scopes/bulk")
    List<Map<String, Object>> saveOrgScopes(
            @RequestBody Map<String, Object> body,
            @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        Object raw = body.get("orgUnitIds");
        if (!(raw instanceof List<?> ids))
            throw new com.behsazan.corebanking.productbuilder.application.ProductBuilderValidationException(
                    "فهرست واحدهای سازمانی معتبر ارسال نشده است.");
        if (!(body.get("values") instanceof Map<?, ?> fields))
            throw new com.behsazan.corebanking.productbuilder.application.ProductBuilderValidationException(
                    "اطلاعات محدوده سازمانی ارسال نشده است.");
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        fields.forEach((key, value) -> { if (key instanceof String name) values.put(name, value); });
        Long editingId = body.get("existingId") == null ? null : ((Number) body.get("existingId")).longValue();
        return service.saveOrgScopes(values, ids, editingId, actor);
    }

    @PostMapping("/tables/{table}/rows")
    Map<String, Object> create(@PathVariable String table,
                               @RequestBody Map<String, Object> values,
                               @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        return service.create(table, values, actor);
    }

    @PutMapping("/tables/{table}/rows/{id}")
    Map<String, Object> update(@PathVariable String table,
                               @PathVariable long id,
                               @RequestBody Map<String, Object> values,
                               @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        return service.update(table, id, values, actor);
    }

    @DeleteMapping("/tables/{table}/rows/{id}")
    ResponseEntity<Void> delete(@PathVariable String table,
                                @PathVariable long id,
                                @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        service.delete(table, id, actor);
        return ResponseEntity.noContent().build();
    }
}
