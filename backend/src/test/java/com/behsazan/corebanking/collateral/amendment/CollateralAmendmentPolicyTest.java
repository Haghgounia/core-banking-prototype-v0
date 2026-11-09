package com.behsazan.corebanking.collateral.amendment;

import static org.junit.jupiter.api.Assertions.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CollateralAmendmentPolicyTest {
    private Map<String,String> before() {
        var v = new LinkedHashMap<String,String>();
        v.put("VALID_FROM","2026-01-01"); v.put("ESTIMATE_AMOUNT","100");
        v.put("VALID_TO","2027-01-01"); v.put("DEPOSIT_NUMBER","123"); v.put("REMARKS","old");
        return v;
    }
    @Test void amountAndDate() {
        var r=CollateralAmendmentPolicy.normalize(before(),Map.of("ESTIMATE_AMOUNT","125.25","VALID_TO","2027-03-01"));
        assertEquals("125.25", r.get("ESTIMATE_AMOUNT"));
    }
    @Test void neverEditIdentity() {
        for (String immutable : new String[] {"PARTY_ID","COLLATERAL_TYPE_ID","VALID_FROM","COLLATERAL_NUMBER","BRANCH_CODE"})
            assertThrows(IllegalArgumentException.class, ()->CollateralAmendmentPolicy.normalize(before(),Map.of(immutable,"1")));
    }
    @Test void rejectsNoOpAndInvalidDate() {
        assertThrows(IllegalArgumentException.class, ()->CollateralAmendmentPolicy.normalize(before(),Map.of("REMARKS","old")));
        assertThrows(IllegalArgumentException.class, ()->CollateralAmendmentPolicy.normalize(before(),Map.of("VALID_TO","2025-01-01")));
    }
    @Test void nullMeansClearOnlyWhenExplicit() {
        var changes=new LinkedHashMap<String,String>(); changes.put("VALID_TO",null);
        assertNull(CollateralAmendmentPolicy.normalize(before(),changes).get("VALID_TO"));
    }
}
