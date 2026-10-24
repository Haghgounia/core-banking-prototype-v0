# CAL2 R2-A2 — Conceptual/Logical ERD (not physical DDL)

```mermaid
erDiagram
    USER_CALENDAR_ENTRIES ||--o{ USER_CALENDAR_RECURRENCE_RULES : has_versions
    USER_CALENDAR_ENTRIES ||--o{ USER_CALENDAR_OCCURRENCES : has_occurrences
    USER_CALENDAR_ENTRIES ||--o{ USER_CALENDAR_REMINDER_RULES : has_reminders
    USER_CALENDAR_ENTRIES ||--o{ USER_CALENDAR_AUDIT_LOG : audits
    USER_CALENDAR_ENTRIES o|--o{ USER_CALENDAR_ENTRIES : predecessor_for_split
    USER_CALENDAR_RECURRENCE_RULES o|--o{ USER_CALENDAR_OCCURRENCES : generated_from
    USER_CALENDAR_OCCURRENCES ||--o{ USER_CALENDAR_ALERTS : has_alerts
    USER_CALENDAR_REMINDER_RULES ||--o{ USER_CALENDAR_ALERTS : creates
    USER_CALENDAR_ALERTS ||--o{ USER_CALENDAR_ALERT_DELIVERIES : external_attempts
    CANONICAL_DAY ||--o{ USER_CALENDAR_ENTRIES : start_day
    CANONICAL_DAY ||--o{ USER_CALENDAR_OCCURRENCES : effective_day
    CALENDAR_VARIANT ||--o{ USER_CALENDAR_RECURRENCE_RULES : controls_calendar

    USER_CALENDAR_ENTRIES {
      number USER_CALENDAR_ENTRY_ID PK
      string OWNER_ISSUER
      string OWNER_SUBJECT_ID
      string ENTRY_TYPE
      number START_DAY_ID FK
      number PREDECESSOR_ENTRY_ID FK
      string ENTRY_STATUS
      number VERSION_NO
    }
    USER_CALENDAR_RECURRENCE_RULES {
      number RECURRENCE_RULE_ID PK
      number USER_CALENDAR_ENTRY_ID FK
      number CALENDAR_VARIANT_ID FK
      string FREQUENCY
      number RULE_VERSION_NO
      string RULE_STATUS
    }
    USER_CALENDAR_OCCURRENCES {
      number USER_CALENDAR_OCCURRENCE_ID PK
      number USER_CALENDAR_ENTRY_ID FK
      number RECURRENCE_RULE_ID FK
      string ORIGINAL_SLOT_KEY UK
      number ORIGINAL_DAY_ID FK
      number DAY_ID FK
      string OCCURRENCE_STATUS
    }
    USER_CALENDAR_REMINDER_RULES {
      number REMINDER_RULE_ID PK
      number USER_CALENDAR_ENTRY_ID FK
      string ALERT_PATTERN
      string ACK_STOP_POLICY
      number RULE_VERSION_NO
    }
    USER_CALENDAR_ALERTS {
      number USER_CALENDAR_ALERT_ID PK
      number USER_CALENDAR_OCCURRENCE_ID FK
      number USER_CALENDAR_ENTRY_ID FK
      number REMINDER_RULE_ID FK
      string SCHEDULED_SLOT_KEY UK
      string ALERT_STATUS
    }
    USER_CALENDAR_AUDIT_LOG {
      number AUDIT_ID PK
      number USER_CALENDAR_ENTRY_ID FK
      string ACTION_CODE
    }
    USER_CALENDAR_ALERT_DELIVERIES {
      number DELIVERY_ID PK
      number USER_CALENDAR_ALERT_ID FK
      string CHANNEL_CODE
      number ATTEMPT_NO
    }
    USER_CALENDAR_REQUEST_KEYS {
      number REQUEST_KEY_ID PK
      string OWNER_ISSUER
      string OWNER_SUBJECT_ID
      string OPERATION_CODE
      string IDEMPOTENCY_KEY UK
      string REQUEST_STATUS
    }
    CANONICAL_DAY {
      number DAY_ID PK
    }
    CALENDAR_VARIANT {
      number CALENDAR_VARIANT_ID PK
    }
```

`USER_CALENDAR_REQUEST_KEYS` belongs to the API command/idempotency boundary and references the resulting resource through a controlled reference, **not** a required FK to Entries (multiple operation kinds can be guarded).

The diagram shows Recurrence Rules with historical revisions; only one may be active at a time per Entry. The relationship from Occurrence to RecurrenceRule is optional for one-time entries. Composite UNIQUE constraints are detailed in the dictionary and require final physical DBA design.
