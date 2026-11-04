import com.behsazan.corebanking.collateral.reporting.CollateralReportingPolicy;
import java.math.BigDecimal;

/** Pure Java policy checks, not a substitute for Spring/Oracle/runtime tests. */
public class CollateralReportingPolicySmoke {
    static int tests=0;
    static void ok(boolean value){if(!value)throw new AssertionError("FAILED_AT:"+(tests+1));tests++;}
    static void fails(Runnable runnable){
        boolean threw=false;
        try{runnable.run();}catch(IllegalArgumentException e){threw=true;}
        ok(threw);
    }
    public static void main(String[] ignored){
        for(int i=1;i<=50;i++)ok(CollateralReportingPolicy.eventLimit(i)==i);
        for(int n:new int[]{-1,0,51,999})fails(()->CollateralReportingPolicy.eventLimit(n));
        for(String amount:new String[]{"0", "1", "42", "100000000000", "0.0001", "42.5000", "9999999999999999.9999", "19.01"})
            ok(CollateralReportingPolicy.amount(new BigDecimal(amount)).equals(amount));
        fails(()->CollateralReportingPolicy.amount(null));
        for(Long n:new Long[]{0L,1L,Long.MAX_VALUE})ok(CollateralReportingPolicy.count(n)==n);
        fails(()->CollateralReportingPolicy.count(null));
        fails(()->CollateralReportingPolicy.count(-1L));
        ok(CollateralReportingPolicy.hasTotals(true,true));
        ok(!CollateralReportingPolicy.hasTotals(true,false));
        ok(!CollateralReportingPolicy.hasTotals(false,true));
        ok(!CollateralReportingPolicy.hasTotals(false,false));
        if(tests!=72)throw new AssertionError("EXPECTED_72_ACTUAL_"+tests);
        System.out.println("CLT_R6B_REPORT_POLICY_SMOKE_PASS="+tests+"; ORACLE_EXECUTION=NONE");
    }
}
