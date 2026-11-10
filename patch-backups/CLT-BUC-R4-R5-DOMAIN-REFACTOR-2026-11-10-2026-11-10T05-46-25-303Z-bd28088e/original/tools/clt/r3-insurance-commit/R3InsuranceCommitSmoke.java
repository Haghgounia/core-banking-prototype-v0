import com.behsazan.corebanking.collateral.workflow.CollateralR3InsuranceCommitPolicy;
import com.behsazan.corebanking.collateral.workflow.CollateralR3InsuranceCommitPolicy.*;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels;
import com.behsazan.corebanking.collateral.insurance.CollateralInsurancePolicy;
import java.math.BigDecimal;
import java.util.List;

/** Pure Java21 business guard smoke; no pretending that a successful DB transaction occurred. */
public class R3InsuranceCommitSmoke {
    private static int count;
    static void yes(boolean pass,String code) {if(!pass)throw new AssertionError(code);count++;}
    static void fails(Class<? extends Throwable> kind,Runnable f,String code) {
        try {f.run();throw new AssertionError("SHOULD_FAIL:"+code);}
        catch (Throwable e){if(!kind.isInstance(e))throw new AssertionError(code+":"+e,e);count++;}
    }
    static Command cmd(){return new Command("3","ins-post.20261109-0001","Approved insurance registration");}
    static Release release(){return new Release("INS-POLICY-20261109","G04-BUSINESS-APPROVED",
           "G04-DBA-DECISION-001","G09-EXTERNAL-DECISION","G04-COVERAGE-RULE-001",
           "G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-DECISION","BANK-BUSINESS-OWNER",true);}
    static StageEvidence stage(){return new StageEvidence("REVIEWED","3",true,true,false,"maker-01","poster-02");}
    static CollateralInsurancePolicy.Candidate candidate(String previous,String from,String to,String amount){
        return CollateralInsurancePolicy.candidate(new CollateralInsuranceModels.CandidateRequest(
            "2","12","42","123456","POL-2026-00001",amount,"0",null,
            from,to,previous,"IRR","Justified with insurance certificate"));
    }
    static CoverageRule rule(){return new CoverageRule("24","42","IRR","1000000",
            "INS-POLICY-20261109","BANK-RULE-OWNER",true);}
    static void guard(Command c,StageEvidence s,Release r,boolean feat,boolean schema,boolean permitted){
        CollateralR3InsuranceCommitPolicy.guard(c,s,r,feat,schema,permitted);
    }
    public static void main(String[] args){
        guard(cmd(),stage(),release(),true,true,true);yes(true,"positive Stage guard");
        CollateralR3InsuranceCommitPolicy.requireCoverage(rule(),"24",
           candidate(null,"2026-11-10","2027-11-10","1000000"),release(),"poster-02");
        yes(true,"approved minimum equal");
        CollateralR3InsuranceCommitPolicy.requireCoverage(rule(),"24",
           candidate(null,"2026-11-10","2027-11-10","5000000"),release(),"poster-02");
        yes(true,"approved minimum over");
        CollateralR3InsuranceCommitPolicy.requireRenewal(
           candidate(null,"2026-11-10","2027-11-10","1000000"),"55",null,null,null,22);
        yes(true,"initial no predecessor");
        CollateralR3InsuranceCommitPolicy.requireRenewal(
           candidate("77","2026-11-10","2027-11-10","1000000"),"55","55","42","2026-11-09",0);
        yes(true,"adjacent renewal valid");
        fails(IllegalArgumentException.class,()->CollateralR3InsuranceCommitPolicy.version("2"),"version2");
        fails(IllegalArgumentException.class,()->CollateralR3InsuranceCommitPolicy.version("4"),"version4");
        fails(IllegalArgumentException.class,()->CollateralR3InsuranceCommitPolicy.version(null),"versionnull");
        fails(IllegalArgumentException.class,()->CollateralR3InsuranceCommitPolicy.key("short"),"short idem");
        fails(IllegalArgumentException.class,()->CollateralR3InsuranceCommitPolicy.key("invalid key"),"spaces idem");
        fails(IllegalArgumentException.class,()->CollateralR3InsuranceCommitPolicy.key(null),"null idem");
        fails(IllegalArgumentException.class,()->CollateralR3InsuranceCommitPolicy.reason("short"),"short reason");
        fails(IllegalArgumentException.class,()->CollateralR3InsuranceCommitPolicy.reason("x".repeat(501)),"long reason");
        fails(IllegalArgumentException.class,()->guard(null,stage(),release(),true,true,true),"null cmd");
        fails(IllegalArgumentException.class,()->guard(cmd(),null,release(),true,true,true),"null stage");
        fails(IllegalStateException.class,()->guard(cmd(),stage(),release(),false,true,true),"flag off");
        fails(IllegalStateException.class,()->guard(cmd(),stage(),release(),true,false,true),"schema off");
        fails(SecurityException.class,()->guard(cmd(),stage(),release(),true,true,false),"not authorized");
        fails(SecurityException.class,()->guard(cmd(),new StageEvidence("REVIEWED","3",true,true,false,"maker","maker"),release(),true,true,true),"maker posting");
        fails(SecurityException.class,()->guard(cmd(),new StageEvidence("REVIEWED","3",true,true,false,"maker",null),release(),true,true,true),"no poster");
        fails(SecurityException.class,()->guard(cmd(),stage(),new Release("INS-POLICY-20261109","G04-BUSINESS-APPROVED",
           "G04-DBA-DECISION-001","G09-EXTERNAL-DECISION","G04-COVERAGE-RULE-001",
           "G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-DECISION","poster-02",true),true,true,true),"approver posting");
        fails(IllegalStateException.class,()->guard(cmd(),stage(),null,true,true,true),"no release");
        fails(IllegalStateException.class,()->guard(cmd(),stage(),new Release("INS-POLICY-20261109","G04-BUSINESS-APPROVED",
           "G04-DBA-DECISION-001","G09-EXTERNAL-DECISION","G04-COVERAGE-RULE-001",
           "G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-DECISION","BANK-BUSINESS-OWNER",false),true,true,true),"release revoked");
        List<Release> bad=List.of(
           new Release("short","G04-BUSINESS-APPROVED","G04-DBA-DECISION-001","G09-EXTERNAL-DECISION","G04-COVERAGE-RULE-001","G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-DECISION","BANK-BUSINESS-OWNER",true),
           new Release("INS-POLICY-20261109","short","G04-DBA-DECISION-001","G09-EXTERNAL-DECISION","G04-COVERAGE-RULE-001","G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-DECISION","BANK-BUSINESS-OWNER",true),
           new Release("INS-POLICY-20261109","G04-BUSINESS-APPROVED","short","G09-EXTERNAL-DECISION","G04-COVERAGE-RULE-001","G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-DECISION","BANK-BUSINESS-OWNER",true),
           new Release("INS-POLICY-20261109","G04-BUSINESS-APPROVED","G04-DBA-DECISION-001","short","G04-COVERAGE-RULE-001","G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-DECISION","BANK-BUSINESS-OWNER",true),
           new Release("INS-POLICY-20261109","G04-BUSINESS-APPROVED","G04-DBA-DECISION-001","G09-EXTERNAL-DECISION","short","G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-DECISION","BANK-BUSINESS-OWNER",true),
           new Release("INS-POLICY-20261109","G04-BUSINESS-APPROVED","G04-DBA-DECISION-001","G09-EXTERNAL-DECISION","G04-COVERAGE-RULE-001","short","G06-CANCEL-IMPACT-DECISION","BANK-BUSINESS-OWNER",true),
           new Release("INS-POLICY-20261109","G04-BUSINESS-APPROVED","G04-DBA-DECISION-001","G09-EXTERNAL-DECISION","G04-COVERAGE-RULE-001","G06-RENEWAL-DECISION","short","BANK-BUSINESS-OWNER",true),
           new Release("INS-POLICY-20261109","G04-BUSINESS-APPROVED","G04-DBA-DECISION-001","G09-EXTERNAL-DECISION","G04-COVERAGE-RULE-001","G06-RENEWAL-DECISION","G06-CANCEL-IMPACT-DECISION","short",true)
        );
        for(Release r:bad) fails(IllegalStateException.class,()->guard(cmd(),stage(),r,true,true,true),"bad decision reference");
        List<StageEvidence> invalid=List.of(
            new StageEvidence("DRAFT","3",true,true,false,"maker","reviewer"),
            new StageEvidence("SUBMITTED","3",true,true,false,"maker","reviewer"),
            new StageEvidence("WITHDRAWN","3",true,true,false,"maker","reviewer"),
            new StageEvidence("REJECTED","3",true,true,false,"maker","reviewer"),
            new StageEvidence("REVIEWED","2",true,true,false,"maker","reviewer"),
            new StageEvidence("REVIEWED","3",false,true,false,"maker","reviewer"),
            new StageEvidence("REVIEWED","3",true,false,false,"maker","reviewer"),
            new StageEvidence("REVIEWED","3",true,true,true,"maker","reviewer")
        );
        for(StageEvidence s:invalid) fails(IllegalStateException.class,()->guard(cmd(),s,release(),true,true,true),"stage invalid");
        var a=candidate(null,"2026-11-10","2027-11-10","1000000");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(null,"24",a,release(),"poster-02"),"no rule");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(new CoverageRule("24","42","IRR","1000000","INS-POLICY-20261109","BANK-RULE-OWNER",false),"24",a,release(),"poster-02"),"revoked rule");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(new CoverageRule("24","42","IRR","1000000","OLD-REVISION-001","BANK-RULE-OWNER",true),"24",a,release(),"poster-02"),"wrong revision");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(rule(),"25",a,release(),"poster-02"),"wrong collateral type");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(new CoverageRule("24","43","IRR","1000000","INS-POLICY-20261109","BANK-RULE-OWNER",true),"24",a,release(),"poster-02"),"wrong insurance type");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(new CoverageRule("24","42","USD","1000000","INS-POLICY-20261109","BANK-RULE-OWNER",true),"24",a,release(),"poster-02"),"wrong currency");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(new CoverageRule("24","42","IRR","1000000","INS-POLICY-20261109","poster-02",true),"24",a,release(),"poster-02"),"rule approver same as poster");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(new CoverageRule("24","42","IRR","bad","INS-POLICY-20261109","BANK-RULE-OWNER",true),"24",a,release(),"poster-02"),"rule invalid amount");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(new CoverageRule("24","42","IRR","0","INS-POLICY-20261109","BANK-RULE-OWNER",true),"24",a,release(),"poster-02"),"rule zero");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(new CoverageRule("24","42","IRR","1000000.00001","INS-POLICY-20261109","BANK-RULE-OWNER",true),"24",a,release(),"poster-02"),"rule excessive precision");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireCoverage(rule(),"24",candidate(null,"2026-11-10","2027-11-10","999999"),release(),"poster-02"),"insufficient coverage");
        var renewal=candidate("77","2026-11-10","2027-11-10","1000000");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireRenewal(renewal,"55",null,"42","2026-11-09",0),"missing previous");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireRenewal(renewal,"55","99","42","2026-11-09",0),"other collateral");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireRenewal(renewal,"55","55","13","2026-11-09",0),"other insurance type");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireRenewal(renewal,"55","55","42","2026-11-08",0),"gap in renewal");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireRenewal(renewal,"55","55","42","2026-11-10",0),"overlap in renewal");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireRenewal(renewal,"55","55","42","2026-11-09",1),"duplicate successor");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.requireRenewal(renewal,"55","55","42","bad date",0),"invalid predecessor date");
        CollateralR3InsuranceCommitPolicy.checkReplay(cmd(),cmd().idempotencyKey(),"aaa","aaa",cmd().reason(),"3","poster-02","poster-02");yes(true,"idempotent replay");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.checkReplay(cmd(),"different.key","aaa","aaa",cmd().reason(),"3","poster-02","poster-02"),"replay key");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.checkReplay(cmd(),cmd().idempotencyKey(),"aaa","bbb",cmd().reason(),"3","poster-02","poster-02"),"replay payload");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.checkReplay(cmd(),cmd().idempotencyKey(),"aaa","aaa","different reason","3","poster-02","poster-02"),"replay reason");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.checkReplay(cmd(),cmd().idempotencyKey(),"aaa","aaa",cmd().reason(),"2","poster-02","poster-02"),"replay version");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.checkReplay(cmd(),cmd().idempotencyKey(),"aaa","aaa",cmd().reason(),"3","poster-02","someone else"),"replay actor");
        yes(CollateralR3InsuranceCommitPolicy.positiveId(new BigDecimal("10"),"invalid").equals(BigDecimal.TEN),"positive key");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.positiveId(BigDecimal.ZERO,"invalid"),"zero key");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.positiveId(new BigDecimal("1.2"),"invalid"),"decimal key");
        fails(IllegalStateException.class,()->CollateralR3InsuranceCommitPolicy.positiveId(null,"invalid"),"null key");
        yes(CollateralR3InsuranceCommitPolicy.unresolvedAfterRegistration().size()==3,"open gaps retained");
        System.out.println("CLT_R3_INSURANCE_COMMIT_JAVA21_PASS="+count+"/"+count);
    }
}
