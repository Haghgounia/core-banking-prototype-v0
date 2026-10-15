/** HF7-A3/HF5-HF1: prevent historical R15 JUnit from using an incomplete pricing rule fixture. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const testPath = 'backend/src/test/java/com/behsazan/corebanking/productbuilder/application/PdlPricingRelationshipR15Test.java';
const mainPath = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/';
const test = fs.readFileSync(path.join(root,testPath),'utf8');
const semantic = fs.readFileSync(path.join(root,mainPath,'ProductRuleSemanticContract.java'),'utf8');
const pricingTest = test.match(/void earlyTerminationRateIsConditionalAndBounded\(\) \{([\s\S]*?)\n    \}\n/);
let checks = 0;
function check(label, condition) {
 if (!condition) throw Error('FAIL | '+label);
 checks++; console.log('PASS | '+label);
}
check('legacy pricing test exists', Boolean(pricingTest));
check('early-termination scenarios explicitly declare non-tiered',
 Boolean(pricingTest) && (pricingTest[1].match(/"IS_TIERED_RATE", 0/g)||[]).length === 3);
check('negative early-rate cases assert the intended business reason',
 Boolean(pricingTest) && (pricingTest[1].match(/\.hasMessageContaining\(/g)||[]).length === 2);
check('explicit missing-and-invalid tier flag regression retained',
 test.includes('void fixedPricingMustExplicitlyDeclareTierFlag()') &&
 test.includes('"IS_TIERED_RATE", 9') && test.includes('noTierDecision'));
check('zero-flag non-tiered happy path retained',test.includes('void nonTieredPricingWithoutEarlyRateIsAccepted()'));
check('actual semantic contract requires explicit tier and early-termination flags',
 semantic.includes('binary(row, "IS_TIERED_RATE"') && semantic.includes('binary(row, "EARLY_TERMINATION_ALLOWED"'));
check('original Maven JUnit covers Oracle BigDecimal 1.0 and string 1.0',
 test.includes('void oracleDecimalOneAndNumericTextOneRespectEarlyTerminationPermission()') &&
 test.includes('new BigDecimal("1.0")') && test.includes('"EARLY_TERMINATION_ALLOWED", "1.0"'));
check('business boolean adapter normalizes Oracle numeric one without removing strict contract',
 fs.readFileSync(path.join(root,mainPath,'ProductBuilderBusinessValidator.java'),'utf8')
  .includes('new BigDecimal(text).compareTo(BigDecimal.ONE) == 0'));
check('prebuild verifier wired in Windows and Linux builds',
 fs.readFileSync(path.join(root,'build-production.cmd'),'utf8').includes('verify-pdl-r30-hf7a3-hf5-hf1-r15.mjs') &&
 fs.readFileSync(path.join(root,'build-production.sh'),'utf8').includes('verify-pdl-r30-hf7a3-hf5-hf1-r15.mjs'));

const work = fs.mkdtempSync(path.join(os.tmpdir(), 'pdl-hf7a3-r15-'));
function run(bin,args) {
 const result=spawnSync(bin,args,{encoding:'utf8',timeout:80000,maxBuffer:2*1024*1024});
 if (result.stdout) process.stdout.write(result.stdout);
 if (result.status !== 0 || result.error) {
  if (result.stderr) process.stderr.write(result.stderr);
  throw Error('FAIL | '+bin+' failed: '+(result.error?.message||result.status));
 }
}
try {
 const annotation=path.join(work,'org/springframework/stereotype/Component.java');
 fs.mkdirSync(path.dirname(annotation),{recursive:true});
 fs.writeFileSync(annotation,'package org.springframework.stereotype; public @interface Component {}\n');
 const classes=path.join(work,'classes'); fs.mkdirSync(classes);
 run('javac',['-encoding','UTF-8','-d',classes,annotation,
  ...['ProductBuilderValidationException.java','ProductRuleSemanticContract.java','ProductBuilderBusinessValidator.java']
    .map(name=>path.join(root,mainPath,name)),
  path.join(root,'tools/java/HF7A3R15RegressionHarness.java')]);
 run('java',['-cp',classes,'com.behsazan.corebanking.productbuilder.application.HF7A3R15RegressionHarness']);
 // Compile the updated real R15 source to catch Java errors before Maven.
 // JUnit and AssertJ are compile-only stubs; runtime business checks above use original Java.
 function stub(rel, code) {
  const file=path.join(work,rel); fs.mkdirSync(path.dirname(file),{recursive:true});
  fs.writeFileSync(file,code); return file;
 }
 const testStubs=[
  stub('org/junit/jupiter/api/Test.java','package org.junit.jupiter.api; public @interface Test {}'),
  stub('org/assertj/core/api/Assertions.java',`package org.assertj.core.api;
import java.util.*; import java.util.function.*;
public final class Assertions {
 public interface Checked { void call() throws Throwable; }
 public static <T> ListAssert<T> assertThat(List<T> x){return new ListAssert<>();}
 public static ThrowableAssert assertThatThrownBy(Checked x){return new ThrowableAssert();}
 public static ThrowableAssert assertThatCode(Checked x){return new ThrowableAssert();}
 public static final class ListAssert<T>{
  public <R> ListAssert<R> extracting(Function<? super T,? extends R> f){return new ListAssert<>();}
  public ListAssert<T> containsExactly(Object... values){return this;}
  public ListAssert<T> isEmpty(){return this;}
 }
 public static final class ThrowableAssert {
  public ThrowableAssert isInstanceOf(Class<?> c){return this;}
  public ThrowableAssert hasMessageContaining(String s){return this;}
  public void doesNotThrowAnyException(){}
 }
}`),
  stub('com/behsazan/corebanking/productbuilder/application/PdlReferenceOptionService.java',`package com.behsazan.corebanking.productbuilder.application;
import java.util.*;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
public final class PdlReferenceOptionService {
 public static List<SelectOption> businessFormOptions(String table,String field,List<SelectOption> allowed){return List.of();}
}`)
 ];
 run('javac',['-encoding','UTF-8','-cp',classes,'-d',classes,
  path.join(root,'backend/src/main/java/com/behsazan/corebanking/productbuilder/domain/ProductBuilderModels.java'),
  ...testStubs,path.join(root,testPath)]);
 console.log('PASS | Real PdlPricingRelationshipR15Test.java source compiles with JUnit/AssertJ signatures');
} finally {
 fs.rmSync(work,{recursive:true,force:true});
}
console.log('PDL_R30_HF7A3_HF5_HF1_R15_STATIC_PASS='+checks+' FAIL=0');
