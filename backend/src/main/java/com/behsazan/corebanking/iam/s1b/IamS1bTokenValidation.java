package com.behsazan.corebanking.iam.s1b;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Post-signature validation. Spring/Nimbus verifies RS256 signature, iss, exp, nbf before granting authority. */
public final class IamS1bTokenValidation {
    private static final Duration FUTURE_SKEW = Duration.ofSeconds(60);
    private IamS1bTokenValidation() { }

    public static OAuth2TokenValidator<Jwt> requiredClaims(String audience, long maxLifetimeSeconds) {
        return requiredClaims(audience, maxLifetimeSeconds, Clock.systemUTC());
    }

    /** Clock injection is for deterministic tests; production always uses the system UTC clock. */
    static OAuth2TokenValidator<Jwt> requiredClaims(String audience, long maxLifetimeSeconds, Clock clock) {
        if (audience == null || audience.isBlank() || maxLifetimeSeconds < 60 || maxLifetimeSeconds > 3600)
            throw new IllegalArgumentException("IAM_S1B_INVALID_TOKEN_CONTRACT");
        return jwt -> {
            Instant issuedAt = jwt.getIssuedAt(), expiresAt = jwt.getExpiresAt();
            if (jwt.getAudience() == null || !jwt.getAudience().contains(audience)
                    || jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || issuedAt == null || expiresAt == null
                    || issuedAt.isAfter(clock.instant().plus(FUTURE_SKEW))
                    || !expiresAt.isAfter(issuedAt)
                    || Duration.between(issuedAt, expiresAt).compareTo(Duration.ofSeconds(maxLifetimeSeconds)) > 0
                    || !IamS1bTokenClaimPolicy.validClaims(jwt.getClaims())) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "IAM access token contract was not satisfied", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }
}
