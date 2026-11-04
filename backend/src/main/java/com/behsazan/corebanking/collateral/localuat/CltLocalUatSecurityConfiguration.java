package com.behsazan.corebanking.collateral.localuat;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.registry.CollateralRegistryProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/** CLT endpoints only: DEV loopback GET; no IAM integration, no write permissions. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "CLT_LOCAL_UAT_READONLY_ENABLED", havingValue = "true")
public class CltLocalUatSecurityConfiguration {
    private static void lockedDown(HttpSecurity http, CltLocalUatProperties local, CltLocalUatBranchResolver branchResolver) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(new CltLocalUatAuthenticationFilter(local, branchResolver), AnonymousAuthenticationFilter.class);
    }

    @Bean
    @Order(2)
    SecurityFilterChain cltLocalReferenceSecurity(HttpSecurity http, CltLocalUatProperties local,
            CltLocalUatBranchResolver branchResolver,
            Environment environment, CollateralReferenceProperties reference,
            CollateralRegistryProperties registry) throws Exception {
        local.requireSafeMode(environment, reference, registry);
        http.securityMatcher("/api/v1/collateral/**");
        lockedDown(http, local, branchResolver);
        http.authorizeHttpRequests(c -> c
                .requestMatchers(HttpMethod.GET, "/api/v1/collateral/reference/changes",
                        "/api/v1/collateral/reference/governance").denyAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/collateral/reference/catalog",
                        "/api/v1/collateral/reference/readiness",
                        "/api/v1/collateral/reference/*/descriptor",
                        "/api/v1/collateral/reference/*",
                        "/api/v1/collateral/reference/*/*")
                        .hasAuthority("SCOPE_clt.reference.read")
                .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain cltLocalRegistrySecurity(HttpSecurity http, CltLocalUatProperties local,
            CltLocalUatBranchResolver branchResolver,
            Environment environment, CollateralReferenceProperties reference,
            CollateralRegistryProperties registry) throws Exception {
        local.requireSafeMode(environment, reference, registry);
        http.securityMatcher("/api/v1/collaterals", "/api/v1/collaterals/**");
        lockedDown(http, local, branchResolver);
        http.authorizeHttpRequests(c -> c
                // Strict GET allowlist. An added endpoint is NOT exposed automatically.
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/collaterals", "/api/v1/collaterals/readiness",
                        "/api/v1/collaterals/types", "/api/v1/collaterals/types/*/attributes",
                        "/api/v1/collaterals/by-registration/*",
                        "/api/v1/collaterals/reports/overview",
                        "/api/v1/collaterals/reports/recent-events",
                        "/api/v1/collaterals/reports/reference-summary",
                        "/api/v1/collaterals/sponsors/readiness",
                        "/api/v1/collaterals/insurance-policies/readiness",
                        "/api/v1/collaterals/valuations/readiness",
                        "/api/v1/collaterals/introduction-drafts/readiness",
                        "/api/v1/collaterals/introduction-reviews/readiness",
                        "/api/v1/collaterals/*",
                        "/api/v1/collaterals/*/sponsors",
                        "/api/v1/collaterals/*/insurance-policies",
                        "/api/v1/collaterals/*/insurance-policies/options",
                        "/api/v1/collaterals/*/insurance-policies/renewal-options",
                        "/api/v1/collaterals/*/valuations",
                        "/api/v1/collaterals/*/valuations/current-evidence",
                        "/api/v1/collaterals/*/valuations/options",
                        "/api/v1/collaterals/*/capacity",
                        "/api/v1/collaterals/*/allocations",
                        "/api/v1/collaterals/*/allocations/readiness",
                        "/api/v1/collaterals/*/allocations/*/transactions",
                        "/api/v1/collaterals/*/allocation-requests/lock/readiness",
                        "/api/v1/collaterals/*/allocation-requests/intake/readiness",
                        "/api/v1/collaterals/*/unallocation-requests/readiness",
                        "/api/v1/collaterals/*/final-release/readiness",
                        "/api/v1/collaterals/*/final-release/evidence",
                        "/api/v1/collaterals/*/final-release/status-history")
                        .hasAuthority("SCOPE_clt.collateral.read")
                .anyRequest().denyAll());
        return http.build();
    }
}
