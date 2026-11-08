package com.behsazan.corebanking.collateral.introduction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/introduction.properties")
@EnableConfigurationProperties(CollateralIntroductionProperties.class)
public class CollateralIntroductionConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.introduction", name = "draft-writes-enabled", havingValue = "true")
    CollateralIntroductionWriterPool collateralIntroductionWriterPool(CollateralIntroductionProperties properties) {
        return new CollateralIntroductionWriterPool(properties);
    }
}
