package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.VersionDraft;
import com.behsazan.corebanking.fee3.domain.Fee3Models.DraftRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * F6: standalone, side-effect-free calculation for a draft in the analyst wizard.
 * No Oracle repository, workflow, approved tariff, charge or posting is involved.
 * Do NOT use this simulation as an authorization to collect money.
 */
public final class Fee3F6SimulationService {
    private Fee3F6SimulationService() {}

    /** Decimal amounts are serialized as plain strings to preserve monetary precision in JavaScript. */
    public record SimulationResult(String status, String method, String rawIrr, String feeIrr,
            String taxIrr, String totalIrr, List<Map<String,String>> breakdown,
            List<String> diagnostics, boolean persisted, boolean financialPosting) {}

    public static SimulationResult simulate(Fee3PreviewEngine.PreviewRequest request) {
        if (request == null || request.draft() == null || request.draft().version() == null)
            return blocked("UNKNOWN", "MISSING_DRAFT");
        DraftRequest draft = request.draft();
        VersionDraft version = draft.version();
        // F7 domain results must pass the R4-specific banking reference and calendar gates.
        String module=draft.item()==null?"":draft.item().module();
        String kind=draft.item()==null?"":draft.item().amountKind();
        if (List.of("LOAN","GUARANTEE","LC").contains(module) ||
                List.of("FINANCING_PROFIT","QARD_FEE","LATE_DAMAGE","GUARANTEE_FEE","LC_FEE","LEGAL_COST").contains(kind))
            return blocked(version.method(), "F7_SPECIALIZED_DOMAIN_REQUIRED");
        // A pre-tax analyst calculation is useful even before tax policy review;
        // it must NEVER manufacture tax, total payable or approval readiness.
        boolean pretaxOnly = "UNREVIEWED".equals(version.taxMode());
        if (pretaxOnly) {
            VersionDraft safe = new VersionDraft(version.validFrom(), version.validTo(),
                version.origin(), version.sourceId(), version.method(), "NONE", version.rounding(),
                version.changeReason(), version.financialPolicy(), version.serviceCurrency(),
                version.feeCurrency(), version.uiContext());
            draft = new DraftRequest(draft.calcItemId(), draft.tariffVersionId(),
                draft.expectedItemRecordVersion(), draft.expectedVersionRecordVersion(),
                draft.item(), safe, draft.scope(), draft.rules(), draft.tests());
        }
        Fee3PreviewEngine.PreviewResult result = Fee3PreviewEngine.calculate(
            new Fee3PreviewEngine.PreviewRequest(draft, request.inputs()));
        if (!"SIMULATED".equals(result.status()))
            return new SimulationResult("BLOCKED", result.method(), null, null, null, null,
                List.of(), result.diagnostics(), false, false);
        List<Map<String,String>> breakdown = new ArrayList<>();
        for (Map<String,Object> entry : result.breakdown()) {
            Map<String,String> converted = new LinkedHashMap<>();
            entry.forEach((key,value)->converted.put(key,
                value instanceof BigDecimal n ? n.toPlainString() : String.valueOf(value)));
            breakdown.add(Map.copyOf(converted));
        }
        List<String> diagnostics = new ArrayList<>(result.diagnostics());
        if (pretaxOnly)diagnostics.add("TAX_UNREVIEWED_PRETAX_ONLY_NO_PAYABLE_TOTAL");
        diagnostics.add("ANALYST_DRAFT_ONLY_NOT_BANK_TARIFF");
        return new SimulationResult(pretaxOnly ? "PRETAX_ONLY" : "SIMULATED", result.method(),
            decimal(result.rawIrr()), decimal(result.feeIrr()),
            pretaxOnly ? null : decimal(result.taxIrr()),
            pretaxOnly ? null : decimal(result.totalIrr()),
            List.copyOf(breakdown), List.copyOf(diagnostics), false, false);
    }

    private static SimulationResult blocked(String method,String diagnostic) {
        return new SimulationResult("BLOCKED",method,null,null,null,null,
            List.of(),List.of(diagnostic),false,false);
    }
    private static String decimal(BigDecimal value) {return value == null ? null : value.toPlainString();}
}
