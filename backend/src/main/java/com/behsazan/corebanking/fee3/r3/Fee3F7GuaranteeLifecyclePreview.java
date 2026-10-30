package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.DraftRequest;
import com.behsazan.corebanking.fee3.domain.Fee3Models.RuleDraft;
import com.behsazan.corebanking.fee3.r4b2.Fee3R4B2GuaranteeContract;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Read-only analyst preview of independently documented guarantee renewals and amendments.
 * No issuance rate is copied to renewal/change. Each draft must provide its own rule and
 * approval/evidence; portfolio reallocations and refunds fail closed pending bank policy.
 */
public final class Fee3F7GuaranteeLifecyclePreview {
    private Fee3F7GuaranteeLifecyclePreview() {}
    private static final String CONTRACT="FEE3_F7_GUARANTEE_LIFECYCLE_DRAFT_V1";
    private static final BigDecimal MAX=new BigDecimal("1000000000000000000");
    private static final BigDecimal HUNDRED=new BigDecimal("100");
    private static final MathContext MC=MathContext.DECIMAL128;
    public static boolean supports(DraftRequest draft){
        return draft!=null&&draft.item()!=null&&"GUARANTEE".equals(draft.item().module())&&
            "GUARANTEE_FEE".equals(draft.item().amountKind())&&
            Set.of("GUARANTEE_RENEW","GUARANTEE_CHANGE").contains(draft.item().operation());
    }
    public static Fee3F7DomainSimulationService.Result simulate(Fee3F7DomainSimulationService.Request req){
        try{return checked(req);}catch(IllegalArgumentException|ArithmeticException err){
            String code=err.getMessage();return blocked(code!=null&&code.matches("[A-Z0-9_]{3,100}")?
                code:"GUARANTEE_LIFECYCLE_UNVERIFIED");}
    }
    private static IllegalArgumentException bad(String msg){return new IllegalArgumentException(msg);}
    private static String ref(Map<String,Object> values,String key){
        Object raw=values.get(key);
        if(!(raw instanceof String s)||!s.matches("[A-Za-z0-9_./:-]{3,100}"))
            throw bad("GUARANTEE_"+key.toUpperCase()+"_REQUIRED");
        return s;
    }
    private static BigDecimal number(Object raw,String name,boolean positive){
        if(raw==null||!String.valueOf(raw).matches("\\d{1,20}(?:\\.\\d{1,10})?"))throw bad(name+"_INVALID");
        BigDecimal n=new BigDecimal(String.valueOf(raw));
        if((positive?n.signum()<=0:n.signum()<0)||n.compareTo(MAX)>0||
            Math.max(0,n.stripTrailingZeros().scale())>8)throw bad(name+"_OUT_OF_RANGE");
        return n;
    }
    private static LocalDate date(Map<String,Object> inputs,String name){
        Object raw=inputs.get(name);
        if(!(raw instanceof String s)||!s.matches("\\d{4}-\\d{2}-\\d{2}"))throw bad(name.toUpperCase()+"_DATE_REQUIRED");
        try{return LocalDate.parse(s);}catch(RuntimeException ex){throw bad(name.toUpperCase()+"_DATE_INVALID");}
    }
    private static Fee3F7DomainSimulationService.Result checked(Fee3F7DomainSimulationService.Request req){
        if(req==null||!supports(req.draft())||req.inputs()==null)throw bad("GUARANTEE_LIFECYCLE_REQUEST_REQUIRED");
        var d=req.draft();var v=d.version();var inputs=req.inputs();
        if(v==null||d.rules()==null||d.rules().size()!=1||d.rules().get(0)==null)
            throw bad("GUARANTEE_SINGLE_RULE_REQUIRED");
        RuleDraft rule=d.rules().get(0);
        String method=v.method();
        if(!Set.of("PERCENT","DAILY","FIXED").contains(method)||!method.equals(rule.method()))
            throw bad("GUARANTEE_LIFECYCLE_METHOD_NOT_ALLOWED");
        if(!"IRR".equals(v.feeCurrency())||!"IRR".equals(v.serviceCurrency()))throw bad("GUARANTEE_IRR_ONLY");
        if(req.effectiveOn()==null||v.validFrom()==null||req.effectiveOn().isBefore(v.validFrom())||
            v.validTo()!=null&&req.effectiveOn().isAfter(v.validTo()))throw bad("GUARANTEE_VERSION_NOT_EFFECTIVE");
        // An independently-defined lifecycle tariff is mandatory. A generic issuance
        // guarantee policy, mixed portfolio or coverage allocation is not silently reused.
        Map<?,?> policy=v.uiContext()!=null&&v.uiContext().get("guaranteePolicy") instanceof Map<?,?> p?p:null;
        // Issuance collateral policies must NOT be inherited by a lifecycle action.
        // Independent lifecycle basis, rate and approval evidence are carried in inputs.
        if(policy!=null && ("MIXED_PORTFOLIO".equals(policy.get("collateralType"))||
            "GUARANTEE_COLLATERAL_COVERED_VALUE".equals(policy.get("feeBasis"))||
            policy.get("portfolioRules") instanceof List<?> rows&&!rows.isEmpty()))
            throw bad("GUARANTEE_ISSUANCE_PORTFOLIO_RULE_NOT_APPLICABLE");
        if(rule.parameters()!=null&&(rule.parameters().containsKey("_fceEditableExpressionV1")||
           rule.parameters().containsKey("yearFraction")))throw bad("GUARANTEE_UNREVIEWED_FORMULA_FIELDS");
        ref(inputs,"guaranteeReference");ref(inputs,"lifecycleApprovalReference");
        ref(inputs,"regulatoryReference");ref(inputs,"sourceEvidenceReference");
        ref(inputs,"feeBasisReference");
        BigDecimal base;
        String operation=d.item().operation();
        Map<String,String> event=new LinkedHashMap<>();
        if("GUARANTEE_RENEW".equals(operation)){
            LocalDate oldEnd=date(inputs,"previousEndExclusive");
            LocalDate newEnd=date(inputs,"newEndExclusive");
            if(!newEnd.isAfter(oldEnd)||newEnd.toEpochDay()-oldEnd.toEpochDay()>36525)
                throw bad("GUARANTEE_RENEWAL_INTERVAL_INVALID");
            if(!"RENEWAL_EXTENSION".equals(inputs.get("feeBasisPolicy")))
                throw bad("GUARANTEE_RENEWAL_BASIS_NOT_APPROVED");
            base=number(inputs.get("amount"),"GUARANTEE_RENEWAL_AMOUNT",true);
            event.put("previousEndExclusive",oldEnd.toString());event.put("newEndExclusive",newEnd.toString());
            event.put("feeBasisPolicy","RENEWAL_EXTENSION");
        }else{
            String policyCode=String.valueOf(inputs.get("feeBasisPolicy"));
            BigDecimal previous=number(inputs.get("previousAmount"),"GUARANTEE_PREVIOUS_AMOUNT",false);
            BigDecimal revised=number(inputs.get("newAmount"),"GUARANTEE_NEW_AMOUNT",true);
            base=switch(policyCode){
                case "INCREASE_ONLY"-> {
                    if(revised.compareTo(previous)<=0)throw bad("GUARANTEE_INCREASE_MUST_BE_POSITIVE");
                    yield revised.subtract(previous);
                }
                case "FULL_NEW_AMOUNT"->revised;
                case "FIXED_NOTICE"-> {
                    if(!"FIXED".equals(method))throw bad("GUARANTEE_FIXED_NOTICE_REQUIRES_FIXED_RULE");
                    yield revised;
                }
                default->throw bad("GUARANTEE_CHANGE_BASIS_NOT_APPROVED");
            };
            event.put("previousAmountIrr",previous.toPlainString());
            event.put("newAmountIrr",revised.toPlainString());
            event.put("feeBasisPolicy",policyCode);
        }
        String basis=event.get("feeBasisPolicy");
        if("FIXED".equals(method)&&!"FIXED_NOTICE".equals(basis))
            throw bad("GUARANTEE_FIXED_LIFECYCLE_UNSUPPORTED");
        if(!"FIXED".equals(method)&&rule.fixed()!=null)throw bad("GUARANTEE_UNEXPECTED_FIXED_AMOUNT");
        BigDecimal rate=BigDecimal.ZERO;
        if(!"FIXED".equals(method)){
            rate="REQUEST".equals(rule.rateSource())?
                number(inputs.get("contractRate"),"GUARANTEE_CONTRACT_RATE",false):
                "RULE".equals(rule.rateSource())?rule.rate():null;
            if(rate==null||rate.signum()<0||rate.compareTo(new BigDecimal("1000"))>0)
                throw bad("GUARANTEE_INDEPENDENT_RATE_REQUIRED");
            if("REQUEST".equals(rule.rateSource()))ref(inputs,"rateEvidenceReference");
            if("RULE".equals(rule.rateSource())&&inputs.containsKey("contractRate"))
                throw bad("GUARANTEE_RATE_OVERRIDE_FORBIDDEN");
        }
        BigDecimal factor=BigDecimal.ONE;
        if("DAILY".equals(method)){
            LocalDate first=date(inputs,"periodStart");LocalDate end=date(inputs,"periodEndExclusive");
            if("GUARANTEE_RENEW".equals(operation)&&
                (!first.equals(date(inputs,"previousEndExclusive"))||
                 !end.equals(date(inputs,"newEndExclusive"))))
                throw bad("GUARANTEE_RENEWAL_PERIOD_MISMATCH");
            Object cfg=rule.parameters()==null?null:rule.parameters().get("originalConfig");
            String days=cfg instanceof Map<?,?> map?String.valueOf(map.get("dayBasis")):"";
            if(!Set.of("ACT_ACT","ACT_365","ACT_360").contains(days))
                throw bad("GUARANTEE_DAY_BASIS_NOT_APPROVED");
            factor=Fee3R4B2GuaranteeContract.fraction(first,end,days);
            event.put("periodStart",first.toString());event.put("periodEndExclusive",end.toString());
            event.put("dayBasis",days);
        }
        BigDecimal raw="FIXED".equals(method)?rule.fixed():base.multiply(rate,MC).divide(HUNDRED,MC).multiply(factor,MC);
        if(raw==null||raw.signum()<0||raw.compareTo(MAX)>0)throw bad("GUARANTEE_RESULT_OUT_OF_RANGE");
        BigDecimal capped=raw;
        if(rule.min()!=null)capped=capped.max(rule.min());
        if(rule.max()!=null)capped=capped.min(rule.max());
        if(capped.signum()<0||capped.compareTo(MAX)>0||
            rule.min()!=null&&rule.max()!=null&&rule.min().compareTo(rule.max())>0)
            throw bad("GUARANTEE_LIMIT_INVALID");
        if(v.rounding()==null)throw bad("GUARANTEE_ROUNDING_NOT_APPROVED");
        RoundingMode rounding=switch(v.rounding()){
            case "HALF_UP"->RoundingMode.HALF_UP;case "HALF_EVEN"->RoundingMode.HALF_EVEN;
            case "UP","CEIL"->RoundingMode.CEILING;case "DOWN","FLOOR"->RoundingMode.FLOOR;
            default->throw bad("GUARANTEE_ROUNDING_NOT_APPROVED");};
        BigDecimal amount=capped.setScale(0,rounding);
        event.put("kind",operation);event.put("calculationMethod",method);
        event.put("feeBaseIrr",base.toPlainString());event.put("ratePercent",rate.toPlainString());
        event.put("yearFraction",factor.toPlainString());event.put("rawIrr",raw.toPlainString());
        event.put("roundedIrr",amount.toPlainString());
        String sha;
        try{
            String canonical=CONTRACT+"|"+d.item().operation()+"|"+req.effectiveOn()+"|"+
                new TreeMap<>(inputs)+"|"+d.rules()+"|"+event;
            sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
        return new Fee3F7DomainSimulationService.Result("PRETAX_ONLY","GUARANTEE","GUARANTEE_FEE",CONTRACT,
            raw.toPlainString(),amount.toPlainString(),null,null,List.of(Map.copyOf(event)),
            List.of("INDEPENDENT_LIFECYCLE_RULE_UNVERIFIED","DOCUMENT_REFERENCES_NOT_AUTHENTICATED",
                "REFUNDS_AND_PORTFOLIO_REALLOCATIONS_NOT_CALCULATED","NO_TAX_NO_POSTING_NO_ORACLE_WRITE"),
            sha,false,false,false,false);
    }
    private static Fee3F7DomainSimulationService.Result blocked(String reason){
        return new Fee3F7DomainSimulationService.Result("BLOCKED","GUARANTEE","GUARANTEE_FEE",CONTRACT,
            null,null,null,null,List.of(),List.of(reason),null,false,false,false,false);
    }
}
