#!/usr/bin/env python3
"""FEE3 R2 identity-only UI contract verification against the supplied reference HTML.
Run with: python tools/fee3-ui/verify-identity-r2.py REFERENCE_HTML
No Oracle, HTTP or UI writes.
"""
import json,re,sys
from pathlib import Path
base=Path(__file__).resolve().parents[2]
feat=base/'frontend/src/app/features/fee3'
ref=Path(sys.argv[1]).read_text(encoding='utf-8') if len(sys.argv)>1 else None
html=(feat/'fee3-workspace.component.html').read_text(encoding='utf-8')
models=(feat/'fee3.models.ts').read_text(encoding='utf-8')
component=(feat/'fee3-workspace.component.ts').read_text(encoding='utf-8')
validation=(feat/'fee3-validation.ts').read_text(encoding='utf-8')
style=(feat/'fee3-workspace.component.scss').read_text(encoding='utf-8')
form=html.split('@case(0){',1)[1].split('@case(1){',1)[0]
fields=re.findall(r'<mat-label>(.*?)</mat-label>',form,re.S)
check_count=0
def check(cond,description):
 global check_count
 assert cond,description
 check_count+=1
 print('PASS',description)
check(ref is not None,'Reference V13.8.84 supplied for exact comparison')
checks={
 'module':re.search(r"v13Control\('module','([^']+)'",ref).group(1),
 'operation':re.search(r"v13Control\('operation','([^']+)'",ref).group(1),
 'amount':re.search(r"v13Control\('category','([^']+)'",ref).group(1),
 'title':re.search(r"v13Text\('title','([^']+)'",ref).group(1),
 'owner':re.search(r"v13Control\('ownerUnit','([^']+)'",ref).group(1),
 'origin':re.search(r'<label>([^<]+) \*</label><select data-v13-field="tariffOrigin"',ref).group(1),
 'fee_currency':re.search(r"const feeSelect=v13Control\('currency','([^']+)'",ref).group(1),
 'service_currency':re.search(r"v13Control\('serviceCurrency','([^']+)'",ref).group(1)
}
expected=list(checks.values())
check(fields==expected,'Eight business controls in the exact reference/tab order')
check(len(re.findall(r'<mat-select[^>]*required>|<input[^>]*required>',form))==8,'All eight first-stage controls display one Material-required marker (not duplicate stars)')
check('fee3-identity-grid' in form and 'fee3-id-title' in form and 'fee3-identity-currency-grid' in form,'Explicit three-column identity and two-column currency layout')
check('fee3-id-title' in style and 'grid-column:1/-1' in style,'Title spans the entire identity row')
check('fee3-id-owner,.fee3-id-origin {grid-column:1/2;}' in style,'Owner and tariff-origin each occupy the right column in separate rows')
check('fee3-identity-currency {border-top:' in style,'Currency subsection has the expected visual separator')
check('item.enabled' not in form and 'item.description' not in form and 'item.variant' not in form and 'item.code' not in form,'No database-only fields or IS_ENABLED toggle in first-stage screenshot')
check('[(ngModel)]="item.ownerDomain"' in form,'Owner unit is bound to existing OWNER_DOMAIN_CODE equivalent')
check('[(ngModel)]="ver.origin"' in form,'Tariff origin is bound to version origin')
check('[(ngModel)]="ver.origin"' not in html.split('@case(4){',1)[1].split('@case(5){',1)[0],'Tariff origin is not duplicated as editable validity-stage control')
check("ownerDomain:'TARIFF'" in models and "serviceCurrency:'IRR'" in models and "feeCurrency:'IRR'" in models,'Reference defaults: central tariff owner, IRR service, IRR fee')
check('FEE3_OWNER_UNITS' in models and 'FCE_REF_CODE' not in form,'Owner choices come from standalone supplied wizard and are not mislabeled as seeded database references')
check('item.serviceCurrency=code' in component and 'scope.currency=code' in component,'Service-currency change synchronizes current-version scopes')
check('this.editingItem.feeCurrency=code' in component and 'currencyIdentityEditable' in component,'Output currency independently editable only for new model')
check('if(!allowedOrigins.includes(version.origin))add(0,' in validation,'Tariff origin is validated at identity stage')
check('if(!currencies.includes(item.serviceCurrency' in validation and 'if(!currencies.includes(item.feeCurrency' in validation,'Both currency values validated at identity stage')
check('version.scopes[0]?.currency' in component,'Existing seeded catalog has an explicit read-only service-currency fallback')
check('this.editingItem.ownerDomain=this.editingItem.module' not in component,'Changing banking module never overwrites responsible owner unit')
check('[(ngModel)]="scope.currency"' not in html,'No contradictory currency selector remains in scope editor')
check('serviceCurrency?: string;' in models and 'feeCurrency?: string;' in models,'Currency pair is explicitly modeled as provisional UI-only metadata')
check('disabled>\u0627\u0646\u062a\u0634\u0627\u0631 \u062a\u0639\u0631\u0641\u0647' in html,'Fee publication remains disabled')
check('FEE3-UI-R1' not in form,'Identity form does not depend on R1 static text')
print(f'FEE3 R2 IDENTITY PARITY OK: {check_count} checks')
