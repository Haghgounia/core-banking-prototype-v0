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

/** HF7-A3 Final/HF8: persisted publication evidence, not runtime qualification. */
class ProductRulePublicationHF7A3HF8Test {
    private static final Set<String> OPTIONAL_CHILDREN = Set.of(
            "PRODUCT_CHANNEL_OPERATION", "LOAN_ELIGIBILITY_EXTENSION",
            "DEPOSIT_PRODUCT_CLOSURE_PRECHECK", "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",
            "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE", "PRODUCT_ELIGIBILITY_CRITERION",
            "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE");

    private static Map<String, Object> m(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i+1]);
        return result;
    }

    private static class Case {
        final PdlProductBuilderRepository repository = mock(PdlProductBuilderRepository.class);
        final ProductRuleGovernanceService governance = new ProductRuleGovernanceService(
                repository, mock(RuleGovernanceLiteService.class), new ProductBuilderBusinessValidator());
        final Map<String, List<Map<String, Object>>> records = new LinkedHashMap<>();
        final Map<String,Object> product;
        final Map<String,Object> loanType = m("LOAN_GROUP", 1, "RECORD_STATUS_CODE", "ACTIVE");
        String failedReference;
        boolean typePlanAllowed = true;
        boolean typePlanLookupFailure = false;
        Case(String domain, String family) {
            product = m("PRODUCT_ID", 9L, "PRODUCT_CLASS_CODE", domain,
                    "PRODUCT_FAMILY_CODE", family, "PRODUCT_STATUS_CODE", "DRAFT", "DEFAULT_CURRENCY_CODE", "IRR");
            when(repository.findById("PRODUCT_VERSION", 55L)).thenReturn(Optional.of(m(
                    "PRODUCT_ID", 9L, "VALID_FROM", "2026-10-10", "VALID_TO", "2026-10-30", "VERSION_STATUS_CODE", "DRAFT")));
            when(repository.findById("PRODUCT", 9L)).thenReturn(Optional.of(product));
            when(repository.findById("PRODUCT", 99L)).thenAnswer(x -> reference("PRODUCT", m("PRODUCT_ID", 99L)));
            when(repository.findById("PRODUCT_VERSION", 66L)).thenAnswer(x -> reference("PRODUCT_VERSION", m("PRODUCT_ID", 99L)));
            when(repository.findById("LOAN_TYPE", 101L)).thenAnswer(x -> reference("LOAN_TYPE", loanType));
            when(repository.findById("PLAN_TYPE", 201L)).thenAnswer(x -> reference("PLAN_TYPE", m("RECORD_STATUS_CODE", "ACTIVE")));
            when(repository.loanTypePlanAllowed(101L, 201L)).thenAnswer(x -> { if (typePlanLookupFailure) throw new IllegalStateException("read-only Oracle failure"); return typePlanAllowed; });
            when(repository.search(anyString(), isNull(), anyInt(), anyInt(), anyString(), anyString()))
                    .thenAnswer(call -> {
                        String table = call.getArgument(0);
                        int page = call.getArgument(2), size = call.getArgument(3);
                        List<Map<String,Object>> data = "PRODUCT_VERSION_MODULE".equals(table)
                                || OPTIONAL_CHILDREN.contains(table) ? List.of()
                                : List.of(new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table)));
                        if (records.containsKey(table)) data = records.get(table);
                        int first = Math.min(page * size, data.size());
                        int last = Math.min(first + size, data.size());
                        return new TablePage(data.subList(first,last), data.size(), page, size);
                    });
        }
        Optional<Map<String,Object>> reference(String name,Map<String,Object> row) {
            return name.equals(failedReference) ? Optional.empty() : Optional.of(row);
        }
        Case put(String table, Map<String,Object> record) { records.put(table, new ArrayList<>(List.of(record))); return this; }
        Case putAll(String table, Map<String,Object> a, Map<String,Object> b) {
            records.put(table, new ArrayList<>(List.of(a,b))); return this;
        }
        ProductRuleGovernanceService.GovernanceReport report() { return governance.report(55); }
        void noWrites() {
            verify(repository, never()).insert(anyString(), anyMap(), anyString());
            verify(repository, never()).update(anyString(), anyLong(), anyMap(), anyString());
        }
    }
    private static Case deposit() { return new Case("DEPOSIT", "CURRENT_ACCOUNT"); }
    private static Case loan() { return new Case("LOAN", "QARD_HASAN_LOAN"); }
    private static Case term() {
        Case c = new Case("DEPOSIT", "LONG_TERM_DEPOSIT");
        c.put("DEPOSIT_PRODUCT_PROFILE", m("PRODUCT_VERSION_ID",55L,
                "DEPOSIT_GROUP_CODE","LONG_TERM", "DEPOSIT_TYPE_CODE","LONG_TERM_DEPOSIT"));
        return c;
    }
    private static void blocked(Case c, String control, String keyword) {
        var report = c.report();
        assertThat(report.canApprove()).isFalse();
        assertThat(report.canOpen()).isFalse();
        assertThat(report.effectiveRules()).anyMatch(x -> control.equals(x.code())
                && "INVALID_SEMANTICS".equals(x.status()) && x.blocking()
                && x.reason().contains(keyword));
        c.noWrites();
    }

    @Test void validDepositIdentityRemainsDesignOnly() {
        var c=deposit();
        assertThat(c.report().canApprove()).isTrue();
        assertThat(c.report().canOpen()).isFalse();
        c.noWrites();
    }
    @Test void depositIdentityMismatchBlocksApproval() {
        var c=deposit();
        var broken=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PRODUCT_PROFILE"));
        broken.put("DEPOSIT_TYPE_CODE", "SAVINGS");
        c.put("DEPOSIT_PRODUCT_PROFILE",broken);
        blocked(c,"DEPOSIT_PRODUCT_PROFILE","خانواده");
        assertThatThrownBy(() -> c.governance.assertPromotionAllowed(55,
                Map.of("VERSION_STATUS_CODE","DRAFT"), Map.of("VERSION_STATUS_CODE","APPROVED")))
                .isInstanceOf(ProductBuilderValidationException.class);
    }
    @Test void profileWithDifferentVersionIsNotApprovalEvidence() {
        var c=deposit();
        var broken=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PRODUCT_PROFILE"));
        broken.put("PRODUCT_VERSION_ID", 66L);
        c.put("DEPOSIT_PRODUCT_PROFILE",broken);
        blocked(c,"DEPOSIT_PRODUCT_PROFILE","نسخه");
    }
    @Test void validTermProfitScheduleIsAllowedForDesign() {
        var c=term();
        assertThat(c.report().canApprove()).isTrue();
        assertThat(c.report().canOpen()).isFalse();
        c.noWrites();
    }
    @Test void invalidProfitScheduleOptionOrMissingFlagBlocks() {
        var c=term();
        var broken=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PROFIT_PAYMENT_RULE"));
        broken.put("FIRST_PAYMENT_RULE_CODE","NOT_A_RULE");
        c.put("DEPOSIT_PROFIT_PAYMENT_RULE",broken);
        blocked(c,"DEPOSIT_PROFIT_PAYMENT_RULE","اولین پرداخت");
        broken.remove("CAPITALIZATION_DEFAULT");
        broken.put("FIRST_PAYMENT_RULE_CODE","PRORATE_FROM_OPENING");
        blocked(c,"DEPOSIT_PROFIT_PAYMENT_RULE","صریحاً");
    }
    @Test void profitDefaultCannotExceedPermission() {
        var c=term();
        var broken=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PROFIT_PAYMENT_RULE"));
        broken.put("CAPITALIZATION_DEFAULT",1);
        c.put("DEPOSIT_PROFIT_PAYMENT_RULE",broken);
        blocked(c,"DEPOSIT_PROFIT_PAYMENT_RULE","سرمایه");
    }
    @Test void validLoanProfileIsDesignOnly() {
        var c=loan();
        assertThat(c.report().canApprove()).isTrue();
        assertThat(c.report().canOpen()).isFalse();
        c.noWrites();
    }
    @Test void loanCurrencyAndReferenceGroupRecheckedOnApproval() {
        var c=loan();
        var bad=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("LOAN_PRODUCT_PROFILE"));
        bad.put("CURRENCY_TYPE_CODE","USD");
        c.put("LOAN_PRODUCT_PROFILE",bad);
        blocked(c,"LOAN_PRODUCT_PROFILE","ارز");
        bad.put("CURRENCY_TYPE_CODE","IRR");
        c.loanType.put("LOAN_GROUP",2);
        blocked(c,"LOAN_PRODUCT_PROFILE","خانواده");
    }
    @Test void loanTypeAndPlanMappingRequireValidReadOnlyEvidence() {
        var c=loan();
        c.typePlanAllowed=false;
        blocked(c,"LOAN_PRODUCT_PROFILE","طرح");
        c.typePlanAllowed=true; c.failedReference="LOAN_TYPE";
        blocked(c,"LOAN_PRODUCT_PROFILE","مرجع");
    }
    @Test void inactiveLoanTypeCannotApprovePersistedProfile() {
        var c=loan();
        c.loanType.put("RECORD_STATUS_CODE","INACTIVE");
        blocked(c,"LOAN_PRODUCT_PROFILE","فعال نیست");
    }
    @Test void missingPlanReferenceBlocksPublication() {
        var c=loan();
        c.failedReference="PLAN_TYPE";
        blocked(c,"LOAN_PRODUCT_PROFILE","مرجع");
    }
    @Test void failedLoanTypePlanLookupIsAnExplicitPublishBlock() {
        var c=loan();
        c.typePlanLookupFailure=true;
        blocked(c,"LOAN_PRODUCT_PROFILE","استعلام خواندنی");
    }
    @Test void relationshipTargetRequiresOtherExistingProduct() {
        var c=deposit();
        assertThat(c.report().canApprove()).isTrue();
        var bad=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("PRODUCT_RELATIONSHIP"));
        bad.put("TARGET_PRODUCT_ID",9L);
        c.put("PRODUCT_RELATIONSHIP",bad);
        blocked(c,"PRODUCT_RELATIONSHIP","مبدأ");
        bad.put("TARGET_PRODUCT_ID",99L);
        c.failedReference="PRODUCT";
        blocked(c,"PRODUCT_RELATIONSHIP","مرجع");
    }
    @Test void relationshipTargetVersionOwnershipRechecked() {
        var c=deposit();
        var rel=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("PRODUCT_RELATIONSHIP"));
        rel.put("TARGET_PRODUCT_VERSION_ID",66L);
        c.put("PRODUCT_RELATIONSHIP",rel);
        assertThat(c.report().canApprove()).isTrue();
        rel.put("SOURCE_PRODUCT_VERSION_ID",44L);
        blocked(c,"PRODUCT_RELATIONSHIP","نسخه");
        rel.put("SOURCE_PRODUCT_VERSION_ID",55L);
        c.failedReference="PRODUCT_VERSION";
        blocked(c,"PRODUCT_RELATIONSHIP","مرجع");
    }
    @Test void exactDuplicateRelationshipIsConflictButDisjointIsAllowed() {
        var c=deposit();
        var a=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("PRODUCT_RELATIONSHIP"));
        var b=new LinkedHashMap<>(a);
        a.put("VALID_FROM","2026-10-10");a.put("VALID_TO","2026-10-15");
        b.put("VALID_FROM","2026-10-15");b.put("VALID_TO","2026-10-30");
        c.putAll("PRODUCT_RELATIONSHIP",a,b);
        assertThat(c.report().canApprove()).isFalse();
        assertThat(c.report().effectiveRules()).anyMatch(x -> x.code().equals("PRODUCT_RELATIONSHIP")
                && x.status().equals("CONFLICT"));
        b.put("VALID_FROM","2026-10-16");
        assertThat(c.report().canApprove()).isTrue();
        c.noWrites();
    }
}
