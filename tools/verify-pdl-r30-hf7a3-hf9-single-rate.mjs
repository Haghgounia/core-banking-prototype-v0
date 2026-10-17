/** HF9 read-only real Java/JUnit boundary suite. No Oracle, business DML or network. */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const app='backend/src/main/java/com/behsazan/corebanking/productbuilder/application/';
const test='backend/src/test/java/com/behsazan/corebanking/productbuilder/application/';
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const audit=read(app+'ProductRuleHierarchyAudit.java');
const contract=read(app+'ProductRuleSemanticContract.java');
const policy=read(app+'ProductPricingSingleRatePolicy.java');
const governance=read(app+'ProductRuleGovernanceService.java');
const tests=read(test+'ProductRateTierHF7A3HF9Test.java');
let checks=0;
function check(label,condition){if(!condition)throw new Error('FAIL | '+label);checks++;console.log('PASS | '+label);}
check('SINGLE_APPLICABLE_RATE is non-marginal',policy.includes('SINGLE_APPLICABLE_RATE')&&policy.includes('entire base amount')&&policy.includes('NOT progressive/marginal'));
check('amount lower bound inclusive, upper exclusive',policy.includes('value.compareTo(min) >= 0') && policy.includes('value.compareTo(max) < 0'));
check('NULL amount endpoints are not zero',policy.includes('min == null || value.compareTo(min)') && policy.includes('max == null || value.compareTo(max)'));
check('zero width amount tiers fail closed',policy.includes('hi.compareTo(lo) <= 0'));
check('component scope participates in tier proof',audit.includes('ProductPricingSingleRatePolicy.auditComponent(componentId,')&&policy.includes('r.within(componentRange)'));
check('every component is audited independently',audit.includes('int audited = 0;') && audit.includes('audited == 0'));
check('future-effective dates split proof interval',policy.includes('TreeSet<LocalDate> slices')&&policy.includes('end.plusDays(1)'));
check('overlap cannot be resolved by display priority',!policy.includes('DISPLAY_ORDER') && policy.includes('انتخاب نرخ یکتا نیست'));
check('unknown duration null/units fail closed',policy.includes('lower != upper')&&policy.includes('کد عددی DPS بدون نگاشت مصوب'));
check('duration-dependent pricing is NOT falsely certified',policy.includes('دامنه مجاز مدت و معنای شمول مرزهای آن'));
check('no duration unit conversion or month=30',!policy.includes('plusMonths')&&!policy.includes('30 days')&&!policy.includes('365 days'));
check('TIERED method/flag mismatch blocked at publication',audit.includes('tiered != tierMethod'));
check('real governance report retains tier audit',governance.includes('? hasValidPricingTiers(raw, versionStart, versionEnd)')&&governance.includes('if (childFinding.conflict()) conflict = true;'));
check('real governance report accounts for invalid semantics',governance.includes('childFinding.invalidSemantics()'));
check('no modification of canOpen restrictions',governance.includes('if ("LOAN".equals(domain)) {\n            canOpen = false;')&&governance.includes('کنترل انتشار عملیاتی نوسترو/وسترو'));
check('no database write APIs in tier proof',!/(executeUpdate|DataSource|jdbcClient|\b(?:insert|update|delete|merge)\s*\()/i.test(policy));
check('new JUnit exercises amount/temporal/term boundaries',tests.includes('exactUpperBoundarySelectsOnlyNextTier')&&tests.includes('sameDayCutoverWithoutDistinctAmountOrTermIsAmbiguous')&&tests.includes('incompleteTermBoundsAreRejectedWithoutInferredNullMeaning'));
check('Windows build includes HF9',read('build-production.cmd').includes('verify-pdl-r30-hf7a3-hf9-single-rate.mjs'));
check('Unix build includes HF9',read('build-production.sh').includes('verify-pdl-r30-hf7a3-hf9-single-rate.mjs'));
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-hf9-'));
function put(rel,body){const dst=path.join(tmp,rel);fs.mkdirSync(path.dirname(dst),{recursive:true});fs.writeFileSync(dst,body);return dst;}
function run(cmd,args){const result=spawnSync(cmd,args,{encoding:'utf8',cwd:root,timeout:90000,maxBuffer:10*1024*1024});
 if(result.stdout)process.stdout.write(result.stdout);
 if(result.status!==0||result.error){if(result.stderr)process.stderr.write(result.stderr);throw new Error(cmd+' failed: '+(result.error?.message||result.status));}}
try {
 const component=put('org/springframework/stereotype/Component.java','package org.springframework.stereotype; public @interface Component {}');
 const annotation=put('org/junit/jupiter/api/Test.java',`package org.junit.jupiter.api; @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD) public @interface Test {}`);
 const executable=put('org/junit/jupiter/api/function/Executable.java','package org.junit.jupiter.api.function; @FunctionalInterface public interface Executable {void execute() throws Throwable;}');
 const asserts=put('org/junit/jupiter/api/Assertions.java',String.raw`
package org.junit.jupiter.api;
import java.util.Objects;
import org.junit.jupiter.api.function.Executable;
public class Assertions {
 public static void assertTrue(boolean b){if(!b)throw new AssertionError("Expected true");}
 public static void assertFalse(boolean b){if(b)throw new AssertionError("Expected false");}
 public static void assertEquals(Object e,Object a){assertEquals(e,a,"");}
 public static void assertEquals(Object e,Object a,String m){if(!Objects.equals(e,a))throw new AssertionError("Expected "+e+", got "+a+" "+m);}
 public static <T extends Throwable> T assertThrows(Class<T> clazz,Executable cb){try{cb.execute();}catch(Throwable ex){if(clazz.isInstance(ex))return clazz.cast(ex);throw new AssertionError("Wrong exception: "+ex,ex);}throw new AssertionError("Expected "+clazz.getName()+" exception");}
}`);
 const javaMain=put('RateTierHf9Runner.java',String.raw`
import java.lang.reflect.*;
public class RateTierHf9Runner {
 public static void main(String... args)throws Exception {
  var clazz=Class.forName("com.behsazan.corebanking.productbuilder.application.ProductRateTierHF7A3HF9Test");
  var ctor=clazz.getDeclaredConstructor();ctor.setAccessible(true);Object test=ctor.newInstance();int pass=0;
  for(var method:clazz.getDeclaredMethods()){
   if(!method.isAnnotationPresent(org.junit.jupiter.api.Test.class))continue;
   method.setAccessible(true);
   try {method.invoke(test);System.out.println("PASS | JUnit HF9: "+method.getName());pass++;}
   catch(InvocationTargetException e){System.err.println("FAIL | JUnit HF9: "+method.getName());e.getCause().printStackTrace();System.exit(1);}
  }
  if(pass<25)throw new AssertionError("Expected at least 25 executed real HF9 tests, got "+pass);
  System.out.println("PDL_R30_HF7A3_HF9_JAVA_PASS="+pass+" FAIL=0");
 }
}`);
 const sources=['ProductBuilderValidationException.java','ProductRuleSemanticContract.java',
  'ProductPricingSingleRatePolicy.java','ProductRuleEvidenceEvaluator.java',
  'ProductBuilderBusinessValidator.java','ProductRuleHierarchyAudit.java'].map(x=>path.join(root,app+x));
 const junit=path.join(root,test+'ProductRateTierHF7A3HF9Test.java');
 const classes=path.join(tmp,'classes');fs.mkdirSync(classes,{recursive:true});
 run('javac',['-encoding','UTF-8','-d',classes,component,annotation,executable,asserts,...sources,junit,javaMain]);
 run('java',['-cp',classes,'RateTierHf9Runner']);
 console.log('PDL_R30_HF7A3_HF9_STATIC_PASS='+checks+' FAIL=0');
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
