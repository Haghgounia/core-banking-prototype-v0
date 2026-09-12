package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FIX98 write guard for published/approved product configuration.
 *
 * Generic PDL CRUD remains available for DRAFT configuration. Once a version
 * leaves DRAFT, its version row, modules and version-scoped rules are immutable
 * through generic CRUD. Governance actions bypass this guard intentionally by
 * writing through the repository inside ProductGovernanceService.
 */
@Component
public class ProductGovernanceWriteGuard {
    private static final Set<String> GOVERNED_PRODUCT_FIELDS = Set.of(
            "PRODUCT_CODE", "PRODUCT_CLASS_CODE", "BALANCE_NATURE_CODE",
            "PRODUCT_FAMILY_CODE", "DEFAULT_CURRENCY_CODE"
    );

    private static final Map<String, ParentLink> PARENT_LINKS = Map.ofEntries(
            Map.entry("PRODUCT_CHANNEL_OPERATION", new ParentLink("CHANNEL_RULE_ID", "PRODUCT_CHANNEL_RULE")),
            Map.entry("PRODUCT_PRICING_COMPONENT", new ParentLink("PRICING_RULE_ID", "PRODUCT_PRICING_RULE")),
            Map.entry("PRODUCT_RATE_TIER", new ParentLink("PRICING_COMPONENT_ID", "PRODUCT_PRICING_COMPONENT")),
            Map.entry("DEPOSIT_PRODUCT_ALLOWED_TERM", new ParentLink("TERM_RULE_ID", "DEPOSIT_PRODUCT_TERM_RULE")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_PRECHECK", new ParentLink("CLOSURE_RULE_ID", "DEPOSIT_PRODUCT_CLOSURE_RULE")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE", new ParentLink("CLOSURE_RULE_ID", "DEPOSIT_PRODUCT_CLOSURE_RULE")),
            Map.entry("DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE", new ParentLink("CLOSURE_RULE_ID", "DEPOSIT_PRODUCT_CLOSURE_RULE")),
            Map.entry("LOAN_ELIGIBILITY_EXTENSION", new ParentLink("ELIGIBILITY_RULE_ID", "PRODUCT_ELIGIBILITY_RULE")),
            Map.entry("CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE", new ParentLink("CORRESPONDENT_PRODUCT_PROFILE_ID", "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE"))
    );

    private final PdlProductBuilderRepository repository;

    public ProductGovernanceWriteGuard(PdlProductBuilderRepository repository) {
        this.repository = repository;
    }

    public void assertCreateAllowed(String table, Map<String, Object> values) {
        String normalized = normalize(table);
        if ("PRODUCT_VERSION".equals(normalized)) return; // a new version is DRAFT by lifecycle-field guard.
        Long versionId = resolveVersionId(normalized, values);
        if (versionId != null) assertDraft(versionId, normalized);
    }

    public void assertUpdateAllowed(String table, Map<String, Object> existing, Map<String, Object> changes) {
        String normalized = normalize(table);
        Map<String, Object> merged = new LinkedHashMap<>(existing);
        merged.putAll(changes);

        if ("PRODUCT".equals(normalized)) {
            assertProductIdentityMutable(existing, changes);
            return;
        }

        Long existingVersionId = resolveVersionId(normalized, existing);
        Long targetVersionId = resolveVersionId(normalized, merged);
        if (existingVersionId != null) assertDraft(existingVersionId, normalized);
        if (targetVersionId != null && !targetVersionId.equals(existingVersionId)) assertDraft(targetVersionId, normalized);
    }

    public void assertDeleteAllowed(String table, Map<String, Object> existing) {
        String normalized = normalize(table);
        Long versionId = resolveVersionId(normalized, existing);
        if (versionId != null) assertDraft(versionId, normalized);
    }

    private void assertProductIdentityMutable(Map<String, Object> existing, Map<String, Object> changes) {
        boolean criticalChange = GOVERNED_PRODUCT_FIELDS.stream().anyMatch(field ->
                changes.containsKey(field) && !same(existing.get(field), changes.get(field))
        );
        if (!criticalChange) return;
        long productId = number(existing.get("PRODUCT_ID"));
        if (productId <= 0) return;
        var versions = repository.search("PRODUCT_VERSION", null, 0, 200, "PRODUCT_ID", String.valueOf(productId));
        boolean governedVersionExists = versions.items().stream()
                .map(row -> text(row.get("VERSION_STATUS_CODE")))
                .anyMatch(status -> !status.isBlank() && !"DRAFT".equals(status));
        if (governedVersionExists) {
            throw new ProductBuilderValidationException(
                    "هویت حاکمیتی محصول پس از APPROVE/PUBLISH قابل تغییر نیست؛ برای تغییر Family/Class/Currency یک محصول یا چرخه نسخه مناسب ایجاد کنید."
            );
        }
    }

    private Long resolveVersionId(String table, Map<String, Object> row) {
        if (row == null || row.isEmpty()) return null;
        if ("PRODUCT_VERSION".equals(table)) {
            long id = number(row.get("PRODUCT_VERSION_ID"));
            return id > 0 ? id : null;
        }

        long direct = number(row.get("PRODUCT_VERSION_ID"));
        if (direct > 0) return direct;

        long source = number(row.get("SOURCE_PRODUCT_VERSION_ID"));
        if (source > 0 && "PRODUCT_RELATIONSHIP".equals(table)) return source;

        ParentLink link = PARENT_LINKS.get(table);
        if (link == null) return null;
        long parentId = number(row.get(link.foreignKey()));
        if (parentId <= 0) return null;
        Map<String, Object> parent = repository.findById(link.parentTable(), parentId)
                .orElseThrow(() -> new ProductBuilderValidationException(
                        "رکورد والد برای کنترل Governance یافت نشد: " + link.parentTable() + "/" + parentId));
        return resolveVersionId(link.parentTable(), parent);
    }

    private void assertDraft(long versionId, String table) {
        Map<String, Object> version = repository.findById("PRODUCT_VERSION", versionId)
                .orElseThrow(() -> new ProductBuilderValidationException("نسخه محصول یافت نشد: " + versionId));
        String status = text(version.get("VERSION_STATUS_CODE"));
        if (!"DRAFT".equals(status)) {
            throw new ProductBuilderValidationException(
                    "پیکربندی نسخه " + versionId + " در وضعیت " + status
                            + " از Generic CRUD قابل تغییر نیست (" + table + "). ابتدا نسخه را از مسیر Governance به DRAFT بازگردانید یا نسخه جدید بسازید."
            );
        }
    }

    private static boolean same(Object left, Object right) {
        return text(left).equals(text(right));
    }

    private static long number(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString().trim()); }
        catch (NumberFormatException ex) { return 0; }
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private record ParentLink(String foreignKey, String parentTable) {}
}
