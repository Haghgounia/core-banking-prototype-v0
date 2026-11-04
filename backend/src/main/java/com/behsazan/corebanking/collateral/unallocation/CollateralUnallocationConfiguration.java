package com.behsazan.corebanking.collateral.unallocation;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Isolated CLT configuration: does not mutate shared Spring settings. */
@Configuration(proxyBeanMethods=false)
@PropertySource("classpath:clt/unallocation.properties")
public class CollateralUnallocationConfiguration {}
