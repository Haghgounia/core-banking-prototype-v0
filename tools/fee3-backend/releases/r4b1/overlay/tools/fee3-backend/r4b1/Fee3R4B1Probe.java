import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import com.behsazan.corebanking.fee3.r4b1.Fee3R4B1ServiceContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Executable golden and negative cases with actual R4B1+R4A+R3 pure Java sources. */
public class Fee3R4B1Probe {
    static int passed=0,failed=0;
    static final LocalDate DAY=LocalDate.of(2026,10,27);
    static final Map<String,Object> T1=Map.of("upTo",10000000,"ratePercent", "0.1","fixedAmount",0);
    static final Map<String,Object> T2=new LinkedHashMap<>();
    static final Map<String,Object> B1=Map.of("from",0,"to",10000000,"ratePercent",0,"fixedAmount",20000);
    static final Map<String,Object> B2=new LinkedHashMap<>();
    static { T2.put("upTo",null);T2.put("ratePercent","0.05");T2.put("fixedAmount",0);
             B2.put("from",10000000);B2.put("to",null);B2.put("ratePercent","0.08");B2.put("fixedAmount",0);}

    static void ok(String name,boolean condition){
        if(condition)passed++;else{failed++;System.out.println("FAIL | "+name);}
    }
    static BigDecimal b(String n){return new BigDecimal(n);}
    static Map<String,Object> cfg(String m){
        Map<String,Object> c=new LinkedHashMap<>();
        c.put("basisCode","REQUEST_AMOUNT");c.put("basisExplicit",true);c.put("rateSource","CONFIG");
        c.put("tiers","PROGRESSIVE".equals(m)?List.of(T1,T2):List.of());
        c.put("brackets","BRACKET".equals(m)?List.of(B1,B2):List.of());
        return c;
    }
    static Fee3R4B1ServiceContract.Request draft(String method,String amount,Map<String,Object> config){
        return draft(method,amount,config,null,null,"EXEMPT","HALF_UP",Map.of(),null);
    }
    static Fee3R4B1ServiceContract.Request draft(String method,String amount,Map<String,Object> config,
                                                  BigDecimal min,BigDecimal max,String tax,String rounding,
                                                  Map<String,Object> policy,LocalDate effective){
        RuleDraft r=new RuleDraft(1,method,"REQUEST_AMOUNT","RULE",null,null,null,null,
            min,max,null,Map.of("originalConfig",config));
        DraftRequest d=new DraftRequest(null,null,null,null,
            new ItemDraft("Demo service","PAYMENT","TRANSFER","SERVICE_FEE",null,"TARIFF",null),
            new VersionDraft(DAY,null,"BANK_INTERNAL",null,method,tax,rounding,
                "Numerical UAT",policy,"IRR","IRR",Map.of()),
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(r),List.of(new TestDraft("Reference case",Map.of("amount",amount),null)));
        return new Fee3R4B1ServiceContract.Request(d,Map.of("amount",amount),effective);
    }
    static Fee3R4B1ServiceContract.Request edit(Fee3R4B1ServiceContract.Request r,
            Map<String,Object> config,String amount){
        var d=r.draft();RuleDraft old=d.rules().get(0);
        RuleDraft rule=new RuleDraft(old.order(),old.method(),old.base(),old.rateSource(),old.rate(),
            old.fixed(),old.collateral(),old.coverage(),old.min(),old.max(),old.formulaCode(),
            Map.of("originalConfig",config));
        DraftRequest n=new DraftRequest(d.calcItemId(),d.tariffVersionId(),
            d.expectedItemRecordVersion(),d.expectedVersionRecordVersion(),d.item(),d.version(),d.scope(),
            List.of(rule),d.tests());
        return new Fee3R4B1ServiceContract.Request(n,
            amount==null?r.inputs():Map.of("amount",amount),r.effectiveOn());
    }
    static Fee3R4B1ServiceContract.Request version(Fee3R4B1ServiceContract.Request r,
            String mode,Map<String,Object> policy,String rounding,LocalDate from,Map<String,Object> ctx){
        var d=r.draft();var v=d.version();
        var fresh=new VersionDraft(from==null?v.validFrom():from,v.validTo(),v.origin(),v.sourceId(),
            mode==null?v.method():mode,v.taxMode(),rounding==null?v.rounding():rounding,
            v.changeReason(),policy==null?v.financialPolicy():policy,v.serviceCurrency(),v.feeCurrency(),
            ctx==null?v.uiContext():ctx);
        return new Fee3R4B1ServiceContract.Request(new DraftRequest(d.calcItemId(),d.tariffVersionId(),
            d.expectedItemRecordVersion(),d.expectedVersionRecordVersion(),d.item(),fresh,d.scope(),d.rules(),d.tests()),
            r.inputs(),r.effectiveOn());
    }
    static void reject(String name,Fee3R4B1ServiceContract.Request request,String code){
        String got=Fee3R4B1ServiceContract.guard(request,DAY);
        ok(name,Objects.equals(got,code));if(!Objects.equals(got,code))System.out.println("  EXPECT="+code+" ACTUAL="+got);
    }
    static Fee3PreviewEngine.PreviewResult compute(Fee3R4B1ServiceContract.Request r){
        String reason=Fee3R4B1ServiceContract.guard(r,DAY);
        if(reason!=null)throw new IllegalArgumentException("Guard: "+reason);
        return Fee3R4B1ServiceContract.calculate(r);
    }
    static void amount(String name,Fee3R4B1ServiceContract.Request r,String expected){
        var fee=compute(r);ok(name,fee.feeIrr().compareTo(b(expected))==0&&
            fee.totalIrr().compareTo(b(expected))>=0&&!fee.persisted()&&!fee.financialPosting());
    }
    static Map<String,Object> row(Object... pairs){
        Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;
    }
    public static void main(String[] args){
        var progressive=draft("PROGRESSIVE","30000000",cfg("PROGRESSIVE"));
        var bracket=draft("BRACKET","10000000",cfg("BRACKET"));
        amount("reference progressive 30M = 20K",progressive,"20000");
        amount("reference bracket at 10M = 8K",bracket,"8000");
        amount("progressive exact first limit",draft("PROGRESSIVE","10000000",cfg("PROGRESSIVE")),"10000");
        amount("bracket 1 below boundary",draft("BRACKET","9999999",cfg("BRACKET")),"20000");
        amount("bracket first row",draft("BRACKET","1000000",cfg("BRACKET")),"20000");
        amount("progressive second rate",draft("PROGRESSIVE","20000000",cfg("PROGRESSIVE")),"15000");
        var out=compute(progressive);
        ok("progressive breakdown count",out.breakdown().size()==2);
        ok("first band from zero",out.breakdown().get(0).get("chargedBasisIrr").equals(b("10000000")));
        ok("second band shares remainder",out.breakdown().get(1).get("chargedBasisIrr").equals(b("20000000")));
        ok("full amount in single bracket",compute(bracket).breakdown().get(0).get("chargedBasisIrr").equals(b("10000000")));
        ok("breakdown is immutable",testImmutable(out.breakdown()));
        var config=cfg("PROGRESSIVE");
        config.put("tiers",List.of(row("upTo",100,"ratePercent",0,"fixedAmount",20),
                                   row("upTo",null,"ratePercent",0,"fixedAmount",30)));
        amount("fixed amount applied only to hit bands",draft("PROGRESSIVE","100",config),"20");
        amount("fixed each reached band",draft("PROGRESSIVE","101",config),"50");
        amount("fixed with min cap",draft("PROGRESSIVE","101",config,b("100"),null,"EXEMPT","HALF_UP",Map.of(),null),"100");
        amount("fixed with max cap",draft("PROGRESSIVE","101",config,null,b("30"),"EXEMPT","HALF_UP",Map.of(),null),"30");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",1,"fixedAmount",0)));
        amount("half-up .5 rial",draft("PROGRESSIVE","50",config),"1");
        amount("half-even .5 rial",draft("PROGRESSIVE","50",config,null,null,"EXEMPT","HALF_EVEN",Map.of(),null),"0");
        amount("ceil .01 rial",draft("PROGRESSIVE","1",config,null,null,"EXEMPT","UP",Map.of(),null),"1");
        amount("floor .01 rial",draft("PROGRESSIVE","1",config,null,null,"EXEMPT","DOWN",Map.of(),null),"0");
        var taxable=draft("PROGRESSIVE","10000",config,null,null,"TAXABLE","HALF_UP",Map.of("taxPercent",10),null);
        var t=compute(taxable);ok("explicit tax is 10 on 100",t.taxIrr().compareTo(b("10"))==0);
        ok("total fee+tax",t.totalIrr().compareTo(b("110"))==0);
        var zero=draft("PROGRESSIVE","10000",config,null,b("0"),"EXEMPT","HALF_UP",Map.of(),null);
        amount("zero max cap explicit",zero,"0");
        var future=draft("PROGRESSIVE","10000",config,null,null,"EXEMPT","HALF_UP",Map.of(),DAY.plusDays(1));
        ok("future draft-preview guard",Fee3R4B1ServiceContract.guard(future,DAY)==null);
        var ref=new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",null,null,null,null,"UNSAVED",false,0);
        var result=Fee3R4B1ServiceContract.finish(progressive,DAY,ref,compute(progressive));
        ok("finish simulated no writes",result.status().equals("SIMULATED")&&!result.persisted()&&
            !result.financialPosting()&&!result.published()&&!result.approved());
        ok("R4B1 exact contract",result.contractVersion().equals(Fee3R4B1ServiceContract.CONTRACT));
        ok("R4B1 deterministic SHA",result.snapshotSha256().equals(
            Fee3R4B1ServiceContract.finish(progressive,DAY,ref,compute(progressive)).snapshotSha256()));
        ok("SHA hex64",result.snapshotSha256().matches("[0-9a-f]{64}"));
        ok("blocked published",Fee3R4B1ServiceContract.finish(progressive,DAY,
            new Fee3R4CalculationContract.VersionReference("CERTIFIED",1L,2L,1L,1L,"ACTIVE",true,1),compute(progressive))
            .status().equals("BLOCKED"));
        ok("future mode",Fee3R4B1ServiceContract.finish(future,DAY,ref,compute(future))
            .simulationMode().equals("FUTURE_DATED_UAT_SIMULATION"));
        ok("blocked amount has no fingerprint",Fee3R4B1ServiceContract.blocked(progressive,DAY,"TEST")
            .snapshotSha256()==null);
        // Negative contract checks: no silent gap, amount, precision, optional business policy, or waiver inference.
        reject("unsupported method",draft("DAILY","100",cfg("PROGRESSIVE")),"SERVICE_METHOD_NOT_IN_R4B1");
        reject("bad 3rd decimal amount",edit(progressive,cfg("PROGRESSIVE"),"100.001"),"EXPLICIT_POSITIVE_IRR_AMOUNT_REQUIRED");
        reject("zero amount",edit(progressive,cfg("PROGRESSIVE"),"0"),"EXPLICIT_POSITIVE_IRR_AMOUNT_REQUIRED");
        reject("negative amount",edit(progressive,cfg("PROGRESSIVE"),"-1"),"EXPLICIT_POSITIVE_IRR_AMOUNT_REQUIRED");
        reject("amount overflow",edit(progressive,cfg("PROGRESSIVE"),"1000000000000000001"),"EXPLICIT_POSITIVE_IRR_AMOUNT_REQUIRED");
        reject("invalid date",new Fee3R4B1ServiceContract.Request(progressive.draft(),progressive.inputs(),DAY.minusDays(1)),"HISTORICAL_SIMULATION_NOT_ALLOWED");
        reject("valid from after target",version(progressive,null,null,null,DAY.plusDays(1),null),"OUTSIDE_DRAFT_VALIDITY");
        reject("unknown version method",version(progressive,"MATRIX",null,null,null,null),"SERVICE_METHOD_NOT_IN_R4B1");
        reject("unsupported rounding",version(progressive,null,null,"TRUNCATE",null,null),"ROUNDING_POLICY_UNSUPPORTED");
        reject("unsupported financial modifications",version(progressive,null,Map.of("waiverPercent",100),null,null,null),"FINANCIAL_ADJUSTMENTS_NOT_CERTIFIED");
        reject("tax rate missing",version(taxable,null,Map.of(),null,null,null),"EXPLICIT_TAX_RATE_REQUIRED");
        reject("tax rate too many digits",version(taxable,null,Map.of("taxPercent","1.123456789"),null,null,null),"EXPLICIT_TAX_RATE_REQUIRED");
        reject("tax >100",version(taxable,null,Map.of("taxPercent",101),null,null,null),"EXPLICIT_TAX_RATE_REQUIRED");
        reject("tax no taxable mode",version(progressive,null,Map.of("taxPercent",10),null,null,null),"TAX_RATE_WITH_NON_TAXABLE_MODE");
        reject("guarantee ctx blocked",version(progressive,null,null,null,null,Map.of("guaranteePolicy",Map.of())),"SPECIALIZED_CONTEXT_UNREVIEWED");
        config=cfg("PROGRESSIVE");config.put("basisExplicit",false);
        reject("implicit basis blocked",edit(progressive,config,null),"EXPLICIT_MONEY_BASIS_REQUIRED");
        config=cfg("PROGRESSIVE");config.put("basisCode","CONSUMPTION_QTY");
        reject("quantity basis blocked",edit(progressive,config,null),"EXPLICIT_MONEY_BASIS_REQUIRED");
        config=cfg("PROGRESSIVE");config.put("unapprovedKey",true);
        reject("unknown config",edit(progressive,config,null),"UNKNOWN_SERVICE_CONFIG_FIELD");
        config=cfg("PROGRESSIVE");config.put("rateSource","REQUEST");
        reject("request rate blocked",edit(progressive,config,null),"DYNAMIC_TIER_RATES_UNSUPPORTED");
        config=cfg("PROGRESSIVE");config.put("freeUnits",1);
        reject("free units blocked",edit(progressive,config,null),"UNSUPPORTED_SERVICE_CONFIG_MODIFIER");
        config=cfg("PROGRESSIVE");config.put("minOutput",100);
        reject("min mismatch blocked",edit(progressive,config,null),"MINIMUM_INCONSISTENT");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",10,"ratePercent",1,"fixedAmount",0)));
        reject("missing last tier",edit(progressive,config,null),"PROGRESSIVE_LAST_TIER_MUST_BE_OPEN");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",10,"ratePercent",1,"fixedAmount",0),
            row("upTo",10,"ratePercent",1,"fixedAmount",0),T2));
        reject("non ascending tiers",edit(progressive,config,null),"TIER_UPPER_BOUND_INVALID");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",1,"fixedAmount",0),T2));
        reject("open tier not last",edit(progressive,config,null),"OPEN_TIER_MUST_BE_LAST");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent","1.123456789","fixedAmount",0)));
        reject("rate precision",edit(progressive,config,null),"TIER_RATE_OR_FIXED_INVALID");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",1001,"fixedAmount",0)));
        reject("rate >1000",edit(progressive,config,null),"TIER_RATE_OR_FIXED_INVALID");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",1,"fixedAmount","0.001")));
        reject("fixed scale >2",edit(progressive,config,null),"TIER_RATE_OR_FIXED_INVALID");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",1,"fixedAmount",-1)));
        reject("negative fixed",edit(progressive,config,null),"TIER_RATE_OR_FIXED_INVALID");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",1)));
        reject("missing fixed not auto zero",edit(progressive,config,null),"TIER_RATE_OR_FIXED_INVALID");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"fixedAmount",0)));
        reject("missing rate not auto zero",edit(progressive,config,null),"TIER_RATE_OR_FIXED_INVALID");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",1,"fixedAmount",0,"calcType","UNIT_RATE")));
        reject("unit price calcType blocked",edit(progressive,config,null),"TIER_MODE_NOT_CERTIFIED");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",1,"fixedAmount",0,"unitPrice",1)));
        reject("unknown tier key blocked",edit(progressive,config,null),"UNREVIEWED_PROGRESSIVE_ROW");
        config=cfg("PROGRESSIVE");config.put("brackets",List.of(B1,B2));
        reject("mixed config mode blocked",edit(progressive,config,null),"MIXED_TIER_MODE_FORBIDDEN");
        config=cfg("BRACKET");config.put("brackets",List.of(row("from",0,"to",10,"ratePercent",1,"fixedAmount",0),
           row("from",11,"to",null,"ratePercent",1,"fixedAmount",0)));
        reject("bracket gap blocked",edit(bracket,config,null),"BRACKET_GAP_OR_OVERLAP");
        config=cfg("BRACKET");config.put("brackets",List.of(row("from",0,"to",10,"ratePercent",1,"fixedAmount",0),
           row("from",9,"to",null,"ratePercent",1,"fixedAmount",0)));
        reject("bracket overlap blocked",edit(bracket,config,null),"BRACKET_GAP_OR_OVERLAP");
        config=cfg("BRACKET");config.put("brackets",List.of(row("from",1,"to",null,"ratePercent",1,"fixedAmount",0)));
        reject("first bracket must start zero",edit(bracket,config,null),"BRACKET_GAP_OR_OVERLAP");
        config=cfg("BRACKET");config.put("brackets",List.of(row("from",0,"to",10,"ratePercent",1,"fixedAmount",0)));
        reject("bracket last must open",edit(bracket,config,null),"BRACKET_LAST_RANGE_MUST_BE_OPEN");
        config=cfg("BRACKET");config.put("brackets",List.of(row("from",0,"to",null,"ratePercent",1,"fixedAmount",0,
            "inclusiveUpper",true)));
        reject("inclusive upper blocked",edit(bracket,config,null),"BRACKET_MODE_NOT_CERTIFIED");
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",1000,"fixedAmount",0)));
        var tooHigh=draft("PROGRESSIVE","1000000000000000000",config);
        boolean overflow=false;try{compute(tooHigh);}catch(IllegalArgumentException e){overflow="FEE_EXCEEDS_SAFE_LIMIT".equals(e.getMessage());}
        ok("arithmetic maximum safeguard",overflow);
        config=cfg("PROGRESSIVE");config.put("tiers",List.of(row("upTo",null,"ratePercent",100,"fixedAmount",0)));
        var tooTax=draft("PROGRESSIVE","1000000000000000000",config,null,null,"TAXABLE","HALF_UP",Map.of("taxPercent",100),null);
        overflow=false;try{compute(tooTax);}catch(IllegalArgumentException e){overflow="FEE_AND_TAX_TOTAL_EXCEEDS_SAFE_LIMIT".equals(e.getMessage());}
        ok("total-tax overflow safeguard",overflow);
        ok("R4A calculation contract still compiles",Fee3R4CalculationContract.CONTRACT.equals("FEE3_CALCULATION_CONTRACT_R4A_V1"));
        System.out.println("FEE3_R4B1_PURE_JAVA_PASS="+passed+" FAIL="+failed);
        if(failed!=0)System.exit(2);
    }
    static boolean testImmutable(List<Map<String,Object>> rows){
        try{rows.add(Map.of());return false;}catch(UnsupportedOperationException e){return true;}
    }
}
