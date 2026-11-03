package com.behsazan.corebanking.collateral.sponsor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.util.Set;
import java.util.Locale;

/** Independent staging writer: never authorizes inserts into CLT.COLLATERAL_SPONSOR. */
@ConfigurationProperties(prefix = "core-banking.collateral.sponsor")
public record CollateralSponsorProperties(@DefaultValue("false") boolean requestWritesEnabled,
        String writerJdbcUrl, String writerJdbcUsername, String writerJdbcPassword) {
    public void requireSafeWriter() {
        if (!requestWritesEnabled) throw new IllegalStateException("CLT_SPONSOR_REQUEST_WRITES_DISABLED");
        if (writerJdbcUrl == null || !writerJdbcUrl.startsWith("jdbc:oracle:thin:")
                || writerJdbcUsername == null
                || !writerJdbcUsername.matches("[A-Za-z][A-Za-z0-9_#$]{0,29}")
                || writerJdbcPassword == null || writerJdbcPassword.isBlank())
            throw new IllegalStateException("CLT_SPONSOR_WRITER_CONFIG_INVALID");
        if (Set.of("SYS", "SYSTEM", "CLT", "DBA").contains(writerJdbcUsername.toUpperCase(Locale.ROOT)))
            throw new IllegalStateException("CLT_SPONSOR_PRIVILEGED_ACCOUNT_DENIED");
    }
}
