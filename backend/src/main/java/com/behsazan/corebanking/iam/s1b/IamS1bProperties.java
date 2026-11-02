package com.behsazan.corebanking.iam.s1b;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Dedicated, opt-in IAM read plane: never uses the core application's SYSTEM datasource. */
@ConfigurationProperties(prefix = "core-banking.iam.s1b")
public record IamS1bProperties(
        boolean readsEnabled,
        String issuer,
        String jwksUri,
        String audience,
        String jdbcUrl,
        String jdbcUsername,
        String jdbcPassword) {

    public void requireSafeReadConfiguration() {
        if (!readsEnabled) return;
        if (!isHttps(issuer) || !isHttps(jwksUri) || !present(audience)
                || !present(jdbcUrl) || !jdbcUrl.startsWith("jdbc:oracle:thin:")
                || !present(jdbcUsername) || !present(jdbcPassword)) {
            throw new IllegalStateException("IAM_S1B_CONFIG_DENIED: explicit HTTPS issuer/JWKS, audience and isolated Oracle reader are mandatory");
        }
        String name = jdbcUsername.strip();
        if (name.equalsIgnoreCase("SYSTEM") || name.equalsIgnoreCase("SYS")
                || name.equalsIgnoreCase("IAM") || name.equalsIgnoreCase("DBA")) {
            throw new IllegalStateException("IAM_S1B_PRIVILEGED_DB_ACCOUNT_DENIED: use a separate SELECT-only Oracle principal");
        }
    }

    private static boolean present(String s) { return s != null && !s.isBlank(); }

    private static boolean isHttps(String s) {
        if (!present(s)) return false;
        try {
            java.net.URI uri = java.net.URI.create(s);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
