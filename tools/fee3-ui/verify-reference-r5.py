#!/usr/bin/env python3
"""FCE V13.8.84 -> Angular FEE3 R5 offline-UI contract regression.
Static checks only; NEVER claims to replace ng build/browser/financial engine.
"""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[2]
f=root/'frontend/src/app/features/fee3'
h=(f/'fee3-workspace.component.html').read_text(encoding='utf8')
t=(f/'fee3-workspace.component.ts').read_text(encoding='utf8')
m=(f/'fee3.models.ts').read_text(encoding='utf8')
b=(f/'fee3-reference-behavior.ts').read_text(encoding='utf8')
c=(f/'fee3-offline-calculator.ts').read_text(encoding='utf8')
s=(f/'fee3-state.service.ts').read_text(encoding='utf8')
css=(f/'fee3-workspace.component.scss').read_text(encoding='utf8')
checks=[]
def ck(name,result):
 if not result:raise AssertionError('FAIL: '+name)
 checks.append(name)
cases=[h.split(f'@case({i})'+'{',1)[1].split(f'@case({i+1})'+'{',1)[0] for i in range(7)]
cases.append(h.split('@case(7){',1)[1].split('          </section>',1)[0])
ck('01 7 visible stages, 8 internal cases',len(cases)==8 and 'readonly visibleStageIndices=[0,1,2,3,4,6,7]' in t)
ck('02 no 8-step display', 'گام {{visibleStageNumber(stage)}} از ۷' in h)
ck('03 5 jumps directly to source test', 'this.stage===4?6' in t)
ck('04 test to final gated by review', "if(next===7&&this.editingVersion?.status==='DRAFT')next=6" in t)
ck('05 conditional IPMT model', '@if(modelPreset(item); as preset)' in cases[2] and 'modelPreset' in t)
ck('06 preset summary', 'مشخصات محاسباتی مدل منتخب' in cases[2])
ck('07 locked model and formula', 'مدل و فرمول قفل هستند' in cases[2])
for label in ['مدل محاسباتی','روش اجرایی موتور','ارز مبلغ خروجی','ورودی‌های لازم قرارداد/API']:
 ck('summary: '+label,label in cases[2])
ck('12 reference formula and assumptions','مشاهده فرمول ثابت و مفروضات' in cases[2] and 'referenceFormulaExpression(item)' in cases[2])
ck('13 no duplicate free formula for preset',"ver.method==='FORMULA'&&!modelPreset(item)" in cases[2])
ck('14 review after contracts change','loanFormulaContextReviewRequired=true' in t and 'بازبینی انطباق فرمول با عقود جدید لازم است' in cases[2])
ck('15 explicit review confirmation','confirmPresetContractReview()' in cases[2] and 'loanFormulaContextReviewRequired=false' in t)
ck('16 read-only preset day count','dayBasisLabel(item)' in cases[2] and 'مبنای مدت و روزشمار' in cases[2])
ck('17 no second method builder with preset','@if(ver.method && !modelPreset(item) && !(isFee3Guarantee(item)&&guaranteePortfolio(item)))' in cases[2])
ck('18 quick test visible even before saved','آزمون سریع محاسبه' in cases[6] and '@if(!testDraftSaved())' in cases[6])
for label in ['۱. ذخیره پیش‌نویس','آزمون مبلغ','۳. ارسال برای بررسی','محاسبه مبلغ نمونه','ارسال برای بررسی و رفتن به گام ۷']:
 ck('test flow '+label,label in cases[6])
ck('24 summary before test','مرور خلاصه مشخصات پیش از آزمون' in cases[6])
ck('25 template test formula driven', '@for(field of testInputFields();track field.name)' in cases[6])
ck('26 test contract is conditional','@if(modelPreset(item) && businessContext(item).loanContractTypes?.length)' in cases[6])
ck('27 event shown read-only','loanEventTitle(item)' in cases[6])
ck('28 test button gated on saved draft','[disabled]="!testDraftSaved()||!editable()||!ver.method"' in cases[6])
ck('29 no repeatable test cards','addTest()' not in cases[6] and 'removeTest(' not in cases[6])
ck('30 test result only after actual calculation', '@if(testOutcome)' in cases[6] and 'simulateFee3(' in t)
ck('31 review send requires stored successful proof','canSendReview()' in cases[6] and 'lastTestPassed()' in t and "v.tests.some(t=>t.status==='PASS'" in t)
ck('32 review step cannot approve bank tariff','setLocalReviewStatus(' in t and 'اصلاً' not in t)
ck('33 distinct demo lifecycle', 'تکمیل و فعال‌سازی دمو' in cases[6] and 'offlineDemoActivated' in m and 'activateDemo()' in cases[6])
ck('34 demo not real bank activation', 'مجوز انتشار در سامانه واقعی بانک صادر نمی‌کند' in cases[6])
ck('35 final stage gated','@case(7)' in h and 'بازگشت به گام ۶' in cases[7])
ck('36 no real backend or storage','HttpClient' not in t and 'localStorage' not in t and 'sessionStorage' not in t)
ck('37 in-memory state only', 'readonly items=signal<' in s and 'setLocalReviewStatus' in s)
ck('38 preset formulas from seed','formulaExpression(' in t and 'FEE3_SEED_FORMULAS' in t)
ck('39 guarded formula calculator','safeReferenceFormula' in c and not re.search(r'\beval\s*\(',c.split('*/',1)[-1]) and 'new Function' not in c)
ck('40 PMT stable numeric math','-Math.expm1(-x.months*Math.log1p(x.r))' in c)
ck('41 IPMT stable numeric math','-Math.expm1(-(x.months-period+1)*Math.log1p(x.r))' in c)
ck('42 invalid period rejected','Number.isInteger(period)' in c and 'period>x.months' in c)
ck('43 cross-currency not faked','item.serviceCurrency!==item.feeCurrency' in c)
ck('44 unknown formula rejects','default:throw new Error' in c)
ck('45 no external HTTP','fetch(' not in c and 'http://' not in c and 'https://' not in c)
ck('46 theme tokens for preset section','var(--app-border)' in css and '.fee3-preset-reference' in css)
ck('47 test responsive','@media(max-width:600px)' in css and '.fee3-test-input-grid' in css)
ck('48 source-looking save/test/demo panels all present',all(x in css for x in ['.fee3-test-actions','.fee3-demo-panel','.fee3-test-result']))
# Angular 17+ @for needs mandatory track; syntax-level guard inherited from HF1.
count=0
for match in re.finditer(r'@for\(',h):
 pos=match.end();depth=1
 while depth and pos<len(h):
  if h[pos]=='(':depth+=1
  elif h[pos]==')':depth-=1
  pos+=1
 decl=h[match.end():pos-1]
 ck('for track #'+str(count+1),';track ' in decl or '; track ' in decl)
 count+=1
ck('49 nonempty for loop coverage',count>=80)
print('FEE3_R5_REFERENCE_UI_STATIC_PASS='+str(len(checks))+' FAIL=0')
print('FEE3_R5_FOR_LOOPS='+str(count))
print('SCOPE=FCE V13.8.84 preset math + 7-stage review/test/demo; NO ANGULAR NG BUILD OR BROWSER CLAIM')
