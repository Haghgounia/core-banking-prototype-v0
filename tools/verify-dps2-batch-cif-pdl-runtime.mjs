import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const batch=read('frontend/src/app/features/four-deposits/deposit-opening-batch.component.ts');
const ui=read('frontend/src/app/features/four-deposits/deposit-opening-batch.component.html');
const wizard=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const service=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/batch/application/DepositOpeningBatchService.java');
const validator=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/readiness/application/DepositOpeningRuntimeValidator.java');
const tests=[
  ['no fabricated product version fallback in Batch',!batch.includes('versionId:3102')&&!batch.includes('productVersionId:3102')],
  ['no fabricated CIF Party in Batch',!batch.includes('partyId:100001')&&!batch.includes('partyId:100002')&&!batch.includes('partyId:999999')&&!batch.includes('FormControl<number|null>(100001')],
  ['eligible versions come from live PDL',batch.includes("loadAllPdlRows('PRODUCT')")&&batch.includes("loadAllPdlRows('PRODUCT_VERSION')")],
  ['PDL QARD/deposit/IRR, origination, date and status checks', ['PRODUCT_CLASS_CODE','PRODUCT_FAMILY_CODE','QARD_SAVINGS','DEFAULT_CURRENCY_CODE','VERSION_STATUS_CODE','ORIGINATION_STATUS_CODE','RECORD_STATUS_CODE','VALID_FROM','VALID_TO'].every(x=>batch.includes(x))],
  ['CIF search results are authoritative',batch.includes('cifService.searchParties')&&batch.includes('cifService.getParty')],
  ['selected Party and PDL version persisted in each row',batch.includes('productVersionId:version.versionId')&&batch.includes('partyId,')],
  ['Batch reset does not modify a persisted Batch',batch.includes('newBatch():void')&&batch.includes('this.batch.set(null)')&&batch.includes('this.rows.set([])')],
  ['Batch UI binds real PDL and CIF selectors',ui.includes('eligibleVersions()')&&ui.includes('partySearchResults()')&&ui.includes('محصول و نسخه قابل افتتاح از PDL')],
  ['Batch wizard maintains original Product Version',wizard.includes('fromBatch?null')&&wizard.includes('if(this.form.controls.batchItemId.value)return;')],
  ['server checks CIF and PDL for each item',service.includes('runtimeValidator.validateBatchItem')&&validator.includes('repository.partyExists')&&validator.includes('repository.productVersion')],
  ['bulk bypass remains prohibited',service.includes('پردازش مستقیم Batch در Operational v5')&&service.includes('فعال‌سازی مستقیم گروهی در Operational v5 مجاز نیست.')]
];
const fails=tests.filter(([,pass])=>!pass);
for(const [name,pass] of tests)console.log(`${pass?'OK ':'FAIL'} ${name}`);
if(fails.length)process.exit(1);
console.log(`CIF/PDL dynamic Batch intake QA: ${tests.length} passed.`);
