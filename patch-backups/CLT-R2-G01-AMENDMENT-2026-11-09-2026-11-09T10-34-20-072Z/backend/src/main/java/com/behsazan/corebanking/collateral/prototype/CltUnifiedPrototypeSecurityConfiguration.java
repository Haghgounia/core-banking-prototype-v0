package com.behsazan.corebanking.collateral.prototype;

import com.behsazan.corebanking.collateral.localuat.CltLocalUatBranchResolver;
import com.behsazan.corebanking.collateral.localuat.CltLocalUatProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Local-only CLT prototype bridge. JWT is NOT supplied or required by the browser.
 * Legacy read services still consume an internally generated principal; no IAM API is affected.
 * Only allowlisted reference DML and no-write business previews are exposed.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
public class CltUnifiedPrototypeSecurityConfiguration {
    @Bean
    @Order(0)
    SecurityFilterChain cltUnifiedLocalChain(HttpSecurity http, Environment environment, DataSource applicationDataSource)
            throws Exception {
        // A no-token write mode must never bind to an externally reachable interface.
        String address = environment.getProperty("server.address", "");
        if (!"127.0.0.1".equals(address))
            throw new IllegalStateException("CLT_UNIFIED_REQUIRES_LOOPBACK_BINDING");
        if (environment.getProperty("CLT_LOCAL_UAT_READONLY_ENABLED", Boolean.class, false))
            throw new IllegalStateException("CLT_UNIFIED_CONFLICTS_WITH_LEGACY_LOCAL_UAT");
        var branch = new CltLocalUatBranchResolver(
                new CltLocalUatProperties(true, environment.getProperty("CLT_PROTOTYPE_BRANCH_CODE")),
                applicationDataSource);
        http.securityMatcher("/api/v1/collateral/**", "/api/v1/collaterals", "/api/v1/collaterals/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(new LocalCltAuthenticationFilter(branch), AnonymousAuthenticationFilter.class);
        http.authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/api/v1/collateral/reference/**",
                        "/api/v1/collaterals", "/api/v1/collaterals/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/collateral/reference/changes").denyAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/collateral/reference/*").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/v1/collateral/reference/*/*").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/collateral/reference/*/*").permitAll()
                // Draft-only staging table, never CLT.COLLATERAL or final registration.
                .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/introduction-drafts").permitAll()
                // POST allowlist: these methods are business validation/preview only, no permanent CLT DML.
                .requestMatchers(HttpMethod.POST, "/api/v1/collaterals/introduction-drafts/validate",
                        "/api/v1/collaterals/introduction-drafts/registration-preflight",
                        "/api/v1/collaterals/*/sponsors/validate",
                        "/api/v1/collaterals/*/insurance-policies/validate",
                        "/api/v1/collaterals/*/valuations/validate",
                        "/api/v1/collaterals/*/capacity/preview",
                        "/api/v1/collaterals/*/allocation-requests/preview",
                        "/api/v1/collaterals/*/allocations/*/unallocation/preview",
                        "/api/v1/collaterals/*/final-release/preview").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }

    static final class LocalCltAuthenticationFilter extends OncePerRequestFilter {
        private final CltLocalUatBranchResolver branches;
        LocalCltAuthenticationFilter(CltLocalUatBranchResolver branches) { this.branches = branches; }
        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            // Disallow remote network, proxies, DNS rebinding, and cross-origin writes.
            String host = req.getServerName();
            String origin = req.getHeader("Origin");
            if (!"127.0.0.1".equals(req.getRemoteAddr())
                    || !"127.0.0.1".equals(req.getLocalAddr())
                    || !("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host))
                    || req.getHeader("Forwarded") != null || req.getHeader("X-Forwarded-For") != null
                    || req.getHeader("X-Forwarded-Host") != null
                    || (origin != null && !List.of("http://localhost:8091", "http://127.0.0.1:8091",
                            "http://localhost:4200", "http://127.0.0.1:4200").contains(origin))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "CLT_UNIFIED_LOOPBACK_ONLY");
                return;
            }
            final String branch;
            if (req.getRequestURI().startsWith("/api/v1/collaterals")) {
                try { branch = branches.resolve(); }
                catch (RuntimeException ex) {
                    res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "CLT_BRANCH_DISCOVERY_UNAVAILABLE");
                    return;
                }
            } else {
                branch = "CLT_NO_DATA";
            }
            Instant now = Instant.now();
            // INTERNAL adapter for pre-existing CLT services only. No JWT is received or verified.
            Jwt internal = Jwt.withTokenValue("clt-unified-local-internal")
                    .header("alg", "none").subject("CLT_UNIFIED_LOCAL_DEV")
                    .issuedAt(now).expiresAt(now.plusSeconds(60))
                    .claim("branch_code", branch).claim("clt_local_prototype", true).build();
            var authentication = new JwtAuthenticationToken(internal, List.of(
                    new SimpleGrantedAuthority("SCOPE_clt.reference.read"),
                    new SimpleGrantedAuthority("SCOPE_clt.collateral.read"),
                    new SimpleGrantedAuthority("SCOPE_clt.collateral.introduce")), "CLT_UNIFIED_LOCAL_DEV");
            SecurityContext previous = SecurityContextHolder.getContext();
            SecurityContext scoped = SecurityContextHolder.createEmptyContext();
            scoped.setAuthentication(authentication);
            SecurityContextHolder.setContext(scoped);
            try { chain.doFilter(req, res); }
            finally { SecurityContextHolder.setContext(previous); }
        }
    }
}
