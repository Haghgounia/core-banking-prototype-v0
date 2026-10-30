package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic, side-effect-free and deliberately bounded UAT arithmetic. Not bank posting. */
public final class Fee3PreviewEngine {
    private Fee3PreviewEngine() {}
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX = new BigDecimal("1000000000000000000");
    private static final Set<String> COLLATERALS = Set.of("CASH_100","TERM_DEPOSIT","GOLD",
        "DOMESTIC_BANK_GUARANTEE","EXPORT_GUARANTEE_FUND","FOREIGN_BANK_GUARANTEE",
        "LISTED_SHARES","LIQUID_REAL_ESTATE","OTHER_REAL_ESTATE","VESSEL","CHEQUE",
        "PROMISSORY_NOTE","OTHER_COLLATERAL","MOVABLE_PROPERTY");
    public record PreviewRequest(DraftRequest draft, Map<String,Object> inputs) {}
    public record PreviewResult(String status, boolean financialPosting, boolean persisted,
                                String method, BigDecimal rawIrr, BigDecimal feeIrr,
                                BigDecimal taxIrr, BigDecimal totalIrr,
                                List<Map<String,Object>> breakdown, List<String> diagnostics) {}

    public static PreviewResult calculate(PreviewRequest request) {
        String method=request==null||request.draft()==null||request.draft().version()==null?
            "UNKNOWN":request.draft().version().method();
        try {return calculateChecked(request,method);}
        catch (IllegalArgumentException | ArithmeticException ex) {
            return new PreviewResult("BLOCKED",false,false,method,null,null,null,null,
                List.of(),List.of(ex.getMessage()==null?"UNSUPPORTED_CONTRACT":ex.getMessage()));
        }
    }
    private static PreviewResult calculateChecked(PreviewRequest request,String method) {
        if(request==null||request.draft()==null||request.draft().item()==null||
           request.draft().version()==null)throw bad("MISSING_DRAFT");
        DraftRequest draft=request.draft(); VersionDraft v=draft.version(); ItemDraft item=draft.item();
        Map<String,Object> inputs=request.inputs()==null?Map.of():request.inputs();
        if(!"IRR".equals(v.feeCurrency())||!"IRR".equals(v.serviceCurrency()))throw bad("CURRENCY_CONVERSION_UNSUPPORTED");
        if(method==null||!Set.of("FIXED","PERCENT","MIXED","USAGE","DAILY","BRACKET","PROGRESSIVE","MATRIX","FORMULA").contains(method))throw bad("METHOD_NOT_CERTIFIED_FOR_SERVER_PREVIEW");
        if(draft.rules()==null||draft.rules().size()!=1)throw bad("EXACTLY_ONE_RULE_REQUIRED");
        RuleDraft rule=draft.rules().get(0);
        if(rule==null)throw bad("NULL_RULE");
        if(!method.equals(rule.method()))throw bad("RULE_METHOD_MISMATCH");
        if(!"FORMULA".equals(method) && (rule.formulaCode()!=null&&!rule.formulaCode().isBlank() ||
           rule.parameters()!=null&&rule.parameters().containsKey("_fceEditableExpressionV1")))
           throw bad("FORMULA_EXECUTION_FORBIDDEN");
        Map<String,Object> policy=v.financialPolicy()==null?Map.of():v.financialPolicy();
        for(String key:List.of("partsEnabled","adjustmentsEnabled","dealsEnabled","allocationEnabled"))
            if(Boolean.TRUE.equals(policy.get(key)))throw bad("COMPLEX_FINANCIAL_POLICY_UNSUPPORTED");
        RoundingMode rounding=rounding(v.rounding());
        BigDecimal raw;
        List<Map<String,Object>> trace=new ArrayList<>();
        Map<String,Object> context=v.uiContext()==null?Map.of():v.uiContext();
        if(context.get("guaranteePolicy") instanceof Map<?,?> g) {
            if(!"GUARANTEE".equals(item.module())||!"GUARANTEE_ISSUE".equals(item.operation())||
               !"GUARANTEE_FEE".equals(item.amountKind()))throw bad("GUARANTEE_CONTEXT_MISMATCH");
            raw=guarantee(rule,g,method,inputs,trace);
        } else {
            if(Set.of("GUARANTEE_FEE","QARD_FEE","FINANCING_PROFIT","LATE_DAMAGE").contains(item.amountKind()))
                throw bad("SPECIALIZED_BANKING_MODEL_REQUIRES_CERTIFIED_ENGINE");
            raw="FORMULA".equals(method)?Fee3F6FormulaArithmetic.evaluate(rule,inputs,trace):
                simple(rule,method,inputs,trace);
        }
        if(raw.compareTo(BigDecimal.ZERO)<0||raw.compareTo(MAX)>0)throw bad("FEE_OUT_OF_RANGE");
        BigDecimal capped=raw;
        if(rule.min()!=null)capped=capped.max(nonnegative(rule.min(),"MIN_FEE"));
        if(rule.max()!=null)capped=capped.min(nonnegative(rule.max(),"MAX_FEE"));
        if(rule.min()!=null&&rule.max()!=null&&rule.min().compareTo(rule.max())>0)throw bad("MIN_GREATER_THAN_MAX");
        BigDecimal fee=capped.setScale(0,rounding);
        trace.add(Map.of("kind","LIMITS_AND_ROUNDING","rawIrr",raw,
            "limitedIrr",capped,"roundedIrr",fee,"rounding",v.rounding()));
        BigDecimal tax=BigDecimal.ZERO;
        if("TAXABLE".equals(v.taxMode())){
            BigDecimal percent=bounded(policy.get("taxPercent"),"TAX_PERCENT",HUNDRED,true);
            tax=fee.multiply(percent).divide(HUNDRED,12,RoundingMode.HALF_EVEN).setScale(0,rounding);
        } else if(v.taxMode()==null||!Set.of("EXEMPT","NONE","NON_TAXABLE").contains(v.taxMode())){
            throw bad("TAX_POLICY_NOT_REVIEWED");
        }
        if(fee.add(tax).compareTo(MAX)>0)throw bad("TOTAL_OUT_OF_RANGE");
        return new PreviewResult("SIMULATED",false,false,method,raw,fee,tax,fee.add(tax),
            List.copyOf(trace),List.of("UAT_SIMULATION_ONLY","NO_ORACLE_MUTATION","NO_ACCOUNTING_POSTING"));
    }
    private static final Set<String> F6_MONEY_BASES = Set.of("REQUEST_AMOUNT","OPENING_FUNDING",
        "TRANSFER_AMOUNT","BILL_AMOUNT","GUARANTEE_AMOUNT","FINANCING_AMOUNT");
    private static final Set<String> F6_QUANTITY_BASES = Set.of("CONSUMPTION_QTY","SERVICE_COUNT");
    private static final BigDecimal F6_RATE_MAX = new BigDecimal("1000");
    private static final BigDecimal F6_COUNT_MAX = new BigDecimal("1000000000");

    /** R3 compatible plus F6 methods; no arbitrary expression evaluation. */
    private static BigDecimal simple(RuleDraft rule,String method,Map<String,Object> inputs,
                                     List<Map<String,Object>> trace) {
        if("FIXED".equals(method)){
            if(rule.fixed()==null)throw bad("FIXED_AMOUNT_REQUIRED");
            BigDecimal fixed=nonnegative(rule.fixed(),"FIXED_AMOUNT");
            trace.add(Map.of("kind","FIXED","fixedIrr",fixed));
            return fixed;
        }
        if("PERCENT".equals(method)||"MIXED".equals(method)){
            BigDecimal amount=positive(inputs.get("amount"),"AMOUNT",MAX);
            BigDecimal variable=amount.multiply(rate(rule,inputs)).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
            trace.add(Map.of("kind","PERCENT","baseIrr",amount,"variableIrr",variable));
            if("PERCENT".equals(method))return variable;
            if(rule.fixed()==null)throw bad("MIXED_FIXED_AMOUNT_REQUIRED");
            BigDecimal fixed=nonnegative(rule.fixed(),"FIXED_AMOUNT");
            trace.add(Map.of("kind","MIXED_FIXED","fixedIrr",fixed));
            return variable.add(fixed);
        }
        Map<?,?> config=f6Config(rule.parameters());
        if("USAGE".equals(method)){
            BigDecimal units=f6Integer(inputs.get("usage"),"USAGE",F6_COUNT_MAX);
            BigDecimal free=f6Integer(config.get("freeUnits"),"FREE_UNITS",F6_COUNT_MAX);
            BigDecimal price=bounded(config.get("unitPrice"),"UNIT_PRICE",MAX,true);
            BigDecimal chargeable=units.subtract(free).max(BigDecimal.ZERO);
            trace.add(Map.of("kind","USAGE","chargeableUnits",chargeable,"unitPriceIrr",price));
            return chargeable.multiply(price);
        }
        if("DAILY".equals(method)){
            String basis=code(config.get("dayBasis"));
            if("ACT_ACT".equals(basis)||"JALALI_ACT_ACT".equals(basis))
                throw bad("ACT_ACT_REQUIRES_CAL2_CERTIFIED_DAY_COUNT");
            if(!"ACT_360".equals(basis)&&!"ACT_365".equals(basis))throw bad("DAY_BASIS_UNKNOWN");
            BigDecimal days=f6Integer(inputs.get("days"),"DAYS",new BigDecimal("20000"));
            if(days.signum()==0)throw bad("DAYS_MUST_BE_POSITIVE");
            BigDecimal a=positive(inputs.get("amount"),"AMOUNT",MAX);
            BigDecimal amount=a.multiply(rate(rule,inputs)).multiply(days).divide(
                HUNDRED.multiply(new BigDecimal("ACT_360".equals(basis)?360:365)),12,RoundingMode.HALF_EVEN);
            trace.add(Map.of("kind","DAILY","dayBasis",basis,"calendarDays",days));
            return amount;
        }
        if("MATRIX".equals(method))return f6Matrix(config,inputs,trace);
        if("BRACKET".equals(method)||"PROGRESSIVE".equals(method))return f6Bands(config,inputs,method,trace);
        throw bad("METHOD_NOT_CERTIFIED_FOR_SERVER_PREVIEW");
    }
    private static Map<?,?> f6Config(Map<String,Object> parameters){
        if(parameters==null)throw bad("F6_PARAMETERS_MISSING");
        Object original=parameters.get("originalConfig");
        // Existing Oracle FCE items put the authoritative tariff model in originalConfig.
        if(original==null)return parameters;
        if(!(original instanceof Map<?,?> config))throw bad("F6_CONFIG_MALFORMED");
        return config;
    }
    private static BigDecimal f6Integer(Object raw,String field,BigDecimal maximum){
        BigDecimal value=bounded(raw,field,maximum,true);
        if(value.stripTrailingZeros().scale()>0)throw bad(field+"_MUST_BE_INTEGER");
        return value;
    }
    private record F6Basis(BigDecimal value,boolean monetary,String code) {}
    private static F6Basis f6Basis(Map<?,?> cfg,Map<String,Object> inputs){
        String code=code(cfg.get("basisCode"));
        if(code.isEmpty()){
            if(Boolean.TRUE.equals(cfg.get("basisExplicit")))throw bad("F6_BASIS_REQUIRED");
            code="REQUEST_AMOUNT";
        }
        if(F6_MONEY_BASES.contains(code))
            return new F6Basis(bounded(inputs.get("amount"),"AMOUNT",MAX,true),true,code);
        if(F6_QUANTITY_BASES.contains(code))
            return new F6Basis(bounded(inputs.get("usage"),"USAGE",F6_COUNT_MAX,true),false,code);
        throw bad("F6_UNRECOGNIZED_BASIS");
    }
    private static List<?> f6Rows(Object data,String type){
        if(!(data instanceof List<?> rows)||rows.isEmpty()||rows.size()>100)throw bad(type+"_ROWS_REQUIRED");
        for(Object row:rows)if(!(row instanceof Map<?,?>))throw bad(type+"_MALFORMED_ROW");
        return rows;
    }
    private static BigDecimal f6RowFee(Map<?,?> row,BigDecimal share,boolean monetary){
        String mode=code(row.get("calcType"));if(mode.isEmpty())mode="LEGACY";
        return switch(mode){
          case "FIXED" -> bounded(row.get("fixedAmount"),"F6_FIXED",MAX,true);
          case "UNIT_RATE" -> {
              if(monetary)throw bad("F6_UNIT_RATE_REQUIRES_QUANTITY");
              yield share.multiply(bounded(row.get("unitPrice"),"F6_UNIT_PRICE",MAX,true));
          }
          case "PERCENT" -> {
              if(!monetary)throw bad("F6_PERCENT_REQUIRES_MONEY");
              yield share.multiply(bounded(row.get("ratePercent"),"F6_PERCENT",F6_RATE_MAX,true))
                    .divide(HUNDRED,12,RoundingMode.HALF_EVEN);
          }
          case "LEGACY" -> {
              if(!monetary)throw bad("F6_LEGACY_REQUIRES_MONEY");
              yield share.multiply(bounded(row.get("ratePercent"),"F6_PERCENT",F6_RATE_MAX,true))
                  .divide(HUNDRED,12,RoundingMode.HALF_EVEN)
                  .add(bounded(row.get("fixedAmount"),"F6_FIXED",MAX,true));
          }
          default -> throw bad("F6_UNKNOWN_ROW_MODE");
        };
    }
    private static BigDecimal f6Bands(Map<?,?> cfg,Map<String,Object> inputs,String method,
                                      List<Map<String,Object>> trace){
        F6Basis basis=f6Basis(cfg,inputs);
        List<?> rows=f6Rows(cfg.get("BRACKET".equals(method)?"brackets":"tiers"),method);
        if("BRACKET".equals(method)){
            Map<?,?> selected=null;int selectedIndex=0;
            for(int i=0;i<rows.size();i++){
                Map<?,?> row=(Map<?,?>)rows.get(i);
                BigDecimal from=bounded(row.get("from"),"F6_FROM",MAX,true);
                Object t=row.get("to");
                BigDecimal upper=t==null||"".equals(t)?null:bounded(t,"F6_TO",MAX,true);
                if(upper!=null&&upper.compareTo(from)<=0)throw bad("F6_BRACKET_BOUNDARY_INVALID");
                boolean hit=basis.value().compareTo(from)>=0&&(upper==null||
                    (Boolean.TRUE.equals(row.get("inclusiveUpper"))?
                      basis.value().compareTo(upper)<=0:basis.value().compareTo(upper)<0));
                if(hit){if(selected!=null)throw bad("F6_BRACKET_AMBIGUOUS");selected=row;selectedIndex=i+1;}
            }
            if(selected==null)throw bad("F6_BRACKET_UNDEFINED");
            BigDecimal raw=f6RowFee(selected,basis.value(),basis.monetary());
            trace.add(Map.of("kind","BRACKET","basis",basis.code(),"row",selectedIndex));
            return raw;
        }
        BigDecimal last=BigDecimal.ZERO,total=BigDecimal.ZERO;boolean covered=false;
        for(int i=0;i<rows.size();i++){
            Map<?,?> row=(Map<?,?>)rows.get(i);
            Object t=row.get("upTo");
            BigDecimal upper=t==null||"".equals(t)?null:bounded(t,"F6_UP_TO",MAX,true);
            if(upper==null&&i!=rows.size()-1)throw bad("F6_OPEN_TIER_NOT_LAST");
            if(upper!=null&&upper.compareTo(last)<=0)throw bad("F6_TIER_BOUNDARY_INVALID");
            BigDecimal share=basis.value().subtract(last).max(BigDecimal.ZERO);
            if(upper!=null)share=share.min(upper.subtract(last));
            if(share.signum()>0){
                total=total.add(f6RowFee(row,share,basis.monetary()));
                trace.add(Map.of("kind","TIER","basis",basis.code(),"row",i+1,"share",share));
            }
            if(upper==null||basis.value().compareTo(upper)<=0){covered=true;break;}
            last=upper;
        }
        if(!covered)throw bad("F6_TIER_UNDEFINED");
        return total;
    }
    private static BigDecimal f6Matrix(Map<?,?> cfg,Map<String,Object> inputs,
                                      List<Map<String,Object>> trace){
        String customer=code(inputs.get("customer")),channel=code(inputs.get("channel"));
        if(customer.isBlank()||channel.isBlank()||"ALL".equals(customer)||"ALL".equals(channel)||
           !customer.matches("[A-Z][A-Z0-9_]{0,39}")||!channel.matches("[A-Z][A-Z0-9_]{0,39}"))
            throw bad("F6_MATRIX_CUSTOMER_CHANNEL_REQUIRED");
        List<?> rows=f6Rows(cfg.get("matrix"),"MATRIX");
        Map<?,?> selected=null;int best=-1;boolean ambiguous=false;
        for(Object obj:rows){
            Map<?,?> row=(Map<?,?>)obj;
            String c=code(row.get("customer")),h=code(row.get("channel")),mode=code(row.get("basis"));
            if(c.isBlank()||h.isBlank()||!Set.of("FIXED","PERCENT").contains(mode))
                throw bad("F6_MATRIX_BAD_ROW");
            if(!("ALL".equals(c)||c.equals(customer))||!("ALL".equals(h)||h.equals(channel)))continue;
            int specificity=("ALL".equals(c)?0:1)+("ALL".equals(h)?0:1);
            if(specificity==best)ambiguous=true;
            if(specificity>best){selected=row;best=specificity;ambiguous=false;}
        }
        if(selected==null)throw bad("F6_MATRIX_NO_CELL");
        if(ambiguous)throw bad("F6_MATRIX_AMBIGUOUS");
        String mode=code(selected.get("basis"));
        BigDecimal value=bounded(selected.get("value"),"F6_MATRIX_VALUE",
            "PERCENT".equals(mode)?F6_RATE_MAX:MAX,true);
        BigDecimal amount="PERCENT".equals(mode)?
           bounded(inputs.get("amount"),"AMOUNT",MAX,true).multiply(value)
             .divide(HUNDRED,12,RoundingMode.HALF_EVEN):value;
        trace.add(Map.of("kind","MATRIX","customer",code(selected.get("customer")),
            "channel",code(selected.get("channel")),"basis",mode));
        return amount;
    }
    private static BigDecimal guarantee(RuleDraft rule,Map<?,?> guarantee,String method,
                                        Map<String,Object> inputs,List<Map<String,Object>> trace) {
        String type=code(guarantee.get("collateralType"));
        String basis=code(guarantee.get("feeBasis"));
        BigDecimal amount=positive(inputs.get("amount"),"GUARANTEE_AMOUNT",MAX);
        if("MIXED_PORTFOLIO".equals(type)){
            if(!"PERCENT".equals(method))throw bad("PORTFOLIO_PERCENT_ONLY_IN_R3");
            if(!Set.of("GUARANTEE_PORTFOLIO_ALLOCATED","GUARANTEE_PORTFOLIO_COLLATERAL_VALUE").contains(basis))
                throw bad("INVALID_PORTFOLIO_BASIS");
            Object rawRules=guarantee.get("portfolioRules"),rawRows=inputs.get("portfolio");
            if(!(rawRules instanceof List<?> rules)||!(rawRows instanceof List<?> rows) ||
               rules.isEmpty()||rules.size()>50||rows.isEmpty()||rows.size()>50)
                throw bad("PORTFOLIO_RULES_AND_SHARES_REQUIRED");
            Map<String,Map<?,?>> tariffs=new HashMap<>();
            for(Object obj:rules){
                if(!(obj instanceof Map<?,?> r))throw bad("INVALID_PORTFOLIO_RULE");
                String c=code(r.get("collateralType"));
                if(!COLLATERALS.contains(c)||tariffs.putIfAbsent(c,r)!=null)throw bad("DUPLICATE_OR_UNKNOWN_COLLATERAL");
                BigDecimal cov=positive(r.get("coveragePercent"),"COVERAGE_PERCENT",new BigDecimal("1000"));
                if("CASH_100".equals(c)&&cov.compareTo(HUNDRED)<0)throw bad("CASH_COVERAGE_BELOW_100");
                String source=code(r.get("rateSource"));
                if(!Set.of("CONFIG","REQUEST","EXEMPT").contains(source))throw bad("INVALID_RATE_SOURCE");
                if("CONFIG".equals(source))bounded(r.get("ratePercent"),"CONFIG_RATE",new BigDecimal("1000"),true);
                else if(r.get("ratePercent")!=null)throw bad("RATE_NOT_OWNED_BY_CONFIG");
            }
            Set<String> selected=new HashSet<>();BigDecimal shares=BigDecimal.ZERO;BigDecimal total=BigDecimal.ZERO;
            for(Object obj:rows){
                if(!(obj instanceof Map<?,?> row))throw bad("INVALID_COLLATERAL_SHARE");
                String c=code(row.get("collateralType"));
                Map<?,?> tariff=tariffs.get(c);
                if(tariff==null||!selected.add(c))throw bad("UNKNOWN_OR_DUPLICATE_COLLATERAL_SHARE");
                BigDecimal share=positive(row.get("sharePercent"),"SHARE_PERCENT",HUNDRED);
                shares=shares.add(share);
                BigDecimal cov=positive(tariff.get("coveragePercent"),"COVERAGE_PERCENT",new BigDecimal("1000"));
                String source=code(tariff.get("rateSource"));
                BigDecimal r="EXEMPT".equals(source)?BigDecimal.ZERO:
                    "REQUEST".equals(source)?bounded(inputs.get("contractRate"),"CONTRACT_RATE",new BigDecimal("1000"),true):
                    bounded(tariff.get("ratePercent"),"CONFIG_RATE",new BigDecimal("1000"),true);
                BigDecimal allocated=amount.multiply(share).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
                BigDecimal covered=allocated.multiply(cov).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
                BigDecimal base="GUARANTEE_PORTFOLIO_COLLATERAL_VALUE".equals(basis)?covered:allocated;
                BigDecimal fee=base.multiply(r).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
                total=total.add(fee);
                Map<String,Object> entry=new LinkedHashMap<>();
                entry.put("collateralType",c);entry.put("sharePercent",share);entry.put("coveragePercent",cov);
                entry.put("allocatedIrr",allocated);entry.put("coveredIrr",covered);entry.put("feeBaseIrr",base);
                entry.put("ratePercent",r);entry.put("source",source);entry.put("rawFeeIrr",fee);
                trace.add(entry);
            }
            if(shares.compareTo(HUNDRED)!=0)throw bad("PORTFOLIO_SHARES_MUST_EQUAL_100");
            return total;
        }
        if(!COLLATERALS.contains(type))throw bad("INVALID_SINGLE_COLLATERAL");
        if(!Set.of("GUARANTEE_AMOUNT","GUARANTEE_COLLATERAL_COVERED_VALUE").contains(basis))
            throw bad("INVALID_GUARANTEE_BASIS");
        if("CASH_100".equals(type))positive(inputs.get("cashCoveragePercent"),"CASH_COVERAGE",new BigDecimal("1000"));
        BigDecimal base=amount;
        if("GUARANTEE_COLLATERAL_COVERED_VALUE".equals(basis)){
            BigDecimal cov=positive(inputs.get("CASH_100".equals(type)?"cashCoveragePercent":"collateralCoveragePercent"),
                "COLLATERAL_COVERAGE",new BigDecimal("1000"));
            if("CASH_100".equals(type)&&cov.compareTo(HUNDRED)<0)throw bad("CASH_COVERAGE_BELOW_100");
            base=amount.multiply(cov).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
        }
        if("FIXED".equals(method)){
            if(!"GUARANTEE_AMOUNT".equals(basis))throw bad("FIXED_WITH_COVERAGE_FORBIDDEN");
            if(rule.fixed()==null)throw bad("FIXED_AMOUNT_REQUIRED");
            return nonnegative(rule.fixed(),"FIXED_AMOUNT");
        }
        if(!"PERCENT".equals(method))throw bad("GUARANTEE_METHOD_NOT_SUPPORTED");
        BigDecimal rate=rate(rule,inputs);
        return base.multiply(rate).divide(HUNDRED,12,RoundingMode.HALF_EVEN);
    }
    private static String code(Object value){return value instanceof String s?s:"";}
    private static BigDecimal rate(RuleDraft rule,Map<String,Object> inputs) {
        if("RULE".equals(rule.rateSource()))return bounded(rule.rate(),"RULE_RATE",new BigDecimal("1000"),true);
        if("REQUEST".equals(rule.rateSource())){
            if(rule.rate()!=null)throw bad("REQUEST_RATE_MUST_NOT_BE_PERSISTED");
            return bounded(inputs.get("contractRate"),"CONTRACT_RATE",new BigDecimal("1000"),true);
        }
        throw bad("UNKNOWN_RATE_SOURCE");
    }
    private static BigDecimal positive(Object value,String name,BigDecimal maximum){
        BigDecimal n=bounded(value,name,maximum,false);
        if(n.compareTo(BigDecimal.ZERO)==0)throw bad(name+"_MUST_BE_POSITIVE");
        return n;
    }
    private static BigDecimal nonnegative(BigDecimal value,String name){
        if(value==null||value.compareTo(BigDecimal.ZERO)<0||value.compareTo(MAX)>0)throw bad(name+"_OUT_OF_RANGE");
        return value;
    }
    private static BigDecimal bounded(Object value,String name,BigDecimal maximum,boolean allowZero){
        if(value==null)throw bad(name+"_REQUIRED");
        BigDecimal n;
        try {
            if(value instanceof BigDecimal b)n=b;
            else if(value instanceof Number || value instanceof String)n=new BigDecimal(String.valueOf(value));
            else throw bad(name+"_INVALID_TYPE");
        }catch(NumberFormatException ex){throw bad(name+"_INVALID_NUMBER");}
        if(n.compareTo(BigDecimal.ZERO)<0||(!allowZero&&n.signum()==0)||n.compareTo(maximum)>0||n.scale()>12)
            throw bad(name+"_OUT_OF_RANGE");
        return n;
    }
    private static RoundingMode rounding(String mode){
        if(mode==null)throw bad("ROUNDING_REQUIRED");
        return switch(mode){
            case "HALF_UP" -> RoundingMode.HALF_UP;
            case "HALF_EVEN" -> RoundingMode.HALF_EVEN;
            case "DOWN", "FLOOR" -> RoundingMode.FLOOR;
            case "UP", "CEIL" -> RoundingMode.CEILING;
            default -> throw bad("ROUNDING_POLICY_UNSUPPORTED");
        };
    }
    private static IllegalArgumentException bad(String code){return new IllegalArgumentException(code);}
}
