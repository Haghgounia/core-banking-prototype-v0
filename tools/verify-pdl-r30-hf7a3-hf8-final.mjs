/** R30-HF7-A3 Final/HF8: real GovernanceService re-audit for four controls.
 * Runs actual Java source, no Oracle connection, no writes; JUnit source compile is supplemental.
 */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const app='backend/src/main/java/com/behsazan/corebanking/productbuilder/application/';
const tests='backend/src/test/java/com/behsazan/corebanking/productbuilder/application/';
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const governance=read(app+'ProductRuleGovernanceService.java');
const hierarchy=read(app+'ProductRuleHierarchyAudit.java');
const validator=read(app+'ProductBuilderBusinessValidator.java');
const test=read(tests+'ProductRuleHierarchyHF7A3HF7Test.java');
let staticCount=0;
function check(label,condition){if(!condition)throw Error('FAIL | '+label);staticCount++;console.log('PASS | '+label);}
check('both correspondent families have explicit closed operational gate',
  governance.includes('if ("DEPOSIT".equals(domain) && Set.of("NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT").contains(family))') &&
  governance.includes('کنترل انتشار عملیاتی نوسترو/وسترو'));
check('retail deposit and loan runtime denials unchanged',
  governance.includes('if ("DEPOSIT".equals(domain) && !Set.of("NOSTRO_ACCOUNT", "VOSTRO_ACCOUNT").contains(family))') &&
  governance.includes('if ("LOAN".equals(domain)) {\n            canOpen = false;'));
check('eligibility parent audit includes criterion and loan extension',
  hierarchy.includes('"PRODUCT_ELIGIBILITY_CRITERION", "LOAN_ELIGIBILITY_EXTENSION"') &&
  hierarchy.includes('new String[]{"PRODUCT_ELIGIBILITY_CRITERION"}'));
check('correspondent parent reads settlement by actual foreign key',
  hierarchy.includes('case "CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE"') &&
  hierarchy.includes('childTables = new String[]{"CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE"}'));
check('correspondent audit is connected to real report',
  governance.includes('"CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE" ->') &&
  governance.includes('invalidRows += relationAudit.invalidCount()'));
check('family/account type mismatch blocks approval',
  governance.includes('expectedAccountType.equals(text(rule.get("ACCOUNT_TYPE_CODE")))'));
check('original criterion reference-service validation remains',
  read(app+'ProductBuilderService.java').includes('referenceOptionService.validateEligibilityCriterion(criterion)'));
check('normalized criterion has shape and gender-only checks',
  validator.includes('case "PRODUCT_ELIGIBILITY_CRITERION" ->') &&
  hierarchy.includes('معیار جنسیت فقط برای دامنه صرفاً مشتری حقیقی'));
check('correspondent cutoff, frequency, netting validated',
  validator.includes('"SETTLEMENT_FREQUENCY_CODE"') && validator.includes('"NETTING_ALLOWED"') &&
  validator.includes('CUTOFF_TIME'));
check('duplicates and invalid child status fail closed',
  hierarchy.includes('معیار اهلیت تکراری') && hierarchy.includes('unrecognizedStatuses() > 0'));
check('no production SQL/Oracle write in hierarchy audit',
  !/(executeUpdate|jdbcClient|DataSource|\b(?:insert|update|delete|merge)\s*\()/i.test(hierarchy));
check('Windows build includes new verifier',read('build-production.cmd').includes('verify-pdl-r30-hf7a3-hf7-closure.mjs'));
check('Unix build includes new verifier',read('build-production.sh').includes('verify-pdl-r30-hf7a3-hf7-closure.mjs'));
check('JUnit new tests assert real canApprove and canOpen',test.includes('assertPromotionAllowed') &&
  test.includes('scenario.report()'));

const approval=read(app+'ProductRuleSemanticContract.java');
const evaluator=read(app+'ProductRuleEvidenceEvaluator.java');
const unit=read(tests+'ProductRulePublicationHF7A3HF8Test.java');
for (const table of ['DEPOSIT_PRODUCT_PROFILE','DEPOSIT_PROFIT_PAYMENT_RULE','LOAN_PRODUCT_PROFILE','PRODUCT_RELATIONSHIP'])
  check('HF8 approval re-audit '+table, approval.includes('"'+table+'"'));
check('HF8 actual persisted identity rechecked',governance.includes('DepositProfileIdentityPolicy.forFamily(family)'));
check('HF8 loan type active and compatible rechecked',governance.includes('LoanTypeClassification.groupForProductFamily(family)') && governance.includes('loanTypePlanAllowed(typeId, planId)'));
check('HF8 target version ownership checked',governance.includes('SOURCE_PRODUCT_VERSION_ID') && governance.includes('TARGET_PRODUCT_VERSION_ID'));
check('HF8 bank profit payment codes from FIX96 checked',governance.includes('PRORATE_FROM_OPENING') && governance.includes('FIXED_DAY_OF_MONTH'));
check('HF8 relationship duplicate check has exact business key',evaluator.includes('"PRODUCT_RELATIONSHIP" -> List.of("RELATIONSHIP_TYPE_CODE"'));
check('HF8 profit payment is single-version control',governance.includes('"PAYMENT_PERIOD", true, true'));
check('HF8 JUnit tests contain adverse promotion and source reference checks',unit.includes('assertPromotionAllowed') && unit.includes('relationshipTargetVersionOwnershipRechecked'));
check('HF8 keeps operational denials',governance.includes('if ("LOAN".equals(domain)) {\n            canOpen = false;') && governance.includes('کنترل انتشار عملیاتی نوسترو/وسترو'));
check('HF8 Windows production wrapper',read('build-production.cmd').includes('verify-pdl-r30-hf7a3-hf8-final.mjs'));
check('HF8 Unix production wrapper',read('build-production.sh').includes('verify-pdl-r30-hf7a3-hf8-final.mjs'));
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'hf7a3-hf7-'));
function put(rel,contents){let p=path.join(tmp,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,contents);return p;}
function run(bin,args){const r=spawnSync(bin,args,{encoding:'utf8',maxBuffer:6*1024*1024,timeout:95000});
 if(r.stdout)process.stdout.write(r.stdout);if(r.status!==0||r.error){if(r.stderr)process.stderr.write(r.stderr);throw Error(bin+' failed: '+(r.error?.message||r.stderr||r.status));}}
try {
 const base='com/behsazan/corebanking/productbuilder/';
 const spring=[
  put('org/springframework/stereotype/Component.java','package org.springframework.stereotype; public @interface Component {}'),
  put('org/springframework/stereotype/Service.java','package org.springframework.stereotype; public @interface Service {}'),
  put('org/springframework/beans/factory/annotation/Value.java','package org.springframework.beans.factory.annotation; public @interface Value{String value();}')
 ];
 const lite=put(base+'application/RuleGovernanceLiteService.java',String.raw`
package com.behsazan.corebanking.productbuilder.application;
import java.time.LocalDate;import java.util.List;
public class RuleGovernanceLiteService {
 public record Policy(long id,String code,long versionNo,String name,String state,
  LocalDate validFrom,LocalDate validTo,String maker,String checker,String approvalReference,long controlCount) {}
 public record Control(long id,String code,String title,String category,String module,String presence,
  String absence,boolean runtimeRequired,String description) {}
 public Policy policy(long id){throw new UnsupportedOperationException();}
 public List<Control> controls(long id){throw new UnsupportedOperationException();}
}`);
 const fixture=read(tests+'ProductRuleGovernanceFixture.java')
   .replace('final class ProductRuleGovernanceFixture {','public final class ProductRuleGovernanceFixture {')
   .replace('    static Map<String, Object> row(String table) {','    public static Map<String, Object> row(String table) {');
 const fixturePath=put(base+'application/ProductRuleGovernanceFixture.java',fixture);
 const repository=put(base+'oracle/PdlProductBuilderRepository.java',String.raw`
package com.behsazan.corebanking.productbuilder.oracle;
import java.util.*;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.application.ProductRuleGovernanceFixture;
public class PdlProductBuilderRepository {
 public String domain="DEPOSIT",family="CURRENT_ACCOUNT";
 public String failingChild=null, failingReference=null;
 public boolean planAllowed=true;
 public int loanGroup=1;
 public final Map<String,List<Map<String,Object>>> data=new LinkedHashMap<>();
 private static final Set<String> CHILDREN=Set.of("PRODUCT_CHANNEL_OPERATION","LOAN_ELIGIBILITY_EXTENSION",
    "DEPOSIT_PRODUCT_CLOSURE_PRECHECK","DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",
    "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE","PRODUCT_ELIGIBILITY_CRITERION",
    "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE");
 public boolean loanTypePlanAllowed(long loanType,long planType){return planAllowed && loanType==101L && planType==201L;}
 public Optional<Map<String,Object>> findById(String table,long id){
  if(table.equals("PRODUCT_VERSION") && id==66L)return Optional.of(Map.of("PRODUCT_ID",99L));
  if(table.equals("PRODUCT") && id==99L)return Optional.of(Map.of("PRODUCT_ID",99L));
  if(table.equals("PRODUCT_VERSION"))return Optional.of(Map.of("PRODUCT_ID",9L,"VALID_FROM","2026-10-10","VALID_TO","2026-10-30","VERSION_STATUS_CODE","DRAFT"));
  if(table.equals("PRODUCT"))return Optional.of(Map.of("PRODUCT_CLASS_CODE",domain,"PRODUCT_FAMILY_CODE",family,"PRODUCT_STATUS_CODE","DRAFT","DEFAULT_CURRENCY_CODE","IRR"));
 
  if(table.equals("PRODUCT") && id==99L)return Optional.of(Map.of("PRODUCT_ID",99L));
  if(table.equals("LOAN_TYPE"))return "LOAN_TYPE".equals(failingReference) ? Optional.empty()
   : Optional.of(Map.of("LOAN_GROUP",loanGroup,"RECORD_STATUS_CODE","ACTIVE"));
  if(table.equals("PLAN_TYPE"))return "PLAN_TYPE".equals(failingReference) ? Optional.empty()
   : Optional.of(Map.of("RECORD_STATUS_CODE","ACTIVE"));
 return Optional.empty();
 }
 public long insert(String table,Map<String,Object> values,String actor){throw new AssertionError("audit must not write");}
 public void update(String table,long id,Map<String,Object> values,String actor){throw new AssertionError("audit must not write");}
 public TablePage search(String table,String t,int page,int size,String field,String value){
  if(table.equals(failingChild))throw new IllegalStateException("Oracle read failed");
  List<Map<String,Object>> rows=table.equals("PRODUCT_VERSION_MODULE")||CHILDREN.contains(table)
       ?List.of():List.of(new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table)));
  if(data.containsKey(table)) rows=data.get(table);
  int start=Math.min(page*size,rows.size());int end=Math.min(start+size,rows.size());
  return new TablePage(rows.subList(start,end),rows.size(),page,size);
 }
}`);
 const harness=put('Hf7Smoke.java',String.raw`
import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
public class Hf7Smoke {
 static int pass=0;
 static void ok(String label,boolean b){if(!b)throw new AssertionError(label);pass++;System.out.println("PASS | Java: "+label);}
 static Map<String,Object> row(Object... args){var m=new LinkedHashMap<String,Object>();for(int i=0;i<args.length;i+=2)m.put((String)args[i],args[i+1]);return m;}
 static void with(PdlProductBuilderRepository r,String table,Map<String,Object>... rows){r.data.put(table,new ArrayList<>(List.of(rows)));}
 static ProductRuleGovernanceService.GovernanceReport report(PdlProductBuilderRepository r){return new ProductRuleGovernanceService(r,new RuleGovernanceLiteService(),new ProductBuilderBusinessValidator()).report(55);}
 static boolean reason(ProductRuleGovernanceService.GovernanceReport r,String name,String text){return r.effectiveRules().stream().anyMatch(x->x.code().equals(name)&&x.status().equals("INVALID_SEMANTICS")&&x.reason().contains(text));}
 static boolean denied(PdlProductBuilderRepository r,boolean opening){try{
 var svc=new ProductRuleGovernanceService(r,new RuleGovernanceLiteService(),new ProductBuilderBusinessValidator());
 svc.assertPromotionAllowed(55,Map.of("VERSION_STATUS_CODE","DRAFT","ORIGINATION_STATUS_CODE","CLOSED"),
  Map.of("VERSION_STATUS_CODE","APPROVED","ORIGINATION_STATUS_CODE",opening?"OPEN":"CLOSED"));return false;
 }catch(ProductBuilderValidationException ex){return true;}}
 public static void main(String[] args){
  var r=new PdlProductBuilderRepository();
  ok("retail deposit design approvable",report(r).canApprove());
  ok("retail runtime closed",!report(r).canOpen());
  r.family="NOSTRO_ACCOUNT";
  with(r,"CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE",row("CORRESPONDENT_PRODUCT_PROFILE_ID",715L,
   "ACCOUNT_TYPE_CODE","NOSTRO","PRODUCT_VERSION_ID",55L,"SETTLEMENT_CURRENCY_CODE","USD",
   "RECONCILIATION_METHOD_CODE","AUTOMATIC","OVERDRAFT_ALLOWED",0));
  var result=report(r);
  ok("NOSTRO design approvable",result.canApprove());
  ok("NOSTRO operational open blocked",!result.canOpen() && result.blockers().stream().anyMatch(x->x.contains("نوسترو/وسترو")));
  ok("NOSTRO design approval still permitted without OPEN",!denied(r,false));
  ok("NOSTRO promotion to OPEN denied",denied(r,true));
  r.family="VOSTRO_ACCOUNT";
  r.data.get("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE").get(0).put("ACCOUNT_TYPE_CODE","VOSTRO");
  ok("VOSTRO design approvable",report(r).canApprove());
  ok("VOSTRO operational open blocked",!report(r).canOpen());
  r.data.get("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE").get(0).put("ACCOUNT_TYPE_CODE","NOSTRO");
  ok("correspondent profile family mismatch blocks",!report(r).canApprove()&&reason(report(r),"CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE","خانواده"));
  r.data.get("CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE").get(0).put("ACCOUNT_TYPE_CODE","VOSTRO");
  with(r,"CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE",row("CORRESPONDENT_PRODUCT_PROFILE_ID",715L,
   "SETTLEMENT_FREQUENCY_CODE","DAILY","CUTOFF_TIME","16:30","NETTING_ALLOWED",0,"VALID_FROM","2026-10-10"));
  ok("valid correspondent child canApprove",report(r).canApprove());
  var settle=r.data.get("CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE").get(0);
  settle.put("CUTOFF_TIME","27:30");
  ok("invalid cutoff blocks approval",!report(r).canApprove()&&reason(report(r),"CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE","CUTOFF_TIME"));
  settle.put("CUTOFF_TIME","16:30");settle.put("NETTING_ALLOWED",9);
  ok("invalid netting flag blocks approval",!report(r).canApprove());
  settle.put("NETTING_ALLOWED",0);settle.put("CORRESPONDENT_PRODUCT_PROFILE_ID",999L);
  ok("wrong correspondent child parent blocks",!report(r).canApprove()&&reason(report(r),"CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE","ارتباط"));
  settle.put("CORRESPONDENT_PRODUCT_PROFILE_ID",715L);r.data.remove("CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE");r.failingChild="CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE";
  ok("correspondent child lookup fails closed",!report(r).canApprove()&&reason(report(r),"CORRESPONDENT_ACCOUNT_PRODUCT_PROFILE","قابل بازیابی"));
  r.failingChild=null;r.data.clear();r.family="CURRENT_ACCOUNT";
  ok("optional absence of normalized criterion does not block",report(r).canApprove());
  with(r,"PRODUCT_ELIGIBILITY_CRITERION",row("ELIGIBILITY_RULE_ID",812L,"CRITERION_TYPE_CODE","CUSTOMER_TENURE",
   "OPERATOR_CODE","EQ","CRITERION_VALUE_NUMBER",6,"VALUE_UNIT_CODE","MONTH"));
  ok("invalid tenure operator blocks",!report(r).canApprove()&&reason(report(r),"PRODUCT_ELIGIBILITY_RULE","سابقه"));
  var crit=r.data.get("PRODUCT_ELIGIBILITY_CRITERION").get(0);
  crit.put("OPERATOR_CODE","MIN");
  ok("valid tenure criterion allowed",report(r).canApprove());
  crit.put("VALUE_UNIT_CODE","BAD");
  ok("invalid tenure unit blocked",!report(r).canApprove());
  with(r,"PRODUCT_ELIGIBILITY_CRITERION",row("ELIGIBILITY_RULE_ID",812L,"CRITERION_TYPE_CODE","GENDER",
   "OPERATOR_CODE","IN","CRITERION_VALUE_CODE","1"));
  ok("gender without person-only scope blocked",!report(r).canApprove()&&reason(report(r),"PRODUCT_ELIGIBILITY_RULE","جنسیت"));
  var person=row("ELIGIBILITY_RULE_ID",812L,"CRITERION_TYPE_CODE","CUSTOMER_TYPE","OPERATOR_CODE","IN","CRITERION_VALUE_CODE","PERSON");
  var gender=row("ELIGIBILITY_RULE_ID",812L,"CRITERION_TYPE_CODE","GENDER","OPERATOR_CODE","IN","CRITERION_VALUE_CODE","1");
  with(r,"PRODUCT_ELIGIBILITY_CRITERION",person,gender);
  ok("valid person+gender criteria approvable",report(r).canApprove());
  var organization=row("ELIGIBILITY_RULE_ID",812L,"CRITERION_TYPE_CODE","CUSTOMER_TYPE","OPERATOR_CODE","IN","CRITERION_VALUE_CODE","ORGANIZATION");
  with(r,"PRODUCT_ELIGIBILITY_CRITERION",person,organization,gender);
  ok("mixed person and organization with gender blocked",!report(r).canApprove());
  with(r,"PRODUCT_ELIGIBILITY_CRITERION",person,new LinkedHashMap<>(person));
  ok("duplicate eligibility value blocked",!report(r).canApprove()&&reason(report(r),"PRODUCT_ELIGIBILITY_RULE","تکراری"));
  person.put("ELIGIBILITY_RULE_ID",999L);with(r,"PRODUCT_ELIGIBILITY_CRITERION",person);
  ok("wrong criterion parent blocked",!report(r).canApprove()&&reason(report(r),"PRODUCT_ELIGIBILITY_RULE","ارتباط"));
  person.put("ELIGIBILITY_RULE_ID",812L);person.put("IS_ACTIVE",9);
  ok("unknown criterion active status blocked",!report(r).canApprove()&&reason(report(r),"PRODUCT_ELIGIBILITY_RULE","وضعیت"));
  person.put("IS_ACTIVE",0);person.put("OPERATOR_CODE","BAD");
  ok("inactive criterion ignored as historical",report(r).canApprove());
  r.data.remove("PRODUCT_ELIGIBILITY_CRITERION");r.failingChild="PRODUCT_ELIGIBILITY_CRITERION";
  ok("eligibility child lookup fails closed",!report(r).canApprove()&&reason(report(r),"PRODUCT_ELIGIBILITY_RULE","قابل بازیابی"));
  System.out.println("PDL_R30_HF7A3_HF7_JAVA_PASS="+pass+" FAIL=0");
 }
}`);
 const extra=put('Hf8Smoke.java',String.raw`
import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
public class Hf8Smoke {
 static int pass=0;
 static void ok(String label,boolean test){if(!test)throw new AssertionError(label);pass++;System.out.println("PASS | Java HF8: "+label);}
 static Map<String,Object> m(String table){return new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table));}
 static void with(PdlProductBuilderRepository r,String table,Map<String,Object>... rows){r.data.put(table,new ArrayList<>(List.of(rows)));}
 static ProductRuleGovernanceService.GovernanceReport report(PdlProductBuilderRepository r){return new ProductRuleGovernanceService(r,new RuleGovernanceLiteService(),new ProductBuilderBusinessValidator()).report(55);}
 static boolean invalid(PdlProductBuilderRepository r,String table,String snippet){var g=report(r);
  return !g.canApprove()&&g.effectiveRules().stream().anyMatch(x->x.code().equals(table)&&x.status().equals("INVALID_SEMANTICS")&&x.reason().contains(snippet));}
 static boolean conflict(PdlProductBuilderRepository r,String table){var g=report(r);
  return !g.canApprove()&&g.effectiveRules().stream().anyMatch(x->x.code().equals(table)&&x.status().equals("CONFLICT"));}
 static boolean denied(PdlProductBuilderRepository r){try{new ProductRuleGovernanceService(r,new RuleGovernanceLiteService(),new ProductBuilderBusinessValidator()).assertPromotionAllowed(55,
  Map.of("VERSION_STATUS_CODE","DRAFT","ORIGINATION_STATUS_CODE","CLOSED"),Map.of("VERSION_STATUS_CODE","APPROVED","ORIGINATION_STATUS_CODE","CLOSED"));return false;
 }catch(ProductBuilderValidationException ex){return true;}}
 public static void main(String[] args){
  var r=new PdlProductBuilderRepository();
  ok("retail deposit design remains approvable",report(r).canApprove());
  ok("retail deposit operationally closed",!report(r).canOpen());
  var profile=m("DEPOSIT_PRODUCT_PROFILE");profile.put("DEPOSIT_TYPE_CODE","SAVINGS");with(r,"DEPOSIT_PRODUCT_PROFILE",profile);
  ok("deposit family/type conflict blocks approval",invalid(r,"DEPOSIT_PRODUCT_PROFILE","خانواده"));
  ok("deposit promotion is refused",denied(r));
  profile.put("DEPOSIT_TYPE_CODE","CURRENT_NO_INTEREST");profile.put("PRODUCT_VERSION_ID",66L);
  ok("deposit profile wrong version blocks approval",invalid(r,"DEPOSIT_PRODUCT_PROFILE","نسخه"));
  r.data.remove("DEPOSIT_PRODUCT_PROFILE");
  r.family="LONG_TERM_DEPOSIT";
  profile=m("DEPOSIT_PRODUCT_PROFILE");profile.put("DEPOSIT_GROUP_CODE","LONG_TERM");profile.put("DEPOSIT_TYPE_CODE","LONG_TERM_DEPOSIT");with(r,"DEPOSIT_PRODUCT_PROFILE",profile);
  ok("term deposit design with coherent payment canApprove",report(r).canApprove());
  var payment=m("DEPOSIT_PROFIT_PAYMENT_RULE");payment.put("FIRST_PAYMENT_RULE_CODE","UNKNOWN");with(r,"DEPOSIT_PROFIT_PAYMENT_RULE",payment);
  ok("invalid profit first-payment code blocks",invalid(r,"DEPOSIT_PROFIT_PAYMENT_RULE","اولین پرداخت"));
  payment.put("FIRST_PAYMENT_RULE_CODE","PRORATE_FROM_OPENING");payment.remove("CAPITALIZATION_DEFAULT");
  ok("missing profit capitalization flag blocks",invalid(r,"DEPOSIT_PROFIT_PAYMENT_RULE","صریحاً"));
  payment.put("CAPITALIZATION_DEFAULT",1);
  ok("profit capitalization dependency blocks",invalid(r,"DEPOSIT_PROFIT_PAYMENT_RULE","سرمایه"));
  payment.put("CAPITALIZATION_DEFAULT",0);payment.put("PRODUCT_VERSION_ID",66L);
  ok("wrong profit version blocks",invalid(r,"DEPOSIT_PROFIT_PAYMENT_RULE","نسخه"));
  r.data.remove("DEPOSIT_PROFIT_PAYMENT_RULE");
  r.domain="LOAN";r.family="QARD_HASAN_LOAN";
  ok("valid loan profile canApprove",report(r).canApprove());
  ok("loan remains operationally closed",!report(r).canOpen());
  var loan=m("LOAN_PRODUCT_PROFILE");loan.put("CURRENCY_TYPE_CODE","EUR");with(r,"LOAN_PRODUCT_PROFILE",loan);
  ok("loan currency mismatch blocks",invalid(r,"LOAN_PRODUCT_PROFILE","ارز"));
  loan.put("CURRENCY_TYPE_CODE","IRR");r.loanGroup=2;
  ok("loan type mismatches product family",invalid(r,"LOAN_PRODUCT_PROFILE","خانواده"));
  r.loanGroup=1;r.planAllowed=false;
  ok("invalid loan type/plan relation blocks",invalid(r,"LOAN_PRODUCT_PROFILE","طرح"));
  r.planAllowed=true;r.failingReference="LOAN_TYPE";
  ok("missing loan reference fails closed",invalid(r,"LOAN_PRODUCT_PROFILE","مرجع"));
  r.failingReference=null;r.data.remove("LOAN_PRODUCT_PROFILE");r.domain="DEPOSIT";r.family="CURRENT_ACCOUNT";r.data.remove("DEPOSIT_PRODUCT_PROFILE");
  ok("valid optional product relation canApprove",report(r).canApprove());
  var relation=m("PRODUCT_RELATIONSHIP");relation.put("TARGET_PRODUCT_ID",9L);with(r,"PRODUCT_RELATIONSHIP",relation);
  ok("self-target relationship blocks",invalid(r,"PRODUCT_RELATIONSHIP","مبدأ"));
  relation.put("TARGET_PRODUCT_ID",99L);relation.put("SOURCE_PRODUCT_VERSION_ID",44L);
  ok("wrong source version blocks",invalid(r,"PRODUCT_RELATIONSHIP","نسخه"));
  relation.put("SOURCE_PRODUCT_VERSION_ID",55L);relation.put("TARGET_PRODUCT_VERSION_ID",66L);
  ok("valid target product/version canApprove",report(r).canApprove());
  relation.put("TARGET_PRODUCT_VERSION_ID",55L);
  ok("target version belonging to other product blocks",invalid(r,"PRODUCT_RELATIONSHIP","مقصد"));
  var a=m("PRODUCT_RELATIONSHIP");var b=m("PRODUCT_RELATIONSHIP");
  a.put("VALID_FROM","2026-10-10");a.put("VALID_TO","2026-10-15");
  b.put("VALID_FROM","2026-10-15");b.put("VALID_TO","2026-10-30");with(r,"PRODUCT_RELATIONSHIP",a,b);
  ok("simultaneous exact duplicate relationships conflict",conflict(r,"PRODUCT_RELATIONSHIP"));
  b.put("VALID_FROM","2026-10-16");
  ok("nonoverlapping equivalent relations allowed",report(r).canApprove());
  ok("canOpen remains false after all positive designs",!report(r).canOpen());
  System.out.println("PDL_R30_HF7A3_HF8_JAVA_PASS="+pass+" FAIL=0");
 }
}`);
 const classes=path.join(tmp,'classes');fs.mkdirSync(classes);
 const actual=['ProductBuilderValidationException.java','ProductBuilderBusinessValidator.java',
   'ProductRuleSemanticContract.java','ProductPricingSingleRatePolicy.java','ProductRuleEvidenceEvaluator.java','ProductRuleHierarchyAudit.java',
   'DepositProfileIdentityPolicy.java','LoanTypeClassification.java','ProductRuleGovernanceService.java'].map(f=>path.join(root,app,f));
 actual.push(path.join(root,'backend/src/main/java/com/behsazan/corebanking/productbuilder/domain/ProductBuilderModels.java'));
 run('javac',['-encoding','UTF-8','-d',classes,...spring,lite,fixturePath,repository,...actual,harness,extra]);
 run('java',['-cp',classes,'Hf7Smoke']);
 run('java',['-cp',classes,'Hf8Smoke']);
 // HF9: independently execute the real report and promotion gate on the same
 // side-effect-free repository stub; this is NOT an Oracle UAT run.
 const hf9Publication=put('Hf9PublicationSmoke.java',String.raw`
import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
public class Hf9PublicationSmoke {
 static int count=0;
 static void ok(String name,boolean result){if(!result)throw new AssertionError(name);count++;System.out.println("PASS | HF9 Governance: "+name);}
 static Map<String,Object> get(String t){return new LinkedHashMap<>(ProductRuleGovernanceFixture.row(t));}
 static void set(PdlProductBuilderRepository r,String name,Map<String,Object>...rows){r.data.put(name,new ArrayList<>(List.of(rows)));}
 static ProductRuleGovernanceService.GovernanceReport report(PdlProductBuilderRepository r){return new ProductRuleGovernanceService(r,new RuleGovernanceLiteService(),new ProductBuilderBusinessValidator()).report(55L);}
 static boolean blocked(PdlProductBuilderRepository r,String status){var g=report(r);return !g.canApprove()&&!g.canOpen()&&g.effectiveRules().stream().anyMatch(x->x.code().equals("PRODUCT_PRICING_RULE")&&x.status().equals(status)&&x.blocking());}
 public static void main(String[]args){
  var r=new PdlProductBuilderRepository();
  var p=get("PRODUCT_PRICING_RULE");p.put("PRICING_METHOD_CODE","TIERED");p.put("IS_TIERED_RATE",1);set(r,"PRODUCT_PRICING_RULE",p);
  var c=get("PRODUCT_PRICING_COMPONENT");c.put("MIN_AMOUNT",0);set(r,"PRODUCT_PRICING_COMPONENT",c);
  var a=get("PRODUCT_RATE_TIER");a.put("MIN_AMOUNT",0);a.put("MAX_AMOUNT",100);
  var b=get("PRODUCT_RATE_TIER");b.put("TIER_NO",2);b.put("MIN_AMOUNT",100);
  set(r,"PRODUCT_RATE_TIER",a,b);
  ok("valid partition canApprove",report(r).canApprove());
  ok("valid partition cannot open Runtime",!report(r).canOpen());
  a.put("MAX_AMOUNT",110);
  ok("overlap blocks actual canApprove",blocked(r,"CONFLICT"));
  try {new ProductRuleGovernanceService(r,new RuleGovernanceLiteService(),new ProductBuilderBusinessValidator()).assertPromotionAllowed(55L,
   Map.of("VERSION_STATUS_CODE","DRAFT"),Map.of("VERSION_STATUS_CODE","APPROVED"));
   throw new AssertionError("promotion unexpectedly succeeded");}
  catch(ProductBuilderValidationException expected){ok("approval transition blocked",true);}
  a.put("MAX_AMOUNT",100);b.put("MIN_AMOUNT",110);
  ok("amount gap blocks actual canApprove",blocked(r,"CONFLICT"));
  b.put("MIN_AMOUNT",100);a.put("MIN_TERM_VALUE",1);a.put("MAX_TERM_VALUE",12);a.put("TERM_UNIT_CODE","MONTH");
  ok("unapproved duration semantics block publication",blocked(r,"INVALID_SEMANTICS"));
  a.remove("MIN_TERM_VALUE");a.remove("MAX_TERM_VALUE");a.remove("TERM_UNIT_CODE");
  p.put("PRICING_METHOD_CODE","FIXED");
  ok("pricing method and tier flag mismatch blocks",blocked(r,"INVALID_SEMANTICS"));
  System.out.println("PDL_R30_HF7A3_HF9_GOVERNANCE_PASS="+count+" FAIL=0");
 }
}`);
 run('javac',['-encoding','UTF-8','-cp',classes,'-d',classes,hf9Publication]);
 run('java',['-cp',classes,'Hf9PublicationSmoke']);
 // The actual JUnit test is compiled with compile-time-only Mockito/JUnit/AssertJ
 // signatures; this does not claim to run the JUnit suite or replace Maven.
 const signatures=[
   put('org/junit/jupiter/api/Test.java','package org.junit.jupiter.api; public @interface Test{}'),
   put('org/junit/jupiter/api/function/Executable.java','package org.junit.jupiter.api.function; @FunctionalInterface public interface Executable{void execute() throws Throwable;}'),
   put('org/junit/jupiter/api/Assertions.java','package org.junit.jupiter.api; import org.junit.jupiter.api.function.Executable; public class Assertions{public static void assertTrue(boolean x){} public static void assertFalse(boolean x){} public static void assertEquals(Object e,Object a){} public static void assertEquals(Object e,Object a,String msg){} public static <T extends Throwable>T assertThrows(Class<T> t,Executable fn){return null;}}'),
   put('org/mockito/ArgumentMatchers.java',String.raw`package org.mockito;import java.util.*;public class ArgumentMatchers{public static String anyString(){return "";} public static <T>T isNull(){return null;}public static int anyInt(){return 0;}public static <K,V>Map<K,V>anyMap(){return null;}public static long anyLong(){return 0;}}`),
   put('org/mockito/invocation/InvocationOnMock.java',String.raw`package org.mockito.invocation; public interface InvocationOnMock{<T>T getArgument(int i);}`),
   put('org/mockito/stubbing/Answer.java',String.raw`package org.mockito.stubbing;public interface Answer<T>{T answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable;}`),
   put('org/mockito/stubbing/OngoingStubbing.java',String.raw`package org.mockito.stubbing;public class OngoingStubbing<T>{public OngoingStubbing<T>thenReturn(T x){return this;} public OngoingStubbing<T>thenAnswer(Answer<T> x){return this;}}`),
   put('org/mockito/Mockito.java',String.raw`package org.mockito;public class Mockito {public static <T>T mock(Class<T> type){return null;} public static <T>org.mockito.stubbing.OngoingStubbing<T> when(T value){return null;} public static <T>T verify(T obj,VerificationMode m){return obj;} public static VerificationMode never(){return null;}public interface VerificationMode{}}`),
   put('org/assertj/core/api/Assertions.java',String.raw`
package org.assertj.core.api;
import java.util.*;import java.util.function.Predicate;
public class Assertions {
 public static BooleanAssert assertThat(boolean b){return new BooleanAssert();}
 public static <T> ListAssert<T> assertThat(List<T> values){return new ListAssert<T>();}
 public static ThrowableAssert assertThatThrownBy(ThrowingCallable callable){return new ThrowableAssert();}
 public interface ThrowingCallable{void call() throws Throwable;}
 public static class BooleanAssert{public BooleanAssert isTrue(){return this;}public BooleanAssert isFalse(){return this;}}
 public static class ListAssert<T>{public ListAssert<T>anyMatch(Predicate<T> predicate){return this;}}
 public static class ThrowableAssert{public ThrowableAssert isInstanceOf(Class<?> t){return this;} public ThrowableAssert hasMessageContaining(String x){return this;}}
}`)
 ];
 run('javac',['-encoding','UTF-8','-cp',classes,'-d',classes,...signatures,path.join(root,tests+'ProductRuleHierarchyHF7A3HF7Test.java')]);
 console.log('PASS | Real ProductRuleHierarchyHF7A3HF7Test.java source compiles with JUnit/Mockito/AssertJ signatures');
 run('javac',['-encoding','UTF-8','-cp',classes,'-d',classes,...signatures,path.join(root,tests+'ProductRulePublicationHF7A3HF8Test.java')]);
 console.log('PASS | Real ProductRulePublicationHF7A3HF8Test.java source compiles with JUnit/Mockito/AssertJ signatures');
 run('javac',['-encoding','UTF-8','-cp',classes,'-d',classes,...signatures,path.join(root,tests+'ProductPricingPublicationHF7A3HF9Test.java')]);
 console.log('PASS | Real ProductPricingPublicationHF7A3HF9Test.java source compiles with JUnit/Mockito signatures');
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
console.log('PDL_R30_HF7A3_HF8_STATIC_PASS='+staticCount+' FAIL=0');
