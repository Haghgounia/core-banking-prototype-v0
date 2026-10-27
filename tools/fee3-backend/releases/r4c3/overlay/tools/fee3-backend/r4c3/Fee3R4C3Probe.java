import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4c3.Fee3R4C3QardContract;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Fee3R4C3Probe {
    static int yes=0,no=0;
    static void check(String name,boolean ok){
        System.out.println((ok?"PASS":"FAIL")+" | "+name);
        if(ok)yes++;else no++;
    }
    static final LocalDate EVENT=LocalDate.parse("2026-03-21");
    static final LocalDate ORIGIN=LocalDate.parse("2025-03-21");
    static final String CAL="CAL2:PERSIAN:101:201";
    static final Fee3R4C3QardContract.EventDay ORIGIN_DAY=
        new Fee3R4C3QardContract.EventDay(ORIGIN,1404,1,1,1,365,"CAL2:PERSIAN:101:200");
    static final Fee3R4C3QardContract.EventDay EVENT_DAY=
        new Fee3R4C3QardContract.EventDay(EVENT,1405,1,1,1,365,CAL);
    static List<Fee3R4C1LoanProfitContract.YearSlice> days(){
        return List.of(new Fee3R4C1LoanProfitContract.YearSlice(1405,EVENT,EVENT.plusDays(10),10,365,CAL));
    }
    static Fee3R4C3QardContract.Request request(String model){
        var s=Fee3R4C3QardContract.SPECS.get(model);
        var inputs=new LinkedHashMap<String,Object>();
        inputs.put("contractReference","UAT-LOAN-CONTRACT-A");
        inputs.put("contractEvidenceReference","UAT-CONTRACT-EVIDENCE");
        inputs.put("rateEvidenceReference","UAT-RATE-EVIDENCE");
        inputs.put("scheduleEvidenceReference","UAT-SCHEDULE-EVIDENCE");
        inputs.put("loanContractStartDate",ORIGIN.toString());
        inputs.put("periodStart",EVENT.toString());
        inputs.put("periodEndExclusive",EVENT.plusDays(10).toString());
        inputs.put("eventDate",EVENT.toString());
        inputs.put("amount","100000000.00");
        inputs.put("contractRate","4.00000000");
        if("FIRST_INSTALLMENT_OF_PERIOD".equals(model))
            inputs.put("firstInstallmentDate",EVENT.toString());
        var item=new ItemDraft("Qard R4C3 UAT","LOAN","QARD_FEE","QARD_FEE",null,"CREDIT",null);
        var policy=Map.<String,Object>of("contractEvidenceRequired",true,"contractInputsRequired",true,
            "dayCountBasis","JALALI_ACT_ACT","holidayPolicy","CHARGE_ALL_DAYS",
            "qardChargeTiming",s.qardChargeTiming(),"yearStartBasis",s.yearStartBasis());
        var ver=new VersionDraft(EVENT,null,"BANK_INTERNAL",null,"FORMULA","OUT_OF_SCOPE",
            "HALF_UP","Only UAT",Map.of(),"IRR","IRR",
            Map.of("loanModelProfile",model,"loanContractTypes",List.of("QARD_HASANA"),"loanPolicy",policy));
        var rule=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","REQUEST",null,null,null,
            null,null,null,"TPL_E624746F262F6E",Map.of());
        var draft=new DraftRequest(null,null,null,null,item,ver,
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),
                List.of("QARD_HASANA"),"IRR"),List.of(rule),
            List.of(new TestDraft("UAT only",Map.of("amount","100000000"),null)));
        return new Fee3R4C3QardContract.Request(draft,inputs,null);
    }
    static Fee3R4C3QardContract.Request mutate(String m,String key,Object val){
        var old=request(m);var inputs=new LinkedHashMap<>(old.inputs());
        if(val==null)inputs.remove(key);else inputs.put(key,val);
        return new Fee3R4C3QardContract.Request(old.draft(),inputs,null);
    }
    static Fee3R4C3QardContract.Request changePolicy(String m,String key,Object val){
        var old=request(m);var v=old.draft().version();
        var ctx=new LinkedHashMap<>(v.uiContext());
        var pol=new LinkedHashMap<>((Map<String,Object>)ctx.get("loanPolicy"));
        pol.put(key,val);ctx.put("loanPolicy",pol);
        var newV=new VersionDraft(v.validFrom(),v.validTo(),v.origin(),v.sourceId(),
            v.method(),v.taxMode(),v.rounding(),v.changeReason(),v.financialPolicy(),
            v.serviceCurrency(),v.feeCurrency(),ctx);
        var d=old.draft();var nd=new DraftRequest(d.calcItemId(),d.tariffVersionId(),
            d.expectedItemRecordVersion(),d.expectedVersionRecordVersion(),d.item(),newV,
            d.scope(),d.rules(),d.tests());
        return new Fee3R4C3QardContract.Request(nd,old.inputs(),null);
    }
    static String errorOf(Fee3R4C3QardContract.Request r,
                          List<Fee3R4C1LoanProfitContract.YearSlice> slices,
                          Fee3R4C3QardContract.EventDay first,
                          Fee3R4C3QardContract.EventDay second){
        try{Fee3R4C3QardContract.calculate(r,slices,first,second);return null;}
        catch(IllegalArgumentException ex){return ex.getMessage();}
    }
    public static void main(String[] args){
        for(var model:Fee3R4C3QardContract.SPECS.keySet()){
            var r=request(model);
            check(model+" guard",Fee3R4C3QardContract.guard(r,EVENT)==null);
            var result=Fee3R4C3QardContract.calculate(r,days(),ORIGIN_DAY,EVENT_DAY);
            check(model+" amount",result.roundedIrr().compareTo(new BigDecimal("109589"))==0);
            check(model+" exact factor",result.yearFraction().compareTo(
                new BigDecimal(10).divide(new BigDecimal(365),java.math.MathContext.DECIMAL128))==0);
            check(model+" trace",result.breakdown().size()==1&&
                "CAL2:PERSIAN:101:201".equals(result.breakdown().get(0).get("calendarDatasetEvidence")));
            check(model+" unverified duplicate",result.duplicateCheckStatus().equals(
                "NOT_VERIFIED_NO_AUTHORITATIVE_CHARGE_HISTORY"));
            check(model+" no financial approval",result.diagnostics().contains("NOT_A_CHARGE_AUTHORIZATION"));
            check(model+" replay key",result.candidateAssessmentKey().equals(
                Fee3R4C3QardContract.calculate(r,days(),ORIGIN_DAY,EVENT_DAY).candidateAssessmentKey()));
            check(model+" deterministic hash key length",result.candidateAssessmentKey().length()==64);
        }
        var a=request("ON_ANNIVERSARY");
        var original=Fee3R4C3QardContract.calculate(a,days(),ORIGIN_DAY,EVENT_DAY);
        var changed=mutate("ON_ANNIVERSARY","amount","200000000.00");
        check("same timing identifier despite changed amount",original.candidateAssessmentKey().equals(
            Fee3R4C3QardContract.calculate(changed,days(),ORIGIN_DAY,EVENT_DAY).candidateAssessmentKey()));
        check("different timing different charge key",!original.candidateAssessmentKey().equals(
            Fee3R4C3QardContract.calculate(request("CONTRACT_YEAR_START"),days(),ORIGIN_DAY,EVENT_DAY).candidateAssessmentKey()));
        check("rounded amount zero interest only as zero fee",Fee3R4C3QardContract.calculate(
            mutate("ON_ANNIVERSARY","contractRate","0"),days(),ORIGIN_DAY,EVENT_DAY)
                .roundedIrr().compareTo(BigDecimal.ZERO)==0);
        check("too precise money", "QARD_AMOUNT_NUMBER22_2".equals(Fee3R4C3QardContract.guard(
            mutate("ON_ANNIVERSARY","amount","120.001"),EVENT)));
        check("too precise rate", "QARD_RATE_NUMBER18_8".equals(Fee3R4C3QardContract.guard(
            mutate("ON_ANNIVERSARY","contractRate","4.123456789"),EVENT)));
        check("negative rate blocked","QARD_RATE_NUMBER18_8".equals(Fee3R4C3QardContract.guard(
            mutate("ON_ANNIVERSARY","contractRate","-1"),EVENT)));
        check("missing contract evidence","QARD_CONTRACT_SCHEDULE_EVIDENCE_REQUIRED".equals(
            Fee3R4C3QardContract.guard(mutate("ON_ANNIVERSARY","contractEvidenceReference",""),EVENT)));
        check("missing schedule evidence","QARD_CONTRACT_SCHEDULE_EVIDENCE_REQUIRED".equals(
            Fee3R4C3QardContract.guard(mutate("ON_ANNIVERSARY","scheduleEvidenceReference",""),EVENT)));
        check("client yearFraction forbidden","QARD_UNREVIEWED_INPUT_FIELDS".equals(
            Fee3R4C3QardContract.guard(mutate("ON_ANNIVERSARY","yearFraction","1"),EVENT)));
        check("duplicate/approved claim forbidden","QARD_UNREVIEWED_INPUT_FIELDS".equals(
            Fee3R4C3QardContract.guard(mutate("ON_ANNIVERSARY","previousChargeExists",false),EVENT)));
        check("first installment date mismatch","FIRST_INSTALLMENT_SCHEDULE_MISMATCH".equals(
            Fee3R4C3QardContract.guard(mutate("FIRST_INSTALLMENT_OF_PERIOD","firstInstallmentDate",
                EVENT.plusDays(1).toString()),EVENT)));
        check("wrong year start basis","QARD_TIMING_POLICY_MISMATCH".equals(
            Fee3R4C3QardContract.guard(changePolicy("CONTRACT_YEAR_START","yearStartBasis",
                "JALALI_CALENDAR_YEAR"),EVENT)));
        check("unreviewed holiday policy","QARD_TIMING_POLICY_MISMATCH".equals(
            Fee3R4C3QardContract.guard(changePolicy("ON_ANNIVERSARY","holidayPolicy",
                "ROLL_DUE_TO_WORKDAY"),EVENT)));
        check("event must be bank day","QARD_EVENT_NOT_AT_BANK_BUSINESS_DATE".equals(
            Fee3R4C3QardContract.guard(mutate("FIRST_INSTALLMENT_OF_PERIOD","eventDate",
                EVENT.plusDays(1).toString()),EVENT)));
        check("backdated effective forbidden","QARD_EVENT_MUST_MATCH_BANK_DAY".equals(
            Fee3R4C3QardContract.guard(new Fee3R4C3QardContract.Request(a.draft(),a.inputs(),
                EVENT.minusDays(1)),EVENT)));
        check("unavailable slices blocked","QARD_EVENT_DAY_NOT_COVERED_BY_CAL2_PERIOD".equals(
            errorOf(a,List.of(),ORIGIN_DAY,EVENT_DAY)));
        check("different variant blocked","QARD_CAL2_VARIANT_CHANGED".equals(
            errorOf(a,days(),new Fee3R4C3QardContract.EventDay(ORIGIN,1404,1,1,1,365,
                "CAL2:PERSIAN:102:200"),EVENT_DAY)));
        check("non anniversary blocked","QARD_ANNIVERSARY_NOT_CAL2_VERIFIED".equals(
            errorOf(a,days(),ORIGIN_DAY,new Fee3R4C3QardContract.EventDay(EVENT,1405,1,2,2,365,CAL))));
        check("year start requires Farvardin 1","QARD_NOT_JALALI_NEW_YEAR".equals(
            errorOf(request("JALALI_YEAR_START"),days(),ORIGIN_DAY,
                new Fee3R4C3QardContract.EventDay(EVENT,1405,1,2,2,365,CAL))));
        check("contract year requires matching anniversary","QARD_NOT_START_OF_CONTRACT_YEAR".equals(
            errorOf(request("CONTRACT_YEAR_START"),days(),ORIGIN_DAY,
                new Fee3R4C3QardContract.EventDay(EVENT,1405,1,2,2,365,CAL))));
        check("event missing from slices", "QARD_EVENT_DAY_NOT_COVERED_BY_CAL2_PERIOD".equals(
            errorOf(a,List.of(new Fee3R4C1LoanProfitContract.YearSlice(1405,EVENT.plusDays(1),
                EVENT.plusDays(11),10,365,CAL)),ORIGIN_DAY,EVENT_DAY)));
        var incomplete=Fee3R4C3QardContract.blocked(a,EVENT,"QARD_TEST_BLOCKED");
        check("blocked has no financial amount",incomplete.calculation()==null&&
            incomplete.snapshotSha256()==null&&!incomplete.approved());
        check("no persisted source must provide reference to finish",Fee3R4C3QardContract.finish(
            a,EVENT,null,original).status().equals("BLOCKED"));
        check("all four fee models only",Fee3R4C3QardContract.SPECS.size()==4);
        System.out.println("FEE3_R4C3_PURE_JAVA_PASS="+yes+" FAIL="+no);
        if(no>0)System.exit(2);
    }
}
