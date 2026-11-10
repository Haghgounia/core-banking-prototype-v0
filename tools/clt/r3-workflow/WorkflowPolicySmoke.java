import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy;
import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy.Action;
import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy.Kind;

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
        eq("12",CollateralWorkflowPolicy.id("12"));
        eq("9999999999999999999",CollateralWorkflowPolicy.id("9999999999999999999"));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.id("0"));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.id("1 OR 1=1"));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.id("10000000000000000000"));
        eq("1",CollateralWorkflowPolicy.version("1").toPlainString());
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.version("0"));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.version("1.0"));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.version("10000000000"));
        eq("reason text",CollateralWorkflowPolicy.reason("  reason text  "));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.reason("short"));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.reason("12345678\n"));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.reason("x".repeat(501)));
        eq("test-id-01",CollateralWorkflowPolicy.correlation("test-id-01"));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.correlation("../bad"));
        eq("SUBMITTED",CollateralWorkflowPolicy.next("DRAFT",Action.SUBMIT,true,false));
        eq("WITHDRAWN",CollateralWorkflowPolicy.next("DRAFT",Action.WITHDRAW,true,false));
        eq("WITHDRAWN",CollateralWorkflowPolicy.next("SUBMITTED",Action.WITHDRAW,true,false));
        eq("REVIEWED",CollateralWorkflowPolicy.next("SUBMITTED",Action.REVIEW,false,true));
        eq("REJECTED",CollateralWorkflowPolicy.next("SUBMITTED",Action.REJECT,false,true));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.next("DRAFT",Action.SUBMIT,false,false));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.next("DRAFT",Action.WITHDRAW,false,false));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.next("SUBMITTED",Action.REVIEW,true,true));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.next("SUBMITTED",Action.REVIEW,false,false));
        reject(IllegalArgumentException.class,()->CollateralWorkflowPolicy.next("SUBMITTED",Action.REJECT,true,true));
        reject(IllegalStateException.class,()->CollateralWorkflowPolicy.next("DRAFT",Action.REVIEW,false,true));
        reject(IllegalStateException.class,()->CollateralWorkflowPolicy.next("REVIEWED",Action.SUBMIT,true,false));
        reject(IllegalStateException.class,()->CollateralWorkflowPolicy.next("REVIEWED",Action.WITHDRAW,true,false));
        reject(IllegalStateException.class,()->CollateralWorkflowPolicy.next("WITHDRAWN",Action.SUBMIT,true,false));
        eq("REQUEST_SUBMITTED",CollateralWorkflowPolicy.event(Action.SUBMIT));
        eq("REQUEST_WITHDRAWN",CollateralWorkflowPolicy.event(Action.WITHDRAW));
        eq("REQUEST_REVIEWED",CollateralWorkflowPolicy.event(Action.REVIEW));
        eq("REQUEST_REJECTED",CollateralWorkflowPolicy.event(Action.REJECT));
        if(checks!=38)throw new AssertionError("expected 38 got "+checks);
        System.out.println("CLT_R3_WORKFLOW_POLICY_PASS="+checks+"/"+checks);
    }
}
