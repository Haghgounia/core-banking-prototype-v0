package com.behsazan.corebanking.collateral.valuation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Independent opt-in transaction and audit for staging requests; never touches base valuation DML. */
public final class CollateralValuationWriterPool implements AutoCloseable {
    private final HikariDataSource datasource;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    CollateralValuationWriterPool(CollateralValuationProperties properties) {
        properties.requireSafeWriter();
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setJdbcUrl(properties.writerJdbcUrl());
        config.setUsername(properties.writerJdbcUsername());
        config.setPassword(properties.writerJdbcPassword());
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5000);
        config.setPoolName("clt-valuation-staging-only");
        datasource = new HikariDataSource(config);
        jdbc = new JdbcTemplate(datasource);
        jdbc.setQueryTimeout(10);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(datasource));
        transaction.setTimeout(15);
    }
    public JdbcTemplate jdbc() { return jdbc; }
    public TransactionTemplate transaction() { return transaction; }
    @Override public void close() { datasource.close(); }
}
