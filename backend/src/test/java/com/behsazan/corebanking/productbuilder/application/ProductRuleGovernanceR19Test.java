package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductRuleGovernanceR19Test {
    private final PdlProductBuilderRepository repository = mock(PdlProductBuilderRepository.class);
    private final ProductRuleGovernanceService governance = new ProductRuleGovernanceService(repository, mock(RuleGovernanceLiteService.class));

    private void setup(String domain, String family, boolean transactionEnabled, long profileCount) {
        when(repository.findById("PRODUCT_VERSION", 55L)).thenReturn(Optional.of(Map.of(
                "PRODUCT_VERSION_ID", 55L, "PRODUCT_ID", 9L, "VERSION_STATUS_CODE", "DRAFT", "VALID_FROM", "2026-10-10")));
        when(repository.findById("PRODUCT", 9L)).thenReturn(Optional.of(Map.of(
                "PRODUCT_ID", 9L, "PRODUCT_CLASS_CODE", domain,
                "PRODUCT_FAMILY_CODE", family, "PRODUCT_STATUS_CODE", "DRAFT")));
        when(repository.search(anyString(), isNull(), eq(0), anyInt(), anyString(), anyString())).thenAnswer(inv -> {
            String table = inv.getArgument(0);
            if ("PRODUCT_VERSION_MODULE".equals(table)) return new TablePage(
                    transactionEnabled ? List.of() : List.of(Map.of("MODULE_CODE", "TRANSACTION", "IS_ENABLED", 0)), 1, 0, 200);
            long count = "DEPOSIT_PRODUCT_PROFILE".equals(table) ? profileCount : 1;
            Map<String,Object> example = Map.of("RULE_STATUS_CODE", "ACTIVE", "CHANNEL_CODE", "BRANCH",
                    "PRICING_PURPOSE_CODE", "SAVINGS", "CURRENCY_CODE", "IRR",
                    "TRANSACTION_TYPE_CODE", "CASH_WITHDRAWAL", "CLOSURE_TYPE_CODE", "NORMAL");
            return new TablePage(count == 0 ? List.of() : java.util.Collections.nCopies((int)count, example), count, 0, 200);
        });
    }

    @Test void governanceLevelsAreCategoriesAndDoNotEncodeAnOverrideHierarchy() {
        assertThat(governance.governanceLevels()).hasSize(7);
        var control = governance.controls().stream().filter(c -> c.code().equals("PRODUCT_CHANNEL_RULE")).findFirst().orElseThrow();
        assertThat(control.applicabilityScopes()).containsExactly("CHANNEL", "OPERATION");
        assertThat(control.governanceLevel()).isEqualTo("OPERATIONS");
        assertThat(control.presencePolicy()).isEqualTo("REQUIRED");
        assertThat(control.whenAbsent()).isEqualTo("DENY");
    }

    @Test void missingVersionProfileBlocksApprovalAndNeverBecomesZeroValue() {
        setup("DEPOSIT", "CURRENT_ACCOUNT", true, 0);
        var result = governance.report(55);
        assertThat(result.canApprove()).isFalse();
        assertThat(result.effectiveRules()).anyMatch(rule -> rule.code().equals("DEPOSIT_PRODUCT_PROFILE")
                && rule.blocking() && rule.status().equals("MISSING_REQUIRED"));
    }

    @Test void disablingTransactionCapabilityPreservesHistoryAndRemovesItsPresenceGate() {
        setup("DEPOSIT", "CURRENT_ACCOUNT", false, 1);
        var result = governance.report(55);
        assertThat(result.canApprove()).isTrue();
        assertThat(result.canOpen()).isFalse(); // Disabling a UI module cannot enforce runtime transaction deny.
        assertThat(result.effectiveRules()).anyMatch(rule -> rule.code().equals("DEPOSIT_PRODUCT_TRANSACTION_RULE")
                && rule.status().equals("NOT_APPLICABLE") && rule.source().equals("MODULE_DISABLED"));
    }

    @Test void configuredRulesDoNotClaimOperationalCashWithdrawalEnforcement() {
        setup("DEPOSIT", "CURRENT_ACCOUNT", true, 1);
        var result = governance.report(55);
        assertThat(result.canApprove()).isTrue();
        assertThat(result.canOpen()).isFalse();
        assertThat(result.effectiveRules()).anyMatch(rule -> rule.code().equals("DEPOSIT_PRODUCT_TRANSACTION_RULE")
                && rule.runtimeIntegration().equals("NOT_CONNECTED_TO_OPERATIONAL_ENGINE"));
    }

    @Test void doubleSingleRowProfileIsAConflict() {
        setup("DEPOSIT", "CURRENT_ACCOUNT", false, 2);
        var result = governance.report(55);
        assertThat(result.canApprove()).isFalse();
        assertThat(result.effectiveRules()).anyMatch(rule -> rule.code().equals("DEPOSIT_PRODUCT_PROFILE")
                && rule.status().equals("CONFLICT"));
    }

    @Test void draftEditsAreNotPublicationAndMissingControlsBlockApproval() {
        setup("DEPOSIT", "CURRENT_ACCOUNT", false, 0);
        var old = Map.<String, Object>of("VERSION_STATUS_CODE", "DRAFT", "ORIGINATION_STATUS_CODE", "CLOSED", "IS_CURRENT", 0);
        governance.assertPromotionAllowed(55, old, old);
        assertThatThrownBy(() -> governance.assertPromotionAllowed(55, old,
                Map.of("VERSION_STATUS_CODE", "APPROVED", "ORIGINATION_STATUS_CODE", "CLOSED", "IS_CURRENT", 0)))
                .isInstanceOf(ProductBuilderValidationException.class);
    }
}
