/** HF7-A3/HF4: run actual governance/evidence/validator/hierarchy Java with read-only repository stub. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const src='backend/src/main/java/com/behsazan/corebanking/productbuilder/';
let count=0;
function check(title,test){if(!test)throw Error('FAIL | '+title); console.log('PASS | '+title);count++;}
const gov=fs.readFileSync(path.join(root,src,'application/ProductRuleGovernanceService.java'),'utf8');
const audit=fs.readFileSync(path.join(root,src,'application/ProductRuleHierarchyAudit.java'),'utf8');
const contract=fs.readFileSync(path.join(root,src,'application/ProductRuleSemanticContract.java'),'utf8');
const win=fs.readFileSync(path.join(root,'build-production.cmd'),'utf8');
const unix=fs.readFileSync(path.join(root,'build-production.sh'),'utf8');
const label='verify-pdl-r30-hf7a3-hf4-hierarchy.mjs';
check('read-only child audit wired into governance status',gov.includes('childFinding.invalidSemantics()')&&gov.includes('childFinding.conflict()'));
check('child lookup is paginated and refuses partial evidence',gov.includes('loadChildEvidence(')&&gov.includes('rows.size() >= result.totalElements()'));
check('term children are checked for duplicate defaults and interval coverage',audit.includes('دو مدت پیش‌فرض فعال')&&audit.includes('temporalCoverage("DEPOSIT_PRODUCT_ALLOWED_TERM"'));
check('rate tier children are checked for duplicate tier numbers and interval coverage',audit.includes('شماره پله نرخ تکراری')&&audit.includes('temporalCoverage("PRODUCT_RATE_TIER"'));
check('parent pricing and term contracts are audited at approval',contract.includes('"PRODUCT_PRICING_RULE"')&&contract.includes('"DEPOSIT_PRODUCT_TERM_RULE"')&&contract.includes('case "DEPOSIT_PRODUCT_TERM_RULE" -> termRule(row)'));
check('no Oracle/SQL write in hierarchy audit',!/(insert|update|delete|merge|executeUpdate|jdbcClient|DataSource)\s*\(/i.test(audit));
check('deposit and loan operational gates remain fail-closed',gov.includes('if ("LOAN".equals(domain)) {\n            canOpen = false;')&&gov.includes('if ("DEPOSIT".equals(domain)'));
check('Windows production build runs HF4 verifier',win.includes(label));
check('Linux production build runs HF4 verifier',unix.includes(label));
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'hf7a3-hf4-'));
function put(rel,text){let p=path.join(tmp,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,text);return p;}
try{
 const sources=[
   put('org/springframework/stereotype/Component.java','package org.springframework.stereotype; public @interface Component {}'),
   put('org/springframework/stereotype/Service.java','package org.springframework.stereotype; public @interface Service {}'),
   put('org/springframework/beans/factory/annotation/Value.java','package org.springframework.beans.factory.annotation; public @interface Value {String value();}'),
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
}`)
 ];
 const fixture=fs.readFileSync(path.join(root,'backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceFixture.java'),'utf8')
  .replace('final class ProductRuleGovernanceFixture {','public final class ProductRuleGovernanceFixture {')
  .replace('    static Map<String, Object> row(String table) {','    public static Map<String, Object> row(String table) {');
 sources.push(put('com/behsazan/corebanking/productbuilder/application/ProductRuleGovernanceFixture.java',fixture));
 sources.push(put('com/behsazan/corebanking/productbuilder/oracle/PdlProductBuilderRepository.java',String.raw`
package com.behsazan.corebanking.productbuilder.oracle;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import com.behsazan.corebanking.productbuilder.application.ProductRuleGovernanceFixture;
import java.util.*;
public class PdlProductBuilderRepository {
 public String domain="DEPOSIT",family="LONG_TERM_DEPOSIT";
 public boolean brokenTerm=false,termGap=false,brokenPricing=false,tiered=false,missingTerm=false;
 public Optional<Map<String,Object>> findById(String table,long id){
  if(table.equals("PRODUCT_VERSION"))return Optional.of(Map.of("PRODUCT_ID",9L,"VALID_FROM","2026-10-10","VALID_TO","2026-10-30","VERSION_STATUS_CODE","DRAFT"));
  if(table.equals("PRODUCT"))return Optional.of(Map.of("PRODUCT_CLASS_CODE",domain,"PRODUCT_FAMILY_CODE",family,"PRODUCT_STATUS_CODE","DRAFT"));
  return Optional.empty();
 }
 public TablePage search(String table,String text,int page,int size,String field,String value){
  if(page>0||table.equals("PRODUCT_VERSION_MODULE"))return new TablePage(List.of(),0,page,size);
  if(missingTerm&&table.equals("DEPOSIT_PRODUCT_ALLOWED_TERM"))return new TablePage(List.of(),0,page,size);
  Map<String,Object> row=new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table));
  if(table.equals("DEPOSIT_PRODUCT_ALLOWED_TERM")){
   if(brokenTerm) row.put("TERM_VALUE","1.5");
   if(termGap) row.put("EFFECTIVE_TO_DATE","2026-10-20");
  }
  if(table.equals("PRODUCT_PRICING_RULE") && tiered)row.put("IS_TIERED_RATE",1);
  if(table.equals("PRODUCT_RATE_TIER")&&brokenPricing)row.put("RATE_VALUE","1.4");
  return new TablePage(List.of(row),1,page,size);
 }
}`));
 sources.push(put('Hf4Smoke.java',String.raw`
import java.util.*;import java.time.*;
import com.behsazan.corebanking.productbuilder.application.*;
import com.behsazan.corebanking.productbuilder.oracle.PdlProductBuilderRepository;
public class Hf4Smoke {
 static int pass=0;
 static final LocalDate start=LocalDate.parse("2026-10-10"),end=LocalDate.parse("2026-10-30");
 static final ProductBuilderBusinessValidator validator=new ProductBuilderBusinessValidator();
 static void check(String label,boolean yes){if(!yes)throw new AssertionError(label);pass++;System.out.println("PASS | Java: "+label);}
 static Map<String,Object> m(String table){return new LinkedHashMap<>(ProductRuleGovernanceFixture.row(table));}
 static ProductRuleHierarchyAudit.Finding term(List<Map<String,Object>> children){return ProductRuleHierarchyAudit.allowedTerms(List.of(m("DEPOSIT_PRODUCT_TERM_RULE")),start,end,(t,k,id)->children,validator);}
 static ProductRuleHierarchyAudit.Finding price(List<Map<String,Object>> tiers){var p=m("PRODUCT_PRICING_RULE");p.put("IS_TIERED_RATE",1);
  return ProductRuleHierarchyAudit.pricingTiers(List.of(p),start,end,(t,k,id)->t.equals("PRODUCT_PRICING_COMPONENT")?List.of(m(t)):tiers,validator);}
 static ProductRuleGovernanceService.GovernanceReport report(PdlProductBuilderRepository repo){
  return new ProductRuleGovernanceService(repo,new RuleGovernanceLiteService(),validator).report(55);}
 public static void main(String[] args){
  check("one term covers full version",term(List.of(m("DEPOSIT_PRODUCT_ALLOWED_TERM"))).valid());
  check("no term child blocks",term(List.of()).conflict());
  var bad=m("DEPOSIT_PRODUCT_ALLOWED_TERM");bad.put("IS_ACTIVE",0);
  check("inactive term cannot supply coverage",term(List.of(bad)).conflict());
  bad=m("DEPOSIT_PRODUCT_ALLOWED_TERM");bad.put("EFFECTIVE_TO_DATE","2026-10-20");
  check("term gap identifies first missed day",term(List.of(bad)).reason().contains("2026-10-21"));
  var first=m("DEPOSIT_PRODUCT_ALLOWED_TERM");first.put("EFFECTIVE_TO_DATE","2026-10-20");
  var next=m("DEPOSIT_PRODUCT_ALLOWED_TERM");next.put("EFFECTIVE_FROM_DATE","2026-10-21");next.put("TERM_VALUE",12);next.put("IS_DEFAULT",0);
  check("adjacent allowed term periods valid",term(List.of(first,next)).valid());
  next=m("DEPOSIT_PRODUCT_ALLOWED_TERM");next.put("TERM_VALUE",12);
  check("simultaneously active default terms conflict",term(List.of(m("DEPOSIT_PRODUCT_ALLOWED_TERM"),next)).reason().contains("پیش‌فرض"));
  next.put("IS_DEFAULT",0);next.put("TERM_VALUE",6);
  check("duplicate term value and unit conflict",term(List.of(m("DEPOSIT_PRODUCT_ALLOWED_TERM"),next)).reason().contains("تکراری"));
  next=m("DEPOSIT_PRODUCT_ALLOWED_TERM");next.put("TERM_VALUE","1.5");next.put("EFFECTIVE_FROM_DATE","2026-10-25");next.put("IS_DEFAULT",0);
  check("invalid future term blocked",term(List.of(m("DEPOSIT_PRODUCT_ALLOWED_TERM"),next)).invalidSemantics());
  next=m("DEPOSIT_PRODUCT_ALLOWED_TERM");next.put("TERM_RULE_ID",999);
  check("child parent mismatch blocked",term(List.of(next)).invalidSemantics());
  next=m("DEPOSIT_PRODUCT_ALLOWED_TERM");next.put("EFFECTIVE_FROM_DATE","2026-10-25");next.put("EFFECTIVE_TO_DATE","2026-10-20");
  check("invalid child dates blocked",term(List.of(next)).invalidSemantics());
  next=m("DEPOSIT_PRODUCT_ALLOWED_TERM");next.put("EFFECTIVE_FROM_DATE","bad-date");
  check("unparseable child dates fail closed",term(List.of(next)).invalidSemantics());
  check("term database lookup failure blocks",ProductRuleHierarchyAudit.allowedTerms(List.of(m("DEPOSIT_PRODUCT_TERM_RULE")),start,end,(t,k,id)->{throw new IllegalStateException();},validator).conflict());
  check("full period valid tiered rule",price(List.of(m("PRODUCT_RATE_TIER"))).valid());
  check("missing tier in tiered rule blocks",price(List.of()).conflict());
  bad=m("PRODUCT_RATE_TIER");bad.put("VALID_TO","2026-10-20");
  check("tier gap identifies first missed day",price(List.of(bad)).reason().contains("2026-10-21"));
  bad=m("PRODUCT_RATE_TIER");bad.put("RATE_VALUE","1.4");
  check("invalid tier rate blocks",price(List.of(bad)).invalidSemantics());
  bad=m("PRODUCT_RATE_TIER");bad.put("PRICING_COMPONENT_ID",999);
  check("wrong rate-tier parent blocks",price(List.of(bad)).invalidSemantics());
  first=m("PRODUCT_RATE_TIER");first.put("VALID_TO","2026-10-20");
  next=m("PRODUCT_RATE_TIER");next.put("VALID_FROM","2026-10-21");
  check("nonoverlapping same tier numbers allowed",price(List.of(first,next)).valid());
  next.put("VALID_FROM","2026-10-20");
  check("overlapping duplicate tier numbers denied",price(List.of(first,next)).conflict());
  var p=m("PRODUCT_PRICING_RULE");p.put("IS_TIERED_RATE",0);
  check("flat pricing needs no tiers",ProductRuleHierarchyAudit.pricingTiers(List.of(p),start,end,(t,k,id)->{throw new AssertionError("unexpected lookup");},validator).valid());
  p=m("PRODUCT_PRICING_RULE");p.put("IS_TIERED_RATE",1);p.put("VALID_FROM","2026-10-20");
  bad=m("PRODUCT_RATE_TIER");bad.put("VALID_FROM","2026-10-20");
  final var child=bad;
  check("tier parent scope starts at pricing rule validity",ProductRuleHierarchyAudit.pricingTiers(List.of(p),start,end,(t,k,id)->t.equals("PRODUCT_PRICING_COMPONENT")?List.of(m(t)):List.of(child),validator).valid());
  var repo=new PdlProductBuilderRepository();var result=report(repo);
  check("valid deposit design canApprove",result.canApprove());
  check("runtime canOpen still blocked",!result.canOpen());
  repo.brokenTerm=true;result=report(repo);
  check("report marks invalid term child semantics",!result.canApprove()&&result.effectiveRules().stream().anyMatch(r->r.code().equals("DEPOSIT_PRODUCT_TERM_RULE")&&r.status().equals("INVALID_SEMANTICS")));
  repo.brokenTerm=false;repo.termGap=true;result=report(repo);
  check("report blocks child term gap",!result.canApprove()&&result.effectiveRules().stream().anyMatch(r->r.code().equals("DEPOSIT_PRODUCT_TERM_RULE")&&r.status().equals("CONFLICT")&&r.reason().contains("2026-10-21")));
  repo.termGap=false;repo.missingTerm=true;result=report(repo);
  check("report blocks absent allowed terms",!result.canApprove());
  repo.missingTerm=false;repo.tiered=true;result=report(repo);
  check("valid tiered pricing canApprove",result.canApprove());
  repo.brokenPricing=true;result=report(repo);
  check("report marks invalid tier rate",!result.canApprove()&&result.effectiveRules().stream().anyMatch(r->r.code().equals("PRODUCT_PRICING_RULE")&&r.status().equals("INVALID_SEMANTICS")));
  System.out.println("PDL_R30_HF7A3_HF4_JAVA_PASS="+pass+" FAIL=0");
 }
}`));
 const app=path.join(root,src,'application');
 const actual=['ProductBuilderValidationException.java','ProductBuilderBusinessValidator.java','ProductRuleSemanticContract.java','ProductRuleEvidenceEvaluator.java','ProductRuleHierarchyAudit.java','ProductRuleGovernanceService.java']
  .map(s=>path.join(app,s));
 actual.push(path.join(root,src,'domain/ProductBuilderModels.java'));
 const bin=path.join(tmp,'classes');fs.mkdirSync(bin);
 for(const [cmd,args] of [
  ['javac',['-encoding','UTF-8','-d',bin,...actual,...sources]],
  ['java',['-cp',bin,'Hf4Smoke']]
 ]){
  const out=spawnSync(cmd,args,{encoding:'utf8',timeout:90000,maxBuffer:3*1024*1024});
  if(out.stdout)process.stdout.write(out.stdout);
  if(out.status!==0||out.error)throw Error(cmd+' failed: '+(out.stderr||out.error));
 }
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
console.log('PDL_R30_HF7A3_HF4_STATIC_PASS='+count+' FAIL=0');
