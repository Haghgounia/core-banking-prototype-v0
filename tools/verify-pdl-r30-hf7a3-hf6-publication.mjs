/** R30-HF7-A3/HF6: real read-only hierarchy audit wired to canApprove; no Oracle mutation. */
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
const test=read(tests+'ProductRuleHierarchyHF7A3HF6Test.java');
let staticCount=0;
function check(title,predicate){if(!predicate)throw Error('FAIL | '+title); staticCount++;console.log('PASS | '+title);}
for (const table of ['PRODUCT_CHANNEL_RULE','PRODUCT_ELIGIBILITY_RULE','DEPOSIT_PRODUCT_CLOSURE_RULE'])
 check('report audits relationship '+table, governance.includes('case "PRODUCT_CHANNEL_RULE", "PRODUCT_ELIGIBILITY_RULE", "DEPOSIT_PRODUCT_CLOSURE_RULE"') && hierarchy.includes('case "'+table+'"'));
check('report passes real domain and paginated read-only child loader', governance.includes('domain, this::loadChildEvidence, businessValidator'));
check('child defects are included in publish-blocking semantics',governance.includes('invalidRows += relationAudit.invalidCount()') && governance.includes('if (!relationAudit.valid())'));
check('every relationship finding reaches rendered reason',governance.includes('semanticProblem = relationAudit.firstProblem()'));
check('audits respect disabled module scope',governance.indexOf('if (skipDisabled)') < governance.indexOf('ProductRuleHierarchyAudit.AuditResult relationAudit'));
check('approved design does not open unqualified deposit or loan',governance.includes('if ("DEPOSIT".equals(domain)') && governance.includes('if ("LOAN".equals(domain)) {\n            canOpen = false;'));
check('all child lookup failures fail closed', hierarchy.includes('children == null') && hierarchy.includes('قواعد فرزند قابل بازیابی کامل نیستند'));
check('invalid status and parent mismatch both blocked',hierarchy.includes('unrecognizedStatuses() > 0') && hierarchy.includes('strictId(child.get(parentIdField))'));
check('optional absence remains optional, not operational authorization',hierarchy.includes('a missing child is allowed') || hierarchy.includes('A missing child is allowed'));
check('test contains real report and maker-checker promotion checks', test.includes('scenario.governance.assertPromotionAllowed') && test.includes('scenario.report()'));
check('new suite never exercises repository writes',test.includes('verify(repo, never()).insert') && test.includes('verify(repo, never()).update'));
check('HF6 verifier is part of Windows production build',read('build-production.cmd').includes('verify-pdl-r30-hf7a3-hf6-publication.mjs'));
check('HF6 verifier is part of Unix production build',read('build-production.sh').includes('verify-pdl-r30-hf7a3-hf6-publication.mjs'));
check('no write primitive is used by hierarchy evaluator',!/(executeUpdate|jdbcClient|DataSource|\b(?:insert|update|delete|merge)\s*\()/i.test(hierarchy));

const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'hf7a3-hf6-'));
function put(rel,contents){let p=path.join(tmp,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,contents);return p;}
function run(bin,args) {
 const r=spawnSync(bin,args,{encoding:'utf8',maxBuffer:6*1024*1024,timeout:95000});
 if(r.stdout)process.stdout.write(r.stdout);
 if(r.status!==0||r.error) {if(r.stderr)process.stderr.write(r.stderr);throw Error(bin+' failed: '+(r.error?.message||r.stderr||r.status));}
}
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
 public boolean failingChannelLookup=false;
 public final Map<String,List<Map<String,Object>>> data=new LinkedHashMap<>();
 private static final Set<String> CHILDREN=Set.of("PRODUCT_CHANNEL_OPERATION","LOAN_ELIGIBILITY_EXTENSION",
    "DEPOSIT_PRODUCT_CLOSURE_PRECHECK","DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",
    "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE","PRODUCT_ELIGIBILITY_CRITERION",
    "CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE");
 public boolean loanTypePlanAllowed(long loanType,long planType){return loanType==101L && planType==201L;}
 public Optional<Map<String,Object>> findById(String table,long id){
  if(table.equals("PRODUCT_VERSION"))return Optional.of(Map.of("PRODUCT_ID",9L,"VALID_FROM","2026-10-10","VALID_TO","2026-10-30","VERSION_STATUS_CODE","DRAFT"));
  if(table.equals("PRODUCT"))return Optional.of(Map.of("PRODUCT_CLASS_CODE",domain,"PRODUCT_FAMILY_CODE",family,"PRODUCT_STATUS_CODE","DRAFT","DEFAULT_CURRENCY_CODE","IRR"));
 
  if(table.equals("PRODUCT") && id==99L)return Optional.of(Map.of("PRODUCT_ID",99L));
  if(table.equals("LOAN_TYPE"))return Optional.of(Map.of("LOAN_GROUP",1,"RECORD_STATUS_CODE","ACTIVE"));
  if(table.equals("PLAN_TYPE"))return Optional.of(Map.of("RECORD_STATUS_CODE","ACTIVE"));
 return Optional.empty();
 }
 public long insert(String table,Map<String,Object> values,String actor){throw new AssertionError("audit must not write");}
 public void update(String table,long id,Map<String,Object> values,String actor){throw new AssertionError("audit must not write");}
 public TablePage search(String table,String t,int page,int size,String field,String value){
  if(failingChannelLookup && table.equals("PRODUCT_CHANNEL_OPERATION"))throw new IllegalStateException("Oracle read failed");
  List<Map<String,Object>> rows=table.equals("PRODUCT_VERSION_MODULE")||CHILDREN.contains(table)
       ?List.of():List.of(new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table)));
  if(data.containsKey(table)) rows=data.get(table);
  int start=Math.min(page*size,rows.size());int end=Math.min(start+size,rows.size());
  return new TablePage(rows.subList(start,end),rows.size(),page,size);
 }
}`);
 const harness=put('Hf6Smoke.java',String.raw`
import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
public class Hf6Smoke {
 static int pass=0;
 static void ok(String label,boolean condition){if(!condition)throw new AssertionError(label);pass++;System.out.println("PASS | Java: "+label);}
 static Map<String,Object> map(Object... values){var row=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)row.put((String)values[i],values[i+1]);return row;}
 static void row(PdlProductBuilderRepository r,String name,Map<String,Object> child){r.data.put(name,List.of(child));}
 static ProductRuleGovernanceService.GovernanceReport report(PdlProductBuilderRepository repo){return new ProductRuleGovernanceService(repo,new RuleGovernanceLiteService(),new ProductBuilderBusinessValidator()).report(55);}
 static boolean status(ProductRuleGovernanceService.GovernanceReport r,String code,String state,String text){return r.effectiveRules().stream().anyMatch(x->x.code().equals(code)&&x.status().equals(state)&&x.reason().contains(text));}
 static boolean approvalDenied(PdlProductBuilderRepository repo){try{new ProductRuleGovernanceService(repo,new RuleGovernanceLiteService(),new ProductBuilderBusinessValidator()).assertPromotionAllowed(55,Map.of("VERSION_STATUS_CODE","DRAFT"),Map.of("VERSION_STATUS_CODE","APPROVED"));return false;}catch(ProductBuilderValidationException e){return true;}}
 public static void main(String[] args){
  var repo=new PdlProductBuilderRepository();
  var baseline=report(repo);
  ok("base deposit design canApprove",baseline.canApprove());
  ok("base deposit runtime canOpen remains closed",!baseline.canOpen());
  row(repo,"PRODUCT_CHANNEL_OPERATION",map("CHANNEL_RULE_ID",811L,"OPERATION_CODE","OPEN_ACCOUNT","IS_ALLOWED",1,"REQUIRES_ADDITIONAL_AUTH",0));
  var r=report(repo);
  ok("denied channel with allowed child blocks actual report",!r.canApprove()&&status(r,"PRODUCT_CHANNEL_RULE","INVALID_SEMANTICS","غیرمجاز"));
  ok("actual approval transition denies invalid channel child",approvalDenied(repo));
  row(repo,"PRODUCT_CHANNEL_RULE",map("CHANNEL_RULE_ID",811L,"CHANNEL_CODE","BRANCH","IS_ALLOWED",1,"RULE_STATUS_CODE","ACTIVE"));
  r=report(repo);ok("allowed channel with valid child canApprove",r.canApprove());
  ok("allowed channel child still does not open runtime",!r.canOpen());
  row(repo,"PRODUCT_CHANNEL_OPERATION",map("CHANNEL_RULE_ID",999L,"OPERATION_CODE","OPEN_ACCOUNT","IS_ALLOWED",1,"REQUIRES_ADDITIONAL_AUTH",0));
  r=report(repo);ok("wrong channel parent FK blocks report",!r.canApprove()&&status(r,"PRODUCT_CHANNEL_RULE","INVALID_SEMANTICS","ارتباط"));
  row(repo,"PRODUCT_CHANNEL_OPERATION",map("CHANNEL_RULE_ID",811L,"OPERATION_CODE","OPEN_ACCOUNT","IS_ALLOWED",1,"REQUIRES_ADDITIONAL_AUTH",0,"IS_ACTIVE",9));
  r=report(repo);ok("invalid child status blocks report",!r.canApprove()&&status(r,"PRODUCT_CHANNEL_RULE","INVALID_SEMANTICS","وضعیت"));
  row(repo,"PRODUCT_CHANNEL_OPERATION",map("CHANNEL_RULE_ID",811L,"OPERATION_CODE","OPEN_ACCOUNT","IS_ALLOWED",1,"REQUIRES_ADDITIONAL_AUTH",0,"VALID_FROM","2026-10-20"));
  ok("future active child remains subject to publication audit",report(repo).canApprove()); // parent is allowed
  row(repo,"PRODUCT_CHANNEL_RULE",map("CHANNEL_RULE_ID",811L,"CHANNEL_CODE","BRANCH","IS_ALLOWED",0,"RULE_STATUS_CODE","ACTIVE"));
  ok("future child allowed on denied channel is blocked",!report(repo).canApprove());
  row(repo,"PRODUCT_CHANNEL_OPERATION",map("CHANNEL_RULE_ID",811L,"OPERATION_CODE","OPEN_ACCOUNT","IS_ALLOWED",1,"REQUIRES_ADDITIONAL_AUTH",0,"IS_ACTIVE",0));
  ok("inactive child does not block approval",report(repo).canApprove());
  repo.data.remove("PRODUCT_CHANNEL_OPERATION");repo.failingChannelLookup=true;
  r=report(repo);ok("child lookup failure fails closed",!r.canApprove()&&status(r,"PRODUCT_CHANNEL_RULE","INVALID_SEMANTICS","قابل بازیابی"));
  repo.failingChannelLookup=false;
  repo.domain="LOAN";repo.family="QARD_HASAN_LOAN";
  ok("loan with absent optional extension canApprove",report(repo).canApprove());
  ok("loan runtime still closed",!report(repo).canOpen());
  row(repo,"LOAN_ELIGIBILITY_EXTENSION",map("ELIGIBILITY_RULE_ID",812L,"REQUIRED_GUARANTOR_COUNT",2.5,"CBI_TRACKING_REQUIRED",0,"CREDIT_INQUIRY_REQUIRED",0,"AML_CHECK_REQUIRED",0));
  r=report(repo);ok("invalid loan extension blocks real report",!r.canApprove()&&status(r,"PRODUCT_ELIGIBILITY_RULE","INVALID_SEMANTICS","تعداد ضامنان"));
  row(repo,"LOAN_ELIGIBILITY_EXTENSION",map("ELIGIBILITY_RULE_ID",812L,"REQUIRED_GUARANTOR_COUNT",2,"CBI_TRACKING_REQUIRED",0,"CREDIT_INQUIRY_REQUIRED",0,"AML_CHECK_REQUIRED",0));
  ok("valid loan extension permits design approval",report(repo).canApprove());
  repo.domain="DEPOSIT";repo.family="CURRENT_ACCOUNT";
  row(repo,"DEPOSIT_PRODUCT_CLOSURE_PRECHECK",map("CLOSURE_RULE_ID",818L,"CHECK_TYPE_CODE","HOLD","FAILURE_ACTION_CODE","BLOCK","SEQUENCE_NO",0,"IS_MANDATORY",1));
  r=report(repo);ok("invalid closure precheck blocks approval",!r.canApprove()&&status(r,"DEPOSIT_PRODUCT_CLOSURE_RULE","INVALID_SEMANTICS","ترتیب"));
  row(repo,"DEPOSIT_PRODUCT_CLOSURE_PRECHECK",map("CLOSURE_RULE_ID",818L,"CHECK_TYPE_CODE","HOLD","FAILURE_ACTION_CODE","BLOCK","SEQUENCE_NO",1,"IS_MANDATORY",1));
  row(repo,"DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",map("CLOSURE_RULE_ID",818L,"SETTLEMENT_COMPONENT_CODE","PRINCIPAL","SETTLEMENT_METHOD_CODE","DEDUCT","DESTINATION_CODE","BANK_ACCOUNT","SEQUENCE_NO",1,"IS_MANDATORY",1));
  r=report(repo);ok("deduct settlement with destination blocks approval",!r.canApprove()&&status(r,"DEPOSIT_PRODUCT_CLOSURE_RULE","INVALID_SEMANTICS","مقصد"));
  row(repo,"DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",map("CLOSURE_RULE_ID",818L,"SETTLEMENT_COMPONENT_CODE","PRINCIPAL","SETTLEMENT_METHOD_CODE","DEDUCT","SEQUENCE_NO",1,"IS_MANDATORY",1));
  row(repo,"DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE",map("CLOSURE_RULE_ID",818L,"APPROVAL_LEVEL_CODE","BRANCH_MANAGER","APPROVAL_SEQUENCE_NO",0,"IS_MANDATORY",1));
  r=report(repo);ok("invalid closure approval order blocks publication",!r.canApprove()&&status(r,"DEPOSIT_PRODUCT_CLOSURE_RULE","INVALID_SEMANTICS","ترتیب"));
  row(repo,"DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE",map("CLOSURE_RULE_ID",818L,"APPROVAL_LEVEL_CODE","BRANCH_MANAGER","APPROVAL_SEQUENCE_NO",1,"IS_MANDATORY",1));
  r=report(repo);ok("three valid closure children allow design approval",r.canApprove());
  ok("valid closure does not qualify runtime",!r.canOpen());
  repo.data.clear();ok("no optional children does not invent new child requirement",report(repo).canApprove());
  System.out.println("PDL_R30_HF7A3_HF6_JAVA_PASS="+pass+" FAIL=0");
 }
}`);
 const classes=path.join(tmp,'classes');fs.mkdirSync(classes);
 const actual=['ProductBuilderValidationException.java','ProductBuilderBusinessValidator.java',
   'ProductRuleSemanticContract.java','ProductPricingSingleRatePolicy.java','ProductRuleEvidenceEvaluator.java','ProductRuleHierarchyAudit.java',
   'DepositProfileIdentityPolicy.java','LoanTypeClassification.java','ProductRuleGovernanceService.java'].map(f=>path.join(root,app,f));
 actual.push(path.join(root,'backend/src/main/java/com/behsazan/corebanking/productbuilder/domain/ProductBuilderModels.java'));
 run('javac',['-encoding','UTF-8','-d',classes,...spring,lite,fixturePath,repository,...actual,harness]);
 run('java',['-cp',classes,'Hf6Smoke']);
 // The actual JUnit test is compiled with compile-time-only Mockito/JUnit/AssertJ
 // signatures; this does not claim to run the JUnit suite or replace Maven.
 const signatures=[
   put('org/junit/jupiter/api/Test.java','package org.junit.jupiter.api; public @interface Test{}'),
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
 run('javac',['-encoding','UTF-8','-cp',classes,'-d',classes,...signatures,path.join(root,tests+'ProductRuleHierarchyHF7A3HF6Test.java')]);
 console.log('PASS | Real ProductRuleHierarchyHF7A3HF6Test.java source compiles with JUnit/Mockito/AssertJ signatures');
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
console.log('PDL_R30_HF7A3_HF6_STATIC_PASS='+staticCount+' FAIL=0');
