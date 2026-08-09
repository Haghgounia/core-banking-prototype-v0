package com.behsazan.corebanking.cif.web;

import com.behsazan.corebanking.cif.application.CifService;
import com.behsazan.corebanking.cif.domain.CifModels.CifDashboardSummary;
import com.behsazan.corebanking.cif.domain.CifModels.ContactPointRequest;
import com.behsazan.corebanking.cif.domain.CifModels.CreatePartyRequest;
import com.behsazan.corebanking.cif.domain.CifModels.KycCaseRequest;
import com.behsazan.corebanking.cif.domain.CifModels.OrganizationRequest;
import com.behsazan.corebanking.cif.domain.CifModels.Party360Response;
import com.behsazan.corebanking.cif.domain.CifModels.PartyAddressRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyDocumentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyIdentifierRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartyNameRequest;
import com.behsazan.corebanking.cif.domain.CifModels.PartySummary;
import com.behsazan.corebanking.cif.domain.CifModels.PersonRequest;
import com.behsazan.corebanking.cif.domain.CifModels.RiskAssessmentRequest;
import com.behsazan.corebanking.cif.domain.CifModels.ScreeningResultRequest;
import com.behsazan.corebanking.cif.domain.CifModels.UpdatePartyRequest;
import com.behsazan.corebanking.shared.model.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

import java.net.URI;

@RestController
@RequestMapping("/api/v1/cif")
public class CifController {
    private final CifService service;

    public CifController(CifService service) {
        this.service = service;
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

    @GetMapping("/parties/{partyId}")
    Party360Response find(@PathVariable long partyId) {
        return service.find(partyId);
    }

    @PostMapping("/parties")
    ResponseEntity<Party360Response> createParty(
            @Valid @RequestBody CreatePartyRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") String actor
    ) {
        Party360Response created = service.createParty(request, actor);
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

    @GetMapping("/dashboard/summary")
    CifDashboardSummary dashboardSummary() {
        return service.dashboardSummary();
    }
}
