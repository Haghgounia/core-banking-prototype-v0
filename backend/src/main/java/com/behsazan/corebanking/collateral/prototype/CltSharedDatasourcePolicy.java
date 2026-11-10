package com.behsazan.corebanking.collateral.prototype;

/** Restricts R3 stage writers to the common Oracle datasource in local unified mode only. */
public final class CltSharedDatasourcePolicy {
    private CltSharedDatasourcePolicy() {}
    public static boolean useApplicationDatasource(boolean prototypeEnabled, String bindAddress) {
        if (!prototypeEnabled) return false;
        if (!"127.0.0.1".equals(bindAddress))
            throw new IllegalStateException("CLT_R3_SHARED_WRITER_LOOPBACK_ONLY");
        return true;
    }
}
