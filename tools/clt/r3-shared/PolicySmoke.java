import com.behsazan.corebanking.collateral.prototype.CltR3SharedDatasourcePolicy;

public class PolicySmoke {
    private static int passes = 0;
    private static void check(boolean ok) {
        if (!ok) throw new IllegalStateException("R3_SHARED_POLICY_FAILED_AT_" + passes);
        passes++;
    }
    private static void rejected(String host) {
        try { CltR3SharedDatasourcePolicy.useApplicationDatasource(true, host); }
        catch (IllegalStateException expected) {
            check("CLT_R3_SHARED_WRITER_LOOPBACK_ONLY".equals(expected.getMessage())); return;
        }
        throw new IllegalStateException("R3_SHARED_NON_LOOPBACK_ACCEPTED_" + host);
    }
    public static void main(String[] args) {
        check(CltR3SharedDatasourcePolicy.useApplicationDatasource(true, "127.0.0.1"));
        check(!CltR3SharedDatasourcePolicy.useApplicationDatasource(false, "127.0.0.1"));
        check(!CltR3SharedDatasourcePolicy.useApplicationDatasource(false, "0.0.0.0"));
        check(!CltR3SharedDatasourcePolicy.useApplicationDatasource(false, null));
        rejected("0.0.0.0"); rejected("localhost"); rejected("::1"); rejected("192.168.1.10");
        rejected(""); rejected(null);
        System.out.println("CLT_R3_SHARED_POLICY_SMOKE_PASS=" + passes);
    }
}
