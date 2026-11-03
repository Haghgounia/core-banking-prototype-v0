package com.behsazan.corebanking.collateral.allocation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/allocation-intake.properties")
@EnableConfigurationProperties(CollateralAllocationIntakeProperties.class)
public class CollateralAllocationIntakeConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.allocation.intake",
            name = "request-writes-enabled", havingValue = "true")
    CollateralAllocationIntakeWriterPool collateralAllocationIntakeWriterPool(CollateralAllocationIntakeProperties p) {
        return new CollateralAllocationIntakeWriterPool(p);
    }
}
