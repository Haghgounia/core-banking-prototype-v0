/** HF7-A2: real governance/evidence/validator Java execution, repository stubs only.
 * It validates full-interval coverage, future semantic violations and fail-closed gates.
 * No network/Oracle connection or mutations are made by this tool.
 */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const base='backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const read=(f)=>fs.readFileSync(path.join(root,f),'utf8');
const evaluator=read(base+'application/ProductRuleEvidenceEvaluator.java');
const governance=read(base+'application/ProductRuleGovernanceService.java');
const validator=read(base+'application/ProductBuilderBusinessValidator.java');
const contract=read(base+'application/ProductRuleSemanticContract.java');
const workspace=read('frontend/src/app/features/product-builder/product-workspace.component.ts');
let staticPass=0;
function check(label,value){if(!value)throw Error('FAIL | '+label);staticPass++;console.log('PASS | Static: '+label);}
check('workspace distinguishes invalid semantics and temporal coverage gaps in Persian',workspace.includes("INVALID_SEMANTICS: 'نقص معنایی در قاعده'") && workspace.includes("MISSING_REQUIRED: 'فقدان یا گسست پوشش قاعده الزامی'"));
check('common governance rules also receive semantic approval audit', ['PRODUCT_CHANNEL_RULE','PRODUCT_ELIGIBILITY_RULE','PRODUCT_REQUIRED_DOCUMENT','PRODUCT_REQUIRED_INQUIRY','PRODUCT_ORG_SCOPE'].every(t=>contract.includes('\"'+t+'\"')));
check('scheduled rules use entire validity interval',governance.includes('isApplicableWithin(control.code(), rule, versionStart, versionEnd)'));
check('required controls use complete temporal coverage',governance.includes('requiredForVersion && !temporal.complete()'));
check('future overlapping single-row rules are denied',governance.includes('hasOverlappingActiveRows('));
check('invalid active dates cannot disappear as historical rows',governance.includes('hasInvalidActivePeriod(control.code(), rule)'));
check('approval remains independent of operational admission',governance.includes('boolean canApprove = blockers.isEmpty();') && governance.includes('canOpen = false;'));
check('unknown product domain is closed for origination',governance.includes('if (!Set.of("DEPOSIT", "LOAN").contains(domain))'));
check('no read-only audit DML/DDL',!governance.includes('repository.insert(') && !governance.includes('repository.update(') && !evaluator.includes('executeUpdate('));
check('current HF7-SG gates untouched',governance.includes('if ("LOAN".equals(domain)) {\n            canOpen = false;') && governance.includes('if ("DEPOSIT".equals(domain)'));
check('record dates use inclusive interval comparison',evaluator.includes('!from(row).isAfter(versionEnd)') && evaluator.includes('!to(row).isBefore(versionStart)'));
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-r30-hf7a2-'));
function put(rel,content){const file=path.join(temp,rel);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,content);return file;}
try {
 const stub=[];
 stub.push(put('org/springframework/stereotype/Component.java','package org.springframework.stereotype;public @interface Component {}'));
 stub.push(put('org/springframework/stereotype/Service.java','package org.springframework.stereotype;public @interface Service {}'));
 stub.push(put('org/springframework/beans/factory/annotation/Value.java','package org.springframework.beans.factory.annotation;public @interface Value {String value();}'));
 stub.push(put('com/behsazan/corebanking/productbuilder/application/RuleGovernanceLiteService.java',`
package com.behsazan.corebanking.productbuilder.application;
import java.time.LocalDate;import java.util.List;
public class RuleGovernanceLiteService {
 public record Policy(long id,String code,long versionNo,String name,String state,
 LocalDate validFrom,LocalDate validTo,String maker,String checker,String approvalReference,long controlCount) {}
 public record Control(long id,String code,String title,String category,String module,String presence,
 String absence,boolean runtimeRequired,String description) {}
 public Policy policy(long id){throw new UnsupportedOperationException();}
 public List<Control> controls(long id){throw new UnsupportedOperationException();}
}`));
 let fixture=read('backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceFixture.java');
 fixture=fixture.replace('final class ProductRuleGovernanceFixture {','public final class ProductRuleGovernanceFixture {')
     .replace('    static Map<String, Object> row(String table) {','    public static Map<String, Object> row(String table) {');
 stub.push(put('com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceFixture.java',fixture));
 stub.push(put('com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java',`
package com.behsazan.corebanking.productbuilder.oracle;
import com.behsazan.corebanking.productbuilder.application.ProductRuleGovernanceFixture;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import java.util.*;
public class PdlProductBuilderRepository {
 public String domain="LOAN",family="QARD_HASAN_LOAN";
 public boolean futureInvalid=false,channelGap=false,invalidActiveInterval=false,missingChannelDecision=false,missingInquiryStage=false;
 public Optional<Map<String,Object>> findById(String table,long id){
  if(table.equals("PRODUCT_VERSION")) return Optional.of(Map.of("PRODUCT_ID",9L,"VALID_FROM","2026-10-10","VALID_TO","2026-10-30","VERSION_STATUS_CODE","DRAFT"));
  if(table.equals("PRODUCT")) return Optional.of(Map.of("PRODUCT_CLASS_CODE",domain,"PRODUCT_FAMILY_CODE",family,"PRODUCT_STATUS_CODE","DRAFT"));
  return Optional.empty();
 }
 public TablePage search(String table,String text,int page,int size,String filterColumn,String filterVal){
  if(table.equals("PRODUCT_VERSION_MODULE") || page>0) return new TablePage(List.of(),0,page,size);
  // HF6: historical smoke fixture has no optional child records; this does
  // not represent permission or runtime integration.
  if(Set.of("PRODUCT_CHANNEL_OPERATION","LOAN_ELIGIBILITY_EXTENSION",
         "DEPOSIT_PRODUCT_CLOSURE_PRECHECK","DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",
         "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE","PRODUCT_ELIGIBILITY_CRITERION","CORRESPONDENT_ACCOUNT_SETTLEMENT_RULE").contains(table))
    return new TablePage(List.of(),0,page,size);
  Map<String,Object> initial=ProductRuleGovernanceFixture.row(table);
  List<Map<String,Object>> entries=new ArrayList<>();
  if(table.equals("PRODUCT_CHANNEL_RULE") && channelGap){
   initial=new LinkedHashMap<>(initial);initial.put("VALID_TO","2026-10-20");
  }
  if(table.equals("PRODUCT_CHANNEL_RULE") && missingChannelDecision){
   initial=new LinkedHashMap<>(initial);initial.remove("IS_ALLOWED");
  }
  if(table.equals("PRODUCT_REQUIRED_INQUIRY") && missingInquiryStage){
   initial=new LinkedHashMap<>(initial);initial.remove("REQUIREMENT_STAGE_CODE");
  }
  if(table.equals("LOAN_PRODUCT_PROCESS_RULE") && (futureInvalid || invalidActiveInterval)) {
   initial=new LinkedHashMap<>(initial);initial.put("VALID_FROM","2026-10-10");initial.put("VALID_TO","2026-10-15");
   entries.add(initial);
   var later=new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table));
   later.put("VALID_FROM","2026-10-16");
   if(futureInvalid) later.remove("ALLOW_OVERRIDE");
   if(invalidActiveInterval) later.put("VALID_TO","2026-10-14");
   entries.add(later);
  } else entries.add(initial);
  return new TablePage(entries,entries.size(),page,size);
 }
}`));
 stub.push(put('TemporalSmoke.java',`
import com.behsazan.corebanking.productbuilder.application.*;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import java.time.LocalDate;import java.util.*;
public class TemporalSmoke {
 static int count=0;
 static void ok(String label,boolean valid){if(!valid)throw new AssertionError(label);count++;System.out.println("PASS | Java: "+label);}
 static void invalid(ProductBuilderBusinessValidator validator,String table,Map<String,Object> row,String label){
  try{validator.validate(table,row);throw new AssertionError(label);}catch(ProductBuilderValidationException expected){ok(label,true);}}
 static Map<String,Object> active(String from,String to){var m=new LinkedHashMap<String,Object>();
  m.put("RECORD_STATUS_CODE","ACTIVE");if(from!=null)m.put("VALID_FROM",from);if(to!=null)m.put("VALID_TO",to);return m;}
 static ProductRuleEvidenceEvaluator.TemporalCoverage cover(List<Map<String,Object>> rows,String from,String to){
  return ProductRuleEvidenceEvaluator.temporalCoverage("LOAN_PRODUCT_PROCESS_RULE",rows,LocalDate.parse(from),LocalDate.parse(to));}
 static ProductRuleGovernanceService.GovernanceReport report(PdlProductBuilderRepository repo){
  return new ProductRuleGovernanceService(repo,new RuleGovernanceLiteService(),new ProductBuilderBusinessValidator()).report(55);}
 public static void main(String[] args){
  var l=List.of(active("2026-10-10","2026-10-15"),active("2026-10-16","2026-10-30"));
  ok("contiguous inclusive schedules",cover(l,"2026-10-10","2026-10-30").complete());
  ok("two disjoint time slices do not overlap",
    !ProductRuleEvidenceEvaluator.hasOverlappingActiveRows("LOAN_PRODUCT_PROCESS_RULE",l,LocalDate.parse("2026-10-10"),LocalDate.parse("2026-10-30")));
  var gap=List.of(active("2026-10-10","2026-10-14"),active("2026-10-16","2026-10-30"));
  var coverage=cover(gap,"2026-10-10","2026-10-30");
  ok("date hole detected",!coverage.complete()&&coverage.firstGap().equals(LocalDate.parse("2026-10-15")));
  ok("midterm no-coverage detected",!cover(List.of(active("2026-10-15",null)),"2026-10-10","2026-10-30").complete());
  ok("unbounded end handled without overflow",cover(List.of(active(null,null)),"2026-10-10","9999-12-31").complete());
  var wrong=active("2026-10-20","2026-10-18");
  ok("invalid period recognized",ProductRuleEvidenceEvaluator.hasInvalidActivePeriod("LOAN_PRODUCT_PROCESS_RULE",wrong));
  var stale=active("2026-10-14","2026-10-14");stale.put("RECORD_STATUS_CODE","INACTIVE");
  ok("inactive history excluded",cover(List.of(stale),"2026-10-10","2026-10-30").firstGap().equals(LocalDate.parse("2026-10-10")));
  ok("future active row included for audit",ProductRuleEvidenceEvaluator.isApplicableWithin("LOAN_PRODUCT_PROCESS_RULE",active("2026-10-23",null),LocalDate.parse("2026-10-10"),LocalDate.parse("2026-10-30")));
  ok("expired active row excluded from audit",!ProductRuleEvidenceEvaluator.isApplicableWithin("LOAN_PRODUCT_PROCESS_RULE",active(null,"2026-10-09"),LocalDate.parse("2026-10-10"),LocalDate.parse("2026-10-30")));
  var conflict=List.of(active("2026-10-10","2026-10-20"),active("2026-10-20","2026-10-25"));
  ok("overlap at inclusive boundary",ProductRuleEvidenceEvaluator.hasOverlappingActiveRows("LOAN_PRODUCT_PROCESS_RULE",conflict,LocalDate.parse("2026-10-10"),LocalDate.parse("2026-10-30")));
  var repo=new PdlProductBuilderRepository();
  var result=report(repo);ok("valid loan design canApprove unaffected",result.canApprove());
  ok("operational loan origination still denied",!result.canOpen());
  repo.futureInvalid=true;result=report(repo);
  ok("scheduled invalid process blocks approval",!result.canApprove()&&result.effectiveRules().stream()
    .anyMatch(e->e.code().equals("LOAN_PRODUCT_PROCESS_RULE")&&e.status().equals("INVALID_SEMANTICS")));
  repo.futureInvalid=false;repo.invalidActiveInterval=true;result=report(repo);
  ok("inverted active dates block approval",!result.canApprove()&&result.effectiveRules().stream()
    .anyMatch(e->e.code().equals("LOAN_PRODUCT_PROCESS_RULE")&&e.status().equals("INVALID_SEMANTICS")));
  repo.invalidActiveInterval=false;repo.channelGap=true;result=report(repo);
  ok("mid-version coverage gap blocks approval",!result.canApprove()&&result.effectiveRules().stream()
    .anyMatch(e->e.code().equals("PRODUCT_CHANNEL_RULE")&&e.status().equals("MISSING_REQUIRED")&&e.reason().contains("2026-10-21")));
  repo.channelGap=false;repo.missingChannelDecision=true;result=report(repo);
  ok("ambiguous common channel rule blocks approval",!result.canApprove()&&result.effectiveRules().stream()
    .anyMatch(e->e.code().equals("PRODUCT_CHANNEL_RULE")&&e.status().equals("INVALID_SEMANTICS")));
  repo.missingChannelDecision=false;repo.missingInquiryStage=true;result=report(repo);
  ok("incomplete common inquiry blocks approval",!result.canApprove()&&result.effectiveRules().stream()
    .anyMatch(e->e.code().equals("PRODUCT_REQUIRED_INQUIRY")&&e.status().equals("INVALID_SEMANTICS")));
  repo.missingInquiryStage=false;repo.domain="NOT_SUPPORTED";result=report(repo);
  ok("unknown domain neither approves nor opens",!result.canApprove()&&!result.canOpen());
  var validator=new ProductBuilderBusinessValidator();
  var opening=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("DEPOSIT_PRODUCT_OPENING_RULE"));
  opening.put("MIN_OPENING_AMOUNT",300);opening.put("MAX_ALLOWED_BALANCE",200);
  invalid(validator,"DEPOSIT_PRODUCT_OPENING_RULE",opening,"opening above max allowed balance rejected");
  opening.remove("MIN_OPENING_AMOUNT");opening.remove("MAX_ALLOWED_BALANCE");
  opening.put("MAX_CUSTOMER_ACCOUNT_COUNT","1.5");
  invalid(validator,"DEPOSIT_PRODUCT_OPENING_RULE",opening,"noninteger account count rejected");
  var loan=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("LOAN_PRODUCT_REPAYMENT_RULE"));
  loan.put("MIN_INSTALLMENT_COUNT","1.5");
  invalid(validator,"LOAN_PRODUCT_REPAYMENT_RULE",loan,"fractional installments rejected");
  var proc=new LinkedHashMap<>(ProductRuleGovernanceFixture.row("LOAN_PRODUCT_PROCESS_RULE"));
  proc.put("CONTRACT_VALIDITY_DAYS","1.5");
  invalid(validator,"LOAN_PRODUCT_PROCESS_RULE",proc,"fractional approval validity rejected");
  var eligible=new LinkedHashMap<String,Object>();eligible.put("REQUIRED_GUARANTOR_COUNT","1.5");
  eligible.put("CBI_TRACKING_REQUIRED",0);eligible.put("CREDIT_INQUIRY_REQUIRED",0);eligible.put("AML_CHECK_REQUIRED",0);
  invalid(validator,"LOAN_ELIGIBILITY_EXTENSION",eligible,"fractional guarantor count rejected");
  System.out.println("PDL_R30_HF7A2_JAVA_PASS="+count+" FAIL=0");
 }
}`));
 const app=path.join(root,base,'application');
 const actual=['ProductBuilderValidationException.java','ProductRuleSemanticContract.java',
  'ProductBuilderBusinessValidator.java','ProductRuleEvidenceEvaluator.java','ProductRuleHierarchyAudit.java','ProductRuleGovernanceService.java']
  .map(name=>path.join(app,name));
 actual.push(path.join(root,base,'domain/ProductBuilderModels.java'));
 const bin=path.join(temp,'classes');fs.mkdirSync(bin);
 for(const [cmd,args] of [
  ['javac',['-encoding','UTF-8','-d',bin,...actual,...stub]],
  ['java',['-cp',bin,'TemporalSmoke']]
 ]){
  const result=spawnSync(cmd,args,{encoding:'utf8',maxBuffer:3*1024*1024});
  if(result.stdout)process.stdout.write(result.stdout);
  if(result.error||result.status!==0)throw Error(cmd+' failed '+result.status+' '+(result.stderr||result.error));
 }
}finally{fs.rmSync(temp,{recursive:true,force:true});}
console.log('PDL_R30_HF7A2_STATIC_PASS='+staticPass+' FAIL=0');
