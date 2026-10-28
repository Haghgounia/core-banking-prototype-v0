package com.behsazan.corebanking.fee3.f2;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Isolated engineering mathematics only; no regulatory approval inferred. */
class Fee3F2FormulaContractTest {
    private static void fee(String code,int version,Map<String,Object> inputs,String expected){
        var r=Fee3F2Fixture.create(code,version,inputs);
        assertThat(Fee3F2FormulaContract.guard(r,Fee3F2Fixture.DAY)).isNull();
        assertThat(Fee3F2FormulaContract.calculate(r).feeIrr()).isEqualByComparingTo(expected);
        assertThat(Fee3F2FormulaContract.calculate(r).persisted()).isFalse();
    }
    private static void blocked(Fee3F2FormulaContract.Request r,String code){
        assertThat(Fee3F2FormulaContract.guard(r,Fee3F2Fixture.DAY)).isEqualTo(code);
        assertThat(Fee3F2FormulaContract.blocked(r,Fee3F2Fixture.DAY,code).financialPosting()).isFalse();
    }
    @Test void allowlistIsFixedToExactlyEightCodesAndElevenVersions(){
        assertThat(Fee3F2FormulaContract.knownVersions()).isEqualTo(11);
        assertThat(Fee3F2FormulaContract.uniqueFormulaCodes()).isEqualTo(8);
    }
    @Test void chequebookCostsMustBeExplicit(){
        fee("FCE-000051",1,Map.of("printingCost","10000","stampDuty","5000"),"90000");
        fee("FCE-000296",1,Map.of("printingCost","10000","stampDuty","5000"),"90000");
        blocked(Fee3F2Fixture.create("FCE-000051",1,Map.of("printingCost","10")),
            "FORMULA_INPUT_CONTRACT_MISMATCH");
    }
    @Test void cardStepsRespectExclusiveThresholdAndVersion(){
        fee("FCE-000157",1,Map.of("amount","10000000"),"9000");
        fee("FCE-000157",1,Map.of("amount","10000001"),"12200");
        fee("FCE-000157",1,Map.of("amount","20000000"),"12200");
        fee("FCE-000157",1,Map.of("amount","20000001"),"15400");
        fee("FCE-000157",2,Map.of("amount","10000000"),"11000");
        fee("FCE-000157",2,Map.of("amount","10000001"),"14500");
    }
    @Test void fileFeePlusPositiveItemCountIsDeterministic(){
        fee("FCE-000160",1,Map.of("usage","2"),"18000");
        fee("FCE-000164",1,Map.of("usage","3"),"5850");
        blocked(Fee3F2Fixture.create("FCE-000164",1,Map.of("usage","0")),
            "USAGE_MUST_BE_POSITIVE_INTEGER");
        blocked(Fee3F2Fixture.create("FCE-000164",1,Map.of("usage","1.1")),
            "USAGE_MUST_BE_POSITIVE_INTEGER");
    }
    @Test void valuationUsesCorrectThresholdForEachReference(){
        fee("FCE-000291",1,Map.of("amount","20000010000"),"2587520");
        fee("FCE-000292",1,Map.of("amount","2000001000"),"2587502");
        fee("FCE-000293",1,Map.of("amount","2000001000"),"2587502");
        fee("FCE-000294",1,Map.of("amount","2000001000"),"2587502");
        blocked(Fee3F2Fixture.create("FCE-000294",1,Map.of("amount","400000000001")),
            "CERTIFIED_EXPERT_FEE_REQUIRED_ABOVE_AUTO_LIMIT");
    }
    @Test void alteredValuationMetadataAndExpressionAreRejected(){
        var r=Fee3F2Fixture.create("FCE-000292",1,Map.of("amount","100"));
        @SuppressWarnings("unchecked") var existing=(Map<String,Object>)Fee3F2Fixture.config(r).get("valuationRule1404");
        Map<String,Object> modified=new LinkedHashMap<>(existing);modified.put("maximumAutoValue",999999999999L);
        Fee3F2Fixture.config(r).put("valuationRule1404",modified);
        blocked(r,"VALUATION_POLICY_METADATA_MISMATCH");
        var altered=Fee3F2Fixture.create("FCE-000160",1,Map.of("usage","3"));
        Fee3F2Fixture.config(altered).put("formula","some arbitrary expression");
        blocked(altered,"UNVERIFIED_FORMULA_EXPRESSION");
    }
    @Test void electronicChequebookAndRoundingAreDeterministic(){
        fee("FCE-000297",1,Map.of("approvedOperatingCost","1000","stampDuty","200"),"1400");
        fee("FCE-000297",1,Map.of("approvedOperatingCost","1.25","stampDuty","0"),"2");
    }
    @Test void taxRequiresExplicitDeclaredPolicy(){
        var r=Fee3F2Fixture.create("FCE-000160",1,Map.of("usage","2"));
        blocked(Fee3F2Fixture.change(r,Map.of(),"UNREVIEWED","HALF_UP",Fee3F2Fixture.DAY,null),
            "TAX_POLICY_UNREVIEWED");
        blocked(Fee3F2Fixture.change(r,Map.of("taxPercent","101"),"TAXABLE","HALF_UP",Fee3F2Fixture.DAY,null),
            "EXPLICIT_TAX_PERCENT_REQUIRED");
        var taxable=Fee3F2Fixture.change(r,Map.of("taxPercent","10"),"TAXABLE","HALF_UP",Fee3F2Fixture.DAY,null);
        assertThat(Fee3F2FormulaContract.calculate(taxable).taxIrr()).isEqualByComparingTo("1800");
    }
    @Test void wrongInputsCannotSilentlyUseFeeVariables(){
        blocked(Fee3F2Fixture.create("FCE-000160",1,Map.of("usage","1","amount","100")),
            "FORMULA_INPUT_CONTRACT_MISMATCH");
        blocked(Fee3F2Fixture.create("FCE-000157",2,Map.of("amount","-1")),
            "FORMULA_INPUT_NOT_VALID_IRR_OR_COUNT");
        blocked(Fee3F2Fixture.create("FCE-000157",2,Map.of("amount","0")),
            "TRANSFER_OR_VALUATION_AMOUNT_REQUIRED");
    }
    @Test void expirationAndHistoricalPreviewAreFailClosed(){
        var r=Fee3F2Fixture.create("FCE-000297",1,Map.of("approvedOperatingCost","50","stampDuty","10"));
        var expired=Fee3F2Fixture.change(r,Map.of(),"EXEMPT","HALF_UP",
            Fee3F2Fixture.DAY.minusDays(10),Fee3F2Fixture.DAY.minusDays(1));
        blocked(expired,"OUTSIDE_TARIFF_VALIDITY");
        blocked(new Fee3F2FormulaContract.Request(r.draft(),r.inputs(),Fee3F2Fixture.DAY.minusDays(1)),
            "HISTORICAL_PREVIEW_NOT_ALLOWED");
    }
    @Test void activeVersionCanNeverProduceSimulatedEvidence(){
        var r=Fee3F2Fixture.create("FCE-000157",2,Map.of("amount","11000000"));
        var ref=new com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract.VersionReference(
            "ACTIVE",1L,1L,1L,1L,"ACTIVE",true,1);
        assertThat(Fee3F2FormulaContract.finish(r,Fee3F2Fixture.DAY,ref,
            Fee3F2FormulaContract.calculate(r)).status()).isEqualTo("BLOCKED");
    }
    @Test void unsavedEngineeringProofIsNeverPostingAuthorization(){
        var r=Fee3F2Fixture.create("FCE-000160",1,Map.of("usage","2"));
        var ref=new com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract.VersionReference(
            "UNPERSISTED_CLIENT_DRAFT",null,null,null,null,"UNSAVED",false,0);
        var a=Fee3F2FormulaContract.finish(r,Fee3F2Fixture.DAY,ref,Fee3F2FormulaContract.calculate(r));
        var b=Fee3F2FormulaContract.finish(r,Fee3F2Fixture.DAY,ref,Fee3F2FormulaContract.calculate(r));
        assertThat(a.status()).isEqualTo("SIMULATED");
        assertThat(a.snapshotSha256()).isEqualTo(b.snapshotSha256());
        assertThat(a.financialPosting()).isFalse();
        assertThat(a.persisted()).isFalse();
        assertThat(a.approved()).isFalse();
    }
}
