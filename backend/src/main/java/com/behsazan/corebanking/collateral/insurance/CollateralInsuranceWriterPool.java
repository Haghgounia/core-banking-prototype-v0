package com.behsazan.corebanking.collateral.insurance;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Physically independent, opt-in staging-only JDBC connection, transaction and audit. */
public final class CollateralInsuranceWriterPool implements AutoCloseable {
    private final HikariDataSource pool;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    CollateralInsuranceWriterPool(CollateralInsuranceProperties properties) {
        properties.requireSafeWriter();
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setJdbcUrl(properties.writerJdbcUrl());
        config.setUsername(properties.writerJdbcUsername());
        config.setPassword(properties.writerJdbcPassword());
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5000);
        config.setPoolName("clt-insurance-staging-only");
        pool = new HikariDataSource(config);
        jdbc = new JdbcTemplate(pool);
        jdbc.setQueryTimeout(10);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(pool));
        transaction.setTimeout(15);
    }
    public JdbcTemplate jdbc() { return jdbc; }
    public TransactionTemplate transaction() { return transaction; }
    @Override public void close() { pool.close(); }
}
