/** R30-HF2: non-writing source and pure-Java current-version governance gate regression. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root,p),'utf8');
const java = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const service = read(java+'application/ProductBuilderService.java');
const policy = read(java+'application/ProductVersionCurrentGate.java');
const repository = read(java+'oracle/PdlProductBuilderRepository.java');
const controller = read(java+'web/ProductBuilderController.java');
const ui = 'frontend/src/app/features/product-builder/';
const component = read(ui+'product-workspace.component.ts');
const html = read(ui+'product-workspace.component.html');
const api = read(ui+'product-builder.service.ts');
const migration = read('database/oracle/pdl/migrations/0.11.0-r30-hf2-current-version-governance.sql');
const guard = read(java+'application/ProductGovernanceWriteGuard.java');
const result = [];
function test(title, condition){result.push(Boolean(condition));console.log(`${condition?'PASS':'FAIL'} | ${title}`);}
test('Explicit independent make-current REST operation',controller.includes('@PostMapping("/products/{productId}/versions/{versionId}/make-current")'));
test('Reasons are validated at API and service gate',controller.includes('request.get("reason")')&&service.includes('ProductVersionCurrentGate.requireRequest(productId, versionId, reason)'));
test('Dedicated service method uses transaction',service.includes('@Transactional\n    public Map<String, Object> makeApprovedVersionCurrent('));
test('Product row is locked before changing current versions', service.indexOf('repository.lockProduct(productId);')<service.indexOf('repository.currentProductVersionId(productId)'));
test('Target version locked before approval',service.indexOf('repository.lockProductVersion(versionId);')<service.indexOf('var approval = governance.report(versionId)'));
test('Version belongs to product',service.includes('number(version.get("PRODUCT_ID")) != productId'));
test('Approved and ACTIVE status required',policy.includes('"APPROVED".equals(versionStatus)')&&policy.includes('"ACTIVE".equals(recordStatus)'));
test('Recorded approval audit required',service.includes('version.get("APPROVED_AT") != null')&&service.includes('version.get("APPROVED_BY")'));
test('Date validity controlled by Oracle day',service.includes('repository.currentDatabaseDate()')&&policy.includes('validFrom.isAfter(bankToday)'));
test('Governance approval re-evaluated before flag update',service.includes('governance.report(versionId)')&&service.includes('ProductVersionCurrentGate.requireGovernance(approval.canApprove(), approval.blockers())'));
test('Live product OPEN gate still requires canOpen',service.includes('"OPEN".equals(text(version.get("ORIGINATION_STATUS_CODE")))')&&service.includes('!approval.canOpen()'));
test('Retired and deleted products denied',policy.includes('deleted || "RETIRED".equals(status)'));
test('Existing single-current guard',service.includes('ProductVersionCurrentGate.requireSingleCurrent(count)'));
test('Idempotent replay has no audit write',service.indexOf('"alreadyCurrent", true')<service.indexOf('repository.appendProductVersionCurrentAudit('));
test('Old current cleared before new current set',service.indexOf('repository.update("PRODUCT_VERSION", previousId, Map.of("IS_CURRENT", 0)')<service.indexOf('repository.update("PRODUCT_VERSION", versionId, Map.of("IS_CURRENT", 1)'));
test('Only flags changed, not product activation nor code issuance',!service.substring(service.indexOf('public Map<String, Object> makeApprovedVersionCurrent('),service.indexOf('/** Only the approved, current loan configuration')).includes('repository.update("PRODUCT"')); 
test('Audit insert within same transaction',service.indexOf('repository.appendProductVersionCurrentAudit(')>service.indexOf('repository.update("PRODUCT_VERSION", versionId, Map.of("IS_CURRENT", 1)'));
test('Generic new versions cannot set IS_CURRENT',service.includes('if (number(values.get("IS_CURRENT")) == 1)'));
test('Generic updates cannot promote IS_CURRENT',service.includes('number(existing.get("IS_CURRENT")) != 1'));
test('Approved version generic guard remains stop only',guard.includes('assertNonDraftVersionStopOnly')&&guard.includes('فعالسازی مجدد نسخه مصوب'));
test('Migration R30 prerequisite',migration.includes("CONSTRAINT_NAME='CK_PDL_PRODUCT_CODE_DRAFT'"));
test('Migration refuses duplicated current versions',migration.includes('HAVING COUNT(*)>1')&&migration.includes('RAISE_APPLICATION_ERROR(-20141'));
test('Oracle function-based unique current index',migration.includes('CREATE UNIQUE INDEX PDL.UX_PDL_PV_ONE_CURRENT')&&migration.includes('CASE WHEN IS_CURRENT=1 THEN PRODUCT_ID END'));
test('Oracle audit table records previous, target, reason, actor and date',['PRODUCT_VERSION_CURRENT_AUDIT','PREVIOUS_VERSION_ID','TARGET_VERSION_ID','CHANGE_REASON','ACTOR','CHANGED_AT'].every(x=>migration.includes(x)));
test('Migration contains no business-data UPDATE',!/\bUPDATE\s+PDL\./i.test(migration));
test('Migration idempotent checks',migration.includes("TABLE_NAME='PRODUCT_VERSION_CURRENT_AUDIT'")&&migration.includes("INDEX_NAME='UX_PDL_PV_ONE_CURRENT'"));
test('Frontend invokes dedicated API',api.includes('/make-current')&&component.includes('this.service.makeApprovedVersionCurrent('));
test('Stage two exposes governed action with reason',html.includes('تعیین نسخه جاری')&&html.includes('[formGroup]="currentVersionForm"')&&html.includes('formControlName="reason"')&&html.includes('(ngSubmit)="makeApprovedVersionCurrent()"'));
test('Current checkbox removed from generic version editor',!html.includes('formControlName="IS_CURRENT"')&&html.includes('current-flag-readonly'));
test('Generic edit does not submit a stale IS_CURRENT flag',component.includes("if (id !== null) delete payload['IS_CURRENT']"));
test('Review directs user back to stage two when not current',html.includes('canMakeSelectedVersionCurrent() && !hasIssuedProductCode()')&&html.includes('رفتن به نسخه‌بندی'));
test('All existing finalization endpoints remain',controller.includes('/finalize-loan')&&controller.includes('/finalize-deposit'));
test('Windows production build verifies HF2',read('build-production.cmd').includes('verify-pdl-r30-hf2-current-version.mjs'));
test('Linux production build verifies HF2',read('build-production.sh').includes('verify-pdl-r30-hf2-current-version.mjs'));
// Syntax-only TypeScript parse using global or installed package. No network.
let ts;
for(const packagePath of [path.join(root,'frontend','package.json'),'/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript/package.json']){
  try {ts=createRequire(packagePath)('typescript'); break;}catch{}
}
if(ts){for(const file of ['product-workspace.component.ts','product-builder.service.ts']){
  const doc=ts.createSourceFile(file,read(ui+file),ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
  test('TypeScript parses '+file,doc.parseDiagnostics.length===0);
}}
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'r30-hf2-current-'));
try {
const testFile=path.join(tmp,'ProductVersionCurrentGateCheck.java');
fs.writeFileSync(testFile,`import java.time.*;
import com.behsazan.corebanking.productbuilder.application.*;
public class ProductVersionCurrentGateCheck {
 static int ok=0;
 static boolean fail(Runnable f){try{f.run();return false;}catch(ProductBuilderValidationException e){return true;}}
 static void check(String what,boolean valid){if(!valid)throw new AssertionError(what);ok++;System.out.println("PASS | "+what);}
 static final LocalDate NOW=LocalDate.of(2026,10,12);
 public static void main(String[] args){
   check("Valid request",!fail(()->ProductVersionCurrentGate.requireRequest(31,35,"governed promotion")));
   check("Reject missing reason",fail(()->ProductVersionCurrentGate.requireRequest(31,35,"")));
   check("Reject too short reason",fail(()->ProductVersionCurrentGate.requireRequest(31,35,"abc")));
   check("Reject oversized reason",fail(()->ProductVersionCurrentGate.requireRequest(31,35,"x".repeat(501))));
   check("Reject zero product ID",fail(()->ProductVersionCurrentGate.requireRequest(0,35,"governed promotion")));
   check("Reject zero version ID",fail(()->ProductVersionCurrentGate.requireRequest(31,0,"governed promotion")));
   check("DRAFT product allowed",!fail(()->ProductVersionCurrentGate.requireProduct("DRAFT",false)));
   check("INACTIVE coded product allowed",!fail(()->ProductVersionCurrentGate.requireProduct("INACTIVE",false)));
   check("ACTIVE product allowed subject to runtime gate",!fail(()->ProductVersionCurrentGate.requireProduct("ACTIVE",false)));
   check("RETIRED product rejected",fail(()->ProductVersionCurrentGate.requireProduct("RETIRED",false)));
   check("Deleted product rejected",fail(()->ProductVersionCurrentGate.requireProduct("DRAFT",true)));
   check("Approved active audited version accepted",!fail(()->ProductVersionCurrentGate.requireVersion("APPROVED","ACTIVE",true)));
   check("DRAFT version rejected",fail(()->ProductVersionCurrentGate.requireVersion("DRAFT","ACTIVE",true)));
   check("REJECTED version rejected",fail(()->ProductVersionCurrentGate.requireVersion("REJECTED","ACTIVE",true)));
   check("Inactive approved version rejected",fail(()->ProductVersionCurrentGate.requireVersion("APPROVED","INACTIVE",true)));
   check("Missing approval record rejected",fail(()->ProductVersionCurrentGate.requireVersion("APPROVED","ACTIVE",false)));
   check("Valid today accepted",!fail(()->ProductVersionCurrentGate.requireEffective(NOW,null,NOW)));
   check("Valid bounded interval accepted",!fail(()->ProductVersionCurrentGate.requireEffective(NOW,NOW,NOW)));
   check("Future version rejected",fail(()->ProductVersionCurrentGate.requireEffective(NOW.plusDays(1),null,NOW)));
   check("Expired version rejected",fail(()->ProductVersionCurrentGate.requireEffective(NOW.minusDays(5),NOW.minusDays(1),NOW)));
   check("Inverted dates rejected",fail(()->ProductVersionCurrentGate.requireEffective(NOW,NOW.minusDays(1),NOW)));
   check("Null start rejected",fail(()->ProductVersionCurrentGate.requireEffective(null,null,NOW)));
   check("No prior current allowed",!fail(()->ProductVersionCurrentGate.requireSingleCurrent(0)));
   check("One prior current allowed",!fail(()->ProductVersionCurrentGate.requireSingleCurrent(1)));
   check("Duplicate current rejected",fail(()->ProductVersionCurrentGate.requireSingleCurrent(2)));
   check("Approved governance accepted",!fail(()->ProductVersionCurrentGate.requireGovernance(true,java.util.List.of())));
   check("Blocked governance rejected",fail(()->ProductVersionCurrentGate.requireGovernance(false,java.util.List.of("missing rule"))));
   System.out.println("PDL_R30_HF2_JAVA_PASS="+ok+" FAIL=0");
 }
}`, 'utf8');
const sources = [java+'application/ProductBuilderValidationException.java',java+'application/ProductVersionCurrentGate.java'].map(x=>path.join(root,x));
const compiler=spawnSync('javac',['-encoding','UTF-8','-d',tmp,...sources,testFile],{encoding:'utf8'});
if(compiler.status!==0)throw new Error('javac error: '+compiler.stdout+' '+compiler.stderr);
const exec=spawnSync('java',['-cp',tmp,'ProductVersionCurrentGateCheck'],{encoding:'utf8'});
if(exec.status!==0)throw new Error('java policy test error: '+exec.stdout+' '+exec.stderr);
process.stdout.write(exec.stdout);
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
const failed=result.filter(x=>!x).length;
console.log(`PDL_R30_HF2_STATIC_PASS=${result.length-failed} FAIL=${failed}`);
if(failed)process.exitCode=1;
