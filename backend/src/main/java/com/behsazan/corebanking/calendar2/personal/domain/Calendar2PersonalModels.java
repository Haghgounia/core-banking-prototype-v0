package com.behsazan.corebanking.calendar2.personal.domain;

import java.util.List;

/** Personal information is deliberately separate from official CAL2 events/holidays. */
public final class Calendar2PersonalModels {
    private Calendar2PersonalModels() {}

    public record Item(
            String itemId,
            long dayId,
            String canonicalIsoDate,
            String itemType,
            String title,
            String description,
            String reminderTime,
            int advanceDays,
            String repeatPolicy,
            String status,
            String lastAlertAckDate,
            boolean firstAdvanceAcknowledged
    ) {}

    public record SaveRequest(
            Long dayId,
            String itemType,
            String title,
            String description,
            String reminderTime,
            Integer advanceDays,
            String repeatPolicy
    ) {}

    public record CompletionRequest(boolean completed) {}

    public record Alert(
            String itemId,
            long dayId,
            String dueDate,
            String title,
            String alertKind,
            int daysToDue,
            String reminderTime
    ) {}

    public record Inbox(String actorLabel, String zoneId, List<Alert> alerts) {
        public Inbox { alerts = List.copyOf(alerts); }
    }
}
