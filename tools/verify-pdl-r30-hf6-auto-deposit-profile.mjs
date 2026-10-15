#!/usr/bin/env node
// Non-writing, standalone HF6 policy / wiring regression verifier.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root,p), 'utf8');
const app = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/';
const pdl = 'frontend/src/app/features/product-builder/';
const service = read(app+'ProductBuilderService.java');
const policy = read(app+'DepositProfileIdentityPolicy.java');
const ui = read(pdl+'pdl-table.component.html');
const ts = read(pdl+'pdl-table.component.ts');
const workspace = read(pdl+'product-workspace.component.ts');
const reference = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java');
const repo = read('backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java');
const legacy = read('tools/verify-pb-r13.mjs');
const cmd = read('build-production.cmd'), sh = read('build-production.sh');
let ok = 0, failed = 0;
function check(name, good) {
  if(good){ok++;console.log('PASS | '+name);}
  else {failed++; console.log('FAIL | '+name);}
}
const html = read('docs/reference/Unified_Product_Builder_Interactive_Forms_FA.html');
const modelMatch = html.match(/const MODEL=(\[[^\n]+\]);/);
const model = modelMatch ? JSON.parse(modelMatch[1]) : [];
const profileModel = model.find(x=>x.name==='DEPOSIT_PRODUCT_PROFILE')?.columns || [];
check('Reviewed PDL profile model has exactly two business identity fields',
  profileModel.filter(c=>!c.system).map(c=>c.name).sort().join(',') ===
  'DEPOSIT_GROUP_CODE,DEPOSIT_TYPE_CODE,PRODUCT_VERSION_ID');
check('Five canonical family codes stay in server policy',
  ['QARD_SAVINGS','CURRENT_ACCOUNT','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT','CERTIFICATE_OF_DEPOSIT']
    .every(x=>policy.includes('"'+x+'"')));
check('No family-to-profile duplication inside Angular', !ts.includes('DEPOSIT_PROFILE_BY_FAMILY'));
check('Approved family aliases remain available', policy.includes('"SAVINGS", "QARD_SAVINGS"') && policy.includes('"CURRENT", "CURRENT_ACCOUNT"'));
check('New version is created in an explicit transaction',
  service.includes('@Transactional\n    public Map<String, Object> create('));
const createStart = service.indexOf('    public Map<String, Object> create(');
const updateStart = service.indexOf('    public Map<String, Object> update(', createStart);
const createBlock = service.slice(createStart, updateStart);
check('Profile generation is synchronous after inserting the new version before returning',
  createBlock.indexOf('long id = repository.insert(table, prepared, resolvedActor);')>=0 &&
  createBlock.indexOf('createSystemDepositProfile(id, number(prepared.get("PRODUCT_ID")), resolvedActor);')>
  createBlock.indexOf('long id = repository.insert(table, prepared, resolvedActor);') &&
  createBlock.indexOf('createSystemDepositProfile(')<createBlock.indexOf('return findById(table, id);'));
check('Transaction aborts with invalid reference, no catch-and-ignore',
  service.includes('referenceOptionService.validateChangedValues(descriptor("DEPOSIT_PRODUCT_PROFILE"), profile, null)') &&
  service.includes('repository.insert("DEPOSIT_PRODUCT_PROFILE", profile, actor);') &&
  !service.slice(service.indexOf('private void createSystemDepositProfile('), service.indexOf('/** The FIX96 DPS')).includes('catch ('));
check('Auto-profile includes version, group and type but no fake numeric surrogate key',
  ['PRODUCT_VERSION_ID','DEPOSIT_GROUP_CODE','DEPOSIT_TYPE_CODE'].every(x=>service.includes('profile.put("'+x+'",')) &&
  !service.includes('profile.put("DEPOSIT_TYPE_ID"'));
check('Non-deposit version unaffected', service.includes('if (!"DEPOSIT".equals(text(product.get("PRODUCT_CLASS_CODE")))) return;'));
check('NOSTRO/VOSTRO left on correspondent profile path', service.includes('if (DepositProductCodePolicy.isCorrespondent(family)) return;'));
check('Runtime finalization still looks up real version profile and checks numeric type',
  service.includes('String depositTypeCode = text(profile.get("DEPOSIT_TYPE_CODE"))') &&
  service.includes('DepositProductCodePolicy.numericTypeCode(depositTypeCode)'));
check('Generic CREATE of the system profile refused', createBlock.includes('if ("DEPOSIT_PRODUCT_PROFILE".equals(normalizeTable(table)))') && createBlock.includes('افزودن دستی مجاز نیست'));
check('Generic UPDATE of system profile refused', service.slice(updateStart,service.indexOf('    public Map<String, Object> makeApprovedVersionCurrent(')).includes('پروفایل سپرده سیستمی و فقط خواندنی است'));
check('Generic DELETE of system profile refused', service.includes('حذف مستقل آن مجاز نیست'));
check('Deposit family changes blocked after first version', service.includes('repository.search("PRODUCT_VERSION", null, 0, 1,') && service.includes('خانواده محصول سپرده پس از ایجاد نخستین نسخه ثابت است'));
check('Parent row locked in version create and PRODUCT update to prevent family races',
  service.includes('repository.lockProduct(productId);\n        prepared.remove("APPROVED_AT")') &&
  service.includes('if ("PRODUCT".equals(normalizeTable(table))) repository.lockProduct(id);'));
check('Version ownership cannot be moved after profile created', service.includes('نسخه به محصول دیگری قابل انتقال نیست'));
check('No opportunistic legacy-profile backfill during reads',
  !service.slice(service.indexOf('    public TablePage search('), service.indexOf('    public Map<String, Object> findById(')).includes('createSystemDepositProfile('));
check('No new deposit profile DDL/migration',
  !fs.existsSync(path.join(root,'database/oracle/pdl/migrations/0.11.0-r30-hf6-auto-deposit-profile.sql')));
check('Reference still anchored on active DPS group/type codes',
  reference.includes('Map.entry("DEPOSIT_GROUP_CODE", "dps-deposit-groups")') &&
  reference.includes('Map.entry("DEPOSIT_TYPE_CODE", "dps-deposit-types")'));
check('Repository reuses existing PK allocation, system fields and validation',
  repo.includes('public long insert(String requestedTable, Map<String, Object> values, String actor)') &&
  repo.includes('addSystemInsertColumns(descriptor, actor, columns, placeholders, params)'));
check('Grid shows both persisted fields even for legacy missing labels',
  ts.includes("return ['DEPOSIT_GROUP_CODE', 'DEPOSIT_TYPE_CODE']") &&
  ts.includes('`کد ثبت‌شده: ${String(value)}`'));
check('No Add control is displayed on profile page',
  ui.includes("@if (tableName() !== 'DEPOSIT_PRODUCT_PROFILE')") &&
  ui.indexOf('@if (tableName() !== \'DEPOSIT_PRODUCT_PROFILE\')') < ui.indexOf('editorOpening()'));
check('Profile row actions (edit / clone / delete) hidden',
  ui.includes('<td class="actions-cell">') &&
  ui.includes("@if (tableName() !== 'DEPOSIT_PRODUCT_PROFILE') {\n                  <td class=\"actions-cell\">"));
check('Disabled profile CRUD remains guarded at method entry points',
  ['async create(): Promise<void>','async edit(row:','async clone(row:','async save(): Promise<void>','async remove(row:']
    .every(key=>{const i=ts.indexOf(key);return i>=0 && ts.slice(i,i+230).includes("this.tableName() === 'DEPOSIT_PRODUCT_PROFILE'");}));
check('No Angular-side deposit group/type assignment used for saving',
  !ts.includes('DEPOSIT_PROFILE_BY_FAMILY') && !ts.includes('resolveDepositProfileContext()'));
check('Legacy profile missing warning does not insert anything',
  ui.includes('نسخه‌های قدیمی به‌صورت خودکار بازنویسی نمی‌شوند'));
check('NOSTRO/VOSTRO profile editor stays separate',
  workspace.includes("{table: 'CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE'") &&
  ui.includes("tableName() === 'CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE'"));
check('Workspaces still expose a profile read-only link', workspace.includes('پروفایل سپرده (سیستمی)'));
check('Historical PB-R13 verifier recognizes centralized server policy', legacy.includes('profilePolicy'));
check('HF6 wired to both production build wrappers',
  cmd.includes('verify-pdl-r30-hf6-auto-deposit-profile.mjs') &&
  sh.includes('verify-pdl-r30-hf6-auto-deposit-profile.mjs'));
// TypeScript and Angular template syntax checks: no source build or Oracle writes.
try {
  const tsModule = await import('/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript/lib/typescript.js');
  const tsCompiler = tsModule.default || tsModule;
  for(const component of ['pdl-table.component.ts','product-workspace.component.ts']){
    const src=read(pdl+component);
    const parsed=tsCompiler.createSourceFile(component,src,tsCompiler.ScriptTarget.Latest,true);
    check('TypeScript parses: '+component,parsed.parseDiagnostics.length===0);
  }
}catch(e){ console.log('INFO | Global TypeScript compiler unavailable: '+String(e)); }
// Execute pure Java identity policy against 5 approved deposit families, 2 aliases,
// invalid/correspondent inputs and HF5 numeric issuance policy.
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-hf6-'));
try{
  const j=path.join(tmp,'DepositProfileIdentityPolicyCheck.java');
  fs.writeFileSync(j,`import com.behsazan.corebanking.productbuilder.application.*;\nimport java.util.Arrays;\npublic class DepositProfileIdentityPolicyCheck {\n  static int pass=0;\n  static void ok(String name,boolean v){if(!v)throw new AssertionError(name); pass++; System.out.println("PASS | "+name);}\n  static boolean rejects(String family){try{DepositProfileIdentityPolicy.forFamily(family);return false;}catch(ProductBuilderValidationException ex){return true;}}\n  static void verify(String family,String group,String type,String prefix){String[] p=DepositProfileIdentityPolicy.forFamily(family);ok("identity "+family,Arrays.equals(p,new String[]{group,type}));ok("HF5 numeric "+family,DepositProductCodePolicy.format(p[1],1405,1).equals(prefix+"1405001"));}\n  public static void main(String[] a){\n    verify("QARD_SAVINGS","QARD_HASAN","SAVINGS","3510");\n    verify("CURRENT_ACCOUNT","CURRENT","CURRENT_NO_INTEREST","2510");\n    verify("SHORT_TERM_DEPOSIT","SHORT_TERM","SHORT_TERM_DEPOSIT","4510");\n    verify("LONG_TERM_DEPOSIT","LONG_TERM","LONG_TERM_DEPOSIT","5510");\n    verify("CERTIFICATE_OF_DEPOSIT","CERTIFICATE","CERTIFICATE_OF_DEPOSIT","6510");\n    verify(" savings ","QARD_HASAN","SAVINGS","3510");\n    verify("CURRENT","CURRENT","CURRENT_NO_INTEREST","2510");\n    ok("no unspecified TERM",rejects("TERM"));\n    ok("no unknown",rejects("UNKNOWN"));\n    ok("no empty",rejects(""));\n    ok("no null",rejects(null));\n    ok("no NOSTRO",rejects("NOSTRO_ACCOUNT"));\n    ok("no VOSTRO",rejects("VOSTRO_ACCOUNT"));\n    ok("returns independent values",DepositProfileIdentityPolicy.forFamily("SAVINGS")!=DepositProfileIdentityPolicy.forFamily("SAVINGS"));\n    ok("loan unchanged",LoanProductCodePolicy.format("10",1405,2).equals("101405002"));\n    System.out.println("PDL_R30_HF6_JAVA_PASS="+pass+" FAIL=0");\n  }\n}`);
  const src=['ProductBuilderValidationException.java','DepositProfileIdentityPolicy.java','DepositProductCodePolicy.java','LoanProductCodePolicy.java']
    .map(name=>path.join(root,app,name));
  for(const [bin,args] of [['javac',['-encoding','UTF-8','-d',tmp,...src,j]],['java',['-cp',tmp,'DepositProfileIdentityPolicyCheck']]]){
    const r=spawnSync(bin,args,{encoding:'utf8'});
    if(r.error || r.status!==0) throw new Error(`${bin} failed: ${r.stderr||r.stdout||r.error}`);
    process.stdout.write(r.stdout||'');
  }
} finally{ fs.rmSync(tmp,{recursive:true,force:true}); }
console.log(`PDL_R30_HF6_STATIC_PASS=${ok} FAIL=${failed}`);
if(failed) process.exitCode=1;
