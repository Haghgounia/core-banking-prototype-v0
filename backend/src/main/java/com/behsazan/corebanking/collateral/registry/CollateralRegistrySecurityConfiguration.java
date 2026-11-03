package com.behsazan.corebanking.collateral.registry;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionProperties;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorProperties;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceProperties;
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
            CollateralIntroductionProperties introduction,
            CollateralSponsorProperties sponsor,
            CollateralInsuranceProperties insurance,
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
                        // R3B: only insured-party candidate checks and optional STAGING writes.
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/insurance-policies/validate")
                        .hasAuthority("SCOPE_clt.collateral.introduce")
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/insurance-policies/requests")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                insurance.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/insurance-policies/requests/mine")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                insurance.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        // R3A: candidate checks are independent of the consuming loan/case.
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/sponsors/validate")
                        .hasAuthority("SCOPE_clt.collateral.introduce")
                        // Only staging REQUEST persistence; never CLT.COLLATERAL_SPONSOR DML.
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/sponsors/requests")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                sponsor.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/sponsors/requests/mine")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                sponsor.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/introduction-drafts/validate",
                                "/api/v1/collaterals/introduction-drafts/registration-preflight")
                        .hasAuthority("SCOPE_clt.collateral.introduce")
                        // R2D draft review is optional and never permits CLT.COLLATERAL writes.
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/introduction-reviews/readiness")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/introduction-drafts/*/submit-review")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                introduction.draftWritesEnabled() && introduction.reviewWorkflowEnabled()
                                && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/introduction-reviews/mine")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                introduction.draftWritesEnabled() && introduction.reviewWorkflowEnabled()
                                && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/introduction-reviews/pending")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                introduction.draftWritesEnabled() && introduction.reviewWorkflowEnabled()
                                && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.review".equals(a.getAuthority()))))
                        // R2E: explicit read-only maker scope, no POST final registration route.
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/introduction-reviews/*/finalization-readiness")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                introduction.draftWritesEnabled() && introduction.reviewWorkflowEnabled()
                                && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/introduction-reviews/*/draft")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                introduction.draftWritesEnabled() && introduction.reviewWorkflowEnabled()
                                && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.review".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/introduction-reviews/*/decision")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                introduction.draftWritesEnabled() && introduction.reviewWorkflowEnabled()
                                && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.review".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/introduction-drafts")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                introduction.draftWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/introduction-drafts/readiness")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/introduction-drafts/*")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                introduction.draftWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals", "/api/v1/collaterals/**")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(decoder.getObject())));
        return http.build();
    }
}
