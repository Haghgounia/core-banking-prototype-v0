package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.DraftRequest;
import com.behsazan.corebanking.fee3.r4b2.Fee3R4B2GuaranteeContract;
import com.behsazan.corebanking.fee3.r4b2.Fee3R4B2Service;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1Service;
import com.behsazan.corebanking.fee3.r4c2.Fee3R4C2LoanModelContract;
import com.behsazan.corebanking.fee3.r4c2.Fee3R4C2Service;
import com.behsazan.corebanking.fee3.r4c3.Fee3R4C3QardContract;
import com.behsazan.corebanking.fee3.r4c3.Fee3R4C3Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * F7: one analyst-facing, read-only adapter for the existing R4 domain contracts.
 * The individual R4 services remain responsible for all Oracle reference, calendar,
 * regulatory and effective-date gates. This adapter never runs local financial
 * formulas, bypasses a rejected gate or writes any data.
 */
@Service
public final class Fee3F7DomainSimulationService {
    private final Fee3R4C1Service loanProfit;
    private final Fee3R4C2Service loanModels;
    private final Fee3R4C3Service qard;
    private final Fee3R4B2Service guarantee;
    private final Fee3F7LateDamageService lateDamage;

    public Fee3F7DomainSimulationService(Fee3R4C1Service loanProfit, Fee3R4C2Service loanModels,
            Fee3R4C3Service qard, Fee3R4B2Service guarantee, Fee3F7LateDamageService lateDamage) {
        this.loanProfit=loanProfit;
        this.loanModels=loanModels;
        this.qard=qard;
        this.guarantee=guarantee;
        this.lateDamage=lateDamage;
    }

    public record Request(DraftRequest draft, Map<String,Object> inputs, LocalDate effectiveOn) {}
    public record Result(String status, String domain, String resultKind, String contractVersion,
            String rawIrr, String amountIrr, String taxIrr, String totalIrr,
            List<Map<String,String>> breakdown, List<String> diagnostics, String snapshotSha256,
            boolean persisted, boolean financialPosting, boolean approved, boolean published) {}

    public Result preview(Request r) {
        if(r==null||r.draft()==null||r.draft().item()==null||r.draft().version()==null||r.inputs()==null)
            return blocked("UNKNOWN","DRAFT_AND_INPUTS_REQUIRED");
        var i=r.draft().item();
        // Explicit evidence-gated domain routes from FCE V13.8.84. No implicit
        // fallback from LC, cheque or valuation to a bank-collectable result.
        if (Fee3F7DocumentedDomainPreview.supports(r.draft()))
            return Fee3F7DocumentedDomainPreview.simulate(r);
        if (Fee3F7LateDamageContract.supports(r.draft()))
            return lateDamage.preview(r);
        if (Fee3F7GuaranteeLifecyclePreview.supports(r.draft()))
            return Fee3F7GuaranteeLifecyclePreview.simulate(r);
        // No generic fallback for specialized banking amounts, even if the method is PERCENT.
        if("LOAN".equals(i.module()) && "QARD_FEE".equals(i.operation()) &&
                "QARD_FEE".equals(i.amountKind())) {
            var ev=qard.preview(new Fee3R4C3QardContract.Request(r.draft(),r.inputs(),r.effectiveOn()));
            if(ev==null||ev.persisted()||ev.financialPosting()||ev.approved()||ev.published())
                return blocked("QARD","UNSAFE_DOMAIN_EVIDENCE");
            var calc=ev.calculation();
            return result(ev.status(),"QARD","QARD_FEE",ev.contractVersion(),
                calc==null?null:calc.rawIrr(),calc==null?null:calc.roundedIrr(),
                null,null,calc==null?List.of():calc.breakdown(),ev.diagnostics(),ev.snapshotSha256());
        }
        if("LOAN".equals(i.module()) && "LOAN_PROFIT".equals(i.operation()) &&
                "FINANCING_PROFIT".equals(i.amountKind())) {
            Object profile=r.draft().version().uiContext()==null?null:
                r.draft().version().uiContext().get("loanModelProfile");
            if(profile instanceof String p && List.of("PARTICIPATION_REALIZED","PARTICIPATION_ESTIMATE",
                    "DEBT_PURCHASE_SPREAD","SALAM_SPREAD","SALAM_ESTIMATE").contains(p)) {
                var ev=loanModels.preview(new Fee3R4C2LoanModelContract.Request(r.draft(),r.inputs(),r.effectiveOn()));
            if(ev==null||ev.persisted()||ev.financialPosting()||ev.approved()||ev.published())
                return blocked("LOAN_MODEL","UNSAFE_DOMAIN_EVIDENCE");
                var calc=ev.calculation();
                return result(ev.status(),"LOAN",ev.resultKind(),ev.contractVersion(),
                    calc==null?null:calc.rawIrr(),calc==null?null:calc.roundedIrr(),
                    null,null,calc==null?List.of():calc.breakdown(),ev.diagnostics(),ev.snapshotSha256());
            }
            var ev=loanProfit.preview(new Fee3R4C1LoanProfitContract.Request(r.draft(),r.inputs(),r.effectiveOn()));
            if(ev==null||ev.persisted()||ev.financialPosting()||ev.approved()||ev.published())
                return blocked("LOAN","UNSAFE_DOMAIN_EVIDENCE");
            var calc=ev.calculation();
            return result(ev.status(),"LOAN","FINANCING_PROFIT",ev.contractVersion(),
                calc==null?null:calc.rawIrr(),calc==null?null:calc.feeIrr(),
                null,null,calc==null?List.of():calc.breakdown(),ev.diagnostics(),ev.snapshotSha256());
        }
        if("GUARANTEE".equals(i.module()) && "GUARANTEE_ISSUE".equals(i.operation()) &&
                "GUARANTEE_FEE".equals(i.amountKind())) {
            var ev=guarantee.preview(new Fee3R4B2GuaranteeContract.Request(r.draft(),r.inputs(),r.effectiveOn()));
            if(ev==null||ev.persisted()||ev.financialPosting()||ev.approved()||ev.published())
                return blocked("GUARANTEE","UNSAFE_DOMAIN_EVIDENCE");
            var calc=ev.calculation();
            return result(ev.status(),"GUARANTEE","GUARANTEE_FEE",ev.contractVersion(),
                calc==null?null:calc.rawIrr(),calc==null?null:calc.feeIrr(),
                calc==null?null:calc.taxIrr(),calc==null?null:calc.totalIrr(),
                calc==null?List.of():calc.breakdown(),ev.diagnostics(),ev.snapshotSha256());
        }
        return blocked(i.module(),"F7_DOMAIN_OPERATION_NOT_YET_CERTIFIED");
    }

    private static Result blocked(String domain,String reason) {
        return new Result("BLOCKED",domain,"UNSUPPORTED","",null,null,null,null,
            List.of(),List.of(reason),null,false,false,false,false);
    }
    private static String exact(BigDecimal v) { return v==null?null:v.toPlainString(); }
    private static Result result(String status,String domain,String kind,String contract,
            BigDecimal raw,BigDecimal amount,BigDecimal tax,BigDecimal total,
            List<Map<String,Object>> source,List<String> diagnostics,String sha) {
        if(!"SIMULATED".equals(status)||amount==null)
            return new Result("BLOCKED",domain,kind,contract,null,null,null,null,
                List.of(),List.copyOf(diagnostics),null,false,false,false,false);
        List<Map<String,String>> trace=new ArrayList<>();
        for(var entry:source){
            Map<String,String> row=new LinkedHashMap<>();
            entry.forEach((key,value)->row.put(key,
                value instanceof BigDecimal d?d.toPlainString():String.valueOf(value)));
            trace.add(Map.copyOf(row));
        }
        List<String> notes=new ArrayList<>(diagnostics);
        notes.add("F7_DOMAIN_UAT_PREVIEW_ONLY");
        notes.add("NOT_A_CHARGE_OR_POSTING_AUTHORIZATION");
        return new Result("SIMULATED",domain,kind,contract,exact(raw),exact(amount),
            exact(tax),exact(total),List.copyOf(trace),List.copyOf(notes),sha,
            false,false,false,false);
    }
}
