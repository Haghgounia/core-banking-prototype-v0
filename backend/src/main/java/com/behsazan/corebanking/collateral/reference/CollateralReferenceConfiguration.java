package com.behsazan.corebanking.collateral.reference;

import com.behsazan.corebanking.collateral.localuat.CltLocalUatProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
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
    CollateralReferenceReaderPool collateralReferenceReaderPool(CollateralReferenceProperties config,
            CltLocalUatProperties local, CollateralRegistryProperties registry,
            Environment environment, DataSource applicationDataSource) {
        String launcherMode = environment.getProperty("CLT_LOCAL_UAT_READONLY_ENABLED", "false");
        if ("true".equalsIgnoreCase(launcherMode) != local.enabled())
            throw new IllegalStateException("CLT_R6H_LOCAL_MODE_BINDING_MISMATCH");
        if (local.enabled()) {
            local.requireSafeMode(environment, config, registry);
            return new CollateralReferenceReaderPool(applicationDataSource);
        }
        return new CollateralReferenceReaderPool(config);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "core-banking.collateral.reference", name = "writes-enabled", havingValue = "true")
    CollateralReferenceWriterPool collateralReferenceWriterPool(CollateralReferenceProperties config) {
        return new CollateralReferenceWriterPool(config);
    }
}
