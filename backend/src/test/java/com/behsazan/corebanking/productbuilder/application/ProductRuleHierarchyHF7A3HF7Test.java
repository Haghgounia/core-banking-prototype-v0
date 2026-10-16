package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** HF7-A3/HF7: normalized eligibility, correspondent settlement, operational fail-closed. */
class ProductRuleHierarchyHF7A3HF7Test {
    private static final Set<String> OPTIONAL_CHILDREN = Set.of(
            "PRODUCT_CHANNEL_OPERATION", "LOAN_ELIGIBILITY_EXTENSION",
            "DEPOSIT_PRODUCT_CLOSURE_PRECHECK", "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",
            "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE", "PRODUCT_ELIGIBILITY_CRITERION",
            "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE");

    private static Map<String,Object> row(Object... items) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (int i = 0; i < items.length; i += 2) result.put((String) items[i], items[i + 1]);
        return result;
    }

    private static final class Scenario {
        final PdlProductBuilderRepository repository = mock(PdlProductBuilderRepository.class);
        final ProductRuleGovernanceService governance = new ProductRuleGovernanceService(
                repository, mock(RuleGovernanceLiteService.class), new ProductBuilderBusinessValidator());
        final Map<String,List<Map<String,Object>>> records = new LinkedHashMap<>();
        String failingChild;

        Scenario(String domain, String family) {
            when(repository.findById("PRODUCT_VERSION", 55L)).thenReturn(Optional.of(Map.of(
                    "PRODUCT_ID", 9L, "VALID_FROM", "2026-10-10",
                    "VALID_TO", "2026-10-30", "VERSION_STATUS_CODE", "DRAFT")));
            when(repository.findById("PRODUCT", 9L)).thenReturn(Optional.of(Map.of(
                    "PRODUCT_CLASS_CODE", domain, "PRODUCT_FAMILY_CODE", family,
                    "PRODUCT_STATUS_CODE", "DRAFT", "DEFAULT_CURRENCY_CODE", "IRR")));
            when(repository.findById("PRODUCT", 99L)).thenReturn(Optional.of(Map.of("PRODUCT_ID", 99L)));
        when(repository.findById("LOAN_TYPE", 101L)).thenReturn(Optional.of(Map.of("LOAN_GROUP", 1, "RECORD_STATUS_CODE", "ACTIVE")));
        when(repository.findById("PLAN_TYPE", 201L)).thenReturn(Optional.of(Map.of("RECORD_STATUS_CODE", "ACTIVE")));
        when(repository.loanTypePlanAllowed(101L, 201L)).thenReturn(true);
        when(repository.search(anyString(), isNull(), anyInt(), anyInt(), anyString(), anyString()))
                    .thenAnswer(call -> {
                        String table = call.getArgument(0);
                        int page = call.getArgument(2);
                        int size = call.getArgument(3);
                        if (table.equals(failingChild)) throw new IllegalStateException("Oracle READ failed");
                        List<Map<String,Object>> data = "PRODUCT_VERSION_MODULE".equals(table)
                                || OPTIONAL_CHILDREN.contains(table) ? List.of()
                                : List.of(new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table)));
                        if (records.containsKey(table)) data = records.get(table);
                        int start = Math.min(data.size(), page * size);
                        int end = Math.min(data.size(), start + size);
                        return new TablePage(data.subList(start, end), data.size(), page, size);
                    });
        }
        Scenario with(String table, Map<String,Object> value) {
            records.put(table, List.of(value));
            return this;
        }
        Scenario withRows(String table, Map<String,Object>... values) {
            records.put(table, new ArrayList<>(List.of(values)));
            return this;
        }
        ProductRuleGovernanceService.GovernanceReport report() { return governance.report(55L); }
        void noWrites() {
            verify(repository, never()).insert(anyString(), anyMap(), anyString());
            verify(repository, never()).update(anyString(), anyLong(), anyMap(), anyString());
        }
    }

    private static void blocked(ProductRuleGovernanceService.GovernanceReport result,
                                String control, String snippet) {
        assertThat(result.canApprove()).isFalse();
        assertThat(result.canOpen()).isFalse();
        assertThat(result.effectiveRules()).anyMatch(item -> item.code().equals(control)
                && "INVALID_SEMANTICS".equals(item.status()) && item.blocking()
                && item.reason().contains(snippet));
    }

    private static Scenario correspondent(String family) {
        String account = "NOSTRO_ACCOUNT".equals(family) ? "NOSTRO" : "VOSTRO";
        return new Scenario("DEPOSIT", family).with("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE", row(
                "CORRESPONDENT_PRODUCT_PROFILE_ID", 715L,
                "PRODUCT_VERSION_ID", 55L, "ACCOUNT_TYPE_CODE", account,
                "SETTLEMENT_CURRENCY_CODE", "USD", "RECONCILIATION_METHOD_CODE", "AUTOMATIC",
                "OVERDRAFT_ALLOWED", 0));
    }

    @Test void correspondentDesignApprovalNeverGrantsOperationalOpen() {
        for (String family : List.of("NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT")) {
            var scenario = correspondent(family);
            var report = scenario.report();
            assertThat(report.canApprove()).isTrue();
            assertThat(report.canOpen()).isFalse();
            assertThat(report.blockers()).anyMatch(item -> item.contains("نوسترو/وسترو"));
            scenario.governance.assertPromotionAllowed(55L,
                    Map.of("VERSION_STATUS_CODE", "DRAFT", "ORIGINATION_STATUS_CODE", "CLOSED"),
                    Map.of("VERSION_STATUS_CODE", "APPROVED", "ORIGINATION_STATUS_CODE", "CLOSED"));
            assertThatThrownBy(() -> scenario.governance.assertPromotionAllowed(55L,
                    Map.of("VERSION_STATUS_CODE", "DRAFT", "ORIGINATION_STATUS_CODE", "CLOSED"),
                    Map.of("VERSION_STATUS_CODE", "APPROVED", "ORIGINATION_STATUS_CODE", "OPEN")))
                    .isInstanceOf(ProductBuilderValidationException.class)
                    .hasMessageContaining("نوسترو/وسترو");
            scenario.noWrites();
        }
    }

    @Test void correspondentProfileMustMatchFamily() {
        var scenario = correspondent("NOSTRO_ACCOUNT");
        scenario.records.get("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE").get(0)
                .put("ACCOUNT_TYPE_CODE", "VOSTRO");
        blocked(scenario.report(), "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE", "خانواده");
        scenario.noWrites();
    }

    @Test void validCorrespondentSettlementCanApproveDesignButNotOpen() {
        var scenario = correspondent("VOSTRO_ACCOUNT").with("CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE", row(
                "CORRESPONDENT_PRODUCT_PROFILE_ID", 715L,
                "SETTLEMENT_FREQUENCY_CODE", "DAILY", "CUTOFF_TIME", "16:30",
                "NETTING_ALLOWED", 0, "VALID_FROM", "2026-10-10"));
        assertThat(scenario.report().canApprove()).isTrue();
        assertThat(scenario.report().canOpen()).isFalse();
        scenario.noWrites();
    }

    @Test void correspondentSettlementInvalidCutoffBlocksRealApproval() {
        var scenario = correspondent("NOSTRO_ACCOUNT").with("CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE", row(
                "CORRESPONDENT_PRODUCT_PROFILE_ID", 715L, "SETTLEMENT_FREQUENCY_CODE", "DAILY",
                "CUTOFF_TIME", "27:90", "VALID_FROM", "2026-10-10"));
        blocked(scenario.report(), "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE", "CUTOFF_TIME");
        scenario.noWrites();
    }

    @Test void correspondentWrongForeignKeyAndLookupFailureFailClosed() {
        var scenario = correspondent("NOSTRO_ACCOUNT").with("CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE", row(
                "CORRESPONDENT_PRODUCT_PROFILE_ID", 999L, "SETTLEMENT_FREQUENCY_CODE", "DAILY",
                "VALID_FROM", "2026-10-10"));
        blocked(scenario.report(), "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE", "ارتباط");
        scenario.records.remove("CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE");
        scenario.failingChild = "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE";
        blocked(scenario.report(), "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE", "قابل بازیابی");
        scenario.noWrites();
    }

    @Test void invalidEligibilityCriterionBlocksRealApprovalForDeposit() {
        var scenario = new Scenario("DEPOSIT", "CURRENT_ACCOUNT").with("PRODUCT_ELIGIBILITY_CRITERION", row(
                "ELIGIBILITY_RULE_ID", 812L, "CRITERION_TYPE_CODE", "CUSTOMER_TENURE",
                "OPERATOR_CODE", "EQ", "CRITERION_VALUE_NUMBER", 12,
                "VALUE_UNIT_CODE", "MONTH"));
        blocked(scenario.report(), "PRODUCT_ELIGIBILITY_RULE", "سابقه");
        scenario.noWrites();
    }

    @Test void validPersonAndGenderCriteriaAreAllowedForBothDomains() {
        for (String domain : List.of("DEPOSIT", "LOAN")) {
            var scenario = new Scenario(domain, domain.equals("DEPOSIT") ? "CURRENT_ACCOUNT" : "QARD_HASAN_LOAN");
            scenario.withRows("PRODUCT_ELIGIBILITY_CRITERION",
                    row("ELIGIBILITY_RULE_ID", 812L, "CRITERION_TYPE_CODE", "CUSTOMER_TYPE",
                            "OPERATOR_CODE", "IN", "CRITERION_VALUE_CODE", "PERSON", "IS_ACTIVE", 1),
                    row("ELIGIBILITY_RULE_ID", 812L, "CRITERION_TYPE_CODE", "GENDER",
                            "OPERATOR_CODE", "IN", "CRITERION_VALUE_CODE", "1", "IS_ACTIVE", 1));
            assertThat(scenario.report().canApprove()).isTrue();
            assertThat(scenario.report().canOpen()).isFalse();
            scenario.noWrites();
        }
    }

    @Test void genderOutsidePersonOnlyScopeMustBlock() {
        var scenario = new Scenario("LOAN", "QARD_HASAN_LOAN");
        scenario.withRows("PRODUCT_ELIGIBILITY_CRITERION",
                row("ELIGIBILITY_RULE_ID", 812L, "CRITERION_TYPE_CODE", "CUSTOMER_TYPE",
                        "OPERATOR_CODE", "IN", "CRITERION_VALUE_CODE", "ORGANIZATION"),
                row("ELIGIBILITY_RULE_ID", 812L, "CRITERION_TYPE_CODE", "GENDER",
                        "OPERATOR_CODE", "EQ", "CRITERION_VALUE_CODE", "1"));
        blocked(scenario.report(), "PRODUCT_ELIGIBILITY_RULE", "جنسیت");
        scenario.noWrites();
    }

    @Test void repeatedAndForeignEligibilityCriteriaAreRejected() {
        var scenario = new Scenario("DEPOSIT", "CURRENT_ACCOUNT");
        var criterion = row("ELIGIBILITY_RULE_ID", 812L, "CRITERION_TYPE_CODE", "CUSTOMER_SEGMENT",
                "OPERATOR_CODE", "IN", "CRITERION_VALUE_CODE", "VIP");
        scenario.withRows("PRODUCT_ELIGIBILITY_CRITERION", criterion, new LinkedHashMap<>(criterion));
        blocked(scenario.report(), "PRODUCT_ELIGIBILITY_RULE", "تکراری");
        scenario.with("PRODUCT_ELIGIBILITY_CRITERION", row("ELIGIBILITY_RULE_ID", 999L,
                "CRITERION_TYPE_CODE", "CUSTOMER_SEGMENT", "OPERATOR_CODE", "IN",
                "CRITERION_VALUE_CODE", "VIP"));
        blocked(scenario.report(), "PRODUCT_ELIGIBILITY_RULE", "ارتباط");
        scenario.noWrites();
    }

    @Test void invalidCriterionFlagAndLookupFailureAreRejected() {
        var scenario = new Scenario("DEPOSIT", "CURRENT_ACCOUNT");
        scenario.with("PRODUCT_ELIGIBILITY_CRITERION", row("ELIGIBILITY_RULE_ID", 812L,
                "CRITERION_TYPE_CODE", "CUSTOMER_TYPE", "OPERATOR_CODE", "IN",
                "CRITERION_VALUE_CODE", "PERSON", "IS_ACTIVE", 9));
        blocked(scenario.report(), "PRODUCT_ELIGIBILITY_RULE", "وضعیت");
        scenario.records.remove("PRODUCT_ELIGIBILITY_CRITERION");
        scenario.failingChild = "PRODUCT_ELIGIBILITY_CRITERION";
        blocked(scenario.report(), "PRODUCT_ELIGIBILITY_RULE", "قابل بازیابی");
        scenario.noWrites();
    }

    @Test void inactiveCriteriaAreNotUsedAsApprovalEvidence() {
        var scenario = new Scenario("DEPOSIT", "CURRENT_ACCOUNT").with("PRODUCT_ELIGIBILITY_CRITERION", row(
                "ELIGIBILITY_RULE_ID", 812L, "CRITERION_TYPE_CODE", "GENDER",
                "OPERATOR_CODE", "BAD", "IS_ACTIVE", 0));
        assertThat(scenario.report().canApprove()).isTrue();
        assertThat(scenario.report().canOpen()).isFalse();
        scenario.noWrites();
    }
}
