package com.behsazan.corebanking.fee3.r4c2;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/** Real engine assertions (not test-only formula replicas). No Oracle/financial effects. */
class Fee3R4C2LoanModelContractTest {
    private static final LocalDate DAY=LocalDate.of(2026,10,27);
    private Fee3R4C2LoanModelContract.Request make(String profile,String type,String formula,
        String event,String basis,Map<String,Object> inputs){
        var item=new ItemDraft("UAT model","LOAN","LOAN_PROFIT","FINANCING_PROFIT",
            null,"LOAN",null);
        var version=new VersionDraft(DAY,null,"CONTRACT_RATE_REGULATED",null,"FORMULA",
            "OUT_OF_SCOPE","HALF_UP","UAT",Map.of(),"IRR","IRR",
            Map.of("loanModelProfile",profile,"loanCalculationEvent",event,
                "loanContractTypes",List.of(type),"loanPolicy",Map.of(
                    "dayCountBasis",basis,"holidayPolicy","CHARGE_ALL_DAYS",
                    "contractEvidenceRequired",true)));
        var rule=new RuleDraft(1,"FORMULA","REQUEST_AMOUNT","REQUEST",null,null,null,
            null,null,null,formula,Map.of());
        var draft=new DraftRequest(null,null,null,null,item,version,
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),
                List.of(type),"IRR"),List.of(rule),List.of(new TestDraft("UAT",Map.of("amount","1"),null)));
        return new Fee3R4C2LoanModelContract.Request(draft,inputs,null);
    }
    private static Map<String,Object> base(){
        var inputs=new LinkedHashMap<String,Object>();
        inputs.put("contractReference","UAT-LOAN-UNVERIFIED");
        inputs.put("calculationEvidenceReference","UAT-CALC-UNVERIFIED");
        inputs.put("eventDate",DAY.toString());
        return inputs;
    }
    private Fee3R4C2LoanModelContract.Request realized(){
        var inputs=base();inputs.put("realizedProfit","90000000.00");
        inputs.put("sharePercent","40.00000000");
        inputs.put("realizationStatementReference","UAT-REALIZATION-UNVERIFIED");
        inputs.put("shareAgreementReference","UAT-SHARE-UNVERIFIED");
        return make("PARTICIPATION_REALIZED","MUDARABA","PARTICIPATION_REALIZED",
            "REALIZED_PROFIT","CONTRACT_MONTHS",inputs);
    }
    private Fee3R4C2LoanModelContract.Request spread(boolean salam){
        var inputs=base();
        if(salam){
            inputs.put("saleAmount","105000000.00");inputs.put("purchaseAmount","90000000.00");
            inputs.put("saleEvidenceReference","UAT-SALE-UNVERIFIED");
            inputs.put("purchaseEvidenceReference","UAT-PURCHASE-UNVERIFIED");
            return make("SALAM_SPREAD","SALAM","SALAM_PRICE_SPREAD","SALAM_SETTLEMENT",
                "CONTRACT_MONTHS",inputs);
        }
        inputs.put("faceAmount","100000000.00");inputs.put("purchasePrice","92000000.00");
        inputs.put("faceAmountEvidenceReference","UAT-FACE-UNVERIFIED");
        inputs.put("purchasePriceEvidenceReference","UAT-PURCHASE-UNVERIFIED");
        return make("DEBT_PURCHASE_SPREAD","DEBT_PURCHASE","DEBT_PURCHASE_SPREAD",
            "PURCHASE_SPREAD","CONTRACT_MONTHS",inputs);
    }
    private Fee3R4C2LoanModelContract.Request estimated(String profile,String type){
        var inputs=base();inputs.remove("eventDate");
        inputs.put("amount","100000000.00");inputs.put("contractRate","18.00000000");
        inputs.put("rateEvidenceReference","UAT-RATE-UNVERIFIED");
        inputs.put("estimateBasisReference","UAT-ESTIMATE-UNVERIFIED");
        inputs.put("periodStart",DAY.toString());inputs.put("periodEndExclusive",DAY.plusDays(10).toString());
        return make(profile,type,"TPL_E624746F262F6E","ESTIMATED_PROFIT","JALALI_ACT_ACT",inputs);
    }
    private static List<Fee3R4C1LoanProfitContract.YearSlice> tenDays(){
        return List.of(new Fee3R4C1LoanProfitContract.YearSlice(1405,DAY,DAY.plusDays(10),
            10,365,"CAL2:PERSIAN:UAT"));
    }
    @Test void reportedRealizedShareNotAnAnnualRate(){
        var req=realized();
        assertThat(Fee3R4C2LoanModelContract.guard(req,DAY)).isEqualTo(null);
        var result=Fee3R4C2LoanModelContract.calculate(req,List.of());
        assertThat(result.roundedIrr()).isEqualByComparingTo("36000000");
        assertThat(result.resultKind()).isEqualTo("BANK_SHARE_OF_REPORTED_REALIZED_PROFIT_UNVERIFIED");
    }
    @Test void debtPurchaseSpreadIsGrossDifference(){
        var req=spread(false);
        assertThat(Fee3R4C2LoanModelContract.guard(req,DAY)).isEqualTo(null);
        assertThat(Fee3R4C2LoanModelContract.calculate(req,List.of()).roundedIrr())
            .isEqualByComparingTo("8000000");
    }
    @Test void salamSpreadIsNotAnnualRate(){
        var req=spread(true);
        assertThat(Fee3R4C2LoanModelContract.guard(req,DAY)).isEqualTo(null);
        assertThat(Fee3R4C2LoanModelContract.calculate(req,List.of()).roundedIrr())
            .isEqualByComparingTo("15000000");
    }
    @Test void participationEstimatedProfitUsesJalaliYearFromCalendar(){
        var req=estimated("PARTICIPATION_ESTIMATE","MUSHARAKA_MADANI");
        assertThat(Fee3R4C2LoanModelContract.guard(req,DAY)).isEqualTo(null);
        var calc=Fee3R4C2LoanModelContract.calculate(req,tenDays());
        assertThat(calc.roundedIrr()).isEqualByComparingTo("493151");
        assertThat(calc.resultKind()).isEqualTo("PARTICIPATION_PERIOD_ESTIMATE_NOT_REALIZED");
    }
    @Test void salamEstimateHasDistinctIdentity(){
        var req=estimated("SALAM_ESTIMATE","SALAM");
        assertThat(Fee3R4C2LoanModelContract.guard(req,DAY)).isEqualTo(null);
        var calc=Fee3R4C2LoanModelContract.calculate(req,tenDays());
        assertThat(calc.roundedIrr()).isEqualByComparingTo("493151");
        assertThat(calc.resultKind()).isEqualTo("SALAM_PERIOD_ESTIMATE_NOT_REALIZED");
    }
    @Test void negativeSpreadRequiresExplicitLossPolicy(){
        var old=spread(false);var inputs=new LinkedHashMap<>(old.inputs());
        inputs.put("purchasePrice","110000000.00");
        var req=new Fee3R4C2LoanModelContract.Request(old.draft(),inputs,null);
        assertThat(Fee3R4C2LoanModelContract.guard(req,DAY))
            .isEqualTo("NEGATIVE_SPREAD_NEEDS_LOSS_POLICY");
    }
    @Test void noClientSuppliedYearFraction(){
        var old=estimated("SALAM_ESTIMATE","SALAM");var inputs=new LinkedHashMap<>(old.inputs());
        inputs.put("yearFraction","1.0");
        var req=new Fee3R4C2LoanModelContract.Request(old.draft(),inputs,null);
        assertThat(Fee3R4C2LoanModelContract.guard(req,DAY)).isEqualTo("UNREVIEWED_INPUT_FIELDS");
    }
    @Test void unverifiedDayCoverageDoesNotProduceAnAmount(){
        boolean rejected=false;
        try {Fee3R4C2LoanModelContract.calculate(
            estimated("PARTICIPATION_ESTIMATE","MUSHARAKA_MADANI"),List.of());}
        catch(IllegalArgumentException ex){rejected=true;}
        assertThat(rejected).isEqualTo(true);
    }
}
