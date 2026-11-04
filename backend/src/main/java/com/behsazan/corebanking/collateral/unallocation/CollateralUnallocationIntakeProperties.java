package com.behsazan.corebanking.collateral.unallocation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Independent staging-only writer; default false; never grant CLT base table DML. */
@ConfigurationProperties(prefix="core-banking.collateral.unallocation.intake")
public record CollateralUnallocationIntakeProperties(
        @DefaultValue("false") boolean requestWritesEnabled,
        String writerJdbcUrl,String writerJdbcUsername,String writerJdbcPassword) {
    public void requireSafeWriter() {
        if(!requestWritesEnabled())throw new IllegalStateException("CLT_R5B_INTAKE_DISABLED");
        if(writerJdbcUrl()==null||!writerJdbcUrl().startsWith("jdbc:oracle:thin:")
            ||writerJdbcUsername()==null
            ||!writerJdbcUsername().matches("[A-Za-z][A-Za-z0-9_#$]{0,29}")
            ||writerJdbcPassword()==null||writerJdbcPassword().isBlank())
            throw new IllegalStateException("CLT_R5B_WRITER_CONFIG_INVALID");
        String name=writerJdbcUsername().toUpperCase(java.util.Locale.ROOT);
        if(java.util.Set.of("CLT","SYS","SYSTEM","DBA","SYSDBA").contains(name))
            throw new IllegalStateException("CLT_R5B_PRIVILEGED_ACCOUNT_DENIED");
    }
}
