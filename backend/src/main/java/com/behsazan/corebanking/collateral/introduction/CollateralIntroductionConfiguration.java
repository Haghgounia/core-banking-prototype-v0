package com.behsazan.corebanking.collateral.introduction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/introduction.properties")
@EnableConfigurationProperties(CollateralIntroductionProperties.class)
public class CollateralIntroductionConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.introduction", name = "draft-writes-enabled", havingValue = "true")
    CollateralIntroductionWriterPool collateralIntroductionWriterPool(CollateralIntroductionProperties properties,
                                                                      Environment environment, DataSource dataSource) {
        if (environment.getProperty("core-banking.collateral.prototype.enabled", Boolean.class, false)) {
            if (!"127.0.0.1".equals(environment.getProperty("server.address", "")))
                throw new IllegalStateException("CLT_INTRODUCTION_SHARED_WRITER_LOOPBACK_ONLY");
            return new CollateralIntroductionWriterPool(dataSource);
        }
        return new CollateralIntroductionWriterPool(properties);
    }
}
