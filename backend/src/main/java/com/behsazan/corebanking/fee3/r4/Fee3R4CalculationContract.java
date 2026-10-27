package com.behsazan.corebanking.fee3.r4;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.r3.Fee3PreviewEngine;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** R4-A: precision, effective date, provenance and immutable simulation evidence. Never posts. */
public final class Fee3R4CalculationContract {
    private Fee3R4CalculationContract() {}
    public static final String CONTRACT = "FEE3_CALCULATION_CONTRACT_R4A_V1";
    private static final BigDecimal MAX = new BigDecimal("1000000000000000000");

    public record Request(DraftRequest draft, Map<String,Object> inputs, LocalDate effectiveOn) {}
    public record VersionReference(String provenance, Long calcItemId, Long tariffVersionId,
                                   Long itemRecordVersion, Long tariffRecordVersion,
                                   String status, boolean active, int versionNumber) {}
    public record Evidence(String contractVersion, String status, boolean persisted,
                           boolean financialPosting, boolean published, boolean approved,
                           String simulationMode, VersionReference versionReference,
                           LocalDate oracleBusinessDate, LocalDate effectiveOn,
                           String currency, String roundingCode, int roundingScale,
                           String snapshotSha256, Fee3PreviewEngine.PreviewResult calculation,
                           List<String> diagnostics) {}

    public static String guard(Request request, LocalDate bankDay) {
        if(bankDay==null)return "ORACLE_BANK_DATE_REQUIRED";
        if(request==null||request.draft()==null||request.draft().item()==null||request.draft().version()==null)
            return "DRAFT_REQUIRED";
        var draft=request.draft(); var version=draft.version();var item=draft.item();
        LocalDate effective=request.effectiveOn()==null?bankDay:request.effectiveOn();
        if(effective.isBefore(bankDay))return "HISTORICAL_SIMULATION_NOT_IN_R4A";
        if(version.validFrom()==null||effective.isBefore(version.validFrom()) ||
           version.validTo()!=null&&effective.isAfter(version.validTo()))return "OUTSIDE_TARIFF_VALIDITY";
        if(version.validTo()!=null&&version.validFrom()!=null&&version.validTo().isBefore(version.validFrom()))
            return "INVALID_TARIFF_DATES";
        if(!"SERVICE_FEE".equals(item.amountKind()))return "SPECIALIZED_METHOD_NOT_IN_R4A";
        if(!Set.of("FIXED","PERCENT","MIXED").contains(version.method()))
            return "METHOD_NOT_CERTIFIED_FOR_R4A";
        if(!"IRR".equals(version.feeCurrency())||!"IRR".equals(version.serviceCurrency()))
            return "NON_IRR_CONTRACT_UNSUPPORTED";
        if(!Set.of("HALF_UP","HALF_EVEN","FLOOR","DOWN","UP","CEIL").contains(version.rounding()))
            return "ROUNDING_POLICY_UNSUPPORTED";
        if(draft.rules()==null||draft.rules().size()!=1||draft.rules().get(0)==null)
            return "EXACTLY_ONE_RULE_REQUIRED";
        var rule=draft.rules().get(0);
        if(!version.method().equals(rule.method()))return "METHOD_MISMATCH";
        if(!Set.of("RULE","REQUEST").contains(rule.rateSource()))return "INVALID_RATE_SOURCE";
        if("FIXED".equals(version.method()) && rule.rate()!=null)
            return "FIXED_METHOD_MUST_NOT_HAVE_RATE";
        if("PERCENT".equals(version.method()) && rule.fixed()!=null)
            return "PERCENT_METHOD_MUST_NOT_HAVE_FIXED";
        if("MIXED".equals(version.method()) && rule.fixed()==null)
            return "MIXED_FIXED_REQUIRED";
        if(rule.formulaCode()!=null&&!rule.formulaCode().isBlank()||
           rule.parameters()!=null&&rule.parameters().containsKey("_fceEditableExpressionV1"))
            return "FORMULA_NOT_CERTIFIED_FOR_R4A";
        if(!valid(rule.fixed(),2)||!valid(rule.min(),2)||!valid(rule.max(),2))
            return "IRR_MONEY_EXCEEDS_NUMBER_22_2";
        if(!valid(rule.rate(),8))return "RATE_EXCEEDS_NUMBER_18_8";
        if(!valid(rule.coverage(),4))return "COVERAGE_EXCEEDS_NUMBER_12_4";
        Map<String,Object> inputs=request.inputs()==null?Map.of():request.inputs();
        if(!Set.of("amount","contractRate").containsAll(inputs.keySet()))return "UNREVIEWED_INPUT_VARIABLE";
        if("FIXED".equals(version.method())&&inputs.containsKey("contractRate"))
            return "FIXED_METHOD_MUST_NOT_HAVE_CONTRACT_RATE";
        if(!"FIXED".equals(version.method())&&"RULE".equals(rule.rateSource())&&
           inputs.containsKey("contractRate"))return "UNUSED_CONTRACT_RATE_FOR_RULE";
        if(!validNumber(inputs.get("amount"),2))return "AMOUNT_EXCEEDS_IRR_PRECISION";
        if(!validNumber(inputs.get("contractRate"),8))return "CONTRACT_RATE_EXCEEDS_PRECISION";
        Map<String,Object> policy=version.financialPolicy()==null?Map.of():version.financialPolicy();
        if(!validNumber(policy.get("taxPercent"),8))return "TAX_PERCENT_EXCEEDS_PRECISION";
        if(!"TAXABLE".equals(version.taxMode())&&policy.get("taxPercent")!=null)
            return "UNUSED_TAX_PERCENT_FOR_NON_TAXABLE";
        if(draft.calcItemId()==null ^ draft.tariffVersionId()==null)
            return "PARTIAL_VERSION_REFERENCE_FORBIDDEN";
        if(draft.calcItemId()==null && (draft.expectedItemRecordVersion()!=null||
           draft.expectedVersionRecordVersion()!=null))return "UNPERSISTED_DRAFT_WITH_RECORD_VERSION";
        if(draft.calcItemId()!=null && (draft.calcItemId()<=0 || draft.tariffVersionId()<=0 ||
           draft.expectedItemRecordVersion()==null || draft.expectedItemRecordVersion()<=0 ||
           draft.expectedVersionRecordVersion()==null || draft.expectedVersionRecordVersion()<=0))
            return "VERSION_REFERENCE_INCOMPLETE";
        return null;
    }
    private static boolean valid(BigDecimal n,int scale){
        return n==null || n.signum()>=0 && n.compareTo(MAX)<=0 &&
               Math.max(0,n.stripTrailingZeros().scale())<=scale;
    }
    private static boolean validNumber(Object value,int scale){
        if(value==null)return true;
        if(!(value instanceof Number)&&!(value instanceof String))return false;
        try{return valid(new BigDecimal(String.valueOf(value)),scale);}
        catch(NumberFormatException ex){return false;}
    }
    public static Evidence blocked(Request request,LocalDate bankDay,String reason){
        var v=request!=null&&request.draft()!=null?request.draft().version():null;
        LocalDate date=request==null||request.effectiveOn()==null?bankDay:request.effectiveOn();
        return new Evidence(CONTRACT,"BLOCKED",false,false,false,false,"UAT_DRAFT_SIMULATION",
            null,bankDay,date,"IRR",v==null?null:v.rounding(),0,null,null,List.of(reason));
    }
    public static Evidence finish(Request request,LocalDate bankDay,VersionReference source,
                                  Fee3PreviewEngine.PreviewResult calculated){
        if(calculated==null||!"SIMULATED".equals(calculated.status())||
           calculated.financialPosting()||calculated.persisted()||calculated.feeIrr()==null||
           calculated.rawIrr()==null||calculated.taxIrr()==null||calculated.totalIrr()==null||
           Math.max(0,calculated.feeIrr().stripTrailingZeros().scale())>0||
           Math.max(0,calculated.totalIrr().stripTrailingZeros().scale())>0||
           source==null||source.active()||
           !("UNSAVED".equals(source.status())||"DRAFT".equals(source.status()))){
            String code=calculated==null?"CALCULATION_NULL":
                calculated.diagnostics().isEmpty()?"CALCULATION_BLOCKED":calculated.diagnostics().get(0);
            return blocked(request,bankDay,code);
        }
        var v=request.draft().version();
        LocalDate date=request.effectiveOn()==null?bankDay:request.effectiveOn();
        return new Evidence(CONTRACT,"SIMULATED",false,false,false,false,
            date.isAfter(bankDay)?"FUTURE_DATED_UAT_SIMULATION":"UAT_DRAFT_SIMULATION",
            source,bankDay,date,"IRR",v.rounding(),0,
            fingerprint(request,source,date),calculated,
            List.of("NOT_A_PUBLISHED_TARIFF","NOT_A_CHARGE_AUTHORIZATION",
                    "NOT_A_TAX_DETERMINATION","NO_ORACLE_WRITE","NO_ACCOUNTING_POSTING"));
    }

    /** Stable across different map insertion orders; hashes opaque request, not personal/financial payload. */
    public static String fingerprint(Request request,VersionReference reference){
        return fingerprint(request,reference,request==null?null:request.effectiveOn());
    }
    public static String fingerprint(Request request,VersionReference reference,LocalDate resolvedEffectiveOn){
        StringBuilder data=new StringBuilder(1024);
        append(data,CONTRACT,0);append(data,request,0);append(data,reference,0);
        append(data,resolvedEffectiveOn,0);
        if(data.length()>500_000)throw new IllegalArgumentException("SNAPSHOT_TOO_LARGE");
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(data.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA-256 unavailable",ex);}
    }
    private static void append(StringBuilder sb,Object o,int depth){
        if(depth>25||sb.length()>500_000)throw new IllegalArgumentException("SNAPSHOT_TOO_LARGE");
        if(o==null){sb.append("n;");return;}
        if(o instanceof Number n){
            try{sb.append('d').append(new BigDecimal(String.valueOf(n)).stripTrailingZeros().toPlainString()).append(';');}
            catch(NumberFormatException ex){throw new IllegalArgumentException("INVALID_NUMERIC_SNAPSHOT");}
        } else if(o instanceof CharSequence s){sb.append('s').append(s.length()).append(':').append(s).append(';');}
        else if(o instanceof Boolean b){sb.append(b?"true;":"false;");}
        else if(o instanceof LocalDate d){sb.append('t').append(d).append(';');}
        else if(o instanceof Map<?,?> m){
            if(m.size()>500)throw new IllegalArgumentException("SNAPSHOT_TOO_LARGE");
            sb.append("m{");
            List<String> keys=new ArrayList<>();
            for(Object k:m.keySet()){
                if(!(k instanceof String s))throw new IllegalArgumentException("NON_STRING_SNAPSHOT_KEY");
                keys.add(s);
            }
            keys.sort(Comparator.naturalOrder());
            for(String k:keys){append(sb,k,depth+1);append(sb,m.get(k),depth+1);}
            sb.append("};");
        } else if(o instanceof List<?> l){
            if(l.size()>500)throw new IllegalArgumentException("SNAPSHOT_TOO_LARGE");
            sb.append("l[");for(Object e:l)append(sb,e,depth+1);sb.append("];");
        } else if(o.getClass().isRecord()){
            sb.append("r:").append(o.getClass().getName()).append('{');
            for(RecordComponent c:o.getClass().getRecordComponents()){
                append(sb,c.getName(),depth+1);
                try{append(sb,c.getAccessor().invoke(o),depth+1);}
                catch(ReflectiveOperationException ex){throw new IllegalArgumentException("SNAPSHOT_REFLECTION_ERROR",ex);}
            }
            sb.append("};");
        } else throw new IllegalArgumentException("UNSUPPORTED_SNAPSHOT_TYPE");
    }
}
