package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.RuleDraft;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stateless, bounded decimal interpreter for the FCE editable FORMULA analyst preview.
 * It never executes Java/JavaScript expressions or resolves bank tariffs. Published
 * F2 allowlisted formula contracts remain separate and are NOT certified by this class.
 */
public final class Fee3F6FormulaArithmetic {
    private Fee3F6FormulaArithmetic() {}
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal MAX = new BigDecimal("1000000000000000000");
    private static final BigDecimal INTERMEDIATE_MAX = new BigDecimal("1000000000000000000000000");
    private static final BigDecimal MAX_MONTHS = new BigDecimal("1200");
    private static final Set<String> BUILTIN = Set.of("amount","rate","months","days","usage","fixed","period");
    private static final Set<String> FUNCTIONS = Set.of("min","max","abs","floor","ceil","round","sqrt","pow","pmt","ipmt");

    public static BigDecimal evaluate(RuleDraft rule, Map<String,Object> inputs,
                                      List<Map<String,Object>> trace) {
        if(rule==null || rule.parameters()==null)throw bad("FORMULA_PARAMETERS_REQUIRED");
        if(rule.formulaCode()!=null && !rule.formulaCode().isBlank())
            throw bad("FORMULA_REFERENCE_REQUIRES_F2_ALLOWLIST_CONTRACT");
        Object expression = rule.parameters().get("_fceEditableExpressionV1");
        if(!(expression instanceof String source) || source.isBlank())
            throw bad("FORMULA_EDITABLE_EXPRESSION_REQUIRED");
        if(source.length()>2000)throw bad("FORMULA_TOO_LONG");
        // A formula is an arithmetic UAT calculation, never a client-supplied policy override.
        Map<String,Object> original = config(rule);
        Object paramDefs=original.get("formulaParams");
        Set<String> allowed=new HashSet<>(BUILTIN);
        if(paramDefs!=null){
            if(!(paramDefs instanceof List<?> params) || params.size()>30)
                throw bad("FORMULA_PARAMETER_SCHEMA_INVALID");
            for(Object obj:params){
                if(!(obj instanceof Map<?,?> param) || !(param.get("name") instanceof String name) ||
                    !name.matches("[A-Za-z_][A-Za-z0-9_]{0,63}") || BUILTIN.contains(name) ||
                    FUNCTIONS.contains(name) || !Set.of("NUMBER","MONEY").contains(param.get("type")) ||
                    !(param.get("label") instanceof String label) || label.isBlank() ||
                    !allowed.add(name))throw bad("FORMULA_PARAMETER_SCHEMA_INVALID");
            }
        }
        Map<String,BigDecimal> vars=new HashMap<>();
        if(inputs!=null)for(var entry:inputs.entrySet()){
            if(!allowed.contains(entry.getKey()))continue;
            BigDecimal val=number(entry.getValue(),"FORMULA_INPUT_INVALID");
            if(val.signum()<0 || val.compareTo(MAX)>0)throw bad("FORMULA_INPUT_OUT_OF_RANGE");
            if("rate".equals(entry.getKey()) && val.compareTo(new BigDecimal("1000"))>0)
                throw bad("FORMULA_RATE_OUT_OF_RANGE");
            vars.put(entry.getKey(),val);
        }
        if("RULE".equals(rule.rateSource()) && rule.rate()!=null)vars.put("rate",nonnegative(rule.rate()));
        if(rule.fixed()!=null)vars.put("fixed",nonnegative(rule.fixed()));
        BigDecimal result=new Parser(source,vars,allowed).parse();
        if(result.signum()<0 || result.compareTo(MAX)>0)throw bad("FORMULA_RESULT_OUT_OF_RANGE");
        trace.add(Map.of("kind","FORMULA_DECIMAL","expression",source,"rawIrr",result,
            "evaluation","BOUNDED_DECIMAL_AST","authorized",false));
        return result;
    }
    private static Map<String,Object> config(RuleDraft rule){
        Object raw=rule.parameters().get("originalConfig");
        if(raw==null)return Map.of();
        if(!(raw instanceof Map<?,?> map))throw bad("FORMULA_CONFIG_INVALID");
        Map<String,Object> result=new HashMap<>();
        for(var e:map.entrySet())if(e.getKey() instanceof String s)result.put(s,e.getValue());
        return result;
    }
    private static BigDecimal nonnegative(BigDecimal n){
        if(n.signum()<0 || n.compareTo(MAX)>0)throw bad("FORMULA_INPUT_OUT_OF_RANGE");
        return n;
    }
    private static BigDecimal number(Object value,String code){
        if(!(value instanceof Number || value instanceof String) || value.toString().length()>48)throw bad(code);
        try {
            BigDecimal n=new BigDecimal(value.toString());
            if(n.precision()>38 || Math.abs((long)n.scale())>30)throw bad(code);
            return n;
        }catch(NumberFormatException ex){throw bad(code);}
    }
    private static BigDecimal bounded(BigDecimal n){
        if(n==null || n.abs().compareTo(INTERMEDIATE_MAX)>0 || Math.abs((long)n.scale())>80)
            throw bad("FORMULA_INTERMEDIATE_OUT_OF_RANGE");
        return n;
    }
    private static IllegalArgumentException bad(String code){return new IllegalArgumentException(code);}
    private static int positiveInteger(BigDecimal n,String error,int max){
        try {int v=n.intValueExact();if(v<1 || v>max)throw bad(error);return v;}
        catch(ArithmeticException e){throw bad(error);}
    }
    private static BigDecimal pmt(BigDecimal amount,BigDecimal annualRate,BigDecimal months){
        int n=positiveInteger(months,"FORMULA_MONTHS_INVALID",1200);
        if(amount.signum()<0 || amount.compareTo(MAX)>0 || annualRate.signum()<0 ||
            annualRate.compareTo(new BigDecimal("1000"))>0)throw bad("FORMULA_LOAN_INPUT_OUT_OF_RANGE");
        if(amount.signum()==0)return BigDecimal.ZERO;
        BigDecimal r=annualRate.divide(new BigDecimal("1200"),MC);
        if(r.signum()==0)return amount.divide(BigDecimal.valueOf(n),MC);
        BigDecimal factor=BigDecimal.ONE.add(r,MC).pow(n,MC);
        return bounded(amount.multiply(r,MC).multiply(factor,MC).divide(factor.subtract(BigDecimal.ONE),MC));
    }
    private static BigDecimal ipmt(BigDecimal amount,BigDecimal annualRate,BigDecimal months,BigDecimal period){
        int n=positiveInteger(months,"FORMULA_MONTHS_INVALID",1200);
        int p=positiveInteger(period,"FORMULA_PERIOD_INVALID",n);
        BigDecimal r=annualRate.divide(new BigDecimal("1200"),MC);
        if(r.signum()==0)return BigDecimal.ZERO;
        BigDecimal factor=BigDecimal.ONE.add(r,MC).pow(n-p+1,MC);
        return bounded(pmt(amount,annualRate,months).multiply(
            BigDecimal.ONE.subtract(BigDecimal.ONE.divide(factor,MC)),MC));
    }
    private static BigDecimal fn(String name,List<BigDecimal> args){
        switch(name){
            case "min","max" -> {
                if(args.isEmpty() || args.size()>20)throw bad("FORMULA_FUNCTION_ARITY");
                BigDecimal val=args.get(0);
                for(BigDecimal a:args)val="min".equals(name)?val.min(a):val.max(a);
                return val;
            }
            case "abs","floor","ceil","round","sqrt" -> {
                if(args.size()!=1)throw bad("FORMULA_FUNCTION_ARITY");
                BigDecimal n=args.get(0);
                return switch(name){
                    case "abs" -> n.abs();
                    case "floor" -> n.setScale(0,RoundingMode.FLOOR);
                    case "ceil" -> n.setScale(0,RoundingMode.CEILING);
                    case "round" -> n.setScale(0,RoundingMode.HALF_UP);
                    default -> {
                        if(n.signum()<0)throw bad("FORMULA_SQRT_NEGATIVE");
                        yield n.sqrt(MC);
                    }
                };
            }
            case "pow" -> {
                if(args.size()!=2)throw bad("FORMULA_FUNCTION_ARITY");
                return power(args.get(0),args.get(1));
            }
            case "pmt" -> {
                if(args.size()!=3)throw bad("FORMULA_FUNCTION_ARITY");
                return pmt(args.get(0),args.get(1),args.get(2));
            }
            case "ipmt" -> {
                if(args.size()!=4)throw bad("FORMULA_FUNCTION_ARITY");
                return ipmt(args.get(0),args.get(1),args.get(2),args.get(3));
            }
            default -> throw bad("FORMULA_FUNCTION_NOT_ALLOWED");
        }
    }
    private static BigDecimal power(BigDecimal base,BigDecimal exponent){
        int n;
        try {n=exponent.intValueExact();}catch(ArithmeticException ex){throw bad("FORMULA_FRACTIONAL_POWER_UNSUPPORTED");}
        if(Math.abs(n)>1200)throw bad("FORMULA_POWER_TOO_LARGE");
        if(base.abs().compareTo(new BigDecimal("1000000000"))>0 && Math.abs(n)>10)
            throw bad("FORMULA_POWER_TOO_LARGE");
        if(n>=0)return bounded(base.pow(n,MC));
        if(base.signum()==0)throw bad("FORMULA_DIVIDE_BY_ZERO");
        return bounded(BigDecimal.ONE.divide(base.pow(-n,MC),MC));
    }

    private static final class Parser {
        final String src;final Map<String,BigDecimal> inputs;final Set<String> allowed;
        final List<String> tokens=new ArrayList<>();int pos,visits,depth;
        Parser(String s,Map<String,BigDecimal> vars,Set<String> allowed){
            src=s;inputs=vars;this.allowed=allowed;
            java.util.regex.Pattern re=java.util.regex.Pattern.compile(
                "\\s*([A-Za-z_][A-Za-z0-9_]*|(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?|[+*\\/^%(),-])\\s*");
            for(int i=0;i<src.length();){
                var matcher=re.matcher(src);
                matcher.region(i,src.length());
                if(!matcher.lookingAt())throw bad("FORMULA_INVALID_CHARACTER");
                tokens.add(matcher.group(1));i=matcher.end();
                if(tokens.size()>180)throw bad("FORMULA_TOKEN_LIMIT");
            }
        }
        BigDecimal parse(){
            if(tokens.isEmpty())throw bad("FORMULA_EDITABLE_EXPRESSION_REQUIRED");
            BigDecimal value=add();
            if(pos!=tokens.size())throw bad("FORMULA_TRAILING_TOKEN");
            return bounded(value);
        }
        boolean has(String token){return pos<tokens.size()&&token.equals(tokens.get(pos));}
        String take(){if(pos==tokens.size())throw bad("FORMULA_INCOMPLETE");return tokens.get(pos++);}
        void require(String token){if(!has(token))throw bad("FORMULA_EXPECTED_DELIMITER");pos++;}
        void check(){if(++visits>450)throw bad("FORMULA_TOO_COMPLEX");}
        BigDecimal primary(){
            check();if(++depth>70)throw bad("FORMULA_DEPTH_LIMIT");
            try {
                if(has("+")){take();return primary();}
                if(has("-")){take();return bounded(primary().negate());}
                if(has("(")){take();BigDecimal n=add();require(")");return n;}
                String key=take();
                if(key.matches("(?:[0-9].*|\\.[0-9].*)"))return bounded(number(key,"FORMULA_NUMBER_INVALID"));
                if(!key.matches("[A-Za-z_][A-Za-z0-9_]*"))throw bad("FORMULA_IDENTIFIER_INVALID");
                if(has("(")){
                    if(!FUNCTIONS.contains(key))throw bad("FORMULA_FUNCTION_NOT_ALLOWED");
                    take();List<BigDecimal> args=new ArrayList<>();
                    if(!has(")")){args.add(add());while(has(",")){take();args.add(add());
                        if(args.size()>20)throw bad("FORMULA_FUNCTION_ARITY");}}
                    require(")");return bounded(fn(key,args));
                }
                if(!allowed.contains(key))throw bad("FORMULA_VARIABLE_NOT_DECLARED");
                BigDecimal value=inputs.get(key);
                if(value==null)throw bad("FORMULA_VARIABLE_MISSING_"+key.toUpperCase());
                return value;
            } finally {depth--;}
        }
        BigDecimal power(){
            BigDecimal left=primary();
            if(has("^")){take();return power(left,power());}
            return left;
        }
        BigDecimal power(BigDecimal x,BigDecimal y){return Fee3F6FormulaArithmetic.power(x,y);}
        BigDecimal multiply(){
            BigDecimal result=power();
            while(has("*")||has("/")||has("%")){
                String op=take();BigDecimal b=power();
                result=switch(op){
                    case "*" -> bounded(result.multiply(b,MC));
                    case "/" -> {
                        if(b.signum()==0)throw bad("FORMULA_DIVIDE_BY_ZERO");
                        yield bounded(result.divide(b,MC));
                    }
                    default -> {
                        if(b.signum()==0)throw bad("FORMULA_DIVIDE_BY_ZERO");
                        yield bounded(result.remainder(b,MC));
                    }
                };
            }
            return result;
        }
        BigDecimal add(){
            BigDecimal result=multiply();
            while(has("+")||has("-")){
                String op=take();BigDecimal b=multiply();
                result=bounded("+".equals(op)?result.add(b,MC):result.subtract(b,MC));
            }
            return result;
        }
    }
}
