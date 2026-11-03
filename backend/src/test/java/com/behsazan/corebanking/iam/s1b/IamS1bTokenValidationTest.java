package com.behsazan.corebanking.iam.s1b;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.assertj.core.api.Assertions.assertThat;

/** Contract tests only: these do NOT replace real RS256+JWKS/IdP integration tests. */
class IamS1bTokenValidationTest {
    private final Instant now = Instant.parse("2026-11-02T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    private Jwt token(Instant issuedAt, Instant expiresAt, List<String> audience,
                      Object domains, Object contexts, String subject) {
        Jwt.Builder builder = Jwt.withTokenValue("test-only-jwt-not-cryptographically-signed")
                .header("alg", "RS256")
                .claim("aud", audience)
                .claim("iam_domains", domains)
                .claim("iam_contexts", contexts)
                .claim("sub", subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt);
        return builder.build();
    }

    private boolean allowed(Jwt token) {
        return !IamS1bTokenValidation.requiredClaims("core-iam-api", 1800, clock).validate(token).hasErrors();
    }

    @Test void boundedValidTokenPasses() {
        assertThat(allowed(token(now.minusSeconds(15), now.plusSeconds(900),List.of("core-iam-api"),
                List.of("DEMO_WF"),List.of("WORKFORCE"),"test-subject"))).isTrue();
    }

    @Test void wrongAudienceDenied() {
        assertThat(allowed(token(now.minusSeconds(1),now.plusSeconds(900),List.of("some-other-api"),
                List.of("DEMO_WF"),List.of("WORKFORCE"),"test-subject"))).isFalse();
    }

    @Test void excessiveLifetimeDenied() {
        assertThat(allowed(token(now.minusSeconds(3600),now.plusSeconds(900),List.of("core-iam-api"),
                List.of("DEMO_WF"),List.of("WORKFORCE"),"test-subject"))).isFalse();
    }

    @Test void futureIssuedAtDenied() {
        assertThat(allowed(token(now.plusSeconds(61),now.plusSeconds(900),List.of("core-iam-api"),
                List.of("DEMO_WF"),List.of("WORKFORCE"),"test-subject"))).isFalse();
    }

    @Test void missingGrantArraysDenied() {
        assertThat(allowed(token(now.minusSeconds(1),now.plusSeconds(900),List.of("core-iam-api"),
                "DEMO_WF",List.of("WORKFORCE"),"test-subject"))).isFalse();
    }

    @Test void scopeNotInTokenValidatorBecauseAuthorizationMustReturn403() {
        // This validator does not decide scope. Missing SCOPE_iam.users.read is an AuthZ 403, not 401.
        assertThat(allowed(token(now.minusSeconds(1),now.plusSeconds(900),List.of("core-iam-api"),
                List.of("DEMO_WF"),List.of("WORKFORCE"),"test-subject"))).isTrue();
    }

    @Test void emptyDomainGrantsRemainValidButHaveNoAuthorization() {
        assertThat(allowed(token(now.minusSeconds(1),now.plusSeconds(900),List.of("core-iam-api"),
                List.of(),List.of(),"test-subject"))).isTrue();
        assertThat(IamS1bPolicy.canRead("test-subject",List.of("iam.users.read"),List.of(),List.of(),
                "DEMO_WF","WORKFORCE")).isFalse();
    }
}
