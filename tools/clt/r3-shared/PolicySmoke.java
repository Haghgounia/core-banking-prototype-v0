import com.behsazan.corebanking.collateral.prototype.CltSharedDatasourcePolicy;

public class PolicySmoke {
    private static int passes = 0;
    private static void check(boolean ok) {
        if (!ok) throw new IllegalStateException("R3_SHARED_POLICY_FAILED_AT_" + passes);
        passes++;
    }
    private static void rejected(String host) {
        try { CltSharedDatasourcePolicy.useApplicationDatasource(true, host); }
        catch (IllegalStateException expected) {
            check("CLT_R3_SHARED_WRITER_LOOPBACK_ONLY".equals(expected.getMessage())); return;
        }
        throw new IllegalStateException("R3_SHARED_NON_LOOPBACK_ACCEPTED_" + host);
    }
    public static void main(String[] args) {
        check(CltSharedDatasourcePolicy.useApplicationDatasource(true, "127.0.0.1"));
        check(!CltSharedDatasourcePolicy.useApplicationDatasource(false, "127.0.0.1"));
        check(!CltSharedDatasourcePolicy.useApplicationDatasource(false, "0.0.0.0"));
        check(!CltSharedDatasourcePolicy.useApplicationDatasource(false, null));
        rejected("0.0.0.0"); rejected("localhost"); rejected("::1"); rejected("192.168.1.10");
        rejected(""); rejected(null);
        System.out.println("CLT_R3_SHARED_POLICY_SMOKE_PASS=" + passes);
    }
}
