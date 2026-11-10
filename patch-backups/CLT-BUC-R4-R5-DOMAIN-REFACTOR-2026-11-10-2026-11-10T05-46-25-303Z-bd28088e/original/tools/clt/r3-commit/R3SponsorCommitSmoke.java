import com.behsazan.corebanking.collateral.workflow.CollateralR3SponsorCommitPolicy;
import com.behsazan.corebanking.collateral.workflow.CollateralR3SponsorCommitPolicy.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Standalone Java 21 policy contract tests; no Oracle or fake successful DML. */
public class R3SponsorCommitSmoke {
    private static int checks = 0;
    private static void yes(boolean result, String code) {
        if(!result) throw new AssertionError(code);
        checks++;
    }
    private static void fails(Class<? extends Throwable> expected, Runnable test, String code) {
        try {test.run();throw new AssertionError(code+" SHOULD_FAIL");}
        catch (Throwable thrown) {
            if(!expected.isInstance(thrown)) throw new AssertionError(code+": "+thrown,thrown);
            checks++;
        }
    }
    private static Command command() { return new Command("3","idem.sponsor.20261109-test", "Business approval recorded"); }
    private static Release approved() { return new Release("POLICY-REVISION-20261109","G05-DECISION-2026-11-09", "DBA-APPROVAL-2026-11-09", "G09-CONTRACT-APPROVED", "BANK-DECISION-OWNER", true); }
    private static StageEvidence stage() { return new StageEvidence("REVIEWED","3",true,true,false,"maker-01","checker-02"); }
    private static void guard(Command cmd,StageEvidence facts,Release release,boolean enabled,boolean schema,boolean auth) {
        CollateralR3SponsorCommitPolicy.guard(cmd,facts,release,enabled,schema,auth);
    }
    public static void main(String[] args) {
        guard(command(),stage(),approved(),true,true,true);yes(true,"valid");
        yes(CollateralR3SponsorCommitPolicy.version("3").equals("3"),"version ok");
        yes(CollateralR3SponsorCommitPolicy.key(command().idempotencyKey()).equals(command().idempotencyKey()),"key ok");
        yes(CollateralR3SponsorCommitPolicy.reason("  some good reason  ").equals("some good reason"),"reason normalized");
        fails(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.version("1"),"stale v1");
        fails(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.version("2"),"stale v2");
        fails(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.version("4"),"future v4");
        fails(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.version(null),"null version");
        fails(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.key("short"),"key short");
        fails(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.key("wrong key with spaces"),"key space");
        fails(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.key(null),"null key");
        fails(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.reason("no"),"reason short");
        fails(IllegalArgumentException.class,()->CollateralR3SponsorCommitPolicy.reason("x".repeat(501)),"reason long");
        fails(IllegalStateException.class,()->guard(command(),stage(),approved(),false,true,true),"feature off");
        fails(IllegalStateException.class,()->guard(command(),stage(),approved(),true,false,true),"schema off");
        fails(SecurityException.class,()->guard(command(),stage(),approved(),true,true,false),"no auth");
        fails(SecurityException.class,()->guard(command(),new StageEvidence("REVIEWED","3",true,true,false,"maker","maker"),approved(),true,true,true),"same maker");
        fails(SecurityException.class,()->guard(command(),new StageEvidence("REVIEWED","3",true,true,false,"maker",null),approved(),true,true,true),"no poster");
        fails(SecurityException.class,()->guard(command(),stage(),new Release("POLICY-REVISION-20261109","G05-DECISION-2026-11-09", "DBA-APPROVAL-2026-11-09", "G09-CONTRACT-APPROVED", "checker-02", true),true,true,true),"approver same as poster");
        fails(IllegalStateException.class,()->guard(command(),stage(),null,true,true,true),"release missing");
        fails(IllegalStateException.class,()->guard(command(),stage(),new Release("POLICY-REVISION-20261109","G05-DECISION-2026-11-09", "DBA-APPROVAL-2026-11-09", "G09-CONTRACT-APPROVED", "BANK-DECISION-OWNER", false),true,true,true),"release not approved");
        List<Release> releases=List.of(
           new Release("p","G05-DECISION-2026-11-09","DBA-APPROVAL-2026-11-09","G09-CONTRACT-APPROVED","BANK-DECISION-OWNER",true),
           new Release("POLICY-REVISION-20261109","short","DBA-APPROVAL-2026-11-09","G09-CONTRACT-APPROVED","BANK-DECISION-OWNER",true),
           new Release("POLICY-REVISION-20261109","G05-DECISION-2026-11-09","short","G09-CONTRACT-APPROVED","BANK-DECISION-OWNER",true),
           new Release("POLICY-REVISION-20261109","G05-DECISION-2026-11-09","DBA-APPROVAL-2026-11-09","short","BANK-DECISION-OWNER",true),
           new Release("POLICY-REVISION-20261109","G05-DECISION-2026-11-09","DBA-APPROVAL-2026-11-09","G09-CONTRACT-APPROVED","short",true)
        );
        for(Release x:releases) fails(IllegalStateException.class,()->guard(command(),stage(),x,true,true,true),"release refs");
        List<StageEvidence> bad=List.of(
           new StageEvidence("SUBMITTED","3",true,true,false,"maker","checker"),
           new StageEvidence("WITHDRAWN","3",true,true,false,"maker","checker"),
           new StageEvidence("REJECTED","3",true,true,false,"maker","checker"),
           new StageEvidence("DRAFT","3",true,true,false,"maker","checker"),
           new StageEvidence("REVIEWED","2",true,true,false,"maker","checker"),
           new StageEvidence("REVIEWED","3",false,true,false,"maker","checker"),
           new StageEvidence("REVIEWED","3",true,false,false,"maker","checker"),
           new StageEvidence("REVIEWED","3",true,true,true,"maker","checker")
        );
        for(StageEvidence x:bad) fails(IllegalStateException.class,()->guard(command(),x,approved(),true,true,true),"stage gate");
        fails(IllegalArgumentException.class,()->guard(null,stage(),approved(),true,true,true),"null command");
        fails(IllegalArgumentException.class,()->guard(command(),null,approved(),true,true,true),"null stage");
        CollateralR3SponsorCommitPolicy.checkReplay(command(),command().idempotencyKey(),"aaa","aaa","checker");yes(true,"exact replay");
        fails(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.checkReplay(command(),"different-key","aaa","aaa","checker"),"replay key mismatch");
        fails(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.checkReplay(command(),command().idempotencyKey(),"aaa","bbb","checker"),"replay hash mismatch");
        fails(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.checkReplay(command(),command().idempotencyKey(),"aaa","aaa",null),"replay no actor");
        CollateralR3SponsorCommitPolicy.requireExactlyOne(1L,"fail");yes(true,"one audit");
        fails(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.requireExactlyOne(0L,"no audit"),"no audit");
        fails(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.requireExactlyOne(2L,"duplicate"),"dup audit");
        yes(CollateralR3SponsorCommitPolicy.decimal(new BigDecimal("12"),"bad").equals(new BigDecimal("12")),"decimal ok");
        fails(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.decimal(new BigDecimal("0"),"bad"),"zero id");
        fails(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.decimal(null,"bad"),"missing id");
        fails(IllegalStateException.class,()->CollateralR3SponsorCommitPolicy.decimal(new BigDecimal("1".repeat(20)),"bad"),"huge id");
        yes(CollateralR3SponsorCommitPolicy.unresolvedOutsideAdd().size()==2,"unresolved not hidden");
        System.out.println("CLT_R3_SPONSOR_COMMIT_JAVA21_PASS="+checks+"/"+checks);
    }
}
