package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** HF7-A3/HF4: real child-level semantic/temporal publication gates. */
class ProductRuleHierarchyHF7A3HF4Test {
    private static final LocalDate FROM = LocalDate.parse("2026-10-10");
    private static final LocalDate TO = LocalDate.parse("2026-10-30");
    private final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();

    private static Map<String,Object> termParent() {
        return new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PRODUCT_TERM_RULE"));
    }
    private static Map<String,Object> termChild() {
        return new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PRODUCT_ALLOWED_TERM"));
    }
    private static Map<String,Object> pricingParent() {
        var p = new LinkedHashMap<>(ProductRuleGovernanceFixture.row("PRODUCT_PRICING_RULE"));
        p.put("IS_TIERED_RATE", 1);
        return p;
    }
    private static Map<String,Object> component() {
        return new LinkedHashMap<>(ProductRuleGovernanceFixture.row("PRODUCT_PRICING_COMPONENT"));
    }
    private static Map<String,Object> tier() {
        return new LinkedHashMap<>(ProductRuleGovernanceFixture.row("PRODUCT_RATE_TIER"));
    }
    private static ProductRuleHierarchyAudit.Finding term(List<Map<String,Object>> children) {
        return ProductRuleHierarchyAudit.allowedTerms(List.of(termParent()), FROM, TO,
                (table, fk, id) -> children, new ProductBuilderBusinessValidator());
    }
    private static ProductRuleHierarchyAudit.Finding tiered(List<Map<String,Object>> children) {
        return ProductRuleHierarchyAudit.pricingTiers(List.of(pricingParent()), FROM, TO,
                (table, fk, id) -> "PRODUCT_PRICING_COMPONENT".equals(table) ? List.of(component()) : children,
                new ProductBuilderBusinessValidator());
    }

    @Test void oneValidTermCoversFullVersionAndDoesNotClaimRuntimeQualification() {
        assertThat(term(List.of(termChild())).valid()).isTrue();
    }
    @Test void missingOrInactiveTermChildCannotBeApproved() {
        assertThat(term(List.of()).status()).isEqualTo("CONFLICT");
        var inactive = termChild(); inactive.put("IS_ACTIVE", 0);
        assertThat(term(List.of(inactive)).status()).isEqualTo("CONFLICT");
    }
    @Test void termChildDateGapBlocksApproval() {
        var shorter = termChild(); shorter.put("EFFECTIVE_TO_DATE", "2026-10-20");
        var finding = term(List.of(shorter));
        assertThat(finding.status()).isEqualTo("CONFLICT");
        assertThat(finding.reason()).contains("2026-10-21");
    }
    @Test void adjacentTermChildrenCoverAnInclusivePeriod() {
        var first = termChild(); first.put("EFFECTIVE_TO_DATE", "2026-10-20");
        var second = termChild(); second.put("EFFECTIVE_FROM_DATE", "2026-10-21");
        second.put("TERM_VALUE", 12); second.put("IS_DEFAULT", 0);
        assertThat(term(List.of(first, second)).valid()).isTrue();
    }
    @Test void duplicateTermOrTwoOverlappingDefaultsAreNotPublishable() {
        var first = termChild();
        var second = termChild(); second.put("TERM_VALUE", 12);
        assertThat(term(List.of(first, second)).reason()).contains("پیش‌فرض");
        second.put("IS_DEFAULT", 0); second.put("TERM_VALUE", 6);
        assertThat(term(List.of(first, second)).reason()).contains("تکراری");
    }
    @Test void futureInvalidTermChildStillBlocksApproval() {
        var valid = termChild();
        var invalid = termChild(); invalid.put("EFFECTIVE_FROM_DATE", "2026-10-20");
        invalid.put("TERM_VALUE", "1.5"); invalid.put("IS_DEFAULT", 0);
        assertThat(term(List.of(valid, invalid)).status()).isEqualTo("INVALID_SEMANTICS");
    }
    @Test void invalidTermDateAndWrongFkAreRejected() {
        var child = termChild(); child.put("EFFECTIVE_FROM_DATE", "2026-10-25");
        child.put("EFFECTIVE_TO_DATE", "2026-10-20");
        assertThat(term(List.of(child)).status()).isEqualTo("INVALID_SEMANTICS");
        child = termChild(); child.put("TERM_RULE_ID", 111L);
        assertThat(term(List.of(child)).status()).isEqualTo("INVALID_SEMANTICS");
    }
    @Test void failedChildLookupNeverGrantsApproval() {
        var finding = ProductRuleHierarchyAudit.allowedTerms(List.of(termParent()), FROM, TO,
                (table, field, id) -> { throw new IllegalStateException("Oracle not available"); }, validator);
        assertThat(finding.status()).isEqualTo("CONFLICT");
    }
    @Test void tieredPricingRequiresValidTierAcrossParentPeriod() {
        assertThat(tiered(List.of(tier())).valid()).isTrue();
        assertThat(tiered(List.of()).status()).isEqualTo("CONFLICT");
    }
    @Test void missingTierDaysAndMissingRatesBlockApproval() {
        var shortTier = tier(); shortTier.put("VALID_TO", "2026-10-20");
        assertThat(tiered(List.of(shortTier)).reason()).contains("2026-10-21");
        var badRate = tier(); badRate.put("RATE_VALUE", "1.2");
        assertThat(tiered(List.of(badRate)).status()).isEqualTo("INVALID_SEMANTICS");
    }
    @Test void duplicateTierNumbersAreRejectedOnlyWhenDatesOverlap() {
        var first = tier(); first.put("VALID_TO", "2026-10-20");
        var next = tier(); next.put("VALID_FROM", "2026-10-21");
        assertThat(tiered(List.of(first,next)).valid()).isTrue();
        next.put("VALID_FROM", "2026-10-20");
        assertThat(tiered(List.of(first,next)).status()).isEqualTo("CONFLICT");
    }
    @Test void nonTieredPricingDoesNotRequireChildRecords() {
        var parent = new LinkedHashMap<>(pricingParent()); parent.put("IS_TIERED_RATE", 0);
        assertThat(ProductRuleHierarchyAudit.pricingTiers(List.of(parent), FROM, TO,
                (table, fk, id) -> { throw new AssertionError("No tier query expected"); }, validator).valid()).isTrue();
    }
    @Test void laterEffectiveTieredParentUsesItsOwnDateScope() {
        var parent = new LinkedHashMap<>(pricingParent()); parent.put("VALID_FROM", "2026-10-20");
        var child = tier(); child.put("VALID_FROM", "2026-10-20");
        assertThat(ProductRuleHierarchyAudit.pricingTiers(List.of(parent), FROM, TO,
                (table, fk, id) -> "PRODUCT_PRICING_COMPONENT".equals(table) ? List.of(component()) : List.of(child),
                validator).valid()).isTrue();
    }
    @Test void governanceReportBlocksSemanticDefectInRelatedChild() {
        var repo = mock(PdlProductBuilderRepository.class);
        when(repo.findById("PRODUCT_VERSION",55L)).thenReturn(Optional.of(Map.of(
                "PRODUCT_ID",9L,"VALID_FROM","2026-10-10","VALID_TO","2026-10-30","VERSION_STATUS_CODE","DRAFT")));
        when(repo.findById("PRODUCT",9L)).thenReturn(Optional.of(Map.of(
                "PRODUCT_CLASS_CODE","DEPOSIT","PRODUCT_FAMILY_CODE","LONG_TERM_DEPOSIT","PRODUCT_STATUS_CODE","DRAFT")));
        when(repo.search(anyString(),isNull(),anyInt(),anyInt(),anyString(),anyString())).thenAnswer(inv -> {
            String table = inv.getArgument(0);
            int page = inv.getArgument(2);
            if (page > 0 || "PRODUCT_VERSION_MODULE".equals(table)) return new TablePage(List.of(),0,page,200);
            Map<String,Object> row = new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table));
            if ("DEPOSIT_PRODUCT_ALLOWED_TERM".equals(table)) row.put("TERM_VALUE", "1.5");
            return new TablePage(List.of(row),1,page,200);
        });
        var report = new ProductRuleGovernanceService(repo, mock(RuleGovernanceLiteService.class), validator).report(55L);
        assertThat(report.canApprove()).isFalse();
        assertThat(report.canOpen()).isFalse();
        assertThat(report.effectiveRules()).anyMatch(r -> r.code().equals("DEPOSIT_PRODUCT_TERM_RULE")
                && r.status().equals("INVALID_SEMANTICS") && r.reason().contains("عدد صحیح"));
        verify(repo,never()).insert(anyString(),anyMap(),anyString());
        verify(repo,never()).update(anyString(),anyLong(),anyMap(),anyString());
    }
}
