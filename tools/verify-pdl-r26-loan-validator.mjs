/** Real Java behavioural test of ProductBuilderBusinessValidator; needs local JDK, no Maven/Oracle. */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const source=path.join(root,'backend','src','main','java','com','behsazan','corebanking','productbuilder','application');
const scratch=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-r26-java-'));
const ann=path.join(scratch,'org','springframework','stereotype','Component.java');
fs.mkdirSync(path.dirname(ann),{recursive:true});
fs.writeFileSync(ann,'package org.springframework.stereotype; public @interface Component {}\n');
const test=path.join(scratch,'CheckLoanR26.java');
fs.writeFileSync(test,`import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
class CheckLoanR26 {
 static int pass=0;
 static final ProductBuilderBusinessValidator v=new ProductBuilderBusinessValidator();
 static Map<String,Object> props(Object...pairs){Map<String,Object> m=new HashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
 static Map<String,Object> fillBusinessDefaults(String table,Map<String,Object> p){
  if(table.equals("LOAN_PRODUCT_PROCESS_RULE"))for(String key: List.of("MANUAL_APPROVAL_REQUIRED","COMMITTEE_APPROVAL_REQUIRED","MULTI_LEVEL_APPROVAL_REQUIRED","CBI_TRACKING_REQUIRED","EXTERNAL_INQUIRY_REQUIRED","ALLOW_OVERRIDE"))p.putIfAbsent(key,0);
  if(table.equals("LOAN_PRODUCT_COLLATERAL_RULE")){
   p.putIfAbsent("LOAN_PRODUCT_COLLATERAL_ID",701);
   p.putIfAbsent("PRIORITY_NO",1);
   for(String key: List.of("IS_REQUIRED","SUBSTITUTION_ALLOWED","THIRD_PARTY_ALLOWED","CURRENCY_MATCH_REQUIRED"))p.putIfAbsent(key,0);
  }
  return p;
 }
 static void allowed(String name,String table,Map<String,Object> p){
  p=fillBusinessDefaults(table,p);try{v.validate(table,p); System.out.println("PASS | "+name);pass++;}catch(Exception x){throw new AssertionError(name+": "+x.getMessage(),x);}}
 static void blocked(String name,String table,Map<String,Object> p){
  p=fillBusinessDefaults(table,p);try{v.validate(table,p);}catch(ProductBuilderValidationException x){System.out.println("PASS | "+name);pass++;return;}throw new AssertionError(name+" unexpectedly allowed");}
 public static void main(String[] args){
  allowed("process no extra approval", "LOAN_PRODUCT_PROCESS_RULE",props("MANUAL_APPROVAL_REQUIRED",0,"COMMITTEE_APPROVAL_REQUIRED",0,"MULTI_LEVEL_APPROVAL_REQUIRED",0));
  allowed("process manual only", "LOAN_PRODUCT_PROCESS_RULE",props("MANUAL_APPROVAL_REQUIRED",1));
  allowed("process committee only", "LOAN_PRODUCT_PROCESS_RULE",props("COMMITTEE_APPROVAL_REQUIRED",1));
  allowed("process multi-level only", "LOAN_PRODUCT_PROCESS_RULE",props("MULTI_LEVEL_APPROVAL_REQUIRED",1));
  blocked("process manual + committee rejected", "LOAN_PRODUCT_PROCESS_RULE",props("MANUAL_APPROVAL_REQUIRED",1,"COMMITTEE_APPROVAL_REQUIRED",1));
  blocked("process all three rejected", "LOAN_PRODUCT_PROCESS_RULE",props("MANUAL_APPROVAL_REQUIRED",1,"COMMITTEE_APPROVAL_REQUIRED",1,"MULTI_LEVEL_APPROVAL_REQUIRED",1));
  allowed("collateral 120%-150% ratio accepted", "LOAN_PRODUCT_COLLATERAL_RULE",props("MIN_COVERAGE_PERCENT",1.2,"MAX_COVERAGE_PERCENT",1.5,"PRIORITY_NO",1));
  allowed("collateral zero legacy priority accepted", "LOAN_PRODUCT_COLLATERAL_RULE",props("MIN_COVERAGE_PERCENT",0,"MAX_COVERAGE_PERCENT",1,"PRIORITY_NO",0));
  blocked("collateral negative min rejected", "LOAN_PRODUCT_COLLATERAL_RULE",props("MIN_COVERAGE_PERCENT",-0.1,"MAX_COVERAGE_PERCENT",1,"PRIORITY_NO",1));
  blocked("collateral max below min rejected", "LOAN_PRODUCT_COLLATERAL_RULE",props("MIN_COVERAGE_PERCENT",1.5,"MAX_COVERAGE_PERCENT",1.2,"PRIORITY_NO",2));
  blocked("collateral negative rank rejected", "LOAN_PRODUCT_COLLATERAL_RULE",props("PRIORITY_NO",-1));
  allowed("collateral optional type allowed", "LOAN_PRODUCT_COLLATERAL_RULE",props("IS_REQUIRED",0,"PRIORITY_NO",3));
  System.out.println("PDL_R26_LOAN_VALIDATOR_RUNTIME_PASS="+pass+" FAIL=0");
 }
}
`);
function run(cmd,args){const result=spawnSync(cmd,args,{encoding:'utf8'});if(result.error)throw result.error; if(result.status!==0)throw Error(`${cmd} exit ${result.status}: ${result.stdout}\n${result.stderr}`); if(result.stdout)process.stdout.write(result.stdout);if(result.stderr)process.stderr.write(result.stderr);}
try{
run('javac',['-encoding','UTF-8','-d',scratch,ann,path.join(source,'ProductBuilderBusinessValidator.java'),path.join(source,'ProductRuleSemanticContract.java'),path.join(source,'ProductBuilderValidationException.java'),test]);
run('java',['-cp',scratch,'CheckLoanR26']);
}finally{fs.rmSync(scratch,{recursive:true,force:true});}
