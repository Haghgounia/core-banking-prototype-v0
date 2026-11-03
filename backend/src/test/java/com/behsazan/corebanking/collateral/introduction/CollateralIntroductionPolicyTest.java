package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralIntroductionPolicyTest {
    private Request valid() {
        return new Request("123", "456", "120000000000.0000", "IRR",
                "2026-11-03", "2027-11-03", null, "valid draft",
                List.of(new AttributeInput("7", "A")));
    }
    @Test void normalizesWithoutFloatingPointLoss() {
        var n = CollateralIntroductionPolicy.normalize(valid());
        assertEquals("120000000000", n.estimateAmount());
        assertEquals("2026-11-03", n.validFrom());
        assertEquals("2027-11-03", n.validTo());
    }
    @Test void rejectsUnsafeNumericFields() {
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.id("12345678901234567890"));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.amount("1e8"));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.amount("0"));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.amount("100.00001"));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.amount("1,000"));
    }
    @Test void validatesDatesAndTypedAttributes() {
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.date("2026-02-30", false));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.validateValue("NUMBER", "1e50", null));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.validateValue("LIST", "X", "X,Y"));
        assertEquals("N", CollateralIntroductionPolicy.validateValue("BOOLEAN", "N", null));
    }
    @Test void stableFingerprintForSameAttributeSetRegardlessOfInputOrder() {
        var a = new Request("1","2","3.00","IRR","2026-11-03",null,null,null,
                List.of(new AttributeInput("9","B"),new AttributeInput("8","A")));
        var b = new Request("1","2","3","IRR","2026-11-03",null,null,null,
                List.of(new AttributeInput("8","A"),new AttributeInput("9","B")));
        assertEquals(CollateralIntroductionPolicy.sha256(CollateralIntroductionPolicy.normalize(a).canonical()),
                CollateralIntroductionPolicy.sha256(CollateralIntroductionPolicy.normalize(b).canonical()));
    }
    @Test void rejectsUnsafeCorrelationAndKey() {
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.key("short"));
        assertThrows(IllegalArgumentException.class, () -> CollateralIntroductionPolicy.correlation("not valid"));
        assertNotNull(CollateralIntroductionPolicy.correlation(null));
    }
}
