# R2 V2 REST Contract — Design Candidate

**Base Path:** `/api/v2/calendar2/personal`; مسيرهاي V1 بدون برنامه Deprecation شکسته نشوند.

## Command and Query

| Verb | Path | رفتار |
|---|---|---|
| POST | `/entries` | Create با `Idempotency-Key`; Owner از Principal؛ 201 |
| GET | `/entries?from=...&to=...` | تقويم کاربر، Range + Cursor؛ بدون پارامتر Owner |
| GET | `/entries/{entryId}` | Details، version، recurrence/reminders |
| PUT | `/entries/{entryId}` | `If-Match` و mutation در يک Transaction |
| DELETE | `/entries/{entryId}` | Soft cancellation با `If-Match`؛ هيچ DELETE فيزيکي |
| GET | `/entries/{entryId}/occurrences` | روزهاي قابل رويت با pagination |
| PATCH | `/occurrences/{occurrenceId}` | scope=`THIS_ONLY`, `THIS_AND_FUTURE`, `ENTIRE_SERIES` + `If-Match` |
| POST | `/occurrences/{occurrenceId}/complete` | تنها نوبت منتخب را DONE مي کند |
| POST | `/entries/{entryId}/reminder-rules` | Rule جديد با Version + `If-Match` |
| GET | `/alerts?status=AVAILABLE` | Server-side inbox؛ بدون نياز به مرورگر باز |
| POST | `/alerts/{alertId}/acknowledge` | ACK فقط همان Alert، نه DONE کار |
| POST | `/alerts/{alertId}/snooze` | تغيير Next Eligible، بدون تغيير Planned |

## Create Task + recurrence + daily reminders

```json
{
  "entryType": "TASK",
  "title": "پیگیری گزارش شعب",
  "schedule": {
    "dayId": "<resolved from CAL2 calendar API>",
    "allDay": false,
    "localStartTime": "10:00",
    "timeZoneId": "Asia/Tehran"
  },
  "recurrence": {
    "calendarVariantId": "<real variant ID>",
    "frequency": "MONTHLY",
    "interval": 1,
    "dayOfMonth": 15,
    "dayResolutionPolicy": "SKIP"
  },
  "reminderRules": [
    {
      "alertPattern": "DAILY_WINDOW",
      "windowStartDays": 3,
      "windowEndDays": 1,
      "windowIntervalDays": 1,
      "localAlertTime": "09:00",
      "ackStopPolicy": "NONE",
      "channelCode": "IN_APP"
    },
    {
      "alertPattern": "AT_DUE",
      "channelCode": "IN_APP"
    }
  ]
}
```

Values in `<...>` are placeholders requiring real reference-data lookup; **not** valid final numeric DB IDs.

## Idempotency and concurrency

- On POST, client supplies opaque `Idempotency-Key` scoped to owner+operation. Same hash replays same result; different hash with same key yields 409.
- ETag / `If-Match` required for modifications. stale version = 412; absent precondition = 428.
- Decimal IDs serialized as **strings** on wire, including NUMBER(19), to avoid JavaScript unsafe integers.
- 401 (unauthenticated), 404 (not owned/not visible), 422 (calendar/rule invalid), 409 (idempotency collision), 503 (essential infrastructure not ready); server logs must not leak note contents.
- For demo Principal `prototype-admin`, V2 is disabled outside controlled single-user test environment.

## Unsupported / subsequent phases

- Meeting invitations, sharing, attendees, push, SMS/email, banking action automation are **not** included in R2-B/C.
- Business-day offsets and arbitrary complex RRULE imports are deferred until calendars and validation policies are approved.
