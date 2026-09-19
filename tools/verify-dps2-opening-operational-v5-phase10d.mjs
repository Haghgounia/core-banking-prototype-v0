import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const checks=[];
const add=(name,file,token,neg=false)=>checks.push({name,file,token,neg});

add('version 0.10.0','VERSION','0.10.0');
add('CIF party search integration','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','cifService.searchParties');
add('party search only active','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts',"status:'ACTIVE'");
add('party search UX','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html','جستجو و انتخاب Party');
add('funding account operations integration','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','accountOperationsService.search');
add('funding ownership verification','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','details.owners.some');
add('funding ownership source reference','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','ACCOUNT_OPERATIONS:');
add('funding active account selector','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html','حساب مبدأ ACTIVE');
add('org-unit digital routing','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts',"channel==='INTERNET'||channel==='MOBILE'");
add('org-unit virtual branch code','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts',"setValue('0205'");
add('product-driven withdrawal media','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html','currentFamily().media');
add('cheque quantity','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','chequeCount');
add('withdrawal/payment instrument separation','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html','Withdrawal Media با درخواست صدور Payment Instrument یکی نیست');
add('opening obligation summary','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','openingObligations');
add('no fabricated fee tax obligation','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html','Fee/Tax/Service Charge تا اتصال قرارداد واقعی سرویس‌ها ساخته نمی‌شوند');
add('coverage uses total obligations','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','fundingCoverageGap');
add('create gate evidence controls','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html','Evidence کنترل‌های Create Gate');
add('required external evidence not auto pass','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','else if(c.required)fail=!this.checkEvidenceReference(c.code).trim()');
add('product eligibility runtime proof','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','PDL-RUNTIME:');
add('tax no-fabrication notice','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html','مقدار ساختگی برای عبور از Gate ثبت نکنید');
add('final compliance validity evidence','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','finalComplianceValidUntil');
add('account opened sms evidence forwarded','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts',"add('ACCOUNT_OPENED_SMS',v.accountOpenedSmsReference)");
add('mandatory create-settle-readiness-activate order','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html','ثبت Opening → ایجاد Account → Settlement → Activation Readiness → Activate');
add('batch direct processing disabled UI','frontend/src/app/features/four-deposits/deposit-opening-batch.component.html','پردازش و فعال‌سازی مستقیم گروهی در Backend فعلاً مسدود است');
add('batch direct activation disabled backend','backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/application/DepositOpeningBatchService.java','فعال‌سازی مستقیم گروهی در Operational v5 مجاز نیست.');

let ok=0;
for(const c of checks){
  const p=path.join(root,c.file);
  let pass=fs.existsSync(p);
  if(pass){
    const s=fs.readFileSync(p,'utf8');
    pass=c.neg?!s.includes(c.token):s.includes(c.token);
  }
  console.log(`${pass?'OK ':'ERR'} ${c.name}`);
  if(pass) ok++;
}
if(ok!==checks.length) throw new Error(`Phase 10D v5 verifier failed: ${ok}/${checks.length}`);
console.log(`DPS2 Operational Opening v5 Phase 10D verification OK: ${ok}/${checks.length} UI/integration checks passed.`);
