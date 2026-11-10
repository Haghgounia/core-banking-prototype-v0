package com.behsazan.corebanking.collateral.registration;

import com.behsazan.corebanking.collateral.registration.CollateralRegistrationReadbackModels.Check;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CollateralRegistrationReadbackPolicyTest {
    private static Map<String,Object> core() {
        Map<String,Object> x=new HashMap<>();
        x.put("COLLATERAL_ID","42");x.put("COLLATERAL_NUMBER","9876543210987654321");
        x.put("BRANCH_CODE","123");x.put("PARTY_ID","17");x.put("COLLATERAL_TYPE_ID","3");
        x.put("CURRENCY_CODE","IRR");x.put("ESTIMATE_AMOUNT","1000.00");
        x.put("VALID_FROM","2026-11-10");x.put("VALID_TO",null);x.put("DEPOSIT_NUMBER",null);
        return x;
    }
    private static Map<String,Object> ledger() {
        Map<String,Object> x=new HashMap<>();
        x.put("COLLATERAL_ID","42");x.put("COLLATERAL_NUMBER","9876543210987654321");
        x.put("BRANCH_CODE","123");x.put("REGISTRATION_STATUS","REGISTERED");
        x.put("DRAFT_ID","7");x.put("DRAFT_HASH","hash");return x;
    }
    private static Map<String,Object> draft() {
        Map<String,Object> x=new HashMap<>();
        x.put("DRAFT_ID","7");x.put("REQUEST_HASH","hash");
        x.put("PARTY_ID","17");x.put("COLLATERAL_TYPE_ID","3");
        x.put("CURRENCY_CODE","IRR");x.put("ESTIMATE_AMOUNT","1000");
        x.put("VALID_FROM","2026-11-10");x.put("VALID_TO",null);x.put("DEPOSIT_NUMBER",null);
        return x;
    }
    @Test void readbackSupports19DigitDecimalIdentifiersWithoutFloatingPoint() {
        assertThat(CollateralRegistrationReadbackPolicy.matchesCore(core(),ledger())).isTrue();
        assertThat(CollateralRegistrationReadbackPolicy.matchesDraft(core(),ledger(),draft())).isTrue();
    }
    @Test void branchMismatchNeverPasses() {
        var mismatch=ledger();mismatch.put("BRANCH_CODE","another-branch");
        assertThat(CollateralRegistrationReadbackPolicy.matchesCore(core(),mismatch)).isFalse();
    }
    @Test void changedOwnerOrDraftHashBlocksLineage() {
        var changed=draft();changed.put("PARTY_ID","18");
        assertThat(CollateralRegistrationReadbackPolicy.matchesDraft(core(),ledger(),changed)).isFalse();
        changed=draft();changed.put("REQUEST_HASH","changed");
        assertThat(CollateralRegistrationReadbackPolicy.matchesDraft(core(),ledger(),changed)).isFalse();
    }
    @Test void monetaryScaleDoesNotChangeOracleValue() {
        assertThat(CollateralRegistrationReadbackPolicy.equalMoney("1000.00","1000")).isTrue();
        assertThat(CollateralRegistrationReadbackPolicy.equalMoney("1000.01","1000")).isFalse();
        assertThat(CollateralRegistrationReadbackPolicy.equalMoney("invalid","1000")).isFalse();
    }
    @Test void schemaUnknownIsNotEquivalentToZeroRegistration() {
        assertThat(CollateralRegistrationReadbackPolicy.state("REVIEW",false,false,false,List.of()))
                .isEqualTo("LEDGER_SCHEMA_NOT_READY");
        assertThat(CollateralRegistrationReadbackPolicy.state("REVIEW",false,true,false,List.of()))
                .isEqualTo("NOT_REGISTERED");
    }
    @Test void existingLegacyCoreDoesNotClaimNewLedger() {
        assertThat(CollateralRegistrationReadbackPolicy.state("NUMBER",true,true,false,List.of()))
                .isEqualTo("HISTORICAL_CORE_WITHOUT_LEDGER");
    }
    @Test void missingCoreAndEvidenceCannotBePromoted() {
        assertThat(CollateralRegistrationReadbackPolicy.state("REVIEW",false,true,true,List.of()))
                .isEqualTo("CORE_READBACK_MISSING");
        assertThat(CollateralRegistrationReadbackPolicy.state("NUMBER",true,true,true,
                List.of(new Check("IDENTITY","NOT_VERIFIED",""))))
                .isEqualTo("READBACK_EVIDENCE_INCOMPLETE");
    }
    @Test void duplicateOrConflictingEvidenceRemainsAReadbackMismatch() {
        assertThat(CollateralRegistrationReadbackPolicy.state("NUMBER",true,true,true,
                List.of(new Check("BANK_NUMBER","MISMATCH",""))))
                .isEqualTo("READBACK_MISMATCH");
        assertThat(CollateralRegistrationReadbackPolicy.technical("READBACK_MISMATCH")).isFalse();
    }
    @Test void technicalReadbackRequiresEveryEvidenceCategory() {
        assertThat(CollateralRegistrationReadbackPolicy.state("NUMBER",true,true,true,List.of()))
                .isEqualTo("READBACK_EVIDENCE_INCOMPLETE");
        assertThat(CollateralRegistrationReadbackPolicy.state("NUMBER",true,true,true,
                List.of(new Check("CORE_BRANCH_ROW","VERIFIED",""))))
                .isEqualTo("READBACK_EVIDENCE_INCOMPLETE");
    }
    @Test void completeTechnicalReadbackIsNotBankApproval() {
        var checks=List.of(new Check("CORE_BRANCH_ROW","VERIFIED",""),
                new Check("REGISTRATION_LEDGER","VERIFIED",""),
                new Check("LEDGER_TO_CORE_BINDING","VERIFIED",""),
                new Check("INDEPENDENT_REVIEW_DECISION","VERIFIED",""),
                new Check("BANK_NUMBER_ISSUANCE_ROW","VERIFIED",""),
                new Check("VERIFIED_ASSET_CURRENT_LINK","VERIFIED",""),
                new Check("IMMUTABLE_DRAFT_TO_CORE","VERIFIED",""),
                new Check("PERSISTED_ATTRIBUTE_VALUES","OBSERVED",""),
                new Check("PERSISTED_SERIAL_RANGES","OBSERVED",""));
        assertThat(CollateralRegistrationReadbackPolicy.state("NUMBER",true,true,true,checks))
                .isEqualTo("TECHNICAL_LINEAGE_OBSERVED");
    }
}
