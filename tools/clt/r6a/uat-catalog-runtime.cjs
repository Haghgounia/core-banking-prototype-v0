// Generated for Node-only UAT smoke; source SHA256 0fb166333ef274fdff3d5d18e7513c842363de1c10f9deaa86f7ffdf05500c51
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CLT_UI_COVERAGE = exports.CLT_UAT_CASES = exports.CLT_UAT_STATUS_LABELS = exports.CLT_UAT_STATUSES = void 0;
exports.cltNormalizeUatStatus = cltNormalizeUatStatus;
exports.cltCleanUatNote = cltCleanUatNote;
exports.cltUatCounts = cltUatCounts;
exports.cltUatExport = cltUatExport;
exports.CLT_UAT_STATUSES = ['NOT_RUN', 'PASS', 'FAIL', 'BLOCKED'];
exports.CLT_UAT_STATUS_LABELS = {
    NOT_RUN: 'اجرانشده', PASS: 'قبول', FAIL: 'رد', BLOCKED: 'مسدود'
};
/** Precisely ten cases from the source UI Specification. The steps are compact test instructions. */
exports.CLT_UAT_CASES = [
    { id: 'UAT-01', title: 'استعلام مستقیم شماره انتظامی مجاز', precondition: 'شخص مجاز و وثیقه واقعی در شعبه مجاز موجود باشد.', steps: 'شماره معتبر را جست‌وجو و بخش ضامن، بیمه و آزادسازی را باز کنید.', expected: 'شناسنامه مجاز بازیابی شود و دسترسی مستقل به ضامن/بیمه/آزادسازی فراهم باشد.', bucs: ['BUC-13'], sucs: ['SUC-01'], uiTarget: 'direct', source: 'Collateral_UI_Specification_FA.html' },
    { id: 'UAT-02', title: 'ابطال انتخاب قبلی با شماره نامعتبر', precondition: 'ابتدا شناسنامه یک وثیقه مجاز باز شده باشد.', steps: 'شماره را به مقدار نامعتبر تغییر دهید و دوباره جست‌وجو را امتحان کنید.', expected: 'وثیقه قبلی بلافاصله از انتخاب خارج شود و عملیات روی آن ممکن نباشد.', bucs: ['BUC-13'], sucs: ['SUC-01'], uiTarget: 'direct', source: 'Collateral_UI_Specification_FA.html' },
    { id: 'UAT-03', title: 'تغییر ضریب پذیرش و کسری پوشش', precondition: 'سیاست مصوب و داده تخصیص قابل مقایسه فراهم باشد.', steps: 'تغییر مصوب ضریب را در محیط آزمون اعمال و وضعیت پوشش را بررسی کنید.', expected: 'کسری پوشش اعلام شود، تخصیص ثبت‌شده قبلی به‌صورت خودکار باطل نشود.', bucs: ['BUC-01', 'BUC-14'], sucs: ['SUC-16', 'SUC-17'], uiTarget: 'reference', source: 'Collateral_UI_Specification_FA.html' },
    { id: 'UAT-04', title: 'تعارض ضامن با بدهکار پرونده', precondition: 'ضامن و بدهکار یک شخص باشند.', steps: 'ضامن را مستقل ثبت/اعتبارسنجی و پیش‌بررسی تخصیص را اجرا کنید.', expected: 'ضامن به‌صورت مستقل قابل نگهداری باشد؛ تخصیص در حالت تعارض رد شود.', bucs: ['BUC-06', 'BUC-09'], sucs: ['SUC-07', 'SUC-08'], uiTarget: 'direct', source: 'Collateral_UI_Specification_FA.html' },
    { id: 'UAT-05', title: 'ارزیابی منقضی و تخصیص جدید', precondition: 'وثیقه با ارزیابی منقضی و تخصیص فعال قبلی در محیط تست.', steps: 'شواهد ارزیابی را باز کنید و درخواست افزایش تخصیص را پیش‌بررسی کنید.', expected: 'تخصیص/افزایش جدید مسدود و تخصیص قبلی حفظ شود.', bucs: ['BUC-08', 'BUC-09'], sucs: ['SUC-10', 'SUC-11'], uiTarget: 'direct', source: 'Collateral_UI_Specification_FA.html' },
    { id: 'UAT-06', title: 'معرفی مجدد دارایی آزادشده', precondition: 'دارایی آزادشده قطعی و شناسه پایدار دارایی موجود باشد.', steps: 'دارایی قبلاً آزادشده را دوباره معرفی کنید.', expected: 'شماره انتظامی جدید و پیوند به سابقه قبلی ثبت شود؛ شماره قبلی استفاده نشود.', bucs: ['BUC-12'], sucs: ['SUC-15'], uiTarget: 'introduction', source: 'Collateral_UI_Specification_FA.html' },
    { id: 'UAT-07', title: 'ممنوعیت اصلاح شناسنامه دارای وابستگی', precondition: 'برای وثیقه بیمه یا تخصیص موجود باشد.', steps: 'درخواست اصلاح شناسنامه را امتحان کنید.', expected: 'اصلاح مسدود و علت بازدارنده واضح نمایش داده شود.', bucs: ['BUC-05'], sucs: ['SUC-05', 'SUC-06'], uiTarget: 'direct', source: 'Collateral_UI_Specification_FA.html' },
    { id: 'UAT-08', title: 'محدودیت بازه اقلام شمارشی', precondition: 'نوع وثیقه شمارشی در محیط تست تنظیم شده باشد.', steps: 'بازه بیشتر از ۲۰ قلم یا هم‌پوشان را وارد کنید.', expected: 'هم‌پوشانی یا بازه بزرگ‌تر از ۲۰ با خطای همان ردیف رد شود.', bucs: ['BUC-04'], sucs: ['SUC-04'], uiTarget: 'introduction', source: 'Collateral_UI_Specification_FA.html' },
    { id: 'UAT-09', title: 'بیش از ۹ ضامن معتبر', precondition: 'حداقل موردنیاز ضامنین طبق سیاست مصوب مشخص باشد.', steps: 'بیش از ۹ ضامن معتبر را برای وثیقه آزمایشی بررسی کنید.', expected: 'سقف سراسری ۹تایی اعمال نشود؛ فقط حداقل معتبر طبق نوع کنترل شود.', bucs: ['BUC-06'], sucs: ['SUC-07'], uiTarget: 'direct', source: 'Collateral_UI_Specification_FA.html' },
    { id: 'UAT-10', title: 'نمای ستادی در عرض ۱۳۶۶ پیکسل', precondition: 'کاربر ستادی مجاز در مرورگر با عرض ۱۳۶۶ پیکسل.', steps: 'صفحه اطلاعات پایه و جدول‌های آن را باز و اسکرول کنید.', expected: 'عدم هم‌پوشانی متن و کنترل‌ها، ناوبری قابل دسترس و RTL مناسب.', bucs: ['BUC-01'], sucs: ['SUC-16'], uiTarget: 'reference', source: 'Collateral_UI_Specification_FA.html' }
];
/** Fifteen official navigational views, not fifteen completed banking screens. */
exports.CLT_UI_COVERAGE = [
    { key: 'list', title: 'فهرست وثایق', group: 'OPERATION', state: 'CONNECTED_COMPONENT', target: 'list', explanation: 'جست‌وجو/فهرست و اتصال شناسنامه؛ وابسته به دسترسی Oracle و Route' },
    { key: 'new', title: 'معرفی وثیقه', group: 'OPERATION', state: 'PARTIAL', target: 'introduction', explanation: 'پیش‌نویس و Preflight؛ ثبت نهایی غیرفعال' },
    { key: 'direct', title: 'دسترسی با شماره انتظامی', group: 'OPERATION', state: 'CONNECTED_COMPONENT', target: 'direct', explanation: 'بازیابی مجاز از طریق Registry؛ وابسته به Route و JWT' },
    { key: 'valuation', title: 'ارزیابی و تجدید ارزیابی', group: 'OPERATION', state: 'PARTIAL', target: 'direct', explanation: 'پنل سوابق و درخواست؛ نهایی‌سازی ارزیابی غیرفعال' },
    { key: 'insurance', title: 'بیمه و ضامنین', group: 'OPERATION', state: 'PARTIAL', target: 'direct', explanation: 'دو پنل مستقل در شناسنامه؛ ثبت قطعی غیرفعال' },
    { key: 'release', title: 'آزادسازی / خروج', group: 'OPERATION', state: 'PARTIAL', target: 'direct', explanation: 'شواهد R5C و Preflight؛ آزادسازی واقعی غیرفعال' },
    { key: 'history', title: 'سوابق و رویدادها', group: 'OPERATION', state: 'PARTIAL', target: 'direct', explanation: 'سوابق تخصیص و وضعیت یک وثیقه؛ فهرست رویداد سراسری ندارد' },
    { key: 'allocation', title: 'آزمون سرویس تخصیص (موقت)', group: 'TEST', state: 'PARTIAL', target: 'direct', explanation: 'Preflight بدون نوشتن؛ سرویس مصرف‌کننده از UI فراخوانی نمی‌شود' },
    { key: 'qa', title: 'کنترل کیفیت عملیاتی', group: 'TEST', state: 'CONNECTED_COMPONENT', target: 'uat', explanation: 'میزکار دستی R6A؛ نتایج نیازمند ثبت واقعی تحلیلگر هستند' },
    { key: 'dashboard', title: 'شاخص‌های کلان وثایق', group: 'REPORT', state: 'GAP', target: 'uat', explanation: 'تجمیع بانکی معتبر و API گزارش مستقل هنوز پیاده‌سازی نشده' },
    { key: 'recentEvents', title: 'رویدادهای اخیر', group: 'REPORT', state: 'GAP', target: 'uat', explanation: 'فید رویداد بین‌وثیقه‌ای با مجوز شعبه پیاده‌سازی نشده' },
    { key: 'capacityOverview', title: 'نمای سریع ظرفیت وثایق', group: 'REPORT', state: 'PARTIAL', target: 'direct', explanation: 'شواهد ظرفیت تک‌وثیقه موجود؛ داشبورد تجمیعی پیاده‌سازی نشده' },
    { key: 'hqRefs', title: 'تعاریف و اطلاعات پایه', group: 'HQ', state: 'CONNECTED_COMPONENT', target: 'reference', explanation: 'میزکار داده مرجع CLT؛ نیازمند ثبت Route' },
    { key: 'hqAudit', title: 'تاریخچه تغییرات مرجع', group: 'HQ', state: 'PARTIAL', target: 'reference', explanation: 'درخواست/تصمیم Reference Governance؛ گزارش مستقل Audit سراسری ندارد' },
    { key: 'hqDashboard', title: 'آمار اطلاعات پایه', group: 'HQ', state: 'GAP', target: 'reference', explanation: 'آمار تجمیعی ستادیِ متصل به Oracle هنوز موجود نیست' }
];
function cltNormalizeUatStatus(input) {
    return exports.CLT_UAT_STATUSES.includes(input) ? input : 'NOT_RUN';
}
function cltCleanUatNote(input) {
    // Exclude control characters; retain Persian Unicode; avoid collecting actual customer identifiers.
    return input.replace(/[\u0000-\u001f\u007f-\u009f]/g, ' ').trim().slice(0, 280);
}
function cltUatCounts(results) {
    const count = { NOT_RUN: 0, PASS: 0, FAIL: 0, BLOCKED: 0 };
    for (const row of exports.CLT_UAT_CASES)
        count[cltNormalizeUatStatus(results[row.id]?.status ?? 'NOT_RUN')]++;
    return count;
}
function cltUatExport(results, generatedAt) {
    const cases = exports.CLT_UAT_CASES.map(item => ({ id: item.id, bucs: item.bucs, sucs: item.sucs,
        status: cltNormalizeUatStatus(results[item.id]?.status ?? 'NOT_RUN'),
        note: cltCleanUatNote(results[item.id]?.note ?? '') }));
    const summary = cltUatCounts(results);
    return JSON.stringify({ format: 'CLT-R6A-UAT-EVIDENCE-1', roadmap: 'CLT-ROADMAP-R1.0',
        source: 'Collateral_UI_Specification_FA.html', generatedAt, localOnly: true, oracleAttestation: false,
        runtimeAttestation: false, analystApproval: false, summary, cases }, null, 2) + '\n';
}
