package com.behsazan.corebanking.collateral.unallocation;

import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods=false)
@PropertySource("classpath:clt/unallocation-intake.properties")
@EnableConfigurationProperties(CollateralUnallocationIntakeProperties.class)
public class CollateralUnallocationIntakeConfiguration {
    @Bean(destroyMethod="close")
    @ConditionalOnProperty(prefix="core-banking.collateral.unallocation.intake",
            name="request-writes-enabled",havingValue="true")
    CollateralUnallocationIntakeWriterPool collateralUnallocationIntakeWriterPool(
            CollateralUnallocationIntakeProperties properties, DataSource applicationDataSource, Environment environment) {
        if (environment.getProperty("core-banking.collateral.prototype.enabled", Boolean.class, false))
            return new CollateralUnallocationIntakeWriterPool(applicationDataSource);
        return new CollateralUnallocationIntakeWriterPool(properties);
    }
}
