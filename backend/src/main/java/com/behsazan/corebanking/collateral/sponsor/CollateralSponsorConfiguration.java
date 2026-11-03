package com.behsazan.corebanking.collateral.sponsor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/sponsor.properties")
@EnableConfigurationProperties(CollateralSponsorProperties.class)
public class CollateralSponsorConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.sponsor", name = "request-writes-enabled", havingValue = "true")
    CollateralSponsorWriterPool collateralSponsorWriterPool(CollateralSponsorProperties properties) {
        return new CollateralSponsorWriterPool(properties);
    }
}
