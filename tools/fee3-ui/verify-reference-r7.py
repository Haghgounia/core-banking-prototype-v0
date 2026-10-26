from pathlib import Path
from html.parser import HTMLParser
import re
root=Path(__file__).resolve().parents[2]/'frontend/src/app/features/fee3'
h=(root/'fee3-workspace.component.html').read_text(encoding='utf-8')
ts=(root/'fee3-workspace.component.ts').read_text(encoding='utf-8')
math=(root/'fee3-formula-editor.ts').read_text(encoding='utf-8')
calc=(root/'fee3-offline-calculator.ts').read_text(encoding='utf-8')
spec=(root/'fee3-specialized-reference.ts').read_text(encoding='utf-8')
checks={
 'editable formula header':'فرمول‌ساز قابل ویرایش' in h,
 'formula textarea ngModel':'(ngModelChange)="formulaChanged(rule,$event)"' in h,
 'editable formula token insertion':'formulaInsert(rule,formulaBox' in h,
 'formula preset selector':'formulaUsePreset(rule,preset.id)' in h,
 'annual rate source options':'منبع نرخ سالانه (درصد)' in h and 'نرخ قرارداد / API ماژول مصرف‌کننده' in h,
 'formula custom params':'پارامترهای اختصاصی فرمول' in h,
 'formula quick test':'آزمون سریع همین فرمول' in h and 'formulaQuickTest(rule)' in h,
 'formula quick sample':'formulaSample()' in h,
 'formula quick clear':'formulaQuickClear()' in h,
 'test output safe':'هنوز محاسبه‌ای اجرا نشده است.' in h,
 'no unsafe browser eval':'eval(' not in math and 'new Function(' not in math,
 'strict formula AST':'parseFee3Expression(source:string)' in math,
 'strict denied undeclared param':'باید در جدول پارامترهای اختصاصی تعریف شود' in math,
 'guarantee portfolio strict linear':'برای سبد ترکیبی وثایق فقط فرمول خطی' in spec,
 'guarantee single custom math':'raw=fee3ComputeExpression(expression,values)' in spec,
 'guarantee per type rate':'ruleForType.rateSource' in spec,
 'derived partnership gate':'loanProfitNeedsDerivedPeriod(item)' in h,
 'derived partnership dates':'loanProfitTest.periodStart' in h and 'loanProfitTest.periodEndExclusive' in h,
 'derived partnership literal hint':'ورود دستی «کسر سال» وجود ندارد' in h,
 'derived with jalali calculator':'jalaliContractFraction(this.loanProfitTest.periodStart' in ts,
 'derived contractual no manual':'loanProfitDerivedInputs()' in ts,
 'preflight rejection wrong months':'مدل برآورد روزشمار با مبنای ماهانه ناسازگار' in ts,
 'do not persist derived input':'delete requestInputs[\'yearFraction\'];' in ts,
 'realized monthly not derived':'this.calculationFormulaVariables(rule).includes(\'yearFraction\')' in ts,
 'without raw yearFraction input':'[ngModel]="testNumbers[\'yearFraction\']"' not in h,
 'no generic raw fraction':'name===\'yearFraction\'' in ts,
 'model warning result':'سود قطعی مشارکت از این آزمون قابل استنتاج نیست' in ts,
 'rate only if used':'names.includes(\'rate\')' in calc,
 'safe math custom':'raw=formula?safeReferenceFormula' in calc,
 'legacy R6 preserved':'qardDerivedInputs()' in ts and 'guaranteePortfolioPreview(item)' in h,
 'forms not linked to Oracle':'runTest():Promise<void>' in ts,
}
for name,passed in checks.items():print(('PASS' if passed else 'FAIL')+' | '+name)
assert all(checks.values()),'static check failed'
# Angular control flow @for must include track; comments are not counted.
loops=re.findall(r'@for\s*\(([^)]*)\)\s*\{',h)
for loop in loops:
 assert re.search(r';\s*track\s+',loop), 'track missing: '+loop
print('FEE3_R7_TRACKED_FOR_COUNT='+str(len(loops)))
class CheckMarkup(HTMLParser):
 def __init__(self):super().__init__(convert_charrefs=True);self.stack=[];self.errors=[]
 def handle_starttag(self,tag,attrs):
  if tag not in ('input','br','img','hr','meta','link','source','area','wbr','col'):
   self.stack.append(tag)
 def handle_startendtag(self,tag,attrs):pass
 def handle_endtag(self,tag):
  if tag not in self.stack: self.errors.append(f'unmatched closing </{tag}>');return
  if self.stack[-1]==tag:self.stack.pop();return
  self.errors.append(f'incorrect nesting expected </{self.stack[-1]}> got </{tag}>')
  while self.stack and self.stack[-1]!=tag:self.stack.pop()
  if self.stack:self.stack.pop()
parser=CheckMarkup();parser.feed(h)
assert not parser.errors,parser.errors[:20]
assert not parser.stack,parser.stack[:15]
print('FEE3_R7_HTML_TAGS_BALANCED=true')
print('FEE3_R7_STATIC_PASS='+str(len(checks))+' FAIL=0')
