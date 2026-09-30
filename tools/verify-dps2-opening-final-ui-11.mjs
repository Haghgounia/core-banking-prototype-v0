import fs from 'node:fs';
const html=fs.readFileSync('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html','utf8');
const scss=fs.readFileSync('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.scss','utf8');
const ts=fs.readFileSync('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts','utf8');
const java=fs.readFileSync('backend/src/main/java/com/behsazan/corebanking/deposit/opening/domain/DepositOpeningModels.java','utf8');
const repo=fs.readFileSync('backend/src/main/java/com/behsazan/corebanking/deposit/opening/oracle/DepositOpeningAggregateRepository.java','utf8');
const checks=[
 ['topbar',html.includes('opening-topbar')],['page head',html.includes('head-badges')],['final stepper',html.includes('stepTechnicalLabels[i]')],
 ['payment holder role UI',html.includes('مبنای مجاز بودن دارنده')],['leaf count UI',html.includes('paymentInstrumentLeafCount')],
 ['final visual css',scss.includes('Final UI alignment from 11.html')],['holder role payload',ts.includes('INSTRUMENT_HOLDER_ROLE_CODE')],['leaf payload',ts.includes('CHEQUEBOOK_LEAF_COUNT')],
 ['settlement account id',ts.includes('SETTLEMENT_ACCOUNT_ID')],['profit destination account id',ts.includes('DESTINATION_ACCOUNT_ID')],
 ['backend holder role',java.includes('INSTRUMENT_HOLDER_ROLE_CODE')],['backend leaf count',java.includes('CHEQUEBOOK_LEAF_COUNT')],
 ['repo holder role',repo.includes('INSTRUMENT_HOLDER_ROLE_CODE')],['repo account ids',repo.includes('SETTLEMENT_ACCOUNT_ID')&&repo.includes('DESTINATION_ACCOUNT_ID')]
];
let fail=0; for(const [n,ok] of checks){console.log(`${ok?'PASS':'FAIL'} | ${n}`);if(!ok)fail++;}
console.log(`DPS2_OPENING_FINAL_UI_11_PASS=${checks.length-fail}`);console.log(`DPS2_OPENING_FINAL_UI_11_FAIL=${fail}`);if(fail)process.exit(1);console.log('DPS2_OPENING_FINAL_UI_11_STATIC_PASS');
