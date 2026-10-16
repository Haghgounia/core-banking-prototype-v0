package com.behsazan.corebanking.posting.configuration;

import com.behsazan.corebanking.posting.configuration.PostingConfigurationModels.Finding;
import com.behsazan.corebanking.posting.configuration.PostingConfigurationModels.PreviewReport;
import com.behsazan.corebanking.posting.configuration.PostingConfigurationModels.PreviewRequest;
import com.behsazan.corebanking.posting.configuration.PostingConfigurationModels.StatusReport;
import com.behsazan.corebanking.posting.readiness.PostingReadinessCatalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * R1-D2 first vertical slice: deterministic read-only configuration preview.
 * THIS CLASS IS NOT A FINANCIAL POSTING ENGINE. No amounts, no writes, no commit.
 * All returned IDs are strings to avoid JSON precision loss for Oracle NUMBER(19).
 */
public final class PostingConfigurationPreviewEngine {
    public static final String WARNING = "CONFIGURATION PREVIEW ONLY. No funds movement, reserve, posting plan persistence, GL journal, "
            + "authorization, balance check or Commit. Published hash SHAPE is checked, not cryptographically verified; "
            + "bank-approved currency scaling, fiscal policy, ledger ownership, immutable publication and outbox are still required.";
    private static final String HEX_64 = "(?i)[0-9a-f]{64}";
    private static final int CAP = 51;

    private final PostingConfigurationRepository database;

    public PostingConfigurationPreviewEngine(PostingConfigurationRepository database) { this.database = database; }

    public StatusReport status() {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<Finding> findings = new ArrayList<>();
        for (String table : PostingReadinessCatalog.CONFIGURATION_TABLES) {
            try {
                Long count = database.count(table);
                if (count == null || count < 0) {
                    findings.add(new Finding("CONFIGURATION", "COUNT_UNKNOWN", table + " count was not returned"));
                } else {
                    counts.put(table, count);
                    if (count == 0) findings.add(new Finding("CONFIGURATION", "TABLE_EMPTY", table));
                }
            } catch (RuntimeException exception) {
                findings.add(new Finding("CONFIGURATION", "SELECT_FAILED", table + " is not readable; check grants/schema"));
            }
        }
        String state = findings.isEmpty() ? "CONFIG_PRESENT_NOT_POSTING_READY" : "BLOCKED_CONFIGURATION";
        return new StatusReport("R1-D2", state, true, false, Map.copyOf(counts), List.copyOf(findings), WARNING, Instant.now());
    }

    public PreviewReport preview(PreviewRequest request) {
        Instant at = request.validatedInstant();
        StatusReport status = status();
        if (!status.findings().isEmpty()) {
            return blocked(status.findings());
        }
        try {
            return resolve(request, at);
        } catch (ConfigurationFailure failure) {
            return blocked(List.of(new Finding(failure.stage, failure.code, failure.getMessage())));
        } catch (RuntimeException failure) {
            // Never leak SQL, credentials or table contents in HTTP diagnostics.
            return blocked(List.of(new Finding("ORACLE", "CONFIGURATION_READ_FAILED",
                    "Read-only query or configuration conversion failed; inspect backend logs securely")));
        }
    }

    private PreviewReport resolve(PreviewRequest request, Instant at) {
        var asset = one("ASSET", "MISSING_ASSET", select("ASSET", Map.of("assetCode", request.assetCode())),
                row -> "ACTIVE".equals(s(row, "ASSET_STATE")));
        long precision = id(asset, "DECIMAL_PRECISION");
        must(precision >= 0 && precision <= 12, "ASSET", "INVALID_PRECISION", "Asset scale outside conservative preview bounds");

        var book = one("BOOK", "MISSING_ACTIVE_BOOK", select("BOOK", Map.of("bookCode", request.bookCode(),
                "legalEntityReference", request.legalEntityReference())), row -> "ACTIVE".equals(s(row, "BOOK_STATE"))
                && request.currencyCode().equals(s(row, "BASE_CURRENCY_CODE")));
        long bookId = id(book, "IDENTIFIER");
        LocalDate bookingDate = LocalDate.parse(request.bookingDate());
        var period = one("PERIOD", "MISSING_OPEN_PERIOD", select("PERIOD", Map.of("bookId", bookId)), row -> {
            String start = s(row, "START_DATE"), end = s(row, "END_DATE");
            if (start == null || end == null) throw bad("PERIOD", "INCOMPLETE_PERIOD", "Fiscal period start/end is missing");
            LocalDate beginning = LocalDate.parse(start), ending = LocalDate.parse(end);
            must(!ending.isBefore(beginning), "PERIOD", "INVERTED_PERIOD", "Fiscal period end precedes start");
            return "OPEN".equals(s(row, "PERIOD_STATE"))
                    && !bookingDate.isBefore(beginning) && !bookingDate.isAfter(ending);
        });

        var ledger = one("LEDGER", "MISSING_ACTIVE_LEDGER", select("LEDGER", Map.of(
                "ledgerCode", request.ledgerCode(), "legalEntityReference", request.legalEntityReference())),
                row -> "ACTIVE".equals(s(row, "LEDGER_STATE")));
        long ledgerId = id(ledger, "IDENTIFIER");

        List<Map<String, Object>> versions = select("RULE", Map.of("ruleCode", request.ruleCode()));
        List<Map<String, Object>> matched = new ArrayList<>();
        for (var version : versions) {
            if (!publishedAt(version, at, true)) continue;
            must(id(version, "VERSION_NUMBER") > 0, "RULE", "INVALID_VERSION", "Published rule version must be positive");
            var conditions = select("CONDITIONS", Map.of("ruleId", id(version, "IDENTIFIER")));
            if (conditions.isEmpty()) continue;
            Set<Long> numbers = new LinkedHashSet<>();
            boolean passes = true;
            for (var condition : conditions) {
                long order = id(condition, "CONDITION_SEQUENCE");
                must(order > 0 && numbers.add(order), "RULE", "INVALID_CONDITION_SEQUENCE", "Rule condition order is invalid/duplicate");
                String name = s(condition, "ATTRIBUTE_NAME");
                String actual = scopeValue(request, name);
                must(actual != null && "EQUALS".equals(s(condition, "COMPARISON_OPERATOR")),
                        "RULE", "UNSUPPORTED_CONDITION", "Only known exact equality conditions are supported");
                if (!actual.equals(s(condition, "EXPECTED_VALUE"))) passes = false;
            }
            if (passes) matched.add(version);
        }
        var rule = one("RULE", "NO_MATCHING_PUBLISHED_RULE", matched, row -> true);
        long ruleId = id(rule, "IDENTIFIER");
        var assignment = one("ASSIGNMENT", "MISSING_RULE_TEMPLATE_ASSIGNMENT", select("ASSIGNMENT",
                        Map.of("ruleId", ruleId, "accountingPurpose", request.accountingPurpose())),
                row -> publishedAt(row, at, true));
        long assignmentId = id(assignment, "IDENTIFIER");
        long templateId = id(assignment, "POSTING_TEMPLATE_VERSION_IDENTIFIER");
        var template = one("TEMPLATE", "MISSING_PUBLISHED_TEMPLATE", select("TEMPLATE", Map.of("templateId", templateId)),
                row -> publishedAt(row, at, false));
        must(id(template, "VERSION_NUMBER") > 0, "TEMPLATE", "INVALID_VERSION", "Template version must be positive");
        String subject = "PST.POSTING_TEMPLATE_VERSIONS:" + templateId;
        one("TEMPLATE_APPROVAL", "MISSING_MAKER_CHECKER_APPROVAL", select("TEMPLATE_APPROVAL", Map.of("subjectReference", subject)),
                row -> "APPROVED".equals(s(row, "DECISION_STATE"))
                        && "PUBLISH".equals(s(row, "APPROVAL_TYPE"))
                        && nonblank(s(row, "REQUESTED_BY")) && nonblank(s(row, "APPROVED_BY"))
                        && !s(row, "REQUESTED_BY").equals(s(row, "APPROVED_BY"))
                        && occurredNotLater(s(row, "DECIDED_UTC"), at));
        validateTemplateLines(select("TEMPLATE_LINES", Map.of("templateId", templateId)));

        var mapping = one("MAPPING", "MISSING_PUBLISHED_MAPPING", select("MAPPING", Map.of("mappingCode", request.mappingCode())),
                row -> publishedAt(row, at, true));
        must(id(mapping, "VERSION_NUMBER") > 0, "MAPPING", "INVALID_VERSION", "Accounting mapping version must be positive");
        long mappingId = id(mapping, "IDENTIFIER");
        var entries = select("MAPPING_ENTRIES", Map.of("mappingId", mappingId, "eventType", request.eventType()));
        validateMappingEntries(entries);
        var chart = one("CHART", "MISSING_EFFECTIVE_CHART", select("CHART", Map.of("bookId", bookId,
                "chartCode", request.chartCode())), row -> effective(row, at, "CHART"));
        long chartId = id(chart, "IDENTIFIER");

        var roles = select("ROLE_ASSIGNMENT", Map.of("mappingId", mappingId, "bookId", bookId,
                "currencyCode", request.currencyCode(), "productVersionReference", request.productVersionReference(),
                "bankUnitReference", request.bankUnitReference()));
        List<Map<String, String>> roleBindings = new ArrayList<>();
        for (var entry : entries) {
            String role = s(entry, "ACCOUNT_ROLE");
            var binding = one("ROLE_ASSIGNMENT", "MISSING_GL_ROLE_" + role, roles,
                    row -> role.equals(s(row, "ACCOUNT_ROLE")) && publishedAt(row, at, true));
            long glId = id(binding, "GENERAL_LEDGER_ACCOUNT_IDENTIFIER");
            var gl = one("GL", "MISSING_ACTIVE_GL_ACCOUNT", select("GL", Map.of("glId", glId)),
                    row -> "ACTIVE".equals(s(row, "ACCOUNT_STATE"))
                            && id(row, "CHART_OF_ACCOUNTS_IDENTIFIER") == chartId);
            must(nonblank(s(gl, "ACCOUNT_TYPE")) && ("DEBIT".equals(s(gl, "NORMAL_BALANCE_DIRECTION"))
                    || "CREDIT".equals(s(gl, "NORMAL_BALANCE_DIRECTION"))),
                    "GL", "INVALID_GL_CLASSIFICATION", "GL account type/normal direction is incomplete");
            roleBindings.add(Map.of("role", role,
                    "postingDirection", s(entry, "POSTING_DIRECTION"),
                    "accountingComponent", s(entry, "ACCOUNTING_COMPONENT"),
                    "glAccountCode", s(gl, "ACCOUNT_CODE"),
                    "glAccountId", String.valueOf(glId),
                    "roleAssignmentId", String.valueOf(id(binding, "IDENTIFIER"))));
        }

        List<Map<String, String>> positionBindings = new ArrayList<>();
        String[] operationalRoles = { "CUSTOMER_SOURCE", "CUSTOMER_DESTINATION", "FEE_CLEARING" };
        String[] domains = { request.sourceDomain(), request.sourceDomain(), "SYSTEM" };
        String[] sources = { request.sourceAccountReference(), request.destinationAccountReference(), request.feeClearingSourceReference() };
        Set<String> positionCodes = new LinkedHashSet<>();
        for (int i = 0; i < operationalRoles.length; i++) {
            String role = operationalRoles[i];
            var binding = one("BINDING", "MISSING_POSITION_BINDING_" + role, select("BINDING", Map.of(
                    "sourceDomain", domains[i], "sourceReference", sources[i], "role", role,
                    "assetCode", request.assetCode())), row -> effective(row, at, "BINDING"));
            String code = s(binding, "POSITION_ACCOUNT_REFERENCE");
            must(nonblank(code) && positionCodes.add(code), "POSITION", "DUPLICATE_OR_EMPTY_POSITION",
                    "Operational roles must resolve to different nonempty positions");
            one("POSITION", "MISSING_ACTIVE_POSITION", select("POSITION", Map.of(
                    "ledgerId", ledgerId, "positionCode", code, "assetCode", request.assetCode())), row ->
                    "ACTIVE".equals(s(row, "ACCOUNT_STATE"))
                            && (id(row, "ALLOW_NEGATIVE_BALANCE") == 0 || id(row, "ALLOW_NEGATIVE_BALANCE") == 1));
            positionBindings.add(Map.of("role", role, "positionCode", code,
                    "accountBindingId", String.valueOf(id(binding, "IDENTIFIER"))));
        }

        Map<String, String> pinned = new LinkedHashMap<>();
        pinned.put("ruleVersionId", String.valueOf(ruleId));
        pinned.put("templateVersionId", String.valueOf(templateId));
        pinned.put("ruleTemplateAssignmentId", String.valueOf(assignmentId));
        pinned.put("accountingMappingVersionId", String.valueOf(mappingId));
        pinned.put("accountingBookId", String.valueOf(bookId));
        pinned.put("chartId", String.valueOf(chartId));
        pinned.put("fiscalPeriodId", String.valueOf(id(period, "IDENTIFIER")));
        pinned.put("ledgerId", String.valueOf(ledgerId));
        pinned.put("assetPrecisionInIntegerUnits", String.valueOf(precision));
        return new PreviewReport("R1-D2", "CONFIGURATION_PREVIEW_RESOLVED_NOT_AUTHORIZED", true, false,
                Map.copyOf(pinned), List.copyOf(positionBindings), List.copyOf(roleBindings), List.of(), WARNING, Instant.now());
    }

    private static String scopeValue(PreviewRequest r, String field) {
        return switch (field) {
            case "event_type" -> r.eventType();
            case "source_domain" -> r.sourceDomain();
            case "accounting_purpose" -> r.accountingPurpose();
            case "product_version_reference" -> r.productVersionReference();
            case "bank_unit_reference" -> r.bankUnitReference();
            case "asset_code" -> r.assetCode();
            case "currency_code" -> r.currencyCode();
            default -> null;
        };
    }

    private static void validateTemplateLines(List<Map<String, Object>> lines) {
        must(lines.size() == 2, "TEMPLATE_LINES", "UNSUPPORTED_TEMPLATE_SHAPE",
                "First diagnostic slice requires exactly two defined movements");
        String[][] expected = {
                {"CUSTOMER_SOURCE", "CUSTOMER_DESTINATION", "TRANSFER_PRINCIPAL"},
                {"CUSTOMER_SOURCE", "FEE_CLEARING", "FEE_AMOUNT"}
        };
        for (int i = 0; i < 2; i++) {
            var line = lines.get(i);
            must(id(line, "SEQUENCE_NUMBER") == i + 1
                            && expected[i][0].equals(s(line, "SOURCE_ACCOUNT_ROLE"))
                            && expected[i][1].equals(s(line, "DESTINATION_ACCOUNT_ROLE"))
                            && expected[i][2].equals(s(line, "AMOUNT_COMPONENT"))
                            && "SAME_AS_INSTRUCTION".equals(s(line, "ASSET_POLICY")),
                    "TEMPLATE_LINES", "UNSUPPORTED_TEMPLATE_LINE", "Template semantics differ from approved PoC shape");
        }
    }

    private static void validateMappingEntries(List<Map<String, Object>> entries) {
        must(entries.size() == 3, "MAPPING_ENTRIES", "UNSUPPORTED_MAPPING_SHAPE",
                "First diagnostic slice requires three GL component roles");
        String[][] expected = {
                {"SOURCE_TOTAL_DEBIT", "CUSTOMER_DEPOSIT_LIABILITY_SOURCE", "DEBIT"},
                {"DESTINATION_PRINCIPAL_CREDIT", "CUSTOMER_DEPOSIT_LIABILITY_DESTINATION", "CREDIT"},
                {"FEE_REVENUE_CREDIT", "FEE_REVENUE", "CREDIT"}
        };
        for (int i = 0; i < 3; i++) {
            var entry = entries.get(i);
            must(id(entry, "SEQUENCE_NUMBER") == i + 1
                            && expected[i][0].equals(s(entry, "ACCOUNTING_COMPONENT"))
                            && expected[i][1].equals(s(entry, "ACCOUNT_ROLE"))
                            && expected[i][2].equals(s(entry, "POSTING_DIRECTION")),
                    "MAPPING_ENTRIES", "UNSUPPORTED_MAPPING_ENTRY", "Account mapping shape differs from reviewed PoC contract");
        }
    }

    private boolean publishedAt(Map<String, Object> row, Instant at, boolean approvalReference) {
        if (!"PUBLISHED".equals(s(row, "PUBLICATION_STATE"))) return false;
        if (!effective(row, at, "PUBLICATION")) return false;
        must(nonblank(s(row, "CONTENT_HASH")) && s(row, "CONTENT_HASH").matches(HEX_64),
                "PUBLICATION", "INVALID_HASH_SHAPE", "Published row lacks a SHA-256-shaped content hash");
        if (approvalReference) must(nonblank(s(row, "APPROVAL_REFERENCE")),
                "PUBLICATION", "MISSING_APPROVAL_REFERENCE", "Published row lacks approval evidence reference");
        return true;
    }

    private static boolean effective(Map<String, Object> row, Instant at, String stage) {
        String fromValue = s(row, "FROM_UTC"), untilValue = s(row, "UNTIL_UTC");
        must(nonblank(fromValue), stage, "MISSING_EFFECTIVE_FROM", "Effective start must not be NULL");
        Instant from = parseUtc(fromValue, stage), until = untilValue == null ? null : parseUtc(untilValue, stage);
        must(until == null || until.isAfter(from), stage, "INVALID_INTERVAL", "Effective interval must be half-open and nonempty");
        return !from.isAfter(at) && (until == null || at.isBefore(until));
    }

    private static Instant parseUtc(String value, String stage) {
        try { return Instant.parse(value); }
        catch (RuntimeException error) { throw bad(stage, "BAD_TIMESTAMP", "Oracle UTC timestamp is not parseable"); }
    }

    private static boolean occurredNotLater(String value, Instant at) {
        return nonblank(value) && !parseUtc(value, "APPROVAL").isAfter(at);
    }

    private List<Map<String, Object>> select(String query, Map<String, ?> parameters) {
        List<Map<String, Object>> rows;
        try {
            rows = database.select(query, parameters);
        } catch (RuntimeException failure) {
            // Keep query identity for diagnostics, never expose SQL or connection secrets.
            throw bad(query, "SELECT_FAILED", "Read-only configuration lookup failed: " + query);
        }
        must(rows != null && rows.size() < CAP, query, "TOO_MANY_CANDIDATES",
                "Candidate set exceeds safe 50-row limit; narrow scope or use indexed resolver");
        return rows;
    }

    private static Map<String, Object> one(String stage, String missing, List<Map<String, Object>> rows,
                                           Predicate<Map<String, Object>> filter) {
        List<Map<String, Object>> matches = rows.stream().filter(filter).toList();
        if (matches.size() != 1) {
            throw bad(stage, matches.isEmpty() ? missing : "AMBIGUOUS_" + stage,
                    "Expected exactly one matching configuration row; found " + matches.size());
        }
        return matches.get(0);
    }

    private static long id(Map<String, Object> row, String key) {
        Object value = row.get(key);
        try {
            if (value instanceof BigDecimal decimal) return decimal.longValueExact();
            if (value instanceof Number number) return new BigDecimal(number.toString()).longValueExact();
            if (value instanceof String s) return new BigDecimal(s).longValueExact();
        } catch (ArithmeticException | NumberFormatException error) { /* bad field */ }
        throw bad("DATABASE", "INVALID_NUMBER", "Missing or non-integral identifier/scale/flag: " + key);
    }

    private static String s(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value == null ? null : value.toString();
    }

    private static boolean nonblank(String s) { return s != null && !s.isBlank(); }
    private static void must(boolean good, String stage, String code, String message) {
        if (!good) throw bad(stage, code, message);
    }
    private static ConfigurationFailure bad(String stage, String code, String message) {
        return new ConfigurationFailure(stage, code, message);
    }
    private static final class ConfigurationFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final String stage, code;
        ConfigurationFailure(String stage, String code, String message) {
            super(message); this.stage = stage; this.code = code;
        }
    }
    private static PreviewReport blocked(List<Finding> findings) {
        return new PreviewReport("R1-D2", "BLOCKED_CONFIGURATION", true, false,
                Map.of(), List.of(), List.of(), List.copyOf(findings), WARNING, Instant.now());
    }
}
