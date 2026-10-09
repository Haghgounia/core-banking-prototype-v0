package com.behsazan.corebanking.fee2.oracle;

import com.behsazan.corebanking.fee2.application.Fee2Catalog;
import com.behsazan.corebanking.fee2.application.Fee2ValidationException;
import com.behsazan.corebanking.fee2.domain.Fee2Models.CalculationConfigRequest;
import com.behsazan.corebanking.fee2.domain.Fee2Models.ColumnDescriptor;
import com.behsazan.corebanking.fee2.domain.Fee2Models.SelectOption;
import com.behsazan.corebanking.fee2.domain.Fee2Models.TableDescriptor;
import com.behsazan.corebanking.fee2.domain.Fee2Models.TablePage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class Fee2Repository {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_$#]*");
    private static final Pattern IN_EXPRESSION = Pattern.compile("(?is)([A-Z][A-Z0-9_$#]*)\\s+IN\\s*\\(([^)]*)\\)");
    private static final Set<String> AUDIT_MANAGED = Set.of(
            "CREATED_AT", "CREATED_BY", "UPDATED_AT", "UPDATED_BY", "ROW_VERSION"
    );
    private static final Set<String> VERSION_SYSTEM = Set.of(
            "VERSION_NO", "STATUS", "ACTIVATED_AT", "APPROVAL_REFERENCE", "CONTENT_HASH",
            "CALC_CONFIG_SCHEMA_VERSION", "CALC_CONFIG_JSON"
    );
    private static final Set<String> SIM_RUN_SYSTEM = Set.of(
            "STATUS", "REQUESTED_BY", "REQUESTED_AT", "STARTED_AT", "FINISHED_AT",
            "TOTAL_CASES", "PASS_COUNT", "FAIL_COUNT", "ERROR_COUNT", "RUN_HASH", "SUMMARY_JSON"
    );
    private static final Set<String> TECHNICAL_READ_ONLY = Set.of(
            "CONTENT_HASH", "CHECKSUM", "INPUT_FINGERPRINT", "DECISION_FINGERPRINT", "REQUEST_HASH",
            "OWNER_TOKEN", "RUN_HASH", "SOURCE_IP"
    );

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("ID", "شناسه"), Map.entry("SCOPE_ID", "دامنه کارمزد"), Map.entry("SCOPE_CODE", "کد دامنه"),
            Map.entry("FEE_ID", "تعریف کارمزد"), Map.entry("FEE_VERSION_ID", "نسخه کارمزد"), Map.entry("BASE_FEE_VERSION_ID", "نسخه مبنای مقایسه"),
            Map.entry("FEE_CODE", "کد کارمزد"), Map.entry("FEE_TYPE_CODE", "نوع کارمزد"), Map.entry("CATEGORY_CODE", "دسته‌بندی"),
            Map.entry("NAME", "نام"), Map.entry("NAME_FA", "نام فارسی"), Map.entry("NAME_EN", "نام انگلیسی"),
            Map.entry("TITLE_FA", "عنوان فارسی"), Map.entry("DESCRIPTION_FA", "توضیحات فارسی"), Map.entry("OWNER_UNIT", "واحد مالک"),
            Map.entry("STATUS", "وضعیت"), Map.entry("TIME_ZONE", "منطقه زمانی"), Map.entry("ROW_VERSION", "نسخه رکورد"),
            Map.entry("VERSION_NO", "شماره نسخه"), Map.entry("BINDING_LEVEL", "سطح اعمال"), Map.entry("PRIORITY", "اولویت"),
            Map.entry("CALCULATION_TYPE", "روش محاسبه"), Map.entry("CURRENCY", "ارز"), Map.entry("BASIS_CODE", "مبنای محاسبه"),
            Map.entry("BASIS_UNIT", "واحد مبنا"), Map.entry("FIXED_AMOUNT", "مبلغ ثابت"), Map.entry("RATE_VALUE", "نرخ"),
            Map.entry("MIN_AMOUNT", "حداقل مبلغ"), Map.entry("MAX_AMOUNT", "حداکثر مبلغ"), Map.entry("ROUNDING_MODE", "روش گرد کردن"),
            Map.entry("ROUNDING_QUANTUM", "واحد گرد کردن"), Map.entry("PERIOD_POLICY", "سیاست دوره"), Map.entry("DAY_BASIS", "مبنای روز"),
            Map.entry("CALC_CONFIG_SCHEMA_VERSION", "نسخه ساختار تنظیمات محاسبه"), Map.entry("CALC_CONFIG_JSON", "تنظیمات تکمیلی محاسبه"),
            Map.entry("EFFECTIVE_FROM", "معتبر از"), Map.entry("EFFECTIVE_TO", "معتبر تا"), Map.entry("ACTIVATED_AT", "زمان فعال‌سازی"),
            Map.entry("APPROVAL_REFERENCE", "مرجع تأیید"), Map.entry("CHANGE_REASON", "علت تغییر"), Map.entry("CONTENT_HASH", "اثر انگشت محتوا"),
            Map.entry("SERVICE_CODE", "کد خدمت"), Map.entry("EVENT_TYPE", "نوع رویداد"), Map.entry("OPERATION_CODE", "کد عملیات"),
            Map.entry("SEQUENCE_NO", "ترتیب"), Map.entry("ENABLED_FLAG", "فعال"), Map.entry("CONDITION_GROUP_NO", "گروه شرط"),
            Map.entry("DIMENSION_CODE", "بُعد شرط"), Map.entry("OPERATOR_CODE", "عملگر"), Map.entry("VALUE_TEXT", "مقدار متنی"),
            Map.entry("VALUE_FROM_NUM", "مقدار عددی از"), Map.entry("VALUE_TO_NUM", "مقدار عددی تا"), Map.entry("VALUE_JSON", "مقدار ساختاریافته"),
            Map.entry("CASE_SENSITIVE_FLAG", "حساس به بزرگی/کوچکی حروف"), Map.entry("MODIFIER_TYPE", "نوع تعدیل"),
            Map.entry("APPLY_PHASE", "مرحله اعمال"), Map.entry("VALUE_TYPE", "نوع مقدار"), Map.entry("AMOUNT_VALUE", "مبلغ تعدیل"),
            Map.entry("REFERENCE_CODE", "کد مرجع"), Map.entry("REGULATION_ID", "مقرره / بخشنامه"), Map.entry("CONDITION_JSON", "شرط تکمیلی"),
            Map.entry("CONFIG_JSON", "تنظیمات تکمیلی"), Map.entry("TAX_CODE", "کد مالیات"), Map.entry("TAX_TYPE", "نوع مالیات"),
            Map.entry("BASIS_TYPE", "مبنای مالیات"), Map.entry("SHARE_TYPE", "نوع سهم"), Map.entry("BENEFICIARY_REF", "مرجع ذی‌نفع"),
            Map.entry("WEIGHT_VALUE", "وزن سهم"), Map.entry("REMAINDER_PRIORITY", "اولویت باقیمانده"), Map.entry("REGULATION_CODE", "کد مقرره"),
            Map.entry("ISSUER", "مرجع صادرکننده"), Map.entry("REFERENCE_NO", "شماره مرجع"), Map.entry("ISSUED_DATE", "تاریخ صدور"),
            Map.entry("DOMAIN_CODE", "دامنه اطلاعات مرجع"), Map.entry("ITEM_CODE", "کد مقدار"), Map.entry("VALID_FROM", "معتبر از"),
            Map.entry("VALID_TO", "معتبر تا"), Map.entry("ACTION_CODE", "عملیات"), Map.entry("DECISION_STATUS", "وضعیت تصمیم"),
            Map.entry("ROLE_CODE", "نقش تصمیم‌گیر"), Map.entry("PRINCIPAL_ID", "شناسه کاربر / عامل"), Map.entry("COMMENT_TEXT", "توضیح تصمیم"),
            Map.entry("DECIDED_AT", "زمان تصمیم"), Map.entry("REVISION_NO", "شماره Revision"), Map.entry("ACTIVE_VERSION_COUNT", "تعداد نسخه فعال"),
            Map.entry("CHECKSUM", "Checksum پیکربندی"), Map.entry("RUN_ID", "اجرای شبیه‌سازی"), Map.entry("CASE_ID", "سناریوی شبیه‌سازی"),
            Map.entry("CASE_NO", "شماره سناریو"), Map.entry("CASE_NAME", "نام سناریو"), Map.entry("INPUT_JSON", "ورودی سناریو"),
            Map.entry("EXPECTED_JSON", "نتیجه مورد انتظار"), Map.entry("TAGS_JSON", "برچسب‌ها"), Map.entry("RESULT_STATUS", "وضعیت نتیجه"),
            Map.entry("ACTUAL_JSON", "نتیجه واقعی"), Map.entry("DELTA_JSON", "اختلاف نتیجه"), Map.entry("ERROR_CODE", "کد خطا"),
            Map.entry("ERROR_DETAIL", "جزئیات خطا"), Map.entry("DURATION_MICROS", "مدت اجرا (میکروثانیه)"), Map.entry("REQUESTED_BY", "درخواست‌کننده"),
            Map.entry("REQUESTED_AT", "زمان درخواست"), Map.entry("STARTED_AT", "زمان شروع"), Map.entry("FINISHED_AT", "زمان پایان"),
            Map.entry("TOTAL_CASES", "تعداد کل سناریوها"), Map.entry("PASS_COUNT", "تعداد موفق"), Map.entry("FAIL_COUNT", "تعداد ناموفق"),
            Map.entry("ERROR_COUNT", "تعداد خطادار"), Map.entry("RUN_HASH", "اثر انگشت اجرا"), Map.entry("SUMMARY_JSON", "خلاصه اجرا"),
            Map.entry("CALC_LOG_ID", "محاسبه"), Map.entry("PARENT_ITEM_ID", "قلم والد"), Map.entry("ITEM_TYPE", "نوع قلم"),
            Map.entry("BASE_AMOUNT", "مبلغ مبنا"), Map.entry("AMOUNT", "مبلغ"), Map.entry("DETAIL_JSON", "جزئیات قلم"),
            Map.entry("CORRELATION_ID", "شناسه همبستگی"), Map.entry("IDEMPOTENCY_KEY", "کلید عدم تکرار"), Map.entry("OCCURRED_AT", "زمان رخداد"),
            Map.entry("INPUT_FINGERPRINT", "اثر انگشت ورودی"), Map.entry("DECISION_FINGERPRINT", "اثر انگشت تصمیم"), Map.entry("REQUEST_JSON", "درخواست محاسبه"),
            Map.entry("FEE_AMOUNT", "مبلغ کارمزد"), Map.entry("TAX_AMOUNT", "مبلغ مالیات"), Map.entry("TOTAL_AMOUNT", "مبلغ کل"),
            Map.entry("ENTITY_TYPE", "نوع موجودیت"), Map.entry("ENTITY_ID", "شناسه موجودیت"), Map.entry("BEFORE_JSON", "وضعیت قبل"),
            Map.entry("AFTER_JSON", "وضعیت بعد"), Map.entry("SOURCE_IP", "نشانی مبدأ"), Map.entry("REQUEST_HASH", "اثر انگشت درخواست"),
            Map.entry("RESPONSE_JSON", "پاسخ ذخیره‌شده"), Map.entry("HTTP_STATUS", "کد HTTP"), Map.entry("EXPIRES_AT", "زمان انقضا"),
            Map.entry("STATE", "وضعیت پردازش"), Map.entry("OWNER_TOKEN", "توکن مالک پردازش"), Map.entry("CREATED_AT", "زمان ایجاد"),
            Map.entry("CREATED_BY", "ایجادکننده"), Map.entry("UPDATED_AT", "زمان آخرین تغییر"), Map.entry("UPDATED_BY", "آخرین تغییردهنده")
    );

    private static final Map<String, String> OPTION_LABELS = Map.ofEntries(
            Map.entry("ACTIVE", "فعال"), Map.entry("INACTIVE", "غیرفعال"), Map.entry("RETIRED", "بازنشسته"),
            Map.entry("DRAFT", "پیش‌نویس"), Map.entry("READY", "آماده بررسی"), Map.entry("APPROVED", "تأییدشده"),
            Map.entry("PENDING", "در انتظار"), Map.entry("REJECTED", "ردشده"), Map.entry("COMPLETED", "تکمیل‌شده"),
            Map.entry("PROCESSING", "در حال پردازش"), Map.entry("RUNNING", "در حال اجرا"), Map.entry("FAILED", "ناموفق"),
            Map.entry("CANCELLED", "لغوشده"), Map.entry("SUCCESS", "موفق"), Map.entry("PASS", "قبول"), Map.entry("FAIL", "رد"),
            Map.entry("ERROR", "خطا"), Map.entry("Y", "بله"), Map.entry("N", "خیر"),
            Map.entry("CONTRACT", "قرارداد"), Map.entry("CUSTOMER", "مشتری"), Map.entry("SEGMENT", "بخش مشتری"),
            Map.entry("PRODUCT", "محصول"), Map.entry("CHANNEL", "کانال"), Map.entry("GENERAL", "عمومی"),
            Map.entry("FIXED", "مبلغ ثابت"), Map.entry("PERCENTAGE", "درصدی"), Map.entry("FIXED_PLUS_PERCENTAGE", "مبلغ ثابت + درصد"),
            Map.entry("PROGRESSIVE", "پلکانی تجمیعی"), Map.entry("BRACKET", "پله‌ای بازه‌ای"), Map.entry("MATRIX", "ماتریسی"),
            Map.entry("USAGE", "مبتنی بر مصرف"), Map.entry("DAILY", "روزانه"), Map.entry("FORMULA", "فرمول"),
            Map.entry("FLOOR", "رو به پایین"), Map.entry("HALF_EVEN", "نیمه به زوج"), Map.entry("HALF_UP", "نیمه رو به بالا"),
            Map.entry("CEILING", "رو به بالا"), Map.entry("DISCOUNT", "تخفیف"), Map.entry("ALLOWANCE", "کمک‌هزینه"),
            Map.entry("FREE_UNITS", "واحد رایگان"), Map.entry("MIN_MAX", "حداقل / حداکثر"), Map.entry("PERIOD_CAP", "سقف دوره‌ای"),
            Map.entry("REGULATORY_CAP", "سقف مقرراتی"), Map.entry("SURCHARGE", "اضافه‌کارمزد"), Map.entry("ADJUSTMENT", "تعدیل"),
            Map.entry("LIMIT", "کنترل سقف"), Map.entry("POST_LIMIT", "پس از کنترل سقف"), Map.entry("EXCLUSIVE", "مالیات افزوده"),
            Map.entry("INCLUSIVE", "مالیات در مبلغ"), Map.entry("NET", "خالص"), Map.entry("GROSS", "ناخالص"),
            Map.entry("FEE_FINAL", "کارمزد نهایی"), Map.entry("FEE", "کارمزد"), Map.entry("TAX", "مالیات"), Map.entry("SHARE", "سهم")
    );

    private final JdbcClient jdbc;
    private final String schemaName;
    private final Map<String, TableDescriptor> descriptorCache = new ConcurrentHashMap<>();

    public Fee2Repository(JdbcClient jdbc, @Value("${core-banking.schemas.fee2:FEE2}") String schemaName) {
        this.jdbc = jdbc;
        this.schemaName = identifier(schemaName);
    }

    public String schemaName() { return schemaName; }

    public boolean tableExists(String tableName) {
        String table = tableName(tableName);
        return jdbc.sql("SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER=:owner AND TABLE_NAME=:table")
                .param("owner", schemaName).param("table", table).query(Long.class).single() > 0;
    }

    public long count(String tableName) {
        return jdbc.sql("SELECT COUNT(*) FROM " + qualified(tableName(tableName))).query(Long.class).single();
    }

    public TableDescriptor descriptor(String tableName) {
        String table = tableName(tableName);
        return descriptorCache.computeIfAbsent(table, this::loadDescriptor);
    }

    public TablePage search(String tableName, String text, int page, int size, String filterColumn, String filterValue) {
        TableDescriptor descriptor = descriptor(tableName);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 200);
        List<String> where = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();

        if (text != null && !text.isBlank()) {
            List<String> searchable = descriptor.columns().stream()
                    .filter(column -> isTextType(column.dataType()) && !"CLOB".equals(column.dataType()))
                    .map(column -> "UPPER(T." + identifier(column.name()) + ") LIKE :q")
                    .toList();
            if (!searchable.isEmpty()) {
                where.add("(" + String.join(" OR ", searchable) + ")");
                params.put("q", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
            }
        }

        if (filterColumn != null && !filterColumn.isBlank() && filterValue != null && !filterValue.isBlank()) {
            ColumnDescriptor column = descriptor.columns().stream()
                    .filter(c -> c.name().equalsIgnoreCase(filterColumn))
                    .findFirst().orElseThrow(() -> new Fee2ValidationException("فیلتر برای این ستون مجاز نیست: " + filterColumn));
            where.add("T." + identifier(column.name()) + " = :filterValue");
            params.put("filterValue", filterDatabaseValue(column, filterValue));
        }

        String whereSql = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);
        long total = jdbc.sql("SELECT COUNT(*) FROM " + qualified(descriptor.tableName()) + " T" + whereSql)
                .params(params).query(Long.class).single();

        String select = descriptor.columns().stream().map(c -> "T." + identifier(c.name())).reduce((a,b) -> a + ", " + b).orElse("T.*");
        String sql = "SELECT " + select + " FROM " + qualified(descriptor.tableName()) + " T" + whereSql
                + " ORDER BY T." + identifier(descriptor.primaryKeyColumn())
                + " OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        Map<String,Object> pageParams = new LinkedHashMap<>(params);
        pageParams.put("offset", safePage * safeSize);
        pageParams.put("pageSize", safeSize);
        List<Map<String,Object>> rows = jdbc.sql(sql).params(pageParams)
                .query((rs, rowNum) -> mapRow(rs, descriptor.columns())).list();
        return new TablePage(rows, total, safePage, safeSize);
    }

    public Optional<Map<String,Object>> findById(String tableName, String id) {
        TableDescriptor descriptor = descriptor(tableName);
        String sql = "SELECT " + descriptor.columns().stream().map(c -> "T." + identifier(c.name())).reduce((a,b) -> a + ", " + b).orElse("T.*")
                + " FROM " + qualified(descriptor.tableName()) + " T WHERE T." + identifier(descriptor.primaryKeyColumn()) + "=:id";
        return jdbc.sql(sql).param("id", id).query((rs,rowNum) -> mapRow(rs, descriptor.columns())).optional();
    }

    public List<SelectOption> lookup(String tableName, String text, int limit) {
        String table = tableName(tableName);
        TableDescriptor d = descriptor(table);
        ColumnDescriptor pk = d.columns().stream().filter(ColumnDescriptor::primaryKey).findFirst().orElseThrow();
        String labelExpression = labelExpression(table, "T");
        List<String> where = new ArrayList<>();
        Map<String,Object> params = new LinkedHashMap<>();
        if (text != null && !text.isBlank()) {
            where.add("UPPER(" + labelExpression + ") LIKE :q");
            params.put("q", "%" + text.trim().toUpperCase(Locale.ROOT) + "%");
        }
        params.put("limit", Math.min(Math.max(limit,1), 1000));
        String sql = "SELECT T." + identifier(pk.name()) + " AS V, " + labelExpression + " AS L FROM " + qualified(table) + " T"
                + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY " + labelExpression + " FETCH FIRST :limit ROWS ONLY";
        return jdbc.sql(sql).params(params).query((rs,rowNum) -> {
            Object value = rs.getObject("V");
            String label = rs.getString("L");
            return new SelectOption(value == null ? null : value.toString(), value == null ? "" : value.toString(), label);
        }).list();
    }

    public String insert(String tableName, Map<String,Object> values, String actor) {
        TableDescriptor d = descriptor(tableName);
        if (!d.editable()) throw new Fee2ValidationException("این فرم فقط خواندنی است: " + d.title());
        String id = UUID.randomUUID().toString();
        List<String> columns = new ArrayList<>();
        List<String> binds = new ArrayList<>();
        Map<String,Object> params = new LinkedHashMap<>();

        if ("ID".equals(d.primaryKeyColumn())) {
            columns.add("ID"); binds.add(":generatedId"); params.put("generatedId", id);
        }

        for (ColumnDescriptor column : d.columns()) {
            if (column.primaryKey() || column.readOnly()) continue;
            Object raw = values.get(column.name());
            if (raw == null && values.containsKey(toCamel(column.name()))) raw = values.get(toCamel(column.name()));
            if (raw == null && column.defaultValue() != null) continue;
            columns.add(identifier(column.name())); binds.add(":" + column.name());
            params.put(column.name(), databaseValue(column, raw));
        }

        if (hasColumn(d, "CREATED_AT")) { columns.add("CREATED_AT"); binds.add("SYSTIMESTAMP"); }
        if (hasColumn(d, "CREATED_BY")) { columns.add("CREATED_BY"); binds.add(":actor"); params.put("actor", actor); }
        if ("FEE_VERSION".equals(d.tableName())) {
            columns.add("VERSION_NO"); binds.add("(SELECT NVL(MAX(VERSION_NO),0)+1 FROM " + qualified("FEE_VERSION") + " WHERE FEE_ID=:versionFeeId)");
            columns.add("STATUS"); binds.add("'DRAFT'");
            params.put("versionFeeId", value(values, "FEE_ID"));
        }
        if ("FEE_SIMULATION_RUN".equals(d.tableName())) {
            columns.add("STATUS"); binds.add("'PENDING'");
            columns.add("REQUESTED_BY"); binds.add(":requestedBy"); params.put("requestedBy", actor);
            columns.add("REQUESTED_AT"); binds.add("SYSTIMESTAMP");
        }

        String sql = "INSERT INTO " + qualified(d.tableName()) + " (" + String.join(",", columns) + ") VALUES (" + String.join(",", binds) + ")";
        jdbc.sql(sql).params(params).update();
        return id;
    }

    public boolean update(String tableName, String id, Map<String,Object> values, String actor) {
        TableDescriptor d = descriptor(tableName);
        if (!d.editable()) throw new Fee2ValidationException("این فرم فقط خواندنی است: " + d.title());
        List<String> sets = new ArrayList<>();
        Map<String,Object> params = new LinkedHashMap<>();
        for (ColumnDescriptor column : d.columns()) {
            if (column.primaryKey() || column.readOnly()) continue;
            Object raw = values.get(column.name());
            if (raw == null && values.containsKey(toCamel(column.name()))) raw = values.get(toCamel(column.name()));
            sets.add(identifier(column.name()) + "=:" + column.name());
            params.put(column.name(), databaseValue(column, raw));
        }
        if (hasColumn(d, "UPDATED_AT")) sets.add("UPDATED_AT=SYSTIMESTAMP");
        if (hasColumn(d, "UPDATED_BY")) { sets.add("UPDATED_BY=:actor"); params.put("actor", actor); }
        StringBuilder where = new StringBuilder(identifier(d.primaryKeyColumn()) + "=:id");
        params.put("id", id);
        if (hasColumn(d, "ROW_VERSION")) {
            Object expected = value(values, "ROW_VERSION");
            if (expected == null) throw new Fee2ValidationException("نسخه رکورد برای کنترل همزمانی ارسال نشده است.");
            sets.add("ROW_VERSION=ROW_VERSION+1");
            where.append(" AND ROW_VERSION=:expectedVersion");
            params.put("expectedVersion", new BigDecimal(expected.toString()));
        }
        return jdbc.sql("UPDATE " + qualified(d.tableName()) + " SET " + String.join(",", sets) + " WHERE " + where)
                .params(params).update() == 1;
    }

    public boolean delete(String tableName, String id) {
        TableDescriptor d = descriptor(tableName);
        if (!d.editable()) throw new Fee2ValidationException("این فرم فقط خواندنی است: " + d.title());
        if ("FEE_VERSION".equals(d.tableName())) {
            String status = jdbc.sql("SELECT STATUS FROM " + qualified("FEE_VERSION") + " WHERE ID=:id")
                    .param("id",id).query(String.class).optional().orElse(null);
            if (status != null && !"DRAFT".equals(status)) throw new Fee2ValidationException("فقط نسخه پیش‌نویس قابل حذف است.");
        }
        return jdbc.sql("DELETE FROM " + qualified(d.tableName()) + " WHERE " + identifier(d.primaryKeyColumn()) + "=:id")
                .param("id", id).update() == 1;
    }

    public long countEnabledBindings(String versionId) {
        return jdbc.sql("SELECT COUNT(*) FROM " + qualified("FEE_BINDING") + " WHERE FEE_VERSION_ID=:id AND ENABLED_FLAG='Y'")
                .param("id", versionId).query(Long.class).single();
    }

    public boolean updateCalculation(String id, CalculationConfigRequest request, String configJson, String actor) {
        String sql = "UPDATE " + qualified("FEE_VERSION") + " SET "
                + "CALCULATION_TYPE=:calculationType, BASIS_CODE=:basisCode, BASIS_UNIT=:basisUnit, "
                + "FIXED_AMOUNT=:fixedAmount, RATE_VALUE=:rateValue, MIN_AMOUNT=:minAmount, MAX_AMOUNT=:maxAmount, "
                + "ROUNDING_MODE=:roundingMode, ROUNDING_QUANTUM=:roundingQuantum, PERIOD_POLICY=:periodPolicy, "
                + "DAY_BASIS=:dayBasis, CALC_CONFIG_SCHEMA_VERSION=1, CALC_CONFIG_JSON=:configJson, "
                + "UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, ROW_VERSION=ROW_VERSION+1 "
                + "WHERE ID=:id AND STATUS='DRAFT' AND ROW_VERSION=:rowVersion";
        return jdbc.sql(sql)
                .param("calculationType", request.calculationType())
                .param("basisCode", request.basisCode())
                .param("basisUnit", request.basisUnit())
                .param("fixedAmount", request.fixedAmount())
                .param("rateValue", request.rateValue())
                .param("minAmount", request.minAmount())
                .param("maxAmount", request.maxAmount())
                .param("roundingMode", request.roundingMode())
                .param("roundingQuantum", request.roundingQuantum())
                .param("periodPolicy", request.periodPolicy())
                .param("dayBasis", request.dayBasis())
                .param("configJson", configJson)
                .param("actor", actor)
                .param("id", id)
                .param("rowVersion", request.rowVersion())
                .update() == 1;
    }

    public boolean transitionVersion(String id, String expectedStatus, String targetStatus, String actor) {
        String sql = "UPDATE " + qualified("FEE_VERSION") + " SET STATUS=:target, UPDATED_AT=SYSTIMESTAMP, UPDATED_BY=:actor, "
                + "ROW_VERSION=ROW_VERSION+1" + ("ACTIVE".equals(targetStatus) ? ", ACTIVATED_AT=SYSTIMESTAMP" : "")
                + " WHERE ID=:id AND STATUS=:expected";
        return jdbc.sql(sql).param("target",targetStatus).param("actor",actor).param("id",id).param("expected",expectedStatus).update() == 1;
    }

    public void addApproval(String versionId, String actionCode, String decisionStatus, String actor, String comment) {
        String sql = "INSERT INTO " + qualified("FEE_APPROVAL")
                + " (ID,FEE_VERSION_ID,ACTION_CODE,DECISION_STATUS,ROLE_CODE,PRINCIPAL_ID,COMMENT_TEXT,DECIDED_AT,CREATED_AT)"
                + " VALUES (:id,:ver,:action,:status,'FEE_APPROVER',:actor,:comment,SYSTIMESTAMP,SYSTIMESTAMP)";
        jdbc.sql(sql).param("id",UUID.randomUUID().toString()).param("ver",versionId).param("action",actionCode)
                .param("status",decisionStatus).param("actor",actor).param("comment",comment).update();
    }

    private TableDescriptor loadDescriptor(String table) {
        Fee2Catalog.Entry entry = Fee2Catalog.require(table);
        if (!tableExists(table)) throw new Fee2ValidationException("جدول در Schema FEE2 موجود نیست: " + table);
        String pk = loadPrimaryKey(table);
        Map<String,ForeignKeyInfo> fks = loadForeignKeys(table);
        Map<String,List<SelectOption>> checks = loadCheckOptions(table);
        String sql = "SELECT C.COLUMN_NAME,C.DATA_TYPE,C.CHAR_LENGTH,C.DATA_PRECISION,C.DATA_SCALE,C.NULLABLE,C.DATA_DEFAULT,CC.COMMENTS "
                + "FROM ALL_TAB_COLUMNS C LEFT JOIN ALL_COL_COMMENTS CC ON CC.OWNER=C.OWNER AND CC.TABLE_NAME=C.TABLE_NAME AND CC.COLUMN_NAME=C.COLUMN_NAME "
                + "WHERE C.OWNER=:owner AND C.TABLE_NAME=:table ORDER BY C.COLUMN_ID";
        List<ColumnDescriptor> columns = jdbc.sql(sql).param("owner",schemaName).param("table",table)
                .query((rs,rowNum) -> {
                    String name = rs.getString("COLUMN_NAME");
                    ForeignKeyInfo fk = fks.get(name);
                    String type = normalizeType(rs.getString("DATA_TYPE"));
                    boolean readOnly = isReadOnly(entry, table, name);
                    List<SelectOption> options = checks.getOrDefault(name, List.of());
                    return new ColumnDescriptor(name, label(name, rs.getString("COMMENTS")), type,
                            integerOrNull(rs.getObject("CHAR_LENGTH")), integerOrNull(rs.getObject("DATA_PRECISION")), integerOrNull(rs.getObject("DATA_SCALE")),
                            "Y".equals(rs.getString("NULLABLE")), name.equals(pk), fk != null,
                            fk == null ? null : fk.parentTable(), fk == null ? null : fk.parentColumn(),
                            readOnly, trimOrNull(rs.getString("DATA_DEFAULT")), options);
                }).list();
        return new TableDescriptor(schemaName, table, entry.title(), entry.description(), entry.groupCode(), entry.groupTitle(), entry.editable(), pk, columns);
    }

    private boolean isReadOnly(Fee2Catalog.Entry entry, String table, String column) {
        if (!entry.editable()) return true;
        if ("ID".equals(column) || AUDIT_MANAGED.contains(column) || TECHNICAL_READ_ONLY.contains(column)) return true;
        if ("FEE_VERSION".equals(table) && VERSION_SYSTEM.contains(column)) return true;
        if ("FEE_SIMULATION_RUN".equals(table) && SIM_RUN_SYSTEM.contains(column)) return true;
        if ("FEE_SIMULATION_CASE".equals(table) && "CREATED_AT".equals(column)) return true;
        return false;
    }

    private String loadPrimaryKey(String table) {
        String sql = "SELECT CC.COLUMN_NAME FROM ALL_CONSTRAINTS C JOIN ALL_CONS_COLUMNS CC ON CC.OWNER=C.OWNER AND CC.CONSTRAINT_NAME=C.CONSTRAINT_NAME "
                + "WHERE C.OWNER=:owner AND C.TABLE_NAME=:table AND C.CONSTRAINT_TYPE='P' ORDER BY CC.POSITION";
        List<String> columns = jdbc.sql(sql).param("owner",schemaName).param("table",table).query(String.class).list();
        if (columns.size()!=1) throw new Fee2ValidationException("جدول FEE2 باید PK تک‌ستونی داشته باشد: " + table);
        return columns.getFirst();
    }

    private Map<String,ForeignKeyInfo> loadForeignKeys(String table) {
        String sql = "SELECT CC.COLUMN_NAME,P.TABLE_NAME PARENT_TABLE,PCC.COLUMN_NAME PARENT_COLUMN FROM ALL_CONSTRAINTS C "
                + "JOIN ALL_CONS_COLUMNS CC ON CC.OWNER=C.OWNER AND CC.CONSTRAINT_NAME=C.CONSTRAINT_NAME "
                + "JOIN ALL_CONSTRAINTS P ON P.OWNER=C.R_OWNER AND P.CONSTRAINT_NAME=C.R_CONSTRAINT_NAME "
                + "JOIN ALL_CONS_COLUMNS PCC ON PCC.OWNER=P.OWNER AND PCC.CONSTRAINT_NAME=P.CONSTRAINT_NAME AND PCC.POSITION=CC.POSITION "
                + "WHERE C.OWNER=:owner AND C.TABLE_NAME=:table AND C.CONSTRAINT_TYPE='R'";
        Map<String,ForeignKeyInfo> result = new HashMap<>();
        jdbc.sql(sql).param("owner",schemaName).param("table",table)
                .query((rs,rowNum)->new ForeignKeyInfo(rs.getString("COLUMN_NAME"),rs.getString("PARENT_TABLE"),rs.getString("PARENT_COLUMN")))
                .list().forEach(fk->result.put(fk.column(),fk));
        return result;
    }

    private Map<String,List<SelectOption>> loadCheckOptions(String table) {
        String sql = "SELECT SEARCH_CONDITION_VC FROM ALL_CONSTRAINTS WHERE OWNER=:owner AND TABLE_NAME=:table AND CONSTRAINT_TYPE='C' AND SEARCH_CONDITION_VC IS NOT NULL";
        Map<String,LinkedHashSet<String>> raw = new LinkedHashMap<>();
        for (String condition : jdbc.sql(sql).param("owner",schemaName).param("table",table).query(String.class).list()) {
            Matcher matcher = IN_EXPRESSION.matcher(condition);
            while (matcher.find()) {
                String column = matcher.group(1).toUpperCase(Locale.ROOT);
                for (String token : splitValues(matcher.group(2))) raw.computeIfAbsent(column,k->new LinkedHashSet<>()).add(token);
            }
        }
        Map<String,List<SelectOption>> result = new LinkedHashMap<>();
        raw.forEach((column,values)->result.put(column, values.stream().map(this::checkOption).toList()));
        return result;
    }

    private SelectOption checkOption(String raw) {
        String value = raw.trim();
        if (value.startsWith("'") && value.endsWith("'")) value = value.substring(1,value.length()-1).replace("''","'");
        String label = OPTION_LABELS.getOrDefault(value, value);
        return new SelectOption(value,value,label);
    }

    private static List<String> splitValues(String text) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder(); boolean quoted=false;
        for (int i=0;i<text.length();i++) {
            char ch=text.charAt(i);
            if (ch=='\'' ) quoted=!quoted;
            if (ch==',' && !quoted) { values.add(current.toString().trim()); current.setLength(0); }
            else current.append(ch);
        }
        if (!current.isEmpty()) values.add(current.toString().trim());
        return values;
    }

    private String labelExpression(String table, String alias) {
        return switch (table) {
            case "FEE_SCOPE" -> "NVL("+alias+".NAME_FA,"+alias+".SCOPE_CODE)";
            case "FEE_DEFINITION" -> "NVL("+alias+".NAME_FA,"+alias+".FEE_CODE)";
            case "FEE_VERSION" -> "'نسخه '||TO_CHAR("+alias+".VERSION_NO)||' - '||"+alias+".STATUS";
            case "FEE_REGULATION" -> "NVL("+alias+".TITLE_FA,"+alias+".REGULATION_CODE)";
            case "FEE_SIMULATION_RUN" -> alias+".NAME";
            case "FEE_SIMULATION_CASE" -> alias+".CASE_NAME";
            default -> alias+"." + identifier(descriptor(table).primaryKeyColumn());
        };
    }

    private Map<String,Object> mapRow(ResultSet rs, List<ColumnDescriptor> columns) throws SQLException {
        Map<String,Object> row = new LinkedHashMap<>();
        for (ColumnDescriptor column : columns) row.put(column.name(), readValue(rs,column));
        return row;
    }

    private Object readValue(ResultSet rs, ColumnDescriptor column) throws SQLException {
        String name = column.name();
        if ("CLOB".equals(column.dataType())) {
            Clob clob = rs.getClob(name);
            return clob == null ? null : clob.getSubString(1,(int)Math.min(clob.length(),Integer.MAX_VALUE));
        }
        if (column.dataType().startsWith("TIMESTAMP")) {
            try {
                OffsetDateTime value = rs.getObject(name, OffsetDateTime.class);
                if (value != null) return value.toString();
            } catch (SQLException | RuntimeException ignored) {
                // Fallback for older Oracle JDBC mappings.
            }
            Timestamp value = rs.getTimestamp(name);
            return value == null ? null : value.toLocalDateTime().toString();
        }
        if ("DATE".equals(column.dataType())) {
            Date date = rs.getDate(name); return date == null ? null : date.toLocalDate().toString();
        }
        if (column.dataType().startsWith("NUMBER")) return rs.getBigDecimal(name);
        return rs.getString(name);
    }

    private Object databaseValue(ColumnDescriptor column, Object raw) {
        if (raw == null || (raw instanceof String s && s.isBlank())) return null;
        String type = column.dataType();
        if (type.startsWith("NUMBER")) return raw instanceof BigDecimal ? raw : new BigDecimal(raw.toString().replace(",",""));
        if ("DATE".equals(type)) return Date.valueOf(raw.toString().substring(0,10));
        if (type.startsWith("TIMESTAMP")) {
            String text = raw.toString().trim();
            try { return OffsetDateTime.parse(text); } catch (RuntimeException ignored) { }
            String normalized = text.replace('T',' ');
            if (normalized.length()==16) normalized += ":00";
            return Timestamp.valueOf(normalized);
        }
        return raw.toString();
    }

    private Object filterDatabaseValue(ColumnDescriptor column, String raw) {
        if (column.dataType().startsWith("NUMBER")) return new BigDecimal(raw);
        return raw;
    }

    private static Object value(Map<String,Object> values, String column) {
        Object direct = values.get(column); return direct != null ? direct : values.get(toCamel(column));
    }
    private static boolean hasColumn(TableDescriptor d, String name) { return d.columns().stream().anyMatch(c->c.name().equals(name)); }
    private static boolean isTextType(String type) { return type.contains("CHAR") || "VARCHAR2".equals(type) || "NVARCHAR2".equals(type); }
    private static String normalizeType(String raw) { return raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("\\s+"," ").trim(); }
    private static Integer integerOrNull(Object value) { return value == null ? null : ((Number)value).intValue(); }
    private static String trimOrNull(String value) { return value == null ? null : value.trim(); }
    private static String label(String column, String comment) {
        if (comment != null && !comment.isBlank()) return comment.trim();
        return LABELS.getOrDefault(column, "فیلد " + column);
    }
    private String tableName(String raw) {
        String table = identifier(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        Fee2Catalog.require(table); return table;
    }
    private String qualified(String table) { return schemaName + "." + identifier(table); }
    private static String identifier(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!IDENTIFIER.matcher(value).matches()) throw new Fee2ValidationException("شناسه Oracle معتبر نیست: " + raw);
        return value;
    }
    private static String toCamel(String column) {
        StringBuilder out = new StringBuilder(); boolean upper=false;
        for (char ch : column.toLowerCase(Locale.ROOT).toCharArray()) {
            if (ch=='_') { upper=true; continue; }
            out.append(upper ? Character.toUpperCase(ch) : ch); upper=false;
        }
        return out.toString();
    }

    private record ForeignKeyInfo(String column, String parentTable, String parentColumn) {}
}
