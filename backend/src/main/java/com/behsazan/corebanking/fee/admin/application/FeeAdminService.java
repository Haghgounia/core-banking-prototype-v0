package com.behsazan.corebanking.fee.admin.application;

import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.CatalogResponse;
import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.GroupCatalogItem;
import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.SelectOption;
import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.TableCatalogItem;
import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.TableDescriptor;
import com.behsazan.corebanking.fee.admin.domain.FeeAdminModels.TablePage;
import com.behsazan.corebanking.fee.admin.oracle.FeeAdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeeAdminService {
    private final FeeAdminRepository repository;

    public FeeAdminService(FeeAdminRepository repository) { this.repository = repository; }

    public CatalogResponse catalog() {
        Map<String, List<TableCatalogItem>> grouped = new LinkedHashMap<>();
        Map<String, String> groupTitles = new LinkedHashMap<>();
        long totalRows = 0;
        int baselineRows = 0;
        int availableTables = 0;
        for (FeeAdminCatalog.Entry entry : FeeAdminCatalog.entries()) {
            boolean available = repository.tableExists(entry.tableName());
            long count = available ? repository.count(entry.tableName()) : 0;
            if (available) availableTables++;
            totalRows += count;
            baselineRows += entry.baselineRows();
            grouped.computeIfAbsent(entry.groupCode(), k -> new ArrayList<>()).add(new TableCatalogItem(
                    entry.tableName(), entry.title(), entry.groupCode(), entry.groupTitle(), entry.baselineRows(), count, available, entry.editable()));
            groupTitles.put(entry.groupCode(), entry.groupTitle());
        }
        List<GroupCatalogItem> groups = grouped.entrySet().stream().map(group -> {
            long rows = group.getValue().stream().mapToLong(TableCatalogItem::rowCount).sum();
            int baseline = group.getValue().stream().mapToInt(TableCatalogItem::baselineRows).sum();
            int available = (int) group.getValue().stream().filter(TableCatalogItem::available).count();
            return new GroupCatalogItem(group.getKey(), groupTitles.get(group.getKey()), rows, baseline, available, List.copyOf(group.getValue()));
        }).toList();
        return new CatalogResponse(repository.schemaName(), FeeAdminCatalog.entries().size(), availableTables, totalRows, baselineRows, groups);
    }

    public TableDescriptor descriptor(String table) { return repository.descriptor(table); }

    public TablePage search(String table, String text, int page, int size, String filterColumn, String filterValue) {
        return repository.search(table, text, page, size, filterColumn, filterValue);
    }

    public Map<String, Object> findById(String table, long id) {
        return repository.findById(table, id).orElseThrow(() -> new FeeAdminValidationException("رکورد یافت نشد: " + table + "/" + id));
    }

    public List<SelectOption> lookup(String table, String column, String text, int limit, Long versionId) {
        return repository.lookup(table, column, text, limit, versionId);
    }

    @Transactional
    public Map<String, Object> create(String table, Map<String, Object> values, String actor) {
        requireEditable(table);
        if (isStage(table)) validateStage(values, null);
        long id = repository.insert(table, values, actorName(actor));
        return findById(table, id);
    }

    @Transactional
    public Map<String, Object> update(String table, long id, Map<String, Object> values, String actor) {
        requireEditable(table);
        if (isStage(table)) {
            Map<String, Object> persisted = repository.lockChargeStage(id)
                    .orElseThrow(() -> new FeeAdminValidationException("مرحله اخذ یافت نشد: " + id));
            validateStage(values, persisted);
        } else if (isStageParent(table)) {
            validateParentReassignment(table, id, values);
        }
        if (!repository.update(table, id, values, actorName(actor))) throw new FeeAdminValidationException("رکورد یافت نشد: " + table + "/" + id);
        return findById(table, id);
    }

    @Transactional
    public void delete(String table, long id) {
        requireEditable(table);
        if (!repository.delete(table, id)) throw new FeeAdminValidationException("رکورد یافت نشد: " + table + "/" + id);
    }

    private void validateStage(Map<String, Object> values, Map<String, Object> persisted) {
        var links = FeeChargeStageIntegrityPolicy.links(values, persisted);
        // Lock both referenced records through the shared transaction to prevent
        // a concurrent version reassignment between validation and stage persistence.
        long ruleVersion = repository.lockStageParentVersion("FEE_CALCULATION_RULE", links.ruleId())
                .orElseThrow(() -> new FeeAdminValidationException("قاعده محاسبه انتخابی وجود ندارد."));
        Long componentVersion = links.componentId() == null ? null
                : repository.lockStageParentVersion("FEE_CHARGE_COMPONENT", links.componentId())
                  .orElseThrow(() -> new FeeAdminValidationException("مؤلفه کارمزد انتخابی وجود ندارد."));
        FeeChargeStageIntegrityPolicy.verifyOwners(links, ruleVersion, componentVersion);
    }

    private void validateParentReassignment(String table, long id, Map<String, Object> values) {
        if (!values.containsKey("FEE_DEFINITION_VERSION_ID")) return;
        long currentVersion = repository.lockStageParentVersion(table, id)
                .orElseThrow(() -> new FeeAdminValidationException("رکورد مرتبط یافت نشد: " + table + "/" + id));
        boolean referenced = repository.stageReferencesParent(table, id);
        FeeChargeStageIntegrityPolicy.verifyParentMove(currentVersion,
                values.get("FEE_DEFINITION_VERSION_ID"), referenced,
                "FEE_CALCULATION_RULE".equalsIgnoreCase(table) ? "قاعده محاسبه" : "مؤلفه کارمزد");
    }

    private static boolean isStage(String table) { return "FEE_CHARGE_STAGE".equalsIgnoreCase(table); }
    private static boolean isStageParent(String table) {
        return "FEE_CALCULATION_RULE".equalsIgnoreCase(table)
                || "FEE_CHARGE_COMPONENT".equalsIgnoreCase(table);
    }

    private static void requireEditable(String table) {
        if (!FeeAdminCatalog.require(table).editable()) {
            throw new FeeAdminValidationException("جدول‌های Runtime کارمزد در فرم عمومی فقط خواندنی هستند: " + table);
        }
    }

    private static String actorName(String actor) { return actor == null || actor.isBlank() ? "prototype-ui" : actor.trim(); }
}
