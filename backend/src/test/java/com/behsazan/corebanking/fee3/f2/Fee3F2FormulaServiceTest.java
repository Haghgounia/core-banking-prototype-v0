package com.behsazan.corebanking.fee3.f2;

import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3R3Service;
import com.behsazan.corebanking.fee3.r3.Fee3SubmissionPolicy;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** No Oracle connection in tests. */
class Fee3F2FormulaServiceTest {
    private static final LocalDate DAY=Fee3F2Fixture.DAY;
    Fee3Repository oracle=mock(Fee3Repository.class);
    Fee3R3Service r3=mock(Fee3R3Service.class);
    Fee3R4ReferenceRepository reference=mock(Fee3R4ReferenceRepository.class);
    Fee3F2FormulaService service=new Fee3F2FormulaService(oracle,r3,reference);
    private static Fee3F2FormulaContract.Request request(){
        return Fee3F2Fixture.create("FCE-000157",2,Map.of("amount","10000001"));
    }
    private static Fee3SubmissionPolicy.Report ready(){
        return new Fee3SubmissionPolicy.Report(true,false,List.of(),"MAKER_CHECKER_REQUIRED",false);
    }
    @Test void methodIsExplicitlySpringReadOnly()throws Exception{
        Transactional tx=Fee3F2FormulaService.class
            .getMethod("preview",Fee3F2FormulaContract.Request.class).getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();assertThat(tx.readOnly()).isTrue();
    }
    @Test void unknownFormulaIsRejectedBeforeR3(){
        when(oracle.bankDate()).thenReturn(DAY);
        var req=Fee3F2Fixture.create("FCE-000157",2,Map.of("amount","10000001"));
        Fee3F2Fixture.config(req).put("formula","untrusted expression");
        assertThat(service.preview(req).status()).isEqualTo("BLOCKED");
        verifyNoInteractions(r3,reference);
    }
    @Test void badOraclePreflightNeverReachesReference(){
        when(oracle.bankDate()).thenReturn(DAY);
        when(r3.preflight(any())).thenReturn(new Fee3SubmissionPolicy.Report(false,false,
            List.of(new Fee3SubmissionPolicy.Issue("TAX_UNREVIEWED","taxMode","unreviewed")),
            "R3_HOLD",false));
        var result=service.preview(request());
        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.diagnostics()).containsExactly("R3_PREFLIGHT_TAX_UNREVIEWED");
        verifyNoInteractions(reference);
    }
    @Test void oracleSnapshotMismatchBlocksCalculation(){
        when(oracle.bankDate()).thenReturn(DAY);
        when(r3.preflight(any())).thenReturn(ready());
        when(reference.resolve(any())).thenThrow(new IllegalArgumentException("STALE_ORACLE_DRAFT_REFERENCE"));
        assertThat(service.preview(request()).diagnostics()).containsExactly("STALE_ORACLE_DRAFT_REFERENCE");
    }
    @Test void activeReferenceNeverReturnsSimulation(){
        when(oracle.bankDate()).thenReturn(DAY);
        when(r3.preflight(any())).thenReturn(ready());
        when(reference.resolve(any())).thenReturn(new Fee3R4CalculationContract.VersionReference(
            "ACTIVE",1L,1L,1L,1L,"ACTIVE",true,1));
        assertThat(service.preview(request()).status()).isEqualTo("BLOCKED");
    }
    @Test void syntheticUatDraftReturnsSimulationOnly(){
        when(oracle.bankDate()).thenReturn(DAY);
        when(r3.preflight(any())).thenReturn(ready());
        when(reference.resolve(any())).thenReturn(new Fee3R4CalculationContract.VersionReference(
            "UNPERSISTED_CLIENT_DRAFT",null,null,null,null,"UNSAVED",false,0));
        var result=service.preview(request());
        assertThat(result.status()).isEqualTo("SIMULATED");
        assertThat(result.calculation().feeIrr()).isEqualByComparingTo("14500");
        assertThat(result.persisted()).isFalse();assertThat(result.financialPosting()).isFalse();
    }
}
