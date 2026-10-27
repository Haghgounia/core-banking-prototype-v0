package com.behsazan.corebanking.fee3.r4c3;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict pure arithmetic / fail-closed tests, no Oracle or financial writes. */
class Fee3R4C3QardContractTest {
    private static final LocalDate DAY=LocalDate.parse("2026-03-21");
    private static final LocalDate ORIGIN=LocalDate.parse("2025-03-21");
    private static final String PROOF="CAL2:PERSIAN:101:201";
    private Fee3R4C3QardContract.Request request(String model){
        var s=Fee3R4C3QardContract.SPECS.get(model);
        var input=new LinkedHashMap<String,Object>();
        input.put("contractReference","UAT-CONTRACT-A");
        input.put("contractEvidenceReference","UAT-CONTRACT-PROOF");
        input.put("rateEvidenceReference","UAT-RATE-PROOF");
        input.put("scheduleEvidenceReference","UAT-SCHEDULE-PROOF");
        input.put("loanContractStartDate",ORIGIN.toString());
        input.put("periodStart",DAY.toString());
        input.put("periodEndExclusive",DAY.plusDays(10).toString());
        input.put("eventDate",DAY.toString());
        input.put("amount","100000000.00");input.put("contractRate","4.00000000");
        if("FIRST_INSTALLMENT_OF_PERIOD".equals(model))input.put("firstInstallmentDate",DAY.toString());
        var draft=new DraftRequest(null,null,null,null,
            new ItemDraft("UAT qard fee","LOAN","QARD_FEE","QARD_FEE",null,"CREDIT",null),
            new VersionDraft(DAY,null,"BANK_INTERNAL",null,"FORMULA","OUT_OF_SCOPE",
                "HALF_UP","UAT",Map.of(),"IRR","IRR",
                Map.of("loanModelProfile",model,"loanContractTypes",List.of("QARD_HASANA"),
                    "loanPolicy",Map.of("contractEvidenceRequired",true,"contractInputsRequired",true,
                    "dayCountBasis","JALALI_ACT_ACT","holidayPolicy","CHARGE_ALL_DAYS",
                    "qardChargeTiming",s.qardChargeTiming(),"yearStartBasis",s.yearStartBasis()))),
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),
                List.of("QARD_HASANA"),"IRR"),
            List.of(new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","REQUEST",null,null,null,
                null,null,null,"TPL_E624746F262F6E",Map.of())),
            List.of(new TestDraft("UAT",Map.of("amount","100000000"),null)));
        return new Fee3R4C3QardContract.Request(draft,input,null);
    }
    private Fee3R4C3QardContract.Calculation calc(String model){
        return Fee3R4C3QardContract.calculate(request(model),
            List.of(new Fee3R4C1LoanProfitContract.YearSlice(1405,DAY,DAY.plusDays(10),
                10,365,PROOF)),
            new Fee3R4C3QardContract.EventDay(ORIGIN,1404,1,1,1,365,"CAL2:PERSIAN:101:200"),
            new Fee3R4C3QardContract.EventDay(DAY,1405,1,1,1,365,PROOF));
    }
    @Test void allFourTimingMethodsAreSeparated(){
        for(var model:Fee3R4C3QardContract.SPECS.keySet()){
            var req=request(model);
            assertThat(Fee3R4C3QardContract.guard(req,DAY)).isEqualTo(null);
            assertThat(calc(model).roundedIrr()).isEqualByComparingTo("109589");
            assertThat(calc(model).duplicateCheckStatus())
                .isEqualTo("NOT_VERIFIED_NO_AUTHORITATIVE_CHARGE_HISTORY");
        }
    }
    @Test void assessmentKeyDoesNotCertifyNoPreviousCharge(){
        var a=calc("ON_ANNIVERSARY");var b=calc("ON_ANNIVERSARY");
        assertThat(a.candidateAssessmentKey()).isEqualTo(b.candidateAssessmentKey());
        assertThat(a.diagnostics().contains("NO_DUPLICATE_CERTIFICATION")).isEqualTo(true);
    }
    @Test void cannotInjectYearFraction(){
        var old=request("ON_ANNIVERSARY");var in=new LinkedHashMap<>(old.inputs());
        in.put("yearFraction","1.0");
        assertThat(Fee3R4C3QardContract.guard(
            new Fee3R4C3QardContract.Request(old.draft(),in,null),DAY))
            .isEqualTo("QARD_UNREVIEWED_INPUT_FIELDS");
    }
    @Test void badAmountMustNotBeRelaidAsACharge(){
        var old=request("FIRST_INSTALLMENT_OF_PERIOD");var in=new LinkedHashMap<>(old.inputs());
        in.put("amount","123.001");
        assertThat(Fee3R4C3QardContract.guard(
            new Fee3R4C3QardContract.Request(old.draft(),in,null),DAY))
            .isEqualTo("QARD_AMOUNT_NUMBER22_2");
    }
    @Test void blockedNeverHasAmount(){
        var blocked=Fee3R4C3QardContract.blocked(request("JALALI_YEAR_START"),DAY,"UAT_BLOCKED");
        assertThat(blocked.status()).isEqualTo("BLOCKED");
        assertThat(blocked.calculation()).isEqualTo(null);
        assertThat(blocked.snapshotSha256()).isEqualTo(null);
        assertThat(blocked.financialPosting()).isEqualTo(false);
    }
}
