package com.behsazan.corebanking.fee3.application;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import com.behsazan.corebanking.fee3.oracle.Fee3Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** FEE3 R1: Oracle reads + controlled, unpublished UAT draft transactions only. */
@Service
public class Fee3Service {
    private final Fee3Repository repository;
    private final boolean readsEnabled;
    private final boolean draftWritesEnabled;
    private final String actor;
    private final String writeToken;
    private final JsonMapper json=new JsonMapper();

    public Fee3Service(Fee3Repository repository,
                       @Value("${FEE3_READS_ENABLED:true}") boolean readsEnabled,
                       @Value("${FEE3_DRAFT_WRITES_ENABLED:false}") boolean draftWritesEnabled,
                       @Value("${FEE3_UAT_ACTOR:FEE3_UAT}") String actor,
                       @Value("${FEE3_UAT_WRITE_TOKEN:}") String writeToken) {
        this.repository=repository;
        this.readsEnabled=readsEnabled;
        this.draftWritesEnabled=draftWritesEnabled;
        if(actor==null||!actor.matches("[A-Za-z0-9_.-]{3,40}"))
            throw new IllegalArgumentException("Invalid configured Fee3 UAT actor");
        this.actor=actor;
        this.writeToken=writeToken == null ? "" : writeToken;
    }
    @Transactional(readOnly=true)
    public Readiness readiness(){
        if(!readsEnabled)return new Readiness(repository.schema(),false,false,"Asia/Tehran",null,List.of(),0,0);
        List<String> missing=repository.missingTables();
        return new Readiness(repository.schema(),true,draftWritesEnabled && writeToken.length()>=20,"Asia/Tehran",
            missing.isEmpty()?repository.bankDate():null,missing,
            missing.isEmpty()?repository.count("FCE_CALC_ITEM"):0,
            missing.isEmpty()?repository.count("FCE_TARIFF_VERSION"):0);
    }
    private void canRead(){
        if(!readsEnabled)throw new Fee3Problem(HttpStatus.SERVICE_UNAVAILABLE,"FEE3_READ_DISABLED","خواندن FEE3 غیرفعال است.");
        List<String> missing=repository.missingTables();
        if(!missing.isEmpty())throw new Fee3Problem(HttpStatus.SERVICE_UNAVAILABLE,"FEE3_SCHEMA_INCOMPLETE",
            "اسکیمای FEE3 کامل نیست یا جداول از Connection فعلی قابل مشاهده نیستند.");
    }
    private void canWrite(String suppliedToken){
        canRead();
        if(!draftWritesEnabled || writeToken.length()<20)
            throw new Fee3Problem(HttpStatus.FORBIDDEN,"FEE3_DRAFT_WRITES_DISABLED",
                "برای درج پیش‌نویس واقعی، تنظیم مجاز UAT و توکن سمت سرور الزامی است.");
        if(suppliedToken==null || !MessageDigest.isEqual(
            suppliedToken.getBytes(StandardCharsets.UTF_8),writeToken.getBytes(StandardCharsets.UTF_8)))
            throw new Fee3Problem(HttpStatus.FORBIDDEN,"FEE3_UAT_TOKEN_DENIED",
                "دسترسی نوشتن پیش‌نویس UAT تأیید نشد.");
    }
    @Transactional(readOnly=true)
    public Page<CatalogRow> catalog(String search,String module,int page,int size){
        canRead();
        if(page<0||page>100000||size<1||size>200)throw validation("صفحه و اندازه صفحه خارج از حد مجاز است.");
        if(search!=null&&search.length()>120)throw validation("عبارت جست‌وجو طولانی است.");
        if(module!=null&&module.length()>30)throw validation("فیلتر بخش بانکی طولانی است.");
        return repository.catalog(search,module,page,size);
    }
    @Transactional(readOnly=true)
    public ItemAggregate item(String code){
        canRead();
        if(code==null||!code.matches("[A-Za-z0-9_-]{1,40}"))throw validation("کد قلم نامعتبر است.");
        return repository.aggregate(code).orElseThrow(()->new Fee3Problem(HttpStatus.NOT_FOUND,
                "FEE3_ITEM_NOT_FOUND","قلم محاسباتی یافت نشد."));
    }
    @Transactional(readOnly=true)
    public List<Operation> operations(){canRead();return repository.operations();}
    @Transactional(readOnly=true)
    public List<Formula> formulas(){canRead();return repository.formulas();}
    @Transactional(readOnly=true)
    public List<Source> sources(){canRead();return repository.sources();}
    @Transactional(readOnly=true)
    public List<RefCode> codes(String group){
        canRead();
        if(group==null||!group.matches("[A-Z0-9_]{1,50}"))throw validation("گروه مرجع معتبر نیست.");
        return repository.codes(group);
    }
    private static Fee3Problem validation(String message){return new Fee3Problem(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_INVALID_DRAFT",message);}

    @Transactional
    public DraftReceipt createDraft(DraftRequest request,String token){
        canWrite(token);
        if(request==null||request.calcItemId()!=null||request.tariffVersionId()!=null||
           request.expectedItemRecordVersion()!=null||request.expectedVersionRecordVersion()!=null)
            throw validation("در ایجاد پیش‌نویس جدید، شناسه و شماره رکورد نسخه نباید ارسال شود.");
        LocalDate today=repository.bankDate();
        verify(request,today);
        Serialized data=serialize(request);
        return repository.insert(request,actor,data.financial(),data.channels(),data.customers(),
            data.groups(),data.products(),data.contracts(),data.rules(),data.tests(),data.audit());
    }
    @Transactional
    public DraftReceipt updateDraft(long itemId,long versionId,DraftRequest request,String token){
        canWrite(token);
        if(itemId<=0||versionId<=0||request==null||request.calcItemId()==null||request.tariffVersionId()==null||
           request.expectedItemRecordVersion()==null||request.expectedVersionRecordVersion()==null||
           request.calcItemId()!=itemId||request.tariffVersionId()!=versionId||
           request.expectedItemRecordVersion()<1||request.expectedVersionRecordVersion()<1)
            throw validation("شناسه‌ها و شماره نسخه همزمانی رکورد باید صحیح و مطابق URL باشند.");
        // Lock item, then version in a consistent order. No writes to any Seed or published versions.
        Map<String,Object> persisted=repository.itemByIdForUpdate(itemId).orElseThrow(()->new Fee3Problem(
            HttpStatus.NOT_FOUND,"FEE3_ITEM_NOT_FOUND","قلم محاسباتی یافت نشد."));
        String code=String.valueOf(persisted.get("CALC_CODE"));
        if(!code.matches("F3-UAT-[0-9]+"))throw new Fee3Problem(HttpStatus.FORBIDDEN,"FEE3_SEED_WRITE_DENIED",
            "ویرایش اقلام Seed و تعرفه‌های عملیاتی از API پیش‌نویس UAT ممنوع است.");
        Map<String,Object> version=repository.versionByIdForUpdate(versionId,itemId).orElseThrow(()->new Fee3Problem(
            HttpStatus.NOT_FOUND,"FEE3_VERSION_NOT_FOUND","نسخه موردنظر یافت نشد."));
        if(!"DRAFT".equals(version.get("STATUS_CODE"))||!Integer.valueOf(0).equals(
             ((Number)version.get("IS_ACTIVE")).intValue()))
            throw new Fee3Problem(HttpStatus.CONFLICT,"FEE3_VERSION_IMMUTABLE","فقط نسخه DRAFT غیرفعال قابل ویرایش است.");
        long itemRv=((Number)persisted.get("RECORD_VERSION")).longValue();
        long versionRv=((Number)version.get("RECORD_VERSION")).longValue();
        if(itemRv!=request.expectedItemRecordVersion()||versionRv!=request.expectedVersionRecordVersion())
            throw new Fee3Problem(HttpStatus.CONFLICT,"FEE3_STALE_VERSION",
                "نسخه رکورد از زمان بارگذاری تغییر کرده است. اطلاعات را مجدداً دریافت کنید.");
        if(!Integer.valueOf(1).equals(((Number)version.get("VERSION_NO")).intValue()))
            throw new Fee3Problem(HttpStatus.FORBIDDEN,"FEE3_UAT_VERSION_ONLY","در R1 فقط نسخه اولیه UAT قابل ویرایش است.");
        if(repository.versionCount(itemId)!=1)
            throw new Fee3Problem(HttpStatus.CONFLICT,"FEE3_UAT_MULTI_VERSION",
                "برای اقلام دارای بیش از یک نسخه، تغییر شناسنامه از مسیر ساده UAT مجاز نیست.");
        if(repository.executedTestCount(versionId)>0)
            throw new Fee3Problem(HttpStatus.CONFLICT,"FEE3_EXECUTED_TEST_PROTECTED",
                "سناریوهای دارای سابقه اجرای آزمون نباید هنگام ویرایش پیش‌نویس حذف شوند.");
        LocalDate today=repository.bankDate();
        verify(request,today);
        Serialized data=serialize(request);
        String before=json.writeValueAsString(Map.of("calcItemId",itemId,"tariffVersionId",versionId,
             "itemRecordVersion",itemRv,"tariffRecordVersion",versionRv,"status","DRAFT"));
        return repository.update(request,actor,data.financial(),data.channels(),data.customers(),data.groups(),
            data.products(),data.contracts(),data.rules(),data.tests(),before,data.audit(),code,
            ((Number)version.get("VERSION_NO")).intValue());
    }
    private void verify(DraftRequest request,LocalDate bankToday){
        if(request==null||request.item()==null||request.version()==null||request.scope()==null)
            throw validation("پیش‌نویس بدون شناسنامه، نسخه یا دامنه است.");
        Operation operation=repository.activeOperation(request.item().module(),request.item().operation())
            .orElseThrow(()->validation("عملیات فعال در FEE3.FCE_REF_OPERATION یافت نشد."));
        Fee3DraftPolicy.check(request,bankToday,operation.allowedAmountKinds());
        Long source=request.version().sourceId();
        String status=source==null?null:repository.sourceStatus(source);
        if(source!=null&&status==null)throw validation("مستند مرجع در Oracle یافت نشد.");
        if("CBI_MANDATORY".equals(request.version().origin()) && !"VERIFIED".equals(status))
            throw validation("برای تعرفه الزام‌آور بانک مرکزی، مستند مرجع تأییدشده الزامی است.");
        List<RuleDraft> rules=request.rules()==null?List.of():request.rules();
        for(RuleDraft rule:rules) {
            if(rule.formulaCode()!=null&&!rule.formulaCode().isBlank()&& !repository.activeFormula(rule.formulaCode()))
                throw validation("کد فرمول انتخابی در کتابخانه فعال Oracle وجود ندارد.");
            if(!rule.method().equals(request.version().method()))
                throw validation("روش محاسبه قاعده با روش نسخه سازگار نیست.");
        }
    }
    private record Serialized(String financial,String channels,String customers,String groups,String products,
                              String contracts,List<String> rules,List<String> tests,String audit) {}
    private Serialized serialize(DraftRequest request){
        ScopeDraft s=request.scope();
        List<String> rules=new ArrayList<>();
        for(RuleDraft r:request.rules()==null?List.<RuleDraft>of():request.rules())
            rules.add(boundedJson(r.parameters()==null?Map.of():r.parameters(),20000,"پارامترهای محاسبه"));
        List<String> tests=new ArrayList<>();
        for(TestDraft t:request.tests()==null?List.<TestDraft>of():request.tests())
            tests.add(boundedJson(t.request()==null?Map.of():t.request(),12000,"درخواست آزمون"));
        // Audit contains business metadata only; not the supplied arbitrary request payload.
        Map<String,Object> audit=new LinkedHashMap<>();
        audit.put("purpose","FEE3_UAT_DRAFT_ONLY");audit.put("operation",request.item().operation());
        audit.put("amountKind",request.item().amountKind());audit.put("ruleCount",rules.size());
        audit.put("validFrom",request.version().validFrom().toString());
        audit.put("status","DRAFT");audit.put("published",false);
        return new Serialized(boundedJson(request.version().financialPolicy()==null?Map.of():request.version().financialPolicy(),20000,"ضوابط مالی"),
            boundedJson(s.channels(),9000,"کانال"),boundedJson(s.customers(),9000,"نوع مشتری"),
            boundedJson(s.groups()==null?List.of("ALL"):s.groups(),9000,"گروه مشتری"),
            boundedJson(s.products()==null?List.of("ALL"):s.products(),9000,"محصول"),
            boundedJson(s.contracts()==null?List.of():s.contracts(),9000,"عقد"),
            rules,tests,boundedJson(audit,1000,"ردپا"));
    }
    private String boundedJson(Object value,int max,String name){
        String serialized;
        try{serialized=json.writeValueAsString(value);}catch(RuntimeException ex){throw validation(name+" قالب JSON معتبر ندارد.");}
        if(serialized.length()>max)throw validation(name+" از حجم مجاز بزرگ‌تر است.");
        return serialized;
    }
}
