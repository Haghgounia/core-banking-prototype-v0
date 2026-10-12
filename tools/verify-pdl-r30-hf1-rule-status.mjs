/** R30-HF1: DPS rule-status reference mapping in Product Builder governance (read-only, no Oracle writes). */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const src='backend/src/main/java/com/behsazan/corebanking/productbuilder/application/';
const evidence=fs.readFileSync(path.join(root,src,'ProductRuleEvidenceEvaluator.java'),'utf8');
const reference=fs.readFileSync(path.join(root,src,'PdlReferenceOptionService.java'),'utf8');
const junit=fs.readFileSync(path.join(root,'backend/src/test/java/com/behsazan/corebanking/productbuilder/application/ProductRuleEvidenceR19Test.java'),'utf8');
const windows=fs.readFileSync(path.join(root,'build-production.cmd'),'utf8');
const unix=fs.readFileSync(path.join(root,'build-production.sh'),'utf8');
let pass=0,fail=0;
function check(name,condition){
 console.log(`${condition?'PASS':'FAIL'} | ${name}`);
 if(condition)pass++;else fail++;
}
check('DPS numeric reference status mapping is explicit', /"1",\s*"DRAFT",\s*"2",\s*"ACTIVE",\s*"3",\s*"INACTIVE",\s*"4",\s*"SUSPENDED",\s*"5",\s*"RETIRED"/.test(evidence));
check('Only numeric business codes verified against DPS are canonicalized', evidence.includes('DPS_RULE_STATUSES.getOrDefault(value, value)'));
check('Rule status column maps to DPS governed reference in form', reference.includes('Map.entry("RULE_STATUS_CODE", "dps-rule-statuses")'));
check('Active eligibility rule is evaluated after canonicalization', evidence.includes('String status = status(control, row);') && evidence.includes('dpsRuleStatus(text(row.get("RULE_STATUS_CODE")))'));
check('Deposit closure STATUS_CODE maps to same DPS reference', reference.includes('"DEPOSIT_PRODUCT_CLOSURE_RULE".equals(normalize(table))') && evidence.includes('"DEPOSIT_PRODUCT_CLOSURE_RULE".equals(control)'));
check('Generic STATUS_CODE not blindly mapped', evidence.includes('for (String column : List.of("STATUS_CODE", "RECORD_STATUS_CODE"))'));
check('Existing IS_ACTIVE precedence retained', evidence.indexOf('if (row.containsKey("IS_ACTIVE")')<evidence.indexOf('if (row.get("RULE_STATUS_CODE") != null)'));
check('Unrecognized status remains a blocking governance conflict', evidence.includes('unknown > 0') && evidence.includes('if (!INACTIVE_STATUSES.contains(status)) unknown++'));
check('Historical non-active statuses are never effective rules', evidence.includes('INACTIVE_STATUSES') && evidence.includes('if (!status.isBlank() && !USABLE_STATUSES.contains(status))'));
check('JUnit covers numeric active rule and unknown fail-closed', junit.includes('dpsNumericActiveEligibilityRuleCountsAsEffective') && junit.includes('unknownDpsNumericCodeRemainsBlocking'));
check('JUnit covers all nonactive numeric states and closure', junit.includes('dpsOtherNumericRuleStatusesCannotSatisfyRequiredRule') && junit.includes('numericDepositClosureRuleStatusUsesTheDpsReference'));
check('Hotfix verifier included in Windows build', windows.includes('verify-pdl-r30-hf1-rule-status.mjs'));
check('Hotfix verifier included in Linux build', unix.includes('verify-pdl-r30-hf1-rule-status.mjs'));

const temp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-r30-hf1-'));
try{
 const filename=path.join(temp,'R30HF1RuleStatusTest.java');
 fs.writeFileSync(filename,`import java.time.*;
import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
public final class R30HF1RuleStatusTest {
 static int passed=0;
 static final LocalDate FROM=LocalDate.of(2026,10,12), TO=LocalDate.of(2027,10,12);
 static ProductRuleEvidenceEvaluator.Evidence evaluate(String c,Map<String,Object> row){
   return ProductRuleEvidenceEvaluator.evaluate(c,List.of(row),FROM,TO);
 }
 static void ok(String title,boolean good){ if(!good)throw new AssertionError(title);passed++;System.out.println("PASS | "+title); }
 public static void main(String[] args) {
   var active=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("RULE_STATUS_CODE",2,"VALID_FROM","2026-10-12"));
   ok("Numeric 2 active eligibility has one effective record",active.effectiveCount()==1 && !active.conflict() && active.unrecognizedStatuses()==0);
   var activeString=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("RULE_STATUS_CODE","2"));
   ok("String 2 active eligibility is recognized",activeString.effectiveCount()==1 && !activeString.conflict());
   String[] statuses={"1","3","4","5"};
   String[] titles={"DRAFT","INACTIVE","SUSPENDED","RETIRED"};
   for(int i=0;i<statuses.length;i++) {
     var e=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("RULE_STATUS_CODE",statuses[i]));
     ok("DPS status "+statuses[i]+" "+titles[i]+" is non-effective but recognized",e.effectiveCount()==0 && !e.conflict() && e.unrecognizedStatuses()==0);
   }
   var unknown=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("RULE_STATUS_CODE","9"));
   ok("Unknown DPS 9 is fail-closed",unknown.effectiveCount()==0 && unknown.conflict() && unknown.unrecognizedStatuses()==1);
   var unknownText=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("RULE_STATUS_CODE","NEW_STATUS"));
   ok("Unknown text status is fail-closed",unknownText.conflict() && unknownText.unrecognizedStatuses()==1);
   var textActive=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("RULE_STATUS_CODE","ACTIVE"));
   ok("Legacy textual ACTIVE remains recognized",textActive.effectiveCount()==1 && !textActive.conflict());
   var textDraft=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("RULE_STATUS_CODE","DRAFT"));
   ok("Legacy textual DRAFT remains non-effective",textDraft.effectiveCount()==0 && !textDraft.conflict());
   var closure=evaluate("DEPOSIT_PRODUCT_CLOSURE_RULE",Map.of("STATUS_CODE","2","CLOSURE_TYPE_CODE","NORMAL"));
   ok("Deposit closure numeric 2 counts as active",closure.effectiveCount()==1 && !closure.conflict());
   var closureStopped=evaluate("DEPOSIT_PRODUCT_CLOSURE_RULE",Map.of("STATUS_CODE","4","CLOSURE_TYPE_CODE","NORMAL"));
   ok("Deposit closure numeric 4 does not count",closureStopped.effectiveCount()==0 && !closureStopped.conflict());
   var other=evaluate("PRODUCT_CHANNEL_RULE",Map.of("STATUS_CODE","2","CHANNEL_CODE","BRANCH"));
   ok("Unrelated STATUS_CODE numeric 2 is NOT presumed active",other.effectiveCount()==0 && other.conflict());
   var isActiveZero=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("IS_ACTIVE",0,"RULE_STATUS_CODE","2"));
   ok("IS_ACTIVE zero takes precedence over DPS status",isActiveZero.effectiveCount()==0 && !isActiveZero.conflict());
   var isActiveOne=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("IS_ACTIVE",1,"RULE_STATUS_CODE","3"));
   ok("Legacy IS_ACTIVE one precedence preserved",isActiveOne.effectiveCount()==1 && !isActiveOne.conflict());
   var outOfRange=evaluate("PRODUCT_ELIGIBILITY_RULE",Map.of("RULE_STATUS_CODE","2","VALID_FROM","2027-11-01"));
   ok("Active but future-dated rule does not count",outOfRange.effectiveCount()==0 && !outOfRange.conflict());
   System.out.println("PDL_R30_HF1_JAVA_PASS="+passed+" FAIL=0");
 }
}`, 'utf8');
 function command(cmd,args){
  const result=spawnSync(cmd,args,{encoding:'utf8',maxBuffer:1024*1024*5});
  if(result.error)throw result.error;
  if(result.status!==0)throw new Error(`${cmd} failed (exit ${result.status}): ${result.stderr||result.stdout}`);
  if(result.stdout)process.stdout.write(result.stdout);
 }
 command('javac',['-encoding','UTF-8','-d',temp,path.join(root,src,'ProductRuleEvidenceEvaluator.java'),path.join(root,src,'ProductBuilderValidationException.java'),filename]);
 command('java',['-cp',temp,'R30HF1RuleStatusTest']);
} finally {
 fs.rmSync(temp,{recursive:true,force:true});
}
console.log(`PDL_R30_HF1_STATIC_PASS=${pass} FAIL=${fail}`);
if(fail)process.exitCode=1;
