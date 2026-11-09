package com.behsazan.corebanking.collateral.sponsor;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Separate, optionally activated Oracle account limited to sponsor staging tables. */
public final class CollateralSponsorWriterPool implements AutoCloseable {
    private final HikariDataSource pool; // null for shared application datasource
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    CollateralSponsorWriterPool(CollateralSponsorProperties properties) {
        properties.requireSafeWriter();
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setJdbcUrl(properties.writerJdbcUrl());
        config.setUsername(properties.writerJdbcUsername());
        config.setPassword(properties.writerJdbcPassword());
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5000);
        config.setPoolName("clt-sponsor-staging-only");
        pool = new HikariDataSource(config);
        jdbc = new JdbcTemplate(pool);
        jdbc.setQueryTimeout(10);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(pool));
        transaction.setTimeout(15);
    }
    /** Unified loopback mode: reuse the application Oracle datasource without another account. */
    CollateralSponsorWriterPool(javax.sql.DataSource applicationDataSource) {
        this.pool = null;
        this.jdbc = new JdbcTemplate(applicationDataSource);
        this.jdbc.setQueryTimeout(10);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(applicationDataSource));
        this.transaction.setTimeout(15);
    }
    public JdbcTemplate jdbc() { return jdbc; }
    public TransactionTemplate transaction() { return transaction; }
    @Override public void close() { if (pool != null) pool.close(); }
}
