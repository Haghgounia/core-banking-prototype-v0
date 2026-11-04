package com.behsazan.corebanking.collateral.reference;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/reference.properties")
@EnableConfigurationProperties(CollateralReferenceProperties.class)
public class CollateralReferenceConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.reference", name = "reads-enabled", havingValue = "true")
    CollateralReferenceReaderPool collateralReferenceReaderPool(CollateralReferenceProperties config) {
        return new CollateralReferenceReaderPool(config);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.reference", name = "writes-enabled", havingValue = "true")
    CollateralReferenceWriterPool collateralReferenceWriterPool(CollateralReferenceProperties config) {
        return new CollateralReferenceWriterPool(config);
    }
}
