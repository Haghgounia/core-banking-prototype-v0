import fs from 'node:fs';
const checks=[
 ['models','backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/domain/DepositAccountWaveDModels.java',['CorrespondentAccountCreation']],
 ['repository','backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/oracle/DepositAccountWaveDRepository.java',['CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE','insertCorrespondentAccountMaster','ACCOUNT_STATUS_CODE','OPENED_PRODUCT_VERSION_ID','CURRENT_PRODUCT_VERSION_ID']],
 ['service','backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/application/DepositAccountWaveDService.java',['createCorrespondentAccount','SEQ_DEPOSIT_ACCOUNT','correspondentProductVersion','balances.initializeAccount','CORRESPONDENT_ACCOUNT_CREATE','CORRESPONDENT_ACCOUNT:','CorrespondentProductProfileNotFoundException']],
 ['profile error','backend/src/main/java/com/behsazan/corebanking/deposit/account/error/CorrespondentProductProfileNotFoundException.java',['CorrespondentProductProfileNotFoundException','accountTypeCode','settlementCurrencyCode']],
 ['error handler','backend/src/main/java/com/behsazan/corebanking/shared/error/GlobalExceptionHandler.java',['handleCorrespondentProductProfileNotFound','CORRESPONDENT_PRODUCT_PROFILE_NOT_FOUND','HttpStatus.CONFLICT']],
 ['controller','backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/web/DepositAccountOperationsController.java',['@PostMapping("/correspondent-accounts")','createCorrespondentAccount']],
 ['operations repo','backend/src/main/java/com/behsazan/corebanking/deposit/account/operations/oracle/DepositAccountOperationsRepository.java',['LEFT JOIN "+openingSchema+".DEPOSIT_OPENING_REQUEST','NVL(A.OPENING_REQUEST_ID,0)','CORRESPONDENT']],
 ['angular service','frontend/src/app/features/four-deposits/deposit-account-operations.service.ts',['DepositCorrespondentAccountCreation','createCorrespondentAccount']],
 ['angular component','frontend/src/app/features/four-deposits/deposit-account-operations.component.ts',['correspondentCurrency','correspondentStatementSource','correspondentFrequency','correspondentTolerance','createCorrespondentAccount','runCorrespondentReconciliation','startReconciliation','addReconciliationItem','completeCorrespondentReconciliation','created.accountNo','created.accountId']],
 ['angular html','frontend/src/app/features/four-deposits/deposit-account-operations.component.html',['ثبت حساب نوسترو/وسترو','ارز تسویه','منبع صورت‌حساب','تناوب تطبیق','تلورانس مبلغ','اجرای تطبیق']],
 ['qualification fixture','tools/prepare-dps2-designer-step16-correspondent-products.mjs',['QA_STEP16_NOSTRO','QA_STEP16_VOSTRO','PRODUCT_FAMILY_CODE','NOSTRO_ACCOUNT','VOSTRO_ACCOUNT','CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE','DPS2_DESIGNER_STEP16_QUALIFICATION_FIXTURE_READY']],
 ['step16 qualifier','tools/qualify-dps2-designer-step16.cmd',['prepare-dps2-designer-step16-correspondent-products.mjs','runtime-dps2-designer-step16-correspondent-account-e2e.mjs']],
 ['final qualifier','tools/qualify-dps2-phase11nk-final.cmd',['prepare-dps2-designer-step16-correspondent-products.mjs','runtime-dps2-designer-step16-correspondent-account-e2e.mjs']],
 ['product builder repository','backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java',['columns.add("IS_DELETED"); placeholders.add("0")','"MIGRATED_AT", "IS_DELETED"']],
 ['reconciliation uniqueness UI','frontend/src/app/features/four-deposits/deposit-account-operations.component.ts',['CORR_${d.account.accountId}_${c.statementSourceCode',"existing?.statusCode==='COMPLETED'"]],
 ['reconciliation uniqueness runtime','tools/runtime-dps2-designer-step16-correspondent-account-e2e.mjs',['CORR_${aid}_${corr.statementSourceCode','DESIGNER_STEP16_RUNTIME_${type}_RECON_SOURCE']]
];
let pass=0,fail=0;
for(const [label,file,needles] of checks){const body=fs.readFileSync(file,'utf8');for(const n of needles){const ok=body.includes(n);console.log(`${ok?'PASS':'FAIL'} | ${label} | ${n}`);ok?pass++:fail++;}}
const svc=fs.readFileSync('backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/application/DepositAccountWaveDService.java','utf8');
const repo=fs.readFileSync('backend/src/main/java/com/behsazan/corebanking/deposit/account/waved/oracle/DepositAccountWaveDRepository.java','utf8');
const html=fs.readFileSync('frontend/src/app/features/four-deposits/deposit-account-operations.component.html','utf8');
const semantic=[
 ['Step16 creates a new account master before extension',svc.indexOf('insertCorrespondentAccountMaster')>=0&&svc.indexOf('insertCorrespondentAccountMaster')<svc.indexOf('upsertCorrespondent')],
 ['Step16 resolves governed product profile by account type and currency',repo.includes('ACCOUNT_TYPE_CODE=:type')&&repo.includes('SETTLEMENT_CURRENCY_CODE=:cur')],
 ['Step16 does not require an opening request for correspondent master',repo.includes('OPENING_REQUEST_ID')&&repo.includes('VALUES(:id,:no,NULL,:pv')],
 ['Step16 initializes canonical balance infrastructure for selectable account',svc.includes('balances.initializeAccount(id,cur,actor)')],
 ['Step16 uses independent POST create action in designer UI',html.includes('(click)="saveCorrespondent()"')&&html.includes('ثبت حساب نوسترو/وسترو')],
 ['Step16 qualification prepares governed correspondent profiles only through Product Builder API',fs.readFileSync('tools/prepare-dps2-designer-step16-correspondent-products.mjs','utf8').includes('/api/v1/product-builder/tables/')&&!fs.readFileSync('tools/prepare-dps2-designer-step16-correspondent-products.mjs','utf8').includes('INSERT INTO')],
 ['Step16 qualification fixture is explicitly QA-scoped',fs.readFileSync('tools/prepare-dps2-designer-step16-correspondent-products.mjs','utf8').includes('QA_STEP16_NOSTRO')&&fs.readFileSync('tools/prepare-dps2-designer-step16-correspondent-products.mjs','utf8').includes('QA_STEP16_VOSTRO')],
 ['Step16 runtime positively covers both NOSTRO and VOSTRO',fs.readFileSync('tools/runtime-dps2-designer-step16-correspondent-account-e2e.mjs','utf8').includes("exercise('NOSTRO','USD')")&&fs.readFileSync('tools/runtime-dps2-designer-step16-correspondent-account-e2e.mjs','utf8').includes("exercise('VOSTRO','EUR')")],
 ['Step16 runtime no longer accepts missing profile as a deferred branch',!fs.readFileSync('tools/runtime-dps2-designer-step16-correspondent-account-e2e.mjs','utf8').includes('STEP16_NO_GOVERNED_CORRESPONDENT_PRODUCT_PROFILE')],
 ['Step16 reconciliation source is account scoped to respect the canonical run uniqueness key',fs.readFileSync('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts','utf8').includes('CORR_${d.account.accountId}_${c.statementSourceCode')&&fs.readFileSync('tools/runtime-dps2-designer-step16-correspondent-account-e2e.mjs','utf8').includes('CORR_${aid}_${corr.statementSourceCode')],
 ['Step16 designer UI reuses a completed same-day run instead of violating the canonical uniqueness key',fs.readFileSync('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts','utf8').includes("existing?.statusCode==='COMPLETED'")&&!fs.readFileSync('frontend/src/app/features/four-deposits/deposit-account-operations.component.ts','utf8').includes("sourceSystemCode:'CORRESPONDENT_STATEMENT'")]
];
for(const [label,ok] of semantic){console.log(`${ok?'PASS':'FAIL'} | ${label}`);ok?pass++:fail++;}
console.log(`DESIGNER_STEP16_STATIC_PASS=${pass}`);console.log(`DESIGNER_STEP16_STATIC_FAIL=${fail}`);if(fail)process.exit(1);console.log('DPS2_DESIGNER_STEP16_CORRESPONDENT_ACCOUNT_STATIC_PASS');
