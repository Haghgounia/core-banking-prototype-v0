package com.behsazan.corebanking.iam.s1b;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

/** Only the IAM API is protected here. Existing prototype endpoints remain unchanged. */
@Configuration(proxyBeanMethods = false)
public class IamS1bSecurityConfiguration {
    @Bean
    @Order(1)
    public SecurityFilterChain iamApiSecurity(HttpSecurity http, IamS1bProperties p) throws Exception {
        http.securityMatcher("/api/v1/iam/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (!p.readsEnabled()) {
            http.authorizeHttpRequests(req -> req.anyRequest().denyAll());
            return http.build();
        }
        p.requireSafeReadConfiguration();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(p.jwksUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(p.issuer());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults,
                IamS1bTokenValidation.requiredClaims(p.audience(), p.maxTokenLifetimeSeconds())));
        http.exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                .authorizeHttpRequests(req -> req
                        .requestMatchers(HttpMethod.GET, "/api/v1/iam/**").hasAuthority("SCOPE_iam.users.read")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(decoder)));
        return http.build();
    }
}
