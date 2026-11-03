import com.behsazan.corebanking.collateral.capacity.CollateralCapacityPolicy;
import com.behsazan.corebanking.collateral.capacity.CollateralCapacityModels.AllocationBucket;
import java.math.BigDecimal;
import java.util.List;

/** Standalone policy smoke compiled with the ACTUAL production Policy + Models. */
public final class CapacityPolicySmoke {
    private static int passed;
    static void test(boolean ok, String text) {
        if (!ok) throw new AssertionError(text);
        ++passed;
    }
    static void rejects(Runnable fn, String message) {
        try {fn.run();throw new AssertionError("Expected rejection:"+message);}
        catch (IllegalArgumentException expected) {passed++;}
    }
    static CollateralCapacityPolicy.Input scenario(String code, String state, String to,
            BigDecimal eligible, List<AllocationBucket> a, String currency) {
        return new CollateralCapacityPolicy.Input(code, true, true, false, false, false,
                "IRR", "101", state, currency, "2026-01-01", to,
                "2026-11-03", "2025-01-01", null, eligible, a);
    }
    static AllocationBucket allocation(String currency, String sum, int count) {
        return new AllocationBucket("1",currency,count,sum);
    }
    public static void main(String[] args) {
        var normal = CollateralCapacityPolicy.calculate(scenario("ACTIVE", "FINAL", "2026-12-31",
                new BigDecimal("100000000"),List.of(allocation("IRR","60000000",3)),"IRR"));
        test("60000000".equals(normal.sameCurrencyAllocated()),"active allocation sum");
        test("40000000".equals(normal.residual()),"raw residual");
        test("0".equals(normal.shortfall()),"no raw shortfall");
        test(normal.activeCount()==3,"active count");
        test(normal.periodProven(),"valuation recorded window");
        test(normal.blockers().contains("CLT_CAPACITY_APPROVED_FACTOR_AND_CONSUMER_EVIDENCE_REQUIRED"),"always fail closed");
        test(!normal.blockers().contains("CLT_CAPACITY_RECORDED_VALUE_SHORTFALL"),"no false deficit");
        var shortfall=CollateralCapacityPolicy.calculate(scenario("PLEDGED","FINAL","2026-12-31",
                new BigDecimal("50000000"),List.of(allocation("IRR","60000000",1)),"IRR"));
        test("-10000000".equals(shortfall.residual()),"negative raw delta retained");
        test("10000000".equals(shortfall.shortfall()),"shortfall 10m");
        test(shortfall.blockers().contains("CLT_CAPACITY_RECORDED_VALUE_SHORTFALL"),"shortfall blocker");
        var mixed=CollateralCapacityPolicy.calculate(scenario("ACTIVE","FINAL","2026-12-31",
                BigDecimal.valueOf(100),List.of(allocation("IRR","30",2),allocation("USD","40",1)),"IRR"));
        test(mixed.residual()==null,"mixed cannot produce residual");
        test(mixed.shortfall()==null,"mixed cannot produce shortfall");
        test("30".equals(mixed.sameCurrencyAllocated()),"same currency only");
        test(mixed.activeCount()==3,"multi currency count");
        test(mixed.blockers().contains("CLT_CAPACITY_ACTIVE_ALLOCATION_FX_UNRESOLVED"),"mixed FX blocker");
        var expired=CollateralCapacityPolicy.calculate(scenario("ACTIVE","FINAL","2026-01-01",
                BigDecimal.valueOf(100),List.of(),"IRR"));
        test(!expired.periodProven(),"expired cannot become valid");
        test(expired.blockers().contains("CLT_CAPACITY_CURRENT_VALUATION_PERIOD_UNPROVEN_OR_EXPIRED"),"expired blocker");
        var draft=CollateralCapacityPolicy.calculate(scenario("ACTIVE","DRAFT","2026-12-31",
                BigDecimal.valueOf(100),List.of(),"IRR"));
        test(!draft.periodProven(),"DRAFT not final");
        test(draft.blockers().contains("CLT_CAPACITY_CURRENT_VALUATION_NOT_FINAL"),"DRAFT blocker");
        var cross=CollateralCapacityPolicy.calculate(scenario("ACTIVE","FINAL","2026-12-31",
                BigDecimal.valueOf(100),List.of(),"USD"));
        test(cross.blockers().contains("CLT_CAPACITY_VALUATION_CURRENCY_MISMATCH_NO_FX"),"valuation FX blocker");
        var empty=CollateralCapacityPolicy.calculate(scenario("ACTIVE","FINAL","2026-12-31",
                null,List.of(allocation("IRR","100",1)),"IRR"));
        test(empty.residual()==null,"not invented eligible");
        test(empty.blockers().contains("CLT_CAPACITY_EXISTING_ALLOCATION_WITHOUT_ELIGIBLE_VALUE"),"allocation with missing eligible");
        var flags=new CollateralCapacityPolicy.Input("ACTIVE",true,true,true,true,true,"IRR","1","FINAL","IRR",
                "2026-01-01","2026-12-31","2026-11-03","2025-01-01",null,BigDecimal.ONE,List.of());
        var flagBlockers=CollateralCapacityPolicy.calculate(flags).blockers();
        test(flagBlockers.contains("CLT_CAPACITY_SPONSOR_VALIDITY_NOT_VERIFIED"),"sponsor gate");
        test(flagBlockers.contains("CLT_CAPACITY_INSURANCE_COVERAGE_NOT_VERIFIED"),"insurance gate");
        test(flagBlockers.contains("CLT_CAPACITY_EXCLUSIVE_USAGE_CASE_NOT_VERIFIED"),"exclusive gate");
        test("10000000".equals(CollateralCapacityPolicy.hypotheticalShortfall("-8000000","2000000")),"hypothetical deficit");
        test("0".equals(CollateralCapacityPolicy.hypotheticalShortfall("10000000","2000000")),"hypothetical no deficit");
        test(CollateralCapacityPolicy.hypotheticalShortfall(null,"2000000")==null,"null evidence no invented deficit");
        rejects(()->CollateralCapacityPolicy.positiveMoney("0"),"zero denied");
        rejects(()->CollateralCapacityPolicy.positiveMoney("-1"),"negative denied");
        rejects(()->CollateralCapacityPolicy.storedMoney("1.12345"),"scale denied");
        rejects(()->CollateralCapacityPolicy.storedMoney("10000000000000000"),"precision denied");
        rejects(()->CollateralCapacityPolicy.storedMoney("1000000000000000.00001"),"precision and scale");
        rejects(()->CollateralCapacityPolicy.storedMoney("1e5"),"exponential denied");
        rejects(()->CollateralCapacityPolicy.storedMoney("0003"),"leading zeros denied");
        rejects(()->CollateralCapacityPolicy.currency("IR R"),"invalid currency");
        rejects(()->CollateralCapacityPolicy.domain("UNAPPROVED"),"unapproved domain");
        rejects(()->CollateralCapacityPolicy.hypotheticalShortfall("xyz","1"),"invalid aggregate");
        test(CollateralCapacityPolicy.DOMAINS.contains("LOAN"),"loan domain");
        test(CollateralCapacityPolicy.DOMAINS.contains("GUARANTEE"),"guarantee domain");
        test(CollateralCapacityPolicy.DOMAINS.contains("LETTER_OF_CREDIT"),"lc domain");
        test(CollateralCapacityPolicy.DOMAINS.contains("GREEN_LICENSE"),"green domain");
        test(CollateralCapacityPolicy.POLICY_GAPS.size()>=8,"gaps recorded");
        System.out.println("CLT_R4A_CAPACITY_POLICY_SMOKE_PASS="+passed);
    }
}
