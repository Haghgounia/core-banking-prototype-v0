package com.behsazan.corebanking.collateral.valuation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import com.behsazan.corebanking.collateral.prototype.CltR3SharedDatasourcePolicy;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:clt/valuation.properties")
@EnableConfigurationProperties(CollateralValuationProperties.class)
public class CollateralValuationConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.valuation",
            name = "request-writes-enabled", havingValue = "true")
    CollateralValuationWriterPool collateralValuationWriterPool(CollateralValuationProperties properties,
            Environment environment, DataSource applicationDataSource) {
        if (CltR3SharedDatasourcePolicy.useApplicationDatasource(
                environment.getProperty("core-banking.collateral.prototype.enabled", Boolean.class, false),
                environment.getProperty("server.address", ""))) {
            return new CollateralValuationWriterPool(applicationDataSource);
        }
        return new CollateralValuationWriterPool(properties);
    }
}
