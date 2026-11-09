package com.behsazan.corebanking.collateral.introduction;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.SerialRangeInput;
import java.util.List;
public class PolicySmoke {
    private static int success=0;
    static void ok(boolean v,String id){if(!v)throw new AssertionError(id);success++;}
    static void invalid(String code,Runnable call){try{call.run();throw new AssertionError("accepted "+code);}
        catch(IllegalArgumentException ex){ok(code.equals(ex.getMessage()),code);}}
    public static void main(String[] args){
        var independent=CollateralCountabilityPolicy.evaluate("N","Y");
        ok(independent.configured()&&!independent.countable()&&independent.hasMultiplicity(),"G12 independent N/Y");
        var inverse=CollateralCountabilityPolicy.evaluate("Y","N");
        ok(inverse.configured()&&inverse.countable()&&!inverse.hasMultiplicity(),"G12 independent Y/N");
        var missing=CollateralCountabilityPolicy.evaluate(null,"Y");
        ok(!missing.configured()&&!missing.countable(),"G12 missing value is not guessed");
        invalid("G12_COUNTABLE_FLAG_INVALID",()->CollateralCountabilityPolicy.evaluate("1","N"));
        invalid("G12_MULTIPLICITY_FLAG_INVALID",()->CollateralCountabilityPolicy.evaluate("Y","1"));
        var rows=List.of(new SerialRangeInput("0001","0003","100"),
                         new SerialRangeInput("5","5","200"));
        var a=CollateralSerialRangePolicy.assess(true,"IRR","300",rows);
        ok(a.itemCount()==4&&a.totalAmountIRR().equals("300"),"G03 inclusive count and IRR total");
        invalid("CLT_R2C_SERIAL_ROW_OVER_20",()->CollateralSerialRangePolicy.normalize(
             List.of(new SerialRangeInput("1","21","100"))));
        invalid("CLT_R2C_SERIAL_RANGE_OVERLAP",()->CollateralSerialRangePolicy.normalize(
             List.of(new SerialRangeInput("0001","0002","100"),new SerialRangeInput("2","3","100"))));
        invalid("CLT_R2C_TYPE_NOT_COUNTABLE",()->CollateralSerialRangePolicy.assess(false,"IRR","300",rows));
        invalid("CLT_R2C_ITEM_AMOUNT_SUM_MISMATCH",()->CollateralSerialRangePolicy.assess(true,"IRR","301",rows));
        ok(CollateralSerialRangePolicy.normalize(List.of(new SerialRangeInput(
            "9".repeat(99)+"1","9".repeat(99)+"2","100"))).size()==1,
            "G03 exact 100-digit serials without database NUMBER conversion");
        System.out.println("CLT_R2_G03_G12_POLICY_PASS="+success+"/"+success);
    }
}
