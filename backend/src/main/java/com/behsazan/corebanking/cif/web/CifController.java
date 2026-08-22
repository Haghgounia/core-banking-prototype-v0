package com.behsazan.corebanking.cif.web;

import com.behsazan.corebanking.cif.application.CifService;
import com.behsazan.corebanking.cif.domain.CifModels.*;
import com.behsazan.corebanking.cif.document.DocumentStorageService;
import com.behsazan.corebanking.cif.document.DocumentStorageService.DocumentUploadResponse;
import com.behsazan.corebanking.cif.error.CifNotFoundException;
import com.behsazan.corebanking.cif.domain.CifModels.CifDashboardSummary;
import com.behsazan.corebanking.cif.domain.CifModels.ExternalInquiryRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyConsentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.CommunicationPreferenceRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyGeneralPreferenceRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyStatusChangeRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyMergeRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointAddressRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointRequest;
import com.behsazan.corebanking.cif.domain.CifModels.CreatePartyRequest;
import com.behsazan.corebanking.cif.domain.CifModels.FinancialProfileRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAssetLiabilityRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyEmploymentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIncomeSourceRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyLicenseRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyClassificationRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyRelationshipRequest;
import com.behsazan.corebanking.cif.domain.CifModels.BeneficialOwnershipRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAuthorityRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyRoleRequest;
import com.behsazan.corebanking.cif.domain.CifModels.KycCaseRequest;
import com.behsazan.corebanking.cif.domain.CifModels.OrganizationRequest;
import com.behsazan.corebanking.cif.domain.CifModels.Party360Response;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAddressRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyDocumentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIdentifierRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyNameRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyOnboardingRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartySummary;
import com.behsazan.corebanking.cif.domain.CifModels.PersonRequest;
import com.behsazan.corebanking.cif.domain.CifModels.RiskAssessmentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ScreeningResultRequest;
import com.behsazan.corebanking.cif.domain.CifModels.UpdatePartyRequest;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.LookupOption;
import com.behsazan.corebanking.shared.model.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cif")
public class CifController {
    private final CifService service;
    private final DocumentStorageService documentStorage;

    public CifController(CifService service, DocumentStorageService documentStorage) {
        this.service = service;
        this.documentStorage = documentStorage;
    }

    @GetMapping("/risk-models/{modelCode}/profile")
    RiskModelProfile riskModelProfile(@PathVariable String modelCode) {
        return service.riskModelProfile(modelCode);
    }

    @GetMapping("/parties")
    PageResponse<PartySummary> search(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String partyType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(text, partyType, status, page, size);
    }

    @GetMapping("/classification-values")
    List<LookupOption> classificationValues(
            @RequestParam String typeCode,
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return service.classificationValues(typeCode, text, limit);
    }

    @GetMapping("/parties/{partyId}")
    Party360Response find(@PathVariable long partyId) {
        return service.find(partyId);
    }

    @GetMapping("/parties/{partyId}/readiness")
    PartyReadinessSummary readiness(@PathVariable long partyId) {
        return service.readiness(partyId);
    }

    @PostMapping("/parties")
    ResponseEntity<Party360Response> createParty(
            @Valid @RequestBody CreatePartyRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor
    ) {
        Party360Response created = service.createParty(request, actor);
        return ResponseEntity.created(URI.create("/api/v1/cif/parties/" + created.party().partyId())).body(created);
    }

    @PostMapping("/parties/onboarding")
    ResponseEntity<Party360Response> onboardParty(
            @Valid @RequestBody PartyOnboardingRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor
    ) {
        Party360Response created = service.onboardParty(request, actor);
        return ResponseEntity.created(URI.create("/api/v1/cif/parties/" + created.party().partyId())).body(created);
    }

    @PutMapping("/parties/{partyId}")
    Party360Response updateParty(
            @PathVariable long partyId,
            @Valid @RequestBody UpdatePartyRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor
    ) {
        return service.updateParty(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/person")
    Party360Response upsertPerson(
            @PathVariable long partyId,
            @Valid @RequestBody PersonRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor
    ) {
        return service.upsertPerson(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/organization")
    Party360Response upsertOrganization(
            @PathVariable long partyId,
            @Valid @RequestBody OrganizationRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor
    ) {
        return service.upsertOrganization(partyId, request, actor);
    }

    @PostMapping("/parties/{partyId}/names")
    Party360Response createName(@PathVariable long partyId, @Valid @RequestBody PartyNameRequest request,
                                @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createName(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/names/{id}")
    Party360Response updateName(@PathVariable long partyId, @PathVariable long id,
                                @Valid @RequestBody PartyNameRequest request,
                                @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateName(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/names/{id}")
    Party360Response deleteName(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteName(partyId, id);
    }

    @PostMapping("/parties/{partyId}/identifiers")
    Party360Response createIdentifier(@PathVariable long partyId, @Valid @RequestBody PartyIdentifierRequest request,
                                      @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createIdentifier(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/identifiers/{id}")
    Party360Response updateIdentifier(@PathVariable long partyId, @PathVariable long id,
                                      @Valid @RequestBody PartyIdentifierRequest request,
                                      @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateIdentifier(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/identifiers/{id}")
    Party360Response deleteIdentifier(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteIdentifier(partyId, id);
    }

    @PostMapping("/parties/{partyId}/addresses")
    Party360Response createAddress(@PathVariable long partyId, @Valid @RequestBody PartyAddressRequest request,
                                   @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createAddress(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/addresses/{id}")
    Party360Response updateAddress(@PathVariable long partyId, @PathVariable long id,
                                   @Valid @RequestBody PartyAddressRequest request,
                                   @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateAddress(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/addresses/{id}")
    Party360Response deleteAddress(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteAddress(partyId, id);
    }

    @PostMapping("/parties/{partyId}/contacts")
    Party360Response createContact(@PathVariable long partyId, @Valid @RequestBody ContactPointRequest request,
                                   @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createContact(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/contacts/{id}")
    Party360Response updateContact(@PathVariable long partyId, @PathVariable long id,
                                   @Valid @RequestBody ContactPointRequest request,
                                   @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateContact(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/contacts/{id}")
    Party360Response deleteContact(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteContact(partyId, id);
    }

    @PostMapping("/parties/{partyId}/contact-addresses")
    Party360Response createContactAddressAssociation(
            @PathVariable long partyId,
            @Valid @RequestBody ContactPointAddressRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor
    ) {
        return service.createContactAddressAssociation(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/contact-addresses/{id}")
    Party360Response updateContactAddressAssociation(
            @PathVariable long partyId, @PathVariable long id,
            @Valid @RequestBody ContactPointAddressRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor
    ) {
        return service.updateContactAddressAssociation(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/contact-addresses/{id}")
    Party360Response deleteContactAddressAssociation(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteContactAddressAssociation(partyId, id);
    }

    @PostMapping("/parties/{partyId}/financial-profiles")
    Party360Response createFinancialProfile(@PathVariable long partyId, @Valid @RequestBody FinancialProfileRequest request,
                                            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createFinancialProfile(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/financial-profiles/{id}")
    Party360Response updateFinancialProfile(@PathVariable long partyId, @PathVariable long id, @Valid @RequestBody FinancialProfileRequest request,
                                            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateFinancialProfile(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/financial-profiles/{id}")
    Party360Response deleteFinancialProfile(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteFinancialProfile(partyId, id);
    }

    @PostMapping("/parties/{partyId}/employments")
    Party360Response createEmployment(@PathVariable long partyId, @Valid @RequestBody PartyEmploymentRequest request,
                                      @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createEmployment(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/employments/{id}")
    Party360Response updateEmployment(@PathVariable long partyId, @PathVariable long id, @Valid @RequestBody PartyEmploymentRequest request,
                                      @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateEmployment(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/employments/{id}")
    Party360Response deleteEmployment(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteEmployment(partyId, id);
    }

    @PostMapping("/parties/{partyId}/income-sources")
    Party360Response createIncomeSource(@PathVariable long partyId, @Valid @RequestBody PartyIncomeSourceRequest request,
                                        @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createIncomeSource(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/income-sources/{id}")
    Party360Response updateIncomeSource(@PathVariable long partyId, @PathVariable long id, @Valid @RequestBody PartyIncomeSourceRequest request,
                                        @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateIncomeSource(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/income-sources/{id}")
    Party360Response deleteIncomeSource(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteIncomeSource(partyId, id);
    }

    @PostMapping("/parties/{partyId}/asset-liabilities")
    Party360Response createAssetLiability(@PathVariable long partyId, @Valid @RequestBody PartyAssetLiabilityRequest request,
                                          @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createAssetLiability(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/asset-liabilities/{id}")
    Party360Response updateAssetLiability(@PathVariable long partyId, @PathVariable long id, @Valid @RequestBody PartyAssetLiabilityRequest request,
                                          @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateAssetLiability(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/asset-liabilities/{id}")
    Party360Response deleteAssetLiability(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteAssetLiability(partyId, id);
    }

    @PostMapping("/parties/{partyId}/licenses")
    Party360Response createLicense(@PathVariable long partyId, @Valid @RequestBody PartyLicenseRequest request,
                                   @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createLicense(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/licenses/{id}")
    Party360Response updateLicense(@PathVariable long partyId, @PathVariable long id, @Valid @RequestBody PartyLicenseRequest request,
                                   @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateLicense(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/licenses/{id}")
    Party360Response deleteLicense(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteLicense(partyId, id);
    }

    @PostMapping("/parties/{partyId}/classifications")
    Party360Response createClassification(@PathVariable long partyId, @Valid @RequestBody PartyClassificationRequest request,
                                          @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createClassification(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/classifications/{id}")
    Party360Response updateClassification(@PathVariable long partyId, @PathVariable long id,
                                          @Valid @RequestBody PartyClassificationRequest request,
                                          @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateClassification(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/classifications/{id}")
    Party360Response deleteClassification(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteClassification(partyId, id);
    }


    @PostMapping("/parties/{partyId}/relationships")
    Party360Response createRelationship(@PathVariable long partyId, @Valid @RequestBody PartyRelationshipRequest request,
                                        @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createRelationship(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/relationships/{id}")
    Party360Response updateRelationship(@PathVariable long partyId, @PathVariable long id,
                                        @Valid @RequestBody PartyRelationshipRequest request,
                                        @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateRelationship(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/relationships/{id}")
    Party360Response deleteRelationship(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteRelationship(partyId, id);
    }

    @PostMapping("/parties/{partyId}/beneficial-ownerships")
    Party360Response createBeneficialOwnership(@PathVariable long partyId, @Valid @RequestBody BeneficialOwnershipRequest request,
                                               @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createBeneficialOwnership(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/beneficial-ownerships/{id}")
    Party360Response updateBeneficialOwnership(@PathVariable long partyId, @PathVariable long id,
                                               @Valid @RequestBody BeneficialOwnershipRequest request,
                                               @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateBeneficialOwnership(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/beneficial-ownerships/{id}")
    Party360Response deleteBeneficialOwnership(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteBeneficialOwnership(partyId, id);
    }

    @PostMapping("/parties/{partyId}/authorities")
    Party360Response createAuthority(@PathVariable long partyId, @Valid @RequestBody PartyAuthorityRequest request,
                                     @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createAuthority(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/authorities/{id}")
    Party360Response updateAuthority(@PathVariable long partyId, @PathVariable long id,
                                     @Valid @RequestBody PartyAuthorityRequest request,
                                     @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateAuthority(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/authorities/{id}")
    Party360Response deleteAuthority(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteAuthority(partyId, id);
    }

    @PostMapping("/parties/{partyId}/roles")
    Party360Response createRole(@PathVariable long partyId, @Valid @RequestBody PartyRoleRequest request,
                                @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createRole(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/roles/{id}")
    Party360Response updateRole(@PathVariable long partyId, @PathVariable long id,
                                @Valid @RequestBody PartyRoleRequest request,
                                @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateRole(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/roles/{id}")
    Party360Response deleteRole(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteRole(partyId, id);
    }

    @PostMapping("/parties/{partyId}/kyc-cases")
    Party360Response createKyc(@PathVariable long partyId, @Valid @RequestBody KycCaseRequest request,
                               @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createKycCase(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/kyc-cases/{id}")
    Party360Response updateKyc(@PathVariable long partyId, @PathVariable long id,
                               @Valid @RequestBody KycCaseRequest request,
                               @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateKycCase(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/kyc-cases/{id}")
    Party360Response deleteKyc(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteKycCase(partyId, id);
    }

    @PostMapping(value = "/parties/{partyId}/document-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DocumentUploadResponse uploadDocumentFile(@PathVariable long partyId, @RequestParam("file") MultipartFile file) {
        service.find(partyId);
        return documentStorage.store(partyId, file);
    }

    @GetMapping("/parties/{partyId}/documents/{documentId}/file")
    ResponseEntity<Resource> downloadDocumentFile(@PathVariable long partyId, @PathVariable long documentId) {
        PartyDocumentRecord document = service.find(partyId).documents().stream()
                .filter(item -> item.documentId() == documentId)
                .findFirst()
                .orElseThrow(() -> new CifNotFoundException("مدرک در پرونده Party یافت نشد."));
        var stored = documentStorage.load(partyId, document.storageRef(), document.mimeType());
        String extension = switch (stored.mimeType()) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/tiff" -> ".tiff";
            default -> "";
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=party-document-" + documentId + extension)
                .body(stored.resource());
    }

    @PostMapping("/parties/{partyId}/documents")
    Party360Response createDocument(@PathVariable long partyId, @Valid @RequestBody PartyDocumentRequest request,
                                    @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createDocument(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/documents/{id}")
    Party360Response updateDocument(@PathVariable long partyId, @PathVariable long id,
                                    @Valid @RequestBody PartyDocumentRequest request,
                                    @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateDocument(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/documents/{id}")
    Party360Response deleteDocument(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteDocument(partyId, id);
    }

    @PostMapping("/parties/{partyId}/risk-assessments")
    Party360Response createRisk(@PathVariable long partyId, @Valid @RequestBody RiskAssessmentRequest request,
                                @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createRisk(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/risk-assessments/{id}")
    Party360Response updateRisk(@PathVariable long partyId, @PathVariable long id,
                                @Valid @RequestBody RiskAssessmentRequest request,
                                @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateRisk(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/risk-assessments/{id}")
    Party360Response deleteRisk(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteRisk(partyId, id);
    }

    @PostMapping("/parties/{partyId}/screenings")
    Party360Response createScreening(@PathVariable long partyId, @Valid @RequestBody ScreeningResultRequest request,
                                     @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createScreening(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/screenings/{id}")
    Party360Response updateScreening(@PathVariable long partyId, @PathVariable long id,
                                     @Valid @RequestBody ScreeningResultRequest request,
                                     @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateScreening(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/screenings/{id}")
    Party360Response deleteScreening(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteScreening(partyId, id);
    }

    @PostMapping("/parties/{partyId}/external-inquiries")
    Party360Response createExternalInquiry(@PathVariable long partyId, @Valid @RequestBody ExternalInquiryRequest request,
                                           @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createExternalInquiry(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/external-inquiries/{id}")
    Party360Response updateExternalInquiry(@PathVariable long partyId, @PathVariable long id,
                                           @Valid @RequestBody ExternalInquiryRequest request,
                                           @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateExternalInquiry(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/external-inquiries/{id}")
    Party360Response deleteExternalInquiry(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteExternalInquiry(partyId, id);
    }

    @PostMapping("/parties/{partyId}/status-changes")
    Party360Response changePartyStatus(@PathVariable long partyId, @Valid @RequestBody PartyStatusChangeRequest request,
                                       @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.changePartyStatus(partyId, request, actor);
    }

    @PostMapping("/parties/{partyId}/merge")
    Party360Response mergeParty(@PathVariable long partyId, @Valid @RequestBody PartyMergeRequest request,
                                @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.mergeParty(partyId, request, actor);
    }

    @GetMapping("/dashboard/summary")
    CifDashboardSummary dashboardSummary() {
        return service.dashboardSummary();
    }
    @PostMapping("/parties/{partyId}/consents")
    Party360Response createConsent(@PathVariable long partyId, @Valid @RequestBody PartyConsentRequest request,
                                   @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createConsent(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/consents/{id}")
    Party360Response updateConsent(@PathVariable long partyId, @PathVariable long id,
                                   @Valid @RequestBody PartyConsentRequest request,
                                   @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateConsent(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/consents/{id}")
    Party360Response revokeConsent(@PathVariable long partyId, @PathVariable long id,
                                   @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.revokeConsent(partyId, id, actor);
    }

    @PostMapping("/parties/{partyId}/communication-preferences")
    Party360Response createCommunicationPreference(@PathVariable long partyId,
                                                    @Valid @RequestBody CommunicationPreferenceRequest request,
                                                    @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createCommunicationPreference(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/communication-preferences/{id}")
    Party360Response updateCommunicationPreference(@PathVariable long partyId, @PathVariable long id,
                                                    @Valid @RequestBody CommunicationPreferenceRequest request,
                                                    @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateCommunicationPreference(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/communication-preferences/{id}")
    Party360Response deleteCommunicationPreference(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteCommunicationPreference(partyId, id);
    }

    @PostMapping("/parties/{partyId}/general-preferences")
    Party360Response createGeneralPreference(@PathVariable long partyId,
                                             @Valid @RequestBody PartyGeneralPreferenceRequest request,
                                             @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.createGeneralPreference(partyId, request, actor);
    }

    @PutMapping("/parties/{partyId}/general-preferences/{id}")
    Party360Response updateGeneralPreference(@PathVariable long partyId, @PathVariable long id,
                                             @Valid @RequestBody PartyGeneralPreferenceRequest request,
                                             @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor) {
        return service.updateGeneralPreference(partyId, id, request, actor);
    }

    @DeleteMapping("/parties/{partyId}/general-preferences/{id}")
    Party360Response deleteGeneralPreference(@PathVariable long partyId, @PathVariable long id) {
        return service.deleteGeneralPreference(partyId, id);
    }

}
