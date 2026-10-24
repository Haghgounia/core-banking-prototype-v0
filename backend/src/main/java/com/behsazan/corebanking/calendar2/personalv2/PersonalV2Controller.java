package com.behsazan.corebanking.calendar2.personalv2;

import com.behsazan.corebanking.calendar2.personalv2.PersonalV2Models.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/** The old R1C route is disabled by default; new frontend uses only this R2-B2 route. */
@RestController
@RequestMapping("/api/v2/calendar2/personal")
public class PersonalV2Controller {
    private final PersonalV2Service service;
    public PersonalV2Controller(PersonalV2Service service) { this.service = service; }

    @GetMapping("/entries")
    public List<Entry> entries(Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.entries(service.owner(principal), from, to);
    }

    @GetMapping("/entries/{id}")
    public Entry one(Principal principal, @PathVariable String id) {
        return service.find(service.owner(principal), id);
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public Entry create(Principal principal, @RequestHeader("Idempotency-Key") String key,
            @RequestBody SaveRequest request) {
        return service.create(service.owner(principal), key, request);
    }

    @PutMapping("/entries/{id}")
    public Entry update(Principal principal, @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody SaveRequest request) {
        return service.update(service.owner(principal), id, ifMatch, request);
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        service.delete(service.owner(principal), id, ifMatch);
    }

    @PatchMapping("/occurrences/{id}/completion")
    public Entry completion(Principal principal, @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody CompletionRequest request) {
        return service.complete(service.owner(principal), id, ifMatch, request.completed());
    }

    @GetMapping("/alerts")
    public Inbox alerts(Principal principal) { return service.inbox(service.owner(principal)); }

    @PostMapping("/alerts/{id}/acknowledge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledge(Principal principal, @PathVariable String id) {
        service.acknowledge(service.owner(principal), id);
    }

    @PostMapping("/alerts/{id}/snooze")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void snooze(Principal principal, @PathVariable String id, @RequestBody SnoozeRequest request) {
        service.snooze(service.owner(principal), id, request.minutes());
    }
}
