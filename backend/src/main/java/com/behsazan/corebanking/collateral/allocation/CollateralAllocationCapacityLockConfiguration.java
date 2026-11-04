package com.behsazan.corebanking.collateral.allocation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/allocation-capacity-lock.properties")
@EnableConfigurationProperties(CollateralAllocationCapacityLockProperties.class)
public class CollateralAllocationCapacityLockConfiguration {}
