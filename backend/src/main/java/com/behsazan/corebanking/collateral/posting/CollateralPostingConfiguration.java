package com.behsazan.corebanking.collateral.posting;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods=false)
@PropertySource("classpath:clt/collateral-posting.properties")
public class CollateralPostingConfiguration {}
