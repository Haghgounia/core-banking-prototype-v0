package com.behsazan.corebanking.fee3.f1;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Deterministic test data are engineering fixtures, not a certified bank tariff. */
class Fee3F1UsageContractTest {
    static final LocalDate BANK_DAY = LocalDate.of(2026,10,28);
    static BigDecimal d(String v){return v==null?null:new BigDecimal(v);}
    static Fee3F1UsageContract.Request request(String usage,String allowance,String price,
            String min,String max,String taxMode,String taxRate){
        Map<String,Object> cfg=new LinkedHashMap<>();
        cfg.put("basisCode","REQUEST_AMOUNT");cfg.put("rateSource","CONFIG");
        if(allowance!=null)cfg.put("freeUnits",allowance);
        if(price!=null)cfg.put("unitPrice",price);
        RuleDraft rule=new RuleDraft(1,"USAGE","REQUEST_AMOUNT","RULE",null,null,null,null,
            d(min),d(max),null,Map.of("originalConfig",cfg));
        Map<String,Object> policy=new LinkedHashMap<>();
        if(taxRate!=null)policy.put("taxPercent",taxRate);
        DraftRequest draft=new DraftRequest(null,null,null,null,
            new ItemDraft("Engineering usage fixture","DIGITAL","API_USAGE","SERVICE_FEE",null,"DIGITAL",null),
            new VersionDraft(BANK_DAY,null,"BANK_INTERNAL",null,"USAGE",taxMode,"HALF_UP",
                "Read-only test",policy,"IRR","IRR",Map.of()),
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(rule),List.of(new TestDraft("Usage engineering case",Map.of("usage",usage),null)));
        return new Fee3F1UsageContract.Request(draft,Map.of("usage",usage),null);
    }
    static Fee3F1UsageContract.Request r(String usage,String free,String price){
        return request(usage,free,price,null,null,"EXEMPT",null);
    }
    static Map<String,Object> cfg(Fee3F1UsageContract.Request request){
        @SuppressWarnings("unchecked") Map<String,Object> c=(Map<String,Object>)
            request.draft().rules().get(0).parameters().get("originalConfig");return c;
    }
    static Fee3F1UsageContract.Request withInputs(Fee3F1UsageContract.Request r,Map<String,Object> inputs){
        return new Fee3F1UsageContract.Request(r.draft(),inputs,r.effectiveOn());
    }
    static Fee3F1UsageContract.Request withVersion(Fee3F1UsageContract.Request request,VersionDraft version){
        var old=request.draft();
        DraftRequest draft=new DraftRequest(old.calcItemId(),old.tariffVersionId(),
            old.expectedItemRecordVersion(),old.expectedVersionRecordVersion(),old.item(),version,
            old.scope(),old.rules(),old.tests());
        return new Fee3F1UsageContract.Request(draft,request.inputs(),request.effectiveOn());
    }
    static VersionDraft version(Fee3F1UsageContract.Request r,String origin,String tax,String round,
            LocalDate from,LocalDate to,Map<String,Object> policy){
        VersionDraft v=r.draft().version();
        return new VersionDraft(from,to,origin,v.sourceId(),v.method(),tax,round,
            v.changeReason(),policy,v.serviceCurrency(),v.feeCurrency(),v.uiContext());
    }
    static void blocked(Fee3F1UsageContract.Request r,String code){
        assertThat(Fee3F1UsageContract.guard(r,BANK_DAY)).isEqualTo(code);
        assertThat(Fee3F1UsageContract.blocked(r,BANK_DAY,code).financialPosting()).isFalse();
    }
    @Test void feeAfterFreeUnitsIsExactBigDecimal(){
        var req=r("101","100","41250");
        assertThat(Fee3F1UsageContract.guard(req,BANK_DAY)).isNull();
        var calc=Fee3F1UsageContract.calculate(req);
        assertThat(calc.feeIrr()).isEqualByComparingTo("41250");
        assertThat(calc.breakdown().get(0).get("chargedUnits")).isEqualTo(d("1"));
    }
    @Test void freeQuotaChargesNothingIfNoMinimum(){
        var req=r("3","5","9000");
        assertThat(Fee3F1UsageContract.guard(req,BANK_DAY)).isNull();
        assertThat(Fee3F1UsageContract.calculate(req).feeIrr()).isEqualByComparingTo("0");
    }
    @Test void exactlyFreeUnitsProducesZero(){
        var req=r("5","5","9000");
        assertThat(Fee3F1UsageContract.calculate(req).rawIrr()).isEqualByComparingTo("0");
    }
    @Test void floorAndCeilingAreAppliedAfterCap(){
        var a=request("4","0","1.25",null,null,"EXEMPT",null);
        var b=withVersion(a,version(a,"BANK_INTERNAL","EXEMPT","FLOOR",BANK_DAY,null,Map.of()));
        assertThat(Fee3F1UsageContract.calculate(b).feeIrr()).isEqualByComparingTo("5");
        var c=request("1","0","1.25",null,null,"EXEMPT",null);
        c=withVersion(c,version(c,"BANK_INTERNAL","EXEMPT","CEIL",BANK_DAY,null,Map.of()));
        assertThat(Fee3F1UsageContract.calculate(c).feeIrr()).isEqualByComparingTo("2");
    }
    @Test void halfEvenRoundingOnPurePositiveAmount(){
        var a=r("1","0","2.50");
        var b=withVersion(a,version(a,"BANK_INTERNAL","EXEMPT","HALF_EVEN",BANK_DAY,null,Map.of()));
        assertThat(Fee3F1UsageContract.calculate(b).feeIrr()).isEqualByComparingTo("2");
        assertThat(Fee3F1UsageContract.calculate(a).feeIrr()).isEqualByComparingTo("3");
    }
    @Test void minAndMaxAreAppliedToRawAmount(){
        var lower=request("1","0","180","900000",null,"EXEMPT",null);
        assertThat(Fee3F1UsageContract.calculate(lower).feeIrr()).isEqualByComparingTo("900000");
        var upper=request("200","0","7500",null,"150000","EXEMPT",null);
        assertThat(Fee3F1UsageContract.calculate(upper).feeIrr()).isEqualByComparingTo("150000");
    }
    @Test void taxOnlyWhenExplicitlyTaxable(){
        var a=request("3","1","100","0",null,"TAXABLE","10");
        assertThat(Fee3F1UsageContract.guard(a,BANK_DAY)).isNull();
        var calc=Fee3F1UsageContract.calculate(a);
        assertThat(calc.feeIrr()).isEqualByComparingTo("200");
        assertThat(calc.taxIrr()).isEqualByComparingTo("20");
        assertThat(calc.totalIrr()).isEqualByComparingTo("220");
    }
    @Test void unreviewedTaxIsAlwaysBlocked(){blocked(request("1","0","100",null,null,"UNREVIEWED",null),"TAX_POLICY_UNREVIEWED");}
    @Test void missingContractPriceIsAlwaysBlocked(){
        var req=request("1","0",null,null,null,"EXEMPT",null);
        cfg(req).put("priceStatus","CONTRACT_MISSING");
        blocked(req,"UNIT_PRICE_SOURCE_PENDING");
        var missing=request("1","0",null,null,null,"EXEMPT",null);
        blocked(missing,"UNIT_PRICE_MISSING_OR_INVALID");
    }
    @Test void numericInputIsRequiredAndWhole(){
        blocked(withInputs(r("1","0","20"),Map.of("usage","1.001")),"USAGE_MUST_BE_NONNEGATIVE_INTEGER");
        blocked(withInputs(r("1","0","20"),Map.of("usage","-1")),"USAGE_MUST_BE_NONNEGATIVE_INTEGER");
        blocked(withInputs(r("1","0","20"),Map.of("usage","NaN")),"USAGE_MUST_BE_NONNEGATIVE_INTEGER");
        blocked(withInputs(r("1","0","20"),Map.of("usage","1e100")),"USAGE_MUST_BE_NONNEGATIVE_INTEGER");
        blocked(withInputs(r("1","0","20"),Map.of("amount","1")),"EXPLICIT_USAGE_ONLY");
        blocked(withInputs(r("1","0","20"),Map.of("usage","1","amount","2")),"EXPLICIT_USAGE_ONLY");
    }
    @Test void allowanceMustBeExplicitNonnegativeInteger(){
        blocked(r("4","-1","12"),"FREE_UNITS_MUST_BE_NONNEGATIVE_INTEGER");
        blocked(r("4","0.2","12"),"FREE_UNITS_MUST_BE_NONNEGATIVE_INTEGER");
        blocked(request("4",null,"12",null,null,"EXEMPT",null),"FREE_UNITS_MUST_BE_NONNEGATIVE_INTEGER");
    }
    @Test void maxTwoDecimalPriceAndCapPrecision(){
        blocked(r("1","0","0.001"),"UNIT_PRICE_MISSING_OR_INVALID");
        blocked(request("1","0","1","0.001",null,"EXEMPT",null),"INVALID_MIN_MAX");
        blocked(request("1","0","1","9","8","EXEMPT",null),"INVALID_MIN_MAX");
    }
    @Test void zeroChargeWithPositiveMinimumRequiresExplicitPolicy(){
        blocked(request("0","0","180","900000",null,"EXEMPT",null),
            "MIN_FEE_FOR_FREE_OR_ZERO_UNITS_REQUIRES_POLICY");
        blocked(request("4","5","180","900000",null,"EXEMPT",null),
            "MIN_FEE_FOR_FREE_OR_ZERO_UNITS_REQUIRES_POLICY");
    }
    @Test void unknownConfigCannotSilentlyChangeTheCharge(){
        var x=r("2","0","100");cfg(x).put("discountPercent","100");
        blocked(x,"UNKNOWN_USAGE_CONFIG_FIELD");
        var y=r("2","0","100");cfg(y).put("tiers",List.of(Map.of("upTo",10)));
        blocked(y,"ADVANCED_USAGE_MODIFIERS_NOT_CERTIFIED");
        var z=r("2","0","100");cfg(z).put("timeEnabled",true);
        blocked(z,"ADVANCED_USAGE_MODIFIERS_NOT_CERTIFIED");
    }
    @Test void conflictingMinimumAndMaximumCannotBeIgnored(){
        var min=request("10","0","100","90",null,"EXEMPT",null);cfg(min).put("minOutput","91");
        blocked(min,"MINIMUM_DISAGREES_WITH_RULE");
        var max=request("10","0","100",null,"120","EXEMPT",null);cfg(max).put("maxOutput","119");
        blocked(max,"MAXIMUM_DISAGREES_WITH_RULE");
    }
    @Test void cannotInventTaxes(){
        blocked(request("2","0","100",null,null,"TAXABLE",null),"EXPLICIT_TAX_PERCENT_REQUIRED");
        blocked(request("2","0","100",null,null,"TAXABLE","101"),"EXPLICIT_TAX_PERCENT_REQUIRED");
        blocked(request("2","0","100",null,null,"EXEMPT","10"),"TAX_PERCENT_NOT_ALLOWED_FOR_NON_TAXABLE");
        var policy=r("2","0","100");
        var financial=new LinkedHashMap<String,Object>();financial.put("waiver",50);
        blocked(withVersion(policy,version(policy,"BANK_INTERNAL","EXEMPT","HALF_UP",BANK_DAY,null,financial)),
            "UNREVIEWED_FINANCIAL_POLICY");
    }
    @Test void unsafeNumericOverflowIsRejectedWithoutCapping(){
        var tooLarge=r("1000000000000000000","0","1000000000000000000");
        assertThat(Fee3F1UsageContract.guard(tooLarge,BANK_DAY)).isNull();
        assertThatThrownBy(()->Fee3F1UsageContract.calculate(tooLarge))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("RAW_FEE_EXCEEDS_SAFE_LIMIT");
    }
    @Test void cbiWithoutSourceCannotPass(){
        var x=r("2","0","100");
        blocked(withVersion(x,version(x,"CBI_MANDATORY","EXEMPT","HALF_UP",BANK_DAY,null,Map.of())),
            "CBI_SOURCE_REQUIRED");
    }
    @Test void dateRangeIsStrictAndNoHistoricalSimulation(){
        var x=r("2","0","100");
        var historic=new Fee3F1UsageContract.Request(x.draft(),x.inputs(),BANK_DAY.minusDays(1));
        blocked(historic,"HISTORICAL_PREVIEW_NOT_ALLOWED");
        var expired=withVersion(x,version(x,"BANK_INTERNAL","EXEMPT","HALF_UP",BANK_DAY.minusDays(10),BANK_DAY.minusDays(1),Map.of()));
        blocked(expired,"OUTSIDE_DRAFT_VALIDITY");
    }
    @Test void nullableOriginAndTaxModeFailClosedNotWithNpe(){
        var x=r("2","0","100");
        blocked(withVersion(x,version(x,null,"EXEMPT","HALF_UP",BANK_DAY,null,Map.of())),
            "TARIFF_ORIGIN_UNREVIEWED");
        blocked(withVersion(x,version(x,"BANK_INTERNAL",null,"HALF_UP",BANK_DAY,null,Map.of())),
            "TAX_POLICY_UNREVIEWED");
    }
    @Test void finishDeclaresNoBankActionAndFingerprintsAreDeterministic(){
        var r=r("7","2","25.50");
        var source=new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",
            null,null,null,null,"UNSAVED",false,0);
        var a=Fee3F1UsageContract.finish(r,BANK_DAY,source,Fee3F1UsageContract.calculate(r));
        var b=Fee3F1UsageContract.finish(r,BANK_DAY,source,Fee3F1UsageContract.calculate(r));
        assertThat(a.status()).isEqualTo("SIMULATED");
        assertThat(a.persisted()).isFalse();assertThat(a.financialPosting()).isFalse();
        assertThat(a.published()).isFalse();assertThat(a.approved()).isFalse();
        assertThat(a.snapshotSha256()).hasSize(64).isEqualTo(b.snapshotSha256());
        assertThat(a.roundingScale()).isZero();
        var unsafe=new Fee3R4CalculationContract.VersionReference("ACTIVE",1L,1L,1L,1L,"ACTIVE",true,1);
        assertThat(Fee3F1UsageContract.finish(r,BANK_DAY,unsafe,Fee3F1UsageContract.calculate(r)).status())
            .isEqualTo("BLOCKED");
    }
}
