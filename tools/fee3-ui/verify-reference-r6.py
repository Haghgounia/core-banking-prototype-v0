#!/usr/bin/env python3
"""FCE V13.8.84 R6 specialized loan Qard + guarantee collateral UI regression.
STATIC checks only, never certifies ng build, browser parity or banking approval.
"""
from pathlib import Path
import re,sys
from html.parser import HTMLParser
root=Path(__file__).resolve().parents[2]
fee=root/'frontend/src/app/features/fee3'
html=(fee/'fee3-workspace.component.html').read_text(encoding='utf8')
ts=(fee/'fee3-workspace.component.ts').read_text(encoding='utf8')
ref=(fee/'fee3-specialized-reference.ts').read_text(encoding='utf8')
valid=(fee/'fee3-validation.ts').read_text(encoding='utf8')
beh=(fee/'fee3-reference-behavior.ts').read_text(encoding='utf8')
calc=(fee/'fee3-offline-calculator.ts').read_text(encoding='utf8')
css=(fee/'fee3-workspace.component.scss').read_text(encoding='utf8')
cases={i:html.split('@case('+str(i)+'){',1)[1].split('@case('+str(i+1)+'){',1)[0] for i in range(7)}
checks=[]
def check(title,condition):
 if not condition:raise AssertionError('FAIL | '+title)
 checks.append(title)
for name,text in [('Qard not user year fraction', "['yearFraction','days'].includes(name)"),('test real disbursement', 'qardTest.disbursementDate'),
 ('start date', 'qardTest.periodStart'),('end exclusive','qardTest.periodEndExclusive'),
 ('contract ref','qardTest.loanContractRef'),('charge key','qardTest.chargePeriodKey'),
 ('charge event','qardTest.chargeEventEvidenceRef'),('charge date','qardTest.chargeDate')]:
 check(name,text in html or text in ts)
check('no manually editable yearFraction field in test',"{name:'yearFraction'" not in ts.split('testInputFields():',1)[1].split('testNumber(',1)[0])
for s in ['qardDerivedInputs()','jalaliContractFraction(start,q.periodEndExclusive,ctx.dayCountBasis)','Object.assign(values,this.qardDerivedInputs())','qardFractionPreview()']:
 check('Qard: '+s,s in ts)
check('Qard supports Jalali leap split',"basis!=='JALALI_ACT_ACT'" in ref and 'persianYearStart(y+1)' in ref and '[365,366]' in ref)
check('Qard refuses inferred month daycount',"ctx.dayCountBasis==='CONTRACT_MONTHS'" in ts)
check('Qard does not fabricate rate','this.testNumbers' in ts and "name:'rate'" not in ts.split('qardDerivedInputs()',1)[1].split('private testInputFingerprint()',1)[0])
for s in ['نوع وثیقه / ساختار وثیقه','مبنای اعمال نرخ کارمزد','قواعد نرخ به تفکیک نوع وثیقه','پوشش مصوب وثیقه','نمونه سه وثیقه (۱۵/۵۰/۳۵)','نمونه دو وثیقه (۴۰/۶۰)','جمع سهم:','مبنای کارمزد','کف و سقف یک‌بار روی کل سبد']:
 check('Guarantee UI: '+s,s in html)
for s in ['GUARANTEE_PORTFOLIO_SHARE','GUARANTEE_PORTFOLIO_COVERED','GUARANTEE_SINGLE_BASE','GUARANTEE_COVERED_BASE','GUARANTEE_COLLATERALS',"rateSource==='EXEMPT'",'Number(row.sharePercent)','Math.abs(sum-100)>1e-7','ruleForType.coveragePercent','finalizeGuarantee(raw,version,rule.min,rule.max)','guaranteePortfolio={']:
 check('Guarantee model: '+s,s in ref)
for s in ['guaranteePortfolio(item)','guaranteeTestRows','guaranteeRuleChanged','guaranteeTestSample','simulateFee3Guarantee(this.editingItem','guaranteeFeeBasisChange','guaranteeCoverageValue','guaranteeCollateralLabel','testInputFingerprint']:
 check('Guarantee viewmodel: '+s,s in ts)
check('Portfolio validation in stage3','guaranteeRuleIssues(policy,version.method,basis)' in valid)
check('No step7 invalid artifact data','lastTestPassed()' in ts and 'definitionFingerprint' in ts and 'inputFingerprint' in ts)
check('Safe no manual user rate fallback',"ruleForType.rateSource==='REQUEST'" in ref and 'finite(input.contractRate' in ref)
check('Coverage cash protected',"r.collateralType==='CASH_100'" in ref and 'r.coveragePercent<100' in ref)
check('No HTTP/Oracle in new specialized math','HttpClient' not in ref and 'fetch(' not in ref and 'localStorage' not in ref)
check('Demo rates explicitly marked as example','قواعد صرفاً آموزشی' in ts or 'صرفاً آموزشی' in ts)
check('Control-based tax only once','const tax=rounded(amount*taxRate/100,version.rounding)' in ref)
check('No eval',not re.search(r'\beval\s*\(',ref))
check('UI reads correct fee basis as Persian','guaranteeBasisLabel(item)' in html)
check('New responsive styles',all(x in css for x in ('.fee3-guarantee-panel','.fee3-specialized-test','@media(max-width:700px)')))
check('Qard not a guessed bank-approved rate','FEE3_SEED_FORMULAS' in ts and 'safeReferenceFormula' in calc)
loops=re.findall(r'@for\((.*?)\)\s*\{',html,re.S)
check('Angular loop coverage',len(loops)>=90)
check('Each Angular @for includes mandatory track',all(';track ' in x or '; track ' in x for x in loops))
class StrictHTML(HTMLParser):
 void=set('area base br col embed hr img input link meta param source track wbr'.split())
 def __init__(self):super().__init__();self.stack=[];self.mismatch=[]
 def handle_starttag(self,tag,attrs):
  if tag not in self.void:self.stack.append(tag)
 def handle_endtag(self,tag):
  if tag in self.void:return
  if not self.stack or self.stack[-1]!=tag:self.mismatch.append((tag,self.stack[-1] if self.stack else None))
  else:self.stack.pop()
p=StrictHTML();p.feed(html)
check('HTML tag structure',not p.mismatch and not p.stack)
print('FEE3_R6_SPECIALIZED_STATIC_PASS='+str(len(checks))+' FAIL=0')
print('FEE3_R6_TRACKED_FOR_COUNT='+str(len(loops)))
print('SCOPE=QARD_FEE, GUARANTEE_FEE (one and multi collateral); NO ANGULAR BUILD/BROWSER CLAIM')
