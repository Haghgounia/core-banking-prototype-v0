/** R30-HF7-A3/HF5: legacy JUnit source/modern HF4 API compatibility and pure read-only regressions. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const pkg = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/';
const testPkg = 'backend/src/test/java/com/behsazan/corebanking/productbuilder/application/';
const read = rel => fs.readFileSync(path.join(root,rel),'utf8');
let staticPass = 0;
function check(label, condition) {
  if (!condition) throw Error('FAIL | '+label);
  staticPass++;
  console.log('PASS | '+label);
}
const hierarchy = read(pkg+'ProductRuleHierarchyAudit.java');
const evaluator = read(pkg+'ProductRuleEvidenceEvaluator.java');
const oldTest = read(testPkg+'ProductRuleHierarchyHF7A3Test.java');
const service = read(pkg+'ProductRuleGovernanceService.java');
check('HF4 detailed Finding contract retained',hierarchy.includes('record Finding(String status, String reason)') &&
    hierarchy.includes('Finding allowedTerms(') && hierarchy.includes('Finding pricingTiers('));
check('legacy hierarchy contract has read-only three-parent audit',hierarchy.includes('AuditResult audit(') &&
    hierarchy.includes('case "PRODUCT_CHANNEL_RULE"') && hierarchy.includes('case "PRODUCT_ELIGIBILITY_RULE"') &&
    hierarchy.includes('case "DEPOSIT_PRODUCT_CLOSURE_RULE"'));
check('missing or failed child lookup fails closed',hierarchy.includes('children == null') && hierarchy.includes('قواعد فرزند قابل بازیابی کامل نیستند'));
check('child FK linkage and cross-field contract checked',hierarchy.includes('strictId(child.get(parentIdField))') && hierarchy.includes('validator.validate(childTable, child)'));
check('child audit ignores inactive/historical rows but checks future-effective',hierarchy.includes('isApplicableWithin(childTable, child, from, to)') && hierarchy.includes('hasInvalidActivePeriod(childTable, child)'));
check('invalid IS_ACTIVE is never silently normalized to inactive',evaluator.includes('INVALID_ACTIVE_FLAG'));
check('decimal Oracle 1.0 and 0.0 status supported',evaluator.includes('value.compareTo(java.math.BigDecimal.ONE)') && evaluator.includes('value.compareTo(java.math.BigDecimal.ZERO)'));
check('duplicate documents inquiries and rate tiers are covered by evaluator',
    ['PRODUCT_REQUIRED_DOCUMENT','PRODUCT_REQUIRED_INQUIRY','PRODUCT_RATE_TIER'].every(x=>evaluator.includes('case "'+x+'" -> List.of(')));
check('historical JUnit reflection uses Finding, not obsolete boolean',oldTest.includes('(ProductRuleHierarchyAudit.Finding) check.invoke(') && !oldTest.includes('assertEquals(false, check.invoke('));
check('historical JUnit invokes actual civil partnership policy',oldTest.includes('LoanCivilPartnershipPolicy.matches(') &&
    !oldTest.includes('getDeclaredMethod("validateLoanCivilContribution"'));
check('source service still enforces civil partnership validation',read(pkg+'ProductBuilderService.java').includes('LoanCivilPartnershipPolicy.matches(contract)'));
check('no SQL writes in new hierarchy audit',!/(executeUpdate|jdbcClient|DataSource|\b(insert|update|delete|merge)\s*\()/i.test(hierarchy));
check('original runtime canOpen blockers remain untouched',service.includes('if ("LOAN".equals(domain)) {\n            canOpen = false;') && service.includes('if ("DEPOSIT".equals(domain)'));
check('HF5 verifier wired to Windows and Linux builds',read('build-production.cmd').includes('verify-pdl-r30-hf7a3-hf5-compat.mjs') &&
    read('build-production.sh').includes('verify-pdl-r30-hf7a3-hf5-compat.mjs'));
const tmp = fs.mkdtempSync(path.join(os.tmpdir(),'pdl-hf7a3-hf5-'));
function add(rel, value) {
 const dest = path.join(tmp,rel);
 fs.mkdirSync(path.dirname(dest),{recursive:true});
 fs.writeFileSync(dest,value);
 return dest;
}
function run(program,args) {
 const result=spawnSync(program,args,{encoding:'utf8',timeout:110000,maxBuffer:4*1024*1024});
 if(result.stdout) process.stdout.write(result.stdout);
 if(result.status!==0||result.error) {
  if(result.stderr) process.stderr.write(result.stderr);
  throw Error(program+' failed (status='+result.status+'): '+(result.error?.message||result.stderr));
 }
}
try {
 const srcBase = path.join(root,pkg);
 const appFiles = ['ProductBuilderValidationException.java','ProductBuilderBusinessValidator.java',
    'ProductRuleSemanticContract.java','ProductRuleEvidenceEvaluator.java','ProductRuleHierarchyAudit.java',
    'LoanCivilPartnershipPolicy.java'].map(name=>path.join(srcBase,name));
 const springComponent = add('org/springframework/stereotype/Component.java',
   'package org.springframework.stereotype;public @interface Component {}');
 const classes=path.join(tmp,'classes');fs.mkdirSync(classes);
 run('javac',['-encoding','UTF-8','-d',classes,springComponent,...appFiles,
    path.join(root,'tools/java/HF7A3HF5CompatibilityHarness.java')]);
 run('java',['-cp',classes,'com.behsazan.corebanking.productbuilder.application.HF7A3HF5CompatibilityHarness']);
 // Compile real historical JUnit file against minimal compile-time-only framework stubs.
 // This proves Java source signatures, NOT a full Mockito/JUnit test run.
 const stubs=[
  add('org/junit/jupiter/api/Test.java','package org.junit.jupiter.api;public @interface Test {}'),
  add('org/junit/jupiter/api/function/Executable.java','package org.junit.jupiter.api.function;public interface Executable{void execute() throws Throwable;}'),
  add('org/junit/jupiter/api/Assertions.java',`package org.junit.jupiter.api;
import org.junit.jupiter.api.function.Executable;
public class Assertions {
 public static void assertTrue(boolean x){} public static void assertFalse(boolean x){}
 public static void assertEquals(Object a,Object b){} public static void assertDoesNotThrow(Executable r){}
 public static <T extends Throwable>T assertThrows(Class<T> t,Executable r){return null;}
 public static <T>void assertInstanceOf(Class<T> t,Object v){}
}`),
  add('org/mockito/ArgumentMatchers.java',`package org.mockito;import java.util.*;
public class ArgumentMatchers {
 public static String anyString(){return "";} public static <T>T isNull(){return null;}
 public static int anyInt(){return 0;} public static <T>T eq(T x){return x;}
 public static <K,V>Map<K,V> anyMap(){return null;} public static long anyLong(){return 0;}
}`),
  add('org/mockito/invocation/InvocationOnMock.java',
    'package org.mockito.invocation;public interface InvocationOnMock{<T>T getArgument(int i);}'),
  add('org/mockito/stubbing/Answer.java',
    'package org.mockito.stubbing;public interface Answer<T>{T answer(org.mockito.invocation.InvocationOnMock i) throws Throwable;}'),
  add('org/mockito/stubbing/OngoingStubbing.java',
    'package org.mockito.stubbing;public class OngoingStubbing<T>{public OngoingStubbing<T> thenReturn(T t){return this;} public OngoingStubbing<T> thenAnswer(Answer<T>a){return this;}}'),
  add('org/mockito/Mockito.java',`package org.mockito;
public class Mockito {public static <T>T mock(Class<T> c){return null;}
 public static <T>org.mockito.stubbing.OngoingStubbing<T> when(T v){return null;}
 public static <T>T verify(T m,VerificationMode mode){return m;}
 public static VerificationMode never(){return null;} public interface VerificationMode{}
}`),
  add('org/springframework/stereotype/Service.java','package org.springframework.stereotype;public @interface Service {}'),
  add('org/springframework/beans/factory/annotation/Value.java',
    'package org.springframework.beans.factory.annotation;public @interface Value{String value();}'),
  add('com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java',`package com.behsazan.corebanking.productbuilder.oracle;
import java.util.*;import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
public class PdlProductBuilderRepository {
 public boolean loanTypePlanAllowed(long loanType,long planType){return loanType==101L && planType==201L;}
 public Optional<Map<String,Object>> findById(String t,long id){return Optional.empty();}
 public TablePage search(String t,String s,int page,int size,String f,String v){return null;}
 public long insert(String t,Map<String,Object> row,String actor){return 0;}
 public void update(String t,long id,Map<String,Object> row,String actor){}
}`),
  add('com/behsazan/corebanking/productbuilder/application/RuleGovernanceLiteService.java',`package com.behsazan.corebanking.productbuilder.application;
import java.time.LocalDate;import java.util.List;
public class RuleGovernanceLiteService {
 public record Policy(long id,String code,long versionNo,String name,String state,
   LocalDate validFrom,LocalDate validTo,String maker,String checker,String approvalReference,long controlCount) {}
 public record Control(long id,String code,String title,String category,String module,String presence,
   String absence,boolean runtimeRequired,String description) {}
 public Policy policy(long id){throw new UnsupportedOperationException();}
 public List<Control> controls(long id){throw new UnsupportedOperationException();}
}`)
 ];
 run('javac',['-encoding','UTF-8','-cp',classes,'-d',classes,...stubs,
    path.join(srcBase,'DepositProfileIdentityPolicy.java'),
    path.join(srcBase,'LoanTypeClassification.java'),
    path.join(srcBase,'ProductRuleGovernanceService.java'),
    path.join(root,'backend/src/main/java/com/behsazan/corebanking/productbuilder/domain/ProductBuilderModels.java'),
    path.join(root,testPkg,'ProductRuleHierarchyHF7A3Test.java')]);
 console.log('PASS | Historical ProductRuleHierarchyHF7A3Test.java source compiles against HF4 API');
} finally {
 fs.rmSync(tmp,{recursive:true,force:true});
}
console.log('PDL_R30_HF7A3_HF5_STATIC_PASS='+staticPass+' FAIL=0');
