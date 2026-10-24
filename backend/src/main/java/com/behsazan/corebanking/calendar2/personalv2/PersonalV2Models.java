package com.behsazan.corebanking.calendar2.personalv2;

import java.util.List;

/** R2-B2: all Oracle IDs are decimal strings on the wire (including DAY_ID NUMBER(19)). */
public final class PersonalV2Models {
    private PersonalV2Models() {}

    public record Owner(String issuer, String subjectId) {}

    public record Entry(
            String userCalendarEntryId,
            String occurrenceId,
            String dayId,
            String canonicalIsoDate,
            String entryType,
            String title,
            String description,
            String localStartTime,
            String reminderTime,
            int advanceDays,
            String repeatPolicy,
            String status,
            long versionNo,
            long occurrenceVersionNo,
            String recurrenceFrequency
    ) {}

    /** One-day create/update. Recurring creation is supported by the bounded generator.
     *  recurrenceFrequency: NONE, DAILY, WEEKLY, MONTHLY, YEARLY.
     *  calendarVariantId is mandatory for recurring entries and comes from CAL2 reference APIs.
     */
    public record SaveRequest(
            String dayId,
            String entryType,
            String title,
            String description,
            String localStartTime,
            String reminderTime,
            Integer advanceDays,
            String repeatPolicy,
            String timeZoneId,
            String recurrenceFrequency,
            String calendarVariantId,
            Integer recurrenceInterval,
            Integer dayOfMonth,
            Integer monthOfYear,
            String byWeekdaysMask,
            String dayResolutionPolicy
    ) {}

    public record CompletionRequest(boolean completed) {}
    public record SnoozeRequest(int minutes) {}

    public record Alert(
            String alertId,
            String occurrenceId,
            String userCalendarEntryId,
            String dayId,
            String dueDate,
            String title,
            String alertKind,
            int daysToDue,
            String reminderTime,
            String alertStatus
    ) {}

    public record Inbox(String actorLabel, String zoneId, List<Alert> alerts) {
        public Inbox { alerts = List.copyOf(alerts); }
    }
}
