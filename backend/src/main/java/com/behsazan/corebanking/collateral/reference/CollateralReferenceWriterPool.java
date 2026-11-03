package com.behsazan.corebanking.collateral.reference;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Isolated CLT reference governance DML connection; not a global Spring DataSource bean. */
public final class CollateralReferenceWriterPool implements AutoCloseable {
    private final HikariDataSource pool;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public CollateralReferenceWriterPool(CollateralReferenceProperties config) {
        config.requireSafeConfig();
        if (!config.writesEnabled()) throw new IllegalStateException("CLT_REFERENCE_WRITES_DISABLED");
        HikariConfig c = new HikariConfig();
        c.setDriverClassName("oracle.jdbc.OracleDriver");
        c.setJdbcUrl(config.writerJdbcUrl());
        c.setUsername(config.writerJdbcUsername());
        c.setPassword(config.writerJdbcPassword());
        c.setReadOnly(false);
        c.setAutoCommit(false);
        c.setMaximumPoolSize(2);
        c.setMinimumIdle(0);
        c.setConnectionTimeout(5000);
        c.setPoolName("clt-reference-governance");
        pool = new HikariDataSource(c);
        jdbc = new JdbcTemplate(pool);
        jdbc.setQueryTimeout(10);
        tx = new TransactionTemplate(new DataSourceTransactionManager(pool));
        tx.setTimeout(15);
    }
    public JdbcTemplate jdbc() { return jdbc; }
    public TransactionTemplate transaction() { return tx; }
    @Override public void close() { pool.close(); }
}
