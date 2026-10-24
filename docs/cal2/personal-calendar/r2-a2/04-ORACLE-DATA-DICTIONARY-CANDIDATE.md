# Oracle physical dictionary (candidate; no DDL execution)

Types are candidates; authoritative physical DDL, constraints, grants and schema migration require independent DBA verification. All timestamps ending _UTC are stored as absolute UTC instants. TSTZ driver mapping/index cost must be qualified.

## CAL2.USER_CALENDAR_ENTRIES

| Column | Oracle type candidate | Required | Constraints / meaning |
|---|---|---|---|
| USER_CALENDAR_ENTRY_ID | `NUMBER(12,0)` | PK | DEFAULT CAL2.SEQ_CAL2_USER_CALENDAR_ENTRIES.NEXTVAL; migrate R1B ID exactly |
| OWNER_ISSUER | `VARCHAR2(255 CHAR)` | YES | From validated principal; no client input |
| OWNER_SUBJECT_ID | `VARCHAR2(128 CHAR)` | YES | From validated principal; owner scoped |
| ENTRY_TYPE | `VARCHAR2(20 CHAR)` | YES | NOTE/TASK/APPOINTMENT |
| TITLE | `VARCHAR2(200 CHAR)` | YES | Trim and non-blank |
| DESCRIPTION | `CLOB` | NO | Sensitive personal content; access scoped; no audit echo |
| START_DAY_ID | `NUMBER(19,0)` | YES | FK CAL2.CANONICAL_DAY.DAY_ID |
| END_DAY_ID | `NUMBER(19,0)` | NO | FK CAL2.CANONICAL_DAY.DAY_ID; chronological validation via epoch day |
| ALL_DAY_FLAG | `CHAR(1 CHAR)` | YES | Y/N |
| START_LOCAL_MINUTE | `NUMBER(4,0)` | NO | 0..1439; required for timed entries |
| END_LOCAL_MINUTE | `NUMBER(4,0)` | NO | 0..1439 when explicit end is given |
| DURATION_MINUTES | `NUMBER(6,0)` | NO | Positive for timed recurring appointment/task |
| TIME_ZONE_ID | `VARCHAR2(64 CHAR)` | YES | IANA ZoneId validated in service |
| PREDECESSOR_ENTRY_ID | `NUMBER(12,0)` | NO | Self FK for split series |
| ENTRY_STATUS | `VARCHAR2(12 CHAR)` | YES | ACTIVE/ARCHIVED/CANCELLED |
| SOURCE_KIND | `VARCHAR2(20 CHAR)` | YES | USER/BANK_SYSTEM; SYSTEM only by authorized integration |
| SOURCE_CONTEXT_REF | `VARCHAR2(160 CHAR)` | NO | Opaque reference; no banking PII |
| VERSION_NO | `NUMBER(10,0)` | YES | Starts at 1; increment for mutation |
| CREATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC offset +00:00 |
| UPDATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC offset +00:00 |
| CREATED_BY_SUBJECT_ID | `VARCHAR2(128 CHAR)` | YES | Audit actor; never request supplied |
| UPDATED_BY_SUBJECT_ID | `VARCHAR2(128 CHAR)` | YES | Audit actor; never request supplied |
| DELETED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | Logical deletion / retention |

## CAL2.USER_CALENDAR_RECURRENCE_RULES

| Column | Oracle type candidate | Required | Constraints / meaning |
|---|---|---|---|
| RECURRENCE_RULE_ID | `NUMBER(19,0)` | PK | DEFAULT CAL2.SEQ_CAL2_UCR.NEXTVAL |
| USER_CALENDAR_ENTRY_ID | `NUMBER(12,0)` | YES | FK to entries |
| CALENDAR_VARIANT_ID | `NUMBER(19,0)` | YES | FK CAL2.CALENDAR_VARIANT |
| RULE_VERSION_NO | `NUMBER(10,0)` | YES | Immutable revision of recurrence rule |
| FREQUENCY | `VARCHAR2(12 CHAR)` | YES | DAILY/WEEKLY/MONTHLY/YEARLY |
| INTERVAL_NO | `NUMBER(4,0)` | YES | 1..9999 |
| BY_WEEKDAYS_MASK | `CHAR(7 CHAR)` | NO | Seven binary chars ISO Monday to Sunday |
| DAY_OF_MONTH | `NUMBER(2,0)` | NO | 1..31; variant validation |
| MONTH_OF_YEAR | `NUMBER(2,0)` | NO | 1..13; variant validation |
| NTH_WEEKDAY | `NUMBER(1,0)` | NO | ISO weekday 1..7 |
| NTH_ORDINAL | `NUMBER(2,0)` | NO | -1 or 1..5 |
| DAY_RESOLUTION_POLICY | `VARCHAR2(24 CHAR)` | YES | SKIP/LAST_DAY_IF_INVALID |
| UNTIL_DAY_ID | `NUMBER(19,0)` | NO | FK CAL2.CANONICAL_DAY |
| COUNT_LIMIT | `NUMBER(10,0)` | NO | Positive finite count |
| RULE_STATUS | `VARCHAR2(12 CHAR)` | YES | ACTIVE/SUPERSEDED/CANCELLED; max one ACTIVE per entry |
| CREATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC offset +00:00 |
| CREATED_BY_SUBJECT_ID | `VARCHAR2(128 CHAR)` | YES | Audit actor |

## CAL2.USER_CALENDAR_OCCURRENCES

| Column | Oracle type candidate | Required | Constraints / meaning |
|---|---|---|---|
| USER_CALENDAR_OCCURRENCE_ID | `NUMBER(19,0)` | PK | DEFAULT CAL2.SEQ_CAL2_UCO.NEXTVAL |
| USER_CALENDAR_ENTRY_ID | `NUMBER(12,0)` | YES | FK to entries |
| RECURRENCE_RULE_ID | `NUMBER(19,0)` | NO | FK to creating rule; null for one-time entry |
| ORIGINAL_SLOT_KEY | `VARCHAR2(160 CHAR)` | YES | Stable non-sensitive immutable key; UNIQUE(entry, slot) |
| ORIGINAL_DAY_ID | `NUMBER(19,0)` | YES | FK CAL2.CANONICAL_DAY; immutable |
| DAY_ID | `NUMBER(19,0)` | YES | FK CAL2.CANONICAL_DAY; effective date |
| LOCAL_START_MINUTE | `NUMBER(4,0)` | NO | 0..1439 for timed occurrences |
| DUE_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | UTC instant; null permitted for all-day |
| OCCURRENCE_STATUS | `VARCHAR2(12 CHAR)` | YES | OPEN/DONE/SKIPPED/CANCELLED |
| ORIGIN_RULE_VERSION_NO | `NUMBER(10,0)` | YES | 0 for one-time; actual revision for recurrence |
| IS_OVERRIDDEN | `CHAR(1 CHAR)` | YES | Y/N |
| COMPLETED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | Required when DONE |
| COMPLETED_BY_SUBJECT_ID | `VARCHAR2(128 CHAR)` | NO | Principal when DONE |
| VERSION_NO | `NUMBER(10,0)` | YES | Optimistic locking |
| CREATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC |
| UPDATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC |

## CAL2.USER_CALENDAR_REMINDER_RULES

| Column | Oracle type candidate | Required | Constraints / meaning |
|---|---|---|---|
| REMINDER_RULE_ID | `NUMBER(19,0)` | PK | DEFAULT CAL2.SEQ_CAL2_URR.NEXTVAL |
| USER_CALENDAR_ENTRY_ID | `NUMBER(12,0)` | YES | FK to entries |
| RULE_VERSION_NO | `NUMBER(10,0)` | YES | Increment on rule mutation; unique with rule ID in alerts |
| ALERT_PATTERN | `VARCHAR2(20 CHAR)` | YES | SINGLE_OFFSET/AT_DUE/DAILY_WINDOW |
| TRIGGER_RELATION | `VARCHAR2(12 CHAR)` | YES | BEFORE/AT_DUE; AFTER reserved |
| OFFSET_VALUE | `NUMBER(6,0)` | NO | Positive for SINGLE_OFFSET, zero for AT_DUE |
| OFFSET_UNIT | `VARCHAR2(20 CHAR)` | NO | CALENDAR_DAY/HOUR/MINUTE; no BUSINESS_DAY yet |
| WINDOW_START_DAYS | `NUMBER(3,0)` | NO | First days-before; only DAILY_WINDOW |
| WINDOW_END_DAYS | `NUMBER(3,0)` | NO | Last days-before >=1; only DAILY_WINDOW |
| WINDOW_INTERVAL_DAYS | `NUMBER(3,0)` | NO | Positive increment; only DAILY_WINDOW |
| LOCAL_ALERT_MINUTE | `NUMBER(4,0)` | NO | 0..1439; required for calendar-day and all-day reminder |
| CHANNEL_CODE | `VARCHAR2(20 CHAR)` | YES | IN_APP initially; external only after approval |
| ACK_STOP_POLICY | `VARCHAR2(28 CHAR)` | YES | NONE/STOP_FUTURE_ADVANCES; legacy ONCE compatibility only |
| IS_ACTIVE | `CHAR(1 CHAR)` | YES | Y/N |
| CREATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC |
| UPDATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC |
| CREATED_BY_SUBJECT_ID | `VARCHAR2(128 CHAR)` | YES | Audit actor |

## CAL2.USER_CALENDAR_ALERTS

| Column | Oracle type candidate | Required | Constraints / meaning |
|---|---|---|---|
| USER_CALENDAR_ALERT_ID | `NUMBER(19,0)` | PK | DEFAULT CAL2.SEQ_CAL2_UCA.NEXTVAL |
| USER_CALENDAR_OCCURRENCE_ID | `NUMBER(19,0)` | YES | FK to occurrences |
| USER_CALENDAR_ENTRY_ID | `NUMBER(12,0)` | YES | Same entry as occurrence and reminder; enforce composite FKs |
| REMINDER_RULE_ID | `NUMBER(19,0)` | YES | FK to reminder rules |
| RULE_VERSION_NO | `NUMBER(10,0)` | YES | Revision used to generate alert |
| SCHEDULED_SLOT_KEY | `VARCHAR2(160 CHAR)` | YES | Unique(occ,rule,revision,slot) |
| PLANNED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | Immutable planned instant UTC |
| NEXT_ELIGIBLE_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC; mutable for snooze |
| AVAILABLE_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | Time visible in inbox |
| ALERT_STATUS | `VARCHAR2(16 CHAR)` | YES | SCHEDULED/AVAILABLE/ACKNOWLEDGED/SNOOZED/DISMISSED/CANCELLED/EXPIRED |
| ACKNOWLEDGED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | Only when acknowledged |
| SNOOZED_UNTIL_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | Only when snoozed |
| SNOOZE_COUNT | `NUMBER(4,0)` | YES | Default zero; bounded by policy |
| LEASE_TOKEN | `VARCHAR2(80 CHAR)` | NO | Scheduler claim |
| LEASE_UNTIL_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | Scheduler timeout |
| ATTEMPT_COUNT | `NUMBER(6,0)` | YES | Nonnegative |
| VERSION_NO | `NUMBER(10,0)` | YES | Optimistic locking |
| CREATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC |
| UPDATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC |

## CAL2.USER_CALENDAR_AUDIT_LOG

| Column | Oracle type candidate | Required | Constraints / meaning |
|---|---|---|---|
| AUDIT_ID | `NUMBER(19,0)` | PK | DEFAULT CAL2.SEQ_CAL2_UAL.NEXTVAL |
| USER_CALENDAR_ENTRY_ID | `NUMBER(12,0)` | YES | FK to entries |
| AGGREGATE_KIND | `VARCHAR2(30 CHAR)` | YES | ENTRY/OCCURRENCE/REMINDER_RULE/ALERT |
| AGGREGATE_ID | `VARCHAR2(30 CHAR)` | YES | Decimal ID as string |
| ACTOR_ISSUER | `VARCHAR2(255 CHAR)` | YES | Validated issuer |
| ACTOR_SUBJECT_ID | `VARCHAR2(128 CHAR)` | YES | Validated actor subject |
| ACTION_CODE | `VARCHAR2(40 CHAR)` | YES | Controlled enum in application |
| ACTION_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC |
| CORRELATION_ID | `VARCHAR2(100 CHAR)` | NO | Non-sensitive correlation |
| OBJECT_VERSION_NO | `NUMBER(10,0)` | NO | Revision after mutation |
| BEFORE_SHA256 | `RAW(32)` | NO | Hash only; no full content |
| AFTER_SHA256 | `RAW(32)` | NO | Hash only; no full content |
| CHANGE_METADATA | `CLOB` | NO | Sanitized allowlisted metadata; no PII |

## CAL2.USER_CALENDAR_REQUEST_KEYS

| Column | Oracle type candidate | Required | Constraints / meaning |
|---|---|---|---|
| REQUEST_KEY_ID | `NUMBER(19,0)` | PK | DEFAULT CAL2.SEQ_CAL2_URK.NEXTVAL |
| OWNER_ISSUER | `VARCHAR2(255 CHAR)` | YES | Part of unique owner+operation+key |
| OWNER_SUBJECT_ID | `VARCHAR2(128 CHAR)` | YES | Part of unique owner+operation+key |
| OPERATION_CODE | `VARCHAR2(40 CHAR)` | YES | POST_ENTRY/SPLIT_SERIES/... |
| IDEMPOTENCY_KEY | `VARCHAR2(128 CHAR)` | YES | Opaque client command key |
| REQUEST_SHA256 | `RAW(32)` | YES | Canonical request fingerprint; no payload |
| REQUEST_STATUS | `VARCHAR2(16 CHAR)` | YES | IN_PROGRESS/COMPLETED/FAILED |
| RESOURCE_KIND | `VARCHAR2(20 CHAR)` | NO | Entry etc. |
| RESOURCE_ID | `VARCHAR2(30 CHAR)` | NO | Decimal ID string |
| RESPONSE_STATUS | `NUMBER(3,0)` | NO | HTTP status of completed mutation |
| CREATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC |
| COMPLETED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | UTC |
| EXPIRES_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | Governance retention policy |

## CAL2.USER_CALENDAR_ALERT_DELIVERIES

| Column | Oracle type candidate | Required | Constraints / meaning |
|---|---|---|---|
| DELIVERY_ID | `NUMBER(19,0)` | PK | DEFAULT CAL2.SEQ_CAL2_UCD.NEXTVAL |
| USER_CALENDAR_ALERT_ID | `NUMBER(19,0)` | YES | FK to alerts |
| CHANNEL_CODE | `VARCHAR2(20 CHAR)` | YES | External channel, not IN_APP |
| ATTEMPT_NO | `NUMBER(5,0)` | YES | Positive; unique(alert,channel,attempt) |
| DELIVERY_STATUS | `VARCHAR2(16 CHAR)` | YES | PENDING/LEASED/SENT/RETRY/FAILED_FINAL |
| PROVIDER_MESSAGE_REF | `VARCHAR2(160 CHAR)` | NO | Non-sensitive provider reference |
| FAILURE_CLASS | `VARCHAR2(40 CHAR)` | NO | Categorized failure, no provider payload |
| ATTEMPTED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | UTC |
| NEXT_RETRY_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | UTC |
| COMPLETED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | NO | UTC |
| CREATED_AT_UTC | `TIMESTAMP(6) WITH TIME ZONE` | YES | UTC |

## Constraints to validate during physical DDL design

- PK, FK and no cascaded deletion across Entries/Rules/Occurrences/Alerts/Audit.
- Entry sequence start must be HIGHER than both migrated maximum ID and suitable old sequence high-water. Do not call NEXTVAL in preflight.
- Conditional uniqueness for a single ACTIVE recurrence rule per Entry (Oracle function-based unique index or reviewed virtual-column approach).
- Recurrence: positive INTERVAL, resolved WEEKLY mask, month/day shape and allowed DAY_RESOLUTION_POLICY.
- Reminder pattern-specific CHECK constraints; some cross-table conditions must be service-enforced.
- Occurrence UNIQUE (ENTRY_ID, ORIGINAL_SLOT_KEY); Alert UNIQUE (OCCURRENCE_ID, RULE_ID, RULE_VERSION_NO, SCHEDULED_SLOT_KEY).
- Important privacy integrity: add unique (OCCURRENCE_ID, ENTRY_ID) and (REMINDER_RULE_ID, ENTRY_ID) on parents; Alert carries ENTRY_ID and composite FKs to BOTH, preventing cross-entry/owner pairing.
- Occurrence recurrence rule must also belong to the same Entry via composite FK (RECURRENCE_RULE_ID, ENTRY_ID) to Recurrence Rules.
- Request key UNIQUE (OWNER_ISSUER, OWNER_SUBJECT_ID, OPERATION_CODE, IDEMPOTENCY_KEY); check combined index size for database block size and character set.
- All local minutes 0..1439; no automatic midnight for ALL_DAY; explicit time for timed reminders.
- Time zone correctness cannot be guaranteed by VARCHAR2 CHECK alone: validate via Java ZoneId and controlled policy.
- Keep all old R1B tables and sequences intact during shadow migration and cutover.
