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
