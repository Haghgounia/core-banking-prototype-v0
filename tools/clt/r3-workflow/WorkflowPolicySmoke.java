import com.behsazan.corebanking.collateral.workflow.CollateralR3WorkflowPolicy;
import com.behsazan.corebanking.collateral.workflow.CollateralR3WorkflowPolicy.Action;
import com.behsazan.corebanking.collateral.workflow.CollateralR3WorkflowPolicy.Kind;

public final class WorkflowPolicySmoke {
    private static int checks = 0;
    private static void eq(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError(expected+" != "+actual);
        checks++;
    }
    private static void reject(Class<? extends Throwable> error, Runnable action) {
        try { action.run(); }
        catch (Throwable thrown) {
            if (error.isInstance(thrown)) { checks++; return; }
            throw new AssertionError("wrong exception "+thrown,thrown);
        }
        throw new AssertionError("expected rejection: "+error.getName());
    }
    public static void main(String[] args) {
        eq("SPONSOR", Kind.parse("sponsor").name());
        eq("INSURANCE", Kind.parse("insurance").name());
        eq("VALUATION", Kind.parse("valuation").name());
        reject(IllegalArgumentException.class, ()->Kind.parse("../../CLT.COLLATERAL"));
        reject(IllegalArgumentException.class, ()->Kind.parse("Sponsor"));
        eq("12",CollateralR3WorkflowPolicy.id("12"));
        eq("9999999999999999999",CollateralR3WorkflowPolicy.id("9999999999999999999"));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.id("0"));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.id("1 OR 1=1"));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.id("10000000000000000000"));
        eq("1",CollateralR3WorkflowPolicy.version("1").toPlainString());
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.version("0"));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.version("1.0"));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.version("10000000000"));
        eq("reason text",CollateralR3WorkflowPolicy.reason("  reason text  "));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.reason("short"));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.reason("12345678\n"));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.reason("x".repeat(501)));
        eq("test-id-01",CollateralR3WorkflowPolicy.correlation("test-id-01"));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.correlation("../bad"));
        eq("SUBMITTED",CollateralR3WorkflowPolicy.next("DRAFT",Action.SUBMIT,true,false));
        eq("WITHDRAWN",CollateralR3WorkflowPolicy.next("DRAFT",Action.WITHDRAW,true,false));
        eq("WITHDRAWN",CollateralR3WorkflowPolicy.next("SUBMITTED",Action.WITHDRAW,true,false));
        eq("REVIEWED",CollateralR3WorkflowPolicy.next("SUBMITTED",Action.REVIEW,false,true));
        eq("REJECTED",CollateralR3WorkflowPolicy.next("SUBMITTED",Action.REJECT,false,true));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.next("DRAFT",Action.SUBMIT,false,false));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.next("DRAFT",Action.WITHDRAW,false,false));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.next("SUBMITTED",Action.REVIEW,true,true));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.next("SUBMITTED",Action.REVIEW,false,false));
        reject(IllegalArgumentException.class,()->CollateralR3WorkflowPolicy.next("SUBMITTED",Action.REJECT,true,true));
        reject(IllegalStateException.class,()->CollateralR3WorkflowPolicy.next("DRAFT",Action.REVIEW,false,true));
        reject(IllegalStateException.class,()->CollateralR3WorkflowPolicy.next("REVIEWED",Action.SUBMIT,true,false));
        reject(IllegalStateException.class,()->CollateralR3WorkflowPolicy.next("REVIEWED",Action.WITHDRAW,true,false));
        reject(IllegalStateException.class,()->CollateralR3WorkflowPolicy.next("WITHDRAWN",Action.SUBMIT,true,false));
        eq("REQUEST_SUBMITTED",CollateralR3WorkflowPolicy.event(Action.SUBMIT));
        eq("REQUEST_WITHDRAWN",CollateralR3WorkflowPolicy.event(Action.WITHDRAW));
        eq("REQUEST_REVIEWED",CollateralR3WorkflowPolicy.event(Action.REVIEW));
        eq("REQUEST_REJECTED",CollateralR3WorkflowPolicy.event(Action.REJECT));
        if(checks!=38)throw new AssertionError("expected 38 got "+checks);
        System.out.println("CLT_R3_WORKFLOW_POLICY_PASS="+checks+"/"+checks);
    }
}
