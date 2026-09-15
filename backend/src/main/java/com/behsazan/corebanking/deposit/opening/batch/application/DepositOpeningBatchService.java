package com.behsazan.corebanking.deposit.opening.batch.application;

import com.behsazan.corebanking.deposit.account.application.DepositAccountLifecycleService;
import com.behsazan.corebanking.deposit.account.domain.DepositAccountModels.AccountLifecycleResponse;
import com.behsazan.corebanking.deposit.opening.audit.application.DepositOpeningAuditService;
import com.behsazan.corebanking.deposit.opening.batch.domain.DepositOpeningBatchModels.*;
import com.behsazan.corebanking.deposit.opening.batch.oracle.DepositOpeningBatchRepository;
import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.OpeningParty;
import com.behsazan.corebanking.deposit.opening.domain.DepositOpeningModels.OpeningRequest;
import com.behsazan.corebanking.deposit.opening.error.DepositOpeningValidationException;
import com.behsazan.corebanking.deposit.opening.oracle.DepositOpeningAggregateRepository;
import com.behsazan.corebanking.deposit.opening.readiness.application.DepositOpeningRuntimeValidator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class DepositOpeningBatchService {
    private static final Set<String> CREATEABLE_STATUSES = Set.of("DRAFT", "FAILED", "READY", "PARTIAL");
    
    private final DepositOpeningBatchRepository repository;
    private final DepositOpeningAggregateRepository aggregateRepository;
    private final DepositOpeningAuditService auditService;
    private final DepositAccountLifecycleService accountLifecycleService;
    private final DepositOpeningRuntimeValidator runtimeValidator;
    private final TransactionTemplate requiresNew;

    public DepositOpeningBatchService(
            DepositOpeningBatchRepository repository,
            DepositOpeningAggregateRepository aggregateRepository,
            DepositOpeningAuditService auditService,
            DepositAccountLifecycleService accountLifecycleService,
            DepositOpeningRuntimeValidator runtimeValidator,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.aggregateRepository = aggregateRepository;
        this.auditService = auditService;
        this.accountLifecycleService = accountLifecycleService;
        this.runtimeValidator = runtimeValidator;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public BatchView create(BatchCreateRequest raw, String actor) {
        BatchCreateRequest request = normalize(raw);
        validateCreate(request);

        BatchHeaderView existing = repository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) return view(existing.openingBatchId(), true);

        if (!repository.referenceCodeExists("REF_DEP_OPEN_BATCH_SOURCE_TYPE", "SOURCE_TYPE_CODE", request.sourceTypeCode())) {
            throw validation("نوع منبع Batch معتبر/فعال نیست.", "DEPOSIT_OPENING_BATCH.SOURCE_TYPE_CODE", request.sourceTypeCode());
        }

        long batchId = repository.nextBatchId();
        String batchNo = blank(request.batchNo()) ? "BOP-" + batchId : request.batchNo();
        try {
            repository.insertBatch(batchId, batchNo, request.idempotencyKey(), request.sourceTypeCode(),
                    request.sourceReference(), request.items().size(), actor);
        } catch (DuplicateKeyException ex) {
            BatchHeaderView concurrent = repository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
            if (concurrent != null) return view(concurrent.openingBatchId(), true);
            throw ex;
        }

        int fallbackRowNo = 1;
        for (BatchItemCreateRequest item : request.items()) {
            int rowNo = item.rowNo() == null ? fallbackRowNo : item.rowNo();
            String externalKey = blank(item.externalRowKey()) ? "ROW-" + rowNo : item.externalRowKey();
            BatchItemCreateRequest normalizedItem = new BatchItemCreateRequest(
                    rowNo, externalKey, item.partyId(), item.productVersionId(), upper(item.currencyCode()), item.openingAmount()
            );
            repository.insertItem(repository.nextBatchItemId(), batchId, normalizedItem, rowNo, actor);
            fallbackRowNo++;
        }
        return view(batchId, false);
    }

    @Transactional(readOnly = true)
    public BatchView get(long batchId) {
        return view(batchId, false);
    }

    @Transactional
    public BatchView validate(long batchId, String actor) {
        BatchHeaderView batch = repository.lockBatch(batchId)
                .orElseThrow(() -> validation("Batch یافت نشد.", "DEPOSIT_OPENING_BATCH.OPENING_BATCH_ID", String.valueOf(batchId)));
        if (!CREATEABLE_STATUSES.contains(upper(batch.batchStatusCode()))) {
            throw validation("Batch در وضعیت قابل اعتبارسنجی نیست.", "DEPOSIT_OPENING_BATCH.BATCH_STATUS_CODE", batch.batchStatusCode());
        }
        repository.updateBatchStatus(batchId, "VALIDATING", actor, false, false);

        List<BatchItemView> items = repository.listItems(batchId);
        for (BatchItemView item : items) {
            if ("SUCCESS".equals(upper(item.itemStatusCode()))) continue;
            repository.clearErrors(item.openingBatchItemId(), "VALIDATION");
            Map<String, String> errors = validateItem(item);
            if (errors.isEmpty()) {
                repository.updateItemStatus(item.openingBatchItemId(), "VALID", actor);
            } else {
                repository.updateItemStatus(item.openingBatchItemId(), "INVALID", actor);
                String message = String.join("؛ ", errors.values());
                String field = errors.size() == 1 ? errors.keySet().iterator().next() : null;
                repository.insertError(item.openingBatchItemId(), "VALIDATION", "ROW_INVALID", field, message, true, actor);
            }
        }

        Counts counts = counts(repository.listItems(batchId));
        String finalStatus = counts.valid > 0 ? "READY" : "FAILED";
        repository.updateCounters(batchId, counts.total, counts.success, counts.failed, actor);
        repository.updateBatchStatus(batchId, finalStatus, actor, false, "FAILED".equals(finalStatus));
        return view(batchId, false);
    }

    public BatchView process(long batchId, BatchProcessRequest raw, String actor, String correlationId) {
        BatchProcessRequest request = normalize(raw);
        validateProcessRequest(request);
        runtimeValidator.validateBatchProcessContract(request.openingChannelCode());

        requiresNew.executeWithoutResult(status -> {
            BatchHeaderView batch = repository.lockBatch(batchId)
                    .orElseThrow(() -> validation("Batch یافت نشد.", "DEPOSIT_OPENING_BATCH.OPENING_BATCH_ID", String.valueOf(batchId)));
            if (!Set.of("READY", "PARTIAL", "COMPLETED").contains(upper(batch.batchStatusCode()))) {
                throw validation("Batch برای پردازش آماده نیست.", "DEPOSIT_OPENING_BATCH.BATCH_STATUS_CODE", batch.batchStatusCode());
            }
            repository.updateBatchStatus(batchId, "PROCESSING", actor, true, false);
        });

        List<BatchItemView> items = repository.listItems(batchId);
        for (BatchItemView item : items) {
            if ("SUCCESS".equals(upper(item.itemStatusCode()))) continue;
            if (!"VALID".equals(upper(item.itemStatusCode()))) continue;
            try {
                requiresNew.executeWithoutResult(status -> processItem(batchId, item.openingBatchItemId(), request, actor, correlationId));
            } catch (RuntimeException ex) {
                requiresNew.executeWithoutResult(status -> failProcessingItem(item.openingBatchItemId(), actor, ex));
            }
        }

        requiresNew.executeWithoutResult(status -> finalizeBatch(batchId, actor));
        return view(batchId, false);
    }

    public BatchView activate(long batchId, String actor, String correlationId) {
        BatchHeaderView batch = repository.findBatch(batchId)
                .orElseThrow(() -> validation("Batch یافت نشد.", "DEPOSIT_OPENING_BATCH.OPENING_BATCH_ID", String.valueOf(batchId)));
        if (!Set.of("COMPLETED", "PARTIAL").contains(upper(batch.batchStatusCode()))) {
            throw validation("فعال‌سازی حساب‌های Batch فقط پس از پایان پردازش مجاز است.",
                    "DEPOSIT_OPENING_BATCH.BATCH_STATUS_CODE", batch.batchStatusCode());
        }

        for (BatchItemView item : repository.listItems(batchId)) {
            if (!"SUCCESS".equals(upper(item.itemStatusCode())) || item.openingRequestId() == null || item.accountId() == null) continue;
            try {
                AccountLifecycleResponse account = accountLifecycleService.getAccount(item.openingRequestId());
                if (!"ACTIVE".equals(upper(account.accountStatusCode()))) {
                    accountLifecycleService.activateAccount(item.openingRequestId(), actor, correlationId + ":row:" + item.rowNo());
                }
            } catch (RuntimeException ex) {
                requiresNew.executeWithoutResult(status -> {
                    repository.clearErrors(item.openingBatchItemId(), "POSTING");
                    repository.insertError(item.openingBatchItemId(), "POSTING", "ACTIVATION_FAILED", null,
                            rootMessage(ex), true, actor);
                });
            }
        }
        return view(batchId, false);
    }

    private void processItem(long batchId, long itemId, BatchProcessRequest process, String actor, String correlationId) {
        BatchItemView item = repository.lockItem(itemId)
                .orElseThrow(() -> validation("ردیف Batch یافت نشد.", "DEPOSIT_OPENING_BATCH_ITEM.OPENING_BATCH_ITEM_ID", String.valueOf(itemId)));
        if ("SUCCESS".equals(upper(item.itemStatusCode()))) return;
        if (!"VALID".equals(upper(item.itemStatusCode()))) {
            throw validation("ردیف در وضعیت VALID نیست.", "DEPOSIT_OPENING_BATCH_ITEM.ITEM_STATUS_CODE", item.itemStatusCode());
        }
        runtimeValidator.validateBatchItem(item.partyId(), item.productVersionId(), item.currencyCode(), process.requestedOpeningDate());
        repository.updateItemStatus(itemId, "PROCESSING", actor);
        repository.clearErrors(itemId, "PROCESSING");

        BatchHeaderView batch = repository.findBatch(batchId)
                .orElseThrow(() -> validation("Batch یافت نشد.", "DEPOSIT_OPENING_BATCH.OPENING_BATCH_ID", String.valueOf(batchId)));
        long requestId = aggregateRepository.nextRequestId();
        String requestNo = requestNo(batchId, item.rowNo());
        String idempotency = itemIdempotencyKey(batch.idempotencyKey(), item.externalRowKey(), item.rowNo());
        OpeningRequest opening = new OpeningRequest(
                requestNo, idempotency, item.productVersionId(), "BULK", "INDIVIDUAL", item.currencyCode(),
                process.openingChannelCode(), process.orgUnitCode(), process.requestedOpeningDate(),
                item.openingAmount(), "OTHER", "SAVING", "APPROVED"
        );
        aggregateRepository.insertBatchRequest(requestId, opening, itemId, actor);
        aggregateRepository.insertParty(requestId,
                new OpeningParty(item.partyId(), "OWNER", 1, new BigDecimal("100"), 1), 1, actor);
        auditService.recordOpeningCreated(requestId, opening, actor, correlationId + ":row:" + item.rowNo());

        AccountLifecycleResponse account = accountLifecycleService.createAccount(
                requestId, actor, correlationId + ":row:" + item.rowNo()
        );
        repository.linkItemResult(itemId, requestId, account.accountId(), actor);
    }

    private void failProcessingItem(long itemId, String actor, RuntimeException ex) {
        BatchItemView item = repository.lockItem(itemId).orElse(null);
        if (item == null || "SUCCESS".equals(upper(item.itemStatusCode()))) return;
        repository.updateItemStatus(itemId, "FAILED", actor);
        repository.clearErrors(itemId, "PROCESSING");
        repository.insertError(itemId, "PROCESSING", "PROCESSING_FAILED", null, rootMessage(ex), true, actor);
    }

    private void finalizeBatch(long batchId, String actor) {
        repository.lockBatch(batchId).orElseThrow(() -> validation("Batch یافت نشد.", "DEPOSIT_OPENING_BATCH.OPENING_BATCH_ID", String.valueOf(batchId)));
        Counts counts = counts(repository.listItems(batchId));
        String status = counts.failed > 0 ? (counts.success > 0 ? "PARTIAL" : "FAILED") : "COMPLETED";
        repository.updateCounters(batchId, counts.total, counts.success, counts.failed, actor);
        repository.updateBatchStatus(batchId, status, actor, true, true);
    }

    private Map<String, String> validateItem(BatchItemView item) {
        Map<String, String> errors = new LinkedHashMap<>();
        try {
            runtimeValidator.validateBatchItem(item.partyId(), item.productVersionId(), item.currencyCode(), LocalDate.now());
        } catch (DepositOpeningValidationException ex) {
            errors.putAll(ex.fieldErrors());
        }
        if (item.openingAmount() == null || item.openingAmount().signum() <= 0) errors.put("OPENING_AMOUNT", "مبلغ افتتاح باید بیشتر از صفر باشد.");
        if (blank(item.currencyCode()) || item.currencyCode().trim().length() != 3) errors.put("CURRENCY_CODE", "کد ارز سه‌حرفی الزامی است.");
        return errors;
    }

    private BatchView view(long batchId, boolean replay) {
        BatchHeaderView header = repository.findBatch(batchId)
                .orElseThrow(() -> validation("Batch یافت نشد.", "DEPOSIT_OPENING_BATCH.OPENING_BATCH_ID", String.valueOf(batchId)));
        return new BatchView(header, List.copyOf(repository.listItems(batchId)), List.copyOf(repository.listErrors(batchId)), replay);
    }

    private static BatchCreateRequest normalize(BatchCreateRequest value) {
        if (value == null) return null;
        List<BatchItemCreateRequest> items = new ArrayList<>();
        int seq = 1;
        for (BatchItemCreateRequest item : value.items() == null ? List.<BatchItemCreateRequest>of() : value.items()) {
            if (item == null) continue;
            Integer rowNo = item.rowNo() == null ? seq : item.rowNo();
            String external = blank(item.externalRowKey()) ? "ROW-" + rowNo : item.externalRowKey().trim();
            items.add(new BatchItemCreateRequest(rowNo, external, item.partyId(), item.productVersionId(), upper(item.currencyCode()), item.openingAmount()));
            seq++;
        }
        return new BatchCreateRequest(trim(value.batchNo()), trim(value.idempotencyKey()), upper(value.sourceTypeCode()),
                trim(value.sourceReference()), List.copyOf(items));
    }

    private static BatchProcessRequest normalize(BatchProcessRequest value) {
        if (value == null) return null;
        return new BatchProcessRequest(upper(value.openingChannelCode()), trim(value.orgUnitCode()), value.requestedOpeningDate());
    }

    private static void validateCreate(BatchCreateRequest request) {
        if (request == null) throw validation("مشخصات Batch ارسال نشده است.", "DEPOSIT_OPENING_BATCH", "الزامی");
        Map<String, String> errors = new LinkedHashMap<>();
        if (blank(request.idempotencyKey())) errors.put("DEPOSIT_OPENING_BATCH.IDEMPOTENCY_KEY", "کلید Idempotency الزامی است.");
        if (blank(request.sourceTypeCode())) errors.put("DEPOSIT_OPENING_BATCH.SOURCE_TYPE_CODE", "نوع منبع الزامی است.");
        if (request.items() == null || request.items().isEmpty()) errors.put("DEPOSIT_OPENING_BATCH_ITEM", "حداقل یک ردیف Batch الزامی است.");
        if (request.idempotencyKey() != null && request.idempotencyKey().length() > 80) errors.put("DEPOSIT_OPENING_BATCH.IDEMPOTENCY_KEY", "حداکثر طول ۸۰ کاراکتر است.");
        if (request.items() != null) {
            Set<String> externalKeys = new java.util.HashSet<>();
            Set<Integer> rowNumbers = new java.util.HashSet<>();
            for (BatchItemCreateRequest item : request.items()) {
                if (!externalKeys.add(item.externalRowKey())) errors.put("DEPOSIT_OPENING_BATCH_ITEM.EXTERNAL_ROW_KEY", "External Row Key داخل Batch باید یکتا باشد.");
                if (!rowNumbers.add(item.rowNo())) errors.put("DEPOSIT_OPENING_BATCH_ITEM.ROW_NO", "شماره ردیف داخل Batch باید یکتا باشد.");
            }
        }
        if (!errors.isEmpty()) throw new DepositOpeningValidationException("اطلاعات Batch ناقص یا نامعتبر است.", errors);
    }

    private static void validateProcessRequest(BatchProcessRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request == null || blank(request.openingChannelCode())) errors.put("OPENING_CHANNEL_CODE", "کانال افتتاح الزامی است.");
        if (request == null || blank(request.orgUnitCode())) errors.put("ORG_UNIT_CODE", "واحد سازمانی الزامی است.");
        if (request == null || request.requestedOpeningDate() == null) errors.put("REQUESTED_OPENING_DATE", "تاریخ افتتاح الزامی است.");
        if (!errors.isEmpty()) throw new DepositOpeningValidationException("پارامترهای پردازش Batch ناقص است.", errors);
    }

    private static Counts counts(List<BatchItemView> items) {
        int success = 0, failed = 0, valid = 0;
        for (BatchItemView item : items) {
            switch (upper(item.itemStatusCode())) {
                case "SUCCESS" -> { success++; valid++; }
                case "VALID" -> valid++;
                case "INVALID", "FAILED" -> failed++;
                default -> { }
            }
        }
        return new Counts(items.size(), valid, success, failed);
    }

    private static String requestNo(long batchId, int rowNo) {
        return "DOP-B-" + batchId + "-" + rowNo;
    }

    private static String itemIdempotencyKey(String batchKey, String externalRowKey, int rowNo) {
        String raw = batchKey + ":" + (blank(externalRowKey) ? "ROW-" + rowNo : externalRowKey);
        if (raw.length() <= 80) return raw;
        return raw.substring(0, 47) + ":" + sha256(raw).substring(0, 32);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static String rootMessage(Throwable value) {
        Throwable current = value;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return blank(message) ? current.getClass().getSimpleName() : message;
    }

    private static DepositOpeningValidationException validation(String message, String field, String detail) {
        return new DepositOpeningValidationException(message, Map.of(field, detail == null ? "نامعتبر" : detail));
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String upper(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private record Counts(int total, int valid, int success, int failed) {}
}
