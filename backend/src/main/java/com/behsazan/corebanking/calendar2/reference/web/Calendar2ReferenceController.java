package com.behsazan.corebanking.calendar2.reference.web;

import com.behsazan.corebanking.calendar2.reference.application.Calendar2ReferenceService;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CanonicalDayFilterMeta;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CanonicalDaySummary;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CalendarDateFilterMeta;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.CatalogResponse;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.LookupOption;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.RecordResponse;
import com.behsazan.corebanking.calendar2.reference.domain.Calendar2ReferenceModels.TableDescriptor;
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
@RequestMapping("/api/v1/calendar2/reference")
public class Calendar2ReferenceController {
    private final Calendar2ReferenceService service;

    public Calendar2ReferenceController(Calendar2ReferenceService service) { this.service = service; }

    @GetMapping("/catalog") CatalogResponse catalog() { return service.catalog(); }
    @GetMapping("/{resource}/descriptor") TableDescriptor descriptor(@PathVariable String resource) { return service.descriptor(resource); }

    @GetMapping("/{resource}/lookup")
    List<LookupOption> lookup(@PathVariable String resource, @RequestParam(required = false) String text,
                              @RequestParam(defaultValue = "50") int limit) {
        return service.lookup(resource, text, limit);
    }

    @GetMapping("/canonical-days/filter-meta")
    CanonicalDayFilterMeta canonicalDayFilterMeta() { return service.canonicalDayFilterMeta(); }

    @GetMapping("/canonical-days/explorer")
    PageResponse<CanonicalDaySummary> canonicalDays(@RequestParam(required = false) String text,
                                                     @RequestParam(required = false) Integer solarYear,
                                                     @RequestParam(required = false) Integer solarCentury,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size,
                                                     @RequestParam(required = false) String sortBy,
                                                     @RequestParam(defaultValue = "asc") String direction) {
        return service.searchCanonicalDays(text, solarYear, solarCentury, page, size, sortBy, direction);
    }

    @GetMapping("/calendar-dates/filter-meta")
    CalendarDateFilterMeta calendarDateFilterMeta() { return service.calendarDateFilterMeta(); }

    @GetMapping("/calendar-dates/explorer")
    PageResponse<Map<String,Object>> calendarDates(@RequestParam(required=false) String text, @RequestParam(required=false) String calendarCode,
                                                    @RequestParam(required=false) Integer yearNo, @RequestParam(required=false) Integer century,
                                                    @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
                                                    @RequestParam(required=false) String sortBy, @RequestParam(defaultValue="asc") String direction) {
        return service.searchCalendarDates(text, calendarCode, yearNo, century, page, size, sortBy, direction);
    }

    @GetMapping("/events/explorer")
    PageResponse<Map<String,Object>> events(@RequestParam(required=false) String text, @RequestParam(required=false) Long eventTypeId,
                                            @RequestParam(required=false) String calendarCode, @RequestParam(defaultValue="0") int page,
                                            @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String sortBy,
                                            @RequestParam(defaultValue="asc") String direction) {
        return service.searchEvents(text, eventTypeId, calendarCode, page, size, sortBy, direction);
    }

    @GetMapping("/{resource}")
    PageResponse<Map<String, Object>> search(@PathVariable String resource, @RequestParam(required = false) String text,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String sortBy,
                                             @RequestParam(defaultValue = "asc") String direction) {
        return service.search(resource, text, page, size, sortBy, direction);
    }

    @GetMapping("/{resource}/{key}") RecordResponse find(@PathVariable String resource, @PathVariable String key) {
        return service.find(resource, key);
    }

    @PostMapping("/{resource}") RecordResponse create(@PathVariable String resource, @RequestBody Map<String, Object> values) {
        return service.create(resource, values);
    }

    @PutMapping("/{resource}/{key}")
    RecordResponse update(@PathVariable String resource, @PathVariable String key, @RequestBody Map<String, Object> values) {
        return service.update(resource, key, values);
    }

    @DeleteMapping("/{resource}/{key}") ResponseEntity<Void> delete(@PathVariable String resource, @PathVariable String key) {
        service.delete(resource, key);
        return ResponseEntity.noContent().build();
    }
}
