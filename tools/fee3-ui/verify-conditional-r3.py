#!/usr/bin/env python3
"""Offline static FEE3 R3 smoke test; standard library only. Run from project root."""
from pathlib import Path
from html.parser import HTMLParser
import json
import re
import sys

root=Path(__file__).resolve().parents[2]
base=root/'frontend/src/app/features/fee3'
html=(base/'fee3-workspace.component.html').read_text(encoding='utf-8')
ts=(base/'fee3-reference-behavior.ts').read_text(encoding='utf-8')
comp=(base/'fee3-workspace.component.ts').read_text(encoding='utf-8')
seed=(base/'fee3-seed.data.ts').read_text(encoding='utf-8')

class ElementStack(HTMLParser):
    void={'input','br','hr','meta','img','link','source','wbr','area'}
    def __init__(self): super().__init__();self.stack=[];self.count=0
    def handle_starttag(self,tag,attrs):
        self.count+=1
        if tag not in self.void:self.stack.append((tag,self.getpos()[0]))
    def handle_endtag(self,tag):
        if not self.stack:raise AssertionError(f'Unexpected closing {tag} at {self.getpos()}')
        top,line=self.stack.pop()
        if top!=tag:raise AssertionError(f'Expected closing {top} opened line {line}, found {tag} at {self.getpos()}')

def fixture(name):
    marker=f'export const {name}:'
    pos=seed.index(marker)
    start=seed.index(' = ',pos)+3
    return json.JSONDecoder().raw_decode(seed[start:])[0]

checks=[]
def check(title,cond):
    if not cond:raise AssertionError('FAIL: '+title)
    checks.append(title)

parser=ElementStack();parser.feed(html);parser.close()
check('HTML elements balanced',not parser.stack)
check('113 reference operations present',len(fixture('FEE3_SEED_OPERATIONS'))==113)
check('33 reference formulas present',len(fixture('FEE3_SEED_FORMULAS'))==33)
for token in ['hasTransfer(item)','hasRemittance(item)','hasPaymentModes(item)','hasCardService(item)',
              'hasLoanModel(item)','hasLoanProfit(item)','hasLoanLegal(item)','loanPolicyRelevant(item)']:
    check('Conditional '+token,token in html)
for label in ['مدل مستقل محاسباتی','عقود مشمول و رویداد قراردادی','ساختار درخواست انتقال',
              'زمان اجرای درخواست','بستر انتقال وجه','سامانه انجام حواله','نوع درخواست کارت',
              'زمان مطالبه کارمزد قرض‌الحسنه','سیاست خسارت در تعطیلات']:
    check('Reference field '+label,label in html)
for key in ['LOAN_PROFIT:', 'QARD_FEE:', 'LATE_DAMAGE:', 'LOAN_LEGAL_COST:', 'LOAN_OTHER_AMOUNT:']:
    check('F1368 whitelist '+key,key in ts)
check('Source of reference behavior imported',"from './fee3-reference-behavior'" in comp)
check('Original enabled flag absent from stage 1', 'IS_ENABLED' not in html)
check('Frontend remains free of HTTP side effects',not re.search(r'HttpClient|fetch\s*\(|localStorage\.|sessionStorage\.',comp))
for name in checks:print('PASS:',name)
print(f'R3_STATIC_CHECKS_OK={len(checks)}; HTML_ELEMENTS={parser.count}')
