package com.behsazan.corebanking.collateral.allocation;

import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Spring/JUnit regression; Oracle lock competition still requires an independent E2E test. */
class CollateralAllocationCapacityLockPolicyTest {
  private CollateralAllocationCapacityLockPolicy.Snapshot sample() {
    var input=new CollateralCapacityPolicy.Input("ACTIVE",true,true,false,false,false,
       "IRR","90","FINAL","IRR","2026-01-01","2026-12-31","2026-11-03",
       "2026-01-01",null,new BigDecimal("100"),List.of());
    return new CollateralAllocationCapacityLockPolicy.Snapshot(input,"1",2,2,1,0);
  }
  @Test void evenPositiveLockedEvidenceNeverAuthorizesFinancialWrite() {
    var result=CollateralAllocationCapacityLockPolicy.inspect(sample(),"RECORDED_NOT_ALLOCATED","10","IRR",2);
    assertEquals("0",result.hypotheticalShortfall());
    assertFalse(result.allocationAuthorized());
    assertFalse(result.capacityReservationPersisted());
    assertTrue(result.blockers().contains("CLT_R4D_EXTERNAL_ALLOCATION_WRITERS_NOT_CERTIFIED_TO_LOCK_PARENT"));
  }
  @Test void tamperedRosterAndChangedRequestStatusAreRejected() {
    var tampered=new CollateralAllocationCapacityLockPolicy.Snapshot(sample().capacity(),"1",2,1,1,0);
    assertFalse(CollateralAllocationCapacityLockPolicy.inspect(tampered,
            "RECORDED_NOT_ALLOCATED","10","IRR",2).debtorRosterWellFormed());
    assertTrue(CollateralAllocationCapacityLockPolicy.inspect(sample(),"ALLOCATED","10","IRR",2)
            .blockers().contains("CLT_R4D_REQUEST_NOT_IMMUTABLE_INTAKE"));
  }
  @Test void rejectsUnsignedMoneyAndDoesNotGuessFx() {
    assertThrows(IllegalArgumentException.class,()->CollateralAllocationCapacityLockPolicy.inspect(
          sample(),"RECORDED_NOT_ALLOCATED","-1","IRR",2));
    assertNull(CollateralAllocationCapacityLockPolicy.inspect(sample(),"RECORDED_NOT_ALLOCATED",
          "10","USD",2).hypotheticalShortfall());
  }
}
