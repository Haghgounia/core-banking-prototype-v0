package com.behsazan.corebanking.deposit.opening.readiness.application;

import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.ReadinessCheck;
import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.ReadinessReport;
import com.behsazan.corebanking.deposit.opening.readiness.domain.DepositOpeningRuntimeModels.RollbackProbeResult;
import com.behsazan.corebanking.deposit.opening.readiness.oracle.DepositOpeningRuntimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class DepositOpeningReadinessService {
    private final DepositOpeningRuntimeRepository repository;
    private final TransactionTemplate requiresNew;

    public DepositOpeningReadinessService(DepositOpeningRuntimeRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    }

    public ReadinessReport readiness() {
        List<ReadinessCheck> checks = new ArrayList<>();
        check(checks, "DPS2_ROOT", "جداول اصلی DPS2",
                repository.tableExists(repository.schema(), "DEPOSIT_OPENING_REQUEST")
                        && repository.tableExists(repository.schema(), "DEPOSIT_ACCOUNT")
                        && repository.tableExists(repository.schema(), "DEPOSIT_OPENING_BATCH"),
                "DEPOSIT_OPENING_REQUEST / DEPOSIT_ACCOUNT / DEPOSIT_OPENING_BATCH");
        check(checks, "CIF_PARTY", "قرارداد Party",
                repository.tableExists(repository.cifSchema(), "PARTY"), repository.cifSchema() + ".PARTY");
        check(checks, "PDL_PRODUCT", "قرارداد Product Builder",
                repository.tableExists(repository.productSchema(), "PRODUCT")
                        && repository.tableExists(repository.productSchema(), "PRODUCT_VERSION"),
                repository.productSchema() + ".PRODUCT / PRODUCT_VERSION");
        check(checks, "SEQUENCES", "Sequenceهای Opening/Batch",
                repository.sequenceExists(repository.schema(), "SEQ_DEPOSIT_OPENING_REQUEST")
                        && repository.sequenceExists(repository.schema(), "SEQ_DEPOSIT_OPENING_BATCH")
                        && repository.sequenceExists(repository.schema(), "SEQ_DEPOSIT_OPENING_BATCH_ITEM"),
                "Sequence allocation contract");
        check(checks, "APPEND_ONLY", "Triggerهای Append-only ممیزی",
                repository.triggerEnabled(repository.schema(), "TRG_DEP_OPEN_AUD_EVT_APPEND_ONLY")
                        && repository.triggerEnabled(repository.schema(), "TRG_DEP_OPEN_AUD_FLD_APPEND_ONLY")
                        && repository.triggerEnabled(repository.schema(), "TRG_DEP_OPEN_SNAPSHOT_APPEND_ONLY")
                        && repository.triggerEnabled(repository.schema(), "TRG_DEP_OPEN_STATUS_APPEND_ONLY"),
                "Audit / field / snapshot / status history guards");
        check(checks, "IDEMPOTENCY", "Unique Guardهای Idempotency",
                repository.uniqueIndexExists(repository.schema(), "UX_DEP_OPEN_REQ_IDEMPOTENCY")
                        && repository.uniqueIndexExists(repository.schema(), "UX_DEP_OPEN_BATCH_IDEMPOTENCY")
                        && repository.uniqueIndexExists(repository.schema(), "UX_DEP_OPEN_BATCH_EXT_ROW_KEY"),
                "Phase 7 DB-level concurrency guards");
        check(checks, "REFERENCE", "Referenceهای Runtime پایه",
                repository.referenceCodeExists("REF_DEP_OPEN_REQUEST_TYPE", "REQUEST_TYPE_CODE", "BULK")
                        && repository.referenceCodeExists("REF_DEP_OPEN_OWNERSHIP_TYPE", "OWNERSHIP_TYPE_CODE", "INDIVIDUAL")
                        && repository.referenceCodeExists("REF_DEP_OPEN_CHANNEL", "CHANNEL_CODE", "BRANCH")
                        && repository.referenceCodeExists("REF_DEP_OPEN_REQUEST_STATUS", "REQUEST_STATUS_CODE", "APPROVED"),
                "BULK / INDIVIDUAL / BRANCH / APPROVED");
        checks.add(new ReadinessCheck("ACCOUNT_SERVICE", "Account Service خارجی", "WARN",
                "طبق HTML، انتخاب/کنترل حساب مبدأ قرارداد خارجی است و در Opening شبیه‌سازی Integration واقعی نمی‌شود."));
        checks.add(new ReadinessCheck("TAX_PROFILE_SERVICE", "Tax Profile / Withholding خارجی", "WARN",
                "طبق HTML، وضعیت مالیاتی از سرویس خارجی دریافت می‌شود و تا اتصال واقعی WARN باقی می‌ماند."));
        boolean failed = checks.stream().anyMatch(c -> "FAIL".equals(c.status()));
        return new ReadinessReport(failed ? "NOT_READY" : "READY_WITH_WARNINGS", OffsetDateTime.now(), List.copyOf(checks));
    }

    public RollbackProbeResult rollbackProbe() {
        String key = "PHASE7-PROBE-" + UUID.randomUUID();
        final boolean[] visible = {false};
        Long batchId = requiresNew.execute(status -> {
            long id = repository.nextBatchId();
            repository.insertProbeBatch(id, key);
            visible[0] = repository.countBatchByIdempotency(key) == 1;
            status.setRollbackOnly();
            return id;
        });
        long resolvedId = batchId == null ? -1L : batchId;
        boolean remained = repository.countBatchByIdempotency(key) > 0;
        boolean success = visible[0] && !remained;
        return new RollbackProbeResult(success, resolvedId, key, visible[0], remained,
                success ? "رکورد Probe داخل Transaction دیده شد و پس از rollback باقی نماند."
                        : "Rollback Probe ناموفق بود؛ وضعیت Transaction/Oracle را بررسی کنید.");
    }

    private static void check(List<ReadinessCheck> checks, String code, String title, boolean ok, String detail) {
        checks.add(new ReadinessCheck(code, title, ok ? "PASS" : "FAIL", detail));
    }
}
