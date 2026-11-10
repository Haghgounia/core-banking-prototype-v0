package com.behsazan.corebanking.collateral.registration;

import java.util.List;
import java.util.Map;

/** Read-only Oracle registration evidence; never proof of bank acceptance. */
public final class CollateralRegistrationReadbackModels {
    private CollateralRegistrationReadbackModels() {}
    public record Check(String code, String state, String evidence) {}
    public record Report(String lookupMode, String reviewId, String collateralId,
            String registrationNumber, String state, boolean technicalLineageObserved,
            boolean bankAccepted, String source, List<Check> checks,
            Map<String,Object> profile, List<Map<String,Object>> attributeValues,
            List<Map<String,Object>> serialRanges) {}
}
