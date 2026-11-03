package com.behsazan.corebanking.collateral.valuation;

import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** This account may have staging request/event grants only; base table write grants forbidden. */
@ConfigurationProperties(prefix = "core-banking.collateral.valuation")
public record CollateralValuationProperties(@DefaultValue("false") boolean requestWritesEnabled,
        String writerJdbcUrl, String writerJdbcUsername, String writerJdbcPassword) {
    public void requireSafeWriter() {
        if (!requestWritesEnabled || writerJdbcUrl == null || !writerJdbcUrl.startsWith("jdbc:oracle:thin:")
                || writerJdbcUsername == null
                || !writerJdbcUsername.matches("[A-Za-z][A-Za-z0-9_#$]{0,29}")
                || writerJdbcPassword == null || writerJdbcPassword.isBlank())
            throw new IllegalStateException("CLT_VALUATION_WRITER_CONFIGURATION_INVALID");
        if (Set.of("SYS", "SYSTEM", "CLT", "DBA").contains(writerJdbcUsername.toUpperCase(Locale.ROOT)))
            throw new IllegalStateException("CLT_VALUATION_PRIVILEGED_ACCOUNT_DENIED");
    }
}
