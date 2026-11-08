package com.behsazan.corebanking.collateral.introduction;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Writer credentials are isolated from core, reference governance and CIF connections. */
public final class CollateralIntroductionWriterPool implements AutoCloseable {
    private final HikariDataSource pool; // null when the application owns the shared datasource
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    CollateralIntroductionWriterPool(CollateralIntroductionProperties properties) {
        properties.requireSafeWriter();
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setJdbcUrl(properties.writerJdbcUrl());
        config.setUsername(properties.writerJdbcUsername());
        config.setPassword(properties.writerJdbcPassword());
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5000);
        config.setReadOnly(false);
        config.setPoolName("clt-introduction-draft-only");
        pool = new HikariDataSource(config);
        jdbc = new JdbcTemplate(pool);
        jdbc.setQueryTimeout(10);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(pool));
        transaction.setTimeout(15);
    }
    /** Unified local mode: same managed datasource and credentials as every other form. */
    CollateralIntroductionWriterPool(DataSource applicationDataSource) {
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
