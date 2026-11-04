package com.behsazan.corebanking.collateral.localuat;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import org.springframework.core.env.Environment;

/** Temporary, explicitly opted-in, loopback-only CLT UAT mode; never production identity. */
public record CltLocalUatProperties(boolean enabled, String branchCode) {
    private static final String[] FORBIDDEN_WRITE_FLAGS = {
            "core-banking.collateral.reference.writes-enabled",
            "core-banking.collateral.introduction.draft-writes-enabled",
            "core-banking.collateral.introduction.review-workflow-enabled",
            "core-banking.collateral.sponsor.request-writes-enabled",
            "core-banking.collateral.insurance.request-writes-enabled",
            "core-banking.collateral.valuation.request-writes-enabled",
            "core-banking.collateral.allocation.intake.request-writes-enabled",
            "core-banking.collateral.allocation.capacity-lock.probe-enabled",
            "core-banking.collateral.unallocation.intake.request-writes-enabled"
    };

    public void requireSafeMode(Environment environment, CollateralReferenceProperties reference,
                                CollateralRegistryProperties registry) {
        if (!enabled) throw new IllegalStateException("CLT_LOCAL_UAT_NOT_ENABLED");
        if (!"127.0.0.1".equals(environment.getProperty("server.address")))
            throw new IllegalStateException("CLT_LOCAL_UAT_REQUIRES_SERVER_ADDRESS_127_0_0_1");
        if (!reference.readsEnabled() || !registry.readsEnabled())
            throw new IllegalStateException("CLT_LOCAL_UAT_REQUIRES_READ_FLAGS");
        // Optional SERVER-SIDE override; normal launcher uses Oracle branch discovery.
        if (branchCode != null && !branchCode.isBlank()
                && !branchCode.matches("[A-Za-z0-9_-]{1,30}"))
            throw new IllegalStateException("CLT_LOCAL_UAT_BRANCH_CONFIG_INVALID");
        for (String flag : FORBIDDEN_WRITE_FLAGS) {
            if (environment.getProperty(flag, Boolean.class, false))
                throw new IllegalStateException("CLT_LOCAL_UAT_WRITE_GATE_MUST_BE_DISABLED: " + flag);
        }
    }
}
