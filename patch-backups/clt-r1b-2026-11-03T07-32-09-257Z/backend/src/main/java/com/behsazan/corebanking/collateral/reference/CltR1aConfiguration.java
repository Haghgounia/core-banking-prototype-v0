package com.behsazan.corebanking.collateral.reference;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/r1a.properties")
@EnableConfigurationProperties(CltR1aProperties.class)
public class CltR1aConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.r1a", name = "reads-enabled", havingValue = "true")
    CltR1aReaderPool cltR1aReaderPool(CltR1aProperties config) {
        return new CltR1aReaderPool(config);
    }
}
