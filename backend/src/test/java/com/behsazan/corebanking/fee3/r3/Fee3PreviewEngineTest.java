package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class Fee3PreviewEngineTest {
    private static BigDecimal b(String value){return new BigDecimal(value);}
    private static DraftRequest fixture(String method,String amountKind,RuleDraft rule,Map<String,Object> context){
        boolean guarantee="GUARANTEE_FEE".equals(amountKind);
        return new DraftRequest(null,null,null,null,
           new ItemDraft("UAT test",guarantee?"GUARANTEE":"DEPOSIT",
               guarantee?"GUARANTEE_ISSUE":"ACCOUNT_OPENING",amountKind,null,"TARIFF",null),
           new VersionDraft(LocalDate.of(2026,10,26),null,"BANK_INTERNAL",null,method,
               "EXEMPT","HALF_UP",null,Map.of(),"IRR","IRR",context),
           new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
           List.of(rule),List.of());
    }
    @Test void fixedFeeIsOnlySimulated(){
        var rule=new RuleDraft(1,"FIXED","REQUEST_AMOUNT","RULE",null,b("25000"),null,null,null,null,null,Map.of());
        var result=Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(
            fixture("FIXED","SERVICE_FEE",rule,Map.of()),Map.of()));
        assertThat(result.status()).isEqualTo("SIMULATED");
        assertThat(result.totalIrr()).isEqualByComparingTo("25000");
        assertThat(result.financialPosting()).isFalse();
        assertThat(result.persisted()).isFalse();
    }
    @Test void missingContractRateIsBlocked(){
        var rule=new RuleDraft(1,"PERCENT","REQUEST_AMOUNT","REQUEST",null,null,null,null,null,null,null,Map.of());
        var result=Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(
            fixture("PERCENT","SERVICE_FEE",rule,Map.of()),Map.of("amount",1000000)));
        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.totalIrr()).isNull();
    }
    @Test void unrestrictedFormulaExecutionIsNeverAttempted(){
        var rule=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","RULE",null,null,null,null,null,null,null,Map.of());
        var result=Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(
            fixture("FORMULA","SERVICE_FEE",rule,Map.of()),Map.of()));
        assertThat(result.status()).isEqualTo("BLOCKED");
    }
    @Test void checklistNeverLabelsDraftAsApproved(){
        var rule=new RuleDraft(1,"FIXED","REQUEST_AMOUNT","RULE",null,b("25000"),null,null,null,null,null,Map.of());
        var report=Fee3SubmissionPolicy.check(fixture("FIXED","SERVICE_FEE",rule,Map.of()));
        assertThat(report.submissionReady()).isFalse();
        assertThat(report.approvalReady()).isFalse();
    }
    @Test void makerAndCheckerMustBeDifferent(){
        assertThat(Fee3SubmissionPolicy.differentActors("operator1","OPERATOR1")).isFalse();
        assertThat(Fee3SubmissionPolicy.differentActors("operator1","operator2")).isTrue();
    }
}
