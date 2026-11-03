package com.behsazan.corebanking.collateral.reference;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Independent, fail-closed collateral reference access; does not inherit the prototype SYSTEM account. */
@ConfigurationProperties(prefix = "core-banking.collateral.reference")
public record CollateralReferenceProperties(
        @DefaultValue("false") boolean readsEnabled,
        String issuer,
        String jwksUri,
        String audience,
        @DefaultValue("3600") long maxTokenLifetimeSeconds,
        String jdbcUrl,
        String jdbcUsername,
        String jdbcPassword,
        @DefaultValue("false") boolean writesEnabled,
        String writerJdbcUrl,
        String writerJdbcUsername,
        String writerJdbcPassword) {

    public void requireSafeConfig() {
        if (!readsEnabled && !writesEnabled) return;
        if (!readsEnabled || !https(issuer) || !https(jwksUri) || !present(audience)
                || maxTokenLifetimeSeconds < 60 || maxTokenLifetimeSeconds > 3600
                || !oracle(jdbcUrl) || !allowedAccount(jdbcUsername) || !present(jdbcPassword)) {
            throw new IllegalStateException("CLT_REFERENCE_READ_CONFIG_DENIED");
        }
        if (writesEnabled && (!oracle(writerJdbcUrl) || !allowedAccount(writerJdbcUsername)
                || !present(writerJdbcPassword) || writerJdbcUsername.equalsIgnoreCase(jdbcUsername))) {
            throw new IllegalStateException("CLT_REFERENCE_WRITE_CONFIG_DENIED");
        }
    }
    private static boolean oracle(String value) {
        return present(value) && value.startsWith("jdbc:oracle:thin:");
    }
    private static boolean allowedAccount(String value) {
        if (!present(value) || !value.strip().matches("[A-Za-z][A-Za-z0-9_#$]{0,29}")) return false;
        String username = value.strip();
        return !username.equalsIgnoreCase("SYSTEM") && !username.equalsIgnoreCase("SYS")
                && !username.equalsIgnoreCase("DBA") && !username.equalsIgnoreCase("CLT");
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
