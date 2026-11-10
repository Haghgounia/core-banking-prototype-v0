package com.behsazan.corebanking.collateral.workflow;

import com.behsazan.corebanking.collateral.sponsor.CollateralSponsorPolicy;
import com.behsazan.corebanking.collateral.insurance.CollateralInsuranceModels;
import com.behsazan.corebanking.collateral.insurance.CollateralInsurancePolicy;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationModels;
import com.behsazan.corebanking.collateral.valuation.CollateralValuationPolicy;
import com.behsazan.corebanking.collateral.workflow.CollateralWorkflowPolicy.Kind;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/** Canonical Stage payload hash, shared by the transition and pre-posting read model.
 * Never trust browser-submitted hash and never log payload/Party identifiers.
 */
final class CollateralStageFingerprint {
    private CollateralStageFingerprint() {}
    private static String s(Object value) { return value == null ? null : value.toString().trim(); }
    private static String n(Object value) {
        if (value == null) return null;
        return value instanceof BigDecimal b ? b.stripTrailingZeros().toPlainString() : value.toString();
    }
    private static String day(Object date) {
        if (date == null) return null;
        if (date instanceof Date value) return value.toLocalDate().toString();
        if (date instanceof Timestamp value) return value.toLocalDateTime().toLocalDate().toString();
        if (date instanceof LocalDate value) return value.toString();
        if (date instanceof LocalDateTime value) return value.toLocalDate().toString();
        throw new IllegalArgumentException("CLT_R3_WORKFLOW_ORACLE_DATE_INVALID");
    }
    static String digest(Kind kind, String collateralId, Map<String,Object> source) {
        return switch (kind) {
            case SPONSOR -> {
                var candidate = CollateralSponsorPolicy.candidate(n(source.get("PARTY_ID")),
                        s(source.get("DEPOSIT_NUMBER")), s(source.get("JUSTIFICATION")));
                yield CollateralSponsorPolicy.digest(collateralId + "\u001f" + candidate.canonical());
            }
            case INSURANCE -> {
                var request = new CollateralInsuranceModels.CandidateRequest(
                        s(source.get("INSURER_CODE")), n(source.get("INSURANCE_COMPANY_ID")),
                        n(source.get("COLLATERAL_INSURANCE_TYPE_ID")), s(source.get("DEPOSIT_NUMBER")),
                        s(source.get("INSURANCE_NUMBER")), n(source.get("INSURANCE_AMOUNT")),
                        n(source.get("INSURANCE_RECEIVED_AMOUNT")), n(source.get("TAX_AMOUNT")),
                        day(source.get("VALID_FROM")), day(source.get("VALID_TO")),
                        n(source.get("PREVIOUS_INSURANCE_ID")), s(source.get("CURRENCY_CODE")),
                        s(source.get("JUSTIFICATION")));
                var candidate = CollateralInsurancePolicy.candidate(request);
                yield CollateralInsurancePolicy.digest(collateralId + "\u001f" + candidate.canonical());
            }
            case VALUATION -> {
                var request = new CollateralValuationModels.CandidateRequest(
                        s(source.get("VALUATION_TYPE_CODE")), s(source.get("VALUATION_METHOD_CODE")),
                        day(source.get("VALUATION_DATE")), n(source.get("VALUATION_AMOUNT")),
                        s(source.get("CURRENCY_CODE")), n(source.get("FORCED_SALE_VALUE")),
                        n(source.get("ELIGIBLE_VALUE")), n(source.get("HAIRCUT_PERCENT")),
                        n(source.get("VALUER_PARTY_ID")), s(source.get("VALUER_NAME")),
                        s(source.get("VALUATION_REFERENCE")), day(source.get("EFFECTIVE_FROM")),
                        day(source.get("EFFECTIVE_TO")), n(source.get("SUPERSEDES_VALUATION_HISTORY_ID")),
                        s(source.get("JUSTIFICATION")));
                var candidate = CollateralValuationPolicy.normalize(request);
                yield CollateralValuationPolicy.digest(collateralId + "\u001f" + candidate.canonical());
            }
        };
    }
}
