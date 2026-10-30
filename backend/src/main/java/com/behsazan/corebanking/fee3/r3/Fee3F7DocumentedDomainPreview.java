package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.DraftRequest;
import com.behsazan.corebanking.fee3.domain.Fee3Models.RuleDraft;
import com.behsazan.corebanking.fee3.domain.Fee3Models.VersionDraft;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * F7 documentary analyst preview for LC, cheque and collateral-valuation rules.
 * It extends F6 arithmetic to specifically scoped operations ONLY after the
 * mandatory documentary/contract evidence is present. Evidence references are
 * not authenticated here: every result is PRETAX_ONLY, unapproved and non-posting.
 * In particular the HTML 1404 valuation policy is never represented as an
 * effective bank tariff for 1405, regardless of what a user types in the UI.
 */
public final class Fee3F7DocumentedDomainPreview {
    private Fee3F7DocumentedDomainPreview() {}
    private static final BigDecimal MAX = new BigDecimal("1000000000000000000");
    private static final Set<String> LC_OPS = Set.of("LC_OPEN","LC_RENEW","LC_CHANGE",
            "LC_DOCUMENT_REVIEW","LC_DISCREPANCY","LC_SETTLEMENT","LC_CANCEL");
    private static final Set<String> CHEQUE_OPS = Set.of("CHEQUEBOOK_REQUEST",
            "CHEQUEBOOK_ISSUE_PAPER","CHEQUEBOOK_ISSUE_ELECTRONIC",
            "CHEQUEBOOK_DELIVERY_HOME","CHEQUEBOOK_DELIVERY_OTHER",
            "CHEQUE_LEAF_STOP","CHEQUE_LEAF_BLOCK_ADMIN");
    private static final Map<String,String> VALUATION_ROWS = Map.of(
            "COLLATERAL_VALUATION_REAL_ESTATE","7-2",
            "COLLATERAL_VALUATION_MACHINERY","7-1",
            "COLLATERAL_VALUATION_FARMLAND","7-3",
            "COLLATERAL_VALUATION_SURPLUS","7-7");
    private static final String CONTRACT="FEE3_F7_DOCUMENTED_DRAFT_V1";

    public static boolean supports(DraftRequest draft) {
        if(draft==null||draft.item()==null)return false;
        var i=draft.item();
        return ("LC".equals(i.module())&&"LC_FEE".equals(i.amountKind())&&LC_OPS.contains(i.operation()))
            ||("DEPOSIT".equals(i.module())&&"SERVICE_FEE".equals(i.amountKind())&&CHEQUE_OPS.contains(i.operation()))
            ||("GENERAL".equals(i.module())&&"SERVICE_FEE".equals(i.amountKind())&&VALUATION_ROWS.containsKey(i.operation()));
    }

    public static Fee3F7DomainSimulationService.Result simulate(Fee3F7DomainSimulationService.Request req) {
        String domain=req==null||req.draft()==null||req.draft().item()==null?"UNKNOWN":req.draft().item().module();
        try {return checked(req,domain);}
        catch(IllegalArgumentException|ArithmeticException error) {
            return blocked(domain,error.getMessage()==null?"DOMAIN_CONTRACT_INVALID":error.getMessage());
        }
    }

    private static Fee3F7DomainSimulationService.Result checked(Fee3F7DomainSimulationService.Request req,String domain) {
        if(req==null||!supports(req.draft())||req.inputs()==null)throw bad("F7_OPERATION_NOT_SUPPORTED");
        DraftRequest d=req.draft(); VersionDraft v=d.version(); var i=d.item();
        if(v==null||d.rules()==null||d.rules().size()!=1)throw bad("F7_SINGLE_RULE_REQUIRED");
        RuleDraft rule=d.rules().get(0);
        if(rule==null||v.method()==null||!v.method().equals(rule.method()))throw bad("F7_RULE_METHOD_MISMATCH");
        if(!"IRR".equals(v.feeCurrency())||!"IRR".equals(v.serviceCurrency()))throw bad("F7_IRR_ONLY");
        if(v.validFrom()==null)throw bad("F7_EFFECTIVE_START_REQUIRED");
        LocalDate on=req.effectiveOn();
        if(on==null)throw bad("F7_EXPLICIT_EFFECTIVE_DATE_REQUIRED");
        if(on.isBefore(v.validFrom())||(v.validTo()!=null&&on.isAfter(v.validTo())))
            throw bad("F7_VERSION_NOT_EFFECTIVE_ON_DATE");
        require(req.inputs(),"regulatoryReference");
        require(req.inputs(),"sourceEvidenceReference");
        if("LC".equals(domain)) {
            require(req.inputs(),"contractReference");
            if("REQUEST".equals(rule.rateSource()))require(req.inputs(),"rateEvidenceReference");
        }
        if("DEPOSIT".equals(domain)) {
            String op=i.operation();
            if("CHEQUEBOOK_ISSUE_PAPER".equals(op)) {
                require(req.inputs(),"invoiceReference");
                require(req.inputs(),"stampDutyReference");
            }
            if("CHEQUEBOOK_ISSUE_ELECTRONIC".equals(op)) {
                require(req.inputs(),"operatingCostReference");
                require(req.inputs(),"stampDutyReference");
            }
        }
        if("GENERAL".equals(domain))return valuation(req,domain);

        // Preserve all validated draft rules, but do not infer reviewed tax or
        // return a payable total from an unsaved tariff. The arithmetic is still
        // owned by the common Java engine rather than the browser.
        var safe=new VersionDraft(v.validFrom(),v.validTo(),v.origin(),v.sourceId(),
            v.method(),"NONE",v.rounding(),v.changeReason(),v.financialPolicy(),
            v.serviceCurrency(),v.feeCurrency(),v.uiContext());
        var noTax=new DraftRequest(d.calcItemId(),d.tariffVersionId(),
            d.expectedItemRecordVersion(),d.expectedVersionRecordVersion(),d.item(),safe,
            d.scope(),d.rules(),d.tests());
        var computed=Fee3PreviewEngine.calculate(new Fee3PreviewEngine.PreviewRequest(noTax,req.inputs()));
        if(!"SIMULATED".equals(computed.status())||computed.feeIrr()==null)
            return blocked(domain,computed.diagnostics().isEmpty()?"F7_CALCULATION_BLOCKED":computed.diagnostics().get(0));
        return result(domain,i.amountKind(),computed.rawIrr(),computed.feeIrr(),
            computed.breakdown(),req,"UNVERIFIED_DOCUMENT_REFERENCE_DRAFT_ONLY");
    }

    private static Fee3F7DomainSimulationService.Result valuation(Fee3F7DomainSimulationService.Request req,String domain) {
        var draft=req.draft();var rule=draft.rules().get(0);
        if(!"FORMULA".equals(draft.version().method()))throw bad("VALUATION_FORMULA_REQUIRED");
        require(req.inputs(),"expertReportReference");
        // This is the explicitly versioned 1404 reference from FCE V13.8.84.
        // It is NOT extrapolated to a 1405 effective date.
        Object sourceConfig=rule.parameters()==null?null:rule.parameters().get("originalConfig");
        if(!(sourceConfig instanceof Map<?,?> cfg)||!(cfg.get("valuationRule1404") instanceof Map<?,?> policy))
            throw bad("VALUATION_1404_POLICY_MISSING");
        if(!VALUATION_ROWS.get(draft.item().operation()).equals(String.valueOf(policy.get("officialRow"))))
            throw bad("VALUATION_OFFICIAL_ROW_MISMATCH");
        // 1404 (Solar Hijri) = 2025-03-21 .. 2026-03-20 inclusive.
        if(req.effectiveOn().isBefore(LocalDate.of(2025,3,21))||
                req.effectiveOn().isAfter(LocalDate.of(2026,3,20)))
            throw bad("VALUATION_1404_REFERENCE_NOT_VALID_FOR_SELECTED_DATE");
        BigDecimal base=decimal(policy.get("baseFee"),"BASE_FEE");
        BigDecimal threshold=decimal(policy.get("threshold"),"THRESHOLD");
        BigDecimal maximum=decimal(policy.get("maximumAutoValue"),"MAX_AUTO_VALUE");
        BigDecimal rate=decimal(policy.get("marginalRate"),"MARGINAL_RATE");
        BigDecimal amount=decimal(req.inputs().get("amount"),"VALUATION_AMOUNT");
        if(base.signum()<0||threshold.signum()<=0||maximum.compareTo(threshold)<=0||
            rate.signum()<0||rate.compareTo(BigDecimal.ONE)>=0||amount.signum()<=0)
            throw bad("VALUATION_POLICY_VALUES_INVALID");
        BigDecimal eligible=amount.min(maximum);
        BigDecimal incremental=eligible.subtract(threshold).max(BigDecimal.ZERO).multiply(rate);
        BigDecimal extra=BigDecimal.ZERO;
        if(amount.compareTo(maximum)>0) {
            require(req.inputs(),"excessExpertFeeReference");
            extra=decimal(req.inputs().get("officialExpertFeeForExcess"),"EXCESS_EXPERT_FEE");
        }
        BigDecimal raw=base.add(incremental).add(extra);
        if(rule.min()!=null)raw=raw.max(rule.min());
        if(rule.max()!=null)raw=raw.min(rule.max());
        if(raw.signum()<0||raw.compareTo(MAX)>0)throw bad("VALUATION_RESULT_OUT_OF_RANGE");
        RoundingMode rounding=switch(String.valueOf(draft.version().rounding())) {
            case "HALF_UP" -> RoundingMode.HALF_UP;
            case "HALF_EVEN" -> RoundingMode.HALF_EVEN;
            case "DOWN" -> RoundingMode.DOWN;
            case "UP" -> RoundingMode.UP;
            default -> throw bad("VALUATION_ROUNDING_UNSUPPORTED");
        };
        BigDecimal fee=raw.setScale(0,rounding);
        List<Map<String,Object>> trace=new ArrayList<>();
        trace.add(Map.of("kind","VALUATION_1404_REFERENCE","officialRow",policy.get("officialRow"),
            "assessedIrr",amount,"thresholdIrr",threshold,"automaticCeilingIrr",maximum));
        trace.add(Map.of("kind","VALUATION_COMPONENTS","baseIrr",base,
            "marginalIrr",incremental,"expertExcessIrr",extra));
        trace.add(Map.of("kind","ROUNDING","rawIrr",raw,"roundedIrr",fee));
        return result(domain,draft.item().amountKind(),raw,fee,trace,req,
            "HISTORICAL_1404_RULE_ONLY_NOT_CURRENT_BANK_TARIFF");
    }

    private static Fee3F7DomainSimulationService.Result result(String domain,String kind,
        BigDecimal raw,BigDecimal amount,List<Map<String,Object>> rows,
        Fee3F7DomainSimulationService.Request req,String note) {
        List<Map<String,String>> trace=new ArrayList<>();
        for(Map<String,Object> row:rows){
            Map<String,String> copy=new LinkedHashMap<>();
            row.forEach((key,value)->copy.put(key,value instanceof BigDecimal n?n.toPlainString():String.valueOf(value)));
            trace.add(Map.copyOf(copy));
        }
        String signature;
        try {
            String material=req.draft().item().module()+"|"+req.draft().item().operation()+"|"+
                req.draft().version().method()+"|"+req.effectiveOn()+"|"+
                new TreeMap<>(req.inputs())+"|"+req.draft().rules()+"|"+amount.toPlainString();
            signature=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}
        return new Fee3F7DomainSimulationService.Result("PRETAX_ONLY",domain,kind,CONTRACT,
            raw.toPlainString(),amount.toPlainString(),null,null,List.copyOf(trace),
            List.of(note,"DOCUMENTS_SUBMITTED_NOT_VERIFIED","TAX_AND_PAYABLE_TOTAL_NOT_COMPUTED",
                "NO_BANK_APPROVAL_NO_ORACLE_WRITE_NO_POSTING"),signature,false,false,false,false);
    }
    private static String require(Map<String,Object> values,String name) {
        Object val=values.get(name);
        if(!(val instanceof String text)||!text.matches("[A-Za-z0-9_./:-]{3,120}"))
            throw bad("DOCUMENT_REFERENCE_REQUIRED_"+name.toUpperCase());
        return text;
    }
    private static BigDecimal decimal(Object raw,String name) {
        try {
            if(raw==null||(!String.valueOf(raw).matches("\\d{1,20}(?:\\.\\d{1,10})?")))
                throw bad(name+"_INVALID");
            BigDecimal n=new BigDecimal(String.valueOf(raw));
            if(n.compareTo(MAX)>0)throw bad(name+"_OUT_OF_RANGE");
            return n;
        }catch(NumberFormatException ex){throw bad(name+"_INVALID");}
    }
    private static IllegalArgumentException bad(String reason){return new IllegalArgumentException(reason);}
    private static Fee3F7DomainSimulationService.Result blocked(String domain,String reason) {
        return new Fee3F7DomainSimulationService.Result("BLOCKED",domain,"UNSUPPORTED",CONTRACT,
            null,null,null,null,List.of(),List.of(reason),null,false,false,false,false);
    }
}
