package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.DraftRequest;
import com.behsazan.corebanking.fee3.domain.Fee3Models.RuleDraft;
import com.behsazan.corebanking.fee3.r4c1.Fee3R4C1LoanProfitContract.YearSlice;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * F7 late-damage *draft-only* calculation: no bank authorization, collection or Oracle writes.
 * The calendar/year denominator comes from CAL2 via server-side repositories, not HTTP inputs.
 * Reference FCE V13.8.84, late-damage policy (due+1, exclusive end, explicit holiday policy).
 */
public final class Fee3F7LateDamageContract {
    private Fee3F7LateDamageContract() {}
    public static final String CONTRACT="FEE3_F7_LATE_DAMAGE_CAL2_V1";
    private static final BigDecimal MAX=new BigDecimal("1000000000000000000");
    private static final BigDecimal HUNDRED=new BigDecimal("100");
    private static final MathContext MC=MathContext.DECIMAL128;
    private static final String FORMULA="amount*(rate+penaltyMargin)/100*yearFraction";
    private static final Set<String> BASES=Set.of("JALALI_ACT_ACT","ACT_365","ACT_360");
    private static final Set<String> POLICIES=Set.of("CHARGE_ALL_DAYS","ROLL_DUE_TO_WORKDAY","EXCLUDE_ALL_HOLIDAYS");

    public record Workday(LocalDate date,boolean business,String evidence) {}
    public record Context(LocalDate due,LocalDate endExclusive,String dayBasis,String holidayPolicy,
            String calendarCode,BigDecimal principal,BigDecimal rate,BigDecimal margin) {}

    public static boolean supports(DraftRequest d) {
        return d!=null&&d.item()!=null&&"LOAN".equals(d.item().module())&&
            "LATE_DAMAGE".equals(d.item().operation())&&"LATE_DAMAGE".equals(d.item().amountKind());
    }
    private static IllegalArgumentException bad(String code){return new IllegalArgumentException(code);}
    private static String ref(Object raw,String name){
        if(!(raw instanceof String s)||!s.matches("[A-Za-z0-9_./:-]{3,100}"))throw bad(name+"_REFERENCE_REQUIRED");
        return s;
    }
    private static BigDecimal number(Object raw,String name,boolean positive,BigDecimal limit,int precision){
        if(raw==null||!String.valueOf(raw).matches("\\d{1,20}(?:\\.\\d{1,10})?"))throw bad(name+"_INVALID");
        BigDecimal value;
        try{value=new BigDecimal(String.valueOf(raw));}catch(NumberFormatException ex){throw bad(name+"_INVALID");}
        if((positive?value.signum()<=0:value.signum()<0)||value.compareTo(limit)>0||
            Math.max(0,value.stripTrailingZeros().scale())>precision)throw bad(name+"_OUT_OF_RANGE");
        return value;
    }
    private static LocalDate day(Object raw,String name){
        if(!(raw instanceof String s)||!s.matches("\\d{4}-\\d{2}-\\d{2}"))throw bad(name+"_ISO_REQUIRED");
        try {return LocalDate.parse(s);}catch(RuntimeException e){throw bad(name+"_INVALID_DATE");}
    }
    private static Map<?,?> policy(DraftRequest d){
        Object raw=d.version().uiContext()==null?null:d.version().uiContext().get("loanPolicy");
        if(!(raw instanceof Map<?,?> p))throw bad("LATE_LOAN_POLICY_REQUIRED");
        return p;
    }
    public static Context validate(Fee3F7DomainSimulationService.Request request) {
        if(request==null||!supports(request.draft())||request.inputs()==null)throw bad("LATE_DRAFT_REQUIRED");
        var d=request.draft();var v=d.version();
        if(v==null||d.rules()==null||d.rules().size()!=1||d.rules().get(0)==null)
            throw bad("LATE_SINGLE_RULE_REQUIRED");
        RuleDraft r=d.rules().get(0);
        if(!"FORMULA".equals(v.method())||!"FORMULA".equals(r.method())||
            !"REQUEST".equals(r.rateSource())||r.rate()!=null||r.fixed()!=null)
            throw bad("LATE_REQUEST_FORMULA_REQUIRED");
        Object rawExpression=r.parameters()==null?null:r.parameters().get("_fceEditableExpressionV1");
        String formula=rawExpression instanceof String s?s.replaceAll("\\s+",""):"";
        if(!FORMULA.equals(formula))throw bad("LATE_FORMULA_NOT_ALLOWLISTED");
        if(!"IRR".equals(v.serviceCurrency())||!"IRR".equals(v.feeCurrency()))throw bad("LATE_IRR_ONLY");
        LocalDate effective=request.effectiveOn();
        if(effective==null||v.validFrom()==null||effective.isBefore(v.validFrom())||
            v.validTo()!=null&&effective.isAfter(v.validTo()))throw bad("LATE_VERSION_NOT_EFFECTIVE");
        Map<?,?> p=policy(d);
        String basis=String.valueOf(p.get("dayCountBasis"));
        String holiday=String.valueOf(p.get("holidayPolicy"));
        if(!BASES.contains(basis))throw bad("LATE_DAY_COUNT_UNSUPPORTED");
        if(!POLICIES.contains(holiday))throw bad("LATE_HOLIDAY_POLICY_UNDECIDED");
        String cal="CHARGE_ALL_DAYS".equals(holiday)?"":ref(p.get("businessCalendarRef"),"CAL2_CALENDAR");
        var inputs=request.inputs();
        if(!inputs.keySet().equals(Set.of("amount","contractRate","penaltyMargin","contractReference",
                "rateEvidenceReference","penaltyPolicyReference","loanContractType","dueDate","penaltyUntilExclusive")))
            throw bad("LATE_UNREVIEWED_INPUT_FIELDS");
        ref(inputs.get("contractReference"),"CONTRACT");
        ref(inputs.get("rateEvidenceReference"),"RATE_EVIDENCE");
        ref(inputs.get("penaltyPolicyReference"),"PENALTY_POLICY");
        String contractType=ref(inputs.get("loanContractType"),"LOAN_TYPE");
        Object allowed=v.uiContext()==null?null:v.uiContext().get("loanContractTypes");
        if(!(allowed instanceof List<?> types)||types.isEmpty()||!types.contains(contractType))
            throw bad("LATE_CONTRACT_TYPE_NOT_ELIGIBLE");
        LocalDate due=day(inputs.get("dueDate"),"DUE_DATE");
        LocalDate end=day(inputs.get("penaltyUntilExclusive"),"PENALTY_END");
        if(!end.isAfter(due)||ChronoUnit.DAYS.between(due,end)>3653)
            throw bad("LATE_PERIOD_OUTSIDE_LIMIT");
        BigDecimal amount=number(inputs.get("amount"),"OVERDUE_AMOUNT",true,MAX,2);
        BigDecimal rate=number(inputs.get("contractRate"),"CONTRACT_RATE",false,new BigDecimal("1000"),8);
        BigDecimal margin=number(inputs.get("penaltyMargin"),"PENALTY_MARGIN",false,new BigDecimal("1000"),8);
        if(rate.add(margin).compareTo(new BigDecimal("1000"))>0)throw bad("TOTAL_PENALTY_RATE_OUT_OF_RANGE");
        if(r.min()!=null&&r.min().signum()<0||r.max()!=null&&r.max().signum()<0||
            r.min()!=null&&r.max()!=null&&r.min().compareTo(r.max())>0)throw bad("LATE_FEE_LIMIT_INVALID");
        return new Context(due,end,basis,holiday,cal,amount,rate,margin);
    }
    public static Fee3F7DomainSimulationService.Result calculate(Fee3F7DomainSimulationService.Request request,
            Context ctx,List<YearSlice> years,List<Workday> workdays){
        LocalDate effectiveDue=ctx.due();
        Map<LocalDate,Workday> workMap=new LinkedHashMap<>();
        if(!"CHARGE_ALL_DAYS".equals(ctx.holidayPolicy())){
            if(workdays==null||workdays.isEmpty())throw bad("CAL2_BUSINESS_CALENDAR_COVERAGE_MISSING");
            LocalDate expected=ctx.due();
            for(Workday row:workdays){
                if(row==null||row.date()==null||!row.date().equals(expected)||row.evidence()==null||row.evidence().isBlank())
                    throw bad("CAL2_BUSINESS_DAY_SEQUENCE_INVALID");
                workMap.put(row.date(),row);expected=expected.plusDays(1);
            }
            // Refuse a partially resolved calendar instead of treating missing days as holidays.
            if("EXCLUDE_ALL_HOLIDAYS".equals(ctx.holidayPolicy())&&
                    expected.isBefore(ctx.endExclusive()))
                throw bad("CAL2_BUSINESS_CALENDAR_COVERAGE_MISSING");
            if("ROLL_DUE_TO_WORKDAY".equals(ctx.holidayPolicy())&&!workMap.get(ctx.due()).business()){
                boolean found=false;
                for(int i=1;i<=45;i++){
                    LocalDate candidate=ctx.due().plusDays(i);
                    Workday day=workMap.get(candidate);
                    if(day==null)throw bad("CAL2_ROLL_FORWARD_COVERAGE_MISSING");
                    if(day.business()){effectiveDue=candidate;found=true;break;}
                }
                if(!found)throw bad("CAL2_NO_BUSINESS_DAY_WITHIN_45_DAYS");
            }
        }
        LocalDate start=effectiveDue.plusDays(1);
        if(start.isAfter(ctx.endExclusive()))start=ctx.endExclusive();
        if(start.isBefore(ctx.endExclusive())&&(years==null||years.isEmpty()))throw bad("CAL2_JALALI_YEAR_SLICES_MISSING");
        LocalDate cursor=start;BigDecimal yearFraction=BigDecimal.ZERO;long chargeable=0;
        List<Map<String,String>> trace=new ArrayList<>();
        if(years!=null)for(YearSlice s:years){
            if(s==null||!s.fromInclusive().equals(cursor)||s.days()!=ChronoUnit.DAYS.between(s.fromInclusive(),s.toExclusive())||
                s.daysInYear()!=365&&s.daysInYear()!=366||s.calendarDatasetEvidence()==null||s.calendarDatasetEvidence().isBlank())
                throw bad("CAL2_JALALI_SEGMENT_INVALID");
            long used=0;
            for(LocalDate date=s.fromInclusive();date.isBefore(s.toExclusive());date=date.plusDays(1)){
                boolean include="CHARGE_ALL_DAYS".equals(ctx.holidayPolicy())||
                    "ROLL_DUE_TO_WORKDAY".equals(ctx.holidayPolicy())||
                    (workMap.containsKey(date)&&workMap.get(date).business());
                if(include)used++;
            }
            chargeable+=used;
            int denominator=switch(ctx.dayBasis()){
                case "ACT_360"->360;case "ACT_365"->365;default->s.daysInYear();};
            BigDecimal part=BigDecimal.valueOf(used).divide(BigDecimal.valueOf(denominator),MC);
            yearFraction=yearFraction.add(part,MC);
            Map<String,String> row=new LinkedHashMap<>();
            row.put("kind","JALALI_YEAR_SLICE");row.put("jalaliYear",String.valueOf(s.jalaliYear()));
            row.put("calendarDays",String.valueOf(s.days()));row.put("chargeableDays",String.valueOf(used));
            row.put("denominator",String.valueOf(denominator));row.put("calendarDataset",s.calendarDatasetEvidence());
            row.put("yearFraction",part.toPlainString());trace.add(Map.copyOf(row));cursor=s.toExclusive();
        }
        if(!cursor.equals(ctx.endExclusive()))throw bad("CAL2_PERIOD_NOT_FULLY_COVERED");
        BigDecimal raw=ctx.principal().multiply(ctx.rate().add(ctx.margin()),MC)
            .divide(HUNDRED,MC).multiply(yearFraction,MC);
        if(raw.signum()<0||raw.compareTo(MAX)>0)throw bad("LATE_RESULT_OUT_OF_RANGE");
        RuleDraft rule=request.draft().rules().get(0);
        BigDecimal capped=raw;
        if(rule.min()!=null)capped=capped.max(rule.min());
        if(rule.max()!=null)capped=capped.min(rule.max());
        if(request.draft().version().rounding()==null)throw bad("LATE_ROUNDING_UNSUPPORTED");
        RoundingMode rounding=switch(request.draft().version().rounding()){
            case "HALF_UP"->RoundingMode.HALF_UP;case "HALF_EVEN"->RoundingMode.HALF_EVEN;
            case "DOWN","FLOOR"->RoundingMode.FLOOR;case "UP","CEIL"->RoundingMode.CEILING;
            default->throw bad("LATE_ROUNDING_UNSUPPORTED");};
        BigDecimal amount=capped.setScale(0,rounding);
        Map<String,String> summary=new LinkedHashMap<>();
        summary.put("kind","LATE_DAMAGE_CONTRACT_PERIOD");
        summary.put("dueDate",ctx.due().toString());summary.put("effectiveDueDate",effectiveDue.toString());
        summary.put("periodStartInclusive",start.toString());summary.put("periodEndExclusive",ctx.endExclusive().toString());
        summary.put("holidayPolicy",ctx.holidayPolicy());summary.put("calendarCode",ctx.calendarCode());
        summary.put("chargeableDays",String.valueOf(chargeable));
        summary.put("overdueIrr",ctx.principal().toPlainString());
        summary.put("contractRatePercent",ctx.rate().toPlainString());
        summary.put("penaltyMarginPercent",ctx.margin().toPlainString());
        summary.put("rawIrr",raw.toPlainString());summary.put("roundedIrr",amount.toPlainString());
        trace.add(Map.copyOf(summary));
        String signature;
        try{
            String data=CONTRACT+"|"+request.draft().item().operation()+"|"+request.effectiveOn()+"|"+
                new TreeMap<>(request.inputs())+"|"+trace+"|"+amount.toPlainString();
            signature=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(data.getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA256_UNAVAILABLE",ex);}
        return new Fee3F7DomainSimulationService.Result("PRETAX_ONLY","LOAN","LATE_DAMAGE",CONTRACT,
            raw.toPlainString(),amount.toPlainString(),null,null,List.copyOf(trace),
            List.of("CAL2_READONLY_CALENDAR_EVIDENCE","CLIENT_CONTRACT_AND_RATE_NOT_VERIFIED",
                "PENALTY_RULE_REQUIRES_BANK_LEGAL_APPROVAL","NO_TAX_NO_POSTING_NO_ORACLE_WRITE"),
            signature,false,false,false,false);
    }
    public static Fee3F7DomainSimulationService.Result blocked(String code){
        return new Fee3F7DomainSimulationService.Result("BLOCKED","LOAN","LATE_DAMAGE",CONTRACT,
            null,null,null,null,List.of(),List.of(code),null,false,false,false,false);
    }
}
