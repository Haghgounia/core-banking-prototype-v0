package com.behsazan.corebanking.collateral.registry;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/registry.properties")
@EnableConfigurationProperties(CollateralRegistryProperties.class)
public class CollateralRegistryConfiguration {}
