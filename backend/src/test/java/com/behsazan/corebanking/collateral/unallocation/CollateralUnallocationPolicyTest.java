package com.behsazan.corebanking.collateral.unallocation;

import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationModels.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CollateralUnallocationPolicyTest {
    private static AllocationLine line(boolean used){return new AllocationLine("10","99","2","567","100.0000","IRR",used,"4",null,null);}
    private static CollateralUnallocationPolicy.Normalized request(String amount,String currency,String mode,String etag){
        return CollateralUnallocationPolicy.normalize(new PreviewRequest("LOAN","567",amount,currency,mode,"REQ-001"),etag);
    }
    @Test void fullEvidenceNeverAuthorizes(){
        var blockers=CollateralUnallocationPolicy.blockers(line(true),request("100","IRR","FULL","\"4\""),"2",true);
        assertTrue(blockers.contains("G-09_CONSUMER_OBLIGATION_STATUS_NOT_ATTESTED"));
        assertFalse(CollateralUnallocationPolicy.everAuthorizesUnallocation());
    }
    @Test void noUsageRejects(){assertTrue(CollateralUnallocationPolicy.blockers(line(false),request("100","IRR","FULL","\"4\""),"2",true).contains("CLT_R5A_ALLOCATION_ALREADY_NOT_USED"));}
    @Test void missingMappingRejects(){assertTrue(CollateralUnallocationPolicy.blockers(line(true),request("100","IRR","FULL","\"4\""),null,true).contains("CLT_R5A_DOMAIN_MODULE_ID_NOT_APPROVED_OR_MISMATCH"));}
    @Test void staleEtagRejects(){assertTrue(CollateralUnallocationPolicy.blockers(line(true),request("100","IRR","FULL","\"5\""),"2",true).contains("CLT_R5A_ALLOCATION_VERSION_MISMATCH"));}
    @Test void partialRejects(){assertTrue(CollateralUnallocationPolicy.blockers(line(true),request("50","IRR","PARTIAL","\"4\""),"2",true).contains("CLT_R5A_PARTIAL_UNALLOCATION_POLICY_NOT_APPROVED"));}
    @Test void amountExcessRejects(){assertTrue(CollateralUnallocationPolicy.blockers(line(true),request("101","IRR","FULL","\"4\""),"2",true).contains("CLT_R5A_REQUEST_EXCEEDS_ALLOCATION"));}
    @Test void wrongCurrencyRejects(){assertTrue(CollateralUnallocationPolicy.blockers(line(true),request("100","USD","FULL","\"4\""),"2",true).contains("CLT_R5A_CURRENCY_MISMATCH_NO_FX_ASSUMPTION"));}
    @Test void badNumberPrecisionRejects(){assertThrows(IllegalArgumentException.class,()->request("1.12345","IRR","FULL","\"4\""));}
    @Test void noWeakEtag(){assertThrows(IllegalArgumentException.class,()->request("1","IRR","FULL","W/\"4\""));}
    @Test void digestBindsBranch(){var n=request("100","IRR","FULL","\"4\"");assertNotEquals(CollateralUnallocationPolicy.digest("99","10","001","c",n),CollateralUnallocationPolicy.digest("99","10","002","c",n));}
    @Test void noClientDenied(){assertTrue(CollateralUnallocationPolicy.blockers(line(true),request("100","IRR","FULL","\"4\""),"2",false).contains("CLT_R5A_CONSUMER_CLIENT_NOT_VERIFIED"));}
    @Test void boundedList(){assertThrows(IllegalArgumentException.class,()->CollateralUnallocationPolicy.limit(51));}
}
