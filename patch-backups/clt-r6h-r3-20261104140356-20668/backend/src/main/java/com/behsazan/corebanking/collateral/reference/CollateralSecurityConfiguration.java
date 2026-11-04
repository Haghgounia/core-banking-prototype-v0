package com.behsazan.corebanking.collateral.reference;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

/** Own CLT matcher; it neither modifies nor trusts the IAM S1B chain. */
@Configuration(proxyBeanMethods = false)
public class CollateralSecurityConfiguration {
    @Bean("collateralJwtDecoder")
    @ConditionalOnProperty(prefix = "core-banking.collateral.reference", name = "reads-enabled", havingValue = "true")
    JwtDecoder collateralJwtDecoder(CollateralReferenceProperties p) {
        p.requireSafeConfig();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(p.jwksUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(p.issuer());
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            Instant issued = jwt.getIssuedAt(), expires = jwt.getExpiresAt();
            if (jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || jwt.getAudience() == null || !jwt.getAudience().contains(p.audience())
                    || issued == null || expires == null || !expires.isAfter(issued)
                    || issued.isAfter(Instant.now().plusSeconds(60))
                    || Duration.between(issued, expires).getSeconds() > p.maxTokenLifetimeSeconds()) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "CLT_REFERENCE_JWT_CLAIMS_REJECTED", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, claims));
        return decoder;
    }

    @Bean
    @Order(2)
    SecurityFilterChain collateralSecurity(HttpSecurity http, CollateralReferenceProperties p,
            @Qualifier("collateralJwtDecoder") ObjectProvider<JwtDecoder> decoder) throws Exception {
        http.securityMatcher("/api/v1/collateral/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (!p.readsEnabled()) {
            http.authorizeHttpRequests(c -> c.anyRequest().denyAll());
            return http.build();
        }
        p.requireSafeConfig();
        http.exceptionHandling(c -> c.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                .authorizeHttpRequests(c -> {
                    c.requestMatchers(HttpMethod.GET, "/api/v1/collateral/reference/changes")
                            .hasAuthority("SCOPE_clt.reference.review");
                    c.requestMatchers(HttpMethod.GET, "/api/v1/collateral/reference/**")
                            .hasAuthority("SCOPE_clt.reference.read");
                    if (p.writesEnabled()) {
                        c.requestMatchers(HttpMethod.POST, "/api/v1/collateral/reference/changes")
                                .hasAuthority("SCOPE_clt.reference.propose");
                        c.requestMatchers(HttpMethod.POST, "/api/v1/collateral/reference/changes/*/decisions")
                                .hasAuthority("SCOPE_clt.reference.approve");
                    }
                    c.anyRequest().denyAll();
                })
                .oauth2ResourceServer(c -> c.jwt(jwt -> jwt.decoder(decoder.getObject())));
        return http.build();
    }
}
