#!/usr/bin/env python3
"""Read-only Angular AOT minDate type regression gate for FEE3 R8-HF1.
Not a replacement for actual Angular AOT build on a machine with node_modules.
"""
from pathlib import Path
import re
base=Path(__file__).resolve().parents[2]
feature=base/'frontend/src/app/features/fee3'
h=(feature/'fee3-workspace.component.html').read_text(encoding='utf-8')
t=(feature/'fee3-workspace.component.ts').read_text(encoding='utf-8')
date=(base/'frontend/src/app/shared/ui/persian-date-input.component.ts').read_text(encoding='utf-8')
checks=[
 ('typed date input is string',"@Input() minDate = ''" in date),
 ('unsafe original expression absent',"[minDate]=\"row['from'] || todayDate()\"" not in h),
 ('both end date pickers use typed method',h.count('[minDate]="effectiveRowEndMinDate(row)"')==2),
 ('type annotation is string',bool(re.search(r'effectiveRowEndMinDate\(row:Record<string,unknown>\):string',t))),
 ('unknown is narrowed',"typeof start==='string'" in t),
 ('date from row is validated',"fee3EffectiveStartAllowed(start)" in t),
 ('invalid or past date falls back to current system date',"?start:this.todayDate()" in t),
 ('start date pickers continue to block backdated values',h.count('[minDate]="todayDate()"')>=3),
 ('validity end continues to be bounded', '[minDate]="ver.from || todayDate()"' in h),
]
for label,good in checks:
    if not good:raise SystemExit('FEE3_R8_HF1_FAIL: '+label)
print('FEE3_R8_HF1_AOT_DATE_TYPE_STATIC_PASS='+str(len(checks))+' FAIL=0')
print('SCOPE=Source/type-shape gate. Run build-production.cmd for Angular AOT proof.')
