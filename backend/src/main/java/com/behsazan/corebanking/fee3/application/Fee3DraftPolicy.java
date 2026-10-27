package com.behsazan.corebanking.fee3.application;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure business guard; all decisions rechecked server-side, never based on UI visibility. */
public final class Fee3DraftPolicy {
    private Fee3DraftPolicy() {}
    private static final Set<String> METHODS = Set.of("FIXED","PERCENT","MIXED","PROGRESSIVE",
            "BRACKET","MATRIX","USAGE","DAILY","FORMULA");
    private static final Set<String> ORIGINS = Set.of("CBI_MANDATORY","BANK_INTERNAL",
            "CONTRACT_RATE_REGULATED","CONTRACTUAL","DEMO_ONLY");
    private static final Set<String> AMOUNT_KINDS = Set.of("SERVICE_FEE","FINANCING_PROFIT",
            "LATE_DAMAGE","QARD_FEE","LEGAL_COST","GUARANTEE_FEE","LC_FEE","OTHER_REVENUE");
    private static final Set<String> RATES = Set.of("RULE","REQUEST");
    // An explicit, versioned UI context contract; no arbitrary new top-level field names.
    private static final Set<String> UI_CONTEXT_KEYS=Set.of(
        "productScope","productCode","productCodes","cardServiceType","remittanceSystem",
        "transferRail","transferBatchMode","transferTimingMode","calculationModelKind",
        "calculationModelCode","calculationModelTitle","loanModelProfile","loanCustomModelTitle",
        "loanCalculationEvent","loanContractTypes","loanFormulaContextReviewRequired",
        "legalCostKind","legalCostCode","legalCostTitle","loanPolicy","guaranteePolicy");

    private static void bad(String message) {
        throw new Fee3Problem(HttpStatus.UNPROCESSABLE_ENTITY,"FEE3_INVALID_DRAFT",message);
    }
    public static void check(DraftRequest request, LocalDate bankToday, List<String> allowedKinds) {
        if (request == null || request.item() == null || request.version() == null || request.scope() == null)
            bad("شناسنامه، نسخه و دامنه اصلی باید ارائه شوند.");
        ItemDraft item = request.item();
        VersionDraft version = request.version();
        require(item.title(),250,"عنوان فارسی");
        require(item.module(),30,"بخش بانکی");
        require(item.operation(),60,"عملیات بانکی");
        require(item.ownerDomain(),30,"واحد مسئول");
        if (!AMOUNT_KINDS.contains(item.amountKind()) || !allowedKinds.contains(item.amountKind()))
            bad("ماهیت مبلغ برای عملیات انتخابی در مرجع فعال Oracle مجاز نیست.");
        maxLength(item.variant(),80,"گونه خدمت");
        maxLength(item.description(),700,"شرح");
        if (version.validFrom() == null || version.validFrom().isBefore(bankToday))
            bad("تاریخ شروع اعتبار نمی‌تواند قبل از تاریخ جاری معتبر بانک باشد.");
        if (version.validTo() != null && version.validTo().isBefore(version.validFrom()))
            bad("پایان اعتبار نباید قبل از شروع اعتبار باشد.");
        if (!ORIGINS.contains(version.origin())) bad("منشأ تعرفه معتبر نیست.");
        if (!METHODS.contains(version.method())) bad("روش محاسبه باید از گزینه‌های معتبر انتخاب شود.");
        require(version.taxMode(),24,"نوع مالیات");
        require(version.rounding(),24,"روش گردکردن");
        maxLength(version.changeReason(),500,"دلیل تغییر");
        ScopeDraft scope = request.scope();
        if (scope.channels() == null || scope.channels().isEmpty() ||
            scope.customers() == null || scope.customers().isEmpty()) bad("کانال و نوع مشتری در دامنه الزامی است.");
        require(scope.currency(),3,"ارز دامنه");
        require(version.serviceCurrency(),3,"ارز عملیات");
        require(version.feeCurrency(),3,"ارز کارمزد");
        if(!version.serviceCurrency().matches("[A-Z]{3}") ||
           !scope.currency().equals(version.serviceCurrency()))
            bad("ارز حساب/عملیات باید با ارز دامنه ذخیره‌شده یکسان باشد.");
        // FCE monetary columns are suffixed _IRR; other output currencies require a reviewed physical model.
        if(!"IRR".equals(version.feeCurrency()))
            bad("مبالغ کارمزد در مدل فیزیکی فعلی فقط ریالی هستند؛ ارز غیرریالی هنوز قابل ثبت نیست.");
        if(version.uiContext()==null)bad("قرارداد شرایط کسب‌وکاری باید به صورت ساخت‌یافته ارسال شود.");
        for(var key:version.uiContext().keySet())if(!UI_CONTEXT_KEYS.contains(key))
            bad("کلید ناشناخته در قرارداد شرایط کسب‌وکاری: "+key);
        if(version.financialPolicy()!=null && version.financialPolicy().containsKey("_fee3UiV1"))
            bad("فضای اختصاصی قرارداد UI فقط توسط سرور تولید می‌شود.");
        checkFinancialDates(version.financialPolicy(),bankToday);
        checkConditionalContext(item,version.uiContext());
        if (!scope.currency().matches("[A-Z]{3}")) bad("ارز دامنه باید کد ISO سه‌حرفی باشد.");
        checkCodes(scope.channels(), "کانال");
        checkCodes(scope.customers(), "نوع مشتری");
        checkCodes(scope.groups(), "گروه مشتری");
        checkCodes(scope.products(), "محصول");
        checkCodes(scope.contracts(), "عقد");
        if (!productScopeAllowed(item) && scope.products()!=null && !scope.products().isEmpty()
                && !scope.products().equals(List.of("ALL")))
            bad("انتخاب محصول برای این خدمت مجاز نیست (مانند دسته‌چک یا ارزیابی وثیقه).");
        List<RuleDraft> rules = request.rules() == null ? List.of() : request.rules();
        if (rules.size()>100) bad("تعداد قواعد یک پیش‌نویس از سقف مجاز بیشتر است.");
        Set<Integer> uniqueOrder = new HashSet<>();
        for (RuleDraft rule: rules) {
            if (rule == null || rule.order()<1 || !uniqueOrder.add(rule.order())) bad("ترتیب قواعد باید مثبت و یکتا باشد.");
            if (!METHODS.contains(rule.method())) bad("روش قاعده معتبر نیست.");
            if (!RATES.contains(rule.rateSource())) bad("منبع نرخ باید RULE یا REQUEST باشد.");
            if ("REQUEST".equals(rule.rateSource()) && rule.rate()!=null)
                bad("برای نرخ قراردادی/درخواستی نباید نرخ مصوب ثابت در قاعده ذخیره شود.");
            nonnegative(rule.rate(),"نرخ");nonnegative(rule.fixed(),"مبلغ ثابت");
            nonnegative(rule.min(),"حداقل");nonnegative(rule.max(),"حداکثر");
            if (rule.min()!=null && rule.max()!=null && rule.min().compareTo(rule.max())>0)
                bad("کف کارمزد از سقف آن بیشتر است.");
            if (rule.coverage()!=null && rule.coverage().compareTo(BigDecimal.ZERO)<=0)
                bad("ضریب پوشش وثیقه باید مثبت باشد.");
            maxLength(rule.base(),40,"مبنای مبلغ");maxLength(rule.collateral(),40,"وثیقه");
            maxLength(rule.formulaCode(),60,"کد فرمول");
        }
        List<TestDraft> tests = request.tests()==null ? List.of() : request.tests();
        if (tests.size()>50) bad("تعداد موارد آزمون بیش از حد مجاز است.");
        for (TestDraft t:tests) {
            if(t==null)bad("سناریوی آزمون خالی است.");
            require(t.title(),200,"عنوان آزمون");
            nonnegative(t.expected(),"مبلغ مورد انتظار آزمون");
        }
    }

    private static void checkConditionalContext(ItemDraft item, java.util.Map<String,Object> context) {
        if(context==null)return;
        Object policy=context.get("guaranteePolicy");
        if(policy!=null) {
            if(!("GUARANTEE".equals(item.module())&&"GUARANTEE_ISSUE".equals(item.operation())
                    &&"GUARANTEE_FEE".equals(item.amountKind())))
                bad("قواعد سبد وثایق فقط برای صدور ضمانت‌نامه قابل تعریف است.");
            if(!(policy instanceof java.util.Map<?,?>))bad("قواعد وثیقه باید ساخت‌یافته باشند.");
            java.util.Map<?,?> p=(java.util.Map<?,?>)policy;
            if("MIXED_PORTFOLIO".equals(p.get("collateralType"))) {
                if(!(p.get("portfolioRules") instanceof List<?>))bad("قواعد سبد وثایق نامعتبر است.");
                List<?> rules=(List<?>)p.get("portfolioRules");
                if(rules.size()>50)bad("تعداد قواعد وثایق نباید بیش از ۵۰ باشد.");
                Set<String> types=new HashSet<>();
                for(Object row:rules) {
                    if(!(row instanceof java.util.Map<?,?>))bad("ردیف وثیقه باید ساخت‌یافته باشد.");
                    java.util.Map<?,?> r=(java.util.Map<?,?>)row;
                    String code=String.valueOf(r.get("collateralType"));
                    if(code.isBlank()||!types.add(code))bad("نوع وثیقه خالی یا تکراری مجاز نیست.");
                    String source=String.valueOf(r.get("rateSource"));
                    if(!Set.of("CONFIG","REQUEST","EXEMPT").contains(source))bad("منبع نرخ وثیقه نامعتبر است.");
                    Object coverage=r.get("coveragePercent");
                    if(coverage!=null && !positiveRange(coverage,1000))bad("درصد پوشش وثیقه معتبر نیست.");
                    Object rate=r.get("ratePercent");
                    if("CONFIG".equals(source)&&rate!=null&&!nonnegativeRange(rate,1000))
                        bad("نرخ تعریف‌شده برای وثیقه نامعتبر است.");
                    if(!"CONFIG".equals(source)&&rate!=null)
                        bad("نرخ ثابت فقط در حالت CONFIG مجاز است.");
                }
            }
        }
    }
    private static boolean positiveRange(Object value,int maximum) {
        try{BigDecimal n=new BigDecimal(value.toString());return n.compareTo(BigDecimal.ZERO)>0&&
            n.compareTo(BigDecimal.valueOf(maximum))<=0;}
        catch(Exception ex){return false;}
    }
    private static boolean nonnegativeRange(Object value,int maximum) {
        try{BigDecimal n=new BigDecimal(value.toString());return n.compareTo(BigDecimal.ZERO)>=0&&
            n.compareTo(BigDecimal.valueOf(maximum))<=0;}
        catch(Exception ex){return false;}
    }

    @SuppressWarnings("unchecked")
    private static void checkFinancialDates(java.util.Map<String,Object> policy, LocalDate bankToday) {
        if(policy==null)return;
        for(String collection:List.of("deals","discountGroups")) {
            Object rows=policy.get(collection);
            if(rows==null)continue;
            if(!(rows instanceof List<?>))bad("ساختار توافق و تخفیف گروهی نامعتبر است.");
            List<?> list=(List<?>)rows;
            if(list.size()>100)bad("تعداد ردیف‌های توافق و تخفیف بیش از سقف است.");
            for(Object entry:list) {
                if(!(entry instanceof java.util.Map<?,?>))bad("ردیف مالی باید ساخت‌یافته باشد.");
                java.util.Map<?,?> map=(java.util.Map<?,?>)entry;
                Object from=map.get("from"),to=map.get("to");
                try {
                    LocalDate start=LocalDate.parse(String.valueOf(from));
                    if(start.isBefore(bankToday))bad("تاریخ شروع توافق یا تخفیف نمی‌تواند پیش از روز معتبر بانک باشد.");
                    if(to!=null&&!String.valueOf(to).isBlank() && LocalDate.parse(String.valueOf(to)).isBefore(start))
                        bad("تاریخ پایان توافق یا تخفیف از تاریخ شروع کمتر است.");
                }catch(java.time.format.DateTimeParseException e){bad("تاریخ توافق یا تخفیف معتبر نیست.");}
            }
        }
    }
    public static boolean productScopeAllowed(ItemDraft i) {
        return ("DEPOSIT".equals(i.module()) && Set.of("ACCOUNT_OPENING","ACCOUNT_CLOSING","ACCOUNT_MAINTENANCE").contains(i.operation()))
            || ("LOAN".equals(i.module()) && Set.of("LOAN_SERVICE","LOAN_COMMITMENT").contains(i.operation())
                && "SERVICE_FEE".equals(i.amountKind()));
    }
    private static void checkCodes(List<String> values, String title) {
        if(values==null)return;
        if(values.size()>150 || values.stream().anyMatch(x->x==null||x.isBlank()||x.length()>80)) bad("فهرست "+title+" نامعتبر است.");
        if(values.size()>1 && values.contains("ALL")) bad("گزینه همه موارد نباید همزمان با مقادیر مشخص "+title+" انتخاب شود.");
        if(new HashSet<>(values).size()!=values.size())bad("گزینه‌های تکراری "+title+" مجاز نیست.");
    }
    private static void nonnegative(BigDecimal value,String title) {if(value!=null&&value.signum()<0)bad(title+" منفی مجاز نیست.");}
    private static void require(String value,int maximum,String title) {if(value==null||value.isBlank()||value.length()>maximum)bad(title+" باید تکمیل شود و طول مجاز را رعایت کند.");}
    private static void maxLength(String value,int maximum,String title) {if(value!=null&&value.length()>maximum)bad(title+" از طول مجاز بیشتر است.");}
}
