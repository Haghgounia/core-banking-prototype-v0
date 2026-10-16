package com.behsazan.corebanking.posting.configuration;

import com.behsazan.corebanking.posting.configuration.PostingConfigurationModels.PreviewRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Real pure-Java resolver tests. Nothing uses Oracle, network or a financial writer. */
class PostingConfigurationPreviewEngineTest {
    static PreviewRequest example() {
        return new PreviewRequest("DPS2", "POC_INTERNAL_TRANSFER_WITH_FEE", "TRANSFER_AND_FEE",
                "POC_TRANSFER_WITH_FEE_RULE", "POC_TRANSFER_WITH_FEE",
                "POC_FINANCIAL_BOOK", "POC_FAKE_BANK", "POC_COA", "POC_LEDGER",
                "POC_DEPOSIT_ACCOUNT_A", "POC_DEPOSIT_ACCOUNT_B", "POC_FEE_INTERNAL", "IRR", "IRR",
                "POC-DEPOSIT-PRODUCT-V1", "POC-BRANCH-01", "2026-10-15T12:00:00+03:30", "2026-10-15");
    }
    static Map<String, Object> row(Object... values) {
        var row = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) row.put((String) values[i], values[i+1]);
        return row;
    }
    static String hash() { return "a".repeat(64); }
    static String f() { return "2026-01-01T00:00:00.000000Z"; }
    static String e() { return "2027-01-01T00:00:00.000000Z"; }
    static Map<String, Object> publication(Object... additional) {
        var result = row("PUBLICATION_STATE", "PUBLISHED", "APPROVAL_REFERENCE", "POC_APPROVAL",
                "CONTENT_HASH", hash(), "FROM_UTC", f(), "UNTIL_UTC", e());
        result.putAll(row(additional));
        return result;
    }
    static final class Fake implements PostingConfigurationRepository {
        final Map<String, List<Map<String, Object>>> rows = new HashMap<>();
        final Map<String, Long> counts = new HashMap<>();
        final List<String> queries = new ArrayList<>();
        String failQuery;
        Fake() {
            var asset = row("IDENTIFIER", 600L, "ASSET_CODE", "IRR", "DECIMAL_PRECISION", 0L, "ASSET_STATE", "ACTIVE");
            put("ASSET", asset);
            put("BOOK", row("IDENTIFIER", 401L, "BOOK_CODE", "POC_FINANCIAL_BOOK", "LEGAL_ENTITY_REFERENCE", "POC_FAKE_BANK",
                    "BASE_CURRENCY_CODE", "IRR", "BOOK_STATE", "ACTIVE"));
            put("PERIOD", row("IDENTIFIER", 403L, "ACCOUNTING_BOOK_IDENTIFIER", 401L, "PERIOD_CODE", "POC-2026",
                    "PERIOD_STATE", "OPEN", "START_DATE", "2026-01-01", "END_DATE", "2026-12-31"));
            put("LEDGER", row("IDENTIFIER", 601L, "LEDGER_CODE", "POC_LEDGER", "LEGAL_ENTITY_REFERENCE", "POC_FAKE_BANK", "LEDGER_STATE", "ACTIVE"));
            put("RULE", publication("IDENTIFIER", 102L, "VERSION_NUMBER", 1L, "POSTING_RULE_IDENTIFIER", 101L));
            put("CONDITIONS", row("IDENTIFIER", 103L, "CONDITION_SEQUENCE", 1L, "ATTRIBUTE_NAME", "event_type",
                    "COMPARISON_OPERATOR", "EQUALS", "EXPECTED_VALUE", "POC_INTERNAL_TRANSFER_WITH_FEE"));
            put("ASSIGNMENT", publication("IDENTIFIER", 801L, "POSTING_RULE_VERSION_IDENTIFIER", 102L,
                    "POSTING_TEMPLATE_VERSION_IDENTIFIER", 202L, "ACCOUNTING_PURPOSE", "TRANSFER_AND_FEE"));
            var template = publication("IDENTIFIER", 202L, "VERSION_NUMBER", 1L, "POSTING_TEMPLATE_IDENTIFIER", 201L);
            template.remove("APPROVAL_REFERENCE");
            put("TEMPLATE", template);
            put("TEMPLATE_APPROVAL", row("IDENTIFIER", 901L, "SUBJECT_REFERENCE", "PST.POSTING_TEMPLATE_VERSIONS:202",
                    "APPROVAL_TYPE", "PUBLISH", "REQUESTED_BY", "MAKER", "APPROVED_BY", "CHECKER", "DECISION_STATE", "APPROVED", "DECIDED_UTC", f()));
            put("TEMPLATE_LINES", row("IDENTIFIER", 250L, "SEQUENCE_NUMBER", 1L, "SOURCE_ACCOUNT_ROLE", "CUSTOMER_SOURCE",
                            "DESTINATION_ACCOUNT_ROLE", "CUSTOMER_DESTINATION", "AMOUNT_COMPONENT", "TRANSFER_PRINCIPAL", "ASSET_POLICY", "SAME_AS_INSTRUCTION"),
                    row("IDENTIFIER", 251L, "SEQUENCE_NUMBER", 2L, "SOURCE_ACCOUNT_ROLE", "CUSTOMER_SOURCE",
                            "DESTINATION_ACCOUNT_ROLE", "FEE_CLEARING", "AMOUNT_COMPONENT", "FEE_AMOUNT", "ASSET_POLICY", "SAME_AS_INSTRUCTION"));
            put("MAPPING", publication("IDENTIFIER", 301L, "VERSION_NUMBER", 1L, "MAPPING_CODE", "POC_TRANSFER_WITH_FEE"));
            put("MAPPING_ENTRIES", row("IDENTIFIER", 311L, "SEQUENCE_NUMBER", 1L, "ACCOUNTING_COMPONENT", "SOURCE_TOTAL_DEBIT",
                            "ACCOUNT_ROLE", "CUSTOMER_DEPOSIT_LIABILITY_SOURCE", "POSTING_DIRECTION", "DEBIT"),
                    row("IDENTIFIER", 312L, "SEQUENCE_NUMBER", 2L, "ACCOUNTING_COMPONENT", "DESTINATION_PRINCIPAL_CREDIT",
                            "ACCOUNT_ROLE", "CUSTOMER_DEPOSIT_LIABILITY_DESTINATION", "POSTING_DIRECTION", "CREDIT"),
                    row("IDENTIFIER", 313L, "SEQUENCE_NUMBER", 3L, "ACCOUNTING_COMPONENT", "FEE_REVENUE_CREDIT",
                            "ACCOUNT_ROLE", "FEE_REVENUE", "POSTING_DIRECTION", "CREDIT"));
            put("CHART", row("IDENTIFIER", 402L, "ACCOUNTING_BOOK_IDENTIFIER", 401L, "CHART_CODE", "POC_COA",
                    "FROM_UTC", f(), "UNTIL_UTC", e()));
            put("ROLE_ASSIGNMENT", role(701L,"CUSTOMER_DEPOSIT_LIABILITY_SOURCE",501L),
                    role(702L,"CUSTOMER_DEPOSIT_LIABILITY_DESTINATION",501L), role(703L,"FEE_REVENUE",502L));
            put("GL", row("IDENTIFIER", 501L, "CHART_OF_ACCOUNTS_IDENTIFIER", 402L, "ACCOUNT_CODE", "GL_LIAB",
                            "ACCOUNT_TYPE", "LIABILITY", "ACCOUNT_STATE", "ACTIVE", "NORMAL_BALANCE_DIRECTION", "CREDIT"),
                    row("IDENTIFIER", 502L, "CHART_OF_ACCOUNTS_IDENTIFIER", 402L, "ACCOUNT_CODE", "GL_FEE",
                            "ACCOUNT_TYPE", "REVENUE", "ACCOUNT_STATE", "ACTIVE", "NORMAL_BALANCE_DIRECTION", "CREDIT"));
            put("BINDING", bind(620L, "DPS2", "POC_DEPOSIT_ACCOUNT_A", "CUSTOMER_SOURCE", "POC_SOURCE"),
                    bind(621L, "DPS2", "POC_DEPOSIT_ACCOUNT_B", "CUSTOMER_DESTINATION", "POC_DESTINATION"),
                    bind(622L, "SYSTEM", "POC_FEE_INTERNAL", "FEE_CLEARING", "POC_FEE_CLEARING"));
            put("POSITION", pos(631L, "POC_SOURCE"), pos(632L,"POC_DESTINATION"), pos(633L,"POC_FEE_CLEARING"));
        }
        @SafeVarargs final void put(String key, Map<String, Object>... values) {
            var copy = new ArrayList<Map<String,Object>>();
            for (Map<String,Object> value : values) copy.add(value);
            rows.put(key, copy);
        }
        Map<String, Object> first(String key) { return rows.get(key).get(0); }
        @Override public Long count(String table) {
            if ("ALL".equals(failQuery)) throw new IllegalStateException("fake select denied");
            return counts.getOrDefault(table, 1L);
        }
        @Override public List<Map<String, Object>> select(String query, Map<String, ?> params) {
            queries.add(query);
            if (query.equals(failQuery)) throw new IllegalStateException("fake SELECT fails");
            var source = rows.getOrDefault(query, List.of());
            return source.stream().filter(r -> switch (query) {
                case "GL" -> num(r,"IDENTIFIER") == num(params.get("glId"));
                case "BINDING" -> r.get("SOURCE_DOMAIN").equals(params.get("sourceDomain"))
                        && r.get("SOURCE_ACCOUNT_REFERENCE").equals(params.get("sourceReference"))
                        && r.get("ACCOUNT_ROLE").equals(params.get("role"))
                        && r.get("ASSET_CODE").equals(params.get("assetCode"));
                case "POSITION" -> r.get("POSITION_ACCOUNT_CODE").equals(params.get("positionCode"))
                        && num(r,"LEDGER_IDENTIFIER") == num(params.get("ledgerId"));
                default -> true;
            }).toList();
        }
        static long num(Map<String, Object> row, String key) { return num(row.get(key)); }
        static long num(Object n) { return ((Number)n).longValue(); }
        static Map<String, Object> role(long id, String role, long gl) {
            return publication("IDENTIFIER", id, "ACCOUNTING_MAPPING_VERSION_IDENTIFIER", 301L,
                    "ACCOUNTING_BOOK_IDENTIFIER", 401L, "GENERAL_LEDGER_ACCOUNT_IDENTIFIER", gl,
                    "ACCOUNT_ROLE", role, "CURRENCY_CODE", "IRR",
                    "PRODUCT_VERSION_REFERENCE", "POC-DEPOSIT-PRODUCT-V1", "BANK_UNIT_REFERENCE", "POC-BRANCH-01");
        }
        static Map<String, Object> bind(long id, String domain, String src, String role, String position) {
            return row("IDENTIFIER", id, "SOURCE_DOMAIN", domain, "SOURCE_ACCOUNT_REFERENCE", src,
                    "ACCOUNT_ROLE", role, "POSITION_ACCOUNT_REFERENCE", position, "ASSET_CODE", "IRR",
                    "FROM_UTC", f(), "UNTIL_UTC", e());
        }
        static Map<String, Object> pos(long id, String code) {
            return row("IDENTIFIER", id, "LEDGER_IDENTIFIER", 601L, "POSITION_ACCOUNT_CODE", code,
                    "ASSET_CODE", "IRR", "ACCOUNT_STATE", "ACTIVE", "ALLOW_NEGATIVE_BALANCE", 0L);
        }
    }
    static String code(Fake db) {
        var report = new PostingConfigurationPreviewEngine(db).preview(example());
        assertEquals("BLOCKED_CONFIGURATION", report.status());
        assertFalse(report.findings().isEmpty());
        assertFalse(report.realFinancialPostingAllowed());
        return report.findings().get(0).code();
    }
    @Test void happyPathPinsRuleTemplateMappingGlAndLedgerWithoutPosting() {
        var db = new Fake();
        var report = new PostingConfigurationPreviewEngine(db).preview(example());
        assertEquals("CONFIGURATION_PREVIEW_RESOLVED_NOT_AUTHORIZED", report.status());
        assertEquals("102", report.pinnedVersions().get("ruleVersionId"));
        assertEquals("202", report.pinnedVersions().get("templateVersionId"));
        assertEquals("801", report.pinnedVersions().get("ruleTemplateAssignmentId"));
        assertEquals("301", report.pinnedVersions().get("accountingMappingVersionId"));
        assertEquals("403", report.pinnedVersions().get("fiscalPeriodId"));
        assertEquals(3, report.positionBindings().size());
        assertEquals(3, report.accountingRoleBindings().size());
        assertTrue(report.findings().isEmpty());
        assertFalse(report.realFinancialPostingAllowed());
        assertTrue(report.readOnly());
        assertTrue(db.queries.contains("BINDING"));
        assertTrue(db.queries.contains("GL"));
    }
    @Test void statusOnEmptyConfigurationGivesThirteenExplicitBlockers() {
        var db = new Fake();
        for (var t : com.behsazan.corebanking.posting.readiness.PostingReadinessCatalog.CONFIGURATION_TABLES) db.counts.put(t, 0L);
        var engine = new PostingConfigurationPreviewEngine(db);
        assertEquals(13, engine.status().findings().size());
        assertEquals("BLOCKED_CONFIGURATION", engine.preview(example()).status());
        assertTrue(db.queries.isEmpty());
    }
    @Test void statusWithoutDataNeverImpliesPostingAuthorization() {
        var report = new PostingConfigurationPreviewEngine(new Fake()).status();
        assertEquals("CONFIG_PRESENT_NOT_POSTING_READY", report.status());
        assertFalse(report.realFinancialPostingAllowed());
    }
    @Test void statusCannotSilentlyIgnoreSelectFailure() {
        var db = new Fake(); db.failQuery = "ALL";
        assertEquals("BLOCKED_CONFIGURATION", new PostingConfigurationPreviewEngine(db).status().status());
        assertEquals(13, new PostingConfigurationPreviewEngine(db).status().findings().size());
    }
    @Test void rejectsTwoPublishedRules() { var db = new Fake(); db.rows.get("RULE").add(new LinkedHashMap<>(db.first("RULE"))); assertEquals("AMBIGUOUS_RULE", code(db)); }
    @Test void rejectsUnknownCondition() { var db = new Fake(); db.first("CONDITIONS").put("ATTRIBUTE_NAME", "unknown"); assertEquals("UNSUPPORTED_CONDITION", code(db)); }
    @Test void rejectsUnmatchedRule() { var db = new Fake(); db.first("CONDITIONS").put("EXPECTED_VALUE", "OTHER"); assertEquals("NO_MATCHING_PUBLISHED_RULE", code(db)); }
    @Test void rejectsDuplicateRuleConditionSequence() { var db = new Fake(); db.rows.get("CONDITIONS").add(new LinkedHashMap<>(db.first("CONDITIONS"))); assertEquals("INVALID_CONDITION_SEQUENCE", code(db)); }
    @Test void rejectsPublishedRuleWithoutApproval() { var db = new Fake(); db.first("RULE").put("APPROVAL_REFERENCE", null); assertEquals("MISSING_APPROVAL_REFERENCE", code(db)); }
    @Test void rejectsPublishedRuleWithoutValidHash() { var db = new Fake(); db.first("RULE").put("CONTENT_HASH", "1234"); assertEquals("INVALID_HASH_SHAPE", code(db)); }
    @Test void rejectsOverlappingRuleTemplateAssignments() { var db = new Fake(); db.rows.get("ASSIGNMENT").add(new LinkedHashMap<>(db.first("ASSIGNMENT"))); assertEquals("AMBIGUOUS_ASSIGNMENT", code(db)); }
    @Test void rejectsTemplateWithoutWorkflowApproval() { var db = new Fake(); db.rows.get("TEMPLATE_APPROVAL").clear(); assertEquals("MISSING_MAKER_CHECKER_APPROVAL", code(db)); }
    @Test void rejectsSameMakerChecker() { var db = new Fake(); db.first("TEMPLATE_APPROVAL").put("APPROVED_BY", "MAKER"); assertEquals("MISSING_MAKER_CHECKER_APPROVAL", code(db)); }
    @Test void rejectsFutureApproval() { var db = new Fake(); db.first("TEMPLATE_APPROVAL").put("DECIDED_UTC", "2027-01-01T00:00:00.000000Z"); assertEquals("MISSING_MAKER_CHECKER_APPROVAL", code(db)); }
    @Test void rejectsUnsupportedTemplateComponent() { var db = new Fake(); db.first("TEMPLATE_LINES").put("AMOUNT_COMPONENT", "OTHER"); assertEquals("UNSUPPORTED_TEMPLATE_LINE", code(db)); }
    @Test void rejectsMissingTemplateLine() { var db = new Fake(); db.rows.get("TEMPLATE_LINES").remove(1); assertEquals("UNSUPPORTED_TEMPLATE_SHAPE", code(db)); }
    @Test void rejectsWrongAccountingDirection() { var db = new Fake(); db.first("MAPPING_ENTRIES").put("POSTING_DIRECTION", "CREDIT"); assertEquals("UNSUPPORTED_MAPPING_ENTRY", code(db)); }
    @Test void rejectsMissingRoleAssignment() { var db = new Fake(); db.rows.get("ROLE_ASSIGNMENT").remove(1); assertEquals("MISSING_GL_ROLE_CUSTOMER_DEPOSIT_LIABILITY_DESTINATION", code(db)); }
    @Test void rejectsAmbiguousGlRoleAssignment() { var db = new Fake(); db.rows.get("ROLE_ASSIGNMENT").add(new LinkedHashMap<>(db.first("ROLE_ASSIGNMENT"))); assertEquals("AMBIGUOUS_ROLE_ASSIGNMENT", code(db)); }
    @Test void rejectsGlFromAnotherChart() { var db = new Fake(); db.first("GL").put("CHART_OF_ACCOUNTS_IDENTIFIER", 999L); assertEquals("MISSING_ACTIVE_GL_ACCOUNT", code(db)); }
    @Test void rejectsInactiveGl() { var db = new Fake(); db.first("GL").put("ACCOUNT_STATE", "DISABLED"); assertEquals("MISSING_ACTIVE_GL_ACCOUNT", code(db)); }
    @Test void rejectsClosedFiscalPeriod() { var db = new Fake(); db.first("PERIOD").put("PERIOD_STATE", "CLOSED"); assertEquals("MISSING_OPEN_PERIOD", code(db)); }
    @Test void rejectsOverlappingOpenFiscalPeriods() { var db = new Fake(); db.rows.get("PERIOD").add(new LinkedHashMap<>(db.first("PERIOD"))); assertEquals("AMBIGUOUS_PERIOD", code(db)); }
    @Test void rejectsWrongBookingDate() { var db = new Fake(); db.first("PERIOD").put("END_DATE", "2026-09-01"); assertEquals("MISSING_OPEN_PERIOD", code(db)); }
    @Test void rejectsInvalidAssetPrecision() { var db = new Fake(); db.first("ASSET").put("DECIMAL_PRECISION", 13L); assertEquals("INVALID_PRECISION", code(db)); }
    @Test void rejectsAmbiguousPositionBinding() { var db = new Fake(); db.rows.get("BINDING").add(new LinkedHashMap<>(db.first("BINDING"))); assertEquals("AMBIGUOUS_BINDING", code(db)); }
    @Test void rejectsMissingPosition() { var db = new Fake(); db.rows.get("POSITION").remove(0); assertEquals("MISSING_ACTIVE_POSITION", code(db)); }
    @Test void rejectsSamePhysicalPositionForTwoRoles() { var db = new Fake(); db.rows.get("BINDING").get(1).put("POSITION_ACCOUNT_REFERENCE", "POC_SOURCE"); assertEquals("DUPLICATE_OR_EMPTY_POSITION", code(db)); }
    @Test void rejectsFutureAssignment() { var db = new Fake(); db.first("ASSIGNMENT").put("FROM_UTC", "2027-01-01T00:00:00.000000Z"); db.first("ASSIGNMENT").put("UNTIL_UTC", null); assertEquals("MISSING_RULE_TEMPLATE_ASSIGNMENT", code(db)); }
    @Test void effectiveUntilIsExclusive() { var db = new Fake(); db.first("ASSIGNMENT").put("UNTIL_UTC", "2026-10-15T08:30:00.000000Z"); assertEquals("MISSING_RULE_TEMPLATE_ASSIGNMENT", code(db)); }
    @Test void invertedIntervalFailsClosed() { var db = new Fake(); db.first("ASSIGNMENT").put("UNTIL_UTC", "2025-01-01T00:00:00.000000Z"); assertEquals("INVALID_INTERVAL", code(db)); }
    @Test void unknownDbReadReturnsSafeFinding() { var db = new Fake(); db.failQuery = "GL"; assertEquals("SELECT_FAILED", code(db)); }
    @Test void capsResultSetsInsteadOfTruncatingMatches() { var db = new Fake(); for(int i=0;i<50;i++) db.rows.get("RULE").add(new LinkedHashMap<>(db.first("RULE"))); assertEquals("TOO_MANY_CANDIDATES", code(db)); }
    @Test void rejectsTimezoneNaiveRequest() { var r = example(); var bad = new PreviewRequest(r.sourceDomain(),r.eventType(),r.accountingPurpose(),r.ruleCode(),r.mappingCode(),r.bookCode(),r.legalEntityReference(),r.chartCode(),r.ledgerCode(),r.sourceAccountReference(),r.destinationAccountReference(),r.feeClearingSourceReference(),r.assetCode(),r.currencyCode(),r.productVersionReference(),r.bankUnitReference(),"2026-10-15T12:00:00","2026-10-15"); assertThrows(IllegalArgumentException.class, bad::validatedInstant); }
    @Test void rejectsNullAccountReferenceWithoutNpe() { var r = example(); var bad = new PreviewRequest(r.sourceDomain(),r.eventType(),r.accountingPurpose(),r.ruleCode(),r.mappingCode(),r.bookCode(),r.legalEntityReference(),r.chartCode(),r.ledgerCode(),null,r.destinationAccountReference(),r.feeClearingSourceReference(),r.assetCode(),r.currencyCode(),r.productVersionReference(),r.bankUnitReference(),r.effectiveAt(),r.bookingDate()); assertThrows(IllegalArgumentException.class, bad::validatedInstant); }
    @Test void rejectsCrossAssetDiagnostic() { var r = example(); var bad = new PreviewRequest(r.sourceDomain(),r.eventType(),r.accountingPurpose(),r.ruleCode(),r.mappingCode(),r.bookCode(),r.legalEntityReference(),r.chartCode(),r.ledgerCode(),r.sourceAccountReference(),r.destinationAccountReference(),r.feeClearingSourceReference(),r.assetCode(),"EUR",r.productVersionReference(),r.bankUnitReference(),r.effectiveAt(),r.bookingDate()); assertThrows(IllegalArgumentException.class, bad::validatedInstant); }
    @Test void rejectsSameSourceAndDestination() { var r = example(); var bad = new PreviewRequest(r.sourceDomain(),r.eventType(),r.accountingPurpose(),r.ruleCode(),r.mappingCode(),r.bookCode(),r.legalEntityReference(),r.chartCode(),r.ledgerCode(),r.sourceAccountReference(),r.sourceAccountReference(),r.feeClearingSourceReference(),r.assetCode(),r.currencyCode(),r.productVersionReference(),r.bankUnitReference(),r.effectiveAt(),r.bookingDate()); assertThrows(IllegalArgumentException.class, bad::validatedInstant); }
    @Test void rejectsUnapprovedEventType() { var r = example(); var bad = new PreviewRequest(r.sourceDomain(),"OTHER",r.accountingPurpose(),r.ruleCode(),r.mappingCode(),r.bookCode(),r.legalEntityReference(),r.chartCode(),r.ledgerCode(),r.sourceAccountReference(),r.destinationAccountReference(),r.feeClearingSourceReference(),r.assetCode(),r.currencyCode(),r.productVersionReference(),r.bankUnitReference(),r.effectiveAt(),r.bookingDate()); assertThrows(IllegalArgumentException.class, bad::validatedInstant); }
}
