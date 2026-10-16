package com.behsazan.corebanking.posting.configuration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** R1-D2 diagnostic contracts only; no command/commit/posting DTO. */
public final class PostingConfigurationModels {
    private PostingConfigurationModels() {}

    public record PreviewRequest(
            String sourceDomain, String eventType, String accountingPurpose,
            String ruleCode, String mappingCode,
            String bookCode, String legalEntityReference, String chartCode, String ledgerCode,
            String sourceAccountReference, String destinationAccountReference,
            String feeClearingSourceReference, String assetCode, String currencyCode,
            String productVersionReference, String bankUnitReference,
            String effectiveAt, String bookingDate) {
        private static final Pattern CODE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_./:-]{0,159}");

        public Instant validatedInstant() {
            for (String value : new String[] { sourceDomain, eventType, accountingPurpose, ruleCode,
                    mappingCode, bookCode, legalEntityReference, chartCode, ledgerCode,
                    sourceAccountReference, destinationAccountReference, feeClearingSourceReference,
                    assetCode, currencyCode, productVersionReference, bankUnitReference }) {
                if (value == null || !CODE.matcher(value).matches()) {
                    throw new IllegalArgumentException("Required scope is missing or malformed");
                }
            }
            if (sourceAccountReference.equals(destinationAccountReference)) {
                throw new IllegalArgumentException("Source and destination must differ");
            }
            if (!"DPS2".equals(sourceDomain) || !"POC_INTERNAL_TRANSFER_WITH_FEE".equals(eventType)) {
                throw new IllegalArgumentException("Only the isolated DPS2 transfer-with-fee diagnostic slice is supported");
            }
            if (!assetCode.equals(currencyCode) || !assetCode.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException("First slice requires an exact ISO3 same-asset currency");
            }
            try {
                LocalDate.parse(bookingDate); // exact calendar date (never inferred from clock)
                if (effectiveAt == null || !effectiveAt.matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(?:\\.\\d{1,9})?(?:Z|[+-]\\d\\d:\\d\\d)")) {
                    throw new IllegalArgumentException("Time zone offset is mandatory");
                }
                return OffsetDateTime.parse(effectiveAt).toInstant();
            } catch (DateTimeParseException | NullPointerException ex) {
                throw new IllegalArgumentException("Invalid effectiveAt or bookingDate", ex);
            }
        }
    }

    public record Finding(String stage, String code, String message) {}

    public record StatusReport(String phase, String status, boolean readOnly,
                               boolean realFinancialPostingAllowed,
                               Map<String, Long> configurationRowCounts,
                               List<Finding> findings, String warning, Instant collectedAt) {}

    public record PreviewReport(String phase, String status, boolean readOnly,
                                boolean realFinancialPostingAllowed,
                                Map<String, String> pinnedVersions,
                                List<Map<String, String>> positionBindings,
                                List<Map<String, String>> accountingRoleBindings,
                                List<Finding> findings, String warning, Instant collectedAt) {}

    public record ErrorReport(String phase, String status, String message) {}
}
