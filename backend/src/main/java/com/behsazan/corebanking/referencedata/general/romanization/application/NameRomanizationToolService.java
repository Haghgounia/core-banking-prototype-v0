package com.behsazan.corebanking.referencedata.general.romanization.application;

import com.behsazan.corebanking.shared.error.ReferenceValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class NameRomanizationToolService {
    private static final Pattern ORACLE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_$#]*");

    private final JdbcTemplate jdbcTemplate;
    private final String schemaName;

    public NameRomanizationToolService(
            JdbcTemplate jdbcTemplate,
            @Value("${core-banking.schemas.reference-data:GEO}") String schemaName
    ) {
        this.jdbcTemplate = jdbcTemplate;
        String normalizedSchema = schemaName == null ? "GEO" : schemaName.trim().toUpperCase(Locale.ROOT);
        if (!ORACLE_IDENTIFIER.matcher(normalizedSchema).matches()) {
            throw new IllegalStateException("Invalid Oracle schema configured for name romanization.");
        }
        this.schemaName = normalizedSchema;
    }

    @Transactional(readOnly = true)
    public NameRomanizationResult resolve(String persianName) {
        String input = persianName == null ? "" : persianName.trim();
        if (input.isBlank()) {
            throw validation("نام فارسی را وارد کنید.", "persianName");
        }
        if (input.length() > 400) {
            throw validation("طول نام فارسی نمی‌تواند بیش از ۴۰۰ نویسه باشد.", "persianName");
        }

        ResolverOutput resolver = callResolver(input);
        DictionaryMetadata metadata = findExactDictionaryMetadata(resolver.normalizedPersianName());

        String governanceStatus = metadata == null ? null : metadata.governanceStatusCode();
        boolean exactDictionaryMatch = metadata != null;
        boolean resolved = resolver.englishName() != null && !resolver.englishName().isBlank();

        return new NameRomanizationResult(
                input,
                resolver.normalizedPersianName(),
                resolver.englishName(),
                resolver.methodCode(),
                resolver.confidenceScore(),
                resolver.autoFillAllowed(),
                resolver.requiresReview(),
                governanceStatus,
                exactDictionaryMatch,
                resolved
        );
    }

    private ResolverOutput callResolver(String input) {
        String sql = "{call " + schemaName + ".PKG_NAME_ROMANIZATION.RESOLVE_NAME(?,?,?,?,?,?,?)}";
        return jdbcTemplate.execute(sql, (CallableStatementCallback<ResolverOutput>) cs -> {
            cs.setString(1, input);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.registerOutParameter(4, Types.VARCHAR);
            cs.registerOutParameter(5, Types.NUMERIC);
            cs.registerOutParameter(6, Types.NUMERIC);
            cs.registerOutParameter(7, Types.NUMERIC);
            cs.execute();

            Number confidence = (Number) cs.getObject(5);
            Number autoFill = (Number) cs.getObject(6);
            Number review = (Number) cs.getObject(7);

            return new ResolverOutput(
                    cs.getString(2),
                    cs.getString(3),
                    cs.getString(4),
                    confidence == null ? 0d : confidence.doubleValue(),
                    autoFill != null && autoFill.intValue() == 1,
                    review == null || review.intValue() == 1
            );
        });
    }

    private DictionaryMetadata findExactDictionaryMetadata(String normalizedPersianName) {
        if (normalizedPersianName == null || normalizedPersianName.isBlank()) return null;

        String sql = "SELECT GOVERNANCE_STATUS_CODE "
                + "FROM " + schemaName + ".NAME_ROMANIZATION_DICTIONARY "
                + "WHERE NORMALIZED_PERSIAN_NAME = ? AND IS_ACTIVE = 1";

        List<DictionaryMetadata> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DictionaryMetadata(rs.getString("GOVERNANCE_STATUS_CODE")),
                normalizedPersianName
        );
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static ReferenceValidationException validation(String message, String field) {
        return new ReferenceValidationException(message, Map.of(field, message));
    }

    private record ResolverOutput(
            String normalizedPersianName,
            String englishName,
            String methodCode,
            double confidenceScore,
            boolean autoFillAllowed,
            boolean requiresReview
    ) {}

    private record DictionaryMetadata(String governanceStatusCode) {}

    public record NameRomanizationResult(
            String persianName,
            String normalizedPersianName,
            String englishName,
            String methodCode,
            double confidenceScore,
            boolean autoFillAllowed,
            boolean requiresReview,
            String governanceStatusCode,
            boolean exactDictionaryMatch,
            boolean resolved
    ) {}
}
