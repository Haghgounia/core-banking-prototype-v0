package com.behsazan.corebanking.collateral.localuat;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** R6H: one env-sourced local mode; no parallel property binding or manual branch prompt. */
@Configuration(proxyBeanMethods = false)
public class CltLocalUatConfiguration {
    public static final String BUILD_MARKER = "CLT_R6H_AUTO_BRANCH_20261104";

    @Bean
    CltLocalUatProperties cltLocalUatProperties(Environment environment) {
        String flag = environment.getProperty("CLT_LOCAL_UAT_READONLY_ENABLED", "false");
        if (!"true".equalsIgnoreCase(flag) && !"false".equalsIgnoreCase(flag))
            throw new IllegalStateException("CLT_LOCAL_UAT_FLAG_INVALID");
        boolean enabled = Boolean.parseBoolean(flag);
        if (enabled) System.out.println("CLT_R6H_LOCAL_BOOTSTRAP_ACTIVE; " + BUILD_MARKER);
        return new CltLocalUatProperties(enabled,
                environment.getProperty("CLT_LOCAL_UAT_BRANCH_CODE"));
    }

    @Bean
    @ConditionalOnProperty(name = "CLT_LOCAL_UAT_READONLY_ENABLED", havingValue = "true")
    CltLocalUatBranchResolver cltLocalUatBranchResolver(CltLocalUatProperties local, DataSource dataSource) {
        return new CltLocalUatBranchResolver(local, dataSource);
    }
}
