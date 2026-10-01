import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const seed=read('tools/prepare-dps2-uat-product-pack-v1.mjs');
const runtime=read('tools/runtime-dps2-uat-product-pack-v1.mjs');
const ts=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts');
const html=read('frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html');
const doc=read('docs/patches/DPS2-0.11.0-UAT-PRODUCT-PACK-V1-FA.md');
const checks=[];
const add=(name,ok)=>checks.push({name,ok:!!ok});
const has=(body,...parts)=>parts.every(p=>body.includes(p));

const products={
 QARD_SAVINGS:['UAT-QS-STD-001','UAT-QS-DIG-001','UAT-QS-ORG-001'],
 CURRENT_ACCOUNT:['UAT-CA-PER-001','UAT-CA-ORG-001','UAT-CA-NOCHEQUE-001'],
 SHORT_TERM_DEPOSIT:['UAT-ST-STD-001','UAT-ST-DIG-001','UAT-ST-HIGH-001'],
 LONG_TERM_DEPOSIT:['UAT-LT-STD-001','UAT-LT-PAYOUT-001','UAT-LT-RENEW-001']
};
const all=Object.values(products).flat();
add('UAT pack has exactly 12 unique product codes',all.length===12&&new Set(all).size===12&&all.every(code=>seed.includes(`code:'${code}'`)));
for(const [family,codes] of Object.entries(products))add(`${family} has three UAT products`,codes.length===3&&codes.every(code=>seed.includes(`code:'${code}'`))&&seed.includes(`family:'${family}'`));
add('pack includes expired negative version',has(seed,"'UAT-QS-STD-001':[ {versionNo:1,kind:'EXPIRED'} ]"));
add('pack includes future negative version',has(seed,"'UAT-CA-ORG-001':[ {versionNo:2,kind:'FUTURE'} ]"));
add('pack includes suspended/non-open negative version',has(seed,"'UAT-ST-DIG-001':[ {versionNo:2,kind:'SUSPENDED'} ]"));
add('renew product includes historical version and active v2',has(seed,"'UAT-LT-RENEW-001':[ {versionNo:1,kind:'EXPIRED'} ]","'UAT-LT-RENEW-001':2"));
add('seed is explicit apply-only and defaults to dry run',has(seed,"const apply=process.argv.includes('--apply')","DPS2_UAT_PRODUCT_PACK_V1_DRY_RUN_PASS","DPS2_UAT_PRODUCT_PACK_V1_APPLY_PASS"));
add('seed uses Product Builder REST instead of direct Oracle DML',has(seed,'/api/v1/product-builder/tables/','/descriptor','/rows')&&!/\b(INSERT|UPDATE|DELETE|MERGE)\s+INTO\s+PDL\./i.test(seed));
add('seed is descriptor/metadata driven',has(seed,'async function descriptor(table)','required(desc)','foreignKey','descriptorValue('));
add('opening-rule seed clones a valid family template before override',has(seed,'familyPolicyTemplate(','editableClone(','alignOpeningAmounts(','UAT_PACK_OPENING_CREATED'));
add('opening-rule seed aligns minimum/default/maximum amounts',has(seed,"'DEFAULT_OPENING_AMOUNT'","'MAX_OPENING_AMOUNT'",'n<min'));
add('opening-rule numeric bounds honor descriptor precision',has(seed,'numericIntegerCap(c)','safeNumericUpper(c,min)','n>numericIntegerCap(c)')&&!seed.includes('999999999999'));
add('opening-rule baseline disables unrelated optional flags',has(seed,'applyConservativeOpeningFlags','IS_INTRODUCER_REQUIRED','IS_OVERDRAFT_ALLOWED','IS_STAMP_DUTY_APPLICABLE'));
add('rate-tier upper bounds honor descriptor precision',has(seed,'setSafeUpperSynonyms',"['MAX_AMOUNT','TO_AMOUNT','UPPER_BOUND']"));
add('opening-rule diagnostics print numeric metadata',has(seed,'UAT_PACK_OPENING_DESCRIPTOR','maxPrecision=','maxScale=','maxNullable='));
add('opening-rule conflict prints exact generated payload',has(seed,'UAT_PACK_OPENING_PAYLOAD','JSON.stringify(p)'));
add('seed covers channel eligibility opening and withdrawal policies',has(seed,"'PRODUCT_CHANNEL_RULE'","'PRODUCT_ELIGIBILITY_RULE'","'DEPOSIT_PRODUCT_OPENING_RULE'","'DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA'"));
add('seed covers term pricing rate tier and profit payment policies',has(seed,"'DEPOSIT_PRODUCT_TERM_RULE'","'DEPOSIT_PRODUCT_ALLOWED_TERM'","'PRODUCT_PRICING_RULE'","'PRODUCT_PRICING_COMPONENT'","'PRODUCT_RATE_TIER'","'DEPOSIT_PROFIT_PAYMENT_RULE'"));
add('term-rule seed reuses valid family template before override',has(seed,"familyPolicyTemplate('DEPOSIT_PRODUCT_TERM_RULE'","editableClone(d.termRule,template)",'UAT_PACK_TERM_RULE_CREATED'));
add('term-rule seed preserves business maturity action and canonical flags',has(seed,"'MATURITY_ACTION_CODE',spec.maturityAction","const renewable=spec.maturityAction.startsWith('RENEW_')","'IS_RENEWABLE',renewable?1:0","'GRACE_PERIOD_DAYS',0","'RULE_STATUS_CODE','ACTIVE'"));
add('renewable term rule satisfies CK_DPTR_RENEW',has(seed,"'RENEWAL_INSTRUCTION_CODE',renewable?'AUTO_RENEW':null","'MAX_RENEWAL_COUNT',renewable?12:0"));
add('term and allowed-term audit actor is explicit',has(seed,"setIfColumn(e,d.termRule,'CREATED_BY',actor)","CREATED_BY:actor"));
add('term-rule conflict prints descriptor payload and required columns',has(seed,'UAT_PACK_TERM_RULE_DESCRIPTOR','UAT_PACK_TERM_RULE_PAYLOAD','UAT_PACK_TERM_RULE_REQUIRED','UAT_PACK_TERM_RULE_CONFLICT'));
add('current-account pack differentiates cheque capability',has(seed,"UAT-CA-PER-001", "media:['CASH','CARD','CHEQUE','TRANSFER']", "UAT-CA-NOCHEQUE-001", "media:['CASH','CARD','TRANSFER']"));
add('term products include maturity policy variants',has(seed,"maturityAction:'RENEW_PRINCIPAL'","maturityAction:'PAY_TO_ACCOUNT'","maturityAction:'CLOSE_AND_SETTLE'","maturityAction:'RENEW_PRINCIPAL_PAY_PROFIT'"));
add('term products include profit destination variants',has(seed,"destination:'SAME_DEPOSIT'","destination:'CUSTOMER_SELECTED_ACCOUNT'"));

add('wizard loads PDL policy snapshot',has(ts,'loadPdlPolicySnapshot()','PRODUCT_CHANNEL_RULE','PRODUCT_ELIGIBILITY_RULE','DEPOSIT_PRODUCT_OPENING_RULE','DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA'));
add('wizard loads PDL term and profit policy tables',has(ts,'DEPOSIT_PRODUCT_TERM_RULE','DEPOSIT_PRODUCT_ALLOWED_TERM','PRODUCT_PRICING_RULE','PRODUCT_PRICING_COMPONENT','PRODUCT_RATE_TIER','DEPOSIT_PROFIT_PAYMENT_RULE'));
add('wizard resolves product policy PDL-first',has(ts,'pdlProductPolicy(versionId','channels.length?channels:fallback.channels','withdrawal.length?withdrawal:fallback.media','eligibility.length?eligibility:fallback.customerTypes'));
add('wizard resolves product-specific term options',has(ts,'pdlTerms(versionId','termOptions(){','p?.terms?.length?p.terms:this.currentFamily().terms')&&html.includes('@for(t of termOptions();track t.code)'));
add('wizard resolves product-specific profit payment policy',has(ts,'pdlProfitPayment(versionId','profitPaymentPolicy(){','PAYMENT_FREQUENCY_CODE','PAYMENT_DESTINATION_CODE')&&html.includes('profitPaymentPolicy()?.destination'));
add('payload persists dynamic Product Builder pricing/payment identifiers',has(ts,'const profitPayment=this.profitPaymentPolicy()','PRICING_RULE_ID:profitPayment?.pricingRuleId','PRICING_COMPONENT_ID:profitPayment?.componentId','PROFIT_PAYMENT_RULE_ID:profitPayment?.paymentRuleId'));
add('runtime verifier exists and covers all 12 products',all.every(code=>runtime.includes(`'${code}'`))&&has(runtime,'DPS2_UAT_PRODUCT_PACK_V1_RUNTIME_PASS'));
add('runtime verifier checks openable and negative versions',has(runtime,'openingEligibleVersion(','NEGATIVE_EXPECTATIONS','UAT_PRODUCT_RUNTIME_OK'));
add('runtime verifier checks policy child rows',has(runtime,'PRODUCT_CHANNEL_RULE','PRODUCT_ELIGIBILITY_RULE','DEPOSIT_PRODUCT_OPENING_RULE','DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA','DEPOSIT_PRODUCT_TERM_RULE','DEPOSIT_PRODUCT_ALLOWED_TERM','PRODUCT_PRICING_RULE','DEPOSIT_PROFIT_PAYMENT_RULE'));
add('QA doc contains install and 60-scenario guidance',has(doc,'۱۲ محصول','۶۰ سناریو','prepare-dps2-uat-product-pack-v1.mjs --apply','runtime-dps2-uat-product-pack-v1.mjs'));

for(const c of checks)console.log(`${c.ok?'PASS':'FAIL'} | ${c.name}`);
const failed=checks.filter(c=>!c.ok);
console.log('------------------------------------------------------------');
console.log(`DPS2_UAT_PRODUCT_PACK_V1_STATIC_PASS=${checks.length-failed.length}`);
console.log(`DPS2_UAT_PRODUCT_PACK_V1_STATIC_FAIL=${failed.length}`);
if(failed.length)process.exit(1);
console.log('DPS2_UAT_PRODUCT_PACK_V1_STATIC_BASELINE_PASS');
