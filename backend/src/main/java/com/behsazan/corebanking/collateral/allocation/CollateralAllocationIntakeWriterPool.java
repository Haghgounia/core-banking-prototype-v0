package com.behsazan.corebanking.collateral.allocation;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** NEVER granted DML on CLT.COLLATERAL, CLT.COLLATERAL_ALLOCATION, or DPS2. */
public final class CollateralAllocationIntakeWriterPool implements AutoCloseable {
    private final DataSource source;
    private final boolean ownsSource;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    CollateralAllocationIntakeWriterPool(CollateralAllocationIntakeProperties properties) {
        properties.requireSafeWriter();
        HikariConfig c = new HikariConfig();
        c.setDriverClassName("oracle.jdbc.OracleDriver");
        c.setJdbcUrl(properties.writerJdbcUrl());
        c.setUsername(properties.writerJdbcUsername());
        c.setPassword(properties.writerJdbcPassword());
        c.setMaximumPoolSize(2); c.setMinimumIdle(0); c.setConnectionTimeout(5000);
        c.setReadOnly(false); c.setPoolName("clt-allocation-intake-staging-only");
        source = new HikariDataSource(c); ownsSource = true;
        jdbc = new JdbcTemplate(source); jdbc.setQueryTimeout(10);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setTimeout(20);
    }
    /** Local prototype reuses the existing application DataSource; no other credentials or pool. */
    CollateralAllocationIntakeWriterPool(DataSource sharedSource) {
        if (sharedSource == null) throw new IllegalArgumentException("CLT_SHARED_DATASOURCE_REQUIRED");
        source = sharedSource; ownsSource = false;
        jdbc = new JdbcTemplate(sharedSource); jdbc.setQueryTimeout(10);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(sharedSource));
        transaction.setTimeout(20);
    }
    public JdbcTemplate jdbc() { return jdbc; }
    public TransactionTemplate transaction() { return transaction; }
    @Override public void close() { if (ownsSource) ((HikariDataSource) source).close(); }
}
