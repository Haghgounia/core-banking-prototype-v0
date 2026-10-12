/** R29: issuance only upon full stage-six approval; no eagerly allocated loan code. */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const app='backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const front='frontend/src/app/features/product-builder/';
const service=read(app+'application/ProductBuilderService.java');
const repo=read(app+'oracle/PdlProductBuilderRepository.java');
const controller=read(app+'web/ProductBuilderController.java');
const gate=read(app+'application/LoanCodeIssuanceGate.java');
const api=read(front+'product-builder.service.ts');
const component=read(front+'product-workspace.component.ts');
const html=read(front+'product-workspace.component.html');
const migration=read('database/oracle/pdl/migrations/0.11.0-r29-loan-product-finalization.sql');
const r30=fs.existsSync(path.join(root,'database/oracle/pdl/migrations/0.11.0-r30-deposit-final-product-code.sql'));
let passed=0, failed=0;
const check=(label,condition)=>{if(condition)passed++;else failed++;console.log(`${condition?'PASS':'FAIL'} | ${label}`);};
check('R29 migration checks prior R28 registry',migration.includes('PRODUCT_LOAN_CODE_REGISTRY'));
check('R29 migration permits nullable draft loan product code',migration.includes('MODIFY (PRODUCT_CODE NULL)'));
check('R29 migration adds narrow conditional code requirement',migration.includes("PRODUCT_CLASS_CODE = ''LOAN'' AND PRODUCT_STATUS_CODE = ''DRAFT''"));
check('R29 migration leaves UQ_PRODUCT_CODE intact',!migration.includes('DROP CONSTRAINT UQ_PRODUCT_CODE') && migration.includes('KEEP PDL.UQ_PRODUCT_CODE'));
check('R29 migration idempotently checks constraint',migration.includes('CK_PDL_PRODUCT_CODE_ISSUED') && migration.includes('IF v_constraint=0 THEN'));
check('R29 migration never resets bank year or serial',!migration.includes('UPDATE PDL.PRODUCT_LOAN_WORK_YEAR') && !migration.includes('DELETE FROM PDL.PRODUCT_LOAN_CODE_REGISTRY'));
check('DRAFT loan code is explicitly NULL at create',service.includes('prepared.put("PRODUCT_CODE", null)'));
check('No product code generation in generic create',!service.substring(service.indexOf('public Map<String, Object> create('),service.indexOf('public Map<String, Object> update(')).includes('allocateLoanProductCode('));
check('Draft creation never stores a type specifically for early code',service.includes('prepared.containsKey("LOAN_TYPE_ID")'));
check('No manual loan code possible at create',service.includes(r30 ? 'کد محصول سپرده و تسهیلات فقط پس از نهایی‌سازی موفق مرحله ششم' : 'کد محصول تسهیلات فقط پس از نهایی‌سازی موفق مرحله ششم'));
check('No activation of uncoded loan through generic update',service.includes(r30 ? 'تا پیش از صدور کد نهایی، وضعیت محصول سپرده یا تسهیلات باید پیش‌نویس باشد' : 'تا پیش از صدور کد نهایی، وضعیت محصول تسهیلات باید پیش‌نویس باشد'));
check(r30 ? 'Deposit draft is code-less by design' : 'Deposit code remains required by the server',
    service.includes(r30 ? 'prepared.put("PRODUCT_CODE", null); // R30' : 'کد محصول سپرده الزامی است'));
check('Product domain change still guarded',service.includes('تبدیل دامنه سپرده/تسهیلات مجاز نیست'));
check('Issued code stays immutable via general update',service.includes('prepared.remove("PRODUCT_CODE")') && service.includes('کد محصول و عقد سازنده پس از ایجاد محصول'));
check('Stage-six finalization owns transaction',service.includes('@Transactional\n    public Map<String, Object> finalizeLoanProduct('));
check('Dedicated server operation exists',controller.includes('@PostMapping("/products/{productId}/finalize-loan")'));
check('VersionId validated at API boundary',controller.includes('new java.math.BigDecimal(String.valueOf(requestedVersion)).longValueExact()'));
check('Lock product before reading its code',service.indexOf('repository.lockProduct(productId)')<service.indexOf('String existingCode = text(product.get("PRODUCT_CODE"))'));
check('Lock version before governance approval checks',service.indexOf('repository.lockProductVersion(versionId)')<service.indexOf('LoanCodeIssuanceGate.requireApprovedCurrent('));
check('Product row lock uses Oracle FOR UPDATE',repo.includes('WHERE PRODUCT_ID = :productId FOR UPDATE'));
check('Version row lock uses Oracle FOR UPDATE',repo.includes('WHERE PRODUCT_VERSION_ID = :versionId FOR UPDATE'));
check('Idempotent repeated code issuance',service.includes('"newlyIssued", false') && service.includes('"newlyIssued", true'));
check('Finalization checks version belongs to product',service.includes('number(version.get("PRODUCT_ID")) != productId'));
check('Finalization requires approved current ACTIVE version',service.includes('LoanCodeIssuanceGate.requireApprovedCurrent('));
check('Finalization requires exactly one loan profile',service.includes('LoanCodeIssuanceGate.requireSingleProfile('));
check('Only one current product version is allowed before issuance',service.includes('repository.currentProductVersionCount(productId) != 1L') && repo.includes('WHERE PRODUCT_ID = :productId AND IS_CURRENT = 1'));
check('Loan type comes from approved profile, not early input',service.includes('requiredLoanId(profile, "LOAN_TYPE_ID")'));
check('Finalization checks loan contract active and group',service.includes('expectedGroup != number(loanType.get("LOAN_GROUP"))') && service.includes('loanType.get("RECORD_STATUS_CODE")'));
check('Finalization checks profile and identity currency',service.includes('loanProfileCurrency(versionId, loanTypeId)'));
check('Finalization revalidates profile/reference mapping and party nature',service.includes('validateLoanReferenceRows("LOAN_PRODUCT_PROFILE", profile, null)') && service.includes('businessValidator.validate("LOAN_PRODUCT_PROFILE", profile)'));
check('Code issuance requires design approval, not unproven origination runtime',service.includes('LoanCodeIssuanceGate.requireGovernance(readiness.canApprove(), readiness.blockers())') && service.includes('"PRODUCT_STATUS_CODE", "INACTIVE"'));
check('Code is allocated only after governance checks',service.indexOf('LoanCodeIssuanceGate.requireGovernance(readiness.canApprove()')<service.indexOf('repository.allocateLoanProductCode(loanTypeId'));
check('Transactional product status is INACTIVE (not OPEN)',service.includes('Map.of("PRODUCT_CODE", issued.code(), "PRODUCT_STATUS_CODE", "INACTIVE")'));
check('Code and registry are persisted in same transaction',service.includes('repository.registerLoanProductCode(productId, issued, resolvedActor)'));
check('Working year and loan type remain serially locked',repo.includes('lockLoanTypeForMapping(typeId)') && repo.includes('SETTING_ID = 1 FOR UPDATE'));
check('Serial still follows loan type and working year',repo.includes('WHERE LOAN_TYPE_ID = :typeId AND WORK_YEAR = :workYear'));
check('Loan identity input is never user editable',component.includes('code.disable({emitEvent:false})') &&
    (r30 ? !html.includes('formControlName="PRODUCT_CODE"') : html.includes('[readonly]="!isDeposit()"')));
check('Loan product status stays DRAFT until issuance',html.includes(r30 ? '@if (hasIssuedProductCode())' : '@if (isDeposit() || hasIssuedLoanCode())'));
check('No stage-one premature contract selector',!html.includes('نوع عقد برای تولید کد محصول (الزامی)'));
check('Loan code issuance button only on stage six',html.includes(r30 ? '(click)="finalizeProduct()"' : '(click)="finalizeLoanProduct()"') && html.includes('نهایی‌سازی و صدور کد محصول'));
check('Frontend never formats a code or a serial',!component.includes('String.format') && !component.includes('padStart(3'));
check('Frontend calls only the dedicated finalization API',api.includes('/finalize-loan') && component.includes('this.service.finalizeLoanProduct(productId, versionId)'));
check('Issuance message includes actual response code',component.includes('${issued.productCode}') && component.includes('issued.newlyIssued'));
check('Identity code remains blank during draft', r30
    ? !html.includes('formControlName="PRODUCT_CODE"') && component.includes("values['PRODUCT_CODE'] = ''")
    : html.includes('پس از نهایی‌سازی مرحله ششم صادر می‌شود'));
check('Windows build includes R29 verifier',read('build-production.cmd').includes('verify-pdl-r29-finalization.mjs'));
check('Linux build includes R29 verifier',read('build-production.sh').includes('verify-pdl-r29-finalization.mjs'));
let ts;
for(const entry of [path.join(root,'frontend','package.json'),path.join(root,'package.json')]){
  try{ts=createRequire(entry)('typescript');break;}catch{}
}
if (ts) {
 for (const file of ['product-workspace.component.ts','product-builder.service.ts']) {
   const source=ts.createSourceFile(file,read(front+file),ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
   check('Typescript parses '+file,source.parseDiagnostics.length===0);
 }
}
const scratch=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-r29-java-'));
try{
  const test=path.join(scratch,'R29GateTest.java');
  fs.writeFileSync(test,`import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
public class R29GateTest {
 static int passed=0;
 static void ok(String s,boolean v){if(!v)throw new AssertionError(s);passed++;System.out.println("PASS | "+s);}
 static boolean rejects(Runnable action){try{action.run();return false;}catch(ProductBuilderValidationException e){return true;}}
 public static void main(String[] args){
  ok("DRAFT accepted", !rejects(()->LoanCodeIssuanceGate.requireDraft("DRAFT")));
  ok("ACTIVE without code rejected",rejects(()->LoanCodeIssuanceGate.requireDraft("ACTIVE")));
  ok("INACTIVE without code rejected",rejects(()->LoanCodeIssuanceGate.requireDraft("INACTIVE")));
  ok("blank status rejected",rejects(()->LoanCodeIssuanceGate.requireDraft("")));
  ok("approved current active accepted",!rejects(()->LoanCodeIssuanceGate.requireApprovedCurrent("APPROVED",true,"ACTIVE")));
  ok("DRAFT version rejected",rejects(()->LoanCodeIssuanceGate.requireApprovedCurrent("DRAFT",true,"ACTIVE")));
  ok("IN_REVIEW version rejected",rejects(()->LoanCodeIssuanceGate.requireApprovedCurrent("IN_REVIEW",true,"ACTIVE")));
  ok("approved non-current rejected",rejects(()->LoanCodeIssuanceGate.requireApprovedCurrent("APPROVED",false,"ACTIVE")));
  ok("inactive approved version rejected",rejects(()->LoanCodeIssuanceGate.requireApprovedCurrent("APPROVED",true,"INACTIVE")));
  ok("single loan profile accepted",!rejects(()->LoanCodeIssuanceGate.requireSingleProfile(1)));
  ok("zero loan profiles rejected",rejects(()->LoanCodeIssuanceGate.requireSingleProfile(0)));
  ok("multiple loan profiles rejected",rejects(()->LoanCodeIssuanceGate.requireSingleProfile(2)));
  ok("governance design approval accepted",!rejects(()->LoanCodeIssuanceGate.requireGovernance(true,List.of())));
  ok("governance approval denied",rejects(()->LoanCodeIssuanceGate.requireGovernance(false,List.of("no"))));
  ok("runtime blockers do not prevent issuance of an INACTIVE identity",!rejects(()->LoanCodeIssuanceGate.requireGovernance(true,List.of("runtime unresolved"))));
  ok("empty blocker list still rejects negative approval",rejects(()->LoanCodeIssuanceGate.requireGovernance(false,List.of())));
  ok("loan code format remains exact",LoanProductCodePolicy.format("12",1405,1).equals("121405001"));
  ok("serial increments",LoanProductCodePolicy.format("12",1405,2).equals("121405002"));
  ok("overflow rejected",rejects(()->LoanProductCodePolicy.format("12",1405,1000)));
  System.out.println("PDL_R29_JAVA_GATE_PASS="+passed+" FAIL=0");
 }
}`);
  const run=(command,args)=>{const p=spawnSync(command,args,{encoding:'utf8'});if(p.error)throw p.error;if(p.status!==0)throw new Error(`${command}: ${p.stdout} ${p.stderr}`);process.stdout.write(p.stdout||'');};
  const source=app+'application/';
  run('javac',['-encoding','UTF-8','-d',scratch,...['LoanCodeIssuanceGate.java','LoanProductCodePolicy.java','ProductBuilderValidationException.java'].map(n=>path.join(root,source,n)),test]);
  run('java',['-cp',scratch,'R29GateTest']);
}finally{fs.rmSync(scratch,{recursive:true,force:true});}
console.log(`PDL_R29_STATIC_PASS=${passed} FAIL=${failed}`);
if(failed)process.exitCode=1;
