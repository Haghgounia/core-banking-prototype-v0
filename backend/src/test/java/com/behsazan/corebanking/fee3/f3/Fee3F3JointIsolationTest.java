package com.behsazan.corebanking.fee3.f3;

import com.behsazan.corebanking.fee3.f1.*;
import com.behsazan.corebanking.fee3.f2.*;
import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import com.behsazan.corebanking.fee3.r3.Fee3R3Service;
import com.behsazan.corebanking.fee3.r4.Fee3R4ReferenceRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** F3 isolated integration: no SpringBootTest, database, posting or source mutation. */
class Fee3F3JointIsolationTest {
    static final LocalDate DAY = LocalDate.of(2026, 10, 28);
    static final String F1_ROUTE = "/api/v1/fee3/r4/services/usage/preview";
    static final String F2_ROUTE = "/api/v1/fee3/r4/services/formula-allowlist/preview";

    @Test void distinctPreviewRoutesHaveNoSharedMapping() throws Exception {
        var f1 = Fee3F1UsageController.class.getAnnotation(RequestMapping.class);
        var f2 = Fee3F2FormulaController.class.getAnnotation(RequestMapping.class);
        assertThat(f1.value()).containsExactly("/api/v1/fee3/r4/services/usage");
        assertThat(f2.value()).containsExactly("/api/v1/fee3/r4/services/formula-allowlist");
        assertThat(F1_ROUTE).isNotEqualTo(F2_ROUTE);
        assertThat(Fee3F1UsageController.class.getMethod("preview", Fee3F1UsageContract.Request.class)
            .getAnnotation(PostMapping.class).value()).containsExactly("/preview");
        assertThat(Fee3F2FormulaController.class.getMethod("preview", Fee3F2FormulaContract.Request.class)
            .getAnnotation(PostMapping.class).value()).containsExactly("/preview");
    }

    @Test void independentExceptionAdviceDoesNotCrossControllers() {
        var a1 = Fee3F1UsageProblemAdvice.class.getAnnotation(RestControllerAdvice.class);
        var a2 = Fee3F2FormulaProblemAdvice.class.getAnnotation(RestControllerAdvice.class);
        assertThat(a1.assignableTypes()).containsExactly(Fee3F1UsageController.class);
        assertThat(a2.assignableTypes()).containsExactly(Fee3F2FormulaController.class);
    }

    @Test void bothServicesRemainReadOnlyAndFailClosedOnSharedInfrastructure() throws Exception {
        var tx1 = Fee3F1UsageService.class
            .getMethod("preview", Fee3F1UsageContract.Request.class).getAnnotation(Transactional.class);
        var tx2 = Fee3F2FormulaService.class
            .getMethod("preview", Fee3F2FormulaContract.Request.class).getAnnotation(Transactional.class);
        assertThat(tx1).isNotNull();
        assertThat(tx2).isNotNull();
        assertThat(tx1.readOnly()).isTrue();
        assertThat(tx2.readOnly()).isTrue();

        var oracle = mock(Fee3Repository.class);
        var review = mock(Fee3R3Service.class);
        var refs = mock(Fee3R4ReferenceRepository.class);
        when(oracle.bankDate()).thenReturn(DAY);
        var usage = new Fee3F1UsageService(oracle, review, refs).preview(null);
        var formula = new Fee3F2FormulaService(oracle, review, refs).preview(null);
        assertThat(usage.status()).isEqualTo("BLOCKED");
        assertThat(formula.status()).isEqualTo("BLOCKED");
        assertThat(usage.financialPosting()).isFalse();
        assertThat(formula.financialPosting()).isFalse();
        assertThat(usage.approved()).isFalse();
        assertThat(formula.approved()).isFalse();
        verify(oracle, times(2)).bankDate();
        verifyNoInteractions(review, refs);
    }

    static MockMvc setup(Fee3F1UsageService f1, Fee3F2FormulaService f2) {
        return MockMvcBuilders.standaloneSetup(
            new Fee3F1UsageController(f1), new Fee3F2FormulaController(f2))
            .setControllerAdvice(new Fee3F1UsageProblemAdvice(), new Fee3F2FormulaProblemAdvice())
            .build();
    }

    @Test void combinedMvcDispatchesEachPreviewToItsOwnService() throws Exception {
        var f1 = mock(Fee3F1UsageService.class);
        var f2 = mock(Fee3F2FormulaService.class);
        when(f1.preview(any())).thenReturn(Fee3F1UsageContract.blocked(null, DAY, "F1_HOLD"));
        when(f2.preview(any())).thenReturn(Fee3F2FormulaContract.blocked(null, DAY, "F2_HOLD"));
        var mvc = setup(f1, f2);
        mvc.perform(post(F1_ROUTE).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BLOCKED"))
            .andExpect(jsonPath("$.diagnostics[0]").value("F1_HOLD"))
            .andExpect(jsonPath("$.financialPosting").value(false));
        verify(f1, times(1)).preview(any());
        verifyNoInteractions(f2);
        mvc.perform(post(F2_ROUTE).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BLOCKED"))
            .andExpect(jsonPath("$.diagnostics[0]").value("F2_HOLD"))
            .andExpect(jsonPath("$.financialPosting").value(false));
        verify(f2, times(1)).preview(any());
        mvc.perform(get(F1_ROUTE)).andExpect(status().isMethodNotAllowed());
        mvc.perform(get(F2_ROUTE)).andExpect(status().isMethodNotAllowed());
    }

    @Test void combinedMvcKeepsExceptionResponsesScoped() throws Exception {
        var f1 = mock(Fee3F1UsageService.class);
        var f2 = mock(Fee3F2FormulaService.class);
        when(f1.preview(any())).thenThrow(new IllegalArgumentException("F1_DENIED"));
        when(f2.preview(any())).thenThrow(new IllegalArgumentException("F2_DENIED"));
        var mvc = setup(f1, f2);
        mvc.perform(post(F1_ROUTE).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("FEE3_F1_INVALID_USAGE_PREVIEW"));
        mvc.perform(post(F2_ROUTE).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("FEE3_F2_INVALID_FORMULA_PREVIEW"));
    }
}
