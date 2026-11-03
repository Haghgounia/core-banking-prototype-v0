package com.behsazan.corebanking.iam.s1b;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
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
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(p.jwksUri()).build();
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(p.issuer());
        OAuth2TokenValidator<Jwt> audienceAndExpiry = token ->
                token.getAudience() != null && token.getAudience().contains(p.audience())
                && token.getSubject() != null && !token.getSubject().isBlank()
                && token.getIssuedAt() != null && token.getExpiresAt() != null
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                "invalid_token", "IAM access token lacks required audience, actor or lifetime", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audienceAndExpiry));
        http.authorizeHttpRequests(req -> req.anyRequest().hasAuthority("SCOPE_iam.users.read"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(decoder)));
        return http.build();
    }
}
