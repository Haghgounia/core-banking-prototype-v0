import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Standard-JDK tests of actual R4A contract + R3 engine; no database or mocks needed. */
public final class Fee3R4ContractProbe {
    private static int passed, failed;
    private static final LocalDate TODAY=LocalDate.of(2026,10,27);
    private static BigDecimal b(String s){return new BigDecimal(s);}
    private static RuleDraft rule(String method,String rateSource,String rate,String fixed,String min,String max){
        return new RuleDraft(1,method,"REQUEST_AMOUNT",rateSource,rate==null?null:b(rate),
            fixed==null?null:b(fixed),null,null,min==null?null:b(min),max==null?null:b(max),null,Map.of());
    }
    private static DraftRequest draft(String method,String rounding,String tax,Map<String,Object> policy,
                                      String amountKind,RuleDraft rule,LocalDate from,LocalDate to){
        return new DraftRequest(null,null,null,null,
            new ItemDraft("Test Service","DEPOSIT","ACCOUNT_OPENING",amountKind,null,"TARIFF",null),
            new VersionDraft(from,to,"BANK_INTERNAL",null,method,tax,rounding,null,policy,"IRR","IRR",Map.of()),
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(rule),List.of());
    }
    private static DraftRequest fromRule(String method,RuleDraft rule){
        return draft(method,"HALF_UP","EXEMPT",Map.of(),"SERVICE_FEE",rule,TODAY,null);
    }
    private static Fee3R4CalculationContract.Request request(DraftRequest d,Map<String,Object> inputs,LocalDate date){
        return new Fee3R4CalculationContract.Request(d,inputs,date);
    }
    private static String guard(DraftRequest d,Map<String,Object> inputs,LocalDate date){
        return Fee3R4CalculationContract.guard(request(d,inputs,date),TODAY);
    }
    private static Fee3R4CalculationContract.Evidence evidence(DraftRequest draft,Map<String,Object> inputs){
        var req=request(draft,inputs,null);
        var source=new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",null,null,null,null,"UNSAVED",false,0);
        return Fee3R4CalculationContract.finish(req,TODAY,source,
            Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(draft,inputs)));
    }
    private static void ok(String title,boolean result){
        if(result){passed++;System.out.println("PASS | "+title);}
        else{failed++;System.out.println("FAIL | "+title);}
    }
    private static boolean code(String actual,String expected){return expected.equals(actual);}
    public static void main(String[] args){
        RuleDraft fixed=rule("FIXED","RULE",null,"12500",null,null);
        DraftRequest fixedDraft=fromRule("FIXED",fixed);
        ok("fixed guard",guard(fixedDraft,Map.of("amount",1000000),null)==null);
        var f=evidence(fixedDraft,Map.of("amount",1000000));
        ok("fixed result 12500",f.status().equals("SIMULATED")&&f.calculation().feeIrr().compareTo(b("12500"))==0);
        ok("no persistence posting or approval",!f.persisted()&&!f.financialPosting()&&!f.published()&&!f.approved());
        ok("only IRR rounding at integer scale",f.roundingScale()==0&&f.currency().equals("IRR"));
        ok("Oracle ref is not faked",f.versionReference().tariffVersionId()==null&&f.versionReference().status().equals("UNSAVED"));
        ok("fingerprint length 64",f.snapshotSha256().matches("[0-9a-f]{64}"));
        RuleDraft percent=rule("PERCENT","RULE","2.5",null,null,null);
        DraftRequest ph=fromRule("PERCENT",percent);
        ok("percent guard",guard(ph,Map.of("amount",100),null)==null);
        ok("half-up boundary 2.5=>3",evidence(ph,Map.of("amount",100)).calculation().feeIrr().compareTo(b("3"))==0);
        DraftRequest even=draft("PERCENT","HALF_EVEN","EXEMPT",Map.of(),"SERVICE_FEE",percent,TODAY,null);
        ok("half-even boundary 2.5=>2",evidence(even,Map.of("amount",100)).calculation().feeIrr().compareTo(b("2"))==0);
        DraftRequest down=draft("PERCENT","FLOOR","EXEMPT",Map.of(),"SERVICE_FEE",percent,TODAY,null);
        ok("floor boundary 2.5=>2",evidence(down,Map.of("amount",100)).calculation().feeIrr().compareTo(b("2"))==0);
        DraftRequest up=draft("PERCENT","CEIL","EXEMPT",Map.of(),"SERVICE_FEE",percent,TODAY,null);
        ok("ceil boundary 2.5=>3",evidence(up,Map.of("amount",100)).calculation().feeIrr().compareTo(b("3"))==0);
        RuleDraft mixed=rule("MIXED","RULE","2.5","20",null,null);
        ok("mixed 20+2.5=22.5 halfup=23",evidence(fromRule("MIXED",mixed),Map.of("amount",100)).calculation().feeIrr().compareTo(b("23"))==0);
        RuleDraft capped=rule("PERCENT","RULE","1",null,"60","80");
        ok("caps then rounds",evidence(fromRule("PERCENT",capped),Map.of("amount",5000)).calculation().feeIrr().compareTo(b("60"))==0);
        var taxable=draft("FIXED","HALF_UP","TAXABLE",Map.of("taxPercent",10),"SERVICE_FEE",
            rule("FIXED","RULE",null,"12500",null,null),TODAY,null);
        var tx=evidence(taxable,Map.of());
        ok("tax applied once",tx.calculation().taxIrr().compareTo(b("1250"))==0 && tx.calculation().totalIrr().compareTo(b("13750"))==0);
        ok("bank date required",code(Fee3R4CalculationContract.guard(request(fixedDraft,Map.of(),null),null),"ORACLE_BANK_DATE_REQUIRED"));
        ok("effective today",guard(fixedDraft,Map.of(),TODAY)==null);
        ok("future scenario within validity",guard(fixedDraft,Map.of(),TODAY.plusDays(2))==null);
        ok("historical scenario refused",code(guard(fixedDraft,Map.of(),TODAY.minusDays(1)),"HISTORICAL_SIMULATION_NOT_IN_R4A"));
        DraftRequest future=draft("FIXED","HALF_UP","EXEMPT",Map.of(),"SERVICE_FEE",fixed,TODAY.plusDays(5),null);
        ok("draft not effective today",code(guard(future,Map.of(),null),"OUTSIDE_TARIFF_VALIDITY"));
        ok("draft effective on start",guard(future,Map.of(),TODAY.plusDays(5))==null);
        DraftRequest ended=draft("FIXED","HALF_UP","EXEMPT",Map.of(),"SERVICE_FEE",fixed,TODAY,TODAY.plusDays(1));
        ok("date equal inclusive end",guard(ended,Map.of(),TODAY.plusDays(1))==null);
        ok("date after end denied",code(guard(ended,Map.of(),TODAY.plusDays(2)),"OUTSIDE_TARIFF_VALIDITY"));
        DraftRequest reverse=draft("FIXED","HALF_UP","EXEMPT",Map.of(),"SERVICE_FEE",fixed,TODAY,TODAY.minusDays(1));
        ok("date inverted rejected",guard(reverse,Map.of(),null)!=null);
        DraftRequest loan=draft("FIXED","HALF_UP","EXEMPT",Map.of(),"FINANCING_PROFIT",fixed,TODAY,null);
        ok("specialized profit blocked",code(guard(loan,Map.of(),null),"SPECIALIZED_METHOD_NOT_IN_R4A"));
        DraftRequest noRounding=draft("FIXED","UNKNOWN","EXEMPT",Map.of(),"SERVICE_FEE",fixed,TODAY,null);
        ok("unknown rounding blocked",code(guard(noRounding,Map.of(),null),"ROUNDING_POLICY_UNSUPPORTED"));
        DraftRequest badScale=fromRule("FIXED",rule("FIXED","RULE",null,"100.001",null,null));
        ok("money NUMBER22,2 blocked",code(guard(badScale,Map.of(),null),"IRR_MONEY_EXCEEDS_NUMBER_22_2"));
        DraftRequest zeroTrail=fromRule("FIXED",rule("FIXED","RULE",null,"100.0000",null,null));
        ok("trailing zeroes safe",guard(zeroTrail,Map.of(),null)==null);
        DraftRequest badRate=fromRule("PERCENT",rule("PERCENT","RULE","0.123456789",null,null,null));
        ok("rate NUMBER18,8 blocked",code(guard(badRate,Map.of("amount",100),null),"RATE_EXCEEDS_NUMBER_18_8"));
        ok("amount scale blocked",code(guard(ph,Map.of("amount","100.001"),null),"AMOUNT_EXCEEDS_IRR_PRECISION"));
        ok("fractional amount 2 decimals allowed",guard(ph,Map.of("amount","100.25"),null)==null);
        DraftRequest requested=fromRule("PERCENT",rule("PERCENT","REQUEST",null,null,null,null));
        ok("contract rate 8 decimals allowed",guard(requested,Map.of("amount",100,"contractRate","1.12345678"),null)==null);
        ok("contract rate 9 decimals blocked",code(guard(requested,Map.of("amount",100,"contractRate","1.123456789"),null),"CONTRACT_RATE_EXCEEDS_PRECISION"));
        ok("unknown input denied",code(guard(fixedDraft,Map.of("arbitrary",1),null),"UNREVIEWED_INPUT_VARIABLE"));
        ok("unused configured rate input denied",code(guard(ph,Map.of("amount",100,"contractRate",1),null),"UNUSED_CONTRACT_RATE_FOR_RULE"));
        ok("fixed-with-rate denied",code(guard(fromRule("FIXED",rule("FIXED","RULE","1","10",null,null)),Map.of(),null),"FIXED_METHOD_MUST_NOT_HAVE_RATE"));
        ok("percent-with-fixed denied",code(guard(fromRule("PERCENT",rule("PERCENT","RULE","1","10",null,null)),Map.of(),null),"PERCENT_METHOD_MUST_NOT_HAVE_FIXED"));
        ok("mixed missing fixed denied",code(guard(fromRule("MIXED",rule("MIXED","RULE","1",null,null,null)),Map.of(),null),"MIXED_FIXED_REQUIRED"));
        DraftRequest unknownInputs=fromRule("PERCENT",rule("PERCENT","RULE","1",null,null,null));
        ok("numeric overflow blocked",code(guard(unknownInputs,Map.of("amount","1000000000000000001"),null),"AMOUNT_EXCEEDS_IRR_PRECISION"));
        DraftRequest badTax=draft("FIXED","HALF_UP","TAXABLE",Map.of("taxPercent","10.123456789"),"SERVICE_FEE",fixed,TODAY,null);
        ok("tax fractional precision enforced",code(guard(badTax,Map.of(),null),"TAX_PERCENT_EXCEEDS_PRECISION"));
        RuleDraft formula=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","RULE",null,null,null,null,null,null,"X",Map.of());
        ok("formula unsupported",code(guard(fromRule("FORMULA",formula),Map.of(),null),"METHOD_NOT_CERTIFIED_FOR_R4A"));
        DraftRequest partial=new DraftRequest(1L,null,null,null,fixedDraft.item(),fixedDraft.version(),fixedDraft.scope(),fixedDraft.rules(),fixedDraft.tests());
        ok("partial Oracle id refused",code(guard(partial,Map.of(),null),"PARTIAL_VERSION_REFERENCE_FORBIDDEN"));
        DraftRequest stale=new DraftRequest(1L,2L,null,1L,fixedDraft.item(),fixedDraft.version(),fixedDraft.scope(),fixedDraft.rules(),fixedDraft.tests());
        ok("persisted version requires 2 record versions",code(guard(stale,Map.of(),null),"VERSION_REFERENCE_INCOMPLETE"));
        DraftRequest phantom=new DraftRequest(null,null,1L,null,fixedDraft.item(),fixedDraft.version(),fixedDraft.scope(),fixedDraft.rules(),fixedDraft.tests());
        ok("unpersisted record version refused",code(guard(phantom,Map.of(),null),"UNPERSISTED_DRAFT_WITH_RECORD_VERSION"));
        var refused=Fee3R4CalculationContract.blocked(request(fixedDraft,Map.of(),null),TODAY,"TEST_BLOCK");
        ok("blocked result has no amount",refused.calculation()==null&&refused.snapshotSha256()==null&&refused.status().equals("BLOCKED"));
        ok("blocked never claims money",!refused.persisted()&&!refused.financialPosting()&&!refused.approved());
        Map<String,Object> m1=new LinkedHashMap<>();m1.put("amount",100);m1.put("contractRate",1);
        Map<String,Object> m2=new LinkedHashMap<>();m2.put("contractRate",1.0);m2.put("amount",100.0);
        var ref=new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",null,null,null,null,"UNSAVED",false,0);
        String d1=Fee3R4CalculationContract.fingerprint(request(fixedDraft,m1,null),ref);
        String d2=Fee3R4CalculationContract.fingerprint(request(fixedDraft,m2,null),ref);
        ok("fingerprint canonical map order and numeric representation",d1.equals(d2));
        ok("fingerprint changes with inputs",!d1.equals(Fee3R4CalculationContract.fingerprint(request(fixedDraft,Map.of("amount",101),null),ref)));
        ok("fingerprint changes with version reference",!d1.equals(Fee3R4CalculationContract.fingerprint(request(fixedDraft,m1,null),new Fee3R4CalculationContract.VersionReference("ORACLE_DRAFT",1L,2L,1L,1L,"DRAFT",false,1))));
        ok("fingerprint changes with effective date",!d1.equals(Fee3R4CalculationContract.fingerprint(request(fixedDraft,m1,TODAY.plusDays(1)),ref)));
        ok("R3 result marked simulated",f.calculation().status().equals("SIMULATED"));
        ok("explicit caution on tax not certified",f.diagnostics().contains("NOT_A_TAX_DETERMINATION"));
        System.out.println("FEE3_R4A_CONTRACT_PROBE_PASS="+passed+" FAIL="+failed);
        if(failed!=0)System.exit(2);
    }
}
