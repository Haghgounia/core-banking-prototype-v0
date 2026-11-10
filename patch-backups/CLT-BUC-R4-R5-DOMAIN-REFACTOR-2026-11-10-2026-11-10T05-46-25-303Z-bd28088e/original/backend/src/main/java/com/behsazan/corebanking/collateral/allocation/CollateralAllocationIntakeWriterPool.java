package com.behsazan.corebanking.collateral.allocation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** NEVER granted DML on CLT.COLLATERAL, CLT.COLLATERAL_ALLOCATION, or DPS2. */
public final class CollateralAllocationIntakeWriterPool implements AutoCloseable {
    private final HikariDataSource source;
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
        source = new HikariDataSource(c);
        jdbc = new JdbcTemplate(source); jdbc.setQueryTimeout(10);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setTimeout(20);
    }
    public JdbcTemplate jdbc() { return jdbc; }
    public TransactionTemplate transaction() { return transaction; }
    @Override public void close() { source.close(); }
}
