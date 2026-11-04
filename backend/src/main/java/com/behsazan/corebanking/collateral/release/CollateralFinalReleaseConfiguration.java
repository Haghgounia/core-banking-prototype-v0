package com.behsazan.corebanking.collateral.release;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods=false)
@PropertySource("classpath:clt/final-release.properties")
public class CollateralFinalReleaseConfiguration {}
