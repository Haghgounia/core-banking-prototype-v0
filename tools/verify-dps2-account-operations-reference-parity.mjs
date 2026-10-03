import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const htmlFile=path.join(root,'frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
const scssFile=path.join(root,'frontend/src/app/features/four-deposits/deposit-account-operations.component.scss');
const tsFile=path.join(root,'frontend/src/app/features/four-deposits/deposit-account-operations.component.ts');
let pass=0,fail=0;
const check=(name,ok,detail='')=>{if(ok){pass++;console.log(`PASS | ${name}`)}else{fail++;console.log(`FAIL | ${name}${detail?` | ${detail}`:''}`)}};
for(const f of [htmlFile,scssFile,tsFile])check(`file exists: ${path.basename(f)}`,fs.existsSync(f),f);
if(fail){process.exitCode=1;process.exit();}
const html=fs.readFileSync(htmlFile,'utf8');
const scss=fs.readFileSync(scssFile,'utf8');
const ts=fs.readFileSync(tsFile,'utf8');

// Reference view naming from Deposit_Account_Operations_Operational_v11_RC_2026-09-22.
for(const title of [
 'نمای عملیاتی سپرده','نگهداری حساب سپرده','چرخه عمر، راکدی و انسداد','عملیات سپرده مدت‌دار','مدیریت سود سپرده',
 'پردازش تراکنش‌های سپرده','مدیریت صورتحساب سپرده','مدیریت سقف‌ها و محدودیت‌های حساب','بستن حساب و پردازش سررسید',
 'مدیریت خدمات حساب سپرده','مدیریت ارتباط طرف با حساب','مدیریت مقررات و انطباق سپرده','مدیریت قیمت‌گذاری و کارمزد سپرده',
 'مدیریت مالیات و کسورات سود سپرده','تطبیق و کنترل مغایرت سپرده','مدیریت استثناها و اصلاحات سپرده',
 'حساب‌های نوسترو / وسترو','مدیریت جوایز و قرعه‌کشی سپرده‌ها'
]) check(`reference section title: ${title}`,html.includes(title));

for(const marker of [
 'reference-view"','class="reference-page-head"','class="reference-scope-banner"','class="reference-panel"',
 'class="reference-form-grid"','class="reference-split"','class="reference-toolbar"','XML: 01A — هسته و نگهداری حساب',
 'XML: 01B / 01C — چرخه عمر / مسدودی','اطلاعات پایه و ویژگی‌ها','تغییر محصول / شرط حساب','اختیار صاحب امضا',
 'فعال‌سازی کنترل‌شده حساب','عملیات موردی چرخه عمر','مسدودی دستی/کنترلی غیر وثیقه‌ای','عملیات گروهی حساب'
]) check(`reference UX marker: ${marker}`,html.includes(marker));

for(const css of ['.reference-page-head','.reference-scope-banner','.reference-panel','.reference-section-head','.reference-form-grid','.reference-split','.reference-toolbar'])
 check(`reference CSS: ${css}`,scss.includes(css));

// Static combo labels must be bank-user Persian labels, not raw technical constants.
const optionTexts=[...html.matchAll(/<mat-option\b[^>]*>([\s\S]*?)<\/mat-option>/g)].map(m=>m[1].replace(/<[^>]+>/g,'').trim());
const badStatic=optionTexts.filter(t=>t && !t.includes('{{') && /[A-Z_]{3,}/.test(t) && !/[\u0600-\u06ff]/.test(t));
check('no raw latin technical code in static combo labels',badStatic.length===0,badStatic.slice(0,20).join(' | '));
for(const label of ['اعلان پیامکی','زبان صورتحساب','خدمت ویژه','فعال','غیرفعال','متن','عدد','تاریخ','بله/خیر','شماره موبایل','پست الکترونیکی','نشانی','حداقل مانده حساب','انتقال وجه از حساب','برداشت نقدی','همه عملیات','شعبه','بانکداری اینترنتی'])
 check(`Step01 bank-user label: ${label}`,(html+'\n'+ts).includes(label));

// No duplicate literal ids after moving lifecycle/hold/closure blocks.
const ids=[...html.matchAll(/\bid="([^"]+)"/g)].map(m=>m[1]);
const dup=[...new Set(ids.filter((x,i)=>ids.indexOf(x)!==i))];
check('literal element ids are unique',dup.length===0,dup.join(','));

// ngModel bindings and event handlers must resolve in component source.
const models=[...new Set([...html.matchAll(/\[\(ngModel\)\]="([A-Za-z_$][\w$]*)"/g)].map(m=>m[1]))];
const missingModels=models.filter(m=>!new RegExp(`\\b${m.replace(/[$]/g,'\\$&')}\\s*(?:[:=])`).test(ts));
check('all ngModel bindings resolve to component fields',missingModels.length===0,missingModels.join(','));
const handlers=[...new Set([...html.matchAll(/\((?:click|selectionChange|change)\)="([A-Za-z_$][\w$]*)\s*\(/g)].map(m=>m[1]))];
const missingHandlers=handlers.filter(m=>!new RegExp(`\\b(?:async\\s+)?${m.replace(/[$]/g,'\\$&')}\\s*\\(`).test(ts));
check('all reference UI handlers resolve to component methods',missingHandlers.length===0,missingHandlers.join(','));

for(const token of ['attributeDefinitions','syncAttributeDefinition()','uiLabel(v:any)','selectAccountById(id:number)'])check(`presentation contract: ${token}`,ts.includes(token));
check('technical values remain in model',ts.includes("attributeCode='SMS_NOTIFY'")&&ts.includes("attributeValueType='BOOLEAN'"));
check('no accidental Persian text inside TypeScript-style property identifiers',!/\{\{[^}]*[A-Za-z][^}]*[\u0600-\u06ff][^}]*\}\}|\{\{[^}]*[\u0600-\u06ff][^}]*[A-Za-z][^}]*\}\}/.test(html.replace(/'[^']*[\u0600-\u06ff][^']*'/g,"''")));

console.log('------------------------------------------------------------');
console.log(`DPS2_ACCOUNT_OPERATIONS_REFERENCE_PARITY_PASS=${pass}`);
console.log(`DPS2_ACCOUNT_OPERATIONS_REFERENCE_PARITY_FAIL=${fail}`);
if(!fail)console.log('DPS2_ACCOUNT_OPERATIONS_REFERENCE_PARITY_STATIC_PASS');else process.exitCode=1;
