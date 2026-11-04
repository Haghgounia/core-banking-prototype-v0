package com.behsazan.corebanking.collateral.reference;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Intentionally not a DataSource/JdbcTemplate Spring bean; the existing core datasource stays unchanged. */
public final class CollateralReferenceReaderPool implements AutoCloseable {
    private final HikariDataSource pool;
    private final JdbcTemplate jdbc;
    public CollateralReferenceReaderPool(CollateralReferenceProperties properties) {
        properties.requireSafeConfig();
        HikariConfig c = new HikariConfig();
        c.setDriverClassName("oracle.jdbc.OracleDriver");
        c.setJdbcUrl(properties.jdbcUrl());
        c.setUsername(properties.jdbcUsername());
        c.setPassword(properties.jdbcPassword());
        c.setReadOnly(true);
        c.setAutoCommit(false);
        c.setMaximumPoolSize(3);
        c.setMinimumIdle(0);
        c.setConnectionTimeout(5000);
        c.setPoolName("clt-reference-select-only");
        pool = new HikariDataSource(c);
        jdbc = new JdbcTemplate(pool);
        jdbc.setQueryTimeout(10);
        jdbc.setFetchSize(50);
    }
    public JdbcTemplate jdbc() { return jdbc; }
    @Override public void close() { pool.close(); }
}
