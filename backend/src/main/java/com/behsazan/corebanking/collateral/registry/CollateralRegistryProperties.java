package com.behsazan.corebanking.collateral.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Explicit extra gate. Reference-data read permission alone does not enable registry. */
@ConfigurationProperties(prefix = "core-banking.collateral.registry")
public record CollateralRegistryProperties(@DefaultValue("false") boolean readsEnabled) {}
