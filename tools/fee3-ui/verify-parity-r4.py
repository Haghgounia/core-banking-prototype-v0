#!/usr/bin/env python3
"""FEE3 R4/R5 UI parity static gate, Python 3 stdlib only.

This detects missing/reference-only controls and template structure regressions.
It does NOT replace Angular compiler or browser UAT.
Run from project root: python tools/fee3-ui/verify-parity-r4.py
"""
from pathlib import Path
from html.parser import HTMLParser
import re
import sys
root=Path(__file__).resolve().parents[2]
f=root/'frontend/src/app/features/fee3'
html=(f/'fee3-workspace.component.html').read_text(encoding='utf-8')
ts=(f/'fee3-workspace.component.ts').read_text(encoding='utf-8')
validation=(f/'fee3-validation.ts').read_text(encoding='utf-8')
catalog=(f/'fee3-reference-catalog.ts').read_text(encoding='utf-8')
model=(f/'fee3.models.ts').read_text(encoding='utf-8')
cases=[html.split(f'@case({i})'+'{',1)[1].split(f'@case({i+1})'+'{',1)[0] for i in range(7)]
cases.append(html.split('@case(7){',1)[1].split('          </section>',1)[0])
checks=[]
def must(ok,msg):
 if not ok:raise AssertionError(msg)
 checks.append(msg)
class HTMLChecker(HTMLParser):
 void={'input','area','base','br','col','embed','hr','img','link','meta','param','source','track','wbr'}
 def __init__(self):super().__init__();self.stack=[]
 def handle_starttag(self,tag,attrs):
  if tag not in self.void:self.stack.append(tag)
 def handle_endtag(self,tag):
  assert self.stack and self.stack.pop()==tag,f'HTML invalid closing tag: {tag}'
c=HTMLChecker();c.feed(html);must(not c.stack,'All HTML element start/end tags are balanced')
# Angular 17+ required block syntax: every @for must specify a track expression.
for_loop_count=0
cursor=0
while True:
 start=html.find('@for(',cursor)
 if start<0:break
 pos=start+5
 depth=1
 while pos<len(html) and depth:
  if html[pos]=='(':depth+=1
  elif html[pos]==')':depth-=1
  pos+=1
 if depth!=0:raise AssertionError('Angular @for declaration must have a closing parenthesis')
 declaration=html[start+5:pos-1]
 line_no=html.count('\n',0,start)+1
 if not bool(re.search(r';\s*track\s+[^;\s]',declaration)):raise AssertionError(f'Angular @for loop requires track at line {line_no}')
 for_loop_count+=1
 cursor=pos
must(for_loop_count>0,'Angular @for block coverage is non-empty and all loops have track')

must(len(cases)==8,'Eight steps present')
for label in ['مدل مستقل محاسباتی','عقود مشمول و رویداد قراردادی','بستر انتقال وجه','ساختار درخواست انتقال','زمان اجرای درخواست','سامانه انجام حواله']:
 must(label in cases[0],f'Conditional identity: {label}')
sc=cases[1]
for txt in ['اولویت دامنه','حذف دامنه','افزودن دامنه','ارز حساب / عملیات مشمول','ارز کارمزد']:
 must(txt not in sc,'Scope excludes extra field/control: '+txt)
must(sc.count('<mat-select multiple')==4,'Three scope dimensions and one conditional multi-product picker')
for txt in ['نوع مشتری','گروه مشتری','کانال ثبت درخواست','دامنه محصولات','محصول مشمول','محصولات مشمول']:
 must(txt in sc,'Reference scope field: '+txt)
must("['DEPOSIT','LOAN'].includes(item.module)" in sc,'Product scope only for deposit and loan')
must('!managedLoanProducts()' in sc and 'managedLoanProducts()' in sc,'Managed loan products are derived')
must('scope.priority' not in sc,'Scope priority is not editable')
must('scope.currency' not in sc,'Scope currency is not editable')
must('ver.scopes' not in sc,'UI scope is a single set, not repeatable cards')
must('normalizeScope(scope' in sc,'Multi-pickers normalize ALL/exclusive selections')
calc=cases[2]
for k in ['FIXED','PERCENT','MIXED','USAGE','DAILY','BRACKET','PROGRESSIVE','MATRIX','FORMULA']:
 must(k in ts,'Calculation method included: '+k)
for label in ['روش‌های تخصصی دیگر (بازه‌ای، پلکانی، ماتریسی و فرمول)','مبلغ ثابت هر بار ارائه خدمت','نرخ کارمزد (درصد)','منبع نرخ','بخش ثابت مبلغ','بخش درصدی','بهای هر واحد مصرف','تعداد واحد رایگان','نرخ سالانه روزشمار (درصد)','مبنای شمارش روز','تعرفه وابسته به ساعت (اختیاری)']:
 must(label in calc,'Reference calculation control: '+label)
must('simpleMethods' in calc and 'advancedMethods' in calc,'Methods grouped into 5 common and 4 specialized')
must('timeEnabled' in calc and 'timeRules()' in calc,'Timed tariff is conditional and editable')
finance=cases[3]
for label in ['ماهیت وجه','مالک وجه','زمان تعلق','قابلیت برگشت','سیاست بخشودگی','وضعیت مالیات','روش گردکردن',
 'محاسبات چندمؤلفه‌ای، تخفیف، توافق اختصاصی و تسهیم (اختیاری)','حداقل پس از تخفیف و تخفیف گروهی سازمان‌ها (اختیاری)',
 'کد جزء','ماهیت مالی','وضعیت مالیاتی این جزء','انباشت','مجوز','ذی‌نفع باقیمانده گردکردن']:
 must(label in finance,'Reference finance field: '+label)
must("ver.taxMode==='TAXABLE'" in finance,'Tax rate only when TAXABLE')
must("['PARTIAL','FULL'].includes(policyValue(ver,'waiver'))" in finance,'Maximum waiver only when relevant')
validity=cases[4]
must('تاریخ شروع اعتبار' in validity and 'تاریخ پایان اعتبار (اختیاری)' in validity,'Validity has both reference dates')
for text in ['مرجع مصوب / مستند','دلیل ایجاد یا تغییر نسخه','اولویت دامنه']:
 must(text not in validity,'Validity excludes R3 additional field: '+text)
must('app-persian-date-input' in validity,'Uses shared Persian date component')
review=cases[5]
must('بازبینی' in review,'Review stage retained for prior-state checking')
test=cases[6]
# R4 originally had a screenshot-only test form. R5 implements the same FCE quick-test
# with guarded in-memory arithmetic and 7 visible stages; accept either contract.
if 'runTest()' in test:
 for label in ['آزمون مبلغ و ارسال کارمزد برای بررسی','مرور خلاصه مشخصات پیش از آزمون',
  'پیش‌نویس ذخیره شده','آزمون مبلغ','ارسال برای بررسی',
  'ذخیره پیش‌نویس','آزمون سریع محاسبه','محاسبه مبلغ نمونه',
  'ارسال برای بررسی و رفتن به گام ۷','فعال‌سازی تعرفه در محیط آزمایشی']:
  must(label in test,'R5 source test stage control: '+label)
 must('testInputFields()' in test,'Dynamic formula input fields follow FCE model')
 must('testDraftSaved()' in test and '!testDraftSaved()' in test,'Test only enabled after draft save')
 must('canSendReview()' in test,'Review depends on exact current successful test')
 must('lastTestPassed()' in test,'Saved successful test proof bound to version')
 must('activateDemo()' in test,'Demo activation is a separate restricted action')
 must('addTest()' not in test and 'removeTest(' not in test,'No invented repeatable test cards')
 must('visibleStageIndices' in html and 'visibleStageNumber' in html,'7 visible steps')
else:
 for label in ['آزمون مبلغ و ارسال کارمزد برای بررسی','مرور خلاصه مشخصات پیش از آزمون','پیش‌نویس ذخیره شده','آزمون مبلغ','ارسال برای بررسی','نوع مشتری در آزمون','گروه مشتری در آزمون','کانال در آزمون','مبلغ مبنای آزمون','تعداد مصرف']:
  must(label in test,'Source test stage control: '+label)
 must('addTest()' not in test and 'removeTest(' not in test,'No invented repeatable test cards')
 must("ver.method==='USAGE'" in test and "['PERCENT','MIXED','BRACKET','PROGRESSIVE','MATRIX'].includes(ver.method)" in test,'Conditional test inputs by method')
 must('disabled><mat-icon>calculate' in test,'No fabricated calculation results')
pub=cases[7]
must('بررسی نهایی و انتشار' in pub and 'ver.status' in pub,'Publication gated and source-shaped')
must('goStage(5)' not in pub and 'readiness-list' not in pub,'Additional R3 workflow panels removed from publication step')
must('this.stage===4?6' in ts,'Step 6 review integrated with test navigation')
must('next===7' in ts,'Publication gated for drafts')
must('REF_ALLOWEDCHANNELS' in ts and 'REF_CUSTOMERS' in ts and 'REF_ROUNDINGS' in ts,'Display option catalog imported from executed source reference')
must('REF_ALLOWEDCHANNELS' in catalog and 'REF_PRODUCTS' in catalog,'Source reference catalog exists')
must('scope.priority' not in validation and 'scope.currency' not in validation,'Stage-2 validation excludes hidden extras')
must("{title:'دامنه',description:'مشتری، کانال و محصول'" in model,'Scope step does not advertise currency')
must(not re.search(r'\bHttpClient\b|\bfetch\s*\(|localStorage\.|sessionStorage\.',ts),'No network or persistent browser storage added')
print('FEE3_R4_PARITY_STATIC_OK='+str(len(checks)))
for idx,section in enumerate(['شناسنامه','دامنه','محاسبه','ضوابط مالی','اعتبار و گردش‌کار','بازبینی','آزمون','بررسی نهایی']):
 print(f'OK step {idx+1}: {section}')
print('NOTE: No Angular compiler or interactive browser test is claimed by this verifier.')
