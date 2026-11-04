import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationIntakePolicy;
import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationPolicy;
import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationModels.PreviewRequest;
import java.util.List;

/** 100% JDK policy smoke. No Oracle or Spring test claims. */
public final class UnallocationIntakePolicySmoke {
    private static int tests;
    private static void ok(boolean condition,String label) {
        tests++;
        if(!condition)throw new AssertionError("R5B_POLICY_"+label);
    }
    private static void invalid(Runnable action,String label) {
        boolean caught=false;
        try{action.run();}catch(IllegalArgumentException ex){caught=true;}
        ok(caught,label);
    }
    private static void conflict(Runnable action,String label) {
        boolean caught=false;
        try{action.run();}catch(CollateralUnallocationIntakePolicy.Conflict ex){caught=true;}
        ok(caught,label);
    }
    private static void stale(Runnable action,String label) {
        boolean caught=false;
        try{action.run();}catch(CollateralUnallocationIntakePolicy.Stale ex){caught=true;}
        ok(caught,label);
    }
    public static void main(String[] args) {
        var p=CollateralUnallocationIntakePolicy.class;
        ok(!CollateralUnallocationIntakePolicy.financialUnallocationAuthorized(),"financial_disabled");
        ok(!CollateralUnallocationIntakePolicy.finalReleaseAuthorized(),"final_disabled");
        ok(CollateralUnallocationIntakePolicy.RECORDED.equals("RECORDED_NOT_UNALLOCATED"),"recorded_only");
        ok(CollateralUnallocationIntakePolicy.CANCELLED.equals("CANCELLED_NOT_UNALLOCATED"),"cancelled_only");
        ok(CollateralUnallocationIntakePolicy.EVIDENCE.contains("NO_FINANCIAL"),"evidence_only");
        for(int limit=1;limit<=50;limit++)ok(CollateralUnallocationIntakePolicy.limit(limit)==limit,"limit_"+limit);
        for(int limit : new int[]{-10,-1,0,51,100,1000}){
            int bound=limit;
            invalid(()->CollateralUnallocationIntakePolicy.limit(bound),"invalid_limit_"+bound);
        }
        ok(CollateralUnallocationIntakePolicy.actor("svc-1").equals("svc-1"),"actor");
        invalid(()->CollateralUnallocationIntakePolicy.actor(""),"empty_actor");
        invalid(()->CollateralUnallocationIntakePolicy.actor(null),"null_actor");
        invalid(()->CollateralUnallocationIntakePolicy.actor(" "),"blank_actor");
        invalid(()->CollateralUnallocationIntakePolicy.actor("x".repeat(101)),"long_actor");
        ok(CollateralUnallocationIntakePolicy.reason("REF_1:/A").equals("REF_1:/A"),"reason");
        for(String value:new String[]{""," ","#BAD","DROP TABLE","a".repeat(101),"a\n"})
            invalid(()->CollateralUnallocationIntakePolicy.reason(value),"invalid_reason");
        invalid(()->CollateralUnallocationIntakePolicy.reason(null),"null_reason");
        var blockers=List.of("CLT_R5A_FINANCIAL_UNALLOCATION_AND_AUDIT_WRITES_DISABLED",
                "G-09_CONSUMER_OBLIGATION_STATUS_NOT_ATTESTED",
                "CLT_R5A_PARTIAL_UNALLOCATION_POLICY_NOT_APPROVED");
        String stored=CollateralUnallocationIntakePolicy.serializeBlockers(blockers);
        ok(CollateralUnallocationIntakePolicy.parseBlockers(stored).equals(blockers),"blocker_roundtrip");
        invalid(()->CollateralUnallocationIntakePolicy.snapshotBlockers(List.of("BAD")),"mandatory_blocker");
        invalid(()->CollateralUnallocationIntakePolicy.snapshotBlockers(List.of("CLT_R5A_FINANCIAL_UNALLOCATION_AND_AUDIT_WRITES_DISABLED","BAD!")),"invalid_blocker");
        invalid(()->CollateralUnallocationIntakePolicy.snapshotBlockers(List.of()),"empty_blockers");
        String d=CollateralUnallocationIntakePolicy.digest("a","bc");
        ok(d.length()==64,"digest_len");
        ok(d.equals(CollateralUnallocationIntakePolicy.digest("a","bc")),"digest_deterministic");
        ok(!d.equals(CollateralUnallocationIntakePolicy.digest("ab","c")),"digest_length_prefix");
        ok(!d.equals(CollateralUnallocationIntakePolicy.digest("bc","a")),"digest_order");
        var normal=CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","123","100.0000",
                "IRR","FULL","A-1"),"\"1\"");
        ok(normal.requestedAmount().equals("100"),"normalized_amount");
        ok(normal.expectedVersion().equals("1"),"normalized_version");
        var digest=CollateralUnallocationPolicy.digest("1","2","BR","client",normal);
        ok(digest.length()==64,"request_digest_len");
        ok(!digest.equals(CollateralUnallocationPolicy.digest("1","2","OTHER","client",normal)),"branch_bound");
        ok(!digest.equals(CollateralUnallocationPolicy.digest("1","2","BR","other",normal)),"client_bound");
        ok(!digest.equals(CollateralUnallocationPolicy.digest("1","3","BR","client",normal)),"allocation_bound");
        ok(!digest.equals(CollateralUnallocationPolicy.digest("2","2","BR","client",normal)),"collateral_bound");
        CollateralUnallocationIntakePolicy.replay(d,d);
        ok(true,"same_digest_replays");
        conflict(()->CollateralUnallocationIntakePolicy.replay(d,"f".repeat(64)),"different_digest_conflicts");
        conflict(()->CollateralUnallocationIntakePolicy.replay(null,d),"missing_digest_conflicts");
        CollateralUnallocationIntakePolicy.canCancel(CollateralUnallocationIntakePolicy.RECORDED,"1","1");
        ok(true,"cancel_recorded");
        stale(()->CollateralUnallocationIntakePolicy.canCancel(CollateralUnallocationIntakePolicy.RECORDED,"2","1"),"cancel_stale");
        conflict(()->CollateralUnallocationIntakePolicy.canCancel(CollateralUnallocationIntakePolicy.CANCELLED,"2","2"),"repeat_without_key_conflict");
        String cancel=CollateralUnallocationIntakePolicy.cancelDigest("1","B","C","LOAN","1","X");
        ok(cancel.length()==64,"cancel_digest_len");
        ok(cancel.equals(CollateralUnallocationIntakePolicy.cancelDigest("1","B","C","LOAN","1","X")),"cancel_digest_repeat");
        String[] values={"2","OTHER_BRANCH","OTHER_CLIENT","GUARANTEE","2","OTHER_REASON"};
        for(int i=0;i<values.length;i++){
            String[] v={"1","B","C","LOAN","1","X"};v[i]=values[i];
            ok(!cancel.equals(CollateralUnallocationIntakePolicy.cancelDigest(v[0],v[1],v[2],v[3],v[4],v[5])),
                    "cancel_domain_binding_"+i);
        }
        ok(CollateralUnallocationIntakePolicy.MANDATORY_BLOCKERS.size()>=5,"governance_blockers");
        System.out.println("CLT_R5B_UNALLOCATION_INTAKE_POLICY_SMOKE_PASS="+tests+
                "; ORACLE_EXECUTION=NONE; FINANCIAL_UNALLOCATION=DISABLED");
    }
}
