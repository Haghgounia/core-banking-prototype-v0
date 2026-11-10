import com.behsazan.corebanking.collateral.crossunit.CollateralCrossUnitPolicy;
import java.time.Instant;
import java.util.List;
public final class CrossUnitPolicySmoke {
    private static int checks=0;
    private static void ok(boolean cond){checks++;if(!cond)throw new AssertionError("FAILED "+checks);}
    private static void bad(Runnable run){try {run.run();throw new AssertionError("EXPECTED ERROR");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args){
        Instant now=Instant.parse("2026-11-10T10:00:00Z"),expires=now.plusSeconds(3600);
        ok(CollateralCrossUnitPolicy.authorized(true,true,"APPROVED","001","002","002","4","4",expires,now));
        ok(!CollateralCrossUnitPolicy.authorized(false,true,"APPROVED","001","002","002","4","4",expires,now));
        ok(!CollateralCrossUnitPolicy.authorized(true,false,"APPROVED","001","002","002","4","4",expires,now));
        ok(!CollateralCrossUnitPolicy.authorized(true,true,"PENDING","001","002","002","4","4",expires,now));
        ok(!CollateralCrossUnitPolicy.authorized(true,true,"REVOKED","001","002","002","4","4",expires,now));
        ok(!CollateralCrossUnitPolicy.authorized(true,true,"APPROVED","001","001","001","4","4",expires,now));
        ok(!CollateralCrossUnitPolicy.authorized(true,true,"APPROVED","001","002","003","4","4",expires,now));
        ok(!CollateralCrossUnitPolicy.authorized(true,true,"APPROVED","001","002","002","3","4",expires,now));
        ok(!CollateralCrossUnitPolicy.authorized(true,true,"APPROVED","001","002","002","4","4",now,now));
        ok(!CollateralCrossUnitPolicy.authorized(true,true,"APPROVED","001","002","002","4","4",null,now));
        bad(()->CollateralCrossUnitPolicy.otherBranch("003","003"));
        bad(()->CollateralCrossUnitPolicy.otherBranch("../","002"));
        bad(()->CollateralCrossUnitPolicy.checker("MAKER","MAKER"));
        CollateralCrossUnitPolicy.checker("MAKER","CHECKER");checks++;
        ok(CollateralCrossUnitPolicy.expiry("2026-11-11T10:00:00Z",now).equals(now.plusSeconds(86400)));
        bad(()->CollateralCrossUnitPolicy.expiry("2026-11-11",now));
        bad(()->CollateralCrossUnitPolicy.expiry("2026-11-09T10:00:00Z",now));
        bad(()->CollateralCrossUnitPolicy.expiry("2028-11-10T10:00:00Z",now));
        ok(CollateralCrossUnitPolicy.id("123").equals("123"));
        bad(()->CollateralCrossUnitPolicy.id("01"));
        bad(()->CollateralCrossUnitPolicy.id("1 OR 1=1"));
        bad(()->CollateralCrossUnitPolicy.reference("../unsafe?"));
        ok(CollateralCrossUnitPolicy.reference("BANK:123/X-3").equals("BANK:123/X-3"));
        ok(CollateralCrossUnitPolicy.requestKey("d2c7335d-a12e-4f66-af01-ac6b998bb46e").length()==36);
        bad(()->CollateralCrossUnitPolicy.requestKey("not-a-uuid"));
        String a=CollateralCrossUnitPolicy.hash(List.of("a","bc")),b=CollateralCrossUnitPolicy.hash(List.of("ab","c"));
        ok(!a.equals(b));ok(a.equals(CollateralCrossUnitPolicy.hash(List.of("a","bc"))));
        CollateralCrossUnitPolicy.sameDigest(a,a);checks++;
        bad(()->CollateralCrossUnitPolicy.sameDigest(a,b));
        bad(()->CollateralCrossUnitPolicy.sameDigest(null,b));
        bad(()->CollateralCrossUnitPolicy.subject(""));
        System.out.println("CLT_BUC15_POLICY_PASS="+checks+"/"+checks);
    }
}
