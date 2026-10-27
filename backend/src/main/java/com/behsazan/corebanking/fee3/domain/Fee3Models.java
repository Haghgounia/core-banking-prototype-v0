package com.behsazan.corebanking.fee3.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** FCE physical schema contract. No financial posting, approval, or runtime fee collection. */
public final class Fee3Models {
    private Fee3Models() {}

    public record Readiness(String schema, boolean readsEnabled, boolean draftWritesEnabled,
                            String bankZone, LocalDate oracleBusinessDate,
                            List<String> missingTables, long calcItemCount, long tariffVersionCount) {}
    public record Page<T>(List<T> items, long total, int page, int size) {}
    public record CatalogRow(long calcItemId, String code, String titleFa, String moduleCode,
                             String operationCode, String amountKindCode, String ownerDomainCode,
                             boolean enabled, long itemRecordVersion, Long tariffVersionId,
                             Integer latestVersionNo, String latestStatusCode, LocalDate latestValidFrom) {}
    public record Operation(String code, String module, String titleFa, List<String> allowedAmountKinds, boolean active) {}
    public record Formula(String code, String titleFa, String method, String expression,
                          String inputSchemaJson, String eligibleContextJson, boolean active) {}
    public record RefCode(String group, String code, String labelFa, boolean active, Integer sortNo) {}
    public record Source(long id, String documentNo, String documentTitleFa,
                         String sourceType, String validationStatus) {}
    public record ItemAggregate(Map<String,Object> item, List<VersionAggregate> versions) {}
    public record VersionAggregate(Map<String,Object> version, List<Map<String,Object>> rules,
                                   List<Map<String,Object>> scopes, List<Map<String,Object>> tests) {}

    public record ItemDraft(String title, String module, String operation, String amountKind,
                            String variant, String ownerDomain, String description) {}
    public record VersionDraft(LocalDate validFrom, LocalDate validTo, String origin,
                               Long sourceId, String method, String taxMode, String rounding,
                               String changeReason, Map<String,Object> financialPolicy,
                               String serviceCurrency, String feeCurrency,
                               Map<String,Object> uiContext) {}
    public record ScopeDraft(List<String> channels, List<String> customers, List<String> groups,
                             List<String> products, List<String> contracts, String currency) {}
    public record RuleDraft(int order, String method, String base, String rateSource,
                            BigDecimal rate, BigDecimal fixed, String collateral,
                            BigDecimal coverage, BigDecimal min, BigDecimal max,
                            String formulaCode, Map<String,Object> parameters) {}
    public record TestDraft(String title, Map<String,Object> request, BigDecimal expected) {}
    public record DraftRequest(Long calcItemId, Long tariffVersionId,
                               Long expectedItemRecordVersion, Long expectedVersionRecordVersion,
                               ItemDraft item, VersionDraft version, ScopeDraft scope,
                               List<RuleDraft> rules, List<TestDraft> tests) {}
    public record DraftReceipt(long calcItemId, long tariffVersionId, String calcCode,
                               int versionNo, long itemRecordVersion, long tariffRecordVersion,
                               String status, boolean published) {}
}
