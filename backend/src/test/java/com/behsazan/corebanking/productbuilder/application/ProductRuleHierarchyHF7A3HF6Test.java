package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** HF7-A3/HF6: read-only child decisions MUST reach real canApprove. */
class ProductRuleHierarchyHF7A3HF6Test {
    private static final Set<String> OPTIONAL_CHILDREN = Set.of(
            "PRODUCT_CHANNEL_OPERATION", "LOAN_ELIGIBILITY_EXTENSION",
            "DEPOSIT_PRODUCT_CLOSURE_PRECHECK", "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",
            "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE",
            "PRODUCT_ELIGIBILITY_CRITERION", "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE");

    private static Map<String,Object> row(String table) {
        return new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table));
    }

    private static final class Scenario {
        final PdlProductBuilderRepository repo = mock(PdlProductBuilderRepository.class);
        final ProductRuleGovernanceService governance = new ProductRuleGovernanceService(
                repo, mock(RuleGovernanceLiteService.class), new ProductBuilderBusinessValidator());
        final Map<String,List<Map<String,Object>>> data = new LinkedHashMap<>();
        boolean unavailableChannelChildren;
        final String domain;
        final String family;

        Scenario(String domain, String family) {
            this.domain = domain;
            this.family = family;
            when(repo.findById("PRODUCT_VERSION", 55L)).thenReturn(Optional.of(Map.of(
                    "PRODUCT_ID", 9L, "VALID_FROM", "2026-10-10",
                    "VALID_TO", "2026-10-30", "VERSION_STATUS_CODE", "DRAFT")));
            when(repo.findById("PRODUCT", 9L)).thenReturn(Optional.of(Map.of(
                    "PRODUCT_CLASS_CODE", domain, "PRODUCT_FAMILY_CODE", family,
                    "PRODUCT_STATUS_CODE", "DRAFT", "DEFAULT_CURRENCY_CODE", "IRR")));
            when(repo.findById("PRODUCT", 99L)).thenReturn(Optional.of(Map.of("PRODUCT_ID", 99L)));
        when(repo.findById("LOAN_TYPE", 101L)).thenReturn(Optional.of(Map.of("LOAN_GROUP", 1, "RECORD_STATUS_CODE", "ACTIVE")));
        when(repo.findById("PLAN_TYPE", 201L)).thenReturn(Optional.of(Map.of("RECORD_STATUS_CODE", "ACTIVE")));
        when(repo.loanTypePlanAllowed(101L, 201L)).thenReturn(true);
        when(repo.search(anyString(), isNull(), anyInt(), anyInt(), anyString(), anyString()))
                    .thenAnswer(call -> {
                        String table = call.getArgument(0);
                        int page = call.getArgument(2);
                        int size = call.getArgument(3);
                        if (unavailableChannelChildren && "PRODUCT_CHANNEL_OPERATION".equals(table))
                            throw new IllegalStateException("Read-only Oracle child lookup failed");
                        List<Map<String,Object>> records = "PRODUCT_VERSION_MODULE".equals(table)
                                || OPTIONAL_CHILDREN.contains(table) ? List.of()
                                : List.of(row(table));
                        if (data.containsKey(table)) records = data.get(table);
                        int start = Math.min(records.size(), page * size);
                        int end = Math.min(records.size(), start + size);
                        return new TablePage(records.subList(start, end), records.size(), page, size);
                    });
        }
        Scenario with(String table, Map<String,Object> value) {
            data.put(table, List.of(value));
            return this;
        }
        ProductRuleGovernanceService.GovernanceReport report() { return governance.report(55L); }
        void noWrites() {
            verify(repo, never()).insert(anyString(), anyMap(), anyString());
            verify(repo, never()).update(anyString(), anyLong(), anyMap(), anyString());
        }
    }

    private Scenario deposit() { return new Scenario("DEPOSIT", "CURRENT_ACCOUNT"); }
    private Scenario loan() { return new Scenario("LOAN", "QARD_HASAN_LOAN"); }
    private static void blocked(ProductRuleGovernanceService.GovernanceReport r, String control, String reason) {
        assertThat(r.canApprove()).isFalse();
        assertThat(r.canOpen()).isFalse();
        assertThat(r.effectiveRules()).anyMatch(x -> control.equals(x.code())
                && "INVALID_SEMANTICS".equals(x.status()) && x.blocking()
                && x.reason().contains(reason));
    }

    @Test void validDesignWithOptionalChildrenAbsentKeepsRuntimeClosed() {
        var deposit = deposit();
        assertThat(deposit.report().canApprove()).isTrue();
        assertThat(deposit.report().canOpen()).isFalse();
        deposit.noWrites();
        var loan = loan();
        assertThat(loan.report().canApprove()).isTrue();
        assertThat(loan.report().canOpen()).isFalse();
        loan.noWrites();
    }

    @Test void deniedChannelCannotAuthorizeChildOperationAtApproval() {
        var scenario = deposit().with("PRODUCT_CHANNEL_OPERATION", new LinkedHashMap<>(Map.of(
                "CHANNEL_RULE_ID", 811L, "OPERATION_CODE", "OPEN_ACCOUNT",
                "IS_ALLOWED", 1, "REQUIRES_ADDITIONAL_AUTH", 0)));
        blocked(scenario.report(), "PRODUCT_CHANNEL_RULE", "غیرمجاز");
        assertThatThrownBy(() -> scenario.governance.assertPromotionAllowed(55L,
                Map.of("VERSION_STATUS_CODE", "DRAFT"),
                Map.of("VERSION_STATUS_CODE", "APPROVED")))
                .isInstanceOf(ProductBuilderValidationException.class)
                .hasMessageContaining("کانال و عملیات");
        scenario.noWrites();
    }

    @Test void validAllowedChannelChildRemainsDesignOnly() {
        var scenario = deposit().with("PRODUCT_CHANNEL_RULE", new LinkedHashMap<>(Map.of(
                "CHANNEL_RULE_ID", 811L, "CHANNEL_CODE", "BRANCH", "IS_ALLOWED", 1,
                "RULE_STATUS_CODE", "ACTIVE")));
        scenario.with("PRODUCT_CHANNEL_OPERATION", new LinkedHashMap<>(Map.of(
                "CHANNEL_RULE_ID", 811L, "OPERATION_CODE", "OPEN_ACCOUNT",
                "IS_ALLOWED", 1, "REQUIRES_ADDITIONAL_AUTH", 0)));
        var report = scenario.report();
        assertThat(report.canApprove()).isTrue();
        assertThat(report.canOpen()).isFalse();
        scenario.governance.assertPromotionAllowed(55L,
                Map.of("VERSION_STATUS_CODE", "DRAFT"), Map.of("VERSION_STATUS_CODE", "APPROVED"));
        scenario.noWrites();
    }

    @Test void foreignKeyAndInvalidStatusNeverPassChannelChild() {
        var scenario = deposit();
        var invalid = new LinkedHashMap<>(Map.<String,Object>of(
                "CHANNEL_RULE_ID", 999L, "OPERATION_CODE", "OPEN_ACCOUNT",
                "IS_ALLOWED", 0, "REQUIRES_ADDITIONAL_AUTH", 0));
        scenario.with("PRODUCT_CHANNEL_OPERATION", invalid);
        blocked(scenario.report(), "PRODUCT_CHANNEL_RULE", "ارتباط");
        invalid.put("CHANNEL_RULE_ID", 811L);
        invalid.put("IS_ACTIVE", 9);
        blocked(scenario.report(), "PRODUCT_CHANNEL_RULE", "وضعیت");
        scenario.noWrites();
    }

    @Test void oracleChildFailureFailsClosedButDoesNotThrowHttp500() {
        var scenario = deposit();
        scenario.unavailableChannelChildren = true;
        blocked(scenario.report(), "PRODUCT_CHANNEL_RULE", "قابل بازیابی");
        scenario.noWrites();
    }

    @Test void invalidFutureChildBlocksButInactiveChildDoesNot() {
        var scenario = deposit();
        var child = new LinkedHashMap<>(Map.<String,Object>of(
                "CHANNEL_RULE_ID", 811L, "OPERATION_CODE", "OPEN_ACCOUNT",
                "IS_ALLOWED", 1, "REQUIRES_ADDITIONAL_AUTH", 0,
                "VALID_FROM", "2026-10-20"));
        scenario.with("PRODUCT_CHANNEL_OPERATION", child);
        blocked(scenario.report(), "PRODUCT_CHANNEL_RULE", "غیرمجاز");
        child.put("IS_ACTIVE", 0);
        assertThat(scenario.report().canApprove()).isTrue();
        scenario.noWrites();
    }

    @Test void malformedLoanExtensionBlocksDesignApproval() {
        var scenario = loan();
        scenario.with("LOAN_ELIGIBILITY_EXTENSION", new LinkedHashMap<>(Map.<String,Object>of(
                "ELIGIBILITY_RULE_ID", 812L, "REQUIRED_GUARANTOR_COUNT", 2.5,
                "CBI_TRACKING_REQUIRED", 0, "CREDIT_INQUIRY_REQUIRED", 0,
                "AML_CHECK_REQUIRED", 0)));
        blocked(scenario.report(), "PRODUCT_ELIGIBILITY_RULE", "تعداد ضامنان");
        scenario.noWrites();
    }

    @Test void validLoanExtensionKeepsOriginationClosed() {
        var scenario = loan();
        scenario.with("LOAN_ELIGIBILITY_EXTENSION", new LinkedHashMap<>(Map.<String,Object>of(
                "ELIGIBILITY_RULE_ID", 812L, "REQUIRED_GUARANTOR_COUNT", 2,
                "CBI_TRACKING_REQUIRED", 0, "CREDIT_INQUIRY_REQUIRED", 0,
                "AML_CHECK_REQUIRED", 0)));
        assertThat(scenario.report().canApprove()).isTrue();
        assertThat(scenario.report().canOpen()).isFalse();
        scenario.noWrites();
    }

    @Test void invalidClosurePrecheckBlocksApproval() {
        var scenario = deposit();
        scenario.with("DEPOSIT_PRODUCT_CLOSURE_PRECHECK", new LinkedHashMap<>(Map.<String,Object>of(
                "CLOSURE_RULE_ID", 818L, "CHECK_TYPE_CODE", "HOLD",
                "FAILURE_ACTION_CODE", "BLOCK", "IS_MANDATORY", 1,
                "SEQUENCE_NO", 0)));
        blocked(scenario.report(), "DEPOSIT_PRODUCT_CLOSURE_RULE", "ترتیب");
        scenario.noWrites();
    }

    @Test void inconsistentSettlementDestinationBlocksApproval() {
        var scenario = deposit();
        scenario.with("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE", new LinkedHashMap<>(Map.<String,Object>of(
                "CLOSURE_RULE_ID", 818L, "SETTLEMENT_COMPONENT_CODE", "PRINCIPAL",
                "SETTLEMENT_METHOD_CODE", "DEDUCT", "DESTINATION_CODE", "BANK_ACCOUNT",
                "IS_MANDATORY", 1, "SEQUENCE_NO", 1)));
        blocked(scenario.report(), "DEPOSIT_PRODUCT_CLOSURE_RULE", "مقصد");
        scenario.noWrites();
    }

    @Test void invalidClosureApprovalSequenceBlocksApproval() {
        var scenario = deposit();
        scenario.with("DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE", new LinkedHashMap<>(Map.<String,Object>of(
                "CLOSURE_RULE_ID", 818L, "APPROVAL_LEVEL_CODE", "BRANCH_MANAGER",
                "IS_MANDATORY", 1, "APPROVAL_SEQUENCE_NO", 0)));
        blocked(scenario.report(), "DEPOSIT_PRODUCT_CLOSURE_RULE", "ترتیب");
        scenario.noWrites();
    }

    @Test void validClosureChildrenPreserveDesignApproval() {
        var scenario = deposit();
        scenario.with("DEPOSIT_PRODUCT_CLOSURE_PRECHECK", new LinkedHashMap<>(Map.<String,Object>of(
                "CLOSURE_RULE_ID", 818L, "CHECK_TYPE_CODE", "HOLD",
                "FAILURE_ACTION_CODE", "BLOCK", "IS_MANDATORY", 1,
                "SEQUENCE_NO", 1)));
        scenario.with("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE", new LinkedHashMap<>(Map.<String,Object>of(
                "CLOSURE_RULE_ID", 818L, "SETTLEMENT_COMPONENT_CODE", "PRINCIPAL",
                "SETTLEMENT_METHOD_CODE", "DEDUCT", "IS_MANDATORY", 1,
                "SEQUENCE_NO", 1)));
        scenario.with("DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE", new LinkedHashMap<>(Map.<String,Object>of(
                "CLOSURE_RULE_ID", 818L, "APPROVAL_LEVEL_CODE", "BRANCH_MANAGER",
                "IS_MANDATORY", 1, "APPROVAL_SEQUENCE_NO", 1)));
        assertThat(scenario.report().canApprove()).isTrue();
        assertThat(scenario.report().canOpen()).isFalse();
        scenario.noWrites();
    }
}
