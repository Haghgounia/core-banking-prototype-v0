/** R30-HF7-A3 — read-only parent/child, status and temporal integrity checks.
 * No HTTP, Oracle session, schema change or writes. JDK 21 required.
 */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const base='backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const read=(relative)=>fs.readFileSync(path.join(root,relative),'utf8');
const semantics=read(base+'application/ProductRuleSemanticContract.java');
const evidence=read(base+'application/ProductRuleEvidenceEvaluator.java');
const hierarchy=read(base+'application/ProductRuleHierarchyAudit.java');
const governance=read(base+'application/ProductRuleGovernanceService.java');
const win=read('build-production.cmd');const unix=read('build-production.sh');
let pass=0;
function check(label,condition){if(!condition)throw Error('FAIL | '+label);pass++;console.log('PASS | Static: '+label);}
check('No accidental Oracle mutations or migrations in A3 class',!hierarchy.includes('executeUpdate(')&&!hierarchy.includes('repository.insert(')&&!hierarchy.includes('repository.update('));
check('A3 parent-child audit participates in governance',governance.includes('ProductRuleHierarchyAudit.audit(')&&governance.includes('invalidRows += childAudit.invalidCount()'));
check('Term child must cover entire applicable parent interval',governance.includes('temporalCoverage("DEPOSIT_PRODUCT_ALLOWED_TERM"')&&governance.includes('windowStart, windowEnd).complete()'));
check('Tier child must cover entire applicable parent interval',governance.includes('temporalCoverage("PRODUCT_RATE_TIER"')&&governance.includes('allTiers, windowStart, windowEnd'));
check('Invalid IS_ACTIVE no longer means inactive',evidence.includes('UNRECOGNIZED_IS_ACTIVE')&&evidence.includes('value.compareTo(java.math.BigDecimal.ONE)'));
check('Child contracts cover allowed term, pricing components and rate tiers', ['DEPOSIT_PRODUCT_ALLOWED_TERM','PRODUCT_PRICING_COMPONENT','PRODUCT_RATE_TIER'].every(t=>semantics.includes('case "'+t+'"')));
check('Duplicate common document and inquiry controls checked',evidence.includes('case "PRODUCT_REQUIRED_DOCUMENT" -> List.of')&&evidence.includes('case "PRODUCT_REQUIRED_INQUIRY" -> List.of'));
check('Channel, loan eligibility and closure relationships explicitly audited', ['PRODUCT_CHANNEL_OPERATION','LOAN_ELIGIBILITY_EXTENSION','DEPOSIT_PRODUCT_CLOSURE_PRECHECK','DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE','DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE'].every(t=>hierarchy.includes('"'+t+'"')));
check('Legacy loan contribution is revalidated against actual LOAN_TYPE at approval',governance.includes('validateLoanCivilContribution(versionId, rule)')&&governance.includes('LoanCivilPartnershipPolicy.matches(loanType)'));
check('Safety gate for loans remains operational deny',governance.includes('if ("LOAN".equals(domain)) {\n            canOpen = false;'));
check('Deposit operational deny remains',governance.includes('if ("DEPOSIT".equals(domain)'));
check('A3 verifier included in Windows and Unix production builds',win.includes('verify-pdl-r30-hf7a3-integrity.mjs')&&unix.includes('verify-pdl-r30-hf7a3-integrity.mjs'));
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-r30-hf7a3-'));
function put(relative,contents){const filename=path.join(temp,relative);fs.mkdirSync(path.dirname(filename),{recursive:true});fs.writeFileSync(filename,contents);return filename;}
try {
 const stubs=[
   put('org/springframework/stereotype/Component.java','package org.springframework.stereotype;public @interface Component {}'),
   put('org/springframework/stereotype/Service.java','package org.springframework.stereotype;public @interface Service {}'),
   put('org/springframework/beans/factory/annotation/Value.java','package org.springframework.beans.factory.annotation;public @interface Value {String value();}'),
   put('com/behsazan/corebanking/productbuilder/application/RuleGovernanceLiteService.java',`
package com.behsazan.corebanking.productbuilder.application;
import java.time.LocalDate;import java.util.List;
public class RuleGovernanceLiteService {
 public record Policy(long id,String code,long versionNo,String name,String state,
 LocalDate validFrom,LocalDate validTo,String maker,String checker,String approvalReference,long controlCount) {}
 public record Control(long id,String code,String title,String category,String module,String presence,
 String absence,boolean runtimeRequired,String description) {}
 public Policy policy(long id){throw new UnsupportedOperationException();}
 public List<Control> controls(long id){throw new UnsupportedOperationException();}
}`),
   put('com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java',`
package com.behsazan.corebanking.productbuilder.oracle;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import java.util.*;
public class PdlProductBuilderRepository {
 public Map<String,Map<String,Object>> singleRows=new HashMap<>();
 public Map<String,List<Map<String,Object>>> childRows=new HashMap<>();
 public Optional<Map<String,Object>> findById(String table,long id){return Optional.ofNullable(singleRows.get(table));}
 public TablePage search(String table,String text,int page,int size,String filterColumn,String filterValue){
  String key=table+":"+filterColumn+":"+filterValue;
  var rows=childRows.getOrDefault(key,List.of());
  return new TablePage(page==0?rows:List.of(),rows.size(),page,size);
 }
}`),
   put('Hf7A3Smoke.java',`
import com.behsazan.corebanking.productbuilder.application.*;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
import java.math.BigDecimal;import java.time.LocalDate;import java.lang.reflect.Method;import java.util.*;
public class Hf7A3Smoke {
 static final LocalDate S=LocalDate.parse("2026-10-10"),E=LocalDate.parse("2026-10-20");
 static int passed=0;
 static void ok(String label,boolean value){if(!value)throw new AssertionError(label);passed++;System.out.println("PASS | Java: "+label);}
 static Map<String,Object> row(Object...fields){var map=new LinkedHashMap<String,Object>();for(int i=0;i<fields.length;i+=2)map.put((String)fields[i],fields[i+1]);return map;}
 static boolean invalid(ProductBuilderBusinessValidator v,String t,Map<String,Object> r){try{v.validate(t,r);return false;}catch(ProductBuilderValidationException e){return true;}}
 static boolean covered(Object svc,String method,Map<String,Object> parent)throws Exception{
  var m=ProductRuleGovernanceService.class.getDeclaredMethod(method,List.class,LocalDate.class,LocalDate.class);
  m.setAccessible(true);return (boolean)m.invoke(svc,List.of(parent),S,E);
 }
 public static void main(String[] args)throws Exception{
  var v=new ProductBuilderBusinessValidator();
  var unknown=ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_ALLOWED_TERM",List.of(row("IS_ACTIVE",9)),S,E);
  ok("invalid IS_ACTIVE is conflicting, not silently inactive",unknown.conflict()&&unknown.unrecognizedStatuses()==1);
  ok("NUMBER(1) decimal scale 1.0 recognized active",ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_ALLOWED_TERM",List.of(row("IS_ACTIVE",new BigDecimal("1.0"))),S,E).effectiveCount()==1);
  ok("IS_ACTIVE false still inactive",ProductRuleEvidenceEvaluator.evaluate("DEPOSIT_PRODUCT_ALLOWED_TERM",List.of(row("IS_ACTIVE",0)),S,E).effectiveCount()==0);
  var docs=row("DOCUMENT_TYPE_ID",8,"REQUIREMENT_STAGE_CODE","OPENING","RULE_STATUS_CODE","ACTIVE");
  ok("duplicate same document-stage rejected",ProductRuleEvidenceEvaluator.evaluate("PRODUCT_REQUIRED_DOCUMENT",List.of(docs,docs),S,E).conflict());
  var inq=row("INQUIRY_TYPE_CODE","KYC","REQUIREMENT_STAGE_CODE","OPENING","RULE_STATUS_CODE","ACTIVE");
  ok("duplicate same inquiry-stage rejected",ProductRuleEvidenceEvaluator.evaluate("PRODUCT_REQUIRED_INQUIRY",List.of(inq,inq),S,E).conflict());
  var ch=row("OPERATION_CODE","OPEN","IS_ALLOWED",1,"REQUIRES_ADDITIONAL_AUTH",0);
  ok("duplicate operation in a channel rejected",ProductRuleEvidenceEvaluator.evaluate("PRODUCT_CHANNEL_OPERATION",List.of(ch,ch),S,E).conflict());
  ok("term requires a governed unit",invalid(v,"DEPOSIT_PRODUCT_ALLOWED_TERM",row("TERM_VALUE",30)));
  ok("term cannot have fractional duration",invalid(v,"DEPOSIT_PRODUCT_ALLOWED_TERM",row("TERM_UNIT_CODE","DAY","TERM_VALUE",1.5)));
  ok("valid term passes",!invalid(v,"DEPOSIT_PRODUCT_ALLOWED_TERM",row("TERM_UNIT_CODE","DAY","TERM_VALUE",30,"IS_ACTIVE",1,"IS_DEFAULT",0)));
  ok("tier rate cannot be missing",invalid(v,"PRODUCT_RATE_TIER",row("TIER_NO",1)));
  ok("tier cannot exceed full fraction",invalid(v,"PRODUCT_RATE_TIER",row("TIER_NO",1,"RATE_VALUE",new BigDecimal("1.01"))));
  ok("tier with a term requires term unit",invalid(v,"PRODUCT_RATE_TIER",row("TIER_NO",1,"RATE_VALUE",new BigDecimal("0.1"),"MIN_TERM_VALUE",30)));
  ok("valid rate tier passes",!invalid(v,"PRODUCT_RATE_TIER",row("TIER_NO",1,"RATE_VALUE",new BigDecimal("0.1"))));
  ok("pricing component cannot have rate outside its own bounds",invalid(v,"PRODUCT_PRICING_COMPONENT",row("RATE_VALUE",new BigDecimal("0.5"),"MAX_RATE",new BigDecimal("0.3"))));
  var tier1=row("TIER_NO",1,"RATE_VALUE",new BigDecimal("0.1"),"VALID_FROM","2026-10-10","VALID_TO","2026-10-15");
  var tier2=row("TIER_NO",1,"RATE_VALUE",new BigDecimal("0.2"),"VALID_FROM","2026-10-15","VALID_TO","2026-10-20");
  ok("overlap on tier number at inclusive date denied",ProductRuleEvidenceEvaluator.evaluate("PRODUCT_RATE_TIER",List.of(tier1,tier2),S,E).conflict());
  tier2.put("VALID_FROM","2026-10-16");
  ok("adjacent successive tier periods accepted",!ProductRuleEvidenceEvaluator.evaluate("PRODUCT_RATE_TIER",List.of(tier1,tier2),S,E).conflict());
  var deniedParent=row("CHANNEL_RULE_ID",7,"IS_ALLOWED",0);
  var audit=ProductRuleHierarchyAudit.audit("PRODUCT_CHANNEL_RULE",List.of(deniedParent),S,E,"DEPOSIT",(t,f,id)->List.of(ch),v);
  ok("denied channel with allowed child detected",audit.invalidCount()>0&&audit.firstProblem().contains("غیرمجاز"));
  var allowedParent=row("CHANNEL_RULE_ID",7,"IS_ALLOWED",1);
  ok("explicit allowed parent/child consistent",ProductRuleHierarchyAudit.audit("PRODUCT_CHANNEL_RULE",List.of(allowedParent),S,E,"DEPOSIT",(t,f,id)->List.of(ch),v).invalidCount()==0);
  var eligible=row("ELIGIBILITY_RULE_ID",55);
  var incomplete=row("REQUIRED_GUARANTOR_COUNT",2.4,"CBI_TRACKING_REQUIRED",0,"CREDIT_INQUIRY_REQUIRED",0,"AML_CHECK_REQUIRED",0);
  ok("loan eligibility extension invalid parent-child case detected",ProductRuleHierarchyAudit.audit("PRODUCT_ELIGIBILITY_RULE",List.of(eligible),S,E,"LOAN",(t,f,id)->List.of(incomplete),v).invalidCount()>0);
  ok("deposit eligibility does not enforce loan-only extension",ProductRuleHierarchyAudit.audit("PRODUCT_ELIGIBILITY_RULE",List.of(eligible),S,E,"DEPOSIT",(t,f,id)->List.of(incomplete),v).invalidCount()==0);
  var closure=row("CLOSURE_RULE_ID",60,"IS_CLOSURE_ALLOWED",1);
  var wrong=row("CHECK_TYPE_CODE","HOLD","FAILURE_ACTION_CODE","BLOCK","IS_MANDATORY",1,"SEQUENCE_NO",0);
  ok("invalid closure precheck child detected",ProductRuleHierarchyAudit.audit("DEPOSIT_PRODUCT_CLOSURE_RULE",List.of(closure),S,E,"DEPOSIT",(t,f,id)->t.equals("DEPOSIT_PRODUCT_CLOSURE_PRECHECK")?List.of(wrong):List.of(),v).invalidCount()>0);
  var invalidDates=row("OPERATION_CODE","OPEN","IS_ALLOWED",1,"REQUIRES_ADDITIONAL_AUTH",0,"VALID_FROM","2026-10-20","VALID_TO","2026-10-10");
  ok("invalid active child dates rejected",ProductRuleHierarchyAudit.audit("PRODUCT_CHANNEL_RULE",List.of(allowedParent),S,E,"DEPOSIT",(t,f,id)->List.of(invalidDates),v).invalidCount()>0);
  var repo=new PdlProductBuilderRepository();
  var svc=new ProductRuleGovernanceService(repo,new RuleGovernanceLiteService(),v);
  var termParent=row("TERM_RULE_ID",9,"IS_ACTIVE",1);
  var termChild=row("TERM_UNIT_CODE","DAY","TERM_VALUE",30,"IS_ACTIVE",1,"EFFECTIVE_FROM_DATE","2026-10-10","EFFECTIVE_TO_DATE","2026-10-11");
  repo.childRows.put("DEPOSIT_PRODUCT_ALLOWED_TERM:TERM_RULE_ID:9",List.of(termChild));
  ok("term one-day child cannot certify entire parent",!covered(svc,"hasActiveAllowedTerm",termParent));
  termChild.put("EFFECTIVE_TO_DATE","2026-10-20");
  ok("term complete child coverage accepted",covered(svc,"hasActiveAllowedTerm",termParent));
  termChild.put("IS_ACTIVE",9);
  ok("term unrecognized active flag cannot certify",!covered(svc,"hasActiveAllowedTerm",termParent));
  var pricingParent=row("PRICING_RULE_ID",11,"IS_TIERED_RATE",1);
  repo.childRows.put("PRODUCT_PRICING_COMPONENT:PRICING_RULE_ID:11",List.of(row("PRICING_COMPONENT_ID",12,"RATE_VALUE",new BigDecimal("0.10"))));
  var pricingTier=row("TIER_NO",1,"RATE_VALUE",new BigDecimal("0.10"),"VALID_FROM","2026-10-10","VALID_TO","2026-10-11");
  repo.childRows.put("PRODUCT_RATE_TIER:PRICING_COMPONENT_ID:12",List.of(pricingTier));
  ok("tiered pricing one-day child cannot certify entire parent",!covered(svc,"hasValidPricingTiers",pricingParent));
  pricingTier.put("VALID_TO","2026-10-20");
  ok("tiered pricing child covering whole interval accepted",covered(svc,"hasValidPricingTiers",pricingParent));
  pricingTier.put("RATE_VALUE",new BigDecimal("1.7"));
  ok("tiered pricing out of bounds refused",!covered(svc,"hasValidPricingTiers",pricingParent));
  var civilCheck=ProductRuleGovernanceService.class.getDeclaredMethod("validateLoanCivilContribution",long.class,Map.class);
  civilCheck.setAccessible(true);
  var financial=row("MIN_CUSTOMER_CONTRIBUTION",new BigDecimal("0.1"),"MAX_CUSTOMER_CONTRIBUTION",new BigDecimal("0.3"));
  repo.childRows.put("LOAN_PRODUCT_PROFILE:PRODUCT_VERSION_ID:55",List.of(row("LOAN_TYPE_ID",123L)));
  repo.singleRows.put("LOAN_TYPE",row("LOAN_GROUP",3,"LOAN_TYPE_CODE","MUDARABA","LOAN_TYPE_NAME","مضاربه"));
  boolean rejected=false;
  try{civilCheck.invoke(svc,55L,financial);}catch(java.lang.reflect.InvocationTargetException ex){rejected=ex.getCause() instanceof ProductBuilderValidationException;}
  ok("legacy contribution on non-civil contract blocks approval",rejected);
  repo.singleRows.put("LOAN_TYPE",row("LOAN_GROUP",3,"LOAN_TYPE_CODE","CIVIL_PARTNERSHIP","LOAN_TYPE_NAME","مشارکت مدنی"));
  civilCheck.invoke(svc,55L,financial);
  ok("civil partnership contribution remains valid",true);
  repo.singleRows.remove("LOAN_TYPE");
  civilCheck.invoke(svc,55L,row());
  ok("no contribution does not require civil reference",true);
  System.out.println("PDL_R30_HF7A3_JAVA_PASS="+passed+" FAIL=0");
 }
}`)
 ];
 const app=path.join(root,base,'application');
 const main=['ProductBuilderValidationException.java','ProductRuleSemanticContract.java','LoanCivilPartnershipPolicy.java','ProductBuilderBusinessValidator.java','ProductRuleEvidenceEvaluator.java','ProductRuleHierarchyAudit.java','ProductRuleGovernanceService.java'].map(n=>path.join(app,n));
 main.push(path.join(root,base,'domain/ProductBuilderModels.java'));
 const bin=path.join(temp,'bin');fs.mkdirSync(bin);
 for(const [name,args] of [
  ['javac',['-encoding','UTF-8','-d',bin,...main,...stubs]],
  ['java',['-cp',bin,'Hf7A3Smoke']]
 ]){
   const outcome=spawnSync(name,args,{encoding:'utf8',maxBuffer:2*1024*1024});
   if(outcome.stdout)process.stdout.write(outcome.stdout);
   if(outcome.error||outcome.status!==0)throw Error(name+' A3 check failed: '+(outcome.stderr||outcome.error));
 }
 console.log('PDL_R30_HF7A3_STATIC_PASS='+pass+' FAIL=0');
} finally {fs.rmSync(temp,{recursive:true,force:true});}
