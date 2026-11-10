import com.behsazan.corebanking.collateral.monitoring.CollateralCoverageMonitoringPolicy;
import com.behsazan.corebanking.collateral.monitoring.CollateralCoverageMonitoringPolicy.Snapshot;
import java.math.BigDecimal;

/** Executable Java 21 BUC-14 arithmetic and fail-closed scenario coverage. */
public final class CoverageMonitoringSmoke {
    static int passed;
    static void ok(boolean condition,String name){if(!condition)throw new AssertionError(name);passed++;}
    static void rejects(Runnable r,String name){try{r.run();throw new AssertionError(name);}catch(IllegalArgumentException expected){passed++;}}
    static Snapshot sample(String status,String currency,String valuationCurrency,String start,String end,
                           String eligible,String allocated,long foreign,long count){
        return new Snapshot("42","999001","ACTIVE",currency,"25",status,valuationCurrency,start,end,
                "2026-11-10",eligible==null?null:new BigDecimal(eligible),new BigDecimal(allocated),foreign,count);
    }
    public static void main(String[] args){
        var low=CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-12-31","80.5000","100.2500",0,2));
        ok("RECORDED_SHORTFALL".equals(low.riskStatus()),"shortfall type");
        ok("19.7500".equals(low.recordedShortfall()),"exact decimal no rounding");
        ok(!low.newAllocationAuthorized(),"no allocation authorization");
        ok(low.activeAllocationCount()==2,"allocation count");
        ok(low.reasons().contains("BANK_APPROVED_FACTORS_AND_OBLIGATION_COVERAGE_NOT_ATTESTED"),"factor caveat");
        var adequate=CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-12-31","120","100",0,1));
        ok("NO_RECORDED_SHORTFALL_DETECTED".equals(adequate.riskStatus()),"no recorded deficit");
        ok(adequate.recordedShortfall()==null,"no false shortage zero");
        ok(!adequate.newAllocationAuthorized(),"not a certificate");
        var expired=CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-09-30","80","100",0,1));
        ok("VALUATION_REVIEW_REQUIRED".equals(expired.riskStatus()),"expired not numerically certified");
        ok(expired.recordedShortfall()==null,"expired raw comparison withheld");
        var nonfinal=CollateralCoverageMonitoringPolicy.evaluate(sample("DRAFT","IRR","IRR","2026-01-01","2026-12-31","80","100",0,1));
        ok("VALUATION_REVIEW_REQUIRED".equals(nonfinal.riskStatus()),"draft not eligible");
        var missing=CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-12-31",null,"100",0,1));
        ok("VALUATION_REVIEW_REQUIRED".equals(missing.riskStatus()),"missing eligible unknown");
        var fx=CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","USD","2026-01-01","2026-12-31","80","100",0,1));
        ok("VALUATION_REVIEW_REQUIRED".equals(fx.riskStatus()),"valuation FX not guessed");
        var foreign=CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-12-31","120","100",1,2));
        ok("FX_REVIEW_REQUIRED".equals(foreign.riskStatus()),"foreign currency present");
        ok(foreign.recordedShortfall()==null,"unknown FX not combined");
        var foreignShort=CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-12-31","80","100",1,2));
        ok("RECORDED_SHORTFALL".equals(foreignShort.riskStatus()),"deficit already from same currency");
        ok(foreignShort.reasons().contains("ACTIVE_ALLOCATIONS_IN_OTHER_CURRENCIES_FX_NOT_VERIFIED"),"foreign still flagged");
        var exact=CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-11-10","2026-11-10","100","100",0,1));
        ok("NO_RECORDED_SHORTFALL_DETECTED".equals(exact.riskStatus()),"inclusive dates");
        var early=CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-11-11","2026-12-31","100","100",0,1));
        ok("VALUATION_REVIEW_REQUIRED".equals(early.riskStatus()),"future value blocked");
        rejects(()->CollateralCoverageMonitoringPolicy.size(0),"size low");
        rejects(()->CollateralCoverageMonitoringPolicy.size(101),"size high");
        rejects(()->CollateralCoverageMonitoringPolicy.page(-1),"page negative");
        rejects(()->CollateralCoverageMonitoringPolicy.page(100001),"page high");
        ok(CollateralCoverageMonitoringPolicy.size(100)==100,"size max");
        ok(CollateralCoverageMonitoringPolicy.page(0)==0,"page min");
        rejects(()->CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-12-31","10","-1",0,1)),"negative exposure");
        rejects(()->CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-12-31","-10","1",0,1)),"negative eligible");
        rejects(()->CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-12-31","10","1",2,1)),"foreign exceeds count");
        rejects(()->CollateralCoverageMonitoringPolicy.evaluate(sample("FINAL","IRR","IRR","2026-01-01","2026-12-31","10","1",0,0)),"no allocations");
        System.out.println("BUC14_COVERAGE_JAVA21_SMOKE_PASS="+passed+"/"+passed);
    }
}
