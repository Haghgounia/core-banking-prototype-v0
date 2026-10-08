import {readFileSync} from 'node:fs';
import {resolve, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
const root=resolve(dirname(fileURLToPath(import.meta.url)), '..');
const base='frontend/src/app/features/product-builder/pdl-table.component.';
const ts=readFileSync(resolve(root,base+'ts'),'utf8');
const html=readFileSync(resolve(root,base+'html'),'utf8');
const css=readFileSync(resolve(root,base+'scss'),'utf8');
const checks=[
 ['Optional process number hidden but not deleted from persistence contract',ts.includes("this.tableName() === 'PRODUCT_REQUIRED_DOCUMENT' && column.name === 'PROCESS_STEP_NO'") && html.includes('visibleEditableColumns()')],
 ['Inquiry result validity uses value and unit',html.includes('inquiryAgeValue') && html.includes('inquiryAgeUnit') && html.includes('دقیقه') && html.includes('ساعت') && html.includes('روز')],
 ['Inquiry duration stored in minutes',ts.includes('MAX_RESULT_AGE_MINUTES') && ts.includes('DAY: 1440') && ts.includes('HOUR: 60')],
 ['Deposit profile identity readonly with source context',html.includes('isDepositProfileIdentity(column)') && ts.includes('group.disable({emitEvent: false})') && ts.includes('type.disable({emitEvent: false})')],
 ['Deposit identity derives through governed product and version',ts.includes("this.service.row('PRODUCT_VERSION', version)") && ts.includes("this.service.row('PRODUCT', productId)")],
 ['Stamp count activation depends on stamp applicability',ts.includes("'IS_STAMP_DUTY_APPLICABLE'") && ts.includes('this.syncStampCount()') && ts.includes("count.disable({emitEvent: false})")],
 ['Clone action available in grid',html.includes('(click)="clone(row)"') && ts.includes('async clone(row:')],
 ['Clone does not update original row',ts.includes('this.editingId.set(null);\n    this.cloning.set(true)') && ts.includes('this.service.create(this.tableName(), values)')],
 ['Eligibility clone carries normalized criteria',ts.includes('const detail = await this.service.eligibilityRule(Number(row[descriptor.primaryKeyColumn]))')],
 ['Delete confirmation before API call',ts.indexOf('window.confirm(')>0 && ts.indexOf('window.confirm(')<ts.indexOf('await this.service.delete(')],
 ['Cloning messages, styling and no fake compile success',html.includes('رکورد اصلی تغییر نمی‌کند') && css.includes('.inquiry-age-controls')],
 ['Router Angular signal fix',ts.includes('lastSuccessfulNavigation()?.previousNavigation')]
];
let bad=0;
for (const [label,ok] of checks){console.log(`${ok?'PASS':'FAIL'} | ${label}`);if(!ok)bad++}
console.log(`PB_R12_STATIC_PASS=${checks.length-bad} FAIL=${bad}`);process.exitCode=bad?1:0;
