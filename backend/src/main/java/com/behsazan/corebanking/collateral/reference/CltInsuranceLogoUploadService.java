package com.behsazan.corebanking.collateral.reference;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Local prototype: controlled one-company upload into the existing CLT LOGO BLOB. */
@Service
@ConditionalOnProperty(prefix = "core-banking.collateral.prototype", name = "enabled", havingValue = "true")
public class CltInsuranceLogoUploadService {
    private static final String TABLE = "CLT.INSURANCE_COMPANY";
    private static final String UPLOADER = "CLT_MANUAL_LOGO_LOCAL";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public record Receipt(String companyId, String companyName, long logoBytes,
                          long recordVersion, String contentType, boolean persisted) { }
    private record Snapshot(String companyName, long version, long logoBytes) { }

    public CltInsuranceLogoUploadService(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.setQueryTimeout(12);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.setTimeout(12);
    }

    static long checkedId(String raw) {
        if (raw == null || !raw.matches("[1-9][0-9]{0,17}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLT_LOGO_INVALID_COMPANY_ID");
        try { return Long.parseLong(raw); }
        catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLT_LOGO_INVALID_COMPANY_ID");
        }
    }

    static String checkedImage(byte[] bytes) {
        try { return CltInsuranceImagePolicy.checkedContentType(bytes); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CLT_LOGO_INVALID_IMAGE");
        }
    }

    private Snapshot snapshot(long id) {
        List<Snapshot> matches = jdbc.query("SELECT INSURANCE_COMPANY_NAME, RECORD_VERSION, "
                + "NVL(DBMS_LOB.GETLENGTH(LOGO),0) AS LOGO_BYTES FROM " + TABLE
                + " WHERE INSURANCE_COMPANY_ID=?", (rs, n) -> new Snapshot(
                rs.getString("INSURANCE_COMPANY_NAME"), rs.getLong("RECORD_VERSION"),
                rs.getLong("LOGO_BYTES")), id);
        if (matches.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CLT_LOGO_COMPANY_NOT_FOUND");
        return matches.getFirst();
    }

    public Receipt upload(String rawId, long expectedVersion, byte[] bytes) {
        long id = checkedId(rawId);
        if (expectedVersion < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLT_LOGO_INVALID_VERSION");
        String type = checkedImage(bytes); // Reject HTML, SVG, oversized and undecodable images before SQL.
        Snapshot before = snapshot(id);
        if (before.logoBytes() != 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_LOGO_ALREADY_PRESENT");
        if (before.version() != expectedVersion)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_LOGO_STALE_VERSION");

        transaction.executeWithoutResult(status -> {
            int affected = jdbc.update(connection -> {
                var ps = connection.prepareStatement("UPDATE " + TABLE + " SET LOGO=?, "
                        + "UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=?, RECORD_VERSION=RECORD_VERSION+1 "
                        + "WHERE INSURANCE_COMPANY_ID=? AND RECORD_VERSION=? AND LOGO IS NULL");
                ps.setBinaryStream(1, new ByteArrayInputStream(bytes), bytes.length);
                ps.setString(2, UPLOADER);
                ps.setLong(3, id);
                ps.setLong(4, expectedVersion);
                return ps;
            });
            if (affected != 1)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CLT_LOGO_CONCURRENT_CHANGE");
            byte[] stored = blob(id);
            if (!MessageDigest.isEqual(sha256(bytes), sha256(stored)))
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CLT_LOGO_TRANSACTION_READBACK_FAILED");
        });
        // Independent read on a new connection AFTER commit, not an optimistic UI echo.
        byte[] postCommit = blob(id);
        Snapshot after = snapshot(id);
        if (!MessageDigest.isEqual(sha256(bytes), sha256(postCommit))
                || after.version() != expectedVersion + 1 || after.logoBytes() != bytes.length)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CLT_LOGO_POSTCOMMIT_VERIFY_FAILED");
        return new Receipt(rawId, after.companyName(), after.logoBytes(), after.version(), type, true);
    }

    private byte[] blob(long id) {
        List<byte[]> found = jdbc.query("SELECT LOGO FROM " + TABLE + " WHERE INSURANCE_COMPANY_ID=?",
                (rs, n) -> rs.getBytes(1), id);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static byte[] sha256(byte[] bytes) {
        if (bytes == null) return new byte[0];
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("CLT_SHA256_UNAVAILABLE", ex); }
    }
}
