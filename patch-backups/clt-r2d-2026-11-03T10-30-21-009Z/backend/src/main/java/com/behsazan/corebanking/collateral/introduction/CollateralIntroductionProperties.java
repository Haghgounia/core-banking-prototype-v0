package com.behsazan.corebanking.collateral.introduction;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** No database writer or bean is created by default. This never enables final registration. */
@ConfigurationProperties(prefix = "core-banking.collateral.introduction")
public record CollateralIntroductionProperties(
        @DefaultValue("false") boolean draftWritesEnabled,
        String writerJdbcUrl, String writerJdbcUsername, String writerJdbcPassword) {
    public void requireSafeWriter() {
        if (!draftWritesEnabled) throw new IllegalStateException("CLT_INTRODUCTION_DRAFT_WRITES_DISABLED");
        if (writerJdbcUrl == null || !writerJdbcUrl.startsWith("jdbc:oracle:thin:")
                || writerJdbcUsername == null || !writerJdbcUsername.matches("[A-Za-z][A-Za-z0-9_#$]{0,29}")
                || writerJdbcPassword == null || writerJdbcPassword.isBlank())
            throw new IllegalStateException("CLT_INTRODUCTION_WRITER_CONFIG_INVALID");
        String u = writerJdbcUsername.toUpperCase(java.util.Locale.ROOT);
        if (java.util.Set.of("SYSTEM", "SYS", "DBA", "CLT").contains(u))
            throw new IllegalStateException("CLT_INTRODUCTION_PRIVILEGED_ACCOUNT_DENIED");
    }
}
