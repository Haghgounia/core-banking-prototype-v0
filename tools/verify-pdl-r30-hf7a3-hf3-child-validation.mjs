/** R30-HF7-A3/HF3. Compile and exercise the actual business validator using only a local Spring annotation stub.
 * No Oracle, SQL, HTTP, Maven download or project writes. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const app='backend/src/main/java/com/behsazan/corebanking/productbuilder/application';
const source=fs.readFileSync(path.join(root,app,'ProductRuleSemanticContract.java'),'utf8');
let failed=0;
function check(name,ok){console.log(`${ok?'PASS':'FAIL'} | ${name}`);if(!ok)failed++;}
check('deposit term validator exists',source.includes('case "DEPOSIT_PRODUCT_ALLOWED_TERM" -> allowedTerm(row)'));
check('rate tier validator exists',source.includes('case "PRODUCT_RATE_TIER" -> rateTier(row)'));
check('operational gates remain unchanged',!source.includes('canOpen = true'));
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-hf7a3-hf3-'));
function put(rel,data){const p=path.join(tmp,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,data);return p;}
function run(label,cmd,args){const p=spawnSync(cmd,args,{encoding:'utf8',timeout:90000});if(p.status!==0){failed++;console.log('FAIL | '+label+'\n'+p.stdout+'\n'+p.stderr);}else{console.log('PASS | '+label);if(p.stdout)console.log(p.stdout.trim());}}
try {
 const annotation=put('org/springframework/stereotype/Component.java','package org.springframework.stereotype; public @interface Component {}');
 const test=put('ChildSmoke.java',String.raw`
import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
public class ChildSmoke {
  static final ProductBuilderBusinessValidator v=new ProductBuilderBusinessValidator();
  static int pass=0;
  static Map<String,Object> term(){var m=new LinkedHashMap<String,Object>();m.put("TERM_VALUE",6);m.put("TERM_UNIT_CODE","MONTH");m.put("IS_DEFAULT",0);m.put("IS_ACTIVE",1);return m;}
  static Map<String,Object> tier(){var m=new LinkedHashMap<String,Object>();m.put("TIER_NO",1);m.put("MIN_AMOUNT",0);m.put("MAX_AMOUNT",999999);m.put("RATE_VALUE","0.18");m.put("MIN_TERM_VALUE",3);m.put("MAX_TERM_VALUE",12);m.put("TERM_UNIT_CODE","MONTH");return m;}
  static void good(String table,Map<String,Object> m){v.validate(table,m);pass++;}
  static void bad(String table,Map<String,Object> m,String label){try{v.validate(table,m);throw new AssertionError("Expected rejection: "+label);}catch(ProductBuilderValidationException ok){pass++;}}
  public static void main(String[] args){
    String term="DEPOSIT_PRODUCT_ALLOWED_TERM",tier="PRODUCT_RATE_TIER";
    good(term,term());
    var t=term();t.remove("TERM_UNIT_CODE");bad(term,t,"missing unit");
    t=term();t.put("TERM_UNIT_CODE","HOUR");bad(term,t,"unsupported unit");
    t=term();t.put("TERM_VALUE",0);bad(term,t,"zero term");
    t=term();t.put("TERM_VALUE","1.5");bad(term,t,"fractional term");
    t=term();t.put("TERM_VALUE",-3);bad(term,t,"negative term");
    t=term();t.put("IS_DEFAULT",2);bad(term,t,"invalid default");
    t=term();t.put("DISPLAY_ORDER","2.5");bad(term,t,"fractional display order");
    t=term();t.put("EFFECTIVE_FROM_DATE","2026-10-16");t.put("EFFECTIVE_TO_DATE","2026-10-15");bad(term,t,"invalid term dates");
    good(tier,tier());
    var r=tier();r.remove("RATE_VALUE");bad(tier,r,"missing rate");
    r=tier();r.put("RATE_VALUE","1.2");bad(tier,r,"rate >1");
    r=tier();r.put("RATE_VALUE",-0.01);bad(tier,r,"negative rate");
    r=tier();r.put("TERM_UNIT_CODE","HOUR");bad(tier,r,"unsupported tier unit");
    r=tier();r.put("MIN_TERM_VALUE","3.5");bad(tier,r,"fractional minimum tier term");
    r=tier();r.remove("TERM_UNIT_CODE");bad(tier,r,"missing tier unit");
    r=tier();r.put("MIN_TERM_VALUE",15);bad(tier,r,"tier term reversed");
    r=tier();r.put("MIN_AMOUNT",1000000);bad(tier,r,"tier amount reversed");
    r=tier();r.put("TIER_NO","2.5");bad(tier,r,"fractional tier number");
    r=tier();r.remove("MIN_TERM_VALUE");r.remove("MAX_TERM_VALUE");r.remove("TERM_UNIT_CODE");good(tier,r);
    r=tier();r.remove("MIN_TERM_VALUE");r.remove("MAX_TERM_VALUE");bad(tier,r,"unused tier unit");
    r=tier();r.put("RATE_VALUE",0);good(tier,r);
    System.out.println("HF7A3_HF3_CHILD_JAVA_PASS="+pass+" FAIL=0");
  }
}
`);
 const classes=path.join(tmp,'classes');fs.mkdirSync(classes);
 run('compile actual Java validators','javac',['-encoding','UTF-8','-d',classes,annotation,...['ProductBuilderValidationException.java','ProductRuleSemanticContract.java','ProductPricingSingleRatePolicy.java','ProductBuilderBusinessValidator.java','ProductRuleEvidenceEvaluator.java','ProductRuleHierarchyAudit.java'].map(x=>path.join(root,app,x)),test]);
 if(fs.existsSync(path.join(classes,'ChildSmoke.class')))run('run 22 positive/negative Java boundary assertions','java',['-cp',classes,'ChildSmoke']);
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
console.log('PDL_R30_HF7A3_HF3_PASS='+(!failed)+' FAIL='+failed);
if(failed)process.exitCode=1;
