package com.behsazan.corebanking.collateral.valuation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/valuation.properties")
@EnableConfigurationProperties(CollateralValuationProperties.class)
public class CollateralValuationConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.valuation",
            name = "request-writes-enabled", havingValue = "true")
    CollateralValuationWriterPool collateralValuationWriterPool(CollateralValuationProperties properties) {
        return new CollateralValuationWriterPool(properties);
    }
}
