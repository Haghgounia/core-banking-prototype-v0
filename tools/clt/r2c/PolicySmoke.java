import com.behsazan.corebanking.collateral.introduction.*;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionModels.*;
import java.util.List;

public final class PolicySmoke {
    private static int passed = 0;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("SMOKE_" + passed); passed++; }
    private static void fail(String expected, Runnable action) {
        try { action.run(); throw new AssertionError("DID_NOT_FAIL:"+expected); }
        catch (IllegalArgumentException e) { check(expected.equals(e.getMessage())); }
    }
    public static void main(String[] args) {
        var one = new SerialRangeInput("001", "020", "100");
        var two = new SerialRangeInput("21", "21", "200");
        var a = CollateralSerialRangePolicy.assess(true, "IRR", "300", List.of(one, two));
        check(a.itemCount() == 21);
        check("300".equals(a.totalAmountIRR()));
        check(a.rows().size() == 2);
        fail("CLT_R2C_SERIAL_ROW_OVER_20", () -> CollateralSerialRangePolicy.normalize(List.of(new SerialRangeInput("1", "21", "100"))));
        fail("CLT_R2C_SERIAL_RANGE_OVERLAP", () -> CollateralSerialRangePolicy.normalize(List.of(one,new SerialRangeInput("20", "22", "10"))));
        fail("CLT_R2C_SERIAL_RANGE_OVERLAP", () -> CollateralSerialRangePolicy.normalize(List.of(new SerialRangeInput("001", "002", "10"), new SerialRangeInput("2", "2", "10"))));
        fail("CLT_R2C_SERIAL_END_BEFORE_START", () -> CollateralSerialRangePolicy.normalize(List.of(new SerialRangeInput("3", "2", "100"))));
        fail("CLT_R2C_SERIAL_INVALID", () -> CollateralSerialRangePolicy.normalize(List.of(new SerialRangeInput("1a", "2", "100"))));
        fail("CLT_R2C_SERIAL_INVALID", () -> CollateralSerialRangePolicy.normalize(List.of(new SerialRangeInput("1".repeat(101), "2", "100"))));
        fail("CLT_R2C_ROW_AMOUNT_IRR_INVALID", () -> CollateralSerialRangePolicy.normalize(List.of(new SerialRangeInput("1", "2", "1.0"))));
        fail("CLT_R2C_SERIAL_RANGES_REQUIRED", () -> CollateralSerialRangePolicy.assess(true,"IRR","1",List.of()));
        fail("CLT_R2C_TYPE_NOT_COUNTABLE", () -> CollateralSerialRangePolicy.assess(false,"IRR","2",List.of(new SerialRangeInput("1","2","2"))));
        fail("CLT_R2C_COUNTABLE_FOREIGN_CURRENCY_POLICY_OPEN", () -> CollateralSerialRangePolicy.assess(true,"USD","2",List.of(new SerialRangeInput("1","2","2"))));
        fail("CLT_R2C_ITEM_AMOUNT_SUM_MISMATCH", () -> CollateralSerialRangePolicy.assess(true,"IRR","3",List.of(new SerialRangeInput("1","2","2"))));
        var transliterated = CollateralSerialRangePolicy.normalize(List.of(new SerialRangeInput("\u06f1\u06f2", "\u0661\u0664", "100")));
        check("12".equals(transliterated.getFirst().serialFrom()));
        check("14".equals(transliterated.getFirst().serialTo()));
        check(CollateralSerialRangePolicy.assess(false,"IRR","100",List.of()).itemCount()==0);
        var s1 = new Request("1","2","300","IRR","2026-11-03",null,null,null,List.of(),null,List.of(two,one));
        var s2 = new Request("1","2","300","IRR","2026-11-03",null,null,null,List.of(),null,List.of(one,two));
        check(CollateralIntroductionPolicy.normalize(s1).canonical().equals(CollateralIntroductionPolicy.normalize(s2).canonical()));
        check(CollateralIntroductionPolicy.normalize(s1).canonical().contains("R2C_ASSET_CANDIDATE"));
        var old = new Request("1","2","300","IRR","2026-11-03",null,null,null,List.of());
        check(!CollateralIntroductionPolicy.normalize(old).canonical().contains("R2C_ASSET_CANDIDATE"));
        fail("CLT_R2C_ASSET_REFERENCE_INVALID", () -> CollateralIntroductionPolicy.normalize(new Request("1","2","300","IRR","2026-11-03",null,null,null,List.of(),"x".repeat(201),List.of())));
        System.out.println("CLT_R2C_PURE_POLICY_SMOKE_PASS=" + passed);
    }
}
