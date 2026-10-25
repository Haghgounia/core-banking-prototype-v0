package com.behsazan.corebanking.organization.location.manage;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Typed canonical location input. Existing official/legacy rows are never rewritten. */
public final class OrganizationUnitLocationModels {
    private OrganizationUnitLocationModels() {}

    public record Readiness(boolean locationIdNullable, boolean canonicalManagedFlagPresent,
                            boolean usageColumnsPresent, boolean auditPresent, boolean geoTypePresent, boolean ready) {}

    public record EditInput(
            Long organizationUnitId, String locationRoleCode, LocalDate effectiveFrom, LocalDate effectiveTo,
            Long geoEntityId, String geoEntityTypeCode, BigDecimal latitude, BigDecimal longitude,
            String coordinateReferenceSystemCode, String locationDescriptionFa, String locationDescriptionEn,
            String addressTypeCode, String mainStreetFa, String mainStreetEn,
            String secondaryStreetFa, String secondaryStreetEn,
            String squareCrossroadFa, String squareCrossroadEn, String buildingNo, String postalCode,
            String addressFa, String addressEn, String additionalAddressFa, String additionalAddressEn,
            String unitPresenceCode, String addressUsageCode,
            String relocationReasonCode, String decisionReference, LocalDate decisionDate,
            Long expectedRowVersion
    ) {}

    public record LocationDetails(
            long organizationUnitLocationId, long organizationUnitId, String unitCode, String unitNameFa,
            Long legacyLocationId, Long legacyPostalAddressId, int legacyBackfillFlag,
            int canonicalManagedFlag, long rowVersion, EditInput values
    ) {}

    public record AuditEntry(long auditId, long organizationUnitLocationId, String actionCode,
                             String changeReference, String changedAt, String changedBy) {}
}
