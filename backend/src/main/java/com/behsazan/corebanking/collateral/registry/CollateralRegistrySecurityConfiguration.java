package com.behsazan.corebanking.collateral.registry;

import com.behsazan.corebanking.collateral.reference.CollateralReferenceProperties;
import com.behsazan.corebanking.collateral.introduction.CollateralIntroductionProperties;
import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorProperties;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceProperties;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationProperties;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationIntakeProperties;
import com.behsazan.corebanking.collateral.allocation.CollateralAllocationCapacityLockProperties;
import com.behsazan.corebanking.collateral.unallocation.CollateralUnallocationIntakeProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
@ConditionalOnProperty(name = "CLT_LOCAL_UAT_READONLY_ENABLED",
        havingValue = "false", matchIfMissing = true)
@ConditionalOnExpression("'${core-banking.collateral.prototype.enabled:false}' == 'false'")
public class CollateralRegistrySecurityConfiguration {
    @Bean
    @Order(3)
    SecurityFilterChain collateralRegistrySecurity(HttpSecurity http,
            CollateralReferenceProperties reference,
            CollateralRegistryProperties registry,
            CollateralIntroductionProperties introduction,
            CollateralSponsorProperties sponsor,
            CollateralInsuranceProperties insurance,
            CollateralValuationProperties valuation,
            CollateralAllocationIntakeProperties allocationIntake,
            CollateralAllocationCapacityLockProperties capacityLock,
            CollateralUnallocationIntakeProperties unallocationIntake,
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
                        // G-01 amendment: stage is a separate table; decision is gated by reviewer scope.
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/amendments/readiness",
                                "/api/v1/collaterals/*/amendments")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/amendments")
                        .hasAuthority("SCOPE_clt.collateral.introduce")
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/amendments/*/decision")
                        .hasAuthority("SCOPE_clt.collateral.review")
                        // R5C: historical evidence and diagnostic preview only. NO /final-release commit route.
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/collaterals/*/final-release/readiness",
                                "/api/v1/collaterals/*/final-release/evidence",
                                "/api/v1/collaterals/*/final-release/status-history")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/collaterals/*/final-release/preview")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        // R5B: staged request register; no write to COLLATERAL_ALLOCATION or release.
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/collaterals/*/unallocation-requests/readiness")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/collaterals/*/unallocation-requests/journal")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                unallocationIntake.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.read".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/collaterals/*/allocations/*/unallocation/requests")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                unallocationIntake.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.unallocation.request".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/collaterals/*/allocations/*/unallocation/requests/*")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                unallocationIntake.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.unallocation.request".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/collaterals/*/allocations/*/unallocation/requests/*/cancel")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                unallocationIntake.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.unallocation.request".equals(a.getAuthority()))))
                        // R5A: history and analyst preflight read-only; service route has separate scope.
                        // NEVER use the broad GET catch-all to authorize service POST requests.
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/*/allocations",
                                "/api/v1/collaterals/*/allocations/readiness",
                                "/api/v1/collaterals/*/allocations/*/transactions")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/allocations/*/unallocation/preview")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/allocations/*/unallocation/service-preflight")
                        .hasAuthority("SCOPE_clt.unallocation.preflight")
                        // R4D: bounded, no-write Oracle parent-row lock DIAGNOSTIC only.
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/*/allocation-requests/lock/readiness")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/allocation-requests/lock/probes/*")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                capacityLock.probeEnabled() && allocationIntake.requestWritesEnabled()
                                && auth.get().getAuthorities().stream().anyMatch(a ->
                                "SCOPE_clt.allocation.request".equals(a.getAuthority()))))
                        // R4C request journal: service-only, default deny; NEVER allocation/hold writes.
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/allocation-requests/intake")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                allocationIntake.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.allocation.request".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/*/allocation-requests/intake/readiness")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/*/allocation-requests/intake/journal")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                allocationIntake.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.read".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/*/allocation-requests/intake/receipts/*")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                allocationIntake.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.allocation.request".equals(a.getAuthority()))))
                        // R4B: analyst preview has READ scope; no trusted consumer authority implied.
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/allocation-requests/preview")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        // Service-only preflight additionally verifies signed JWT client_id/domain/grant in service.
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/allocation-requests/service-preflight")
                        .hasAuthority("SCOPE_clt.allocation.preflight")
                        // R4A: what-if POST performs no mutation; explicit READ scope, not introduce.
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/capacity/preview")
                        .hasAuthority("SCOPE_clt.collateral.read")
                        // R3C: candidate validation is not a final approval or an allocation decision.
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/valuations/validate")
                        .hasAuthority("SCOPE_clt.collateral.introduce")
                        .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/*/valuations/requests")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                valuation.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
                        .requestMatchers(HttpMethod.GET, "/api/v1/collaterals/valuations/requests/mine")
                        .access((auth, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                                valuation.requestWritesEnabled() && auth.get().getAuthorities().stream()
                                .anyMatch(a -> "SCOPE_clt.collateral.introduce".equals(a.getAuthority()))))
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
