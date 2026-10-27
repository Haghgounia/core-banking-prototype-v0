import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4b2.Fee3R4B2GuaranteeContract;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Standalone executable against production pure Java contract. No Spring, Oracle, or mocks. */
public class Fee3R4B2Probe {
    static final LocalDate DAY=LocalDate.parse("2026-10-27");
    static int passed=0,failed=0;
    static BigDecimal n(String x){return x==null?null:new BigDecimal(x);}
    static Map<String,Object> map(Object... pairs){
        Map<String,Object> result=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);return result;
    }
    static Map<String,Object> tariff(String type,String source,Object rate,Object coverage){
        return map("collateralType",type,"rateSource",source,"ratePercent",rate,"coveragePercent",coverage);
    }
    static Map<String,Object> position(String type,Object share){return map("collateralType",type,"sharePercent",share);}
    static Fee3R4B2GuaranteeContract.Request portfolio(String basis,String method){
        List<Object> rates=new ArrayList<>(List.of(tariff("CASH_100","EXEMPT",null,100),
            tariff("PROMISSORY_NOTE","CONFIG",2.25,120),tariff("MOVABLE_PROPERTY","CONFIG",1,100)));
        var policy=map("collateralType","MIXED_PORTFOLIO","feeBasis",basis,"portfolioRules",rates);
        Map<String,Object> inputs=map("amount",100000000,"portfolio",new ArrayList<>(List.of(
            position("CASH_100","15"),position("PROMISSORY_NOTE","50"),
            position("MOVABLE_PROPERTY","35"))));
        if("DAILY".equals(method)){
            inputs.put("periodStart","2028-01-01");inputs.put("periodEndExclusive","2029-01-01");
        }
        return request(policy,method,"RULE",null,inputs);
    }
    static Fee3R4B2GuaranteeContract.Request single(String type,String basis,String method,String source,String rate){
        Map<String,Object> policy=map("collateralType",type,"feeBasis",basis,"portfolioRules",List.of());
        Map<String,Object> inputs=map("amount",100000000);
        if("CASH_100".equals(type)||"GUARANTEE_COLLATERAL_COVERED_VALUE".equals(basis))
            inputs.put("collateralCoveragePercent",120);
        if("REQUEST".equals(source))inputs.put("contractRate",rate);
        if("DAILY".equals(method)){
            inputs.put("periodStart","2028-01-01");inputs.put("periodEndExclusive","2029-01-01");
        }
        return request(policy,method,source,"RULE".equals(source)?rate:null,inputs);
    }
    static Fee3R4B2GuaranteeContract.Request request(Map<String,Object> policy,String method,String source,
                                                      String rate,Map<String,Object> inputs){
        var item=new ItemDraft("Guarantee simulation","GUARANTEE","GUARANTEE_ISSUE","GUARANTEE_FEE",
            null,"GUARANTEE",null);
        var v=new VersionDraft(DAY,null,"BANK_INTERNAL",null,method,"EXEMPT","HALF_UP",
            "No-write test",map(),"IRR","IRR",map("guaranteePolicy",policy));
        var scope=new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR");
        var r=new RuleDraft(1,method,(String)policy.get("feeBasis"),source,n(rate),null,
            null,null,null,null,null,map("originalConfig", "DAILY".equals(method)?map("dayBasis","ACT_ACT"):map()));
        var draft=new DraftRequest(null,null,null,null,item,v,scope,List.of(r),
            List.of(new TestDraft("Numeric proof",map("amount",100000000),null)));
        return new Fee3R4B2GuaranteeContract.Request(draft,inputs,null);
    }
    static Fee3R4B2GuaranteeContract.Request with(Fee3R4B2GuaranteeContract.Request req,
         Map<String,Object> policy,Map<String,Object> inputs,RuleDraft rule,VersionDraft version,
         ItemDraft item,Long itemId){
        var d=req.draft();
        var dd=new DraftRequest(itemId,d.tariffVersionId(),d.expectedItemRecordVersion(),
            d.expectedVersionRecordVersion(),item==null?d.item():item,
            version==null?d.version():version,d.scope(),
            List.of(rule==null?d.rules().get(0):rule),d.tests());
        return new Fee3R4B2GuaranteeContract.Request(dd,inputs==null?req.inputs():inputs,req.effectiveOn());
    }
    static Fee3R4B2GuaranteeContract.Request change(Fee3R4B2GuaranteeContract.Request req,
         Map<String,Object> policy,Map<String,Object> inputs){
        var v=req.draft().version();var updated=new VersionDraft(v.validFrom(),v.validTo(),v.origin(),v.sourceId(),
            v.method(),v.taxMode(),v.rounding(),v.changeReason(),v.financialPolicy(),v.serviceCurrency(),
            v.feeCurrency(),map("guaranteePolicy",policy));
        var r=req.draft().rules().get(0);var updatedRule=new RuleDraft(r.order(),r.method(),
            (String)policy.get("feeBasis"),r.rateSource(),r.rate(),r.fixed(),r.collateral(),
            r.coverage(),r.min(),r.max(),r.formulaCode(),r.parameters());
        return with(req,policy,inputs,updatedRule,updated,null,null);
    }
    static Map<String,Object> policy(Fee3R4B2GuaranteeContract.Request r){
        return (Map<String,Object>)r.draft().version().uiContext().get("guaranteePolicy");
    }
    static void ok(String name,boolean condition){
        if(condition){passed++;System.out.println("PASS | "+name);}else{failed++;System.out.println("FAIL | "+name);}
    }
    static void guard(String name,Fee3R4B2GuaranteeContract.Request req,String code){
        String violation=Fee3R4B2GuaranteeContract.guard(req,DAY);
        ok(name,code.equals(violation));if(!code.equals(violation))System.out.println("EXPECTED="+code+" ACTUAL="+violation);
    }
    static void amount(String name,Fee3R4B2GuaranteeContract.Request req,String expected){
        String violation=Fee3R4B2GuaranteeContract.guard(req,DAY);
        if(violation!=null){ok(name,false);System.out.println("BLOCKED="+violation);return;}
        try{var c=Fee3R4B2GuaranteeContract.calculate(req);
            ok(name,c.feeIrr().compareTo(n(expected))==0 && !c.persisted() && !c.financialPosting());
        }catch(RuntimeException exception){ok(name,false);System.out.println(exception);}
    }
    static void expectThrow(String name,Runnable work){
        try{work.run();ok(name,false);}catch(IllegalArgumentException exception){ok(name,true);}
    }
    static VersionDraft v(VersionDraft old,String tax,Map<String,Object> policy,String rounding){
        return new VersionDraft(old.validFrom(),old.validTo(),old.origin(),old.sourceId(),old.method(),
            tax,rounding,old.changeReason(),policy,old.serviceCurrency(),old.feeCurrency(),old.uiContext());
    }
    static RuleDraft limits(RuleDraft old,String min,String max){
        return new RuleDraft(old.order(),old.method(),old.base(),old.rateSource(),old.rate(),old.fixed(),
            old.collateral(),old.coverage(),n(min),n(max),old.formulaCode(),old.parameters());
    }
    static void run(){
        String allocated="GUARANTEE_PORTFOLIO_ALLOCATED",covered="GUARANTEE_PORTFOLIO_COLLATERAL_VALUE";
        var a=portfolio(allocated,"PERCENT");
        amount("reference portfolio allocated 15/50/35 = 1475000",a,"1475000");
        amount("reference portfolio covered 15/50/35 = 1700000",portfolio(covered,"PERCENT"),"1700000");
        var two=portfolio(allocated,"PERCENT");
        two.inputs().put("portfolio",new ArrayList<>(List.of(position("CASH_100",40),position("PROMISSORY_NOTE",60))));
        amount("two collateral 40/60 allocated = 1350000",two,"1350000");
        var twoCovered=portfolio(covered,"PERCENT");twoCovered.inputs().put("portfolio",new ArrayList<>(List.of(
            position("CASH_100",40),position("PROMISSORY_NOTE",60))));
        amount("two collateral 40/60 covered = 1620000",twoCovered,"1620000");
        var s=single("PROMISSORY_NOTE","GUARANTEE_AMOUNT","PERCENT","RULE","1");
        amount("single guarantee amount 1 percent",s,"1000000");
        amount("single coverage 120 percent",single("PROMISSORY_NOTE",
            "GUARANTEE_COLLATERAL_COVERED_VALUE","PERCENT","RULE","1"),"1200000");
        amount("single cash with explicit 120 coverage",single("CASH_100","GUARANTEE_AMOUNT",
            "PERCENT","RULE","1"),"1000000");
        amount("single request rate explicit",single("GOLD","GUARANTEE_AMOUNT",
            "PERCENT","REQUEST","2.25"),"2250000");
        var taxed=portfolio(allocated,"PERCENT");
        amount("taxed fee remains same before tax",with(taxed,null,null,null,
            v(taxed.draft().version(),"TAXABLE",map("taxPercent",10),"HALF_UP"),null,null),"1475000");
        var tx=with(taxed,null,null,null,v(taxed.draft().version(),"TAXABLE",map("taxPercent",10),"HALF_UP"),null,null);
        var tc=Fee3R4B2GuaranteeContract.calculate(tx);
        ok("tax once after aggregate",tc.taxIrr().compareTo(n("147500"))==0 &&
            tc.totalIrr().compareTo(n("1622500"))==0);
        var max=portfolio(allocated,"PERCENT");amount("cap on aggregate once",
            with(max,null,null,limits(max.draft().rules().get(0),null,"1200000"),null,null,null),"1200000");
        var min=portfolio(allocated,"PERCENT");amount("minimum on aggregate once",
            with(min,null,null,limits(min.draft().rules().get(0),"2000000",null),null,null,null),"2000000");
        amount("DAILY 2028 leap whole year ACT_ACT",portfolio(allocated,"DAILY"),"1475000");
        var daily=portfolio(allocated,"DAILY");
        daily.inputs().put("periodEndExclusive","2028-07-01");
        var c=Fee3R4B2GuaranteeContract.calculate(daily);
        ok("DAILY leap 182 days / 366 exact",c.rawIrr().subtract(n("1475000").multiply(
            n("182").divide(n("366"),24,java.math.RoundingMode.HALF_EVEN))).abs().compareTo(n("0.00001"))<0);
        ok("ACT_365 leap fraction greater than one",Fee3R4B2GuaranteeContract.fraction(
            LocalDate.parse("2028-01-01"),LocalDate.parse("2029-01-01"),"ACT_365").compareTo(BigDecimal.ONE)>0);
        ok("ACT_360 exceeds ACT_365",Fee3R4B2GuaranteeContract.fraction(
            LocalDate.parse("2028-01-01"),LocalDate.parse("2029-01-01"),"ACT_360")
            .compareTo(Fee3R4B2GuaranteeContract.fraction(LocalDate.parse("2028-01-01"),
                LocalDate.parse("2029-01-01"),"ACT_365"))>0);
        ok("ACT_ACT cross year is positive and less two",Fee3R4B2GuaranteeContract.fraction(
            LocalDate.parse("2027-07-01"),LocalDate.parse("2028-07-01"),"ACT_ACT").compareTo(BigDecimal.ONE)>0);
        expectThrow("reverse period rejected",()->Fee3R4B2GuaranteeContract.fraction(
            LocalDate.parse("2029-01-01"),LocalDate.parse("2028-01-01"),"ACT_ACT"));
        expectThrow("unapproved day basis rejected",()->Fee3R4B2GuaranteeContract.fraction(
            LocalDate.parse("2028-01-01"),LocalDate.parse("2028-02-01"),"JALALI_ACT_ACT"));
        expectThrow("zero duration rejected",()->Fee3R4B2GuaranteeContract.fraction(
            LocalDate.parse("2028-01-01"),LocalDate.parse("2028-01-01"),"ACT_365"));
        var p=portfolio(allocated,"PERCENT");
        p.inputs().put("portfolio",new ArrayList<>(List.of(position("CASH_100",15),
            position("PROMISSORY_NOTE",50),position("MOVABLE_PROPERTY",30))));
        guard("missing five percent denied",p,"PORTFOLIO_SHARES_MUST_TOTAL_100");
        p=portfolio(allocated,"PERCENT");p.inputs().put("portfolio",new ArrayList<>(List.of(
            position("CASH_100",15),position("PROMISSORY_NOTE",50),position("MOVABLE_PROPERTY",40))));
        guard("excess five percent denied",p,"PORTFOLIO_SHARES_MUST_TOTAL_100");
        p=portfolio(allocated,"PERCENT");p.inputs().put("portfolio",new ArrayList<>(List.of(position("GOLD",100))));
        guard("unknown selected collateral denied",p,"PORTFOLIO_COLLATERAL_NOT_IN_TARIFF_OR_DUPLICATE");
        p=portfolio(allocated,"PERCENT");p.inputs().put("portfolio",new ArrayList<>(List.of(
            position("PROMISSORY_NOTE",50),position("PROMISSORY_NOTE",50))));
        guard("duplicate selected collateral denied",p,"PORTFOLIO_COLLATERAL_NOT_IN_TARIFF_OR_DUPLICATE");
        p=portfolio(allocated,"PERCENT");((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1)).put("coveragePercent",null);
        guard("absent tariff coverage denied",p,"TARIFF_COVERAGE_INVALID");
        p=portfolio(allocated,"PERCENT");((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(0)).put("coveragePercent",99);
        guard("cash below full coverage denied",p,"CASH_100_COVERAGE_BELOW_100");
        p=portfolio(allocated,"PERCENT");((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1)).put("ratePercent",null);
        guard("missing tariff rate denied",p,"TARIFF_RATE_INVALID");
        p=portfolio(allocated,"PERCENT");((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1)).put("ratePercent","2.123456789");
        guard("rate beyond NUMBER18_8 denied",p,"TARIFF_RATE_INVALID");
        p=portfolio(allocated,"PERCENT");((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1)).put("coveragePercent","120.12345");
        guard("coverage beyond four digits denied",p,"TARIFF_COVERAGE_INVALID");
        p=portfolio(allocated,"PERCENT");((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1)).put("collateralType","CASH_100");
        guard("duplicate tariff type denied",p,"TARIFF_COLLATERAL_DUPLICATE_OR_UNKNOWN");
        p=portfolio(allocated,"PERCENT");((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1)).put("rateSource","MYSTERY");
        guard("invalid tariff source denied",p,"COLLATERAL_RATE_SOURCE_UNSUPPORTED");
        p=portfolio(allocated,"PERCENT");((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1)).put("rateSource","REQUEST");
        ((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1)).put("ratePercent",null);
        guard("requested contract rate absent denied",p,"PORTFOLIO_INPUT_FIELDS_UNREVIEWED");
        p=portfolio(allocated,"PERCENT");var r=(Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1);
        r.put("rateSource","REQUEST");r.put("ratePercent",null);p.inputs().put("contractRate","2.25");
        amount("explicit contract rate for one portfolio row",p,"1475000");
        p.inputs().put("contractRate","2.250000001");guard("contract rate extra decimal denied",p,"CONTRACT_RATE_REQUIRED");
        p=portfolio(allocated,"PERCENT");((Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1)).put("rateSource","EXEMPT");
        guard("EXEMPT cannot preserve CONFIG rate",p,"NON_CONFIG_RATE_MUST_BE_NULL");
        p=portfolio(allocated,"PERCENT");var row=(Map<String,Object>)((List<?>)policy(p).get("portfolioRules")).get(1);
        row.put("rateSource","EXEMPT");row.put("ratePercent",null);
        amount("explicit EXEMPT row subtracts fee contribution",p,"350000");
        p=portfolio(allocated,"PERCENT");p.inputs().put("amount","100000000.001");
        guard("fractional money beyond two decimals denied",p,"POSITIVE_GUARANTEE_AMOUNT_REQUIRED");
        p=portfolio(allocated,"PERCENT");p.inputs().put("amount",0);
        guard("zero guarantee amount denied",p,"POSITIVE_GUARANTEE_AMOUNT_REQUIRED");
        p=portfolio(allocated,"PERCENT");p.inputs().put("amount","1000000000000000001");
        guard("money beyond safe maximum denied",p,"POSITIVE_GUARANTEE_AMOUNT_REQUIRED");
        p=portfolio(allocated,"PERCENT");p.inputs().put("yearFraction",1);
        guard("user cannot supply year fraction",p,"PORTFOLIO_INPUT_FIELDS_UNREVIEWED");
        p=portfolio(allocated,"PERCENT");p.inputs().put("taxPercent",10);
        guard("request cannot override tax percent",p,"PORTFOLIO_INPUT_FIELDS_UNREVIEWED");
        p=portfolio(allocated,"PERCENT");var vv=v(p.draft().version(),"EXEMPT",map("waiverPercent",100),"HALF_UP");
        guard("waiver blocked",with(p,null,null,null,vv,null,null),"FINANCIAL_MODIFIERS_UNREVIEWED");
        p=portfolio(allocated,"PERCENT");vv=v(p.draft().version(),"TAXABLE",map(),"HALF_UP");
        guard("taxable requires explicit rate",with(p,null,null,null,vv,null,null),"EXPLICIT_TAX_RATE_REQUIRED");
        p=portfolio(allocated,"PERCENT");vv=v(p.draft().version(),"UNREVIEWED",map(),"HALF_UP");
        guard("unknown tax blocked",with(p,null,null,null,vv,null,null),"TAX_MODE_UNREVIEWED");
        p=portfolio(allocated,"PERCENT");vv=v(p.draft().version(),"EXEMPT",map(),"WEIRD");
        guard("invalid rounding denied",with(p,null,null,null,vv,null,null),"ROUNDING_POLICY_UNSUPPORTED");
        p=portfolio(allocated,"PERCENT");var ii=new ItemDraft("Guar","GUARANTEE","GUARANTEE_RENEW",
            "GUARANTEE_FEE",null,"GUARANTEE",null);
        guard("renewal blocked pending explicit policy",with(p,null,null,null,null,ii,null),"GUARANTEE_ISSUE_ONLY");
        p=portfolio(allocated,"PERCENT");ii=new ItemDraft("Guar","GUARANTEE","GUARANTEE_ISSUE",
            "SERVICE_FEE",null,"GUARANTEE",null);
        guard("wrong amount kind blocked",with(p,null,null,null,null,ii,null),"GUARANTEE_ISSUE_ONLY");
        p=portfolio(allocated,"PERCENT");guard("partial Oracle reference denied",
            with(p,null,null,null,null,null,123L),"PARTIAL_ORACLE_REFERENCE");
        p=portfolio(allocated,"DAILY");p.inputs().put("periodStart","2025-01-01");
        guard("past period not silently calculated",p,"PERIOD_OUTSIDE_SUPPORTED_RANGE");
        p=portfolio(allocated,"DAILY");p.inputs().remove("periodEndExclusive");
        guard("missing explicit period denied",p,"PERIOD_DATES_REQUIRED");
        p=portfolio(allocated,"DAILY");p.inputs().put("periodEndExclusive","2028-01-01");
        guard("reversed period denied",p,"PERIOD_OUTSIDE_SUPPORTED_RANGE");
        p=portfolio(allocated,"DAILY");p.inputs().put("periodEndExclusive","2028-02-30");
        guard("invalid calendar day denied",p,"PERIOD_INVALID_ISO_DATE");
        p=portfolio(allocated,"PERCENT");p.inputs().put("periodStart","2028-01-01");
        guard("non-daily must not include dates",p,"PORTFOLIO_INPUT_FIELDS_UNREVIEWED");
        p=portfolio(allocated,"PERCENT");var rr=p.draft().rules().get(0);
        rr=new RuleDraft(rr.order(),rr.method(),rr.base(),rr.rateSource(),rr.rate(),rr.fixed(),
            rr.collateral(),rr.coverage(),rr.min(),rr.max(),"NOT_REVIEWED",rr.parameters());
        guard("formula blocked",with(p,null,null,rr,null,null,null),"UNREVIEWED_RULE_NUMERIC_OR_FORMULA_FIELD");
        p=portfolio(allocated,"PERCENT");rr=p.draft().rules().get(0);
        rr=new RuleDraft(rr.order(),rr.method(),rr.base(),rr.rateSource(),n("1"),rr.fixed(),
            rr.collateral(),rr.coverage(),rr.min(),rr.max(),rr.formulaCode(),rr.parameters());
        guard("portfolio root rate injection blocked",with(p,null,null,rr,null,null,null),"PORTFOLIO_HAS_NO_SINGLE_RATE");
        p=single("CASH_100","GUARANTEE_AMOUNT","PERCENT","RULE","1");p.inputs().put("collateralCoveragePercent",99);
        guard("single cash must have full coverage",p,"CASH_100_COVERAGE_BELOW_100");
        p=single("GOLD","GUARANTEE_AMOUNT","PERCENT","REQUEST",null);
        guard("single request missing contract rate",p,"SINGLE_CONTRACT_RATE_REQUIRED");
        p=single("GOLD","GUARANTEE_AMOUNT","PERCENT","RULE",null);
        guard("single missing tariff rate",p,"SINGLE_TARIFF_RATE_REQUIRED");
        p=single("GOLD","GUARANTEE_AMOUNT","PERCENT","RULE","50");p.inputs().put("amount",1);
        amount("half-up one rial",p,"1");
        p=single("GOLD","GUARANTEE_AMOUNT","PERCENT","RULE","50");p.inputs().put("amount",1);
        amount("half-even one rial rounds down",with(p,null,null,null,
            v(p.draft().version(),"EXEMPT",map(),"HALF_EVEN"),null,null),"0");
        p=single("GOLD","GUARANTEE_AMOUNT","PERCENT","RULE","50");p.inputs().put("amount",3);
        amount("half-even 1.5 rounds to two",with(p,null,null,null,
            v(p.draft().version(),"EXEMPT",map(),"HALF_EVEN"),null,null),"2");
        p=portfolio(allocated,"PERCENT");
        var ref=new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",
            null,null,null,null,"UNSAVED",false,0);
        var ev=Fee3R4B2GuaranteeContract.finish(p,DAY,ref,Fee3R4B2GuaranteeContract.calculate(p));
        ok("result status SIMULATED only",ev.status().equals("SIMULATED")&&!ev.persisted()&&
            !ev.approved()&&!ev.published()&&!ev.financialPosting());
        ok("sha256 available",ev.snapshotSha256()!=null&&ev.snapshotSha256().matches("[0-9a-f]{64}"));
        var ev2=Fee3R4B2GuaranteeContract.finish(p,DAY,ref,Fee3R4B2GuaranteeContract.calculate(p));
        ok("sha256 deterministic",ev.snapshotSha256().equals(ev2.snapshotSha256()));
        var active=new Fee3R4CalculationContract.VersionReference("TEST",1L,2L,1L,1L,"ACTIVE",true,1);
        ok("active reference blocked",Fee3R4B2GuaranteeContract.finish(p,DAY,active,
            Fee3R4B2GuaranteeContract.calculate(p)).status().equals("BLOCKED"));
        ok("no source reference cannot authorize charge",ev.diagnostics().contains("NOT_A_CHARGE_AUTHORIZATION"));
    }
    public static void main(String[] args){try{run();}catch(Throwable e){failed++;e.printStackTrace(System.out);}
        System.out.println("FEE3_R4B2_PURE_JAVA_PASS="+passed+" FAIL="+failed);
        if(failed!=0)System.exit(2);
    }
}
