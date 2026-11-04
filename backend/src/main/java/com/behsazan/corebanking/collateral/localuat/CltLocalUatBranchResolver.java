package com.behsazan.corebanking.collateral.localuat;

import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.web.server.ResponseStatusException;

/**
 * R6H: picks ONE existing CLT branch on the server, without a user prompt.
 * This never broadens CLT repositories: every registry/report/detail SELECT remains branch-scoped.
 * Query runs lazily on first permitted GET so an unavailable Oracle cannot crash all Prototype modules.
 * Only for synthetic, isolated local DEV data; the app datasource is not independently SELECT-only.
 */
public final class CltLocalUatBranchResolver {
    static final String DISCOVERY_SQL =
            "SELECT MIN(BRANCH_CODE) FROM CLT.COLLATERAL WHERE BRANCH_CODE IS NOT NULL";
    static final String NO_DATA_BRANCH = "CLT_NO_DATA";
    private final CltLocalUatProperties properties;
    private final JdbcTemplate jdbc;
    private volatile String cachedBranch;

    public CltLocalUatBranchResolver(CltLocalUatProperties properties, DataSource dataSource) {
        this(properties, new JdbcTemplate(dataSource));
    }

    CltLocalUatBranchResolver(CltLocalUatProperties properties, JdbcTemplate jdbc) {
        this.properties = properties;
        this.jdbc = jdbc;
        this.jdbc.setQueryTimeout(5);
    }

    public String resolve() {
        if (!properties.enabled())
            throw new IllegalStateException("CLT_LOCAL_UAT_NOT_ENABLED");
        String fixed = properties.branchCode();
        if (fixed != null && !fixed.isBlank()) return valid(fixed);
        String cached = cachedBranch;
        if (cached != null) return cached;
        synchronized (this) {
            if (cachedBranch != null) return cachedBranch;
            try {
                String fromOracle = jdbc.queryForObject(DISCOVERY_SQL, String.class);
                cachedBranch = (fromOracle == null || fromOracle.isBlank())
                        ? NO_DATA_BRANCH : valid(fromOracle.strip());
                return cachedBranch;
            } catch (DataAccessException | IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "CLT_LOCAL_UAT_BRANCH_DISCOVERY_UNAVAILABLE", ex);
            }
        }
    }

    private static String valid(String branch) {
        return CollateralRegistryPolicy.branch(branch);
    }
}
