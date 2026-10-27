package com.behsazan.corebanking.fee3.r4b1;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Maven regression: unapproved, read-only service-fee golden cases, no repository/Oracle writes. */
class Fee3R4B1ServiceContractTest {
    static final LocalDate BANK_DAY=LocalDate.of(2026,10,27);
    static Map<String,Object> row(Object... pairs){
        Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;
    }
    static Fee3R4B1ServiceContract.Request request(String method,String amount,List<Map<String,Object>> rows){
        Map<String,Object> config=new LinkedHashMap<>();
        config.put("basisExplicit",true);config.put("basisCode","REQUEST_AMOUNT");config.put("rateSource","CONFIG");
        config.put(method.equals("BRACKET")?"brackets":"tiers",rows);
        config.put(method.equals("BRACKET")?"tiers":"brackets",List.of());
        RuleDraft rule=new RuleDraft(1,method,"REQUEST_AMOUNT","RULE",null,null,null,null,
            null,null,null,Map.of("originalConfig",config));
        DraftRequest draft=new DraftRequest(null,null,null,null,
            new ItemDraft("Service UAT","PAYMENT","TRANSFER","SERVICE_FEE",null,"TARIFF",null),
            new VersionDraft(BANK_DAY,null,"BANK_INTERNAL",null,method,"EXEMPT","HALF_UP",
                "Read-only contract test",Map.of(),"IRR","IRR",Map.of()),
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(rule),List.of(new TestDraft("Golden",Map.of("amount",amount),null)));
        return new Fee3R4B1ServiceContract.Request(draft,Map.of("amount",amount),null);
    }
    static List<Map<String,Object>> progressive(){return List.of(
        row("upTo",10000000,"ratePercent","0.1","fixedAmount",0),
        row("upTo",null,"ratePercent","0.05","fixedAmount",0));}
    static List<Map<String,Object>> bracket(){return List.of(
        row("from",0,"to",10000000,"ratePercent",0,"fixedAmount",20000),
        row("from",10000000,"to",null,"ratePercent","0.08","fixedAmount",0));}
    @Test void sourceGoldenProgressive30Million(){
        var r=request("PROGRESSIVE","30000000",progressive());
        assertThat(Fee3R4B1ServiceContract.guard(r,BANK_DAY)).isNull();
        assertThat(Fee3R4B1ServiceContract.calculate(r).feeIrr()).isEqualByComparingTo("20000");
    }
    @Test void progressiveUsesOnlyFullOrPartialBandAmounts(){
        var r=request("PROGRESSIVE","20000000",progressive());
        var result=Fee3R4B1ServiceContract.calculate(r);
        assertThat(result.feeIrr()).isEqualByComparingTo("15000");
        assertThat(result.breakdown()).hasSize(2);
    }
    @Test void bracketBoundaryIsInclusiveAtStartExclusiveAtEnd(){
        var justBelow=request("BRACKET","9999999",bracket());
        var exact=request("BRACKET","10000000",bracket());
        assertThat(Fee3R4B1ServiceContract.calculate(justBelow).feeIrr()).isEqualByComparingTo("20000");
        assertThat(Fee3R4B1ServiceContract.calculate(exact).feeIrr()).isEqualByComparingTo("8000");
    }
    @Test void gapIsBlockedNotChargedAsZero(){
        var r=request("BRACKET","100",List.of(
            row("from",0,"to",10,"ratePercent",1,"fixedAmount",0),
            row("from",11,"to",null,"ratePercent",1,"fixedAmount",0)));
        assertThat(Fee3R4B1ServiceContract.guard(r,BANK_DAY)).isEqualTo("BRACKET_GAP_OR_OVERLAP");
    }
    @Test void unboundedLastTierRequired(){
        var r=request("PROGRESSIVE","5",List.of(row("upTo",10,"ratePercent",1,"fixedAmount",0)));
        assertThat(Fee3R4B1ServiceContract.guard(r,BANK_DAY)).isEqualTo("PROGRESSIVE_LAST_TIER_MUST_BE_OPEN");
    }
    @Test void positiveAmountMustRespectOracleScale(){
        var r=request("PROGRESSIVE","10.001",progressive());
        assertThat(Fee3R4B1ServiceContract.guard(r,BANK_DAY)).isEqualTo("EXPLICIT_POSITIVE_IRR_AMOUNT_REQUIRED");
    }
    @Test void rateMustBeExplicitAndMaxEightDecimals(){
        var r=request("PROGRESSIVE","10",List.of(row("upTo",null,
            "ratePercent","0.123456789","fixedAmount",0)));
        assertThat(Fee3R4B1ServiceContract.guard(r,BANK_DAY)).isEqualTo("TIER_RATE_OR_FIXED_INVALID");
    }
    @Test void unsupportedPerUnitMethodIsBlocked(){
        var r=request("PROGRESSIVE","10",List.of(row("upTo",null,
            "ratePercent",1,"fixedAmount",0,"calcType","UNIT_RATE")));
        assertThat(Fee3R4B1ServiceContract.guard(r,BANK_DAY)).isEqualTo("TIER_MODE_NOT_CERTIFIED");
    }
    @Test void noMutationOrApprovalClaims(){
        var r=request("BRACKET","10000000",bracket());
        var result=Fee3R4B1ServiceContract.calculate(r);
        assertThat(result.persisted()).isFalse();
        assertThat(result.financialPosting()).isFalse();
        assertThat(result.status()).isEqualTo("SIMULATED");
    }
}
