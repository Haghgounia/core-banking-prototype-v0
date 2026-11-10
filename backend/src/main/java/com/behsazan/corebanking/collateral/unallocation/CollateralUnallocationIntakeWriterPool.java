package com.behsazan.corebanking.collateral.unallocation;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Separate credentials: DML restricted by DBA to CLT_UNALLOC_REQ* staging tables. */
public final class CollateralUnallocationIntakeWriterPool implements AutoCloseable {
    private final DataSource source;
    private final boolean ownsSource;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    CollateralUnallocationIntakeWriterPool(CollateralUnallocationIntakeProperties properties) {
        properties.requireSafeWriter();
        HikariConfig c=new HikariConfig();
        c.setDriverClassName("oracle.jdbc.OracleDriver");
        c.setJdbcUrl(properties.writerJdbcUrl());
        c.setUsername(properties.writerJdbcUsername());
        c.setPassword(properties.writerJdbcPassword());
        c.setMaximumPoolSize(2);c.setMinimumIdle(0);c.setConnectionTimeout(5000);
        c.setReadOnly(false);c.setPoolName("clt-r5b-unallocation-stage-only");
        source=new HikariDataSource(c);ownsSource=true;
        jdbc=new JdbcTemplate(source);jdbc.setQueryTimeout(10);
        tx=new TransactionTemplate(new DataSourceTransactionManager(source));tx.setTimeout(20);
    }
    /** Local prototype reuses the existing application DataSource; no other credentials or pool. */
    CollateralUnallocationIntakeWriterPool(DataSource sharedSource) {
        if (sharedSource == null) throw new IllegalArgumentException("CLT_SHARED_DATASOURCE_REQUIRED");
        source = sharedSource; ownsSource = false;
        jdbc = new JdbcTemplate(sharedSource); jdbc.setQueryTimeout(10);
        tx = new TransactionTemplate(new DataSourceTransactionManager(sharedSource));
        tx.setTimeout(20);
    }
    public JdbcTemplate jdbc(){return jdbc;}
    public TransactionTemplate transaction(){return tx;}
    @Override public void close(){if(ownsSource)((HikariDataSource)source).close();}
}
