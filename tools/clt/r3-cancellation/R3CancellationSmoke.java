import com.behsazan.corebanking.collateral.insurance.cancellation.CollateralInsuranceCancellationPolicy;
import com.behsazan.corebanking.collateral.insurance.cancellation.CollateralInsuranceCancellationPolicy.*;
import java.util.List;

/** JDK21 independent policy smoke; no Oracle/Spring claims. */
public final class R3CancellationSmoke {
    private static int tests;
    private static void ok(boolean p, String label) {
        tests++;
        if (!p) throw new AssertionError("CLT_R3_CANCEL_SMOKE_FAILED: " + label);
    }
    private static void blocks(Runnable f,String label) {
        tests++;
        try { f.run(); } catch (IllegalArgumentException | IllegalStateException | SecurityException expected) {return;}
        throw new AssertionError("CLT_R3_CANCEL_SHOULD_BLOCK: " + label);
    }
    static Facts facts(long allocations, long successors, boolean cancelled,boolean ledger) {
        return new Facts(true,true,true,"ACTIVE","2026-01-01","2026-12-31","2026-11-10",
                allocations,successors,cancelled,ledger,true,true);
    }
    static Release release() {return new Release("POLICY_REVISION_20261110","BUSINESS_APPROVED_1234",
            "DATA_APPROVED_1234","COVERAGE_APPROVED_1234","ALLOCATION_APPROVED_1234",
            "INTEGRATION_APPROVED_1234","DOWNSTREAM_BINDING_APPROVED_1234",
            "BANK_APPROVER_DIFFERENT",true);}
    static Impact impact() {return new Impact("IMPACT_APPROVED_1234","COVERAGE_APPROVED_1234",
            "ALLOCATION_APPROVED_1234","RISK_APPROVER_DIFFERENT","13",true);}
    static Stage stage() {return new Stage("REVIEWED","MAKER_DIFFERENT","REVIEWER_DIFFERENT",
            "POSTER_DIFFERENT","a".repeat(64),"a".repeat(64));}
    static Command command() {return new Command("3","clt-cancel-20261110-0001","Confirmed business reason");}
    private static void posting(Command c, Stage s, Release r, Impact i, Assessment a,
            boolean flag,boolean schema,boolean scope,boolean replacement,boolean noAlloc) {
        CollateralInsuranceCancellationPolicy.requirePosting(c,s,r,i,a,flag,schema,scope,replacement,noAlloc);
    }
    public static void main(String[] unused) {
        Assessment baseline=CollateralInsuranceCancellationPolicy.assess(facts(0,0,false,true));
        ok(baseline.requestCandidate(),"baseline stage candidate");
        ok(!baseline.automaticCancellationAllowed(),"no automatic cancellation");
        ok(baseline.knownAllocationFree(),"known no allocation rows");
        ok(baseline.historyPreserved(),"history preserved");
        ok(baseline.checks().size()==10,"all checks explicit");
        ok(baseline.checks().stream().filter(x->"UNKNOWN".equals(x.result())).count()==3,"unapproved rules unknown");
        ok(!CollateralInsuranceCancellationPolicy.assess(facts(1,0,false,true)).knownAllocationFree(),"one allocation blocks");
        ok(!CollateralInsuranceCancellationPolicy.assess(facts(999,0,false,true)).knownAllocationFree(),"many allocations block");
        ok(!CollateralInsuranceCancellationPolicy.assess(facts(0,1,false,true)).requestCandidate(),"successor blocks");
        ok(!CollateralInsuranceCancellationPolicy.assess(facts(0,0,true,true)).requestCandidate(),"prior cancellation blocks");
        ok(!CollateralInsuranceCancellationPolicy.assess(facts(0,0,false,false)).requestCandidate(),"no event ledger not proof");
        ok(!CollateralInsuranceCancellationPolicy.assess(new Facts(true,true,false,"ACTIVE",null,null,null,0,0,false,true,true,true)).requestCandidate(),"branch mismatch");
        ok(!CollateralInsuranceCancellationPolicy.assess(new Facts(false,true,true,"ACTIVE",null,null,null,0,0,false,true,true,true)).requestCandidate(),"missing policy");
        ok(!CollateralInsuranceCancellationPolicy.assess(new Facts(true,false,true,"ACTIVE",null,null,null,0,0,false,true,true,true)).requestCandidate(),"collateral mismatch");
        ok(!CollateralInsuranceCancellationPolicy.assess(new Facts(true,true,true,"ACTIVE",null,null,null,0,0,false,true,false,true)).requestCandidate(),"missing version");
        ok(CollateralInsuranceCancellationPolicy.assess(facts(-1,0,false,true)).checks().stream()
                .anyMatch(x->"NO_ALLOCATION_ROWS".equals(x.code()) && "BLOCKED".equals(x.result())),"unknown allocation blocks");
        for (String state: List.of("RELEASED","CLOSED","INACTIVE","")) {
            Assessment a=CollateralInsuranceCancellationPolicy.assess(new Facts(true,true,true,state,null,null,null,0,0,false,true,true,true));
            ok(a.checks().stream().anyMatch(x->"COLLATERAL_NOT_TERMINATED".equals(x.code()) && "BLOCKED".equals(x.result())),"status "+state);
        }
        for (String bad:new String[]{null,"","0","01","-1","abc","12345678901234567890"}) {
            blocks(()->CollateralInsuranceCancellationPolicy.id(bad),"invalid id");
        }
        for (String bad:new String[]{null,"","0","01","-1","abc","12345678901"}) {
            blocks(()->CollateralInsuranceCancellationPolicy.version(bad),"invalid version");
        }
        for (String bad:new String[]{null,"","short","seven77","x".repeat(501)}) {
            blocks(()->CollateralInsuranceCancellationPolicy.reason(bad),"invalid reason");
        }
        for (String bad:new String[]{null,"","x","space key 1111","x".repeat(101)}) {
            blocks(()->CollateralInsuranceCancellationPolicy.key(bad),"invalid idempotency key");
        }
        ok("123".equals(CollateralInsuranceCancellationPolicy.id("123")),"valid id");
        ok("3".equals(CollateralInsuranceCancellationPolicy.version("3")),"valid version");
        ok("12char-reason".equals(CollateralInsuranceCancellationPolicy.reason("  12char-reason  ")),"trim reason");
        ok("clt-cancel-123".equals(CollateralInsuranceCancellationPolicy.key("clt-cancel-123")),"valid key");
        ok(CollateralInsuranceCancellationPolicy.digest("canonical").matches("[0-9a-f]{64}"),"SHA256 hex");
        CollateralInsuranceCancellationPolicy.requireStageTransition("DRAFT","SUBMIT","MAKER_DIFFERENT",null,"MAKER_DIFFERENT",true);
        ok(true,"maker submit");
        CollateralInsuranceCancellationPolicy.requireStageTransition("DRAFT","WITHDRAW","MAKER_DIFFERENT",null,"MAKER_DIFFERENT",true);
        ok(true,"maker withdraw draft");
        CollateralInsuranceCancellationPolicy.requireStageTransition("SUBMITTED","WITHDRAW","MAKER_DIFFERENT",null,"MAKER_DIFFERENT",true);
        ok(true,"maker withdraw submitted");
        CollateralInsuranceCancellationPolicy.requireStageTransition("SUBMITTED","REVIEW","MAKER_DIFFERENT",null,"REVIEWER_DIFFERENT",true);
        ok(true,"independent reviewer review");
        CollateralInsuranceCancellationPolicy.requireStageTransition("SUBMITTED","REJECT","MAKER_DIFFERENT",null,"REVIEWER_DIFFERENT",true);
        ok(true,"independent reviewer reject");
        for (String action:List.of("SUBMIT","WITHDRAW","REVIEW","REJECT")) {
            blocks(()->CollateralInsuranceCancellationPolicy.requireStageTransition("COMMITTED",action,
                    "MAKER_DIFFERENT",null,"REVIEWER_DIFFERENT",true),"terminal cannot move "+action);
        }
        blocks(()->CollateralInsuranceCancellationPolicy.requireStageTransition("DRAFT","REVIEW",
                "MAKER_DIFFERENT",null,"REVIEWER_DIFFERENT",true),"no draft review");
        blocks(()->CollateralInsuranceCancellationPolicy.requireStageTransition("SUBMITTED","SUBMIT",
                "MAKER_DIFFERENT",null,"MAKER_DIFFERENT",true),"no double submit");
        blocks(()->CollateralInsuranceCancellationPolicy.requireStageTransition("SUBMITTED","REVIEW",
                "MAKER_DIFFERENT",null,"MAKER_DIFFERENT",true),"maker cannot review");
        blocks(()->CollateralInsuranceCancellationPolicy.requireStageTransition("SUBMITTED","REVIEW",
                "MAKER_DIFFERENT","OTHER_REVIEWER","REVIEWER_DIFFERENT",true),"reviewer already recorded");
        blocks(()->CollateralInsuranceCancellationPolicy.requireStageTransition("DRAFT","SUBMIT",
                "MAKER_DIFFERENT",null,"OTHER",true),"nonmaker submit");
        blocks(()->CollateralInsuranceCancellationPolicy.requireStageTransition("DRAFT","SUBMIT",
                "MAKER_DIFFERENT",null,"MAKER_DIFFERENT",false),"scope false");
        blocks(()->CollateralInsuranceCancellationPolicy.requireStageTransition("DRAFT","OTHER",
                "MAKER_DIFFERENT",null,"MAKER_DIFFERENT",true),"invalid action");
        posting(command(),stage(),release(),impact(),baseline,true,true,true,true,true);
        ok(true,"fully approved guarded candidate");
        blocks(()->posting(command(),stage(),release(),impact(),baseline,false,true,true,true,true),"feature closed");
        blocks(()->posting(command(),stage(),release(),impact(),baseline,true,false,true,true,true),"schema closed");
        blocks(()->posting(command(),stage(),release(),impact(),baseline,true,true,false,true,true),"no scope");
        blocks(()->posting(command(),stage(),release(),impact(),baseline,true,true,true,false,true),"replacement absent");
        blocks(()->posting(command(),stage(),release(),impact(),baseline,true,true,true,true,false),"allocations present");
        blocks(()->posting(command(),stage(),release(),impact(),CollateralInsuranceCancellationPolicy.assess(facts(1,0,false,true)),true,true,true,true,true),"snapshot allocations");
        blocks(()->posting(command(),stage(),release(),impact(),CollateralInsuranceCancellationPolicy.assess(facts(0,1,false,true)),true,true,true,true,true),"snapshot successor");
        blocks(()->posting(command(),stage(),release(),impact(),CollateralInsuranceCancellationPolicy.assess(facts(0,0,true,true)),true,true,true,true,true),"snapshot prior cancel");
        blocks(()->posting(command(),stage(),release(),impact(),CollateralInsuranceCancellationPolicy.assess(facts(0,0,false,false)),true,true,true,true,true),"snapshot no ledger");
        blocks(()->posting(command(),new Stage("SUBMITTED",stage().maker(),stage().reviewer(),stage().poster(),stage().requestHash(),stage().persistedHash()),release(),impact(),baseline,true,true,true,true,true),"not reviewed");
        blocks(()->posting(command(),new Stage("REVIEWED",stage().maker(),stage().maker(),stage().poster(),stage().requestHash(),stage().persistedHash()),release(),impact(),baseline,true,true,true,true,true),"same reviewer and maker");
        blocks(()->posting(command(),new Stage("REVIEWED",stage().maker(),stage().reviewer(),stage().reviewer(),stage().requestHash(),stage().persistedHash()),release(),impact(),baseline,true,true,true,true,true),"same reviewer and poster");
        blocks(()->posting(command(),new Stage("REVIEWED",stage().maker(),stage().reviewer(),stage().maker(),stage().requestHash(),stage().persistedHash()),release(),impact(),baseline,true,true,true,true,true),"same maker and poster");
        blocks(()->posting(command(),new Stage("REVIEWED",stage().maker(),stage().reviewer(),stage().poster(),"a".repeat(64),"b".repeat(64)),release(),impact(),baseline,true,true,true,true,true),"digest tampering");
        blocks(()->posting(command(),stage(),null,impact(),baseline,true,true,true,true,true),"release missing");
        blocks(()->posting(command(),stage(),new Release(release().policyRevision(),release().businessRef(),release().dataRef(),release().coverageRef(),release().allocationRef(),release().integrationRef(),release().downstreamRef(),release().approvedBy(),false),impact(),baseline,true,true,true,true,true),"release revoked");
        blocks(()->posting(command(),stage(),new Release("short",release().businessRef(),release().dataRef(),release().coverageRef(),release().allocationRef(),release().integrationRef(),release().downstreamRef(),release().approvedBy(),true),impact(),baseline,true,true,true,true,true),"revision too short");
        blocks(()->posting(command(),stage(),new Release(release().policyRevision(),"",release().dataRef(),release().coverageRef(),release().allocationRef(),release().integrationRef(),release().downstreamRef(),release().approvedBy(),true),impact(),baseline,true,true,true,true,true),"business decision missing");
        blocks(()->posting(command(),stage(),new Release(release().policyRevision(),release().businessRef(),release().dataRef(),release().coverageRef(),release().allocationRef(),release().integrationRef(),"",release().approvedBy(),true),impact(),baseline,true,true,true,true,true),"downstream binding missing");
        blocks(()->posting(command(),stage(),new Release(release().policyRevision(),release().businessRef(),release().dataRef(),release().coverageRef(),release().allocationRef(),release().integrationRef(),release().downstreamRef(),stage().poster(),true),impact(),baseline,true,true,true,true,true),"poster equals bank approver");
        blocks(()->posting(command(),stage(),release(),null,baseline,true,true,true,true,true),"impact missing");
        blocks(()->posting(command(),stage(),release(),new Impact(impact().decisionRef(),impact().coverageDecisionRef(),impact().allocationDecisionRef(),impact().approvedBy(),impact().replacementInsuranceId(),false),baseline,true,true,true,true,true),"impact revoked");
        blocks(()->posting(command(),stage(),release(),new Impact("",impact().coverageDecisionRef(),impact().allocationDecisionRef(),impact().approvedBy(),impact().replacementInsuranceId(),true),baseline,true,true,true,true,true),"impact ref missing");
        blocks(()->posting(command(),stage(),release(),new Impact(impact().decisionRef(),impact().coverageDecisionRef(),impact().allocationDecisionRef(),stage().maker(),impact().replacementInsuranceId(),true),baseline,true,true,true,true,true),"maker equals impact approver");
        blocks(()->posting(command(),stage(),release(),new Impact(impact().decisionRef(),impact().coverageDecisionRef(),impact().allocationDecisionRef(),stage().reviewer(),impact().replacementInsuranceId(),true),baseline,true,true,true,true,true),"reviewer equals impact approver");
        blocks(()->posting(command(),stage(),release(),new Impact(impact().decisionRef(),impact().coverageDecisionRef(),impact().allocationDecisionRef(),stage().poster(),impact().replacementInsuranceId(),true),baseline,true,true,true,true,true),"poster equals impact approver");
        blocks(()->posting(null,stage(),release(),impact(),baseline,true,true,true,true,true),"command absent");
        blocks(()->posting(new Command("0",command().idempotencyKey(),command().reason()),stage(),release(),impact(),baseline,true,true,true,true,true),"bad expected version");
        blocks(()->posting(new Command("3","bad",command().reason()),stage(),release(),impact(),baseline,true,true,true,true,true),"bad key");
        blocks(()->posting(new Command("3",command().idempotencyKey(),"bad"),stage(),release(),impact(),baseline,true,true,true,true,true),"bad reason");
        Ledger stored=new Ledger("10","11","12","13",command().idempotencyKey(),"a".repeat(64),command().reason(),command().expectedVersion(),stage().poster(),release().policyRevision());
        CollateralInsuranceCancellationPolicy.requireReplay("10",command(),stage(),stored,"a".repeat(64),release().policyRevision());
        ok(true,"replay exact match");
        blocks(()->CollateralInsuranceCancellationPolicy.requireReplay("99",command(),stage(),stored,"a".repeat(64),release().policyRevision()),"replay different request");
        blocks(()->CollateralInsuranceCancellationPolicy.requireReplay("10",command(),stage(),stored,"b".repeat(64),release().policyRevision()),"replay different hash");
        blocks(()->CollateralInsuranceCancellationPolicy.requireReplay("10",new Command("3","another-valid-key",command().reason()),stage(),stored,"a".repeat(64),release().policyRevision()),"replay different key");
        blocks(()->CollateralInsuranceCancellationPolicy.requireReplay("10",command(),new Stage(stage().status(),stage().maker(),stage().reviewer(),"OTHER_POSTER",stage().requestHash(),stage().persistedHash()),stored,"a".repeat(64),release().policyRevision()),"replay different actor");
        blocks(()->CollateralInsuranceCancellationPolicy.requireReplay("10",command(),stage(),stored,"a".repeat(64),"OTHER_REVISION"),"replay different revision");
        ok("2026-11-10".equals(CollateralInsuranceCancellationPolicy.asOf(java.time.LocalDate.of(2026,11,10))),"date canonical");
        System.out.println("CLT_R3_CANCELLATION_POLICY_PASS=" + tests + "/" + tests);
    }
}
