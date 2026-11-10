package com.behsazan.corebanking.collateral.prototype;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/** Loopback-only analyst Stage identity. NEVER an attested consumer or financial approver. */
public final class CltAnalystStageIdentity {
    private CltAnalystStageIdentity() {}
    public static final String CLIENT = "CLT_LOCAL_ANALYST";
    public static JwtAuthenticationToken require(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()
                || !Boolean.TRUE.equals(jwt.getToken().getClaimAsBoolean("clt_local_prototype"))
                || !"CLT_UNIFIED_LOCAL_DEV".equals(jwt.getName())
                || !"clt-unified-local-internal".equals(jwt.getToken().getTokenValue())
                || jwt.getAuthorities().stream().noneMatch(a ->
                        "SCOPE_clt.collateral.read".equals(a.getAuthority())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "CLT_ANALYST_STAGE_REQUIRES_INTERNAL_LOOPBACK_PRINCIPAL");
        return jwt;
    }
    public static String clientId() { return CLIENT; }
    public static boolean consumerAttested() { return false; }
    public static boolean financialPostingAuthorized() { return false; }
}
