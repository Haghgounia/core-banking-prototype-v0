package com.behsazan.corebanking.cif.reference.web;

import com.behsazan.corebanking.cif.reference.application.PartyReferenceService;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.CatalogResponse;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.LookupOption;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.RecordResponse;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.RowResponse;
import com.behsazan.corebanking.cif.reference.domain.PartyReferenceModels.TableDescriptor;
import com.behsazan.corebanking.shared.model.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cif/reference")
public class PartyReferenceController {
    private final PartyReferenceService service;

    public PartyReferenceController(PartyReferenceService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    CatalogResponse catalog() {
        return service.catalog();
    }

    @GetMapping("/{resource}/descriptor")
    TableDescriptor descriptor(@PathVariable String resource) {
        return service.descriptor(resource);
    }

    @GetMapping("/{resource}/lookup")
    List<LookupOption> lookup(
            @PathVariable String resource,
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "200") int limit
    ) {
        return service.lookup(resource, text, limit);
    }

    @GetMapping("/{resource}")
    PageResponse<RowResponse> search(
            @PathVariable String resource,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return service.search(resource, text, active, page, size, sortBy, direction);
    }

    @GetMapping("/{resource}/{key}")
    RecordResponse find(@PathVariable String resource, @PathVariable String key) {
        return service.find(resource, key);
    }

    @PostMapping("/{resource}")
    RecordResponse create(@PathVariable String resource, @RequestBody Map<String, Object> values) {
        return service.create(resource, values);
    }

    @PutMapping("/{resource}/{key}")
    RecordResponse update(@PathVariable String resource, @PathVariable String key, @RequestBody Map<String, Object> values) {
        return service.update(resource, key, values);
    }

    @DeleteMapping("/{resource}/{key}")
    ResponseEntity<Void> delete(@PathVariable String resource, @PathVariable String key) {
        service.delete(resource, key);
        return ResponseEntity.noContent().build();
    }
}
