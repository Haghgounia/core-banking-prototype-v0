import com.behsazan.corebanking.collateral.allocation.CollateralAllocationIntakePolicy;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestPolicy;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.*;
import java.util.List;
import java.util.UUID;

/** Run with javac/java without Spring or Oracle; never performs network operations. */
public final class AllocationIntakePolicySmoke {
    private static int checks;
    private static void yes(boolean ok,String name) {if(!ok)throw new AssertionError(name);checks++;}
    private static void reject(Runnable body,String name) {
        try {body.run();throw new AssertionError("MUST_REJECT:"+name);}
        catch (IllegalArgumentException expected) {checks++;}
    }
    private static void rejectState(Runnable body,String name) {
        try {body.run();throw new AssertionError("MUST_REJECT:"+name);}
        catch (IllegalStateException expected) {checks++;}
    }
    public static void main(String[] args) {
        var p=CollateralAllocationIntakePolicy.class;
        yes(CollateralAllocationIntakePolicy.STATUS.equals("RECORDED_NOT_ALLOCATED"),"no financial status");
        yes(!CollateralAllocationIntakePolicy.everAuthorizesAllocation(),"never authorize");
        yes(CollateralAllocationIntakePolicy.EVIDENCE.contains("NON_ATOMIC"),"truthful evidence");
        yes(CollateralAllocationIntakePolicy.OPERATIONAL_BLOCKERS.size()>=5,"unresolved blockers");
        yes(CollateralAllocationIntakePolicy.OPERATIONAL_BLOCKERS.contains("CLT_ALLOCATION_CAPACITY_NOT_LOCKED"),"capacity stays unlocked");
        yes(CollateralAllocationIntakePolicy.OPERATIONAL_BLOCKERS.contains("G-09_CONSUMER_OBLIGATION_AND_ROSTER_NOT_VERIFIED"),"consumer not attested");
        yes(CollateralAllocationIntakePolicy.clientId("loan-service-01").equals("loan-service-01"),"client accepted");
        yes(CollateralAllocationIntakePolicy.clientId("service@bank.ir").contains("@"),"client at symbol");
        reject(()->CollateralAllocationIntakePolicy.clientId(""),"blank client");
        reject(()->CollateralAllocationIntakePolicy.clientId(" client"),"space client");
        reject(()->CollateralAllocationIntakePolicy.clientId("client\n"),"control client");
        reject(()->CollateralAllocationIntakePolicy.clientId("x".repeat(161)),"long client");
        yes(CollateralAllocationIntakePolicy.actor("svc-account").length()==11,"actor");
        reject(()->CollateralAllocationIntakePolicy.actor(""),"blank actor");
        reject(()->CollateralAllocationIntakePolicy.actor("x".repeat(101)),"long actor");
        yes(CollateralAllocationIntakePolicy.limit(1)==1,"min journal");
        yes(CollateralAllocationIntakePolicy.limit(50)==50,"max journal");
        reject(()->CollateralAllocationIntakePolicy.limit(0),"zero journal");
        reject(()->CollateralAllocationIntakePolicy.limit(51),"too many journal");
        CollateralAllocationIntakePolicy.requireReplayMatch("abcd","abcd",CollateralAllocationIntakePolicy.STATUS);checks++;
        reject(()->CollateralAllocationIntakePolicy.requireReplayMatch("abc","abD",CollateralAllocationIntakePolicy.STATUS),"different payload");
        rejectState(()->CollateralAllocationIntakePolicy.requireReplayMatch("abc","abc","ALLOCATED"),"unexpected allocated status");
        var debtors=List.of(new Debtor("9000000000000000001","PRIMARY"),new Debtor("9000000000000000002","JOINT"));
        var a=new Request("LOAN","FAC-2026-001","1234567890123456789","60000000.0000","IRR",debtors);
        var b=new Request("LOAN","FAC-2026-001","1234567890123456789","60000000","IRR",List.of(debtors.get(1),debtors.get(0)));
        var na=CollateralAllocationRequestPolicy.normalize(a);
        var nb=CollateralAllocationRequestPolicy.normalize(b);
        String hash=CollateralAllocationRequestPolicy.digest("001","svc-loan","1234567890123456789",na);
        String normalized=CollateralAllocationRequestPolicy.digest("001","svc-loan","1234567890123456789",nb);
        yes(hash.equals(normalized),"digest order/scale canonical");
        yes(hash.matches("[0-9a-f]{64}"),"SHA256");
        yes(!hash.equals(CollateralAllocationRequestPolicy.digest("002","svc-loan","1234567890123456789",na)),"branch scoped");
        yes(!hash.equals(CollateralAllocationRequestPolicy.digest("001","svc-other","1234567890123456789",na)),"client scoped");
        yes(!hash.equals(CollateralAllocationRequestPolicy.digest("001","svc-loan","1234567890123456788",na)),"collateral scoped");
        yes(!hash.equals(CollateralAllocationRequestPolicy.digest("001","svc-loan","1234567890123456789",CollateralAllocationRequestPolicy.normalize(new Request("LOAN","FAC-2026-001","1234567890123456789","60000001","IRR",debtors)))),"amount scoped");
        yes(CollateralAllocationRequestPolicy.key(UUID.randomUUID().toString()).length()==36,"UUID idempotency");
        reject(()->CollateralAllocationRequestPolicy.key("12345"),"unbounded idempotency");
        reject(()->CollateralAllocationRequestPolicy.normalize(new Request("LOAN","FAC-2026-001","1234567890123456789","60000000","IRR",List.of(new Debtor("9000000000000000001","JOINT")))),"missing primary");
        reject(()->CollateralAllocationRequestPolicy.normalize(new Request("LOAN","FAC-2026-001","1234567890123456789","60000000","IRR",List.of(debtors.get(0),debtors.get(0)))),"duplicate debtor");
        yes(na.debtors().size()==2,"normalized roster");
        yes(na.amount().equals("60000000"),"exact monetary normalization");
        System.out.println("CLT_R4C_ALLOCATION_INTAKE_POLICY_SMOKE_PASS="+checks);
    }
}
