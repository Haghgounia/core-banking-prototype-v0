package com.behsazan.corebanking.collateral.reference;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CltInsuranceWebsitePolicyTest {
    @Test void acceptsCurrentInsurerWebsites() {
        assertEquals("https://www.iraninsurance.ir", CltInsuranceWebsitePolicy.normalized(" https://www.iraninsurance.ir "));
        assertEquals("https://melat.ir", CltInsuranceWebsitePolicy.normalized("https://melat.ir"));
        assertEquals("http://www.example.com/path?q=1", CltInsuranceWebsitePolicy.normalized("http://www.example.com/path?q=1"));
        assertNull(CltInsuranceWebsitePolicy.normalized(null));
    }
    @Test void rejectsUnsafeOrInvalidInput() {
        for (Object bad : new Object[] {"", "https://localhost", "https://example.com/invalid path",
                "javascript:alert(1)", "ftp://example.com", "http://admin:secret@example.com",
                "https://example.com\n", "https://example.com:0", "https://", 123,
                "https://" + "a".repeat(501) + ".com"}) {
            assertThrows(IllegalArgumentException.class, () -> CltInsuranceWebsitePolicy.normalized(bad), String.valueOf(bad));
        }
    }
}
