package com.behsazan.corebanking.fee3.r4;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/** Mandatory Maven regression for R4-A contract. No database or mutable operation. */
class Fee3R4CalculationContractTest {
    static LocalDate date=LocalDate.of(2026,10,27);
    static DraftRequest draft(String method,String rounding,RuleDraft r){
        return new DraftRequest(null,null,null,null,
            new ItemDraft("Service test","DEPOSIT","ACCOUNT_OPENING","SERVICE_FEE",null,"TARIFF",null),
            new VersionDraft(date,null,"BANK_INTERNAL",null,method,"EXEMPT",rounding,null,
                Map.of(),"IRR","IRR",Map.of()),
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(r),List.of());
    }
    static RuleDraft r(String method,String rate,String fixed){
        return new RuleDraft(1,method,"REQUEST_AMOUNT","RULE",rate==null?null:new BigDecimal(rate),
            fixed==null?null:new BigDecimal(fixed),null,null,null,null,null,Map.of());
    }
    static Fee3R4CalculationContract.Request req(DraftRequest d,Map<String,Object> inputs,LocalDate on){
        return new Fee3R4CalculationContract.Request(d,inputs,on);
    }
    @Test void halfUpVsHalfEvenAreDifferentAtRialHalf(){
        var rule=r("PERCENT","2.5",null);
        var one=draft("PERCENT","HALF_UP",rule);var two=draft("PERCENT","HALF_EVEN",rule);
        var up=Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(one,Map.of("amount",100)));
        var even=Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(two,Map.of("amount",100)));
        assertThat(up.feeIrr()).isEqualByComparingTo("3");
        assertThat(even.feeIrr()).isEqualByComparingTo("2");
    }
    @Test void fixedFeeValidTodayAndTomorrow(){
        var d=draft("FIXED","HALF_UP",r("FIXED",null,"12500"));
        assertThat(Fee3R4CalculationContract.guard(req(d,Map.of(),null),date)).isNull();
        assertThat(Fee3R4CalculationContract.guard(req(d,Map.of(),date.plusDays(1)),date)).isNull();
    }
    @Test void olderBusinessDateIsRejected(){
        var d=draft("FIXED","HALF_UP",r("FIXED",null,"12500"));
        assertThat(Fee3R4CalculationContract.guard(req(d,Map.of(),date.minusDays(1)),date))
            .isEqualTo("HISTORICAL_SIMULATION_NOT_IN_R4A");
    }
    @Test void moneyBeyondNumber22Scale2IsRejected(){
        var d=draft("FIXED","HALF_UP",r("FIXED",null,"12500.001"));
        assertThat(Fee3R4CalculationContract.guard(req(d,Map.of(),null),date))
            .isEqualTo("IRR_MONEY_EXCEEDS_NUMBER_22_2");
    }
    @Test void configuredRateMustRespectNumber18Scale8(){
        var d=draft("PERCENT","HALF_UP",r("PERCENT","1.000000001",null));
        assertThat(Fee3R4CalculationContract.guard(req(d,Map.of("amount",100),null),date))
            .isEqualTo("RATE_EXCEEDS_NUMBER_18_8");
    }
    @Test void requestMustNotClaimPartiallyPersistedReference(){
        var d=draft("FIXED","HALF_UP",r("FIXED",null,"100"));
        d=new DraftRequest(1L,null,null,null,d.item(),d.version(),d.scope(),d.rules(),d.tests());
        assertThat(Fee3R4CalculationContract.guard(req(d,Map.of(),null),date))
            .isEqualTo("PARTIAL_VERSION_REFERENCE_FORBIDDEN");
    }
    @Test void clientSnapshotHashIsDeterministicWithoutLeakingInput(){
        var d=draft("FIXED","HALF_UP",r("FIXED",null,"100"));
        var ref=new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",null,null,
            null,null,"UNSAVED",false,0);
        var first=Fee3R4CalculationContract.fingerprint(req(d,Map.of("amount",100),null),ref);
        var second=Fee3R4CalculationContract.fingerprint(req(d,Map.of("amount",100.0),null),ref);
        assertThat(first).isEqualTo(second).hasSize(64);
    }
    @Test void successfulPreviewDoesNotClaimPostingApprovalOrPersistence(){
        var d=draft("FIXED","HALF_UP",r("FIXED",null,"100"));
        var inputs=Map.<String,Object>of("amount",500);
        var calculated=Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(d,inputs));
        var ref=new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",null,null,
            null,null,"UNSAVED",false,0);
        var result=Fee3R4CalculationContract.finish(req(d,inputs,null),date,ref,calculated);
        assertThat(result.status()).isEqualTo("SIMULATED");
        assertThat(result.persisted()).isFalse();
        assertThat(result.financialPosting()).isFalse();
        assertThat(result.approved()).isFalse();
        assertThat(result.published()).isFalse();
        assertThat(result.snapshotSha256()).hasSize(64);
    }
    @Test void formulaIsOutOfR4ACertification(){
        var d=draft("FORMULA","HALF_UP",r("FORMULA",null,null));
        assertThat(Fee3R4CalculationContract.guard(req(d,Map.of(),null),date))
            .isEqualTo("METHOD_NOT_CERTIFIED_FOR_R4A");
    }
}
