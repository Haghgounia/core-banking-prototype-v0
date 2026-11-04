import com.behsazan.corebanking.collateral.allocation.CollateralAllocationCapacityLockPolicy;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.AllocationBucket;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/** Pure rules + in-memory RACE SIMULATION, NOT an Oracle/JWT/lock contention test. */
public class AllocationCapacityLockPolicySmoke {
  private static int count;
  private static void check(boolean expression,String label) {if(!expression)throw new AssertionError(label);count++;}
  private static void reject(Runnable action,String label) {
    boolean rejected=false;
    try {action.run();}catch(IllegalArgumentException ignored){rejected=true;}
    check(rejected,label);
  }
  private static CollateralAllocationCapacityLockPolicy.Snapshot snapshot(String eligible,
          String allocated,String currency,String valuationStatus,String valuationEnd,
          int total,int unique,int primary,int conflicts) {
    var buckets=allocated==null?List.<AllocationBucket>of():List.of(
        new AllocationBucket("1",currency,1,allocated));
    var input=new CollateralCapacityPolicy.Input("ACTIVE",true,true,false,false,false,
        "IRR","99",valuationStatus,"IRR","2026-01-01",valuationEnd,
        "2026-11-03","2026-01-01",null,new BigDecimal(eligible),buckets);
    return new CollateralAllocationCapacityLockPolicy.Snapshot(input,"7",total,unique,primary,conflicts);
  }
  private static CollateralAllocationCapacityLockPolicy.Decision run(
          CollateralAllocationCapacityLockPolicy.Snapshot snapshot,String amount) {
    return CollateralAllocationCapacityLockPolicy.inspect(snapshot,
        "RECORDED_NOT_ALLOCATED",amount,"IRR",2);
  }
  public static void main(String[] args)throws Exception {
    var good=snapshot("100000000","60000000","IRR","FINAL","2027-01-01",2,2,1,0);
    var valid=run(good,"30000000");
    check("60000000".equals(valid.allocatedSameCurrency()),"current exposure 60m");
    check("40000000".equals(valid.arithmeticResidual()),"arith residual 40m");
    check("0".equals(valid.hypotheticalShortfall()),"within raw balance");
    check(valid.valuationPeriodProven(),"final valuation within range");
    check(valid.debtorRosterWellFormed(),"exact debtor roster");
    check(!valid.allocationAuthorized(),"NEVER authorized even positive margin");
    check(!valid.capacityReservationPersisted(),"NEVER reservation");
    check(valid.blockers().contains("CLT_R4D_EXTERNAL_ALLOCATION_WRITERS_NOT_CERTIFIED_TO_LOCK_PARENT"),
        "external writers unknown");
    check(valid.blockers().contains("G-09_CONSUMER_OBLIGATION_AND_DEBTOR_ROSTER_NOT_ATTESTED"),
        "external consuming obligation not proven");
    var over=run(good,"50000000");
    check("10000000".equals(over.hypotheticalShortfall()),"what-if shortage 10m");
    check(over.blockers().contains("CLT_R4D_HYPOTHETICAL_ARITHMETIC_SHORTFALL"),"shortage blocker");
    var reduced=run(snapshot("50000000","60000000","IRR","FINAL","2027-01-01",2,2,1,0),"1000000");
    check("-10000000".equals(reduced.arithmeticResidual()),"existing overuse preserved");
    check("11000000".equals(reduced.hypotheticalShortfall()),"existing shortfall + new demand");
    check(!reduced.allocationAuthorized(),"historical overuse does not cancel or approve");
    var mixed=run(snapshot("100000000","60000000","USD","FINAL","2027-01-01",2,2,1,0),"1000");
    check(mixed.arithmeticResidual()==null,"FX prevents residual");
    check(mixed.hypotheticalShortfall()==null,"FX prevents what-if");
    check(mixed.blockers().contains("CLT_CAPACITY_ACTIVE_ALLOCATION_FX_UNRESOLVED"),"FX blocker");
    var expired=run(snapshot("100000000",null,"IRR","FINAL","2026-01-01",2,2,1,0),"1");
    check(!expired.valuationPeriodProven(),"expired valuation denied");
    var draft=run(snapshot("100000000",null,"IRR","DRAFT","2027-01-01",2,2,1,0),"1");
    check(!draft.valuationPeriodProven(),"draft not final");
    var other=run(snapshot("100000000",null,"IRR","APPROVED","2027-01-01",2,2,1,0),"1");
    check(!other.valuationPeriodProven(),"UI approved not Oracle final");
    var conflict=run(snapshot("100000000",null,"IRR","FINAL","2027-01-01",2,2,1,2),"1");
    check(conflict.blockers().contains("CLT_R4D_CURRENT_SPONSOR_DEBTOR_CONFLICT"),"sponsor conflict");
    var missing=run(snapshot("100000000",null,"IRR","FINAL","2027-01-01",1,1,1,0),"1");
    check(!missing.debtorRosterWellFormed(),"missing debtor");
    var duplicate=run(snapshot("100000000",null,"IRR","FINAL","2027-01-01",2,1,1,0),"1");
    check(!duplicate.debtorRosterWellFormed(),"duplicate debtor");
    var twoPrimary=run(snapshot("100000000",null,"IRR","FINAL","2027-01-01",2,2,2,0),"1");
    check(!twoPrimary.debtorRosterWellFormed(),"two primary");
    var nonePrimary=run(snapshot("100000000",null,"IRR","FINAL","2027-01-01",2,2,0,0),"1");
    check(!nonePrimary.debtorRosterWellFormed(),"no primary");
    var changed=CollateralAllocationCapacityLockPolicy.inspect(good,"ALLOCATED","1","IRR",2);
    check(changed.blockers().contains("CLT_R4D_REQUEST_NOT_IMMUTABLE_INTAKE"),"status drift");
    var wrongCurrency=CollateralAllocationCapacityLockPolicy.inspect(good,"RECORDED_NOT_ALLOCATED","1","USD",2);
    check(wrongCurrency.hypotheticalShortfall()==null,"request currency mismatch");
    check(wrongCurrency.blockers().contains("CLT_R4D_REQUEST_CURRENCY_MISMATCH_NO_APPROVED_FX"),"request FX blocker");
    var badVersion=new CollateralAllocationCapacityLockPolicy.Snapshot(good.capacity(),"0",2,2,1,0);
    check(run(badVersion,"1").blockers().contains("CLT_R4D_COLLATERAL_VERSION_INVALID"),"version check");
    var corrupted=new CollateralAllocationCapacityLockPolicy.Snapshot(good.capacity(),null,2,2,1,0);
    check(run(corrupted,"1").blockers().contains("CLT_R4D_COLLATERAL_VERSION_INVALID"),"missing version");
    reject(()->run(good,"0"),"no zero request");
    reject(()->run(good,"-1"),"no negative request");
    reject(()->run(good,"1.12345"),"no excess scale");
    reject(()->CollateralAllocationCapacityLockPolicy.inspect(good,"RECORDED_NOT_ALLOCATED","1","EUR0",2),"currency format");
    reject(()->CollateralAllocationCapacityLockPolicy.inspect(null,"RECORDED_NOT_ALLOCATED","1","IRR",2),"snapshot present");
    check(!CollateralAllocationCapacityLockPolicy.everAllocates(),"hard no-writer contract");
    check(CollateralAllocationCapacityLockPolicy.NON_NEGOTIABLE_BLOCKERS.size()>=6,"persistent blockers");
    check(CollateralAllocationCapacityLockPolicy.EVIDENCE_LEVEL.contains("NO_FINANCIAL_WRITE"),"evidence transparency");
    // Separate pure concurrency SIMULATION: proves serial accounting idea, not Oracle row locking.
    var gate=new ReentrantLock();var available=new AtomicInteger(100);var winners=new AtomicInteger(0);
    var start=new CountDownLatch(1);var finished=new CountDownLatch(2);
    Runnable applicant=()->{try{start.await();gate.lock();try{if(available.get()>=70){available.addAndGet(-70);winners.incrementAndGet();}}finally{gate.unlock();}}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{finished.countDown();}};
    new Thread(applicant).start();new Thread(applicant).start();start.countDown();finished.await();
    check(winners.get()==1,"SIMULATION: one winner not two");
    check(available.get()==30,"SIMULATION: no lost update");
    check(!valid.blockers().isEmpty(),"positive arithmetic does not remove blockers");
    check(CollateralAllocationCapacityLockPolicy.REQUEST_STATUS.equals("RECORDED_NOT_ALLOCATED"),"stage status constant");
    check("100000000".equals(run(snapshot("100000000",null,"IRR","FINAL","2027-01-01",2,2,1,0),"1").arithmeticResidual()),"empty allocation zero");
    check(!run(good,"1").capacityReservationPersisted(),"probe repeat cannot reserve");
    check(!run(good,"2").allocationAuthorized(),"different idempotent request still blocked");
    check(valid.blockers().contains("CLT_R4D_MODULE_ID_AND_FINAL_ATOMIC_WRITE_NOT_APPROVED"),"module mapping pending");
    check(valid.blockers().contains("G-10_OFFICIAL_ALLOCATION_FACTORS_AND_FX_NOT_APPROVED"),"factor approval pending");
    System.out.println("CLT_R4D_LOCK_POLICY_SMOKE_PASS="+count+"; ORACLE_CONCURRENCY_EXECUTION=NONE");
  }
}
