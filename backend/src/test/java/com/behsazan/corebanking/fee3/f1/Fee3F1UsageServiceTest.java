package com.behsazan.corebanking.fee3.f1;

import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3R3Service;
import com.behsazan.corebanking.fee3.r3.Fee3SubmissionPolicy;
import com.behsazan.corebanking.fee3.r4.Fee3R4CalculationContract;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Tests the fail-closed orchestration; mocks never connect to Oracle. */
class Fee3F1UsageServiceTest {
    private static final LocalDate DAY=LocalDate.of(2026,10,28);
    final Fee3Repository repo=mock(Fee3Repository.class);
    final Fee3R3Service preflight=mock(Fee3R3Service.class);
    final Fee3R4ReferenceRepository references=mock(Fee3R4ReferenceRepository.class);
    final Fee3F1UsageService service=new Fee3F1UsageService(repo,preflight,references);
    private static Fee3F1UsageContract.Request request(){
        return Fee3F1UsageContractTest.r("101","100","41250");
    }
    private static Fee3SubmissionPolicy.Report ready(){
        return new Fee3SubmissionPolicy.Report(true,false,List.of(),"MAKER_CHECKER_REQUIRED",false);
    }
    private static Fee3R4CalculationContract.VersionReference unsaved(){
        return new Fee3R4CalculationContract.VersionReference("UNPERSISTED_CLIENT_DRAFT",
            null,null,null,null,"UNSAVED",false,0);
    }
    @Test void readOnlySpringTransactionIsMandatory() throws Exception{
        Transactional anno=Fee3F1UsageService.class
            .getMethod("preview",Fee3F1UsageContract.Request.class).getAnnotation(Transactional.class);
        assertThat(anno).isNotNull();
        assertThat(anno.readOnly()).isTrue();
    }
    @Test void failedLocalGuardNeverReachesOraclePreflightOrReference(){
        when(repo.bankDate()).thenReturn(DAY);
        var req=Fee3F1UsageContractTest.request("10","0",null,null,null,"UNREVIEWED",null);
        var result=service.preview(req);
        assertThat(result.status()).isEqualTo("BLOCKED");
        verifyNoInteractions(preflight,references);
    }
    @Test void failedR3PreflightCannotProduceASimulation(){
        when(repo.bankDate()).thenReturn(DAY);
        when(preflight.preflight(any())).thenReturn(new Fee3SubmissionPolicy.Report(false,false,
            List.of(new Fee3SubmissionPolicy.Issue("SOURCE_UNVERIFIED","sourceId","review pending")),
            "R3_PREFLIGHT_REQUIRED",false));
        var result=service.preview(request());
        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.diagnostics().get(0)).isEqualTo("R3_PREFLIGHT_SOURCE_UNVERIFIED");
        verifyNoInteractions(references);
    }
    @Test void onlyVerifiedDraftSnapshotCanReachPureCalculation(){
        when(repo.bankDate()).thenReturn(DAY);
        when(preflight.preflight(any())).thenReturn(ready());
        when(references.resolve(any())).thenReturn(unsaved());
        var result=service.preview(request());
        assertThat(result.status()).isEqualTo("SIMULATED");
        assertThat(result.calculation().feeIrr()).isEqualByComparingTo("41250");
        assertThat(result.financialPosting()).isFalse();
        assertThat(result.persisted()).isFalse();
        assertThat(result.approved()).isFalse();
        verify(references,times(1)).resolve(any());
    }
    @Test void snapshotMismatchStopsTheRequest(){
        when(repo.bankDate()).thenReturn(DAY);
        when(preflight.preflight(any())).thenReturn(ready());
        when(references.resolve(any())).thenThrow(new IllegalArgumentException("STALE_ORACLE_DRAFT_REFERENCE"));
        var result=service.preview(request());
        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.diagnostics()).containsExactly("STALE_ORACLE_DRAFT_REFERENCE");
    }
    @Test void activeTariffCannotBeTreatedAsDraftSimulation(){
        when(repo.bankDate()).thenReturn(DAY);
        when(preflight.preflight(any())).thenReturn(ready());
        when(references.resolve(any())).thenReturn(new Fee3R4CalculationContract.VersionReference(
            "ACTIVE",1L,1L,1L,1L,"ACTIVE",true,1));
        var result=service.preview(request());
        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.financialPosting()).isFalse();
    }
}
