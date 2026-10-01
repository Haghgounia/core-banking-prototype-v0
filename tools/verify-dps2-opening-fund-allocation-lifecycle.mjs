import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=(p)=>fs.readFileSync(path.join(root,p),'utf8');
const ts=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const aggregate=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/application/DepositOpeningAggregateService.java');
const service=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/application/DepositOpeningOperationalService.java');
const repo=read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/operational/oracle/DepositOpeningOperationalRepository.java');
const ddl=read('docs/dps2/reference/DPS-2026-09-24.txt');

const checks=[];
const add=(name,pass)=>checks.push({name,pass:!!pass});
const has=(s,...xs)=>xs.every(x=>s.includes(x));

add('Oracle constraint allows canonical allocation lifecycle',has(ddl,"ALLOCATION_STATUS_CODE IN ('PENDING','POSTED','REVERSED','REFUNDED','FAILED')"));
add('frontend persists planned allocations as PENDING',has(ts,"ALLOCATION_STATUS_CODE:'PENDING'")&&!ts.includes("ALLOCATION_STATUS_CODE:'PLANNED'"));
add('aggregate validator rejects non-canonical allocation status',has(aggregate,'PENDING/POSTED/REVERSED/REFUNDED/FAILED','وضعیت تخصیص نامعتبر است'));
add('settlement distinguishes pending from non-pending allocations',has(service,'countPendingAllocations(openingRequestId)','countNonPendingAllocations(openingRequestId)'));
add('settlement rejects existing non-pending partial state',has(service,'nonPendingAllocations > 0','برای جلوگیری از Double Posting'));
add('pending plan validates obligation coverage',has(service,'countPendingAllocationCoverageGaps(openingRequestId)')&&has(repo,'countPendingAllocationCoverageGaps'));
add('pending plan validates funding overage',has(service,'countPendingAllocationFundingOverages(openingRequestId)')&&has(repo,'countPendingAllocationFundingOverages'));
add('pending allocations are promoted to POSTED',has(service,'postPendingAllocations(openingRequestId, settlementReference, actor)')&&has(repo,"ALLOCATION_STATUS_CODE = 'POSTED'"));
add('posted allocation receives settlement reference',has(repo,"SETTLEMENT_REFERENCE = COALESCE(SETTLEMENT_REFERENCE, :settlementReference || '-A' || TO_CHAR(OPENING_FUND_ALLOC_ID))"));
add('legacy no-preallocation path still generates POSTED rows',has(service,'allocations = allocate(fundings, obligations, settlementReference, actor)')&&has(repo,"'POSTED', :settlementReference"));

let pass=0;
for(const c of checks){console.log(`${c.pass?'PASS':'FAIL'} | ${c.name}`);if(c.pass)pass++;}
console.log('------------------------------------------------------------');
console.log(`DPS2_OPENING_FUND_ALLOC_LIFECYCLE_PASS=${pass}`);
console.log(`DPS2_OPENING_FUND_ALLOC_LIFECYCLE_FAIL=${checks.length-pass}`);
if(pass!==checks.length)process.exit(1);
console.log('DPS2_OPENING_FUND_ALLOC_LIFECYCLE_STATIC_PASS');
