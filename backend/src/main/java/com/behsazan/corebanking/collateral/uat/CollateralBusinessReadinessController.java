package com.behsazan.corebanking.collateral.uat;

import com.behsazan.corebanking.collateral.registry.CollateralRegistryPolicy;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Collateral BUC/SUC dictionary probe on the same application DataSource.
 * GET performs no DML/DDL and never evaluates business acceptance or bypasses bank flags.
 */
@RestController
@RequestMapping("/api/v1/collaterals/uat")
public class CollateralBusinessReadinessController {
    private final JdbcTemplate jdbc;
    private final Environment env;

    public CollateralBusinessReadinessController(DataSource dataSource, Environment environment) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(10);
        this.env = environment;
    }

    public record Readiness(String schema, String checkedAt, String dictionaryState,
            boolean acceptanceAttested, boolean localPrototype, Map<String,Boolean> operationalFlags,
            List<CollateralBusinessReadinessPolicy.CaseEvidence> cases) {}

    private static void requireReadPrincipal(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()
                || jwt.getToken().getSubject() == null || jwt.getToken().getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CLT_BUC_UAT_SIGNED_READ_PRINCIPAL_REQUIRED");
        }
        if (auth.getAuthorities().stream().noneMatch(a ->
                "SCOPE_clt.collateral.read".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_BUC_UAT_READ_SCOPE_REQUIRED");
        }
        Object branch = jwt.getToken().getClaims().get("branch_code");
        if (!(branch instanceof String value)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_BUC_UAT_BRANCH_REQUIRED");
        }
        try { CollateralRegistryPolicy.branch(value); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CLT_BUC_UAT_BRANCH_INVALID");
        }
    }

    @GetMapping("/business-readiness")
    public Readiness businessReadiness(Authentication auth) {
        requireReadPrincipal(auth);
        Set<String> existing = new LinkedHashSet<>();
        boolean metadataAvailable = false;
        try {
            // Oracle ALL_TABLES is authorization-filtered: no direct CLT DML or privileges required.
            List<String> rows = jdbc.queryForList("SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER='CLT'", String.class);
            if (rows != null) existing.addAll(rows);
            metadataAvailable = true;
        } catch (DataAccessException ex) {
            // Connectivity/privilege failure is UNKNOWN, never interpreted as 'no tables'.
            metadataAvailable = false;
        }
        LinkedHashMap<String,Boolean> flags = new LinkedHashMap<>();
        flags.put("allocationPosting", enabled("core-banking.collateral.posting.allocation-enabled"));
        flags.put("unallocationPosting", enabled("core-banking.collateral.posting.unallocation-enabled"));
        flags.put("reintroductionPosting", enabled("core-banking.collateral.reintroduction.posting-enabled"));
        flags.put("crossUnitRequests", enabled("core-banking.collateral.cross-unit.requests-enabled"));
        flags.put("crossUnitBankDecisions", enabled("core-banking.collateral.cross-unit.bank-decisions-enabled"));
        boolean local = enabled("core-banking.collateral.prototype.enabled")
                || enabled("CLT_LOCAL_UAT_READONLY_ENABLED");
        return new Readiness("CLT-BUC-READINESS-1", Instant.now().toString(),
                metadataAvailable ? "AVAILABLE" : "UNAVAILABLE", false, local,
                Map.copyOf(flags), CollateralBusinessReadinessPolicy.evaluate(existing, metadataAvailable));
    }
    private boolean enabled(String key) {return env.getProperty(key,Boolean.class,false);}
}
