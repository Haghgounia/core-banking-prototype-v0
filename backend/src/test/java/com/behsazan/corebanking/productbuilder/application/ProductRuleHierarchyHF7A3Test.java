package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** R30-HF7-A3: no Oracle/HTTP writes, only pure contracts and read-only stubs. */
class ProductRuleHierarchyHF7A3Test {
    private static final LocalDate START = LocalDate.of(2026, 10, 10);
    private static final LocalDate END = LocalDate.of(2026, 10, 20);
    private final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();

    @Test void invalidIsActiveCannotDisappearAsInactiveHistory() {
        Map<String, Object> malformed = new LinkedHashMap<>(Map.of("IS_ACTIVE", 9));
        var bad = ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_ALLOWED_TERM", List.of(malformed), START, END);
        assertEquals(1, bad.unrecognizedStatuses());
        assertTrue(bad.conflict());
        assertEquals(0, bad.effectiveCount());
        assertEquals(1, ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_ALLOWED_TERM",
                List.of(Map.of("IS_ACTIVE", new BigDecimal("1.0"))), START, END).effectiveCount());
    }

    @Test void duplicateCommonDocumentsAndInquiriesAreNotEquivalentToOneRule() {
        var a = Map.<String, Object>of("DOCUMENT_TYPE_ID", 17, "REQUIREMENT_STAGE_CODE", "OPENING", "RULE_STATUS_CODE", "ACTIVE");
        assertTrue(ProductRuleEvidenceEvaluator.evaluate("PRODUCT_REQUIRED_DOCUMENT", List.of(a, a), START, END).conflict());
        var inquiry = Map.<String, Object>of("INQUIRY_TYPE_CODE", "KYC", "REQUIREMENT_STAGE_CODE", "APPROVAL", "RULE_STATUS_CODE", "ACTIVE");
        assertTrue(ProductRuleEvidenceEvaluator.evaluate("PRODUCT_REQUIRED_INQUIRY", List.of(inquiry, inquiry), START, END).conflict());
    }

    @Test void allowedTermNeedsValidUnitAndIntegralPositiveDuration() {
        assertDoesNotThrow(() -> validator.validate("DEPOSIT_PRODUCT_ALLOWED_TERM",
                Map.of("TERM_UNIT_CODE", "DAY", "TERM_VALUE", 30, "IS_DEFAULT", 0, "IS_ACTIVE", 1)));
        assertThrows(ProductBuilderValidationException.class, () -> validator.validate("DEPOSIT_PRODUCT_ALLOWED_TERM",
                Map.of("TERM_VALUE", 30)));
        assertThrows(ProductBuilderValidationException.class, () -> validator.validate("DEPOSIT_PRODUCT_ALLOWED_TERM",
                Map.of("TERM_UNIT_CODE", "DAY", "TERM_VALUE", 1.5)));
        assertThrows(ProductBuilderValidationException.class, () -> validator.validate("DEPOSIT_PRODUCT_ALLOWED_TERM",
                Map.of("TERM_UNIT_CODE", "DAY", "TERM_VALUE", 30, "IS_DEFAULT", 9)));
    }

    @Test void rateTierRequiresRateAndValidFractionAndTermUnit() {
        assertDoesNotThrow(() -> validator.validate("PRODUCT_RATE_TIER",
                Map.of("TIER_NO", 1, "RATE_VALUE", new BigDecimal("0.15"), "MIN_AMOUNT", 0, "MAX_AMOUNT", 1000)));
        assertThrows(ProductBuilderValidationException.class, () -> validator.validate("PRODUCT_RATE_TIER", Map.of("TIER_NO", 1)));
        assertThrows(ProductBuilderValidationException.class, () -> validator.validate("PRODUCT_RATE_TIER", Map.of("TIER_NO", 1, "RATE_VALUE", new BigDecimal("1.2"))));
        assertThrows(ProductBuilderValidationException.class, () -> validator.validate("PRODUCT_RATE_TIER", Map.of("TIER_NO", 1.2, "RATE_VALUE", new BigDecimal("0.1"))));
        assertThrows(ProductBuilderValidationException.class, () -> validator.validate("PRODUCT_RATE_TIER", Map.of("TIER_NO", 1, "RATE_VALUE", new BigDecimal("0.1"), "MIN_TERM_VALUE", 3)));
    }

    @Test void overlappingSameTierNumberIsConflictButSeparateDatesWork() {
        var a = new LinkedHashMap<String, Object>();
        a.put("TIER_NO", 1); a.put("RATE_VALUE", new BigDecimal("0.1"));
        a.put("VALID_FROM", "2026-10-10"); a.put("VALID_TO", "2026-10-15");
        var b = new LinkedHashMap<>(a); b.put("VALID_FROM", "2026-10-15"); b.put("VALID_TO", "2026-10-20");
        assertTrue(ProductRuleEvidenceEvaluator.evaluate("PRODUCT_RATE_TIER", List.of(a, b), START, END).conflict());
        b.put("VALID_FROM", "2026-10-16");
        assertFalse(ProductRuleEvidenceEvaluator.evaluate("PRODUCT_RATE_TIER", List.of(a, b), START, END).conflict());
    }

    @Test void deniedParentChannelCannotContainAllowedChildOperation() {
        var parent = Map.<String, Object>of("CHANNEL_RULE_ID", 81L, "CHANNEL_CODE", "BRANCH", "IS_ALLOWED", 0);
        var child = Map.<String, Object>of("CHANNEL_RULE_ID", 81L, "OPERATION_CODE", "OPEN_ACCOUNT",
                "IS_ALLOWED", 1, "REQUIRES_ADDITIONAL_AUTH", 0);
        var bad = ProductRuleHierarchyAudit.audit("PRODUCT_CHANNEL_RULE", List.of(parent), START, END,
                "DEPOSIT", (table, fk, id) -> List.of(child), validator);
        assertTrue(bad.invalidCount() > 0);
        assertTrue(bad.firstProblem().contains("غیرمجاز"));
        var good = ProductRuleHierarchyAudit.audit("PRODUCT_CHANNEL_RULE", List.of(Map.of(
                "CHANNEL_RULE_ID", 81L, "IS_ALLOWED", 1)), START, END,
                "DEPOSIT", (table, fk, id) -> List.of(child), validator);
        assertEquals(0, good.invalidCount());
    }

    @Test void optionalLoanEligibilityChildIsValidatedWhenPresent() {
        var parent = Map.<String, Object>of("ELIGIBILITY_RULE_ID", 91L, "RULE_STATUS_CODE", "ACTIVE");
        var incomplete = Map.<String, Object>of("ELIGIBILITY_RULE_ID", 91L, "REQUIRED_GUARANTOR_COUNT", 2.5,
                "CBI_TRACKING_REQUIRED", 0, "CREDIT_INQUIRY_REQUIRED", 0, "AML_CHECK_REQUIRED", 0);
        var bad = ProductRuleHierarchyAudit.audit("PRODUCT_ELIGIBILITY_RULE", List.of(parent), START, END,
                "LOAN", (table, fk, id) -> List.of(incomplete), validator);
        assertTrue(bad.invalidCount() > 0);
        var good = ProductRuleHierarchyAudit.audit("PRODUCT_ELIGIBILITY_RULE", List.of(parent), START, END,
                "DEPOSIT", (table, fk, id) -> List.of(incomplete), validator);
        assertEquals(0, good.invalidCount()); // Loan extension does not belong to deposit domain
    }

    @Test void closureChildInvalidOrderBlocksReadOnlyApprovalAudit() {
        var parent = Map.<String, Object>of("CLOSURE_RULE_ID", 99L, "IS_CLOSURE_ALLOWED", 1);
        var wrong = Map.<String, Object>of("CLOSURE_RULE_ID", 99L,
                "CHECK_TYPE_CODE", "HOLD", "FAILURE_ACTION_CODE", "BLOCK",
                "IS_MANDATORY", 1, "SEQUENCE_NO", 0);
        var result = ProductRuleHierarchyAudit.audit("DEPOSIT_PRODUCT_CLOSURE_RULE", List.of(parent), START, END,
                "DEPOSIT", (table, fk, id) -> table.equals("DEPOSIT_PRODUCT_CLOSURE_PRECHECK") ? List.of(wrong) : List.of(), validator);
        assertTrue(result.invalidCount() > 0);
    }

    /** The civil-partnership discriminator lives in LoanCivilPartnershipPolicy and is
     *  enforced by ProductBuilderService.validateCivilPartnershipContribution.
     *  Do not reflect on a removed governance-service method. */
    @Test void contributionContractIsSpecificToCivilPartnership() {
        var mudaraba = Map.<String, Object>of("LOAN_GROUP", 3,
                "LOAN_TYPE_CODE", "MUDARABA", "LOAN_TYPE_NAME", "مضاربه");
        var civilPartnership = Map.<String, Object>of("LOAN_GROUP", 3,
                "LOAN_TYPE_CODE", "CIVIL_PARTNERSHIP", "LOAN_TYPE_NAME", "مشارکت مدنی");
        assertFalse(LoanCivilPartnershipPolicy.matches(mudaraba));
        assertTrue(LoanCivilPartnershipPolicy.matches(civilPartnership));
    }

    @Test void termParentIsNotCoveredByOneDayOfAllowedChild() throws Exception {
        PdlProductBuilderRepository repo = mock(PdlProductBuilderRepository.class);
        var service = new ProductRuleGovernanceService(repo, mock(RuleGovernanceLiteService.class), validator);
        Method check = ProductRuleGovernanceService.class.getDeclaredMethod("hasActiveAllowedTerm", List.class, LocalDate.class, LocalDate.class);
        check.setAccessible(true);
        var parent = Map.<String, Object>of("TERM_RULE_ID", 7L, "IS_ACTIVE", 1);
        var child = new LinkedHashMap<String, Object>();
        child.put("TERM_RULE_ID", 7L); child.put("TERM_UNIT_CODE", "DAY"); child.put("TERM_VALUE", 30);
        child.put("IS_ACTIVE", 1); child.put("EFFECTIVE_FROM_DATE", "2026-10-10");
        child.put("EFFECTIVE_TO_DATE", "2026-10-11");
        when(repo.search(eq("DEPOSIT_PRODUCT_ALLOWED_TERM"), isNull(), eq(0), eq(200), eq("TERM_RULE_ID"), eq("7")))
                .thenAnswer(inv -> new TablePage(List.of(child), 1, 0, 200));
        assertTrue(((ProductRuleHierarchyAudit.Finding) check.invoke(service, List.of(parent), START, END)).conflict());
        child.put("EFFECTIVE_TO_DATE", "2026-10-20");
        assertTrue(((ProductRuleHierarchyAudit.Finding) check.invoke(service, List.of(parent), START, END)).valid());
    }

    @Test void tieredPricingRequiresContinuousChildTierCoverage() throws Exception {
        PdlProductBuilderRepository repo = mock(PdlProductBuilderRepository.class);
        var service = new ProductRuleGovernanceService(repo, mock(RuleGovernanceLiteService.class), validator);
        Method check = ProductRuleGovernanceService.class.getDeclaredMethod("hasValidPricingTiers", List.class, LocalDate.class, LocalDate.class);
        check.setAccessible(true);
        var parent = Map.<String, Object>of("PRICING_RULE_ID", 11L, "IS_TIERED_RATE", 1);
        var component = Map.<String, Object>of("PRICING_COMPONENT_ID", 12L, "PRICING_RULE_ID", 11L,
                "RATE_VALUE", new BigDecimal("0.15"));
        var tier = new LinkedHashMap<String, Object>();
        tier.put("PRICING_COMPONENT_ID", 12L); tier.put("TIER_NO", 1);
        tier.put("RATE_VALUE", new BigDecimal("0.15"));
        tier.put("VALID_FROM", "2026-10-10"); tier.put("VALID_TO", "2026-10-11");
        when(repo.search(eq("PRODUCT_PRICING_COMPONENT"), isNull(), eq(0), eq(200), eq("PRICING_RULE_ID"), eq("11")))
                .thenReturn(new TablePage(List.of(component), 1, 0, 200));
        when(repo.search(eq("PRODUCT_RATE_TIER"), isNull(), eq(0), eq(200), eq("PRICING_COMPONENT_ID"), eq("12")))
                .thenAnswer(inv -> new TablePage(List.of(tier), 1, 0, 200));
        assertTrue(((ProductRuleHierarchyAudit.Finding) check.invoke(service, List.of(parent), START, END)).conflict());
        tier.put("VALID_TO", "2026-10-20");
        assertTrue(((ProductRuleHierarchyAudit.Finding) check.invoke(service, List.of(parent), START, END)).valid());
    }
}
