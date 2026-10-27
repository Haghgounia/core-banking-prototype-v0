#!/usr/bin/env python3
"""FEE3 R8 UI source contract gate (stdlib, offline, non-mutating).
Not an Angular AOT build and not a bank calculation certification.
"""
from pathlib import Path
from html.parser import HTMLParser
import re

root=Path(__file__).resolve().parents[2]
f=root/'frontend/src/app/features/fee3'
read=lambda name:(f/name).read_text(encoding='utf-8')
html=read('fee3-workspace.component.html')
ts=read('fee3-workspace.component.ts')
pol=read('fee3-form-policies.ts')
cv=read('fee3-money-input.directive.ts')
validation=read('fee3-validation.ts')
state=read('fee3-state.service.ts')
checks=[]
def ok(text,condition):
    if not condition:raise AssertionError('FAIL '+text)
    checks.append(text)

ok('R7 reference retained', 'فرمول‌ساز قابل ویرایش' in html and 'آزمون و جمع‌بندی' in ts)
ok('service-specific scope in template', '@if(allowsProductScope()&&!managedLoanProducts())' in html)
ok('service-specific scope in component', 'fee3AllowsProductScope(this.editingItem)' in ts)
ok('checks scope capability, not only bank module', "item.module==='DEPOSIT'&&deposit.has(item.operation)" in pol)
for c in ['CHEQUEBOOK_ISSUE','CHEQUEBOOK_REQUEST','COLLATERAL_VALUATION']:
    ok('unrelated product scope cannot be listed: '+c, c not in pol.split('const deposit=',1)[1].split('return (item.module',1)[0] and c not in pol.split('const loan=',1)[1].split('return (item.module',1)[0])
ok('explicit allowed deposit scope', "'ACCOUNT_OPENING','ACCOUNT_CLOSING','ACCOUNT_MAINTENANCE'" in pol)
ok('explicit allowed loan scope', "'LOAN_SERVICE','LOAN_COMMITMENT'" in pol)
ok('no scope display for other service', 'if(!fee3AllowsProductScope(item))return []' in pol)
ok('no unrelated product codes', 'fee3RelevantProducts(item).map(x=>x.code)' in validation)
ok('reset prior product selections on changed operation', 'this.resetProductScope();' in ts)
ok('reset prior product selections on changed amount kind', 'changeAmountKind():void{this.resetProductScope()' in ts)
ok('single/multiple selectors remain supported for genuine product services', 'productScope()===\'ONE\'' in html and 'productScope()===\'MULTI\'' in html)

ok('new draft takes system date', 'item.versions[0].from=fee3TodayISO()' in state)
ok('cloned version takes system date', 'copy.from=fee3TodayISO()' in state)
ok('local date, not UTC date string', 'getFullYear()' in pol and 'getMonth()+1' in pol and 'getDate()' in pol)
ok('date chooser rejects prior dates', '[minDate]="todayDate()"' in html)
ok('validity starts bound to setter', 'setValidityStart($event)' in html)
ok('validity start rejects before today', 'fee3EffectiveStartAllowed(value)' in ts)
ok('historical seed date not silently rewritten', "item.code.startsWith('DEMO-')&&version.status==='DRAFT'&&!version.from" in ts)
ok('draft save rejects backdated version', "if(!fee3EffectiveStartAllowed(v.from))" in ts)
ok('review validation rejects backdated date', "version.status==='DRAFT'&&!fee3EffectiveStartAllowed(version.from)" in validation)
ok('discount effective dates use shared picker', html.count('setEffectiveRowStart(row,$event)')==2)
ok('discount row defaults to today', "else if(key==='discountGroups')row=" in ts and 'from:fee3TodayISO()' in ts)
ok('deal row defaults to today', "else if(key==='deals')row=" in ts and 'from:fee3TodayISO()' in ts)
ok('discount/deal save rejects past dates', "issue.stage===3&&issue.text.startsWith('تاریخ شروع اعتبار ردیف')" in ts)
ok('end date bounded by validity start', '[minDate]="ver.from || todayDate()"' in html)
ok('effective group dates labelled Persian', 'از تاریخ میلادی' not in html)

ok('money CVA imported', "import {Fee3MoneyInputDirective}" in ts and 'PersianDateInputComponent,Fee3MoneyInputDirective' in ts)
ok('money is model number, not formatted text', 'this.change(number)' in cv and 'fee3MoneyNumber(before)' in cv)
ok('money groups while editing', 'this.input.value=formatted' in cv and 'this.input.setSelectionRange' in cv)
ok('conditional money/percentage distinction', "value!==false&&value!=='false'" in cv and '[fee3Money]="row[' in html)
ok('persian/arabic digits recognized', '[۰-۹]' in pol and '[٠-٩]' in pol)
ok('no custom money parsing via eval', 'eval(' not in pol and 'new Function(' not in pol)
monetary=[m.group(0) for m in re.finditer(r'<input\b[^>]*>',html) if 'fee3Money' in m.group(0)]
ok('monetary form coverage >= 30 fields', len(monetary)>=30)
ok('monetary controls store numeric input through ngModel', all('ngModel' in inp and 'type="text"' in inp for inp in monetary))
for label in ['حداقل مبلغ خروجی','حداکثر مبلغ خروجی','بخش ثابت مبلغ','بهای هر واحد مصرف','مبلغ ثابت فرمول','کف کارمزد کل سبد','سقف کارمزد کل سبد','مبلغ هر واحد','حداقل مبلغ خالص بعد از تخفیف']:
    idx=html.index('<mat-label>'+label)
    fragment=html[idx:idx+320]
    ok('monetary labelled field: '+label, 'fee3Money' in fragment)
ok('quick trial money grouped', 'this.isMonetaryFormulaInput(name)?fee3MoneyText(value)' in ts)
ok('wizard trial money grouped', 'fee3IsMoneyTest(name)?fee3MoneyText(value)' in ts)
ok('percentage input not currency formatted', bool(re.search(r'<input[^>]*type="number"[^>]*\[\(ngModel\)\]="rule.rate"',html)))

ok('loan fee type filters formula groups', "if(kind==='FINANCING_PROFIT')return ['loan']" in pol)
ok('delay penalty filters formula groups', "if(kind==='LATE_DAMAGE')return ['penalty']" in pol)
ok('general fee type receives common only', "return ['common']" in pol)
ok('template filtered group loop', '@for(group of allowedFormulaGroups();track group.code)' in html)
ok('template filtered preset loop', '@for(preset of allowedFormulaPresets(group.code);track preset.id)' in html)
ok('formula handler rejects incompatible selected preset', '!this.allowedFormulaPresets(preset.group).some(p=>p.id===id)' in ts)
ok('formula validator rejects unrelated signature', 'fee3AllowedFormulaGroups(item.amountKind).includes(preset.group)' in validation)
ok('no Oracle/HTTP added to feature', 'HttpClient' not in ts and 'fetch(' not in cv)

class Parser(HTMLParser):
    void={'input','br','img','hr','meta','link','source','area','wbr','col'}
    def __init__(self):super().__init__();self.stack=[]
    def handle_starttag(self,tag,attrs):
        if tag not in self.void:self.stack.append(tag)
    def handle_endtag(self,tag):
        assert self.stack and self.stack.pop()==tag,'HTML tag mismatch: '+tag
p=Parser();p.feed(html)
ok('HTML tags balanced',not p.stack)
loops=0
for match in re.finditer(r'@for\(',html):
    pos=match.end();depth=1
    while depth and pos<len(html):
        if html[pos]=='(':depth+=1
        if html[pos]==')':depth-=1
        pos+=1
    assert depth==0 and re.search(r';\s*track\s+',html[match.end():pos-1]),'untracked @for at '+str(html.count('\n',0,match.start())+1)
    loops+=1
ok('Angular for loops still tracked',loops>=80)
print('FEE3_R8_STATIC_PASS='+str(len(checks))+' FAIL=0')
print('FEE3_R8_MONEY_INPUTS='+str(len(monetary))+' TRACKED_LOOPS='+str(loops))
print('SCOPE=Offline UI source checks only. No Angular AOT/browser/Oracle claim.')
