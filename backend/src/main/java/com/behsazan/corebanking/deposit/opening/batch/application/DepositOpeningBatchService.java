package com.behsazan.corebanking.deposit.opening.batch.application;

import com.behsazan.corebanking.deposit.opening.batch.domain.DepositOpeningBatchModels.*;
import com.behsazan.corebanking.deposit.opening.batch.oracle.DepositOpeningBatchRepository;
import com.behsazan.corebanking.deposit.opening.error.DepositOpeningValidationException;
import com.behsazan.corebanking.deposit.opening.readiness.application.DepositOpeningRuntimeValidator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


@Service
public class DepositOpeningBatchService {
    private static final Set<String> CREATEABLE_STATUSES = Set.of("DRAFT", "FAILED", "READY", "PARTIAL");
    
    private final DepositOpeningBatchRepository repository;
    private final DepositOpeningRuntimeValidator runtimeValidator;

    public DepositOpeningBatchService(
            DepositOpeningBatchRepository repository,
            DepositOpeningRuntimeValidator runtimeValidator
    ) {
        this.repository = repository;
        this.runtimeValidator = runtimeValidator;
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
                    request.sourceReference(), request.bulkOpeningBasisCode(), request.legalBasisReference(),
                    request.cddApprovalReference(), request.items().size(), actor);
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
    public BatchView refresh(long batchId, String actor) {
        BatchHeaderView batch = repository.lockBatch(batchId)
                .orElseThrow(() -> validation("Batch یافت نشد.", "DEPOSIT_OPENING_BATCH.OPENING_BATCH_ID", String.valueOf(batchId)));
        List<BatchItemView> before = repository.listItems(batchId);
        for (BatchItemView item : before) {
            String target = item.itemStatusCode();
            if (item.openingRequestId() != null) {
                target = "ACTIVE".equals(upper(item.accountStatusCode())) || "CLOSED".equals(upper(item.accountStatusCode()))
                        ? "SUCCESS" : "PROCESSING";
            }
            if (!upper(target).equals(upper(item.itemStatusCode()))) {
                repository.syncItemAccountLink(item.openingBatchItemId(), item.accountId(), target, actor);
            } else if (item.accountId() != null && "SUCCESS".equals(upper(target))) {
                repository.syncItemAccountLink(item.openingBatchItemId(), item.accountId(), target, actor);
            }
        }
        List<BatchItemView> items = repository.listItems(batchId);
        Counts counts = counts(items);
        String finalStatus = deriveBatchStatus(items);
        repository.updateCounters(batchId, counts.total, counts.success, counts.failed, actor);
        repository.updateBatchStatus(batchId, finalStatus, actor,
                items.stream().anyMatch(i -> i.openingRequestId() != null),
                "COMPLETED".equals(finalStatus) || "FAILED".equals(finalStatus));
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
            if ("SUCCESS".equals(upper(item.itemStatusCode())) || "PROCESSING".equals(upper(item.itemStatusCode()))) continue;
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
        return refresh(batchId, actor);
    }

    public BatchView process(long batchId, BatchProcessRequest raw, String actor, String correlationId) {
        BatchProcessRequest request = normalize(raw);
        validateProcessRequest(request);
        runtimeValidator.validateBatchProcessContract(request.openingChannelCode());
        throw validation(
                "پردازش مستقیم Batch در Operational v5 تا ثبت CDD/Risk/Funding Plan مستقل هر ردیف غیرفعال است.",
                "DEPOSIT_OPENING_BATCH",
                "هر Batch Item باید ابتدا Opening مستقل و Create Gate فردی را کامل کند؛ ایجاد Account مستقیم از Header مجاز نیست."
        );
    }

    public BatchView activate(long batchId, String actor, String correlationId) {
        repository.findBatch(batchId)
                .orElseThrow(() -> validation("Batch یافت نشد.", "DEPOSIT_OPENING_BATCH.OPENING_BATCH_ID", String.valueOf(batchId)));
        throw validation(
                "فعال‌سازی مستقیم گروهی در Operational v5 مجاز نیست.",
                "DEPOSIT_OPENING_BATCH",
                "هر Opening تولیدشده باید Settlement و Activation Readiness Gate مستقل را طی کند."
        );
    }

    private Map<String, String> validateItem(BatchItemView item) {
        Map<String, String> errors = new LinkedHashMap<>();
        try {
            var runtime = runtimeValidator.validateBatchItem(item.partyId(), item.productVersionId(), item.currencyCode(), LocalDate.now());
            if (!"QARD_SAVINGS".equals(upper(runtime.productFamilyCode()))) {
                errors.put("PRODUCT_VERSION_ID", "Operational v5 Batch فقط برای QARD_SAVINGS مجاز است.");
            }
        } catch (DepositOpeningValidationException ex) {
            errors.putAll(ex.fieldErrors());
        }
        if (item.openingAmount() == null || item.openingAmount().signum() <= 0) errors.put("OPENING_AMOUNT", "مبلغ افتتاح باید بیشتر از صفر باشد.");
        if (blank(item.currencyCode()) || item.currencyCode().trim().length() != 3) errors.put("CURRENCY_CODE", "کد ارز سه‌حرفی الزامی است.");
        return errors;
    }

    static String deriveBatchStatus(List<BatchItemView> items) {
        if (items == null || items.isEmpty()) return "FAILED";
        long success = items.stream().filter(i -> "SUCCESS".equals(upper(i.itemStatusCode()))).count();
        long failed = items.stream().filter(i -> "FAILED".equals(upper(i.itemStatusCode())) || "INVALID".equals(upper(i.itemStatusCode()))).count();
        long processing = items.stream().filter(i -> "PROCESSING".equals(upper(i.itemStatusCode()))).count();
        long valid = items.stream().filter(i -> "VALID".equals(upper(i.itemStatusCode()))).count();
        if (success == items.size()) return "COMPLETED";
        if (processing > 0) return success > 0 || failed > 0 ? "PARTIAL" : "PROCESSING";
        if (valid > 0) return "READY";
        if (success > 0 && failed > 0) return "PARTIAL";
        if (success > 0) return "PARTIAL";
        return "FAILED";
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
                trim(value.sourceReference()), upper(value.bulkOpeningBasisCode()), trim(value.legalBasisReference()),
                trim(value.cddApprovalReference()), List.copyOf(items));
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
        if (!"GOV_EMPLOYEE_SAVINGS_1376".equals(upper(request.bulkOpeningBasisCode()))) errors.put("DEPOSIT_OPENING_BATCH.BULK_OPENING_BASIS_CODE", "در Prototype v5 فقط GOV_EMPLOYEE_SAVINGS_1376 مجاز است.");
        if (blank(request.legalBasisReference())) errors.put("DEPOSIT_OPENING_BATCH.LEGAL_BASIS_REFERENCE", "مرجع قانون/بخشنامه Batch الزامی است.");
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
                case "VALID", "PROCESSING" -> valid++;
                case "INVALID", "FAILED" -> failed++;
                default -> { }
            }
        }
        return new Counts(items.size(), valid, success, failed);
    }

    private static DepositOpeningValidationException validation(String message, String field, String detail) {
        return new DepositOpeningValidationException(message, Map.of(field, detail == null ? "نامعتبر" : detail));
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String upper(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private record Counts(int total, int valid, int success, int failed) {}
}
