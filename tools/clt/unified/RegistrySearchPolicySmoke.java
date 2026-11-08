package com.behsazan.corebanking.collateral.registry;

/** Pure Java 21 smoke, no Oracle / Spring runtime dependencies. */
public class RegistrySearchPolicySmoke {
    public static void main(String[] args) {
        int ok = 0;
        if (!"9999999999999999999".equals(CollateralRegistryPolicy.id("9999999999999999999").toPlainString()))
            throw new AssertionError("NUMBER19_PRECISION");
        ok++;
        if (!"PENDING_APPROVAL".equals(CollateralRegistryPolicy.statusCode("PENDING_APPROVAL")))
            throw new AssertionError("STATUS_CONTRACT");
        ok++;
        for (String bad : new String[]{"", " PENDING", "X' OR '1'='1", "S%", "A/1"}) {
            try { CollateralRegistryPolicy.statusCode(bad); throw new AssertionError("UNSAFE_STATUS_ACCEPTED"); }
            catch (IllegalArgumentException expected) { ok++; }
        }
        for (String bad : new String[]{"0", "01", "1.2", "10000000000000000000"}) {
            try { CollateralRegistryPolicy.id(bad); throw new AssertionError("UNSAFE_IDENTIFIER_ACCEPTED"); }
            catch (IllegalArgumentException expected) { ok++; }
        }
        System.out.println("CLT_R2_JAVA_POLICY_PASS="+ok+"/"+ok);
    }
}
