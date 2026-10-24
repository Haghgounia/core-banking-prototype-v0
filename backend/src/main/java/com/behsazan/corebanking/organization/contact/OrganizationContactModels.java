package com.behsazan.corebanking.organization.contact;

import java.time.LocalDate;

/** Public/organizational contact data, deliberately independent of party-owned CIF contacts. */
public final class OrganizationContactModels {
    private OrganizationContactModels() {}

    public record ContactRole(long contactRoleId, String roleCode, String nameFa) {}

    public record ContactInput(
            Long organizationUnitId,
            String contactTypeCode,
            String contactValue,
            Long contactRoleId,
            Boolean primaryFlag,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String displayNameFa,
            Long expectedRowVersion
    ) {}

    public record ContactRow(
            long organizationUnitContactPointId,
            long contactPointId,
            long organizationUnitId,
            String unitCode,
            String unitNameFa,
            String contactTypeCode,
            String contactValue,
            String displayNameFa,
            long contactRoleId,
            String contactRoleCode,
            String contactRoleNameFa,
            boolean primaryFlag,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String sourceCode,
            long rowVersion
    ) {}

    public record ContactReadiness(boolean masterPresent, boolean unitForeignKeyPresent,
                                   boolean servicePointForeignKeyPresent, boolean rowVersionPresent, boolean ready) {}
}
