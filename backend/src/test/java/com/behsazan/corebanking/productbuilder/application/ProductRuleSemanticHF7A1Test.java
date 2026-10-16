package com.behsazan.corebanking.productbuilder.application;

import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductRuleSemanticHF7A1Test {
    private final ProductBuilderBusinessValidator validator = new ProductBuilderBusinessValidator();

    private static Map<String,Object> transaction(Object permission) {
        return new LinkedHashMap<>(Map.of("TRANSACTION_TYPE_CODE","WITHDRAWAL", "IS_ALLOWED",permission));
    }
    private static Map<String,Object> media(Object permission) {
        return new LinkedHashMap<>(Map.of("WITHDRAWAL_MEDIA_CODE","CARD", "IS_ALLOWED",permission));
    }
    private void rejects(String table, Map<String,Object> row, String message) {
        assertThatThrownBy(() -> validator.validate(table,row))
                .isInstanceOf(ProductBuilderValidationException.class).hasMessageContaining(message);
    }
    @Test void permittedDepositTransactionRequiresRealCap() {
        rejects("DEPOSIT_PRODUCT_TRANSACTION_RULE", transaction(1), "حداقل یک سقف");
        var limited = transaction(1);
        limited.put("MAX_TRANSACTION_AMOUNT", 2_000_000);
        limited.put("DAILY_COUNT_LIMIT", 4);
        validator.validate("DEPOSIT_PRODUCT_TRANSACTION_RULE", limited);
    }
    @Test void explicitTransactionDenyAllowsEmptyBoundsButNotContradictoryLimits() {
        validator.validate("DEPOSIT_PRODUCT_TRANSACTION_RULE", transaction(0));
        var denied = transaction(0); denied.put("DAILY_AMOUNT_LIMIT", 30);
        rejects("DEPOSIT_PRODUCT_TRANSACTION_RULE", denied, "تراکنش غیرمجاز");
    }
    @Test void transactionAmountsAndCountMustBeConsistent() {
        var invalid = transaction(1);
        invalid.put("MIN_TRANSACTION_AMOUNT",1000);
        invalid.put("MAX_TRANSACTION_AMOUNT",500);
        rejects("DEPOSIT_PRODUCT_TRANSACTION_RULE",invalid,"حداکثر");
        invalid.put("MAX_TRANSACTION_AMOUNT",2000);
        invalid.put("DAILY_COUNT_LIMIT",2.5);
        rejects("DEPOSIT_PRODUCT_TRANSACTION_RULE",invalid,"عدد صحیح");
        invalid.put("DAILY_COUNT_LIMIT",2);
        invalid.put("DAILY_AMOUNT_LIMIT",100);
        rejects("DEPOSIT_PRODUCT_TRANSACTION_RULE",invalid,"قابل اجرا نیست");
    }
    @Test void noImplicitAllowWhenPermissionIsNullOrInvalid() {
        var incomplete = new LinkedHashMap<String,Object>();
        incomplete.put("TRANSACTION_TYPE_CODE","WITHDRAWAL");
        incomplete.put("IS_ALLOWED",null);
        rejects("DEPOSIT_PRODUCT_TRANSACTION_RULE",incomplete,"NULL مجاز نیست");
        incomplete.put("IS_ALLOWED",2);
        rejects("DEPOSIT_PRODUCT_TRANSACTION_RULE",incomplete,"بله");
    }
    @Test void withdrawalMediumNeedsCapWhenAllowedAndNoneWhenDenied() {
        rejects("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",media(1),"حداقل یک سقف");
        var allowed = media(1);allowed.put("DAILY_COUNT_LIMIT",3);
        validator.validate("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",allowed);
        validator.validate("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",media(0));
        var denied = media(0);denied.put("MAX_TRANSACTION_AMOUNT",100);
        rejects("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",denied,"غیرمجاز");
    }
    @Test void openingDoesNotEquateNullFinancialBoundsWithZeroAndValidatesStamp() {
        var opening = new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PRODUCT_OPENING_RULE"));
        validator.validate("DEPOSIT_PRODUCT_OPENING_RULE",opening);
        opening.put("IS_STAMP_DUTY_APPLICABLE",1);
        rejects("DEPOSIT_PRODUCT_OPENING_RULE",opening,"تعداد تمبر");
        opening.put("STAMP_COUNT",2);
        validator.validate("DEPOSIT_PRODUCT_OPENING_RULE",opening);
    }
    @Test void jointCountsCannotDemandMoreSignersThanMinimumOwners() {
        var joint = new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PRODUCT_JOINT_RULE"));
        joint.put("MIN_REQUIRED_SIGNER_COUNT",3);
        rejects("DEPOSIT_PRODUCT_JOINT_RULE",joint,"حداقل تعداد مالکان");
    }
    @Test void nonSupportedHoldCannotHaveLimits() {
        var hold = new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PRODUCT_HOLD_RULE"));
        hold.put("MAX_HOLD_AMOUNT", 1000);
        rejects("DEPOSIT_PRODUCT_HOLD_RULE",hold,"غیرقابل پشتیبانی");
    }
    @Test void loanContributionRequiresBothRatiosInPersistedScale() {
        var financial = new LinkedHashMap<>(ProductRuleGovernanceFixture.row("LOAN_FINANCIAL_EXTENSION"));
        financial.put("MIN_CUSTOMER_CONTRIBUTION", "0.2");
        rejects("LOAN_FINANCIAL_EXTENSION",financial,"حداقل و حداکثر");
        financial.put("MAX_CUSTOMER_CONTRIBUTION", "0.4");
        validator.validate("LOAN_FINANCIAL_EXTENSION",financial);
        financial.put("MAX_CUSTOMER_CONTRIBUTION", "40");
        rejects("LOAN_FINANCIAL_EXTENSION",financial,"صفر تا یک");
    }
    @Test void loanGraceAndInstallmentIntervalMustHaveMeaningfulDuration() {
        var rule = new LinkedHashMap<>(ProductRuleGovernanceFixture.row("LOAN_PRODUCT_REPAYMENT_RULE"));
        rule.put("GRACE_ALLOWED",1);
        rule.put("GRACE_METHOD_CODE","NO_PAYMENT");
        rule.put("GRACE_UNIT_CODE","MONTH");
        rejects("LOAN_PRODUCT_REPAYMENT_RULE",rule,"حد مدت");
        rule.put("MAX_GRACE_DURATION",3);
        validator.validate("LOAN_PRODUCT_REPAYMENT_RULE",rule);
        rule.put("INSTALLMENT_INTERVAL",1);
        rejects("LOAN_PRODUCT_REPAYMENT_RULE",rule,"فاصله اقساط و واحد");
    }
    @Test void loanProcessRejectsConflictingApprovals() {
        var process = new LinkedHashMap<>(ProductRuleGovernanceFixture.row("LOAN_PRODUCT_PROCESS_RULE"));
        process.put("MANUAL_APPROVAL_REQUIRED",1);
        process.put("COMMITTEE_APPROVAL_REQUIRED",1);
        rejects("LOAN_PRODUCT_PROCESS_RULE",process,"باید یکتا باشد");
    }
    @Test void mediaRulesWithEqualKeyOnlyConflictWhenValidityPeriodsOverlap() {
        var first = new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA"));
        first.put("VALID_FROM", "2026-10-10"); first.put("VALID_TO", "2026-10-12");
        var other = new LinkedHashMap<>(first);
        other.put("VALID_FROM", "2026-10-13"); other.put("VALID_TO", "2026-10-20");
        var start = java.time.LocalDate.parse("2026-10-10");
        var end = java.time.LocalDate.parse("2026-10-31");
        assertThat(ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",
                List.of(first,other),start,end).conflict()).isFalse();
        other.put("VALID_FROM", "2026-10-12");
        assertThat(ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",
                List.of(first,other),start,end).conflict()).isTrue();
    }
    @Test void semanticAuditBlocksApprovalOfLegacyIncompleteTransactionWithoutRewritingDatabase() {
        var repo = mock(PdlProductBuilderRepository.class);
        var lite = mock(RuleGovernanceLiteService.class);
        var governance = new ProductRuleGovernanceService(repo,lite,validator);
        when(repo.findById("PRODUCT_VERSION",55L)).thenReturn(Optional.of(Map.of(
                "PRODUCT_ID",9L,"VALID_FROM","2026-10-10","VERSION_STATUS_CODE","DRAFT")));
        when(repo.findById("PRODUCT",9L)).thenReturn(Optional.of(Map.of(
                "PRODUCT_CLASS_CODE","DEPOSIT","PRODUCT_FAMILY_CODE","CURRENT_ACCOUNT","PRODUCT_STATUS_CODE","DRAFT","DEFAULT_CURRENCY_CODE","IRR")));
        when(repo.findById("PRODUCT", 99L)).thenReturn(Optional.of(Map.of("PRODUCT_ID", 99L)));
        when(repo.findById("LOAN_TYPE", 101L)).thenReturn(Optional.of(Map.of("LOAN_GROUP", 1, "RECORD_STATUS_CODE", "ACTIVE")));
        when(repo.findById("PLAN_TYPE", 201L)).thenReturn(Optional.of(Map.of("RECORD_STATUS_CODE", "ACTIVE")));
        when(repo.loanTypePlanAllowed(101L, 201L)).thenReturn(true);
        when(repo.search(anyString(),isNull(),eq(0),anyInt(),anyString(),anyString())).thenAnswer(call -> {
            String table = call.getArgument(0);
            // HF6: optional child rows are absent in this historical fixture;
            // an empty child set is not a fabricated operational permission.
            if ("PRODUCT_CHANNEL_OPERATION".equals(table) || "LOAN_ELIGIBILITY_EXTENSION".equals(table)
                    || "DEPOSIT_PRODUCT_CLOSURE_PRECHECK".equals(table)
                    || "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE".equals(table)
                    || "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE".equals(table)
                    || "PRODUCT_ELIGIBILITY_CRITERION".equals(table)
                    || "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE".equals(table))
                return new TablePage(List.of(), 0, 0, 200);
            if ("PRODUCT_VERSION_MODULE".equals(table)) return new TablePage(List.of(),0,0,200);
            var example = new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table));
            if ("DEPOSIT_PRODUCT_TRANSACTION_RULE".equals(table)) {
                example.put("IS_ALLOWED",1); // Previously persisted with no caps.
            }
            return new TablePage(List.of(example),1,0,200);
        });
        var report = governance.report(55);
        assertThat(report.canApprove()).isFalse();
        assertThat(report.canOpen()).isFalse();
        assertThat(report.effectiveRules()).anyMatch(r -> r.code().equals("DEPOSIT_PRODUCT_TRANSACTION_RULE")
                && r.status().equals("INVALID_SEMANTICS") && r.blocking()
                && r.reason().contains("حداقل یک سقف"));
        verify(repo,never()).update(anyString(),anyLong(),anyMap(),anyString());
        verify(repo,never()).insert(anyString(),anyMap(),anyString());
    }
}
