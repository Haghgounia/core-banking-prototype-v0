package com.behsazan.corebanking.fee3.r4b2;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Real Maven JUnit tests for the R4-B2 production numeric contract. */
class Fee3R4B2GuaranteeContractTest {
    static final LocalDate DAY=LocalDate.parse("2026-10-27");
    static Map<String,Object> dict(Object... args){
        Map<String,Object> result=new LinkedHashMap<>();
        for(int i=0;i<args.length;i+=2)result.put((String)args[i],args[i+1]);return result;
    }
    static Map<String,Object> row(String type,String source,Object rate,Object coverage){
        return dict("collateralType",type,"rateSource",source,"ratePercent",rate,"coveragePercent",coverage);
    }
    static Fee3R4B2GuaranteeContract.Request sample(String basis){
        var p=dict("collateralType","MIXED_PORTFOLIO","feeBasis",basis,"portfolioRules",
            List.of(row("CASH_100","EXEMPT",null,100),row("PROMISSORY_NOTE","CONFIG",2.25,120),
                row("MOVABLE_PROPERTY","CONFIG",1,100)));
        var v=new VersionDraft(DAY,null,"BANK_INTERNAL",null,"PERCENT","EXEMPT","HALF_UP",null,
            dict(),"IRR","IRR",dict("guaranteePolicy",p));
        var r=new RuleDraft(1,"PERCENT",basis,"RULE",null,null,null,null,null,null,null,
            dict("originalConfig",dict()));
        var d=new DraftRequest(null,null,null,null,
            new ItemDraft("تست ضمانت‌نامه","GUARANTEE","GUARANTEE_ISSUE","GUARANTEE_FEE",
                null,"GUARANTEE",null),v,
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(r),List.of(new TestDraft("test",dict("amount",100000000),null)));
        return new Fee3R4B2GuaranteeContract.Request(d,dict("amount",100000000,
            "portfolio",new ArrayList<>(List.of(dict("collateralType","CASH_100","sharePercent",15),
                dict("collateralType","PROMISSORY_NOTE","sharePercent",50),
                dict("collateralType","MOVABLE_PROPERTY","sharePercent",35)))),null);
    }
    @Test void portfolioAllocatedGolden(){
        var request=sample("GUARANTEE_PORTFOLIO_ALLOCATED");
        assertThat(Fee3R4B2GuaranteeContract.guard(request,DAY)).isNull();
        var result=Fee3R4B2GuaranteeContract.calculate(request);
        assertThat(result.feeIrr()).isEqualByComparingTo("1475000");
        assertThat(result.breakdown()).hasSize(3);
        assertThat(result.persisted()).isFalse();
        assertThat(result.financialPosting()).isFalse();
    }
    @Test void portfolioCoveredGolden(){
        var request=sample("GUARANTEE_PORTFOLIO_COLLATERAL_VALUE");
        assertThat(Fee3R4B2GuaranteeContract.guard(request,DAY)).isNull();
        assertThat(Fee3R4B2GuaranteeContract.calculate(request).feeIrr())
            .isEqualByComparingTo("1700000");
    }
    @Test void sharesMustBeExactlyOneHundred(){
        var request=sample("GUARANTEE_PORTFOLIO_ALLOCATED");
        var positions=(List<Map<String,Object>>)request.inputs().get("portfolio");
        positions.get(2).put("sharePercent",30);
        assertThat(Fee3R4B2GuaranteeContract.guard(request,DAY))
            .isEqualTo("PORTFOLIO_SHARES_MUST_TOTAL_100");
    }
    @Test void missingTariffRateDenied(){
        var request=sample("GUARANTEE_PORTFOLIO_ALLOCATED");
        var p=(Map<String,Object>)request.draft().version().uiContext().get("guaranteePolicy");
        var tariff=(List<Map<String,Object>>)p.get("portfolioRules");
        tariff.get(1).put("ratePercent",null);
        assertThat(Fee3R4B2GuaranteeContract.guard(request,DAY)).isEqualTo("TARIFF_RATE_INVALID");
    }
    @Test void gregorianLeapYearIsNot365Days(){
        var a=Fee3R4B2GuaranteeContract.fraction(LocalDate.parse("2028-01-01"),
            LocalDate.parse("2029-01-01"),"ACT_ACT");
        var b=Fee3R4B2GuaranteeContract.fraction(LocalDate.parse("2028-01-01"),
            LocalDate.parse("2029-01-01"),"ACT_365");
        assertThat(a).isEqualByComparingTo("1");
        assertThat(b.compareTo(BigDecimal.ONE)>0).isEqualTo(true);
    }
    @Test void noPublishingViaPreviewEvidence(){
        var req=sample("GUARANTEE_PORTFOLIO_ALLOCATED");
        var ref=new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",null,null,
            null,null,"UNSAVED",false,0);
        var result=Fee3R4B2GuaranteeContract.finish(req,DAY,ref,
            Fee3R4B2GuaranteeContract.calculate(req));
        assertThat(result.status()).isEqualTo("SIMULATED");
        assertThat(result.approved()).isFalse();
        assertThat(result.published()).isFalse();
        assertThat(result.persisted()).isFalse();
    }
}
