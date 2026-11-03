import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestModels.*;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationRequestPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Executable pure Java regression; no HTTP, Oracle or production configuration needed. */
public final class AllocationRequestPolicySmoke {
    private static int pass;
    private static void ok(boolean result, String reason) {
        if (!result) throw new AssertionError(reason);
        pass++;
    }
    private static void fail(Runnable command, String code) {
        try {command.run();throw new AssertionError("accepted:"+code);}
        catch (IllegalArgumentException e) {ok(code.equals(e.getMessage()),"wrong failure:"+code+" -> "+e.getMessage());}
    }
    private static Request req(String domain, String caseRef, String file, String amount,
            String currency, List<Debtor> debtors) {
        return new Request(domain,caseRef,file,amount,currency,debtors);
    }
    private static final List<Debtor> MAIN = List.of(new Debtor("9000000000000000123", "PRIMARY"));
    public static void main(String[] args) {
        var n=CollateralAllocationRequestPolicy.normalize(req("LOAN","FAC-2026.0001","1234567890123456789","100000000.0000","IRR",MAIN));
        ok(n.consumerDomain().equals("LOAN"),"loan");
        ok(n.amount().equals("100000000"),"canonical money");
        ok(n.fileNumber().equals("1234567890123456789"),"19 digit file exact");
        ok(n.debtors().getFirst().partyId().equals("9000000000000000123"),"party exact");
        ok(n.debtors().getFirst().role().equals("PRIMARY"),"main role");
        ok(CollateralAllocationRequestPolicy.normalize(req("GUARANTEE","G-1","1","0.0001","USD",MAIN)).amount().equals("0.0001"),"fraction");
        ok(CollateralAllocationRequestPolicy.normalize(req("LETTER_OF_CREDIT","C","1","9999999999999999.9999","IRR",MAIN)).amount().equals("9999999999999999.9999"),"number20,4 max");
        ok(CollateralAllocationRequestPolicy.normalize(req("GREEN_LICENSE","GREEN","1","1","EUR",MAIN)).consumerDomain().equals("GREEN_LICENSE"),"green");
        fail(()->CollateralAllocationRequestPolicy.normalize(null),"CLT_ALLOCATION_REQUEST_REQUIRED");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("FAKE","C","1","1","IRR",MAIN)),"CLT_CAPACITY_CONSUMER_DOMAIN_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","","1","1","IRR",MAIN)),"CLT_ALLOCATION_CASE_REFERENCE_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN"," with-space","1","1","IRR",MAIN)),"CLT_ALLOCATION_CASE_REFERENCE_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","0","1","IRR",MAIN)),"CLT_ALLOCATION_PARTY_OR_FILE_ID_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","99999999999999999999","1","IRR",MAIN)),"CLT_ALLOCATION_PARTY_OR_FILE_ID_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","0","IRR",MAIN)),"CLT_CAPACITY_AMOUNT_MUST_BE_POSITIVE");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1.00001","IRR",MAIN)),"CLT_CAPACITY_DECIMAL_FORMAT_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","10000000000000000","IRR",MAIN)),"CLT_CAPACITY_DECIMAL_FORMAT_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","irr",MAIN)),"CLT_CAPACITY_CURRENCY_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","IRR",List.of())),"CLT_ALLOCATION_DEBTOR_ROSTER_SIZE_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","IRR",null)),"CLT_ALLOCATION_DEBTOR_ROSTER_SIZE_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","IRR",List.of(new Debtor("1","JOINT")))),"CLT_ALLOCATION_ONE_PRIMARY_REQUIRED");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","IRR",List.of(new Debtor("1","PRIMARY"),new Debtor("2","PRIMARY")))),"CLT_ALLOCATION_ONE_PRIMARY_REQUIRED");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","IRR",List.of(new Debtor("1","PRIMARY"),new Debtor("1","JOINT")))),"CLT_ALLOCATION_DEBTOR_DUPLICATE");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","IRR",List.of(new Debtor("1","SPONSOR")))),"CLT_ALLOCATION_DEBTOR_ROLE_INVALID");
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","IRR",List.of(new Debtor("0","PRIMARY")))),"CLT_ALLOCATION_PARTY_OR_FILE_ID_INVALID");
        List<Debtor> tooMany=new ArrayList<>();tooMany.add(new Debtor("1","PRIMARY"));
        for(int i=2;i<=22;i++)tooMany.add(new Debtor(String.valueOf(i),"JOINT"));
        fail(()->CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","IRR",tooMany)),"CLT_ALLOCATION_DEBTOR_ROSTER_SIZE_INVALID");
        String uuid="a1e42112-f546-49d0-8a8e-983566d9ad1f";
        ok(CollateralAllocationRequestPolicy.key(uuid).equals(uuid),"uuid");
        fail(()->CollateralAllocationRequestPolicy.key("123"),"CLT_ALLOCATION_IDEMPOTENCY_UUID_REQUIRED");
        fail(()->CollateralAllocationRequestPolicy.key(null),"CLT_ALLOCATION_IDEMPOTENCY_UUID_REQUIRED");
        fail(()->CollateralAllocationRequestPolicy.key(uuid.toUpperCase()),"CLT_ALLOCATION_IDEMPOTENCY_UUID_REQUIRED");
        var mixed=CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1.000","IRR",
                List.of(new Debtor("9","JOINT"),new Debtor("1","PRIMARY"))));
        ok(mixed.debtors().getFirst().partyId().equals("1"),"sorted");
        String hash=CollateralAllocationRequestPolicy.digest("UNIT1","svc-loan","12",mixed);
        ok(hash.length()==64,"sha256 length");
        var reordered=CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","1","IRR",
                List.of(new Debtor("1","PRIMARY"),new Debtor("9","JOINT"))));
        ok(hash.equals(CollateralAllocationRequestPolicy.digest("UNIT1","svc-loan","12",reordered)),"order independent");
        ok(!hash.equals(CollateralAllocationRequestPolicy.digest("UNIT2","svc-loan","12",reordered)),"branch in hash");
        ok(!hash.equals(CollateralAllocationRequestPolicy.digest("UNIT1","svc-credit","12",reordered)),"client in hash");
        ok(!hash.equals(CollateralAllocationRequestPolicy.digest("UNIT1","svc-loan","13",reordered)),"collateral in hash");
        ok(!hash.equals(CollateralAllocationRequestPolicy.digest("UNIT1","svc-loan","12",
                CollateralAllocationRequestPolicy.normalize(req("LOAN","C","1","2","IRR",reordered.debtors())))),"amount in hash");
        ok(CollateralAllocationRequestPolicy.conflict(reordered.debtors(),Set.of("9")),"joint debtor conflict");
        ok(CollateralAllocationRequestPolicy.conflict(reordered.debtors(),Set.of("1")),"primary debtor conflict");
        ok(!CollateralAllocationRequestPolicy.conflict(reordered.debtors(),Set.of("10")),"no debtor conflict");
        var blockers=CollateralAllocationRequestPolicy.blockers(false,false,"IRR","IRR","0");
        ok(blockers.contains("CLT_ALLOCATION_CONSUMER_CLIENT_NOT_VERIFIED"),"analyst not verified");
        ok(blockers.contains("G-02_DURABLE_IDEMPOTENCY_REQUEST_REGISTRY_MISSING"),"no durable idempotency");
        ok(blockers.contains("CLT_ALLOCATION_SERIALIZABLE_CAPACITY_LOCK_NOT_IMPLEMENTED"),"no concurrency lock");
        ok(blockers.contains("G-09_CONSUMER_OBLIGATION_AND_COMPLETE_DEBTOR_ROSTER_NOT_ATTESTED"),"roster not attested");
        ok(!blockers.contains("CLT_ALLOCATION_ARITHMETIC_SHORTFALL"),"no raw shortfall");
        ok(CollateralAllocationRequestPolicy.blockers(true,true,"IRR","IRR","10")
            .contains("CLT_ALLOCATION_DEBTOR_IS_COLLATERAL_SPONSOR"),"sponsor conflict");
        ok(CollateralAllocationRequestPolicy.blockers(true,false,"USD","IRR",null)
            .contains("CLT_ALLOCATION_FX_POLICY_NOT_APPROVED"),"fx unresolved");
        ok(CollateralAllocationRequestPolicy.blockers(true,false,"IRR","IRR","0.01")
            .contains("CLT_ALLOCATION_ARITHMETIC_SHORTFALL"),"raw shortfall");
        ok(!CollateralAllocationRequestPolicy.blockers(true,false,"IRR","IRR",null)
            .contains("CLT_ALLOCATION_CONSUMER_CLIENT_NOT_VERIFIED"),"bound service not analyst");
        System.out.println("CLT_R4B_ALLOCATION_CONTRACT_POLICY_PASS="+pass);
    }
}
