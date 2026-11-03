package com.behsazan.corebanking.collateral.registry;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

/** Separate chain for contract /api/v1/collaterals; existing IAM/R1 chains remain untouched. */
@Configuration(proxyBeanMethods = false)
public class CollateralRegistrySecurityConfiguration {
    @Bean
    @Order(3)
    SecurityFilterChain collateralRegistrySecurity(HttpSecurity http,
            CollateralReferenceProperties reference,
            CollateralRegistryProperties registry,
            @Qualifier("collateralJwtDecoder") ObjectProvider<JwtDecoder> decoder) throws Exception {
        http.securityMatcher("/api/v1/collaterals", "/api/v1/collaterals/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (!reference.readsEnabled() || !registry.readsEnabled()) {
            http.authorizeHttpRequests(r -> r.anyRequest().denyAll());
            return http.build();
        }
        reference.requireSafeConfig();
        http.exceptionHandling(e -> e.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                .authorizeHttpRequests(r -> r
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals", "/api/v1/collaterals/**")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(decoder.getObject())));
        return http.build();
    }
}
