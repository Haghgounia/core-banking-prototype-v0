package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RuleGovernanceLiteR20Test {
    private final PdlProductBuilderRepository repo = mock(PdlProductBuilderRepository.class);
    private final RuleGovernanceLiteService policy = mock(RuleGovernanceLiteService.class);
    private final ProductRuleGovernanceService governance = new ProductRuleGovernanceService(repo, policy);

    private void setup(String missingAction, boolean bound) {
        when(repo.findById("PRODUCT_VERSION",55L)).thenReturn(Optional.of(bound ? Map.of(
                "PRODUCT_VERSION_ID",55,"PRODUCT_ID",9,"VERSION_STATUS_CODE","DRAFT",
                "VALID_FROM","2026-10-10","RULE_POLICY_VERSION_ID",7) : Map.of(
                "PRODUCT_VERSION_ID",55,"PRODUCT_ID",9,"VERSION_STATUS_CODE","DRAFT","VALID_FROM","2026-10-10")));
        when(repo.findById("PRODUCT",9L)).thenReturn(Optional.of(Map.of(
                "PRODUCT_CLASS_CODE","DEPOSIT","PRODUCT_FAMILY_CODE","CURRENT_ACCOUNT","PRODUCT_STATUS_CODE","DRAFT")));
        when(repo.search(anyString(),isNull(),anyInt(),anyInt(),anyString(),anyString())).thenAnswer(call -> {
            String table = call.getArgument(0);
            if ("PRODUCT_VERSION_MODULE".equals(table)) return new TablePage(List.of(Map.of("MODULE_CODE","TRANSACTION","IS_ENABLED",1)),1,0,200);
            if ("DEPOSIT_PRODUCT_TRANSACTION_RULE".equals(table)) return new TablePage(List.of(),0,0,200);
            return new TablePage(List.of(Map.of("RULE_STATUS_CODE","ACTIVE","CHANNEL_CODE","BRANCH","TRANSACTION_TYPE_CODE","WITHDRAWAL","PRICING_PURPOSE_CODE","PROFIT","CURRENCY_CODE","IRR","CLOSURE_TYPE_CODE","NORMAL")),1,0,200);
        });
        if (bound) {
            when(policy.policy(7L)).thenReturn(new RuleGovernanceLiteService.Policy(7,"BASE",1,"پایه",
                    "APPROVED",LocalDate.parse("2026-01-01"),null,"maker","checker","REF",23));
            when(policy.controls(7L)).thenReturn(ProductRuleGovernanceService.defaultControls().stream().map(c ->
                    new RuleGovernanceLiteService.Control(1,c.code(),c.title(),c.governanceLevel(),c.moduleCode(),
                            c.code().equals("DEPOSIT_PRODUCT_TRANSACTION_RULE")?"REQUIRED":"OPTIONAL",
                            c.code().equals("DEPOSIT_PRODUCT_TRANSACTION_RULE")?missingAction:"IGNORE",false,null)).toList());
        }
    }

    @Test void operationBlockDoesNotPretendPublicationMustFail() {
        setup("BLOCK_OPERATION",true);
        var review=governance.report(55);
        assertThat(review.canApprove()).isTrue();
        assertThat(review.canOpen()).isFalse();
        assertThat(review.effectiveRules()).anyMatch(r -> "DEPOSIT_PRODUCT_TRANSACTION_RULE".equals(r.code()) && r.blocking());
    }

    @Test void publishBlockPreventsApproval() {
        setup("BLOCK_PUBLISH",true);
        assertThat(governance.report(55).canApprove()).isFalse();
    }

    @Test void nullBindingFollowsLegacyR19GovernanceAndDoesNotReadPolicyTables() {
        setup("IGNORE",false);
        assertThat(governance.report(55).canApprove()).isFalse(); // R19: transaction required
        verifyNoInteractions(policy);
    }
}
