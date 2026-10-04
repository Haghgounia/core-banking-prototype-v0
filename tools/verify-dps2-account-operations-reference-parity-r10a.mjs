import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const htmlFile=path.join(root,'frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
let pass=0,fail=0;
const check=(name,ok,detail='')=>{if(ok){pass++;console.log(`PASS | ${name}`)}else{fail++;console.log(`FAIL | ${name}${detail?` | ${detail}`:''}`)}};
check('operations html exists',fs.existsSync(htmlFile),htmlFile);
if(fail){process.exitCode=1;process.exit();}
const html=fs.readFileSync(htmlFile,'utf8');
const normalized=html.replace(/'[^']*[\u0600-\u06ff][^']*'/g,"''");
const bad=/\{\{[^}]*[A-Za-z][^}]*[\u0600-\u06ff][^}]*\}\}|\{\{[^}]*[\u0600-\u06ff][^}]*[A-Za-z][^}]*\}\}/g;
const matches=[...normalized.matchAll(bad)].map(m=>m[0]);
check('reference-parity mixed Persian interpolation guard passes',matches.length===0,matches.slice(0,5).join(' | '));
check('transaction leg posting text uses control flow',html.includes('@if(l.postingReference){ثبت‌شده در دفتر معین}@else{در انتظار ثبت نهایی}'));
check('limit transaction scope uses control flow',html.includes('@if(l.transactionTypeCode){ {{uiLabel(l.transactionTypeCode)}} }@else{ همه تراکنش‌ها }'));
check('limit channel scope uses control flow',html.includes('@if(l.channelCode){ {{uiLabel(l.channelCode)}} }@else{ همه کانال‌ها }'));
check('restriction channel scope uses control flow',html.includes('@if(r.channelCode){ {{uiLabel(r.channelCode)}} }@else{ همه کانال‌ها }'));
check('restriction direction scope uses control flow',html.includes('@if(r.directionCode){ {{uiLabel(r.directionCode)}} }@else{ همه جهت‌ها }'));
check('signatory channel fallback uses control flow',html.includes('@if(a.channelCode){ {{uiLabel(a.channelCode)}} }@else{ همه کانال‌ها }'));
check('signatory max amount fallback uses control flow',html.includes('@if(a.maxAmount!==null && a.maxAmount!==undefined){ {{a.maxAmount}} }@else{ بدون سقف }'));
check('owner-search button label uses control flow',html.includes('@if(ownerSearchBusy()){در حال جستجو...}@else{جستجو}'));
check('owner display-name fallback uses control flow',html.includes('@if(p.displayName){ {{p.displayName}} }@else{ مشتری {{p.partyId}} }'));
check('old mixed transaction-leg ternary removed',!html.includes("{{l.postingReference?'ثبت‌شده در دفتر معین':'در انتظار ثبت نهایی'}}"));
console.log('------------------------------------------------------------');
console.log(`DPS2_ACCOUNT_OPERATIONS_REFERENCE_PARITY_R10A_PASS=${pass}`);
console.log(`DPS2_ACCOUNT_OPERATIONS_REFERENCE_PARITY_R10A_FAIL=${fail}`);
if(!fail)console.log('DPS2_ACCOUNT_OPERATIONS_REFERENCE_PARITY_R10A_STATIC_PASS');else process.exitCode=1;
