/** HF7-A1: compile and execute REAL rule contracts and governance service with
 * tiny framework/repository stubs. No Oracle connection or data modifications. */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const base='backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const get=(f)=>fs.readFileSync(path.join(root,f),'utf8');
let ok=0;
function check(label,test){if(!test)throw Error('FAIL | '+label);console.log('PASS | '+label);ok++;}
const business=get(base+'application/ProductBuilderBusinessValidator.java');
const service=get(base+'application/ProductBuilderService.java');
const governance=get(base+'application/ProductRuleGovernanceService.java');
const evidence=get(base+'application/ProductRuleEvidenceEvaluator.java');
const contract=get(base+'application/ProductRuleSemanticContract.java');
const form=get('frontend/src/app/features/product-builder/pdl-table.component.ts');
const html=get('frontend/src/app/features/product-builder/pdl-table.component.html');
check('CREATE/UPDATE/eligibility all use central business validation',
 service.includes('businessValidator.validate(table, prepared)') && service.includes('businessValidator.validate(table, merged)') &&
 service.includes('businessValidator.validate("PRODUCT_ELIGIBILITY_RULE", prepared)') &&
 business.includes('ProductRuleSemanticContract.validate(table, values)'));
check('Read-only semantic revalidation before canApprove calculation',
 governance.includes('businessValidator.validate(control.code(), rule)') &&
 governance.indexOf('boolean canApprove = blockers.isEmpty();')>governance.indexOf('boolean invalidSemantics ='));
check('Historic rows filtered; future scheduled rows audited against full version interval',
 governance.includes('isApplicableWithin(control.code(), rule, versionStart, versionEnd)') &&
 evidence.includes('public static boolean isApplicableWithin('));
check('New governance status distinguishable from row count',
 governance.includes('"INVALID_SEMANTICS"') && governance.includes('boolean publishBlocked = invalidSemantics ||'));
check('No automatic Oracle changes in audit',
 !governance.includes('repository.insert(') && !governance.includes('repository.update('));
check('No invented unbounded status in Oracle or unsafe NULL fallback',
 contract.includes('requireAnyCap(') && !service.includes('put("LIMIT_POLICY_CODE"'));
check('Operational LOAN and DEPOSIT canOpen deny retained',
 governance.includes('if ("LOAN".equals(domain)) {\n            canOpen = false;') &&
 governance.includes('if ("DEPOSIT".equals(domain)') && governance.includes('canOpen = false;'));
check('Frontend fast feedback maps to backend contract',
 form.includes('depositLimitValidationIssue()') && form.includes('const depositLimitIssue = this.depositLimitValidationIssue()') &&
 html.includes('قرارداد ایمنی ثبت قاعده HF7-A1'));
// TypeScript is optional in a source-only package before `npm install`.
// Never depend on the Linux author's global Node path on a Windows build host.
const require=createRequire(import.meta.url);
let ts=null;
const globalRoot=spawnSync('npm',['root','-g'],{encoding:'utf8',timeout:8000});
const candidates=[
 path.join(root,'frontend/node_modules/typescript'),
 path.join(root,'node_modules/typescript'),
 ...(globalRoot.status===0 ? [path.join(globalRoot.stdout.trim(),'typescript')] : [])
];
for (const candidate of candidates) { try { ts=require(candidate); break; } catch { /* not installed yet */ } }
if (ts) {
 const parsed=ts.createSourceFile('pdl-table.component.ts',form,ts.ScriptTarget.Latest,true);
 check('TypeScript syntax',parsed.parseDiagnostics.length===0);
} else console.log('SKIP | TypeScript syntax (typescript is not installed; Angular build remains required)');
check('Approval audit covers both deposit and loan rule domains',
 ['DEPOSIT_PRODUCT_TRANSACTION_RULE','DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA','LOAN_PRODUCT_REPAYMENT_RULE','LOAN_PRODUCT_PROCESS_RULE','LOAN_PRODUCT_COLLATERAL_RULE']
 .every(t=>contract.includes('"'+t+'"')));
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-r30-hf7a1-'));
const write=(p,s)=>{const t=path.join(tmp,p);fs.mkdirSync(path.dirname(t),{recursive:true});fs.writeFileSync(t,s);return t;};
try {
 const stubSources=[];
 stubSources.push(write('org/springframework/stereotype/Component.java',
   'package org.springframework.stereotype; public @interface Component {}'));
 stubSources.push(write('org/springframework/stereotype/Service.java',
   'package org.springframework.stereotype; public @interface Service {}'));
 stubSources.push(write('org/springframework/beans/factory/annotation/Value.java',
   'package org.springframework.beans.factory.annotation; public @interface Value {String value();}'));
 stubSources.push(write('com/behsazan/corebanking/productbuilder/application/RuleGovernanceLiteService.java',`
package com.behsazan.corebanking.productbuilder.application;
import java.time.LocalDate;import java.util.List;
public class RuleGovernanceLiteService {
 public record Policy(long id, String code, long versionNo, String name, String state,
  LocalDate validFrom, LocalDate validTo,String maker,String checker,String approvalReference,long controlCount) {}
 public record Control(long id,String code,String title,String category,String module,String presence,
  String absence,boolean runtimeRequired,String description) {}
 public Policy policy(long id){throw new UnsupportedOperationException();}
 public List<Control> controls(long id){throw new UnsupportedOperationException();}
}`));
 stubSources.push(write('com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java',`
package com.behsazan.corebanking.productbuilder.oracle;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.application.ProductRuleGovernanceFixture;
import java.util.*;
public class PdlProductBuilderRepository {
 public boolean incompleteTx=false,incompleteLoanProcess=false;
 public String domain="DEPOSIT"; public String family="CURRENT_ACCOUNT";
 public boolean loanTypePlanAllowed(long loanType,long planType){return loanType==101L && planType==201L;}
 public Optional<Map<String,Object>> findById(String table,long id){
  if(table.equals("PRODUCT_VERSION")) return Optional.of(Map.of("PRODUCT_ID",9L,"VALID_FROM","2026-10-10","VERSION_STATUS_CODE","DRAFT"));
  if(table.equals("PRODUCT")) return Optional.of(Map.of("PRODUCT_CLASS_CODE",domain,"PRODUCT_FAMILY_CODE",family,"PRODUCT_STATUS_CODE","DRAFT","DEFAULT_CURRENCY_CODE","IRR"));
 
  if(table.equals("PRODUCT") && id==99L)return Optional.of(Map.of("PRODUCT_ID",99L));
  if(table.equals("LOAN_TYPE"))return Optional.of(Map.of("LOAN_GROUP",1,"RECORD_STATUS_CODE","ACTIVE"));
  if(table.equals("PLAN_TYPE"))return Optional.of(Map.of("RECORD_STATUS_CODE","ACTIVE"));
 return Optional.empty();
 }
 public TablePage search(String table,String text,int page,int size,String filterCol,String filterVal){
  if(table.equals("PRODUCT_VERSION_MODULE")) return new TablePage(List.of(),0,0,size);
  // HF6: historical smoke fixture has no optional child records; this does
  // not represent permission or runtime integration.
  if(Set.of("PRODUCT_CHANNEL_OPERATION","LOAN_ELIGIBILITY_EXTENSION",
         "DEPOSIT_PRODUCT_CLOSURE_PRECHECK","DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",
         "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE","PRODUCT_ELIGIBILITY_CRITERION","CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE").contains(table))
    return new TablePage(List.of(),0,page,size);
  if(page>0) return new TablePage(List.of(),0,page,size);
  Map<String,Object> row = ProductRuleGovernanceFixture.row(table);
  if(incompleteTx && table.equals("DEPOSIT_PRODUCT_TRANSACTION_RULE")){
    row = new LinkedHashMap<>(row);row.put("IS_ALLOWED",1);
  }
  if(incompleteLoanProcess && table.equals("LOAN_PRODUCT_PROCESS_RULE")){
    row = new LinkedHashMap<>(row);row.remove("ALLOW_OVERRIDE");
  }
  return new TablePage(List.of(row),1,page,size);
 }
}`));
 // The fixture is package-private in the project and needs a public facade in the stub.
 // A public class of the same name is never generated; modify only this copy of the fixture.
 const fixture=get('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceFixture.java')
  .replace('final class ProductRuleGovernanceFixture {','public final class ProductRuleGovernanceFixture {')
  .replace('    static Map<String, Object> row(String table) {','    public static Map<String, Object> row(String table) {');
 stubSources.push(write('com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceFixture.java',fixture));
 stubSources.push(write('Hf7A1SemanticCheck.java',`
import com.behsazan.corebanking.productbuilder.application.*;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import java.util.*;
public class Hf7A1SemanticCheck {
 static int pass=0;
 static void yes(String label,boolean value){if(!value)throw new AssertionError(label);pass++;System.out.println("PASS | Java: "+label);}
 static void invalid(ProductBuilderBusinessValidator v,String table,Map<String,Object> row,String why){
  try{v.validate(table,row);throw new AssertionError("Invalid policy accepted: "+table+"/"+why);}
  catch(ProductBuilderValidationException e){yes(why,e.getMessage()!=null&&!e.getMessage().isBlank());}
 }
 static Map<String,Object> row(String table){return new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table));}
 public static void main(String[] args){
  var v=new ProductBuilderBusinessValidator();
  var tx=row("DEPOSIT_PRODUCT_TRANSACTION_RULE");
  v.validate("DEPOSIT_PRODUCT_TRANSACTION_RULE",tx);yes("denied transaction without limits",true);
  tx.put("IS_ALLOWED",1);invalid(v,"DEPOSIT_PRODUCT_TRANSACTION_RULE",tx,"no cap on allowed transaction");
  tx.put("MAX_TRANSACTION_AMOUNT",1000);v.validate("DEPOSIT_PRODUCT_TRANSACTION_RULE",tx);yes("bounded allowed transaction",true);
  tx.put("MIN_TRANSACTION_AMOUNT",2000);invalid(v,"DEPOSIT_PRODUCT_TRANSACTION_RULE",tx,"invalid transaction bounds");
  tx.put("MIN_TRANSACTION_AMOUNT",500);tx.put("DAILY_COUNT_LIMIT",1.5);
  invalid(v,"DEPOSIT_PRODUCT_TRANSACTION_RULE",tx,"nonintegral transaction count");
  tx.put("IS_ALLOWED",0);invalid(v,"DEPOSIT_PRODUCT_TRANSACTION_RULE",tx,"denied transaction with limits");
  var media=row("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA");media.put("IS_ALLOWED",1);
  invalid(v,"DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",media,"uncapped withdrawal media");
  media.put("DAILY_COUNT_LIMIT",2);v.validate("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",media);yes("bounded withdrawal media",true);
  var opening=row("DEPOSIT_PRODUCT_OPENING_RULE");v.validate("DEPOSIT_PRODUCT_OPENING_RULE",opening);yes("NULL financial opening limits",true);
  opening.put("IS_STAMP_DUTY_APPLICABLE",1);invalid(v,"DEPOSIT_PRODUCT_OPENING_RULE",opening,"stamp requires count");
  var joint=row("DEPOSIT_PRODUCT_JOINT_RULE");joint.put("MIN_REQUIRED_SIGNER_COUNT",4);
  invalid(v,"DEPOSIT_PRODUCT_JOINT_RULE",joint,"impossible joint signature policy");
  var financial=row("LOAN_FINANCIAL_EXTENSION");financial.put("MIN_CUSTOMER_CONTRIBUTION","0.2");
  invalid(v,"LOAN_FINANCIAL_EXTENSION",financial,"missing matching loan contribution bound");
  financial.put("MAX_CUSTOMER_CONTRIBUTION","0.3");v.validate("LOAN_FINANCIAL_EXTENSION",financial);
  yes("paired loan contribution ratios",true);
  financial.put("MAX_CUSTOMER_CONTRIBUTION",30);
  invalid(v,"LOAN_FINANCIAL_EXTENSION",financial,"loan contribution not a ratio");
  var repayment=row("LOAN_PRODUCT_REPAYMENT_RULE");repayment.put("GRACE_ALLOWED",1);
  repayment.put("GRACE_UNIT_CODE","MONTH");repayment.put("GRACE_METHOD_CODE","NO_PAYMENT");
  invalid(v,"LOAN_PRODUCT_REPAYMENT_RULE",repayment,"grace without duration");
  repayment.put("MAX_GRACE_DURATION",3);v.validate("LOAN_PRODUCT_REPAYMENT_RULE",repayment);
  yes("bounded loan grace",true);
  var process=row("LOAN_PRODUCT_PROCESS_RULE");process.put("COMMITTEE_APPROVAL_REQUIRED",1);
  process.put("MANUAL_APPROVAL_REQUIRED",1);invalid(v,"LOAN_PRODUCT_PROCESS_RULE",process,"two competing approval paths");
  var h=row("DEPOSIT_PRODUCT_HOLD_RULE");h.put("MAX_HOLD_AMOUNT",100);
  invalid(v,"DEPOSIT_PRODUCT_HOLD_RULE",h,"unsupported hold has amount");
  var med1=row("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA");
  med1.put("VALID_FROM","2026-10-10");med1.put("VALID_TO","2026-10-12");
  var med2=new LinkedHashMap<>(med1);
  med2.put("VALID_FROM","2026-10-13");med2.put("VALID_TO","2026-10-20");
  var sDate=java.time.LocalDate.parse("2026-10-10");
  var eDate=java.time.LocalDate.parse("2026-10-31");
  yes("nonoverlapping withdrawal media versions permitted",
    !ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",List.of(med1,med2),sDate,eDate).conflict());
  med2.put("VALID_FROM","2026-10-12");
  yes("overlapping withdrawal media versions rejected",
    ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_WITHDRAWAL_MEDIA",List.of(med1,med2),sDate,eDate).conflict());
  var repo = new PdlProductBuilderRepository();
  var gov = new ProductRuleGovernanceService(repo,new RuleGovernanceLiteService(),v);
  var report=gov.report(55);yes("complete deposit design canApprove",report.canApprove());
  yes("deposit canOpen remains denied",!report.canOpen());
  repo.incompleteTx=true;report=gov.report(55);
  yes("legacy incomplete transaction blocks approval",!report.canApprove());
  yes("incomplete transaction reason exposed",report.effectiveRules().stream().anyMatch(r->r.code().equals("DEPOSIT_PRODUCT_TRANSACTION_RULE")&&r.status().equals("INVALID_SEMANTICS")&&r.blocking()));
  repo.domain="LOAN";repo.family="QARD_HASAN_LOAN";repo.incompleteTx=false;
  report=gov.report(55);yes("valid loan design canApprove",report.canApprove());
  yes("loan canOpen remains denied",!report.canOpen());
  repo.incompleteLoanProcess=true;report=gov.report(55);
  yes("legacy invalid loan process blocks design",!report.canApprove());
  System.out.println("PDL_R30_HF7A1_JAVA_PASS="+pass+" FAIL=0");
 }
}`));
 const app=path.join(root,base,'application');
 const actual=['ProductBuilderValidationException.java','ProductRuleSemanticContract.java',
   'ProductBuilderBusinessValidator.java','ProductRuleEvidenceEvaluator.java','ProductRuleHierarchyAudit.java','DepositProfileIdentityPolicy.java','LoanTypeClassification.java','ProductRuleGovernanceService.java']
   .map(name=>path.join(app,name));
 actual.push(path.join(root,base,'domain/ProductBuilderModels.java'));
 const bin=path.join(tmp,'classes');fs.mkdirSync(bin);
 for(const [cmd,args] of [
  ['javac',['-encoding','UTF-8','-d',bin,...actual,...stubSources]],
  ['java',['-cp',bin,'Hf7A1SemanticCheck']]
 ]){
   const result=spawnSync(cmd,args,{encoding:'utf8',maxBuffer:2*1024*1024});
   if(result.stdout)process.stdout.write(result.stdout);
   if(result.error||result.status!==0)throw Error(cmd+' failed '+result.status+' '+(result.stderr||result.error));
 }
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
console.log(`PDL_R30_HF7A1_STATIC_PASS=${ok} FAIL=0`);
