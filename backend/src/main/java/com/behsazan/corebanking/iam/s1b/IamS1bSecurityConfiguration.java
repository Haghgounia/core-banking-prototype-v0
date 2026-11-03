package com.behsazan.corebanking.iam.s1b;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    /** An explicitly named decoder prevents another application's JWT decoder from becoming IAM trust. */
    @Bean("iamS1bJwtDecoder")
    @ConditionalOnProperty(prefix = "core-banking.iam.s1b", name = "reads-enabled", havingValue = "true")
    public JwtDecoder iamS1bJwtDecoder(IamS1bProperties p) {
        p.requireSafeReadConfiguration();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(p.jwksUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(p.issuer());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults,
                IamS1bTokenValidation.requiredClaims(p.audience(), p.maxTokenLifetimeSeconds())));
        return decoder;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain iamApiSecurity(HttpSecurity http, IamS1bProperties p,
            @Qualifier("iamS1bJwtDecoder") ObjectProvider<JwtDecoder> iamDecoder) throws Exception {
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
        // The named decoder is the only accepted trust boundary, never an unqualified global JwtDecoder.
        http.exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                .authorizeHttpRequests(req -> req
                        .requestMatchers(HttpMethod.GET, "/api/v1/iam/**").hasAuthority("SCOPE_iam.users.read")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(iamDecoder.getObject())));
        return http.build();
    }
}
