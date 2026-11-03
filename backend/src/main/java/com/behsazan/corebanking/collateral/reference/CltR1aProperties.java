package com.behsazan.corebanking.collateral.reference;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Fail-closed, isolated CLT read-plane. No reuse of the prototype SYSTEM connection. */
@ConfigurationProperties(prefix = "core-banking.collateral.r1a")
public record CltR1aProperties(
        @DefaultValue("false") boolean readsEnabled,
        String issuer,
        String jwksUri,
        String audience,
        @DefaultValue("3600") long maxTokenLifetimeSeconds,
        String jdbcUrl,
        String jdbcUsername,
        String jdbcPassword) {

    public void requireSafeConfig() {
        if (!readsEnabled) return;
        if (!https(issuer) || !https(jwksUri) || !present(audience)
                || maxTokenLifetimeSeconds < 60 || maxTokenLifetimeSeconds > 3600
                || !present(jdbcUrl) || !jdbcUrl.startsWith("jdbc:oracle:thin:")
                || !present(jdbcUsername) || !present(jdbcPassword)) {
            throw new IllegalStateException("CLT_R1A_CONFIG_DENIED: verified HTTPS JWT and isolated Oracle reader required");
        }
        String name = jdbcUsername.strip();
        if (!name.matches("[A-Za-z][A-Za-z0-9_#$]{0,29}")) {
            throw new IllegalStateException("CLT_R1A_INVALID_READER_USERNAME");
        }
        if (name.equalsIgnoreCase("SYSTEM") || name.equalsIgnoreCase("SYS")
                || name.equalsIgnoreCase("DBA") || name.equalsIgnoreCase("CLT")) {
            throw new IllegalStateException("CLT_R1A_PRIVILEGED_ACCOUNT_DENIED: use SELECT-only reader");
        }
    }
    private static boolean present(String value) { return value != null && !value.isBlank(); }
    private static boolean https(String value) {
        if (!present(value)) return false;
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException e) { return false; }
    }
}
