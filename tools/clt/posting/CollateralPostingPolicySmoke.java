import com.behsazan.corebanking.collateral.posting.CollateralPostingPolicy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class CollateralPostingPolicySmoke {
    static int checks=0;
    static void pass(boolean ok){checks++;if(!ok)throw new AssertionError("CHECK_"+checks);}
    static void fails(String expected,Runnable action){
        checks++;
        try{action.run();throw new AssertionError("EXPECTED_"+expected);}
        catch(IllegalArgumentException ex){if(!expected.equals(ex.getMessage()))throw new AssertionError(ex);}
    }
    public static void main(String[] args){
        var p=CollateralPostingPolicy.class;
        pass("12".equals(CollateralPostingPolicy.id("12")));
        fails("CLT_POSTING_POSITIVE_NUMBER19_REQUIRED",()->CollateralPostingPolicy.id("0"));
        fails("CLT_POSTING_POSITIVE_NUMBER19_REQUIRED",()->CollateralPostingPolicy.id("001"));
        fails("CLT_POSTING_POSITIVE_NUMBER19_REQUIRED",()->CollateralPostingPolicy.id("12345678901234567890"));
        pass(CollateralPostingPolicy.key(UUID.randomUUID().toString()).length()==36);
        fails("CLT_POSTING_IDEMPOTENCY_UUID_REQUIRED",()->CollateralPostingPolicy.key("retry"));
        pass("CLT_SEQ_A1".equals(CollateralPostingPolicy.sequence("CLT_SEQ_A1")));
        fails("CLT_POSTING_APPROVED_SEQUENCE_MISSING",()->CollateralPostingPolicy.sequence("ABC;DROP"));
        fails("CLT_POSTING_BANK_REFERENCE_CODE_MISSING",()->CollateralPostingPolicy.referenceCode(""));
        pass(CollateralPostingPolicy.positiveMoney("2000000.0001").compareTo(new BigDecimal("2000000.0001"))==0);
        fails("CLT_POSTING_POSITIVE_MONEY_REQUIRED",()->CollateralPostingPolicy.positiveMoney("0"));
        fails("CLT_POSTING_NUMBER20_4_REQUIRED",()->CollateralPostingPolicy.positiveMoney("1.12345"));
        fails("CLT_POSTING_NUMBER20_4_REQUIRED",()->CollateralPostingPolicy.positiveMoney("1e4"));
        pass(CollateralPostingPolicy.allocationRemaining(new BigDecimal("100"),
                new BigDecimal("120"),new BigDecimal("70"),new BigDecimal("30"),true).signum()==0);
        fails("POSTING_APPROVED_CAPACITY_EXCEEDED",()->CollateralPostingPolicy.allocationRemaining(
                new BigDecimal("100"),new BigDecimal("120"),new BigDecimal("70"),new BigDecimal("31"),true));
        fails("POSTING_PERMIT_EXCEEDS_RECORDED_ELIGIBLE_VALUE",()->CollateralPostingPolicy.allocationRemaining(
                new BigDecimal("200"),new BigDecimal("120"),new BigDecimal("0"),new BigDecimal("1"),true));
        fails("POSTING_ACTIVE_ALLOCATION_FX_UNRESOLVED",()->CollateralPostingPolicy.allocationRemaining(
                new BigDecimal("100"),new BigDecimal("100"),new BigDecimal("0"),new BigDecimal("1"),false));
        pass(CollateralPostingPolicy.unallocationRemaining(new BigDecimal("45"),new BigDecimal("45"),"FULL").signum()==0);
        pass(CollateralPostingPolicy.unallocationRemaining(new BigDecimal("45"),new BigDecimal("5"),"PARTIAL").equals(new BigDecimal("40")));
        fails("POSTING_FULL_RELEASE_MUST_EQUAL_OUTSTANDING",()->CollateralPostingPolicy.unallocationRemaining(
                new BigDecimal("45"),new BigDecimal("4"),"FULL"));
        fails("POSTING_PARTIAL_RELEASE_MUST_BE_LESS_THAN_OUTSTANDING",()->CollateralPostingPolicy.unallocationRemaining(
                new BigDecimal("45"),new BigDecimal("45"),"PARTIAL"));
        var now=LocalDateTime.of(2026,11,10,12,0);
        var good=new CollateralPostingPolicy.Attestation("ALLOCATE","APPROVED","maker","checker","poster","issuer",
                now.plusDays(1),now,true,true,true,true,true,true,true,true,true,true);
        pass(CollateralPostingPolicy.blockers(good).isEmpty());
        var bad=new CollateralPostingPolicy.Attestation("UNALLOCATE","APPROVED","maker","maker","poster","issuer",
                now.minusSeconds(1),now,false,true,true,true,true,true,true,true,true,false);
        pass(CollateralPostingPolicy.blockers(bad).contains("POSTING_DUTIES_NOT_SEPARATED"));
        pass(CollateralPostingPolicy.blockers(bad).contains("POSTING_PERMIT_EXPIRED"));
        pass(CollateralPostingPolicy.blockers(bad).contains("POSTING_BANK_ATTESTATIONS_INCOMPLETE"));
        pass(CollateralPostingPolicy.blockers(bad).contains("POSTING_ORIGINAL_TRANSACTION_NOT_VERIFIED"));
        pass(CollateralPostingPolicy.sameKey("one","one"));
        pass(!CollateralPostingPolicy.sameKey("two","one"));
        System.out.println("CLT_BUC09_10_POSTING_POLICY_PASS="+checks+"/"+checks);
    }
}
