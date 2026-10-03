import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=(p)=>fs.readFileSync(path.join(root,p),'utf8');
const html=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.html');
const scss=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.scss');
const ts=read('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts');
const uiSvc=read('frontend/src/app/features/four-deposits/deposit-account-operations.service.ts');
const models=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/domain/DepositAccountOperationsModels.java');
const opsRepo=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/oracle/DepositAccountOperationsRepository.java');
const servicingModels=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/domain/DepositAccountServicingModels.java');
const servicingRepo=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/oracle/DepositAccountServicingRepository.java');
const servicingSvc=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/servicing/application/DepositAccountServicingService.java');
const ctl=read('backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java');
let pass=0,fail=0;
const check=(name,ok)=>{if(ok){pass++;console.log(`PASS | ${name}`)}else{fail++;console.log(`FAIL | ${name}`)}};

for(const token of [
  '<h3>اطلاعات پایه و ویژگی‌ها</h3>',
  'سوابق اطلاعات پایه و ویژگی‌ها',
  '<h3>تغییر محصول / شرط حساب</h3>',
  'سوابق تغییر محصول / شرط حساب',
  '<h3>مالک و صاحب حساب</h3>',
  'مالکین و صاحبان حساب',
  'سوابق اصلاح مالک / صاحب حساب'
]) check(`maintenance UX: ${token}`,html.includes(token));
check('base history is visible grid, not collapsed detail',html.includes('aria-label="سوابق اطلاعات پایه و ویژگی‌های حساب"')&&!html.includes('<summary>ویژگی‌های ثبت‌شده حساب</summary>'));
check('product/condition history is visible grid',html.includes('aria-label="سوابق تغییر محصول و شرط حساب"'));
check('owner editor is inside maintenance',html.indexOf('<h3>مالک و صاحب حساب</h3>')>html.indexOf('id="ops-step01"')&&html.indexOf('<h3>مالک و صاحب حساب</h3>')<html.indexOf('id="ops-step02"'));
check('duplicate legacy owner cards removed',!html.includes('<h2>طرف‌های حساب</h2>')&&!html.includes('<h2>افزودن Party</h2>'));
check('owner edit and deactivate actions wired',html.includes('(click)="startOwnerEdit(owner)"')&&html.includes('(click)="removeParty(owner.accountPartyId)"')&&html.includes('(click)="saveOwnerMaintenance()"'));
check('owner names are resolved from CIF presentation map',ts.includes('ownerDisplayName(')&&ts.includes('loadOwnerName(')&&ts.includes('resolveMaintenancePartyName()'));
check('owner historical rows are available',ts.includes("ownerMaintenanceHistory(){return (this.selected()?.servicingHistory??[]).filter(x=>x.changeTypeCode==='OWNER')}"));
check('maintenance grid styling exists',scss.includes('.maintenance-grid-shell')&&scss.includes('.maintenance-grid-scroll')&&scss.includes('.maintenance-owner-panel'));

check('frontend account party carries recordVersion',uiSvc.includes('sourceOpeningPartyId:number|null;recordVersion:number;'));
check('frontend update party API exists',uiSvc.includes('updateParty(id:number,pid:number,body:any)'));
check('backend party response carries recordVersion',models.includes('Long sourceOpeningPartyId,long recordVersion'));
check('backend query selects party record version',opsRepo.includes('SOURCE_OPENING_PARTY_ID,RECORD_VERSION'));
check('backend update request contract exists',servicingModels.includes('UpdateAccountPartyRequest'));
check('backend party row lock exists',servicingRepo.includes('lockAccountParty(')&&servicingRepo.includes('FOR UPDATE'));
check('backend optimistic party update exists',servicingRepo.includes("STATUS_CODE='ACTIVE' AND RECORD_VERSION=:version"));
check('backend update is audited as OWNER',servicingSvc.includes('updateParty(long accountId,long accountPartyId,UpdateAccountPartyRequest')&&servicingSvc.includes('repository.history(accountId,"OWNER",oldValue,newValue'));
check('backend owner correction endpoint exists',ctl.includes('@PutMapping("/{accountId}/parties/{accountPartyId}")'));
check('R5 null-safe attribute selection retained',ts.includes("const currentValue=current?.attributeValue??''"));

console.log('------------------------------------------------------------');
console.log(`DPS2_ACCOUNT_MAINTENANCE_R6_PASS=${pass}`);
console.log(`DPS2_ACCOUNT_MAINTENANCE_R6_FAIL=${fail}`);
if(!fail)console.log('DPS2_ACCOUNT_MAINTENANCE_R6_STATIC_PASS');else process.exitCode=1;
