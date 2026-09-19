import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=(p)=>fs.readFileSync(path.join(root,p),'utf8');
const checks=[];
const add=(name,file,token,neg=false)=>checks.push({name,file,token,neg});

add('version 0.10.0','VERSION','0.10.0');
add('runtime migration present','database/oracle/dps2/migrations/0.10.0-phase10b-opening-operational-runtime-alignment.sql','ATTEMPT_AT NULL');
add('runtime migration fail-fast','database/oracle/dps2/migrations/0.10.0-phase10b-opening-operational-runtime-alignment.sql','WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK');
add('runtime migration success marker','database/oracle/dps2/migrations/0.10.0-phase10b-opening-operational-runtime-alignment.sql','SUCCESS: Phase 10B runtime schema alignment completed.');
add('aggregate funding collection','backend/src/main/java/com/behsazan/corebanking/deposit/opening/domain/DepositOpeningModels.java','List<Funding> fundings');
add('aggregate obligations','backend/src/main/java/com/behsazan/corebanking/deposit/opening/domain/DepositOpeningModels.java','List<OpeningObligation> obligations');
add('aggregate allocations','backend/src/main/java/com/behsazan/corebanking/deposit/opening/domain/DepositOpeningModels.java','List<FundAllocation> fundAllocations');
add('funding source account','backend/src/main/java/com/behsazan/corebanking/deposit/opening/domain/DepositOpeningModels.java','SOURCE_ACCOUNT_ID');
add('check blocking scope','backend/src/main/java/com/behsazan/corebanking/deposit/opening/domain/DepositOpeningModels.java','BLOCKING_SCOPE_CODE');
add('repository funding persistence','backend/src/main/java/com/behsazan/corebanking/deposit/opening/oracle/DepositOpeningAggregateRepository.java','insertFunding(long fundingId');
add('repository obligation persistence','backend/src/main/java/com/behsazan/corebanking/deposit/opening/oracle/DepositOpeningAggregateRepository.java','insertObligation(long obligationId');
add('repository allocation persistence','backend/src/main/java/com/behsazan/corebanking/deposit/opening/oracle/DepositOpeningAggregateRepository.java','insertFundAllocation(long allocationId');
add('funding no pre-create SUCCESS requirement','backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java','برای درخواست تأییدشده حداقل یک منبع تأمین وجه باید برنامه‌ریزی شود.');
add('create gate recheck','backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java','Create Account قبل از عبور کامل از Create Gate مجاز نیست.');
add('create financial plan recheck','backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java','برنامه مالی Opening برای Create Account آماده نیست.');
add('account pending activation','backend/src/main/java/com/behsazan/corebanking/deposit/account/oracle/DepositAccountRepository.java',"'PENDING_ACTIVATION'");
add('account activation status pending readiness','backend/src/main/java/com/behsazan/corebanking/deposit/account/oracle/DepositAccountRepository.java',"ACTIVATION_STATUS_CODE = 'PENDING_READINESS'");
add('settlement service','backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/application/DepositOpeningOperationalService.java','SettlementResponse settle(');
add('allocation generation','backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/application/DepositOpeningOperationalService.java','allocate(fundings, obligations');
add('activation readiness service','backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/application/DepositOpeningOperationalService.java','ActivationReadinessResponse evaluateReadiness(');
add('expired evidence rejection','backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/application/DepositOpeningOperationalService.java','EVIDENCE_EXPIRED');
add('not applicable accepted on refresh','backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/application/DepositOpeningOperationalService.java','Set.of("PASS", "WAIVED", "NOT_APPLICABLE")');
add('activate requires READY','backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java','Activation قبل از تکمیل Activation Readiness Gate مجاز نیست.');
add('canonical settlement endpoint','backend/src/main/java/com/behsazan/corebanking/deposit/opening/web/DepositOpeningController.java','/requests/{id}/account/settlement');
add('readiness endpoint','backend/src/main/java/com/behsazan/corebanking/deposit/opening/web/DepositOpeningController.java','/requests/{id}/account/readiness');
add('frontend settlement endpoint','frontend/src/app/features/four-deposits/deposit-opening.service.ts','/account/settlement');
add('wizard funding plan payload','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','DEPOSIT_OPENING_OBLIGATION');
add('wizard activation readiness','frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','runActivationReadiness()');
add('batch basis rule','backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/application/DepositOpeningBatchService.java','GOV_EMPLOYEE_SAVINGS_1376');
add('batch family rule','backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/application/DepositOpeningBatchService.java','QARD_SAVINGS');
add('batch direct activate disabled','backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/application/DepositOpeningBatchService.java','فعال‌سازی مستقیم گروهی در Operational v5 مجاز نیست.');

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
if(ok!==checks.length) throw new Error(`Phase 10B v5 verifier failed: ${ok}/${checks.length}`);
console.log(`DPS2 Operational Opening v5 Phase 10B verification OK: ${ok}/${checks.length} backend/workflow checks passed.`);
