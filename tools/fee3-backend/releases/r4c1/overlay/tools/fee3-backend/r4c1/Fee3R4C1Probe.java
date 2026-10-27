import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fee3R4C1Probe {
    static int pass=0,fail=0;
    static LocalDate date(String s){return LocalDate.parse(s);}
    static BigDecimal dec(String s){return new BigDecimal(s);}
    static void yes(String name,boolean good){
        System.out.println((good?"PASS":"FAIL")+" | "+name);
        if(good)pass++;else fail++;
    }
    static void same(String name,BigDecimal actual,String expected){
        yes(name,actual!=null&&actual.compareTo(dec(expected))==0);
    }
    static void reject(String name,Runnable action){
        try{action.run();yes(name,false);}catch(IllegalArgumentException exception){yes(name,true);}
    }
    static Fee3R4C1LoanProfitContract.Request make(String basis){
        boolean month="CONTRACT_MONTHS".equals(basis);
        String contract=month?"INSTALLMENT_SALE":"MURABAHA";
        String formula=month?"TPL_7F2FE580EDB351":"TPL_E624746F262F6E";
        ItemDraft item=new ItemDraft("QA EXCHANGE PROFIT","LOAN","LOAN_PROFIT","FINANCING_PROFIT",
            null,"LOAN",null);
        VersionDraft ver=new VersionDraft(date("2026-10-27"),null,"CONTRACT_RATE_REGULATED",
            null,"FORMULA","OUT_OF_SCOPE","HALF_UP","no-write C1 test",Map.of(),"IRR","IRR",Map.of(
                "loanModelProfile","EXCHANGE_ACCRUAL","loanCalculationEvent","PERIOD_PROFIT",
                "loanContractTypes",List.of(contract),"loanPolicy",Map.of("dayCountBasis",basis,
                    "holidayPolicy","CHARGE_ALL_DAYS","contractEvidenceRequired",true)));
        RuleDraft rule=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","REQUEST",null,null,null,null,
            null,null,formula,Map.of());
        ScopeDraft scope=new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),
            List.of("ALL"),List.of(contract),"IRR");
        DraftRequest draft=new DraftRequest(null,null,null,null,item,ver,scope,List.of(rule),
            List.of(new TestDraft("QA theoretical amount",Map.of("amount",100000000),null)));
        Map<String,Object> inputs=new HashMap<>(Map.of("amount","100000000.00","contractRate","18.0",
            "contractReference","TEST-CONTRACT-REF","rateEvidenceReference","TEST-RATE-REF"));
        if(month)inputs.put("contractMonths",12);
        else{inputs.put("periodStart","2026-10-27");inputs.put("periodEndExclusive","2026-11-07");}
        return new Fee3R4C1LoanProfitContract.Request(draft,inputs,null);
    }
    static Fee3R4C1LoanProfitContract.Request withInputs(Fee3R4C1LoanProfitContract.Request x,Map<String,Object> m){
        return new Fee3R4C1LoanProfitContract.Request(x.draft(),m,x.effectiveOn());
    }
    static Fee3R4C1LoanProfitContract.Request withDraft(Fee3R4C1LoanProfitContract.Request x,DraftRequest d){
        return new Fee3R4C1LoanProfitContract.Request(d,x.inputs(),x.effectiveOn());
    }
    static DraftRequest draft(Fee3R4C1LoanProfitContract.Request x,VersionDraft v,RuleDraft r){
        var d=x.draft();return new DraftRequest(d.calcItemId(),d.tariffVersionId(),
            d.expectedItemRecordVersion(),d.expectedVersionRecordVersion(),d.item(),
            v==null?d.version():v,d.scope(),List.of(r==null?d.rules().get(0):r),d.tests());
    }
    static Fee3R4C1LoanProfitContract.YearSlice slice(int yr,String from,String to,int days,int denom){
        return new Fee3R4C1LoanProfitContract.YearSlice(yr,date(from),date(to),days,denom,"CAL2_TEST_DATASET");
    }
    static String guard(Fee3R4C1LoanProfitContract.Request x){
        return Fee3R4C1LoanProfitContract.guard(x,date("2026-10-27"));
    }
    static Map<String,Object> changed(Map<String,Object> original,String key,Object val){
        var m=new HashMap<>(original);if(val==null)m.remove(key);else m.put(key,val);return m;
    }
    public static void main(String[] args){
        var m=make("CONTRACT_MONTHS");var d=make("JALALI_ACT_ACT");
        yes("month guard",guard(m)==null);
        yes("day guard",guard(d)==null);
        var ref=new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",null,null,
            null,null,"UNSAVED",false,0);
        var monthCalc=Fee3R4C1LoanProfitContract.calculate(m,List.of());
        same("12 months 100M x 18 percent",monthCalc.feeIrr(),"18000000");
        same("12 months raw",monthCalc.rawIrr(),"18000000");
        same("no tax on profit",monthCalc.taxIrr(),"0");
        yes("monthly trace length",monthCalc.breakdown().size()==1);
        yes("no account posting",!monthCalc.persisted()&&!monthCalc.financialPosting());
        var finished=Fee3R4C1LoanProfitContract.finish(m,date("2026-10-27"),ref,monthCalc);
        yes("client-draft simulation",finished.status().equals("SIMULATED")&&
            !finished.approved()&&!finished.published()&&!finished.persisted());
        yes("hash length",finished.snapshotSha256().length()==64);
        yes("deterministic hash",finished.snapshotSha256().equals(Fee3R4C1LoanProfitContract.finish(
            m,date("2026-10-27"),ref,Fee3R4C1LoanProfitContract.calculate(m,List.of())).snapshotSha256()));
        yes("unverified source disclaimer",finished.diagnostics().contains("CLIENT_CONTRACT_RATE_UNVERIFIED"));
        var six=withInputs(m,changed(m.inputs(),"contractMonths",6));
        same("6 months factor",Fee3R4C1LoanProfitContract.calculate(six,List.of()).feeIrr(),"9000000");
        var one=withInputs(m,changed(m.inputs(),"contractMonths",1));
        same("one month 100M at 18 percent",Fee3R4C1LoanProfitContract.calculate(one,List.of()).feeIrr(),"1500000");
        var slices=List.of(slice(1405,"2026-10-27","2026-11-07",11,365));
        var calc=Fee3R4C1LoanProfitContract.calculate(d,slices);
        same("11 actual Jalali days",calc.rawIrr().multiply(dec("365")).divide(dec("11"),java.math.MathContext.DECIMAL128),"18000000");
        yes("daily 11-day trace",calc.breakdown().size()==1);
        yes("daily less than annual",calc.feeIrr().compareTo(dec("18000000"))<0);
        var segments=List.of(slice(1403,"2025-03-19","2025-03-21",2,366),
            slice(1404,"2025-03-21","2025-03-25",4,365));
        var factor=Fee3R4C1LoanProfitContract.yearFactor(date("2025-03-19"),date("2025-03-25"),segments);
        same("Jalali 366 plus 365 year split",factor,
            dec("2").divide(dec("366"),java.math.MathContext.DECIMAL128).add(
            dec("4").divide(dec("365"),java.math.MathContext.DECIMAL128),java.math.MathContext.DECIMAL128).toPlainString());
        reject("gap in Jalali slices",()->Fee3R4C1LoanProfitContract.yearFactor(
            date("2025-03-19"),date("2025-03-25"),List.of(segments.get(0))));
        reject("overlap year slices",()->Fee3R4C1LoanProfitContract.yearFactor(
            date("2025-03-19"),date("2025-03-25"),List.of(segments.get(0),segments.get(0))));
        reject("wrong day count",()->Fee3R4C1LoanProfitContract.yearFactor(
            date("2026-10-27"),date("2026-11-07"),List.of(slice(1405,"2026-10-27","2026-11-07",10,365))));
        reject("bad year denominator",()->Fee3R4C1LoanProfitContract.yearFactor(
            date("2026-10-27"),date("2026-11-07"),List.of(slice(1405,"2026-10-27","2026-11-07",11,364))));
        reject("no year slices for daily",()->Fee3R4C1LoanProfitContract.calculate(d,List.of()));
        reject("month disallows calendar slices",()->Fee3R4C1LoanProfitContract.calculate(m,slices));
        yes("precomputed factor forbidden",guard(withInputs(d,changed(d.inputs(),"yearFraction","1")))
            .equals("UNREVIEWED_INPUT_FIELDS"));
        yes("contract rate missing",guard(withInputs(d,changed(d.inputs(),"contractRate",null)))
            .equals("UNREVIEWED_INPUT_FIELDS"));
        yes("high precision contract rate denied",guard(withInputs(d,changed(d.inputs(),"contractRate","1.123456789")))
            .equals("REQUEST_RATE_NOT_NUMBER_18_8"));
        yes("over 2 decimals principal denied",guard(withInputs(d,changed(d.inputs(),"amount","1.001")))
            .equals("PRINCIPAL_EXCEEDS_NUMBER_22_2"));
        yes("negative principal denied",guard(withInputs(d,changed(d.inputs(),"amount","-100")))
            .equals("PRINCIPAL_EXCEEDS_NUMBER_22_2"));
        yes("rate greater than 100 denied",guard(withInputs(d,changed(d.inputs(),"contractRate","101")))
            .equals("REQUEST_RATE_NOT_NUMBER_18_8"));
        yes("rate source evidence missing",guard(withInputs(d,changed(d.inputs(),"rateEvidenceReference",null)))
            .equals("UNREVIEWED_INPUT_FIELDS"));
        yes("month count fractional denied",guard(withInputs(m,changed(m.inputs(),"contractMonths","1.5")))
            .equals("CONTRACT_MONTHS_NOT_VALIDATED"));
        yes("month count zero denied",guard(withInputs(m,changed(m.inputs(),"contractMonths",0)))
            .equals("CONTRACT_MONTHS_NOT_VALIDATED"));
        yes("client yearFraction forbidden months",guard(withInputs(m,changed(m.inputs(),"yearFraction","0.5")))
            .equals("UNREVIEWED_INPUT_FIELDS"));
        yes("daily dates reversed denied",guard(withInputs(d,changed(d.inputs(),"periodEndExclusive","2026-10-20")))
            .equals("INVALID_JALALI_PERIOD"));
        yes("daily before validity denied",guard(withInputs(d,changed(d.inputs(),"periodStart","2026-10-20")))
            .equals("INVALID_JALALI_PERIOD"));
        yes("daily date syntax denied",guard(withInputs(d,changed(d.inputs(),"periodStart","invalid")))
            .equals("INVALID_ISO_PERIOD"));
        var v=m.draft().version();
        var badV=new VersionDraft(v.validFrom(),v.validTo(),v.origin(),v.sourceId(),v.method(),
            "UNREVIEWED",v.rounding(),v.changeReason(),v.financialPolicy(),
            v.serviceCurrency(),v.feeCurrency(),v.uiContext());
        yes("unreviewed tax denied",guard(withDraft(m,draft(m,badV,null)))
            .equals("TAX_TREATMENT_NOT_REVIEWED_FOR_PROFIT"));
        var badCtx=new HashMap<>(v.uiContext());badCtx.put("loanModelProfile","PARTICIPATION_REALIZED");
        badV=new VersionDraft(v.validFrom(),v.validTo(),v.origin(),v.sourceId(),v.method(),
            v.taxMode(),v.rounding(),v.changeReason(),v.financialPolicy(),v.serviceCurrency(),
            v.feeCurrency(),badCtx);
        yes("participation profit not exchange",guard(withDraft(m,draft(m,badV,null)))
            .equals("NOT_EXCHANGE_PERIOD_PROFIT"));
        badCtx=new HashMap<>(v.uiContext());badCtx.put("loanCalculationEvent","OVERDUE_DAMAGE");
        badV=new VersionDraft(v.validFrom(),v.validTo(),v.origin(),v.sourceId(),v.method(),
            v.taxMode(),v.rounding(),v.changeReason(),v.financialPolicy(),v.serviceCurrency(),
            v.feeCurrency(),badCtx);
        yes("late damage operation blocked",guard(withDraft(m,draft(m,badV,null)))
            .equals("NOT_EXCHANGE_PERIOD_PROFIT"));
        var r=m.draft().rules().get(0);
        RuleDraft wrongRule=new RuleDraft(r.order(),r.method(),r.base(),r.rateSource(),r.rate(),
            r.fixed(),r.collateral(),r.coverage(),r.min(),r.max(),"LOAN_LATE_HOLIDAY",r.parameters());
        yes("late damage formula denied",guard(withDraft(m,draft(m,null,wrongRule)))
            .equals("FORMULA_TEMPLATE_NOT_CERTIFIED"));
        wrongRule=new RuleDraft(r.order(),r.method(),r.base(),r.rateSource(),r.rate(),
            r.fixed(),r.collateral(),r.coverage(),r.min(),r.max(),r.formulaCode(),
            Map.of("_fceEditableExpressionV1","x"));
        yes("editable expression forbidden",guard(withDraft(m,draft(m,null,wrongRule)))
            .equals("CLIENT_FORMULA_OVERRIDE_FORBIDDEN"));
        wrongRule=new RuleDraft(r.order(),r.method(),r.base(),r.rateSource(),dec("18"),
            r.fixed(),r.collateral(),r.coverage(),r.min(),r.max(),r.formulaCode(),r.parameters());
        yes("tariff fixed rate forbidden",guard(withDraft(m,draft(m,null,wrongRule)))
            .equals("UNSUPPORTED_PROFIT_RULE"));
        var blocked=Fee3R4C1LoanProfitContract.blocked(m,date("2026-10-27"),"NOT_CERTIFIED");
        yes("block no calculation",blocked.calculation()==null&&blocked.snapshotSha256()==null&&
            blocked.status().equals("BLOCKED"));
        yes("published source denied",Fee3R4C1LoanProfitContract.finish(m,date("2026-10-27"),
            new Fee3R4CalculationContract.VersionReference("X",1L,1L,1L,1L,"ACTIVE",true,1),monthCalc)
            .status().equals("BLOCKED"));
        System.out.println("FEE3_R4C1_PURE_JAVA_PASS="+pass+" FAIL="+fail);
        if(fail!=0)System.exit(2);
    }
}
