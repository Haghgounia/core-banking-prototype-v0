package com.behsazan.corebanking.collateral.localuat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

/**
 * CLT-only single-machine read-only test principal, built by the server.
 * No client JWT, header or branch selector is accepted. The internal Jwt-shaped
 * principal exists ONLY to reuse read-side service contracts without modifying IAM.
 */
public final class CltLocalUatAuthenticationFilter extends OncePerRequestFilter {
    private final CltLocalUatProperties properties;
    private final CltLocalUatBranchResolver branchResolver;

    public CltLocalUatAuthenticationFilter(CltLocalUatProperties properties,
            CltLocalUatBranchResolver branchResolver) {
        this.properties = properties;
        this.branchResolver = branchResolver;
    }

    /** Safe guard diagnostics only: never report credentials, IP values or branch. */
    private static String rejectionReason(HttpServletRequest request) {
        if (!"127.0.0.1".equals(request.getRemoteAddr())) return "REMOTE_NOT_LOOPBACK";
        if (!"127.0.0.1".equals(request.getLocalAddr())) return "SERVER_NOT_LOOPBACK";
        if (!("localhost".equalsIgnoreCase(request.getServerName())
                || "127.0.0.1".equals(request.getServerName()))) return "HOST_INVALID";
        if (request.getHeader("Forwarded") != null
                || request.getHeader("X-Forwarded-For") != null
                || request.getHeader("X-Forwarded-Host") != null) return "FORWARDED_HEADER_DENIED";
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        // CLT-R6L-T1: an earlier HTTP 403 must never be cached by the browser.
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        String denyReason = rejectionReason(request);
        if (denyReason != null) {
            response.setHeader("X-CLT-Local-UAT", denyReason);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CLT_LOCAL_UAT_LOOPBACK_ONLY");
            return;
        }
        response.setHeader("X-CLT-Local-UAT", "LOCAL_READ_ONLY_ACCEPTED");
        if (!"GET".equals(request.getMethod())) {
            // The SecurityFilterChain denies all non-GET methods.
            chain.doFilter(request, response);
            return;
        }
        // The resolver is called in a Servlet filter, outside MVC exception handling.
        // Convert an Oracle discovery failure to an explicit HTTP 503, never an unscoped fallback.
        final String serverBranch;
        try {
            serverBranch = branchResolver.resolve();
        } catch (ResponseStatusException ex) {
            response.setHeader("X-CLT-Local-UAT", "BRANCH_DISCOVERY_UNAVAILABLE");
            response.sendError(ex.getStatusCode().value(), "CLT_LOCAL_UAT_BRANCH_DISCOVERY_UNAVAILABLE");
            return;
        }
        Instant now = Instant.now();
        Jwt internal = Jwt.withTokenValue("clt-server-internal-local-uat-only")
                .header("alg", "none")
                .subject("clt-local-uat-readonly")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("branch_code", serverBranch)
                .claim("clt_local_uat", true)
                .build();
        var principal = new JwtAuthenticationToken(internal, List.of(
                new SimpleGrantedAuthority("SCOPE_clt.collateral.read"),
                new SimpleGrantedAuthority("SCOPE_clt.reference.read")),
                "clt-local-uat-readonly");
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext scoped = SecurityContextHolder.createEmptyContext();
        scoped.setAuthentication(principal);
        SecurityContextHolder.setContext(scoped);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
