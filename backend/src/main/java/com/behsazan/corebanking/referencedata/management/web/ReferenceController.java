package com.behsazan.corebanking.referencedata.management.web;

import com.behsazan.corebanking.referencedata.management.application.ReferenceService;
import com.behsazan.corebanking.referencedata.management.domain.LookupOption;
import com.behsazan.corebanking.referencedata.management.domain.ReferenceRecordResponse;
import com.behsazan.corebanking.referencedata.management.domain.ReferenceSearchQuery;
import com.behsazan.corebanking.shared.model.PageResponse;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceController {
    private final ReferenceService service;

    public ReferenceController(ReferenceService service) {
        this.service = service;
    }

    @GetMapping("/{resource}")
    PageResponse<Map<String, Object>> search(
            @PathVariable String resource,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam Map<String, String> requestParams
    ) {
        Map<String, String> filters = new java.util.LinkedHashMap<>();
        requestParams.forEach((key, value) -> {
            if (key.startsWith("filter.") && key.length() > 7 && value != null && !value.isBlank()) {
                filters.put(key.substring(7), value.trim());
            }
        });
        return service.search(resource, new ReferenceSearchQuery(
                text, parentId, active, filters, page, size, sortBy, direction
        ));
    }

    @GetMapping("/{resource}/lookup")
    List<LookupOption> lookup(
            @PathVariable String resource,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "200") int limit
    ) {
        return service.lookup(resource, parentId, text, limit);
    }

    @GetMapping("/{resource}/{id}")
    ReferenceRecordResponse findById(@PathVariable String resource, @PathVariable long id) {
        return service.findById(resource, id);
    }

    @PostMapping("/{resource}")
    ResponseEntity<ReferenceRecordResponse> create(
            @PathVariable String resource,
            @RequestBody Map<String, Object> values,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") long actorId
    ) {
        ReferenceRecordResponse created = service.create(resource, values, actorId);
        return ResponseEntity.created(URI.create("/api/v1/reference/" + resource + "/" + created.id()))
                .body(created);
    }

    @PutMapping("/{resource}/{id}")
    ReferenceRecordResponse update(
            @PathVariable String resource,
            @PathVariable long id,
            @RequestBody Map<String, Object> values,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") long actorId
    ) {
        return service.update(resource, id, values, actorId);
    }

    @DeleteMapping("/{resource}/{id}")
    ResponseEntity<Void> delete(@PathVariable String resource, @PathVariable long id) {
        service.delete(resource, id);
        return ResponseEntity.noContent().build();
    }
}
