package com.behsazan.corebanking.collateral.allocation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Separate opt-in, staging-table-only writer. Always defaults to DENY. */
@ConfigurationProperties(prefix = "core-banking.collateral.allocation.intake")
public record CollateralAllocationIntakeProperties(
        @DefaultValue("false") boolean requestWritesEnabled,
        String writerJdbcUrl, String writerJdbcUsername, String writerJdbcPassword) {
    public void requireSafeWriter() {
        if (!requestWritesEnabled()) throw new IllegalStateException("CLT_ALLOCATION_INTAKE_WRITES_DISABLED");
        if (writerJdbcUrl() == null || !writerJdbcUrl().startsWith("jdbc:oracle:thin:")
                || writerJdbcUsername() == null
                || !writerJdbcUsername().matches("[A-Za-z][A-Za-z0-9_#$]{0,29}")
                || writerJdbcPassword() == null || writerJdbcPassword().isBlank())
            throw new IllegalStateException("CLT_ALLOCATION_INTAKE_WRITER_CONFIGURATION_INVALID");
        String user = writerJdbcUsername().toUpperCase(java.util.Locale.ROOT);
        if (java.util.Set.of("CLT", "SYS", "SYSTEM", "DBA", "SYSDBA").contains(user))
            throw new IllegalStateException("CLT_ALLOCATION_INTAKE_PRIVILEGED_ACCOUNT_DENIED");
    }
}
