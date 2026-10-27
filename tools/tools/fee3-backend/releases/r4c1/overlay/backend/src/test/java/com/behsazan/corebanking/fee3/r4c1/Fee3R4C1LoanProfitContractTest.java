package com.behsazan.corebanking.fee3.r4c1;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Maven regression tests: Jalali year segmentation is not Gregorian ACT_ACT. */
class Fee3R4C1LoanProfitContractTest {
    @Test void yearFactorForCompleteNonLeapJalaliYearIsOne(){
        var slices=List.of(new Fee3R4C1LoanProfitContract.YearSlice(1405,
            LocalDate.parse("2026-03-21"),LocalDate.parse("2027-03-21"),365,365,"CAL2:TEST"));
        assertThat(Fee3R4C1LoanProfitContract.yearFactor(slices.get(0).fromInclusive(),
            slices.get(0).toExclusive(),slices)).isEqualByComparingTo("1");
    }
    @Test void leapJalaliYearUses366Denominator(){
        var slices=List.of(new Fee3R4C1LoanProfitContract.YearSlice(1403,
            LocalDate.parse("2024-03-20"),LocalDate.parse("2025-03-21"),366,366,"CAL2:TEST"));
        assertThat(Fee3R4C1LoanProfitContract.yearFactor(slices.get(0).fromInclusive(),
            slices.get(0).toExclusive(),slices)).isEqualByComparingTo("1");
    }
    @Test void brokenMappingFailsClosed(){
        var slices=List.of(new Fee3R4C1LoanProfitContract.YearSlice(1405,
            LocalDate.parse("2026-10-27"),LocalDate.parse("2026-11-07"),10,365,"CAL2:TEST"));
        boolean failed=false;
        try{Fee3R4C1LoanProfitContract.yearFactor(LocalDate.parse("2026-10-27"),
            LocalDate.parse("2026-11-07"),slices);}catch(IllegalArgumentException expected){failed=true;}
        assertThat(failed).isEqualTo(true);
    }
    @Test void untrustedClientYearFractionIsNeverAcceptedAsCalendarProof(){
        // Proof is the strictly controlled YearSlice list; empty or forged segments fail closed.
        boolean rejected=false;
        try{Fee3R4C1LoanProfitContract.yearFactor(LocalDate.parse("2026-10-27"),
            LocalDate.parse("2026-11-07"),List.of());}catch(IllegalArgumentException expected){rejected=true;}
        assertThat(rejected).isEqualTo(true);
    }
}
