package com.behsazan.corebanking.iam.s1b;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Deliberately NOT a DataSource or JdbcTemplate Spring bean: preserves core auto-configuration. */
public final class IamS1bReaderPool implements AutoCloseable {
    private final HikariDataSource pool;
    private final JdbcTemplate jdbc;

    public IamS1bReaderPool(IamS1bProperties p) {
        p.requireSafeReadConfiguration();
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName("oracle.jdbc.OracleDriver");
        cfg.setJdbcUrl(p.jdbcUrl());
        cfg.setUsername(p.jdbcUsername());
        cfg.setPassword(p.jdbcPassword());
        cfg.setReadOnly(true);
        cfg.setAutoCommit(false);
        cfg.setMaximumPoolSize(3);
        cfg.setMinimumIdle(0);
        cfg.setPoolName("iam-read-only-s1b");
        cfg.setConnectionTimeout(5000);
        this.pool = new HikariDataSource(cfg);
        this.jdbc = new JdbcTemplate(pool);
        this.jdbc.setQueryTimeout(10);
    }

    public JdbcTemplate jdbc() { return jdbc; }

    @Override public void close() { pool.close(); }
}
