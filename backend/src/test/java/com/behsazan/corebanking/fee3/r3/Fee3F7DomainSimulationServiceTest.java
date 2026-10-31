package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r4b2.*;
import com.behsazan.corebanking.fee3.r4c1.*;
import com.behsazan.corebanking.fee3.r4c2.*;
import com.behsazan.corebanking.fee3.r4c3.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** F7 contract wiring tests, deliberately without Oracle write, HTTP or bank authorization. */
class Fee3F7DomainSimulationServiceTest {
    Fee3R4C1Service loans=mock(Fee3R4C1Service.class);
    Fee3R4C2Service models=mock(Fee3R4C2Service.class);
    Fee3R4C3Service qard=mock(Fee3R4C3Service.class);
    Fee3R4B2Service guarantees=mock(Fee3R4B2Service.class);
    Fee3F7LateDamageService lateDamage=mock(Fee3F7LateDamageService.class);
    Fee3F7DomainSimulationService service=new Fee3F7DomainSimulationService(loans,models,qard,guarantees,lateDamage);

    private static DraftRequest draft(String module,String operation,String kind,String model) {
        return new DraftRequest(null,null,null,null,
            new ItemDraft("Trial",module,operation,kind,null,module,null),
            new VersionDraft(LocalDate.of(2026,10,1),null,"BANK_INTERNAL",null,"PERCENT", "NONE",
                "HALF_UP",null,Map.of(),"IRR","IRR",model==null?Map.of():Map.of("loanModelProfile",model)),
            new ScopeDraft(List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of("ALL"),List.of(),"IRR"),
            List.of(),List.of());
    }
    private Fee3F7DomainSimulationService.Request request(DraftRequest d) {
        return new Fee3F7DomainSimulationService.Request(d,Map.of("amount","1000000"),null);
    }
    @Test void unsupportedLcCannotUseGeneralMethod() {
        var result=service.preview(request(draft("LC","LC_OPEN","LC_FEE",null)));
        assertEquals("BLOCKED",result.status());
        assertNull(result.amountIrr());
        assertFalse(result.financialPosting());
        verifyNoInteractions(loans,models,qard,guarantees,lateDamage);
    }
    @Test void guaranteePassesThroughR4AndKeepsExactDecimalStrings() {
        var calc=new Fee3PreviewEngine.PreviewResult("SIMULATED",false,false,"PERCENT",
            new BigDecimal("15000.25"),new BigDecimal("15000"),new BigDecimal("0"),
            new BigDecimal("15000"),List.of(Map.of("basis",new BigDecimal("1000000"))),List.of());
        var evidence=new Fee3R4B2GuaranteeContract.Evidence("F7-R4", "SIMULATED",false,false,false,false,
            "UAT_DRAFT_SIMULATION",null,LocalDate.of(2026,10,29),LocalDate.of(2026,10,29),
            "IRR","HALF_UP",0,"example-fingerprint",calc,List.of("UNPUBLISHED_DRAFT_ONLY"));
        when(guarantees.preview(any())).thenReturn(evidence);
        var result=service.preview(request(draft("GUARANTEE","GUARANTEE_ISSUE","GUARANTEE_FEE",null)));
        assertEquals("SIMULATED",result.status());
        assertEquals("15000.25",result.rawIrr());
        assertEquals("15000",result.amountIrr());
        assertEquals("1000000",result.breakdown().get(0).get("basis"));
        assertFalse(result.approved());assertFalse(result.published());
        verify(guarantees,times(1)).preview(any());
        verifyNoInteractions(loans,models,qard,lateDamage);
    }
    @Test void deniedBankContractNeverManufacturesAmount() {
        var evidence=Fee3R4B2GuaranteeContract.blocked(null,LocalDate.of(2026,10,29),
            "ORACLE_DRAFT_NOT_FOUND");
        when(guarantees.preview(any())).thenReturn(evidence);
        var result=service.preview(request(draft("GUARANTEE","GUARANTEE_ISSUE","GUARANTEE_FEE",null)));
        assertEquals("BLOCKED",result.status());
        assertNull(result.amountIrr());
        assertTrue(result.diagnostics().contains("ORACLE_DRAFT_NOT_FOUND"));
    }
    @Test void participationUsesItsDedicatedR4ModelService() {
        var evidence=Fee3R4C2LoanModelContract.blocked(null,LocalDate.of(2026,10,29),"MODEL_UNVERIFIED");
        when(models.preview(any())).thenReturn(evidence);
        var result=service.preview(request(draft("LOAN","LOAN_PROFIT","FINANCING_PROFIT","PARTICIPATION_REALIZED")));
        assertEquals("BLOCKED",result.status());
        verify(models).preview(any());
        verifyNoInteractions(loans,qard,guarantees,lateDamage);
    }

    @Test void lateDamageDelegatesToCal2AwareServiceWithoutFallback() {
        var requested=request(draft("LOAN","LATE_DAMAGE","LATE_DAMAGE",null));
        var blocked=new Fee3F7DomainSimulationService.Result("BLOCKED","LOAN","LATE_DAMAGE",
            Fee3F7LateDamageContract.CONTRACT,null,null,null,null,List.of(),
            List.of("CAL2_BUSINESS_CALENDAR_COVERAGE_MISSING"),null,false,false,false,false);
        when(lateDamage.preview(same(requested))).thenReturn(blocked);
        var actual=service.preview(requested);
        assertSame(blocked,actual);
        verify(lateDamage,times(1)).preview(same(requested));
        verifyNoInteractions(loans,models,qard,guarantees);
    }
    @Test void generalF6MustRejectSpecializedLoan() {
        var result=Fee3F6SimulationService.simulate(new Fee3PreviewEngine.PreviewRequest(
            draft("LOAN","LOAN_PROFIT","FINANCING_PROFIT",null),Map.of("amount","1000000")));
        assertEquals("BLOCKED",result.status());
        assertTrue(result.diagnostics().contains("F7_SPECIALIZED_DOMAIN_REQUIRED"));
    }
}
