package com.behsazan.corebanking.cif.isic.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class IsicModels {
    private IsicModels() {}

    public record ReleaseRow(
            Long isicReleaseId,
            String classificationCode,
            String revisionCode,
            String variantCode,
            String countryCode,
            String nameFa,
            String nameEn,
            String sourceAuthority,
            String sourceUri,
            LocalDate publicationDate,
            String datasetStatusCode,
            boolean current,
            boolean active,
            LocalDate validFrom,
            LocalDate validTo,
            Integer recordVersion,
            String createdBy,
            LocalDateTime createdDate,
            String lastModifiedBy,
            LocalDateTime lastModifiedDate
    ) {}

    public record ReleaseRequest(
            String classificationCode,
            String revisionCode,
            String variantCode,
            String countryCode,
            String nameFa,
            String nameEn,
            String sourceAuthority,
            String sourceUri,
            LocalDate publicationDate,
            String datasetStatusCode,
            Boolean current,
            Boolean active,
            LocalDate validFrom,
            LocalDate validTo,
            Integer recordVersion
    ) {}

    public record ReleaseLookup(Long value, String code, String label, boolean active, String datasetStatusCode) {}

    public record ActivityRow(
            Long isicActivityId,
            Long isicReleaseId,
            String releaseLabel,
            String isicCode,
            String baseIsicCode,
            String levelCode,
            String parentIsicCode,
            String sectionCode,
            String nameFa,
            String nameEn,
            String displayName,
            String translationStatus,
            boolean selectable,
            boolean active,
            LocalDate validFrom,
            LocalDate validTo,
            Integer sortOrder,
            Integer recordVersion,
            boolean hasChildren
    ) {}

    public record ActivityDetail(
            Long isicActivityId,
            Long isicReleaseId,
            String isicCode,
            String baseIsicCode,
            String levelCode,
            String parentIsicCode,
            String sectionCode,
            String nameFa,
            String nameEn,
            String descriptionFa,
            String descriptionEn,
            String inclusionsFa,
            String inclusionsEn,
            String exclusionsFa,
            String exclusionsEn,
            String translationStatus,
            boolean selectable,
            boolean active,
            LocalDate validFrom,
            LocalDate validTo,
            Integer sortOrder,
            Integer recordVersion,
            String createdBy,
            LocalDateTime createdDate,
            String lastModifiedBy,
            LocalDateTime lastModifiedDate
    ) {}

    public record ActivityRequest(
            Long isicReleaseId,
            String isicCode,
            String baseIsicCode,
            String levelCode,
            String parentIsicCode,
            String sectionCode,
            String nameFa,
            String nameEn,
            String descriptionFa,
            String descriptionEn,
            String inclusionsFa,
            String inclusionsEn,
            String exclusionsFa,
            String exclusionsEn,
            String translationStatus,
            Boolean selectable,
            Boolean active,
            LocalDate validFrom,
            LocalDate validTo,
            Integer sortOrder,
            Integer recordVersion
    ) {}

    public record ActivityLookup(Long value, String code, String label, String levelCode, String parentIsicCode) {}
}
