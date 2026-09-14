package com.behsazan.corebanking.deposit.opening.audit.application;

import com.behsazan.corebanking.deposit.opening.audit.domain.DepositOpeningAuditModels.*;
import com.behsazan.corebanking.deposit.opening.audit.oracle.DepositOpeningAuditRepository;
import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.OpeningRequest;
import com.behsazan.corebanking.deposit.opening.error.DepositOpeningValidationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class DepositOpeningAuditService {
    private static final String ROOT_ENTITY = "DEPOSIT_OPENING_REQUEST";


    private final DepositOpeningAuditRepository repository;
    private final JsonMapper objectMapper;
    private final Set<String> sensitiveFields;

    public DepositOpeningAuditService(
            DepositOpeningAuditRepository repository,
            JsonMapper objectMapper,
            @Value("${core-banking.deposit-opening.audit.sensitive-fields:}") String sensitiveFields
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.sensitiveFields = parseSensitiveFields(sensitiveFields);
    }

    @Transactional
    public ChangeSetView createChangeSet(
            long openingRequestId,
            ChangeSetCreateRequest request,
            String actor,
            String correlationId
    ) {
        OpeningAuditHeader opening = requireOpeningForChange(openingRequestId);
        String changeTypeCode = upper(request == null ? null : request.changeTypeCode());
        String reasonCode = upper(request == null ? null : request.changeReasonCode());
        String reasonNote = trim(request == null ? null : request.changeReasonNote());

        if (blank(changeTypeCode)) {
            throw validation("نوع Change Set الزامی است.", "changeTypeCode", "CHANGE_TYPE_CODE الزامی است.");
        }
        if (!repository.activeReferenceCode("REF_DEP_OPEN_CHANGE_TYPE", "CHANGE_TYPE_CODE", changeTypeCode)) {
            throw validation("نوع Change Set معتبر نیست.", "changeTypeCode", "کد در REF_DEP_OPEN_CHANGE_TYPE فعال نیست.");
        }
        if (!blank(reasonCode) && !repository.activeReferenceCode("REF_DEP_OPEN_CHANGE_REASON", "CHANGE_REASON_CODE", reasonCode)) {
            throw validation("علت Change Set معتبر نیست.", "changeReasonCode", "کد در REF_DEP_OPEN_CHANGE_REASON فعال نیست.");
        }
        if ("OTHER".equals(reasonCode) && blank(reasonNote)) {
            throw validation("برای علت OTHER توضیح لازم است.", "changeReasonNote", "شرح علت را وارد کنید.");
        }

        long changeSetId = repository.nextChangeSetId();
        long changeNo = repository.nextChangeNo(openingRequestId);
        repository.insertChangeSet(
                changeSetId, openingRequestId, changeNo,
                changeTypeCode, reasonCode, reasonNote,
                opening.recordVersion(), actor, correlationId
        );
        insertAuditEvent(
                repository.nextAuditEventId(), opening, changeSetId,
                "CHANGE_REQUESTED", actor, correlationId,
                reasonCode, reasonNote, null,
                opening.recordVersion(), opening.recordVersion(),
                "CREATE_CHANGE_SET"
        );
        return repository.findChangeSet(openingRequestId, changeSetId, false)
                .orElseThrow(() -> validation("Change Set ثبت شد اما Read-back ناموفق بود.", "changeSetId", "رکورد یافت نشد."));
    }

    @Transactional
    public ChangeSetView approveChangeSet(
            long openingRequestId,
            long changeSetId,
            String actor,
            String correlationId
    ) {
        OpeningAuditHeader opening = requireOpeningForChange(openingRequestId);
        ChangeSetView changeSet = requireChangeSet(openingRequestId, changeSetId, true);
        if (!"DRAFT".equals(upper(changeSet.changeStatusCode()))) {
            throw validation("فقط Change Set در وضعیت DRAFT قابل تأیید است.", "changeStatusCode", "وضعیت مورد انتظار DRAFT است.");
        }
        if (!Objects.equals(changeSet.baseRequestVersion(), opening.recordVersion())) {
            throw validation("نسخه Opening پس از ایجاد Change Set تغییر کرده است.", "baseRequestVersion", "Change Set باید دوباره روی نسخه جاری ایجاد شود.");
        }
        if (repository.approveChangeSet(openingRequestId, changeSetId, actor) != 1) {
            throw validation("Change Set قابل تأیید نبود.", "changeSetId", "Transition همزمان یا نامعتبر رخ داده است.");
        }
        insertAuditEvent(
                repository.nextAuditEventId(), opening, changeSetId,
                "CHANGE_APPROVED", actor, correlationId,
                changeSet.changeReasonCode(), changeSet.changeReasonNote(), null,
                opening.recordVersion(), opening.recordVersion(),
                "APPROVE_CHANGE_SET"
        );
        return requireChangeSet(openingRequestId, changeSetId, false);
    }

    @Transactional
    public ChangeSetView rejectChangeSet(
            long openingRequestId,
            long changeSetId,
            ChangeSetActionRequest request,
            String actor,
            String correlationId
    ) {
        OpeningAuditHeader opening = requireOpeningForChange(openingRequestId);
        ChangeSetView changeSet = requireChangeSet(openingRequestId, changeSetId, true);
        String status = upper(changeSet.changeStatusCode());
        if (!Set.of("DRAFT", "APPROVED").contains(status)) {
            throw validation("Change Set در وضعیت فعلی قابل رد نیست.", "changeStatusCode", "فقط DRAFT یا APPROVED قابل رد است.");
        }
        String note = trim(request == null ? null : request.note());
        if (repository.rejectChangeSet(openingRequestId, changeSetId, actor, note) != 1) {
            throw validation("رد Change Set انجام نشد.", "changeSetId", "Transition همزمان یا نامعتبر رخ داده است.");
        }
        insertAuditEvent(
                repository.nextAuditEventId(), opening, changeSetId,
                "CHANGE_REJECTED", actor, correlationId,
                changeSet.changeReasonCode(), note == null ? changeSet.changeReasonNote() : note, null,
                opening.recordVersion(), opening.recordVersion(),
                "REJECT_CHANGE_SET"
        );
        return requireChangeSet(openingRequestId, changeSetId, false);
    }

    @Transactional
    public ChangeSetApplyResponse applyChangeSet(
            long openingRequestId,
            long changeSetId,
            ApplyChangeSetRequest request,
            String actor,
            String correlationId
    ) {
        OpeningAuditHeader opening = repository.lockOpening(openingRequestId)
                .orElseThrow(() -> validation("پرونده افتتاح یافت نشد.", "openingRequestId", "Opening معتبر نیست."));
        if ("COMPLETED".equals(upper(opening.requestStatusCode()))) {
            throw validation(
                    "Opening تکمیل‌شده از مسیر Change Management قابل تغییر ماهیتی نیست.",
                    "requestStatusCode", "پس از COMPLETED تغییرات ماهیتی متعلق به Deposit Account Operations است."
            );
        }
        ChangeSetView changeSet = requireChangeSet(openingRequestId, changeSetId, true);
        if (!"APPROVED".equals(upper(changeSet.changeStatusCode()))) {
            throw validation("اعمال تغییر فقط برای Change Set تأییدشده مجاز است.", "changeStatusCode", "وضعیت مورد انتظار APPROVED است.");
        }
        if (!Objects.equals(changeSet.baseRequestVersion(), opening.recordVersion())) {
            throw validation(
                    "Change Set روی نسخه قدیمی Opening ایجاد شده است.",
                    "baseRequestVersion", "نسخه پایه " + changeSet.baseRequestVersion() + " با نسخه جاری " + opening.recordVersion() + " برابر نیست."
            );
        }

        List<FieldMutationRequest> requested = request == null || request.changes() == null ? List.of() : request.changes();
        if (requested.isEmpty()) {
            throw validation("حداقل یک تغییر فیلدی لازم است.", "changes", "فهرست تغییرات خالی است.");
        }

        LinkedHashMap<EntityTarget, List<PreparedMutation>> grouped = new LinkedHashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        for (FieldMutationRequest mutation : requested) {
            if (mutation == null) continue;
            String requestedEntity = upper(mutation.entityName());
            final String entity = blank(requestedEntity) ? ROOT_ENTITY : requestedEntity;
            if (!repository.changeableEntity(entity)) {
                throw validation("موجودیت برای Change Management مجاز نیست.", "changes.entityName", entity);
            }
            long entityId;
            if (ROOT_ENTITY.equals(entity)) {
                entityId = mutation.entityId() == null ? openingRequestId : mutation.entityId();
                if (entityId != openingRequestId) {
                    throw validation("شناسه Root Opening نامعتبر است.", "changes.entityId", "برای DEPOSIT_OPENING_REQUEST باید برابر OPENING_REQUEST_ID باشد.");
                }
            } else {
                if (mutation.entityId() == null || mutation.entityId() <= 0) {
                    throw validation("شناسه رکورد Child الزامی است.", "changes.entityId", "برای موجودیت‌های Child شناسه PK را ارسال کنید.");
                }
                entityId = mutation.entityId();
            }
            String field = upper(mutation.fieldName());
            if (blank(field)) {
                throw validation("نام فیلد تغییر الزامی است.", "changes.fieldName", "FIELD_NAME خالی است.");
            }
            String uniqueKey = entity + ":" + entityId + ":" + field;
            if (!seen.add(uniqueKey)) {
                throw validation("یک فیلد برای یک رکورد بیش از یک بار ارسال شده است.", "changes.fieldName", uniqueKey);
            }
            EntityFieldMetadata metadata = repository.mutableFieldMetadata(entity, field)
                    .orElseThrow(() -> validation(
                            "فیلد برای Change Management مجاز نیست.",
                            "changes.fieldName", entity + "." + field + " در whitelist/metadata مجاز نیست."
                    ));
            EntityTarget target = new EntityTarget(entity, entityId);
            grouped.computeIfAbsent(target, ignored -> new ArrayList<>())
                    .add(new PreparedMutation(field, mutation.newValue(), metadata));
        }
        if (grouped.isEmpty()) {
            throw validation("حداقل یک تغییر معتبر لازم است.", "changes", "فهرست تغییرات معتبر خالی است.");
        }

        List<TargetPlan> plans = new ArrayList<>();
        for (Map.Entry<EntityTarget, List<PreparedMutation>> entry : grouped.entrySet()) {
            EntityTarget target = entry.getKey();
            List<String> fields = entry.getValue().stream().map(PreparedMutation::fieldName).toList();
            EntityRowState state = repository.lockEntityFields(openingRequestId, target.entityName(), target.entityId(), fields)
                    .orElseThrow(() -> validation(
                            "رکورد هدف در Opening Aggregate یافت نشد.",
                            "changes.entityId", target.entityName() + ":" + target.entityId()
                    ));
            LinkedHashMap<String, Object> updates = new LinkedHashMap<>();
            LinkedHashMap<String, AuditValue> auditValues = new LinkedHashMap<>();
            for (PreparedMutation mutation : entry.getValue()) {
                Object oldValue = state.values().get(mutation.fieldName());
                Object newValue = convertValue(mutation.fieldName(), mutation.rawValue(), mutation.metadata());
                if (!mutation.metadata().nullable() && newValue == null) {
                    throw validation("فیلد اجباری قابل پاک‌کردن نیست.", "changes." + mutation.fieldName(), target.entityName());
                }
                if (sameValue(oldValue, newValue)) continue;
                updates.put(mutation.fieldName(), newValue);
                auditValues.put(mutation.fieldName(), new AuditValue(oldValue, newValue, mutation.metadata().dataType()));
            }
            if (!updates.isEmpty()) plans.add(new TargetPlan(target, state.recordVersion(), updates, auditValues));
        }
        if (plans.isEmpty()) {
            throw validation("هیچ تغییر مؤثری برای اعمال وجود ندارد.", "changes", "مقادیر جدید با مقادیر جاری برابر هستند.");
        }

        long preSnapshotId = captureSnapshot(
                openingRequestId, changeSetId, "PRE_CHANGE", opening.recordVersion(), actor, correlationId
        );
        boolean rootChanged = false;
        for (TargetPlan plan : plans) {
            if (repository.updateEntityFields(
                    plan.target().entityName(), plan.target().entityId(), plan.rowVersion(), plan.updates(), actor
            ) != 1) {
                throw validation("رکورد هدف همزمان تغییر کرده است.", "recordVersion",
                        plan.target().entityName() + ":" + plan.target().entityId());
            }
            if (ROOT_ENTITY.equals(plan.target().entityName())) rootChanged = true;
        }
        if (!rootChanged && repository.bumpOpeningVersion(openingRequestId, opening.recordVersion(), actor) != 1) {
            throw validation("Opening همزمان تغییر کرده است.", "recordVersion", "Optimistic concurrency check ناموفق بود.");
        }
        long newVersion = opening.recordVersion() + 1;

        List<Long> auditEventIds = new ArrayList<>();
        List<AuditFieldChangeView> persistedFieldChanges = new ArrayList<>();
        String operationName = trim(request == null ? null : request.operationName());
        if (operationName == null) operationName = "APPLY_CHANGE_SET";
        String approvalReference = trim(request == null ? null : request.approvalReference());

        for (TargetPlan plan : plans) {
            long auditEventId = repository.nextAuditEventId();
            auditEventIds.add(auditEventId);
            insertEntityAuditEvent(
                    auditEventId, opening, changeSetId, plan.target(),
                    "CHANGE_APPLIED", actor, correlationId,
                    changeSet.changeReasonCode(), changeSet.changeReasonNote(), approvalReference,
                    opening.recordVersion(), newVersion, operationName
            );
            for (Map.Entry<String, AuditValue> entry : plan.auditValues().entrySet()) {
                String field = entry.getKey();
                AuditValue value = entry.getValue();
                String oldText = canonicalText(value.oldValue());
                String newText = canonicalText(value.newValue());
                String changeType = oldText == null ? "SET" : newText == null ? "CLEAR" : "UPDATE";
                boolean sensitive = isSensitiveField(plan.target().entityName(), field);
                String storedOld = maskedValue(oldText, sensitive);
                String storedNew = maskedValue(newText, sensitive);
                long fieldChangeId = repository.nextFieldChangeId();
                repository.insertFieldChange(
                        fieldChangeId, auditEventId, field, changeType,
                        storedOld, storedNew, hash(oldText), hash(newText), value.dataTypeCode(), sensitive, sensitive
                );
                persistedFieldChanges.add(new AuditFieldChangeView(
                        fieldChangeId, auditEventId, field, changeType,
                        storedOld, storedNew, hash(oldText), hash(newText), value.dataTypeCode(), sensitive, sensitive
                ));
            }
        }

        long postSnapshotId = captureSnapshot(
                openingRequestId, changeSetId, "POST_CHANGE", newVersion, actor, correlationId
        );
        if (repository.markChangeSetApplied(openingRequestId, changeSetId, newVersion, actor) != 1) {
            throw validation("Change Set اعمال شد اما وضعیت آن قابل نهایی‌سازی نبود.", "changeSetId", "Transition APPROVED -> APPLIED ناموفق بود.");
        }
        ChangeSetView applied = requireChangeSet(openingRequestId, changeSetId, false);
        return new ChangeSetApplyResponse(
                applied, List.copyOf(auditEventIds), preSnapshotId, postSnapshotId,
                newVersion, List.copyOf(persistedFieldChanges)
        );
    }

    @Transactional(readOnly = true)
    public AuditTrailResponse auditTrail(long openingRequestId) {
        OpeningAuditHeader opening = repository.findOpening(openingRequestId)
                .orElseThrow(() -> validation("پرونده افتتاح یافت نشد.", "openingRequestId", "Opening معتبر نیست."));
        return new AuditTrailResponse(
                opening,
                repository.listChangeSets(openingRequestId),
                repository.listAuditEvents(openingRequestId),
                repository.listStatusHistory(openingRequestId),
                repository.listSnapshots(openingRequestId)
        );
    }

    @Transactional(readOnly = true)
    public SnapshotDocument snapshot(long openingRequestId, long snapshotId) {
        repository.findOpening(openingRequestId)
                .orElseThrow(() -> validation("پرونده افتتاح یافت نشد.", "openingRequestId", "Opening معتبر نیست."));
        return repository.findSnapshot(openingRequestId, snapshotId)
                .orElseThrow(() -> validation("Snapshot یافت نشد.", "snapshotId", "Snapshot متعلق به این Opening نیست."));
    }

    @Transactional
    public OpeningCreateAuditResult recordOpeningCreated(
            long openingRequestId,
            OpeningRequest request,
            String actor,
            String correlationId
    ) {
        OpeningAuditHeader opening = repository.findOpening(openingRequestId)
                .orElseThrow(() -> validation("Opening پس از ثبت یافت نشد.", "openingRequestId", "Read-back ناموفق بود."));
        long auditEventId = repository.nextAuditEventId();
        repository.insertAuditEvent(
                auditEventId, openingRequestId, null,
                "DEPOSIT_OPENING_REQUEST", "OPENING_REQUEST_ID=" + openingRequestId,
                "CREATE", "USER", actor, "OPENING_OPERATOR",
                request == null ? null : request.openingChannelCode(),
                request == null ? null : request.orgUnitCode(),
                "CORE_BANKING_PROTOTYPE", "CREATE_OPENING",
                null, null, null, correlationId,
                0L, opening.recordVersion(), actor
        );
        int statusHistoryRows = repository.insertStatusHistory(
                openingRequestId, null, opening.requestStatusCode(), null, auditEventId,
                null, "Initial opening status", actor, correlationId
        );
        Long snapshotId = null;
        String status = upper(opening.requestStatusCode());
        if (Set.of("SUBMITTED", "APPROVED", "COMPLETED").contains(status)) {
            snapshotId = captureSnapshot(openingRequestId, null, status, opening.recordVersion(), actor, correlationId);
        }
        return new OpeningCreateAuditResult(auditEventId, statusHistoryRows, snapshotId);
    }

    @Transactional
    public AuditMutationResult recordAccountLinked(
            long openingRequestId,
            long accountId,
            String actor,
            String correlationId
    ) {
        OpeningAuditHeader opening = repository.findOpening(openingRequestId)
                .orElseThrow(() -> validation("Opening برای Audit اتصال حساب یافت نشد.", "openingRequestId", "Read-back ناموفق بود."));
        long after = opening.recordVersion();
        long before = Math.max(1L, after - 1L);
        long auditEventId = repository.nextAuditEventId();
        repository.insertAuditEvent(
                auditEventId, openingRequestId, null,
                "DEPOSIT_OPENING_REQUEST", "OPENING_REQUEST_ID=" + openingRequestId,
                "ACCOUNT_LINKED", "SYSTEM", actor, "ACCOUNT_OPERATIONS",
                null, null, "CORE_BANKING_PROTOTYPE", "LINK_CREATED_ACCOUNT",
                null, null, null, correlationId, before, after, actor
        );
        long fieldChangeId = repository.nextFieldChangeId();
        String newValue = Long.toString(accountId);
        int rows = repository.insertFieldChange(
                fieldChangeId, auditEventId, "CREATED_ACCOUNT_ID", "SET",
                null, newValue, null, hash(newValue), "NUMBER", false, false
        );
        return new AuditMutationResult(auditEventId, rows, null);
    }

    @Transactional
    public AuditMutationResult recordOpeningCompleted(
            long openingRequestId,
            String actor,
            String correlationId
    ) {
        OpeningAuditHeader opening = repository.findOpening(openingRequestId)
                .orElseThrow(() -> validation("Opening برای Audit تکمیل یافت نشد.", "openingRequestId", "Read-back ناموفق بود."));
        if (!"COMPLETED".equals(upper(opening.requestStatusCode()))) {
            throw validation("Opening هنوز COMPLETED نیست.", "requestStatusCode", opening.requestStatusCode());
        }
        long after = opening.recordVersion();
        long before = Math.max(1L, after - 1L);
        long auditEventId = repository.nextAuditEventId();
        repository.insertAuditEvent(
                auditEventId, openingRequestId, null,
                "DEPOSIT_OPENING_REQUEST", "OPENING_REQUEST_ID=" + openingRequestId,
                "STATUS_CHANGE", "SYSTEM", actor, "ACCOUNT_OPERATIONS",
                null, null, "CORE_BANKING_PROTOTYPE", "COMPLETE_OPENING_AFTER_ACCOUNT_ACTIVATION",
                null, "Deposit account activated", null, correlationId, before, after, actor
        );
        long fieldChangeId = repository.nextFieldChangeId();
        int fieldRows = repository.insertFieldChange(
                fieldChangeId, auditEventId, "REQUEST_STATUS_CODE", "UPDATE",
                "APPROVED", "COMPLETED", hash("APPROVED"), hash("COMPLETED"), "VARCHAR2", false, false
        );
        repository.insertStatusHistory(
                openingRequestId, "APPROVED", "COMPLETED", null, auditEventId,
                null, "Deposit account activated", actor, correlationId
        );
        long snapshotId = captureSnapshot(
                openingRequestId, null, "COMPLETED", opening.recordVersion(), actor, correlationId
        );
        return new AuditMutationResult(auditEventId, fieldRows, snapshotId);
    }

    private OpeningAuditHeader requireOpeningForChange(long openingRequestId) {
        OpeningAuditHeader opening = repository.lockOpening(openingRequestId)
                .orElseThrow(() -> validation("پرونده افتتاح یافت نشد.", "openingRequestId", "Opening معتبر نیست."));
        if ("COMPLETED".equals(upper(opening.requestStatusCode()))) {
            throw validation(
                    "پرونده Opening تکمیل‌شده فقط سابقه تاریخی است.",
                    "requestStatusCode", "تغییرات ماهیتی بعد از COMPLETED باید در Deposit Account Operations انجام شود."
            );
        }
        return opening;
    }

    private ChangeSetView requireChangeSet(long openingRequestId, long changeSetId, boolean lock) {
        return repository.findChangeSet(openingRequestId, changeSetId, lock)
                .orElseThrow(() -> validation("Change Set یافت نشد.", "changeSetId", "Change Set متعلق به این Opening نیست."));
    }

    private long captureSnapshot(
            long openingRequestId,
            Long changeSetId,
            String snapshotTypeCode,
            long requestVersion,
            String actor,
            String correlationId
    ) {
        Map<String, Object> payload = repository.canonicalOpeningSnapshot(openingRequestId);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize canonical Deposit Opening snapshot.", exception);
        }
        long snapshotId = repository.nextSnapshotId();
        repository.insertSnapshot(
                snapshotId, openingRequestId, changeSetId, snapshotTypeCode,
                requestVersion, json, hash(json), correlationId, actor
        );
        return snapshotId;
    }

    private void insertAuditEvent(
            long auditEventId,
            OpeningAuditHeader opening,
            Long changeSetId,
            String eventTypeCode,
            String actor,
            String correlationId,
            String reasonCode,
            String reasonNote,
            String approvalReference,
            Long versionBefore,
            Long versionAfter,
            String operationName
    ) {
        Map<String, Object> state = repository.lockMutableFieldState(opening.openingRequestId())
                .map(MutableFieldState::values)
                .orElse(Map.of());
        repository.insertAuditEvent(
                auditEventId, opening.openingRequestId(), changeSetId,
                "DEPOSIT_OPENING_REQUEST", "OPENING_REQUEST_ID=" + opening.openingRequestId(),
                eventTypeCode, "USER", actor, "OPENING_OPERATOR",
                canonicalText(state.get("OPENING_CHANNEL_CODE")),
                canonicalText(state.get("ORG_UNIT_CODE")),
                "CORE_BANKING_PROTOTYPE", operationName,
                reasonCode, reasonNote, approvalReference, correlationId,
                versionBefore, versionAfter, actor
        );
    }

    private void insertEntityAuditEvent(
            long auditEventId,
            OpeningAuditHeader opening,
            Long changeSetId,
            EntityTarget target,
            String eventTypeCode,
            String actor,
            String correlationId,
            String reasonCode,
            String reasonNote,
            String approvalReference,
            Long versionBefore,
            Long versionAfter,
            String operationName
    ) {
        Map<String, Object> rootState = repository.lockMutableFieldState(opening.openingRequestId())
                .map(MutableFieldState::values)
                .orElse(Map.of());
        repository.insertAuditEvent(
                auditEventId, opening.openingRequestId(), changeSetId,
                target.entityName(), entityKey(target),
                eventTypeCode, "USER", actor, "OPENING_OPERATOR",
                canonicalText(rootState.get("OPENING_CHANNEL_CODE")),
                canonicalText(rootState.get("ORG_UNIT_CODE")),
                "CORE_BANKING_PROTOTYPE", operationName,
                reasonCode, reasonNote, approvalReference, correlationId,
                versionBefore, versionAfter, actor
        );
    }

    private static String entityKey(EntityTarget target) {
        return target.entityName() + "#" + target.entityId();
    }

    private static Object convertValue(String field, Object raw, EntityFieldMetadata metadata) {
        if (raw == null) return null;
        if (raw instanceof String text && text.isBlank()) return null;
        String type = upper(metadata.dataType());
        try {
            if (type != null && (type.startsWith("VARCHAR") || type.startsWith("NVARCHAR") || type.equals("CHAR") || type.equals("NCHAR"))) {
                String value = raw.toString().trim();
                if (field.endsWith("_CODE")) value = value.toUpperCase(Locale.ROOT);
                if (metadata.charLength() != null && metadata.charLength() > 0 && value.length() > metadata.charLength()) {
                    throw new IllegalArgumentException("too long");
                }
                return value;
            }
            if (type != null && (type.equals("NUMBER") || type.equals("DECIMAL") || type.equals("NUMERIC"))) {
                BigDecimal decimal = raw instanceof BigDecimal d ? d : new BigDecimal(raw.toString().trim());
                if ((metadata.scale() == null || metadata.scale() == 0) && decimal.scale() <= 0) return decimal.longValueExact();
                return decimal;
            }
            if ("DATE".equals(type)) {
                return raw instanceof LocalDate date ? date : LocalDate.parse(raw.toString().trim());
            }
            if (type != null && type.startsWith("TIMESTAMP")) {
                if (raw instanceof LocalDateTime || raw instanceof Timestamp) return raw;
                if (raw instanceof OffsetDateTime dateTime) return dateTime.toLocalDateTime();
                String value = raw.toString().trim();
                try { return OffsetDateTime.parse(value).toLocalDateTime(); }
                catch (RuntimeException ignored) { return LocalDateTime.parse(value); }
            }
            throw new IllegalArgumentException("unsupported type");
        } catch (RuntimeException exception) {
            throw validation("مقدار جدید فیلد معتبر نیست.", "changes." + field, "نوع مورد انتظار " + metadata.dataType() + " است.");
        }
    }

    private static boolean sameValue(Object left, Object right) {
        if (left instanceof BigDecimal a && right instanceof BigDecimal b) return a.compareTo(b) == 0;
        return Objects.equals(canonicalText(left), canonicalText(right));
    }

    private static String canonicalText(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
        return value.toString();
    }

    private static String hash(String value) {
        if (value == null) return null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) builder.append(String.format(Locale.ROOT, "%02x", item));
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private boolean isSensitiveField(String entityName, String fieldName) {
        return sensitiveFields.contains(fieldName) || sensitiveFields.contains(entityName + "." + fieldName);
    }

    private static String maskedValue(String value, boolean sensitive) {
        return sensitive && value != null ? "[MASKED]" : value;
    }

    private static Set<String> parseSensitiveFields(String configured) {
        if (configured == null || configured.isBlank()) return Set.of();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : configured.split(",")) {
            String normalized = upper(item);
            if (!blank(normalized)) values.add(normalized);
        }
        return Set.copyOf(values);
    }

    private static DepositOpeningValidationException validation(String message, String field, String detail) {
        return new DepositOpeningValidationException(message, Map.of(field, detail == null ? message : detail));
    }

    private static String upper(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record AuditValue(Object oldValue, Object newValue, String dataTypeCode) {
    }

    private record EntityTarget(String entityName, long entityId) {
    }

    private record PreparedMutation(String fieldName, Object rawValue, EntityFieldMetadata metadata) {
    }

    private record TargetPlan(
            EntityTarget target,
            long rowVersion,
            Map<String, Object> updates,
            Map<String, AuditValue> auditValues
    ) {
    }

}
