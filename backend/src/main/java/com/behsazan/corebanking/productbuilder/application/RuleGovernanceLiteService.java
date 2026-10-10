package com.behsazan.corebanking.productbuilder.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * R20 Lite: two installed PDL policy tables. No rule DSL, hierarchy, or arbitrary SQL.
 * These endpoints are for prototype UAT. Replace X-User-Name with authenticated
 * principal and role authorization before production use.
 */
@Service
public class RuleGovernanceLiteService {
    public record Policy(long id, String code, long versionNo, String name, String state,
                         LocalDate validFrom, LocalDate validTo, String maker,
                         String checker, String approvalReference, long controlCount) {}
    public record Control(long id, String code, String title, String category, String module,
                          String presence, String absence, boolean runtimeRequired, String description) {}
    public record CreatePolicy(String code, String name, LocalDate validFrom, LocalDate validTo) {}
    public record ChangeControl(String code, String category, String module, String presence,
                                String absence, boolean runtimeRequired, String description) {}
    public record Approval(String reference) {}
    public record Binding(long policyId) {}

    private static final Set<String> CATEGORIES = Set.of("REGULATORY", "BANK_WIDE", "RISK_COMPLIANCE", "PRODUCT_POLICY", "CONTRACT_VERSION", "OPERATIONS", "APPROVED_EXCEPTION");
    private static final Set<String> PRESENCES = Set.of("REQUIRED", "WHEN_ENABLED", "OPTIONAL");
    private static final Set<String> ABSENCES = Set.of("BLOCK_PUBLISH", "BLOCK_OPERATION", "WARN", "IGNORE");
    private static final Pattern POLICY_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,59}");

    private final JdbcClient jdbc;
    private final String pdl;
    public RuleGovernanceLiteService(JdbcClient jdbc, @Value("${core-banking.schemas.product-definition:PDL}") String schema) {
        this.jdbc = jdbc;
        if (!schema.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("Invalid schema identifier");
        this.pdl = schema;
    }
    private String table(String name) { return pdl + "." + name; }
    private long next(String sequence) { return jdbc.sql("SELECT " + table(sequence) + ".NEXTVAL FROM DUAL").query(Long.class).single(); }
    private static String actor(String actor) {
        String name = actor == null ? "" : actor.trim();
        if (name.isBlank() || name.length() > 100) throw new ProductBuilderValidationException("شناسه کاربر معتبر لازم است.");
        return name;
    }
    private static String required(String value, int max, String label) {
        String result = value == null ? "" : value.trim();
        if (result.isBlank() || result.length() > max) throw new ProductBuilderValidationException(label + " الزامی یا طول آن نامعتبر است.");
        return result;
    }
    private static String validEnum(String value, Set<String> allowed, String label) {
        String v = required(value, 100, label);
        if (!allowed.contains(v)) throw new ProductBuilderValidationException("مقدار " + label + " معتبر نیست.");
        return v;
    }
    private static String upperCode(String code) {
        String v = required(code, 60, "کد سیاست").toUpperCase();
        if (!POLICY_CODE.matcher(v).matches()) throw new ProductBuilderValidationException("کد سیاست فقط حروف لاتین، ارقام و _ را می‌پذیرد.");
        return v;
    }
    private static void dates(LocalDate from, LocalDate to) {
        if (from == null || to != null && to.isBefore(from))
            throw new ProductBuilderValidationException("بازه اعتبار سیاست نامعتبر است.");
    }

    public List<Policy> policies() {
        return jdbc.sql("SELECT P.*, (SELECT COUNT(*) FROM " + table("RULE_CONTROL_POLICY") + " C WHERE C.RULE_POLICY_VERSION_ID=P.RULE_POLICY_VERSION_ID) CONTROL_COUNT FROM " + table("RULE_POLICY_VERSION") + " P ORDER BY P.POLICY_CODE, P.POLICY_VERSION_NO DESC")
                .query((r,n) -> mapPolicy(r)).list();
    }
    private static Policy mapPolicy(java.sql.ResultSet r) throws java.sql.SQLException {
        Date to = r.getDate("VALID_TO");
        return new Policy(r.getLong("RULE_POLICY_VERSION_ID"), r.getString("POLICY_CODE"), r.getLong("POLICY_VERSION_NO"),
                r.getString("POLICY_NAME_FA"), r.getString("STATE_CODE"), r.getDate("VALID_FROM").toLocalDate(),
                to == null ? null : to.toLocalDate(), r.getString("CREATED_BY"), r.getString("APPROVED_BY"),
                r.getString("APPROVAL_REFERENCE"), r.getLong("CONTROL_COUNT"));
    }
    public Policy policy(long id) {
        return jdbc.sql("SELECT P.*, (SELECT COUNT(*) FROM " + table("RULE_CONTROL_POLICY") + " C WHERE C.RULE_POLICY_VERSION_ID=P.RULE_POLICY_VERSION_ID) CONTROL_COUNT FROM " + table("RULE_POLICY_VERSION") + " P WHERE P.RULE_POLICY_VERSION_ID=:id")
                .param("id", id).query((r,n) -> mapPolicy(r)).optional()
                .orElseThrow(() -> new ProductBuilderValidationException("نسخه سیاست یافت نشد."));
    }
    public List<Control> controls(long id) {
        return jdbc.sql("SELECT * FROM " + table("RULE_CONTROL_POLICY") + " WHERE RULE_POLICY_VERSION_ID=:id ORDER BY CONTROL_CODE")
                .param("id", id).query((r,n)-> new Control(r.getLong("RULE_CONTROL_POLICY_ID"),r.getString("CONTROL_CODE"),
                        controlTitle(r.getString("CONTROL_CODE")),r.getString("GOVERNANCE_CATEGORY_CODE"),r.getString("MODULE_CODE"),
                        r.getString("PRESENCE_MODE_CODE"),r.getString("ABSENCE_ACTION_CODE"),r.getInt("RUNTIME_REQUIRED")==1,
                        r.getString("DESCRIPTION_FA"))).list();
    }
    private static String controlTitle(String code) {
        return ProductRuleGovernanceService.defaultControls().stream().filter(c -> c.code().equals(code))
                .map(ProductRuleGovernanceService.RuleControl::title).findFirst().orElse(code);
    }
    private void ensureDraft(long id) {
        if (!"DRAFT".equals(policy(id).state())) throw new ProductBuilderValidationException("نسخه مصوب قابل ویرایش نیست؛ نسخه تازه بسازید.");
    }

    @Transactional
    public Policy create(CreatePolicy request, String username) {
        if (request == null) throw new ProductBuilderValidationException("مشخصات سیاست الزامی است.");
        String code = upperCode(request.code());
        String name = required(request.name(), 200, "نام سیاست");
        dates(request.validFrom(), request.validTo());
        long version = jdbc.sql("SELECT NVL(MAX(POLICY_VERSION_NO),0)+1 FROM " + table("RULE_POLICY_VERSION") + " WHERE POLICY_CODE=:code")
                .param("code",code).query(Long.class).single();
        return createVersion(code,version,name,request.validFrom(),request.validTo(),actor(username),null);
    }
    @Transactional
    public Policy clonePolicy(long sourceId, String username) {
        Policy source=policy(sourceId);
        long nextVersion=jdbc.sql("SELECT NVL(MAX(POLICY_VERSION_NO),0)+1 FROM " + table("RULE_POLICY_VERSION") + " WHERE POLICY_CODE=:code")
                .param("code",source.code()).query(Long.class).single();
        return createVersion(source.code(),nextVersion,source.name(),source.validFrom(),source.validTo(),actor(username),sourceId);
    }
    private Policy createVersion(String code, long versionNo, String name, LocalDate from, LocalDate to, String maker, Long copyFrom) {
        long id=next("SEQ_RULE_POLICY_VERSION");
        jdbc.sql("INSERT INTO " + table("RULE_POLICY_VERSION") + " (RULE_POLICY_VERSION_ID,POLICY_CODE,POLICY_VERSION_NO,POLICY_NAME_FA,VALID_FROM,VALID_TO,CREATED_BY) VALUES (:id,:code,:versionNo,:name,:validFrom,:validTo,:maker)")
                .param("id",id).param("code",code).param("versionNo",versionNo).param("name",name)
                .param("validFrom",Date.valueOf(from)).param("validTo",to==null?null:Date.valueOf(to)).param("maker",maker).update();
        if (copyFrom == null) {
            for (ProductRuleGovernanceService.RuleControl control : ProductRuleGovernanceService.defaultControls()) {
                ChangeControl defaultControl = new ChangeControl(control.code(),control.governanceLevel(),control.moduleCode(),
                        control.presencePolicy(),"REQUIRED".equals(control.presencePolicy())?"BLOCK_PUBLISH":"IGNORE",
                        "DEPOSIT_PRODUCT_TRANSACTION_RULE".equals(control.code()),null);
                insertControl(id,defaultControl,maker);
            }
        } else {
            for (Control c : controls(copyFrom)) insertControl(id,new ChangeControl(c.code(),c.category(),c.module(),
                    c.presence(),c.absence(),c.runtimeRequired(),c.description()),maker);
        }
        return policy(id);
    }
    @Transactional
    public List<Control> saveControls(long id, List<ChangeControl> entries, String username) {
        ensureDraft(id);
        String maker=actor(username);
        if (entries == null || entries.isEmpty() || entries.size()>100) throw new ProductBuilderValidationException("حداقل یک کنترل معتبر لازم است.");
        Set<String> knownCodes = ProductRuleGovernanceService.defaultControls().stream().map(ProductRuleGovernanceService.RuleControl::code).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> unique=new java.util.HashSet<>();
        for(ChangeControl c:entries) {
            if(c==null || !knownCodes.contains(c.code()) || !unique.add(c.code()))
                throw new ProductBuilderValidationException("کد کنترل ناشناخته یا تکراری است.");
            validateControl(c);
        }
        // No delete-all: stable control identifiers and the DRAFT_ONLY trigger remain authoritative.
        for(ChangeControl c:entries) {
            int updated=jdbc.sql("UPDATE " + table("RULE_CONTROL_POLICY") + " SET GOVERNANCE_CATEGORY_CODE=:category,MODULE_CODE=:module,PRESENCE_MODE_CODE=:presence,ABSENCE_ACTION_CODE=:absence,RUNTIME_REQUIRED=:runtime,DESCRIPTION_FA=:description,UPDATED_BY=:maker,UPDATED_AT=SYSTIMESTAMP WHERE RULE_POLICY_VERSION_ID=:id AND CONTROL_CODE=:code")
                    .param("category",c.category()).param("module",c.module()).param("presence",c.presence()).param("absence",c.absence())
                    .param("runtime",c.runtimeRequired()?1:0).param("description",c.description()).param("maker",maker)
                    .param("id",id).param("code",c.code()).update();
            if(updated == 0) insertControl(id,c,maker);
        }
        return controls(id);
    }
    private void validateControl(ChangeControl c) {
        validEnum(c.category(),CATEGORIES,"دسته حاکمیتی");
        validEnum(c.presence(),PRESENCES,"الزام حضور");
        validEnum(c.absence(),ABSENCES,"رفتار نبود");
        if ("WHEN_ENABLED".equals(c.presence()) && (c.module()==null || c.module().isBlank()))
            throw new ProductBuilderValidationException("برای سیاست مشروط، کد ماژول الزامی است.");
        if (c.module()!=null && c.module().length()>40 || c.description()!=null && c.description().length()>1000)
            throw new ProductBuilderValidationException("طول متن کنترل بیشتر از حد مجاز است.");
    }
    private void insertControl(long id, ChangeControl c, String maker) {
        validateControl(c);
        jdbc.sql("INSERT INTO " + table("RULE_CONTROL_POLICY") + " (RULE_CONTROL_POLICY_ID,RULE_POLICY_VERSION_ID,CONTROL_CODE,GOVERNANCE_CATEGORY_CODE,MODULE_CODE,PRESENCE_MODE_CODE,ABSENCE_ACTION_CODE,RUNTIME_REQUIRED,DESCRIPTION_FA,CREATED_BY) VALUES (:pk,:id,:code,:category,:module,:presence,:absence,:runtime,:description,:maker)")
                .param("pk",next("SEQ_RULE_CONTROL_POLICY")).param("id",id).param("code",c.code()).param("category",c.category())
                .param("module",c.module()).param("presence",c.presence()).param("absence",c.absence())
                .param("runtime",c.runtimeRequired()?1:0).param("description",c.description()).param("maker",maker).update();
    }
    @Transactional
    public Policy approve(long id, Approval approval, String username) {
        Policy current=policy(id);
        ensureDraft(id);
        String checker=actor(username);
        if(checker.equalsIgnoreCase(current.maker())) throw new ProductBuilderValidationException("ایجادکننده نمی‌تواند تصویب‌کننده همان نسخه باشد.");
        String reference=required(approval==null?null:approval.reference(),200,"مرجع تصویب");
        if(controls(id).size()!=ProductRuleGovernanceService.defaultControls().size())
            throw new ProductBuilderValidationException("سیاست باید هر ۲۳ کنترل شناخته‌شده را به‌صورت صریح پوشش دهد.");
        int updated=jdbc.sql("UPDATE " + table("RULE_POLICY_VERSION") + " SET STATE_CODE='APPROVED',APPROVED_BY=:checker,APPROVED_AT=SYSTIMESTAMP,APPROVAL_REFERENCE=:reference,UPDATED_BY=:checker,UPDATED_AT=SYSTIMESTAMP WHERE RULE_POLICY_VERSION_ID=:id AND STATE_CODE='DRAFT'")
                .param("checker",checker).param("reference",reference).param("id",id).update();
        if(updated!=1) throw new ProductBuilderValidationException("وضعیت سیاست هم‌زمان تغییر کرده است.");
        return policy(id);
    }
    @Transactional
    public Map<String,Object> bind(long versionId, Binding request) {
        if(request==null || request.policyId()<=0) throw new ProductBuilderValidationException("نسخه سیاست مصوب را انتخاب کنید.");
        Policy policy=policy(request.policyId());
        if(!"APPROVED".equals(policy.state())) throw new ProductBuilderValidationException("فقط سیاست مصوب قابل اتصال است.");
        Map<String,Object> version=jdbc.sql("SELECT PRODUCT_VERSION_ID,VERSION_STATUS_CODE,VALID_FROM,VALID_TO,RULE_POLICY_VERSION_ID FROM " + table("PRODUCT_VERSION") + " WHERE PRODUCT_VERSION_ID=:id FOR UPDATE")
                .param("id",versionId).query((r,n)-> {
                    Map<String,Object> v=new LinkedHashMap<>();
                    v.put("id",r.getLong("PRODUCT_VERSION_ID"));
                    v.put("status",r.getString("VERSION_STATUS_CODE"));
                    v.put("from",r.getDate("VALID_FROM").toLocalDate());
                    Date to=r.getDate("VALID_TO");v.put("to",to==null?null:to.toLocalDate());
                    v.put("policyId",r.getObject("RULE_POLICY_VERSION_ID"));
                    return v;
                }).optional().orElseThrow(()->new ProductBuilderValidationException("نسخه محصول یافت نشد."));
        if(!"DRAFT".equals(version.get("status"))) throw new ProductBuilderValidationException("اتصال سیاست فقط برای نسخه پیش‌نویس مجاز است.");
        LocalDate from=(LocalDate)version.get("from"), to=(LocalDate)version.get("to");
        if(from.isBefore(policy.validFrom()) || policy.validTo()!=null && (to==null || to.isAfter(policy.validTo())))
            throw new ProductBuilderValidationException("بازه اعتبار سیاست مصوب، کل بازه اعتبار نسخه محصول را پوشش نمی‌دهد.");
        jdbc.sql("UPDATE " + table("PRODUCT_VERSION") + " SET RULE_POLICY_VERSION_ID=:policyId,RECORD_VERSION=RECORD_VERSION+1,UPDATED_AT=SYSTIMESTAMP WHERE PRODUCT_VERSION_ID=:versionId")
                .param("policyId",request.policyId()).param("versionId",versionId).update();
        return Map.of("productVersionId",versionId,"rulePolicyVersionId",request.policyId());
    }
}
