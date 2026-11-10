import com.behsazan.corebanking.collateral.workflow.CollateralValuationCommitPolicy;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationPolicy;
import java.time.LocalDate;
import java.util.List;
import static com.behsazan.corebanking.collateral.workflow.CollateralValuationCommitPolicy.*;

/** Standalone Java 21 executable tests: no Spring/Oracle or test dependency required. */
public final class R3ValuationCommitSmoke {
    private static int passed=0;
    private static final Command CMD=new Command("3","valuation.final.20261109.01","Documented valuation approval");
    private static final StageEvidence STAGE=new StageEvidence("REVIEWED","3",true,true,false,"maker","poster");
    private static final Release REL=new Release("VAL-20261109-POLICY", "G07-FINAL-MAPPING", "G18-APPRAISAL-DECISION",
            "G18-INTERVAL-DECISION","G18-TEHRAN-DATE-DOC","G09-VALUER-AUTH-REF",
            "G10-VALUE-POLICY-REF","G07-SUPERSESSION-REF","BANK-APPROVER-01",true);
    private static final TypeRule RULE=new TypeRule("24",true,1,"MONTH","EXCLUSIVE_NEXT_START",
            "EFFECTIVE_FROM","VAL-20261109-POLICY","RULE-APPROVER-01",true);
    private static final ValuerRule VALUER=new ValuerRule("42","24","MARKET", "VAL-20261109-POLICY",
            "VALUER-APPROVER-01",true);
    private static CollateralValuationPolicy.Candidate c(String currency,String amount,String valuer,
            String effectiveFrom,String effectiveTo) {
        return CollateralValuationPolicy.normalize(new CollateralValuationModels.CandidateRequest(
                "INITIAL","MARKET","2026-10-20",amount,currency,"900000","750000","25.00",
                valuer,"Appraiser Authority","APPRAISAL-REF-2026-01",effectiveFrom,effectiveTo,
                null,"Documented value for the collateral"));
    }
    private static void ok(boolean condition,String label) {
        if (!condition) throw new AssertionError(label);
        passed++;
    }
    private static void pass(Runnable job,String label) { job.run();ok(true,label); }
    private static void fail(Class<? extends Throwable> kind,Runnable job,String label) {
        try {job.run();throw new AssertionError("ACCEPTED_INVALID "+label);}
        catch(Throwable e){if(e.getClass()==AssertionError.class)throw e;
            if(!kind.isInstance(e))throw new AssertionError("WRONG_ERROR "+label+" "+e,e);
            passed++;}
    }
    public static void main(String[] args) {
        pass(()->guard(CMD,STAGE,REL,true,true,true),"approved stage");
        fail(IllegalArgumentException.class,()->guard(null,STAGE,REL,true,true,true),"null cmd");
        fail(IllegalArgumentException.class,()->guard(CMD,null,REL,true,true,true),"null stage");
        fail(IllegalArgumentException.class,()->expectedVersion("2"),"version");
        fail(IllegalArgumentException.class,()->idempotencyKey("x"),"key short");
        fail(IllegalArgumentException.class,()->idempotencyKey("space key"),"key space");
        fail(IllegalArgumentException.class,()->reason("few"),"reason");
        fail(IllegalStateException.class,()->guard(CMD,STAGE,REL,false,true,true),"feature off");
        fail(IllegalStateException.class,()->guard(CMD,STAGE,REL,true,false,true),"schema missing");
        fail(SecurityException.class,()->guard(CMD,STAGE,REL,true,true,false),"scope");
        fail(SecurityException.class,()->guard(CMD,new StageEvidence("REVIEWED","3",true,true,false,"maker","maker"),REL,true,true,true),"maker is poster");
        fail(SecurityException.class,()->guard(CMD,new StageEvidence("REVIEWED","3",true,true,false,"maker","BANK-APPROVER-01"),REL,true,true,true),"poster approves release");
        fail(IllegalStateException.class,()->guard(CMD,STAGE,null,true,true,true),"release missing");
        fail(IllegalStateException.class,()->guard(CMD,STAGE,new Release("x", "G07-FINAL-MAPPING", "G18-APPRAISAL-DECISION",
            "G18-INTERVAL-DECISION","G18-TEHRAN-DATE-DOC","G09-VALUER-AUTH-REF",
            "G10-VALUE-POLICY-REF","G07-SUPERSESSION-REF","BANK-APPROVER-01",true),true,true,true),"release short");
        fail(IllegalStateException.class,()->guard(CMD,STAGE,new Release("VAL-20261109-POLICY", "G07-FINAL-MAPPING", "G18-APPRAISAL-DECISION",
            "G18-INTERVAL-DECISION","G18-TEHRAN-DATE-DOC","G09-VALUER-AUTH-REF",
            "G10-VALUE-POLICY-REF","G07-SUPERSESSION-REF","BANK-APPROVER-01",false),true,true,true),"release revoked");
        for(String status:List.of("SUBMITTED","DRAFT","WITHDRAWN","REJECTED","FINAL")) {
            fail(IllegalStateException.class,()->guard(CMD,new StageEvidence(status,"3",true,true,false,"maker","poster"),REL,true,true,true),"bad status "+status);
        }
        fail(IllegalStateException.class,()->guard(CMD,new StageEvidence("REVIEWED","4",true,true,false,"maker","poster"),REL,true,true,true),"stage version");
        fail(IllegalStateException.class,()->guard(CMD,new StageEvidence("REVIEWED","3",false,true,false,"maker","poster"),REL,true,true,true),"integrity");
        fail(IllegalStateException.class,()->guard(CMD,new StageEvidence("REVIEWED","3",true,false,false,"maker","poster"),REL,true,true,true),"duplicate");
        fail(IllegalStateException.class,()->guard(CMD,new StageEvidence("REVIEWED","3",true,true,true,"maker","poster"),REL,true,true,true),"unknown evidence");
        var cand=c("IRR","1000000","42","2026-10-21","2026-11-20");
        ok(requirePeriod(RULE,"24",cand,REL,"poster",LocalDate.parse("2026-11-09")).equals(LocalDate.parse("2026-11-20")),"one month inclusive end");
        pass(()->requireValuer(VALUER,"24",cand,REL,"poster"),"authorized valuer");
        pass(()->requireAmount(cand,"IRR"),"same currency");
        fail(IllegalStateException.class,()->requirePeriod(null,"24",cand,REL,"poster",LocalDate.parse("2026-11-09")),"no type rule");
        fail(IllegalStateException.class,()->requirePeriod(new TypeRule("24",false,1,"MONTH","EXCLUSIVE_NEXT_START","EFFECTIVE_FROM","VAL-20261109-POLICY","RULE-APPROVER-01",true),"24",cand,REL,"poster",LocalDate.parse("2026-11-09")),"non-appraisable");
        fail(IllegalStateException.class,()->requirePeriod(RULE,"25",cand,REL,"poster",LocalDate.parse("2026-11-09")),"type mismatch");
        fail(IllegalStateException.class,()->requirePeriod(new TypeRule("24",true,1,"MONTH","EXCLUSIVE_NEXT_START","EFFECTIVE_FROM","OLD-REVISION","RULE-APPROVER-01",true),"24",cand,REL,"poster",LocalDate.parse("2026-11-09")),"rule wrong revision");
        fail(IllegalStateException.class,()->requirePeriod(new TypeRule("24",true,0,"MONTH","EXCLUSIVE_NEXT_START","EFFECTIVE_FROM","VAL-20261109-POLICY","RULE-APPROVER-01",true),"24",cand,REL,"poster",LocalDate.parse("2026-11-09")),"zero interval");
        fail(IllegalStateException.class,()->requirePeriod(new TypeRule("24",true,1,"WEEK","EXCLUSIVE_NEXT_START","EFFECTIVE_FROM","VAL-20261109-POLICY","RULE-APPROVER-01",true),"24",cand,REL,"poster",LocalDate.parse("2026-11-09")),"unit not approved");
        fail(IllegalStateException.class,()->requirePeriod(new TypeRule("24",true,1,"MONTH","INCLUSIVE_MONTH","EFFECTIVE_FROM","VAL-20261109-POLICY","RULE-APPROVER-01",true),"24",cand,REL,"poster",LocalDate.parse("2026-11-09")),"boundary unknown");
        fail(IllegalStateException.class,()->requirePeriod(new TypeRule("24",true,1,"MONTH","EXCLUSIVE_NEXT_START","OTHER","VAL-20261109-POLICY","RULE-APPROVER-01",true),"24",cand,REL,"poster",LocalDate.parse("2026-11-09")),"anchor unknown");
        fail(IllegalStateException.class,()->requirePeriod(RULE,"24",cand,REL,"RULE-APPROVER-01",LocalDate.parse("2026-11-09")),"same rule approver and poster");
        fail(IllegalStateException.class,()->requirePeriod(RULE,"24",cand,REL,"poster",null),"asof missing");
        fail(IllegalStateException.class,()->requirePeriod(RULE,"24",cand,REL,"poster",LocalDate.parse("2026-10-19")),"valuation future");
        fail(IllegalStateException.class,()->requirePeriod(RULE,"24",cand,REL,"poster",LocalDate.parse("2026-11-21")),"expired");
        fail(IllegalStateException.class,()->requirePeriod(RULE,"24",c("IRR","1000000","42","2026-10-21","2026-11-21"),REL,"poster",LocalDate.parse("2026-11-09")),"beyond approved term");
        fail(IllegalStateException.class,()->requirePeriod(RULE,"24",c("IRR","1000000","42","2026-10-21",null),REL,"poster",LocalDate.parse("2026-11-09")),"missing end");
        // Date-boundaries for e.g. leap month: no silent DAY approximation.
        var leap=c("IRR","1000000","42","2026-10-21","2026-11-20");
        ok(leap.effectiveTo().equals("2026-11-20"),"normalized explicit date");
        fail(IllegalStateException.class,()->requireValuer(null,"24",cand,REL,"poster"),"valuer missing rule");
        fail(IllegalStateException.class,()->requireValuer(VALUER,"24",c("IRR","1000000",null,"2026-10-21","2026-11-20"),REL,"poster"),"valuer name only");
        fail(IllegalStateException.class,()->requireValuer(VALUER,"24",cand,REL,"VALUER-APPROVER-01"),"valuer approver is poster");
        fail(IllegalStateException.class,()->requireValuer(new ValuerRule("43","24","MARKET","VAL-20261109-POLICY","VALUER-APPROVER-01",true),"24",cand,REL,"poster"),"valuer party wrong");
        fail(IllegalStateException.class,()->requireValuer(new ValuerRule("42","24","INDEX","VAL-20261109-POLICY","VALUER-APPROVER-01",true),"24",cand,REL,"poster"),"method wrong");
        fail(IllegalStateException.class,()->requireAmount(c("USD","1000000","42","2026-10-21","2026-11-20"),"IRR"),"FX not approved");
        fail(IllegalStateException.class,()->requireAmount(c("IRR","0","42","2026-10-21","2026-11-20"),"IRR"),"zero value");
        fail(IllegalStateException.class,()->requireAmount(c("IRR","800000","42","2026-10-21","2026-11-20"),"IRR"),"forced sale > assessed");
        pass(()->requireSupersession(null,null,null,null,null,null,"1","IRR","2026-11-01",0),"initial value no predecessor");
        fail(IllegalStateException.class,()->requireSupersession(null,"2",null,null,null,null,"1","IRR","2026-11-01",0),"unexpected initial supersedes");
        pass(()->requireSupersession("3","3","1","FINAL","IRR","2026-10-01","1","IRR","2026-11-01",0),"current final superseded");
        fail(IllegalStateException.class,()->requireSupersession("3","2","1","FINAL","IRR","2026-10-01","1","IRR","2026-11-01",0),"pointer stale");
        fail(IllegalStateException.class,()->requireSupersession("3","3","1","SUPERSEDED","IRR","2026-10-01","1","IRR","2026-11-01",0),"not final");
        fail(IllegalStateException.class,()->requireSupersession("3","3","1","FINAL","USD","2026-10-01","1","IRR","2026-11-01",0),"currency mismatch");
        fail(IllegalStateException.class,()->requireSupersession("3","3","2","FINAL","IRR","2026-10-01","1","IRR","2026-11-01",0),"parent mismatch");
        fail(IllegalStateException.class,()->requireSupersession("3","3","1","FINAL","IRR","2026-10-01","1","IRR","2026-11-01",1),"successor already exists");
        fail(IllegalStateException.class,()->requireSupersession("3","3","1","FINAL","IRR","2026-11-02","1","IRR","2026-11-01",0),"backdated replacement");
        ok(nextSequence(0)==1,"initial seq");
        ok(nextSequence(99)==100,"sequence advance");
        fail(IllegalStateException.class,()->nextSequence(-1),"negative seq");
        fail(IllegalStateException.class,()->nextSequence(9999999999L),"exhausted seq");
        pass(()->checkReplay(CMD,CMD.idempotencyKey(),"h","h",CMD.reason(),"3","poster","poster"),"idempotency replay");
        fail(IllegalStateException.class,()->checkReplay(CMD,"other","h","h",CMD.reason(),"3","poster","poster"),"key changed");
        fail(IllegalStateException.class,()->checkReplay(CMD,CMD.idempotencyKey(),"h","bad",CMD.reason(),"3","poster","poster"),"hash changed");
        fail(IllegalStateException.class,()->checkReplay(CMD,CMD.idempotencyKey(),"h","h","another reason","3","poster","poster"),"reason changed");
        fail(IllegalStateException.class,()->checkReplay(CMD,CMD.idempotencyKey(),"h","h",CMD.reason(),"2","poster","poster"),"version changed");
        fail(IllegalStateException.class,()->checkReplay(CMD,CMD.idempotencyKey(),"h","h",CMD.reason(),"3","poster","other"),"poster changed");
        ok(unresolvedAfterRegistration().size()==4,"residual gaps explicit");
        System.out.println("CLT_R3_VALUATION_POLICY_SMOKE_PASS="+passed+"/"+passed);
    }
}
