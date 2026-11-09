package com.behsazan.corebanking.collateral.insurance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/insurance.properties")
@EnableConfigurationProperties(CollateralInsuranceProperties.class)
public class CollateralInsuranceConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.insurance",
            name = "request-writes-enabled", havingValue = "true")
    CollateralInsuranceWriterPool collateralInsuranceWriterPool(CollateralInsuranceProperties properties) {
        return new CollateralInsuranceWriterPool(properties);
    }
}
