package com.behsazan.corebanking.iam.s1b;

import com.behsazan.corebanking.iam.s1b.IamS1bModels.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** SQL identifiers are fixed, parameters are bound, and the datasource is SELECT-only. */
@Repository
@ConditionalOnProperty(prefix = "core-banking.iam.s1b", name = "reads-enabled", havingValue = "true")
public class IamS1bReadRepository {
    private final JdbcTemplate db;
    private static final Map<String, Set<String>> REQUIRED_COLUMNS = Map.of(
            "SECURITY_DOMAINS", Set.of("SECURITY_DOMAIN_ID", "DOMAIN_CODE", "IDENTITY_CONTEXT_CODE", "IS_ACTIVE"),
            "PRINCIPALS", Set.of("PRINCIPAL_ID", "PRINCIPAL_UID", "PRINCIPAL_TYPE_CODE",
                    "PRINCIPAL_STATUS_CODE", "IDENTITY_CONTEXT_CODE", "RECORD_VERSION"),
            "USER_ACCOUNTS", Set.of("USER_ACCOUNT_ID", "PRINCIPAL_ID", "SECURITY_DOMAIN_ID", "USERNAME",
                    "USERNAME_NORMALIZED", "ACCOUNT_STATUS_CODE", "RECORD_VERSION", "IDENTITY_CONTEXT_CODE"),
            "IDENTITY_LINKS", Set.of("IDENTITY_LINK_ID", "PRINCIPAL_ID", "SOURCE_SYSTEM_CODE",
                    "EXTERNAL_ENTITY_TYPE_CODE", "EXTERNAL_ENTITY_ID", "VALID_FROM", "VALID_TO", "RECORD_VERSION"));

    public IamS1bReadRepository(IamS1bReaderPool pool) { this.db = pool.jdbc(); }

    public boolean metadataReady() {
        Map<String, Set<String>> found = new HashMap<>();
        db.query("SELECT TABLE_NAME, COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER = 'IAM' "
                + "AND TABLE_NAME IN ('SECURITY_DOMAINS','PRINCIPALS','USER_ACCOUNTS','IDENTITY_LINKS')",
                (org.springframework.jdbc.core.RowCallbackHandler) row -> {
                    found.computeIfAbsent(row.getString(1), k -> new HashSet<>()).add(row.getString(2));
                });
        return REQUIRED_COLUMNS.entrySet().stream().allMatch(e ->
                found.getOrDefault(e.getKey(), Set.of()).containsAll(e.getValue()));
    }

    public Optional<Domain> domain(String code) {
        List<Domain> rows = db.query("SELECT SECURITY_DOMAIN_ID, DOMAIN_CODE, IDENTITY_CONTEXT_CODE, IS_ACTIVE "
                + "FROM IAM.SECURITY_DOMAINS WHERE DOMAIN_CODE = ?",
                (rs, i) -> new Domain(rs.getLong(1), rs.getString(2), rs.getString(3), "Y".equals(rs.getString(4))), code);
        return rows.stream().findFirst();
    }

    private static final String ACCOUNT_SELECT = "SELECT ua.USER_ACCOUNT_ID, ua.PRINCIPAL_ID, "
            + "RAWTOHEX(p.PRINCIPAL_UID) PRINCIPAL_UID_HEX, p.PRINCIPAL_TYPE_CODE, p.PRINCIPAL_STATUS_CODE, "
            + "ua.USERNAME, ua.USERNAME_NORMALIZED, ua.ACCOUNT_STATUS_CODE, ua.IDENTITY_CONTEXT_CODE, "
            + "ua.RECORD_VERSION ACCOUNT_VERSION, p.RECORD_VERSION PRINCIPAL_VERSION "
            + "FROM IAM.USER_ACCOUNTS ua JOIN IAM.PRINCIPALS p ON p.PRINCIPAL_ID = ua.PRINCIPAL_ID "
            + "AND p.IDENTITY_CONTEXT_CODE = ua.IDENTITY_CONTEXT_CODE "
            + "WHERE ua.SECURITY_DOMAIN_ID = ? AND ua.IDENTITY_CONTEXT_CODE = ? ";

    private static final RowMapper<Account> ACCOUNT_MAPPER = (rs, i) -> new Account(
            rs.getLong(1), rs.getLong(2), uuidFromRawHex(rs.getString(3)),
            rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
            rs.getString(9), rs.getLong(10), rs.getLong(11));

    public List<Account> accounts(Domain domain, String normalizedSearch, int offset, int limitPlusOne) {
        if (normalizedSearch == null) {
            return db.query(ACCOUNT_SELECT + "ORDER BY ua.USER_ACCOUNT_ID "
                            + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", ACCOUNT_MAPPER,
                    domain.securityDomainId(), domain.context(), offset, limitPlusOne);
        }
        return db.query(ACCOUNT_SELECT + "AND ua.USERNAME_NORMALIZED LIKE ? ESCAPE '!' "
                        + "ORDER BY ua.USER_ACCOUNT_ID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", ACCOUNT_MAPPER,
                domain.securityDomainId(), domain.context(), IamS1bPolicy.escapedLike(normalizedSearch), offset, limitPlusOne);
    }

    public Optional<Account> account(Domain domain, long id) {
        List<Account> rows = db.query(ACCOUNT_SELECT + "AND ua.USER_ACCOUNT_ID = ?", ACCOUNT_MAPPER,
                domain.securityDomainId(), domain.context(), id);
        return rows.stream().findFirst();
    }

    public boolean principalIsExclusiveToDomain(Domain domain, long principalId) {
        Integer count = db.queryForObject("SELECT COUNT(*) FROM IAM.USER_ACCOUNTS WHERE PRINCIPAL_ID = ?",
                Integer.class, principalId);
        Integer inDomain = db.queryForObject("SELECT COUNT(*) FROM IAM.USER_ACCOUNTS "
                        + "WHERE PRINCIPAL_ID = ? AND SECURITY_DOMAIN_ID = ? AND IDENTITY_CONTEXT_CODE = ?",
                Integer.class, principalId, domain.securityDomainId(), domain.context());
        return count != null && count.equals(1) && inDomain != null && inDomain.equals(1);
    }

    public List<IdentityLink> links(Domain domain, long principalId) {
        return db.query("SELECT l.IDENTITY_LINK_ID, l.PRINCIPAL_ID, l.SOURCE_SYSTEM_CODE, "
                + "l.EXTERNAL_ENTITY_TYPE_CODE, l.EXTERNAL_ENTITY_ID, "
                + "TO_CHAR(l.VALID_FROM,'YYYY-MM-DD\"T\"HH24:MI:SSTZH:TZM'), "
                + "TO_CHAR(l.VALID_TO,'YYYY-MM-DD\"T\"HH24:MI:SSTZH:TZM'), l.RECORD_VERSION "
                + "FROM IAM.IDENTITY_LINKS l JOIN IAM.PRINCIPALS p ON p.PRINCIPAL_ID = l.PRINCIPAL_ID "
                + "WHERE l.PRINCIPAL_ID = ? AND p.IDENTITY_CONTEXT_CODE = ? "
                + "ORDER BY l.IDENTITY_LINK_ID FETCH FIRST 101 ROWS ONLY",
                (rs, i) -> new IdentityLink(rs.getLong(1), rs.getLong(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getLong(8)),
                principalId, domain.context());
    }

    /** Oracle RAW(16) is NOT a textual GUID; present it in standard UUID syntax. */
    public static String uuidFromRawHex(String hex) {
        if (hex == null || !hex.matches("[0-9A-Fa-f]{32}"))
            throw new IllegalStateException("IAM_S1B_INVALID_PRINCIPAL_UID");
        String h = hex.toLowerCase(Locale.ROOT);
        return h.substring(0,8) + "-" + h.substring(8,12) + "-" + h.substring(12,16)
                + "-" + h.substring(16,20) + "-" + h.substring(20);
    }
}
