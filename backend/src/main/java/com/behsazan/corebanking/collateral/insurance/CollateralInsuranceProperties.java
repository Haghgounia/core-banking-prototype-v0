package com.behsazan.corebanking.collateral.insurance;

import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Strictly staging-only writer account: NO DML privilege on CLT.COLLATERAL_INSURANCE. */
@ConfigurationProperties(prefix = "core-banking.collateral.insurance")
public record CollateralInsuranceProperties(@DefaultValue("false") boolean requestWritesEnabled,
        String writerJdbcUrl, String writerJdbcUsername, String writerJdbcPassword) {
    public void requireSafeWriter() {
        if (!requestWritesEnabled || writerJdbcUrl == null || !writerJdbcUrl.startsWith("jdbc:oracle:thin:")
                || writerJdbcUsername == null
                || !writerJdbcUsername.matches("[A-Za-z][A-Za-z0-9_#$]{0,29}")
                || writerJdbcPassword == null || writerJdbcPassword.isBlank())
            throw new IllegalStateException("CLT_INSURANCE_WRITER_CONFIGURATION_INVALID");
        if (Set.of("SYS", "SYSTEM", "CLT", "DBA").contains(writerJdbcUsername.toUpperCase(Locale.ROOT)))
            throw new IllegalStateException("CLT_INSURANCE_PRIVILEGED_ACCOUNT_DENIED");
    }
}
