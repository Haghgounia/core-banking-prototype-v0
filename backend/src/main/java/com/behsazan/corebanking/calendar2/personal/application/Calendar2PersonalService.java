package com.behsazan.corebanking.calendar2.personal.application;

import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Alert;
import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Inbox;
import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.Entry;
import com.behsazan.corebanking.calendar2.personal.domain.Calendar2PersonalModels.SaveRequest;
import com.behsazan.corebanking.calendar2.personal.oracle.Calendar2PersonalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class Calendar2PersonalService {
    private final Calendar2PersonalRepository repository;
    private final String actor;
    private final ZoneId zone;

    // This prototype has no authenticated principals. Never accept an untrusted user ID in an API request.
    public Calendar2PersonalService(Calendar2PersonalRepository repository,
            @Value("${core-banking.calendar2.personal.demo-actor:prototype-admin}") String demoActor,
            @Value("${core-banking.calendar2.personal.zone:Asia/Tehran}") String zoneId) {
        this.repository = repository;
        this.actor = demoActor;
        this.zone = ZoneId.of(zoneId);
        if (actor == null || !actor.matches("[A-Za-z0-9_.-]{1,80}")) {
            throw new IllegalArgumentException("Invalid server-configured CAL2 demo actor");
        }
    }

    @Transactional(readOnly = true)
    public List<Entry> entries(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(from.plusDays(62))) {
            throw badRequest("Date range must contain 1-63 days");
        }
        return repository.monthEntries(actor, from, to);
    }

    @Transactional(readOnly = true)
    public Entry find(long id) { return required(id); }

    @Transactional
    public Entry create(SaveRequest body) {
        SaveRequest request = normalized(body);
        long id = repository.insert(actor, request.dayId(), request.entryType(), request.title(),
                request.description(), request.reminderTime(), request.advanceDays(), request.repeatPolicy());
        return required(id);
    }

    @Transactional
    public Entry update(long id, SaveRequest body) {
        SaveRequest request = normalized(body);
        if (repository.update(actor, id, request.dayId(), request.entryType(), request.title(), request.description(),
                request.reminderTime(), request.advanceDays(), request.repeatPolicy()) == 0) throw notFound();
        return required(id);
    }

    @Transactional
    public Entry setCompleted(long id, boolean completed) {
        if (repository.complete(actor, id, completed) == 0) throw notFound();
        return required(id);
    }

    @Transactional
    public void delete(long id) {
        if (repository.delete(actor, id) == 0) throw notFound();
    }

    @Transactional(readOnly = true)
    public Inbox inbox() {
        LocalDate today = LocalDate.now(zone);
        LocalTime now = LocalTime.now(zone);
        List<Alert> alerts = repository.openReminders(actor, today).stream()
                .map(item -> {
                    String kind = Calendar2PersonalAlertPolicy.alertKind(item, today, now);
                    return kind == null ? null : new Alert(item.userCalendarEntryId(), item.dayId(), item.canonicalIsoDate(),
                            item.title(), kind, Calendar2PersonalAlertPolicy.daysToDue(item, today), item.reminderTime());
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt((Alert a) -> switch (a.alertKind()) {
                    case "DUE" -> 0;
                    case "ADVANCE" -> 1;
                    default -> 2;
                }).thenComparing(Alert::dueDate))
                .limit(100)
                .toList();
        return new Inbox("کاربر مدیر (آزمایشی)", zone.getId(), alerts);
    }

    @Transactional
    public void acknowledge(long id) {
        Entry entry = required(id);
        LocalDate today = LocalDate.now(zone);
        String kind = Calendar2PersonalAlertPolicy.alertKind(entry, today, LocalTime.now(zone));
        if (kind == null) throw badRequest("Reminder is not currently due for notification");
        boolean advanceOnce = "ADVANCE".equals(kind) && "ONCE".equals(entry.repeatPolicy());
        if (repository.acknowledge(actor, id, today, advanceOnce) == 0) throw notFound();
    }

    private Entry required(long id) {
        if (id <= 0 || id > 999_999_999_999L) throw notFound();
        return repository.find(actor, id).orElseThrow(Calendar2PersonalService::notFound);
    }

    private SaveRequest normalized(SaveRequest input) {
        if (input == null || input.dayId() == null || input.dayId() <= 0) throw badRequest("Select a calendar day");
        String type = input.entryType() == null ? "" : input.entryType().trim().toUpperCase(Locale.ROOT);
        if (!"NOTE".equals(type) && !"REMINDER".equals(type)) throw badRequest("Invalid entry type");
        String title = input.title() == null ? "" : input.title().trim();
        if (title.isEmpty() || title.length() > 200) throw badRequest("Title must be 1-200 characters");
        String description = input.description() == null ? null : input.description().trim();
        if (description != null && description.length() > 2000) throw badRequest("Description exceeds 2000 characters");
        if (!repository.dayExists(input.dayId())) throw badRequest("Unknown CAL2 canonical day");
        if ("NOTE".equals(type)) return new SaveRequest(input.dayId(), type, title, description, null, 0, "DAILY");
        String time = input.reminderTime() == null ? "" : input.reminderTime().trim();
        if (!time.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) throw badRequest("Time must use HH:mm format");
        int advance = input.advanceDays() == null ? 3 : input.advanceDays();
        if (advance < 0 || advance > 30) throw badRequest("Advance must be 0-30 days");
        String repeat = input.repeatPolicy() == null ? "DAILY" : input.repeatPolicy().trim().toUpperCase(Locale.ROOT);
        if (!"DAILY".equals(repeat) && !"ONCE".equals(repeat)) throw badRequest("Invalid repeat policy");
        return new SaveRequest(input.dayId(), type, title, description, time, advance, repeat);
    }

    private static ResponseStatusException badRequest(String detail) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail); }
    private static ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendar entry not found"); }
}
