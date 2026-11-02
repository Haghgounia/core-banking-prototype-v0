package com.behsazan.corebanking.iam.s1b;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Isolated read pool does not shadow the existing application's primary JDBC auto-wiring. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IamS1bProperties.class)
public class IamS1bConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.iam.s1b", name = "reads-enabled", havingValue = "true")
    public IamS1bReaderPool iamS1bReaderPool(IamS1bProperties p) {
        return new IamS1bReaderPool(p);
    }
}
