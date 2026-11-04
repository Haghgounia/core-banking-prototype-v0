package com.behsazan.corebanking.collateral.unallocation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class CollateralUnallocationIntakePolicyTest {
    @Test void financiallyFailClosed(){assertFalse(CollateralUnallocationIntakePolicy.financialUnallocationAuthorized());}
    @Test void finalReleaseFailClosed(){assertFalse(CollateralUnallocationIntakePolicy.finalReleaseAuthorized());}
    @Test void boundedJournal(){assertTrue(CollateralUnallocationIntakePolicy.limit(50)==50);}
    @Test void journalOverflow(){assertThrows(IllegalArgumentException.class,()->CollateralUnallocationIntakePolicy.limit(51));}
    @Test void requiredActor(){assertThrows(IllegalArgumentException.class,()->CollateralUnallocationIntakePolicy.actor(""));}
    @Test void reasonRejectsWhitespace(){assertThrows(IllegalArgumentException.class,()->CollateralUnallocationIntakePolicy.reason("bad reason"));}
    @Test void blockersImmutable(){var b=List.of("CLT_R5A_FINANCIAL_UNALLOCATION_AND_AUDIT_WRITES_DISABLED");
        assertTrue(CollateralUnallocationIntakePolicy.parseBlockers(CollateralUnallocationIntakePolicy.serializeBlockers(b)).equals(b));}
    @Test void shortBlockersRejected(){assertThrows(IllegalArgumentException.class,
        ()->CollateralUnallocationIntakePolicy.serializeBlockers(List.of("G-09")));}
    @Test void digestsLengthPrefix(){assertNotEquals(CollateralUnallocationIntakePolicy.digest("a","bc"),
        CollateralUnallocationIntakePolicy.digest("ab","c"));}
    @Test void cancelRequiresVersion(){assertThrows(CollateralUnallocationIntakePolicy.Stale.class,
        ()->CollateralUnallocationIntakePolicy.canCancel(CollateralUnallocationIntakePolicy.RECORDED,"2","1"));}
    @Test void cancellationTerminal(){assertThrows(CollateralUnallocationIntakePolicy.Conflict.class,
        ()->CollateralUnallocationIntakePolicy.canCancel(CollateralUnallocationIntakePolicy.CANCELLED,"2","2"));}
    @Test void immutableReplay(){assertThrows(CollateralUnallocationIntakePolicy.Conflict.class,
        ()->CollateralUnallocationIntakePolicy.replay("0".repeat(64),"1".repeat(64)));}
}
