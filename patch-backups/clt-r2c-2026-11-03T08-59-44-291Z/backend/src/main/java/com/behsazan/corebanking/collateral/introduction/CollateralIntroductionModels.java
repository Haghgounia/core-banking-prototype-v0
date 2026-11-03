package com.behsazan.corebanking.collateral.introduction;

import java.util.List;

/** Draft intake is not a registered COLLATERAL; the registration number is never issued here. */
public final class CollateralIntroductionModels {
    private CollateralIntroductionModels() {}

    public record AttributeInput(String attributeId, String value) {}
    public record Request(String partyId, String collateralTypeId,
                          String estimateAmount, String currencyCode,
                          String validFrom, String validTo,
                          String depositNumber, String remarks,
                          List<AttributeInput> attributes) {}
    public record Validation(boolean eligibleForDraft, boolean eligibleForFinalRegistration,
                             List<String> blockers, String partyId, String collateralTypeId,
                             int configuredAttributeCount, String state) {}
    public record Draft(String draftId, String status, String partyId, String collateralTypeId,
                        String estimateAmount, String currencyCode, String validFrom,
                        String validTo, String depositNumber, String remarks,
                        List<AttributeInput> attributes, String branchCode,
                        String createdAt, String recordVersion, String source) {}
    public record Readiness(boolean registryReadsEnabled, boolean draftWritesEnabled,
                            boolean finalRegistrationEnabled, boolean migrationAutomatic,
                            String stage, String numberPolicy) {}
}
