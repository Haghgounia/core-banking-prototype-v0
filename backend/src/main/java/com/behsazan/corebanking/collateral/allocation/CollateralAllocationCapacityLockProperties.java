package com.behsazan.corebanking.collateral.allocation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Optional bounded diagnostic; it does NOT enable allocation or reserve capacity. */
@ConfigurationProperties(prefix = "core-banking.collateral.allocation.capacity-lock")
public record CollateralAllocationCapacityLockProperties(
        @DefaultValue("false") boolean probeEnabled) {}
