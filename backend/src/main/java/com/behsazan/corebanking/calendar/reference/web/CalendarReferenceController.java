package com.behsazan.corebanking.calendar.reference.web;

import com.behsazan.corebanking.calendar.reference.application.CalendarReferenceService;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.CatalogResponse;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.LookupOption;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.RecordResponse;
import com.behsazan.corebanking.calendar.reference.domain.CalendarReferenceModels.TableDescriptor;
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
@RequestMapping("/api/v1/calendar/reference")
public class CalendarReferenceController {
    private final CalendarReferenceService service;

    public CalendarReferenceController(CalendarReferenceService service) {
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
    List<LookupOption> lookup(@PathVariable String resource,
                              @RequestParam(required = false) String text,
                              @RequestParam(defaultValue = "50") int limit) {
        return service.lookup(resource, text, limit);
    }

    @GetMapping("/{resource}")
    PageResponse<Map<String, Object>> search(@PathVariable String resource,
                                             @RequestParam(required = false) String text,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String sortBy,
                                             @RequestParam(defaultValue = "asc") String direction) {
        return service.search(resource, text, page, size, sortBy, direction);
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
    RecordResponse update(@PathVariable String resource, @PathVariable String key,
                          @RequestBody Map<String, Object> values) {
        return service.update(resource, key, values);
    }

    @DeleteMapping("/{resource}/{key}")
    ResponseEntity<Void> delete(@PathVariable String resource, @PathVariable String key) {
        service.delete(resource, key);
        return ResponseEntity.noContent().build();
    }
}
