package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.DraftRequest;
import com.behsazan.corebanking.fee3.domain.Fee3Models.ItemDraft;
import com.behsazan.corebanking.fee3.domain.Fee3Models.RuleDraft;
import com.behsazan.corebanking.fee3.domain.Fee3Models.VersionDraft;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract.YearSlice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** FCE V13.8.84 financial-preview contracts; no database or banking authorization. */
class Fee3F7LateDamageLifecycleTest {
    private static final LocalDate DUE=LocalDate.of(2026,10,1);
    private static final String EXPR="amount * (rate + penaltyMargin) / 100 * yearFraction";
    private static Fee3F7DomainSimulationService.Request request(DraftRequest d,Map<String,Object> inputs){
        return new Fee3F7DomainSimulationService.Request(d,inputs,DUE);
    }
    private static DraftRequest late(String holiday,String formula){
        var policy=Map.<String,Object>of("dayCountBasis","JALALI_ACT_ACT","holidayPolicy",holiday,
            "businessCalendarRef","IR_BANK_1405");
        var ctx=Map.<String,Object>of("loanPolicy",policy,"loanContractTypes",List.of("MURABAHA"));
        var v=new VersionDraft(LocalDate.of(2026,1,1),null,"BANK_INTERNAL",null,
            "FORMULA","UNREVIEWED","HALF_UP",null,Map.of(),"IRR","IRR",ctx);
        var r=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","REQUEST",null,null,null,null,null,null,null,
            Map.of("_fceEditableExpressionV1",formula));
        return new DraftRequest(null,null,null,null,new ItemDraft("QA","LOAN","LATE_DAMAGE",
            "LATE_DAMAGE",null,"CREDIT",null),v,null,List.of(r),List.of());
    }
    private static Map<String,Object> penalty(){
        return new HashMap<>(Map.of("amount","36500000","contractRate","18",
          "penaltyMargin","2","contractReference","LN101","rateEvidenceReference","RATE101",
          "penaltyPolicyReference","PEN101","loanContractType","MURABAHA",
          "dueDate","2026-10-01","penaltyUntilExclusive","2026-10-05"));
    }
    private static List<YearSlice> year(LocalDate start,LocalDate end){
        return List.of(new YearSlice(1405,start,end,(int)java.time.temporal.ChronoUnit.DAYS.between(start,end),
            365,"CAL2:TEST:Y1405"));
    }
    private static List<Fee3F7LateDamageContract.Workday> work(boolean... days){
        List<Fee3F7LateDamageContract.Workday> rows=new ArrayList<>();
        for(int i=0;i<days.length;i++)rows.add(new Fee3F7LateDamageContract.Workday(
            DUE.plusDays(i),days[i],"CAL2_TEST"));
        return rows;
    }
    private static DraftRequest guarantee(String operation,String method,String source,String rate){
        var v=new VersionDraft(LocalDate.of(2026,1,1),null,"BANK_INTERNAL",null,
            method,"UNREVIEWED","HALF_UP",null,Map.of(),"IRR","IRR",Map.of());
        var r=new RuleDraft(1,method,"GUARANTEE_AMOUNT",source,
            rate==null?null:new BigDecimal(rate),null,null,null,null,null,null,
            Map.of("originalConfig",Map.of("dayBasis","ACT_365")));
        return new DraftRequest(null,null,null,null,new ItemDraft("QA","GUARANTEE",operation,
            "GUARANTEE_FEE",null,"GUARANTEE",null),v,null,List.of(r),List.of());
    }
    private static Map<String,Object> guaranteeEvidence(){
        return new HashMap<>(Map.of("guaranteeReference","G101",
          "lifecycleApprovalReference","APP101","regulatoryReference","REG101",
          "sourceEvidenceReference","SOURCE101","feeBasisReference","BASIS101"));
    }
    @Test void lateDamageUsesCal2YearDenominatorAndNeverPosts(){
        var request=request(late("CHARGE_ALL_DAYS",EXPR),penalty());
        var result=Fee3F7LateDamageContract.calculate(request,Fee3F7LateDamageContract.validate(request),
            year(DUE.plusDays(1),DUE.plusDays(4)),List.of());
        assertEquals("PRETAX_ONLY",result.status());assertEquals("60000",result.amountIrr());
        assertNull(result.taxIrr());assertFalse(result.persisted());assertFalse(result.financialPosting());
    }
    @Test void lateDamageRejectsUnlistedExpression(){
        assertThrows(IllegalArgumentException.class,()->Fee3F7LateDamageContract.validate(
            request(late("CHARGE_ALL_DAYS","amount*999"),penalty())));
    }
    @Test void lateDamageRejectsUndecidedHolidayPolicy(){
        assertThrows(IllegalArgumentException.class,()->Fee3F7LateDamageContract.validate(
            request(late("UNDECIDED",EXPR),penalty())));
    }
    @Test void lateDamageCountsOnlyBusinessDaysWithCompleteEvidence(){
        var request=request(late("EXCLUDE_ALL_HOLIDAYS",EXPR),penalty());
        var validated=Fee3F7LateDamageContract.validate(request);
        var slices=year(DUE.plusDays(1),DUE.plusDays(4));
        assertEquals("40000",Fee3F7LateDamageContract.calculate(request,validated,slices,
            work(true,false,true,true)).amountIrr());
        assertThrows(IllegalArgumentException.class,()->Fee3F7LateDamageContract.calculate(request,
            validated,slices,work(true,false)));
    }
    @Test void lateDamageRollsNonbusinessDueDateWithoutUserHolidayInput(){
        var request=request(late("ROLL_DUE_TO_WORKDAY",EXPR),penalty());
        var result=Fee3F7LateDamageContract.calculate(request,Fee3F7LateDamageContract.validate(request),
            year(DUE.plusDays(3),DUE.plusDays(4)),work(false,false,true,true,true));
        assertEquals("20000",result.amountIrr());
    }
    @Test void guaranteeChangeUsesOnlyDocumentedIncrease(){
        var input=guaranteeEvidence();input.put("feeBasisPolicy","INCREASE_ONLY");
        input.put("previousAmount","20000000");input.put("newAmount","30000000");
        var result=Fee3F7GuaranteeLifecyclePreview.simulate(request(guarantee(
            "GUARANTEE_CHANGE","PERCENT","RULE","1"),input));
        assertEquals("PRETAX_ONLY",result.status());assertEquals("100000",result.amountIrr());
        assertFalse(result.approved());
    }
    @Test void guaranteeUndocumentedChangeBasisIsBlocked(){
        var input=guaranteeEvidence();input.put("feeBasisPolicy","UNDECIDED");
        input.put("previousAmount","20000000");input.put("newAmount","30000000");
        assertEquals("BLOCKED",Fee3F7GuaranteeLifecyclePreview.simulate(request(guarantee(
            "GUARANTEE_CHANGE","PERCENT","RULE","1"),input)).status());
    }
    @Test void guaranteeRenewalUsesIndependentTenDayPeriod(){
        var input=guaranteeEvidence();input.put("feeBasisPolicy","RENEWAL_EXTENSION");
        input.put("amount","36500000");input.put("previousEndExclusive","2026-10-02");
        input.put("newEndExclusive","2026-10-12");input.put("periodStart","2026-10-02");
        input.put("periodEndExclusive","2026-10-12");
        var request=request(guarantee("GUARANTEE_RENEW","DAILY","RULE","10"),input);
        assertEquals("100000",Fee3F7GuaranteeLifecyclePreview.simulate(request).amountIrr());
        input.put("periodStart","2026-10-03");
        assertEquals("BLOCKED",Fee3F7GuaranteeLifecyclePreview.simulate(request).status());
    }
}
