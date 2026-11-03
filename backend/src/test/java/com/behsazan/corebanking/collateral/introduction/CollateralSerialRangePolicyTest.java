package com.behsazan.corebanking.collateral.introduction;

import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollateralSerialRangePolicyTest {
    @Test void inclusiveQuantityAndTotalInIrr() {
        var a = CollateralSerialRangePolicy.assess(true, "IRR", "300", List.of(
                new SerialRangeInput("001", "003", "100"),
                new SerialRangeInput("5", "5", "200")));
        assertEquals(4, a.itemCount());
        assertEquals("300", a.totalAmountIRR());
        assertEquals("001", a.rows().getFirst().serialFrom());
    }
    @Test void refusesMoreThanTwentyItemsPerRow() {
        assertThrows(IllegalArgumentException.class, () ->
                CollateralSerialRangePolicy.normalize(List.of(new SerialRangeInput("1", "21", "100"))));
    }
    @Test void overlappingRangesIncludeEquivalentZeroPaddedSerials() {
        assertThrows(IllegalArgumentException.class, () ->
                CollateralSerialRangePolicy.normalize(List.of(
                        new SerialRangeInput("001", "005", "10"),
                        new SerialRangeInput("5", "10", "20"))));
    }
    @Test void supportsPersianAndArabicNumeralsExactly() {
        var rows = CollateralSerialRangePolicy.normalize(List.of(
                new SerialRangeInput("\u06f1\u06f2", "\u0661\u0664", "100")));
        assertEquals("12", rows.getFirst().serialFrom());
        assertEquals("14", rows.getFirst().serialTo());
    }
    @Test void rejectsWrongTypeAndCurrencyAndInconsistentAmount() {
        var input = List.of(new SerialRangeInput("1", "2", "100"));
        assertThrows(IllegalArgumentException.class, () -> CollateralSerialRangePolicy.assess(false, "IRR", "100", input));
        assertThrows(IllegalArgumentException.class, () -> CollateralSerialRangePolicy.assess(true, "USD", "100", input));
        assertThrows(IllegalArgumentException.class, () -> CollateralSerialRangePolicy.assess(true, "IRR", "101", input));
        assertThrows(IllegalArgumentException.class, () -> CollateralSerialRangePolicy.assess(true, "IRR", "100", List.of()));
    }
    @Test void canonicalHashIsStableWhenRangesArriveInDifferentOrder() {
        var a = new Request("1", "2", "300", "IRR", "2026-11-03", null, null, null, List.of(),
                "ASSET-CANDIDATE", List.of(new SerialRangeInput("5", "5", "200"), new SerialRangeInput("1", "3", "100")));
        var b = new Request("1", "2", "300", "IRR", "2026-11-03", null, null, null, List.of(),
                "ASSET-CANDIDATE", List.of(new SerialRangeInput("1", "3", "100"), new SerialRangeInput("5", "5", "200")));
        assertEquals(CollateralIntroductionPolicy.normalize(a).canonical(),
                CollateralIntroductionPolicy.normalize(b).canonical());
    }
}
