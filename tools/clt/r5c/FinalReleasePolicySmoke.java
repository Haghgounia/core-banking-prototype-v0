import com.behsazan.corebanking.collateral.release.CollateralFinalReleasePolicy;
import com.behsazan.corebanking.collateral.release.CollateralFinalReleasePolicy.Facts;
import java.util.List;

/** Runs offline, with JDK only. No Oracle, Spring context or CLT writes. */
public class FinalReleasePolicySmoke {
    private static int checks=0;
    private static void ok(boolean value){checks++;if(!value)throw new AssertionError("CASE_"+checks);}
    private static void bad(Runnable action){
        boolean denied=false;
        try{action.run();}catch(IllegalArgumentException e){denied=true;}
        ok(denied);
    }
    private static Facts facts(long total,long active,long foreign,long unknown,long pending,
            boolean inquiry,boolean terminal,String expected,String current){
        return new Facts(active,total,foreign,unknown,pending,inquiry,terminal,expected,current);
    }
    private static boolean has(Facts f,String code){return CollateralFinalReleasePolicy.blockers(f).contains(code);}
    public static void main(String[] args){
        String id="9223372036854775807";
        ok(CollateralFinalReleasePolicy.positiveId(id).equals(id));
        ok(CollateralFinalReleasePolicy.positiveId("1").equals("1"));
        ok(CollateralFinalReleasePolicy.positiveId("9999999999999999999").length()==19);
        for(String value:List.of("", "0", "01", "-1", "+1", "1.0", "a", " 1", "1 ",
                "10000000000000000000", "1e2", "1/0", "0000000000000000001"))
            bad(()->CollateralFinalReleasePolicy.positiveId(value));
        bad(()->CollateralFinalReleasePolicy.positiveId(null));
        ok(CollateralFinalReleasePolicy.limit(1)==1);
        ok(CollateralFinalReleasePolicy.limit(50)==50);
        for(int i:new int[]{0,-1,-999,51,500,Integer.MAX_VALUE}){
            final int v=i;bad(()->CollateralFinalReleasePolicy.limit(v));
        }
        ok(CollateralFinalReleasePolicy.strongEtag("\"1\"").equals("1"));
        ok(CollateralFinalReleasePolicy.strongEtag("\"9999999999\"").equals("9999999999"));
        for(String tag:List.of("0", "1", "\"0\"", "\"01\"", "W/\"1\"", "\"1\" ",
                "\"1.0\"", "\"-1\"", "\"10000000000\"", "\"1\"\""))
            bad(()->CollateralFinalReleasePolicy.strongEtag(tag));
        bad(()->CollateralFinalReleasePolicy.strongEtag(null));
        for(String reason:List.of("R", "A-1", "CASE_01", "A.B/C:D", "R".repeat(100)))
            ok(CollateralFinalReleasePolicy.reason(reason).equals(reason));
        for(String reason:List.of("", "x y", "../x", "é", "a".repeat(101), "#", "x' OR '1'='1", "\u0622"))
            bad(()->CollateralFinalReleasePolicy.reason(reason));
        bad(()->CollateralFinalReleasePolicy.reason(null));
        String key="550e8400-e29b-41d4-a716-446655440000";
        ok(CollateralFinalReleasePolicy.idempotencyKey(key).equals(key));
        for(String bad:List.of("", "A", key.toUpperCase(), "550e8400-e29b-41d4-0716-446655440000",
                "550e8400-e29b-01d4-a716-446655440000", "550e8400e29b41d4a716446655440000", "\n"+key))
            bad(()->CollateralFinalReleasePolicy.idempotencyKey(bad));
        bad(()->CollateralFinalReleasePolicy.idempotencyKey(null));
        Facts clean=facts(0,0,0,0,0,false,false,null,"1");
        ok(!CollateralFinalReleasePolicy.finalReleaseAllowed(clean));
        ok(CollateralFinalReleasePolicy.blockers(clean).contains("G-21_LEGAL_HOLDS_DPS2_PLEDGES_AND_INQUIRIES_NOT_VERIFIED"));
        ok(CollateralFinalReleasePolicy.blockers(clean).contains("CLT_R5C_FINAL_RELEASE_AND_STATUS_WRITES_DISABLED"));
        ok(CollateralFinalReleasePolicy.blockers(clean).size()>=6);
        ok(has(facts(1,1,0,0,0,false,false,null,"1"),"CLT_R5C_ACTIVE_ALLOCATIONS_REMAIN"));
        ok(has(facts(1,0,1,0,0,false,false,null,"1"),"CLT_R5C_CROSS_BRANCH_ALLOCATION_REVIEW_REQUIRED"));
        ok(has(facts(1,0,0,1,0,false,false,null,"1"),"CLT_R5C_UNKNOWN_ALLOCATION_USAGE_STATE"));
        ok(has(facts(0,0,0,0,1,false,false,null,"1"),"CLT_R5C_PENDING_TRANSACTIONS_REMAIN"));
        ok(has(facts(0,0,0,0,0,true,false,null,"1"),"CLT_R5C_REQUIRED_RELEASE_INQUIRY_NOT_ATTESTED"));
        ok(has(facts(0,0,0,0,0,false,true,null,"1"),"CLT_R5C_TERMINAL_STATUS_IS_NOT_RELEASE_PROOF"));
        ok(has(facts(0,0,0,0,0,false,false,"2","1"),"CLT_R5C_COLLATERAL_VERSION_MISMATCH"));
        ok(!has(facts(0,0,0,0,0,false,false,"1","1"),"CLT_R5C_COLLATERAL_VERSION_MISMATCH"));
        for(String permanent:List.of("G-09_CONSUMER_OBLIGATION_CLEARANCE_NOT_ATTESTED",
                "G-13_CROSS_BRANCH_LEGAL_AUTHORIZATION_UNCONFIRMED",
                "G-20_FINANCIAL_UNALLOCATION_AND_LEDGER_NOT_FINALIZED",
                "G-21_LEGAL_HOLDS_DPS2_PLEDGES_AND_INQUIRIES_NOT_VERIFIED",
                "CLT_R5C_MULTI_QUERY_EVIDENCE_NOT_TRANSACTIONALLY_LOCKED",
                "CLT_R5C_FINAL_RELEASE_AND_STATUS_WRITES_DISABLED"))ok(has(clean,permanent));
        for(Facts broken:List.of(facts(-1,0,0,0,0,false,false,null,"1"),
                facts(1,-1,0,0,0,false,false,null,"1"),
                facts(0,0,-1,0,0,false,false,null,"1"),
                facts(0,0,0,-1,0,false,false,null,"1"),
                facts(0,0,0,0,-1,false,false,null,"1"),
                facts(0,1,0,0,0,false,false,null,"1")))bad(()->CollateralFinalReleasePolicy.blockers(broken));
        bad(()->CollateralFinalReleasePolicy.blockers(null));
        String digest=CollateralFinalReleasePolicy.digest("1","101","2","CASE-A",key);
        ok(digest.matches("[0-9a-f]{64}"));
        ok(digest.equals(CollateralFinalReleasePolicy.digest("1","101","2","CASE-A",key)));
        ok(!digest.equals(CollateralFinalReleasePolicy.digest("2","101","2","CASE-A",key)));
        ok(!digest.equals(CollateralFinalReleasePolicy.digest("1","102","2","CASE-A",key)));
        ok(!digest.equals(CollateralFinalReleasePolicy.digest("1","101","3","CASE-A",key)));
        ok(!digest.equals(CollateralFinalReleasePolicy.digest("1","101","2","CASE-B",key)));
        ok(!digest.equals(CollateralFinalReleasePolicy.digest("1","101","2","CASE-A","550e8400-e29b-41d4-a716-446655440001")));
        ok(!CollateralFinalReleasePolicy.finalReleaseAllowed(facts(1,1,0,0,0,false,false,"1","1")));
        ok(!CollateralFinalReleasePolicy.finalReleaseAllowed(facts(0,0,0,0,0,false,true,"1","1")));
        ok(!CollateralFinalReleasePolicy.finalReleaseAllowed(facts(0,0,0,0,0,true,false,"1","1")));
        System.out.println("CLT_R5C_FINAL_RELEASE_POLICY_SMOKE_PASS="+checks+"; ORACLE_EXECUTION=NONE; FINANCIAL_RELEASE=DISABLED");
    }
}
