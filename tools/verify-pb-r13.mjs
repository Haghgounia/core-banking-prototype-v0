import {readFileSync} from 'node:fs';
import {resolve, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const files = {
  ts: 'frontend/src/app/features/product-builder/pdl-table.component.ts',
  html: 'frontend/src/app/features/product-builder/pdl-table.component.html',
  css: 'frontend/src/app/features/product-builder/pdl-table.component.scss',
  ref: 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java',
  service: 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductBuilderService.java',
  fee: 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlDormancyFeePlanReference.java',
  dps: 'backend/src/main/java/com/behsazan/corebanking/deposit/productfactory/reference/DepositProductReferenceDescriptorProvider.java',
  seed: 'database/oracle/dps/migrations/0.3.88-fix96-unified-product-builder-reference-seed.sql',
  reference: 'docs/reference/Unified_Product_Builder_Interactive_Forms_FA.html'
};
const s = Object.fromEntries(Object.entries(files).map(([k,v]) => [k,readFileSync(resolve(root,v),'utf8')]));

// Extract the literal JSON model from the independently reviewed reference HTML.
function referenceModel(text) {
  const start = text.indexOf('const MODEL=');
  if (start < 0) throw new Error('Reference HTML has no MODEL');
  const begin = text.indexOf('[',start);
  let depth=0,quoted=false,escape=false;
  for (let i=begin;i<text.length;i++) {
    const ch=text[i];
    if (quoted) {
      if (escape) escape=false;
      else if (ch==='\\') escape=true;
      else if (ch==='"') quoted=false;
    } else if (ch==='"') quoted=true;
    else if (ch==='[') depth++;
    else if (ch===']' && --depth===0) return JSON.parse(text.slice(begin,i+1));
  }
  throw new Error('Invalid reference HTML MODEL');
}
const model=referenceModel(s.reference);
const tables=['DEPOSIT_PRODUCT_PROFILE','DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA','DEPOSIT_PRODUCT_JOINT_RULE','DEPOSIT_PRODUCT_DORMANCY_RULE'];
const columnsOf=name=>model.find(m=>m.name===name)?.columns ?? [];
const fieldsFor=(table,names)=>names.every(name=>columnsOf(table).some(c=>c.name===name));
const mapped=(col,resource)=>s.ref.includes(`Map.entry("${col}", "${resource}")`) && s.dps.includes(`spec("${resource}",`);
const checks=[
  ['Reference HTML includes all four reviewed deposit forms',tables.every(n=>model.some(m=>m.name===n))],
  ['FIX96 deposit identity is derived from the official five families',
    ['QARD_SAVINGS','CURRENT_ACCOUNT','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT','CERTIFICATE_OF_DEPOSIT'].every(v=>s.ts.includes(v)&&s.service.includes(v))],
  ['Frontend always renders deposit group and type without editable inputs',
    s.html.includes('isDepositProfileIdentity(column)')&&s.html.includes('depositProfileIdentityLabel(column)')&&s.ts.includes('group.disable({emitEvent: false})')&&s.ts.includes('type.disable({emitEvent: false})')],
  ['New profile identity is resolved and validated against active DPS reference values',
    s.ts.includes('this.profileIdentityValid()')&&s.ts.includes('this.baseOptions(column)')&&s.service.includes('prepared.put("DEPOSIT_GROUP_CODE", identity[0])')],
  ['Backend prohibits changing existing profile identity',
    s.service.includes('"DEPOSIT_PRODUCT_PROFILE".equals(normalizeTable(table))')&&s.service.includes('prepared.remove(code)')],
  ['No ambiguous generic TERM is guessed for a new profile',
    !s.ts.includes("TERM: ['TERM'")&&!s.service.includes('case "TERM" ->')],
  ['Group and type source references exist in the real DPS catalog',
    mapped('DEPOSIT_GROUP_CODE','dps-deposit-groups')&&mapped('DEPOSIT_TYPE_CODE','dps-deposit-types')],
  ['Withdrawal media is rendered as a governed selector with reference labels',
    mapped('WITHDRAWAL_MEDIA_CODE','dps-withdrawal-media')&&s.html.includes('isSelect(column)')&&s.html.includes('<mat-select [formControl]="control(column)"')],
  ['Joint account owner, signing and profit distribution use governed DPS references',
    mapped('OWNERSHIP_TYPE_CODE','dps-ownership-types')&&mapped('SIGNING_RULE_CODE','dps-signing-rules')&&mapped('PROFIT_DISTRIBUTION_CODE','dps-profit-distributions')],
  ['Joint banking field labels are understandable',
    ['نوع حساب از نظر مالکیت','شرط امضا برای برداشت','نحوه تقسیم سود بین صاحبان حساب'].every(v=>s.ts.includes(v))],
  ['Dormancy units and reactivation method all use DPS reference data',
    mapped('INACTIVITY_PERIOD_UNIT_CODE','dps-inactivity-period-units')&&mapped('WARNING_PERIOD_UNIT_CODE','dps-warning-period-units')&&mapped('REACTIVATION_METHOD_CODE','dps-reactivation-methods')],
  ['Dormancy unit comes before value for both periods (also in HTML reference)',
    ['INACTIVITY_PERIOD_UNIT_CODE','INACTIVITY_PERIOD_VALUE','WARNING_PERIOD_UNIT_CODE','WARNING_PERIOD_VALUE'].every(n=>s.ts.includes(n))&&
    s.ts.indexOf('INACTIVITY_PERIOD_UNIT_CODE: 10')<s.ts.indexOf('INACTIVITY_PERIOD_VALUE: 20')&&
    s.ts.indexOf('WARNING_PERIOD_UNIT_CODE: 30')<s.ts.indexOf('WARNING_PERIOD_VALUE: 40')&&
    columnsOf('DEPOSIT_PRODUCT_DORMANCY_RULE').findIndex(c=>c.name==='INACTIVITY_PERIOD_UNIT_CODE')<columnsOf('DEPOSIT_PRODUCT_DORMANCY_RULE').findIndex(c=>c.name==='INACTIVITY_PERIOD_VALUE')],
  ['Dormancy auto-reactivation is a real toggle, not an editable numeric code',
    s.ts.includes('MatSlideToggleModule')&&s.html.includes('<mat-slide-toggle')&&s.html.includes('isDormancyAutoField(column)')],
  ['Maintenance fee plan selector is backed by actual active FEE version IDs',
    s.ref.includes('feePlanReference.options()')&&s.fee.includes('FEE_DEFINITION_VERSION_ID')&&s.fee.includes("V.STATUS_CODE = 'ACTIVE'")&&s.fee.includes("D.IS_ACTIVE = 'Y'")&&fieldsFor('DEPOSIT_PRODUCT_DORMANCY_RULE',['MAINTENANCE_FEE_PLAN_ID'])],
  ['Reference option validation does not invent missing DPS options',
    s.ref.includes('intersection(column.options(), governed.stream()')&&s.ref.includes('return governed.stream().filter')&&s.ts.includes('referenceUnavailable(column)')],
  ['FEE fee-plan SQL follows the actual Oracle schema (not fictitious status columns)',
    !s.fee.includes('LIFECYCLE_STATUS_CODE')&&!s.fee.includes('D.STATUS_CODE')&&
    s.fee.includes('rs.getString("VERSION_NO")')&&s.fee.includes('V.FEE_PLAN_NAME')],
  ['Missing DPS reference cannot be replaced by a surrogate FK lookup',
    s.ts.includes('/^(DPS:|FEE:)/.test(column.referenceSource')&&
    s.ts.includes('!this.isDepositProfileIdentity(column) && column.referenceControlled')],
  ['Backend checks controlled options for create and changed values on update',
    s.service.includes('referenceOptionService.validateChangedValues(descriptor(table), prepared, null)')&&
    s.service.includes('referenceOptionService.validateChangedValues(descriptor(table), prepared, existing)')],
  ['Original Clone and delete-confirm safeguards remain in place',
    s.html.includes('(click)="clone(row)"')&&s.ts.includes('window.confirm(')&&s.ts.includes('this.service.delete(this.tableName(), id)')],
  ['Previously requested document and inquiry usability remain',
    s.ts.includes("column.name === 'PROCESS_STEP_NO'")&&s.html.includes('inquiryAgeUnit')]
];
let fail=0;
for(const [name,ok] of checks){console.log(`${ok?'PASS':'FAIL'} | ${name}`);if(!ok)fail++;}
console.log(`PB_R13_REFERENCE_PARITY_PASS=${checks.length-fail} FAIL=${fail}`);
if(fail) process.exitCode=1;
