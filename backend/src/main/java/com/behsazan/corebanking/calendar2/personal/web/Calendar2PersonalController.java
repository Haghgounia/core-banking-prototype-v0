package com.behsazan.corebanking.calendar2.personal.web;

import com.behsazan.corebanking.calendar2.personal.application.Calendar2PersonalService;
import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.CompletionRequest;
import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Inbox;
import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Entry;
import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.SaveRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar2/personal")
public class Calendar2PersonalController {
    private final Calendar2PersonalService service;

    public Calendar2PersonalController(Calendar2PersonalService service) { this.service = service; }

    @GetMapping("/entries")
    public List<Entry> entries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.entries(from, to);
    }

    @GetMapping("/entries/{id}")
    public Entry one(@PathVariable long id) { return service.find(id); }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public Entry create(@RequestBody SaveRequest request) { return service.create(request); }

    @PutMapping("/entries/{id}")
    public Entry update(@PathVariable long id, @RequestBody SaveRequest request) { return service.update(id, request); }

    @PatchMapping("/entries/{id}/completion")
    public Entry complete(@PathVariable long id, @RequestBody CompletionRequest request) {
        return service.setCompleted(id, request.completed());
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }

    /** Evaluation uses server-side Asia/Tehran time, not untrusted browser dates. */
    @GetMapping("/alerts")
    public Inbox alerts() { return service.inbox(); }

    @PostMapping("/alerts/{id}/acknowledge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledge(@PathVariable long id) { service.acknowledge(id); }
}
