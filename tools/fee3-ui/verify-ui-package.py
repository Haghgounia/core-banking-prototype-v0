#!/usr/bin/env python3
"""No-dependency static preflight for the offline FEE3 UI patch.
This is not a substitute for `npm run build` and browser E2E testing.
Run at the repository root: python tools/fee3-ui/verify-ui-package.py
"""
import json
import re
from collections import Counter
from html.parser import HTMLParser
from pathlib import Path

root=Path(__file__).resolve().parents[2]
feature=root/'frontend/src/app/features/fee3'
fixture=feature/'fee3-seed.data.ts'
html=feature/'fee3-workspace.component.html'
css=feature/'fee3-workspace.component.scss'
routes=root/'frontend/src/app/app.routes.ts'
sidebar=root/'frontend/src/app/layout/app-shell.component.html'
group=root/'frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.ts'
expected={'FEE3_SEED_ITEMS':297,'FEE3_SEED_OPERATIONS':113,
          'FEE3_SEED_FORMULAS':33,'FEE3_SEED_SOURCES':5,'FEE3_SEED_CODES':167}
checks=0

def check(ok,msg):
    global checks
    if not ok: raise AssertionError(msg)
    checks+=1
    print('PASS',msg)

fixture_lines=fixture.read_text(encoding='utf-8').splitlines()
fixture_data={}
for line in fixture_lines:
    if line.startswith('export const FEE3_SEED_'):
        key=line.split(':',1)[0].removeprefix('export const ')
        fixture_data[key]=json.loads(line.split(' = ',1)[1].rstrip(';'))
for k,count in expected.items():check(len(fixture_data[k])==count,f'{k}={count}')
items=fixture_data['FEE3_SEED_ITEMS']
versions=[v for item in items for v in item['versions']]
check(len(versions)==300,'300 version records')
check(sum(len(v['rules']) for v in versions)==313,'313 rule records')
check(sum(len(v['scopes']) for v in versions)==300,'300 scope records')
check(sum(len(v['tests']) for v in versions)==6,'6 draft test-case records')
check(all(v['status']=='DRAFT' and not v['active'] for v in versions),'All supplied studies are non-active DRAFT')
check(len({item['code'] for item in items})==len(items),'Immutable item identities are unique')
methods={c['code'] for c in fixture_data['FEE3_SEED_CODES'] if c['group']=='METHOD'}
check(methods=={'FIXED','PERCENT','MIXED','PROGRESSIVE','BRACKET','MATRIX','USAGE','DAILY','FORMULA'},'All 9 reference methods are present')
page=html.read_text(encoding='utf-8')
check(len(re.findall(r'@case\([0-7]\)',page))==8,'All 8 editor stages exist')
for key in ('PROGRESSIVE','BRACKET','MATRIX','USAGE','DAILY','FORMULA','FIXED','MIXED','PERCENT'):
    check(key in page,f'Method-specific editor includes {key}')
check(page.count('{')==page.count('}'),'Template brace counts are balanced')

class HtmlTags(HTMLParser):
    void={'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}
    def __init__(self):super().__init__();self.stack=[];self.errors=[]
    def handle_starttag(self,tag,attrs):
        if tag not in self.void:self.stack.append(tag)
    def handle_endtag(self,tag):
        if not self.stack or self.stack[-1]!=tag:
            self.errors.append((tag,self.stack[-3:]))
        else:self.stack.pop()
tags=HtmlTags();tags.feed(page)
check(not tags.errors and not tags.stack,'HTML elements have matching start/end tags')
route_text=routes.read_text(encoding='utf-8')
menu_text=sidebar.read_text(encoding='utf-8')
check(route_text.count("path: 'fee3'")==1,'One FEE3 lazy route')
check(menu_text.count('routerLink="/fee3"')==1,'Exactly one new top-level menu entry')
check("'/fee3'" in group.read_text(encoding='utf-8'),'Products group expands for FEE3')
check('app-persian-date-input' in page,'Uses existing shared Persian-date component')
check('mat-form-field' in page and 'mat-expansion-panel' in page,'Uses Angular Material controls')
check('هیچ تعرفه‌ای تأیید یا فعال نمی‌شود' in page and 'disabled>انتشار تعرفه' in page,'Publication clearly unavailable in UI phase')
ui_sources='\n'.join(p.read_text(encoding='utf-8') for p in feature.glob('*.ts') if p.name!='fee3-seed.data.ts')
check(not re.search(r'\b(fetch|HttpClient|localStorage|sessionStorage|indexedDB)\s*\(',ui_sources), 'No transport or browser persistence implementation')
check('providedIn:\'root\'' in (feature/'fee3-state.service.ts').read_text(encoding='utf-8'),'Isolated injectable session-only data adapter')
style=css.read_text(encoding='utf-8')
check('var(--app-primary)' in style and '@media' in style,'Project CSS tokens and responsive breakpoints used')
source_tokens=set(re.findall(r'var\((--app-[a-zA-Z-]+)\)',style))
project_tokens=set(re.findall(r'(--app-[a-zA-Z-]+):', (root/'frontend/src/styles.scss').read_text(encoding='utf-8')))
check(source_tokens<=project_tokens,'All referenced project theme tokens are defined')
print(f'\nSTATIC FEE3 PREFLIGHT OK: {checks} checks. Full Angular build and browser QA still required.')
