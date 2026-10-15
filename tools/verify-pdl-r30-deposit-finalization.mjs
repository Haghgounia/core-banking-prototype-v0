/** R30: system-only product codes in BOTH deposit and loan domains. No Oracle writes. */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=(p)=>fs.readFileSync(path.join(root,p),'utf8');
const app='backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const front='frontend/src/app/features/product-builder/';
const svc=read(app+'application/ProductBuilderService.java');
const repo=read(app+'oracle/PdlProductBuilderRepository.java');
const controller=read(app+'web/ProductBuilderController.java');
const policy=read(app+'application/DepositProductCodePolicy.java');
const api=read(front+'product-builder.service.ts');
const component=read(front+'product-workspace.component.ts');
const html=read(front+'product-workspace.component.html');
const ddl=read('database/oracle/pdl/migrations/0.11.0-r30-deposit-final-product-code.sql');
let passed=0, failed=0;
const check=(name,condition)=>{if(condition)passed++;else failed++;console.log(`${condition?'PASS':'FAIL'} | ${name}`);};

check('No editable product code in stage-one identity (any domain)',!html.includes('formControlName="PRODUCT_CODE"'));
check('No hidden HTML manual product code',!html.includes('name="PRODUCT_CODE"'));
check('Code FormControl remains readonly/disabled internally',component.includes('code.disable({emitEvent:false})') && !component.includes('code.enable({emitEvent:false})'));
check('Both new deposit and loan products explicitly save code-less',svc.includes('prepared.put("PRODUCT_CODE", null); // R30'));
check('Frontend submits blank code for either new draft',component.includes("if (id === null) {\n        // R30") && component.includes("values['PRODUCT_CODE'] = ''"));
check('Backend rejects any user provided product code',svc.includes('کد محصول سپرده و تسهیلات فقط پس از نهایی‌سازی موفق مرحله ششم'));
check('Only DRAFT product can be initially saved without code',svc.includes('محصول فاقد کد فقط در وضعیت پیش‌نویس قابل ثبت است'));
check('Generic update prevents new status without issued code',svc.includes('تا پیش از صدور کد نهایی، وضعیت محصول سپرده یا تسهیلات باید پیش‌نویس باشد'));
check('Code changes rejected in generic update',svc.includes('prepared.remove("PRODUCT_CODE")') && svc.includes('prepared.containsKey("PRODUCT_CODE")'));
check('Deposit issued family is immutable',svc.includes('repository.registeredDepositFamily(id).isPresent()'));
check('Deposit and loan domain conversions forbidden',svc.includes('if (!oldDomain.equals(newDomain))'));
check('Loan R29 finalization retained',svc.includes('public Map<String, Object> finalizeLoanProduct(long productId, long versionId'));
check('R30 deposit stage-six finalization is transactionally scoped',svc.includes('@Transactional\n    public Map<String, Object> finalizeDepositProduct('));
check('Deposit API requires explicit numeric positive version id',controller.includes('@PostMapping("/products/{productId}/finalize-deposit")') && controller.includes('service.finalizeDepositProduct(productId, versionId, actor)') && controller.includes('versionId <= 0'));
check('Deposit product row locked before reading code',(()=>{const method=svc.slice(svc.indexOf('public Map<String, Object> finalizeDepositProduct('),svc.indexOf('    @Transactional\n    public void delete('));return method.indexOf('repository.lockProduct(productId)')>=0 && method.indexOf('repository.lockProduct(productId)')<method.indexOf('String existingCode = text(product.get("PRODUCT_CODE"))');})());
check('Deposit version row locked before approval',svc.includes('repository.lockProductVersion(versionId)'));
check('Version belongs to product at deposit finalization',svc.includes('نسخه انتخاب‌شده متعلق به این محصول سپرده نیست'));
check('Approved ACTIVE current version required',svc.includes('LoanCodeIssuanceGate.requireApprovedCurrent('));
check('Exactly one current version for deposit',svc.includes('repository.currentProductVersionCount(productId) != 1L'));
check('Profile belongs to version and exactly one',svc.includes('profiles.totalElements() != 1 || profiles.items().size() != 1'));
check('Correspondent deposit follows correspondent profile',svc.includes('DepositProductCodePolicy.isCorrespondent(family)') && svc.includes('CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE'));
check('Deposit product and profile business validated',svc.includes('businessValidator.validate(profileTable, profile)'));
check('Governance approval checked before allocation',svc.lastIndexOf('LoanCodeIssuanceGate.requireGovernance(')<svc.indexOf('repository.allocateDepositProductCode(family, depositTypeCode)'));
check('Deposit finalization never claims operational opening',svc.includes('"PRODUCT_STATUS_CODE", "INACTIVE"'));
check('Deposit product code and registry registered transactionally',svc.includes('repository.registerDepositProductCode(productId, issued, resolvedActor)'));
check('Repeated issuance does not consume serial',svc.includes('"newlyIssued", false') && svc.includes('"newlyIssued", true'));
check('Bank year is shared with loan, without a separate year configuration',repo.includes('int year = activeLoanProductWorkingYear(); // SELECT FOR UPDATE'));
check('Deposit numbering serial derived per approved numeric type and year',repo.includes('PRODUCT_DEPOSIT_TYPE_CODE_REGISTRY WHERE ISSUANCE_TYPE_CODE = :typeCode') && repo.includes('AND WORK_YEAR = :workYear'));
check('Serial max 999 and existing product collision checked',repo.includes('for (int serial = maximum + 1; serial <= 999; serial++)') && repo.includes('WHERE PRODUCT_CODE = :code'));
check('Two transactions serialized by working year row lock',repo.includes('SETTING_ID = 1 FOR UPDATE'));
check('No frontend code-formatting or local serial calculation',!component.includes('padStart(3') && !component.includes('String.format'));
check('Deposit family still determines the profile but is not the issuance prefix',svc.includes('String family = text(product.get("PRODUCT_FAMILY_CODE"))'));
check('Numeric deposit code formatting strictly server-side',policy.includes('return prefix + String.format(Locale.ROOT, "%04d%03d", workingYear, serial)'));
check('UI stage six offers issuance for both domains',html.includes('(click)="finalizeProduct()"') && html.includes('hasIssuedProductCode()'));
check('UI calls dedicated deposit and loan APIs',component.includes('this.service.finalizeDepositProduct(productId, versionId)') && component.includes('this.service.finalizeLoanProduct(productId, versionId)'));
check('UI displays actual persisted code response',component.includes('${issued.productCode}') && component.includes('await this.load()'));
check('UI never allows activating uncoded deposit or loan',html.includes('@if (hasIssuedProductCode())'));
check('Stage six displays code only after it exists',html.includes("productForm.controls.PRODUCT_CODE.value || 'هنوز صادر نشده'"));
check('R30 migration retains UQ_PRODUCT_CODE',ddl.includes('Retain UQ_PRODUCT_CODE') && !ddl.includes('DROP CONSTRAINT UQ_PRODUCT_CODE'));
check('R30 migration does not reseed year or change historical codes',!ddl.includes('UPDATE PDL.PRODUCT_LOAN_WORK_YEAR') && !ddl.includes('UPDATE PDL.PRODUCT SET'));
check('R30 migration guarded by R29 prerequisite',ddl.includes('requires successful R29 code issuance migration'));
check('New wider constraint added BEFORE R29 guard removed',ddl.indexOf('ADD CONSTRAINT CK_PDL_PRODUCT_CODE_DRAFT')<ddl.indexOf('DROP CONSTRAINT CK_PDL_PRODUCT_CODE_ISSUED'));
check('New constraint only permits NULL for DRAFT',ddl.includes("PRODUCT_STATUS_CODE = ''DRAFT''"));
check('Deposit registry has product FK and serial uniqueness',ddl.includes('FK_PDL_PDCR_PRODUCT FOREIGN KEY') && ddl.includes('UK_PDL_PDCR_FAMILY_YEAR_NO UNIQUE'));
check('Deposit registry has unique code and serial range',ddl.includes('UK_PDL_PDCR_CODE UNIQUE') && ddl.includes('CHECK (SERIAL_NO BETWEEN 1 AND 999)'));
check('Migration is re-runnable with both new and old check guards',ddl.includes('CK_PDL_PRODUCT_CODE_DRAFT') && ddl.includes('IF v_count = 0 THEN'));
check('R30 verifier included in Windows build',read('build-production.cmd').includes('verify-pdl-r30-deposit-finalization.mjs'));
check('R30 verifier included in Linux build',read('build-production.sh').includes('verify-pdl-r30-deposit-finalization.mjs'));

let ts;
for(const pkg of [path.join(root,'frontend/package.json'),path.join(root,'package.json')]){
 try{ts=createRequire(pkg)('typescript');break;}catch{}
}
try{ts??=await import('/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript/lib/typescript.js');}catch{}
if(ts){
 for(const filename of ['product-workspace.component.ts','product-builder.service.ts']){
   const file=ts.createSourceFile(filename,read(front+filename),ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
   check('Typescript parses '+filename,file.parseDiagnostics.length===0);
 }
}

const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-r30-java-'));
try {
 const javaTest=path.join(tmp,'R30CodePolicyTest.java');
 fs.writeFileSync(javaTest,`import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
public class R30CodePolicyTest {
 static int passed=0;
 static void ok(String label,boolean v){if(!v)throw new AssertionError(label);passed++;System.out.println("PASS | "+label);}
 static boolean fails(Runnable r){try{r.run();return false;}catch(ProductBuilderValidationException e){return true;}}
 public static void main(String[] args){
  ok("savings numeric type first code",DepositProductCodePolicy.format("SAVINGS",1405,1).equals("35101405001"));
  ok("savings numeric type second code",DepositProductCodePolicy.format("SAVINGS",1405,2).equals("35101405002"));
  ok("savings next working year",DepositProductCodePolicy.format("SAVINGS",1406,1).equals("35101406001"));
  ok("current numeric type",DepositProductCodePolicy.format("CURRENT_NO_INTEREST",1405,1).equals("25101405001"));
  ok("short term numeric type",DepositProductCodePolicy.format("SHORT_TERM_DEPOSIT",1405,1).equals("45101405001"));
  ok("long term numeric type",DepositProductCodePolicy.format("LONG_TERM_DEPOSIT",1405,1).equals("55101405001"));
  ok("certificate numeric type",DepositProductCodePolicy.format("CERTIFICATE_OF_DEPOSIT",1405,999).equals("65101405999"));
  ok("nostro has no approved type code",fails(()->DepositProductCodePolicy.format("NOSTRO_ACCOUNT",1405,1)));
  ok("vostro has no approved type code",fails(()->DepositProductCodePolicy.format("VOSTRO_ACCOUNT",1405,1)));
  ok("unknown type rejected",fails(()->DepositProductCodePolicy.format("UNKNOWN",1405,1)));
  ok("numeric reference ID 8 is not business type code",fails(()->DepositProductCodePolicy.format("8",1405,1)));
  ok("legacy numeric 2 not an approved issuance type",fails(()->DepositProductCodePolicy.format("2",1405,1)));
  ok("NULL type rejected",fails(()->DepositProductCodePolicy.format(null,1405,1)));
  ok("serial zero rejected",fails(()->DepositProductCodePolicy.format("SAVINGS",1405,0)));
  ok("serial 1000 rejected",fails(()->DepositProductCodePolicy.format("SAVINGS",1405,1000)));
  ok("working year 1299 rejected",fails(()->DepositProductCodePolicy.format("SAVINGS",1299,1)));
  ok("working year 1601 rejected",fails(()->DepositProductCodePolicy.format("SAVINGS",1601,1)));
  ok("correspondent classification preserved",DepositProductCodePolicy.isCorrespondent("NOSTRO_ACCOUNT") && DepositProductCodePolicy.isCorrespondent("VOSTRO_ACCOUNT"));
  ok("retail not correspondent",!DepositProductCodePolicy.isCorrespondent("QARD_SAVINGS"));
  ok("draft precondition accepted",!fails(()->LoanCodeIssuanceGate.requireDraft("DRAFT")));
  ok("non-draft precondition denied",fails(()->LoanCodeIssuanceGate.requireDraft("ACTIVE")));
  ok("version approved current active accepted",!fails(()->LoanCodeIssuanceGate.requireApprovedCurrent("APPROVED",true,"ACTIVE")));
  ok("unapproved version denied",fails(()->LoanCodeIssuanceGate.requireApprovedCurrent("DRAFT",true,"ACTIVE")));
  ok("non-current version denied",fails(()->LoanCodeIssuanceGate.requireApprovedCurrent("APPROVED",false,"ACTIVE")));
  ok("blocked governance denied",fails(()->LoanCodeIssuanceGate.requireGovernance(false,List.of("blocked"))));
  ok("good governance accepted",!fails(()->LoanCodeIssuanceGate.requireGovernance(true,List.of())));
  ok("existing loan code unchanged",LoanProductCodePolicy.format("12",1405,1).equals("121405001"));
  System.out.println("PDL_R30_JAVA_GATE_PASS="+passed+" FAIL=0");
 }
}`);
 const src=path.join(root,app,'application');
 function run(cmd,args){const res=spawnSync(cmd,args,{encoding:'utf8'});
  if(res.error)throw res.error; if(res.status!==0)throw new Error(`${cmd}: ${res.stderr||res.stdout}`);process.stdout.write(res.stdout||'');}
 run('javac',['-encoding','UTF-8','-d',tmp,...['DepositProductCodePolicy.java','LoanCodeIssuanceGate.java','LoanProductCodePolicy.java','ProductBuilderValidationException.java'].map(n=>path.join(src,n)),javaTest]);
 run('java',['-cp',tmp,'R30CodePolicyTest']);
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
console.log(`PDL_R30_STATIC_PASS=${passed} FAIL=${failed}`);
if(failed)process.exitCode=1;
