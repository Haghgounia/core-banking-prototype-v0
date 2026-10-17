/** R30-HF7-A3/HF2: read-only form, Java business and Oracle CHECK parser smoke tests.
 * Compiles the real validators and real CHECK parser source with local stubs only.
 * No live database access, SQL execution, network calls or application writes.
 */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import vm from 'node:vm';
import {createRequire} from 'node:module';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const base='backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const repo=read(base+'oracle/PdlProductBuilderRepository.java');
const ref=read(base+'application/PdlReferenceOptionService.java');
const validator=read(base+'application/ProductBuilderBusinessValidator.java');
const contract=read(base+'application/ProductRuleSemanticContract.java');
const ui=read('frontend/src/app/features/product-builder/pdl-table.component.ts');
const html=read('frontend/src/app/features/product-builder/pdl-table.component.html');
const sourceFiles=[
 'frontend/src/app/layout/app-shell.component.html',
 'frontend/src/app/shared/ui/app-breadcrumb.component.ts',
 'frontend/src/app/features/four-deposits/deposit-opening-operations.component.html',
 'frontend/src/app/features/four-deposits/four-deposits-home.component.html',
 'frontend/src/app/features/reference-menu/reference-menu.component.ts',
 'frontend/src/app/features/reference-menu/reference-menu.component.html',
 'backend/src/main/java/com/behsazan/corebanking/system/modelcomparison/ConfiguredDatabaseSchemas.java'];
let passed=0,failed=0;
function check(label,condition){if(condition){passed++;console.log('PASS | '+label);}else{failed++;console.error('FAIL | '+label);}}
check('all current module labels use generic deposit wording',sourceFiles.every(p=>!read(p).includes('\u0686\u0647\u0627\u0631 \u0633\u067e\u0631\u062f\u0647')));
check('legacy four-deposit route is retained',read('frontend/src/app/shared/ui/app-breadcrumb.component.ts').includes('/four-deposits'));
check('quoted Oracle identifiers and OR equality are supported',repo.includes('EQUALITY_CLAUSE')&&repo.includes('parseCheckOptions(constraints)')&&repo.includes('LinkedHashSet<String>'));
check('joint PDL aliases persist only CHECK-permitted codes',ref.includes('JOINT_PDL_ALIASES')&&ref.includes('allowed.contains(alias.getKey())')&&ref.includes('allowed.contains(semantic)'));
check('individual and joint ownership cross-field checks remain in backend',contract.includes('INDIVIDUAL')&&contract.includes('ownersMin != 1')&&contract.includes('ownersMin < 2'));
check('settlement method has server-side cross-field guards',validator.includes('SETTLEMENT_METHOD_CODE')&&validator.includes('"DEDUCT", "WAIVE"')&&validator.includes('"TRANSFER".equals(method)')&&validator.includes('destination.isBlank()'));
check('individual joint inputs disabled and normalized on type change',ui.includes('syncJointOwnerFields(true)')&&ui.includes('field.setValue(1')&&ui.includes("distribution.setValue(null"));
check('settlement destination driven by method in UI and backend',ui.includes('syncSettlementDestination()')&&ui.includes('settlementAllowedOptions(column)')&&html.includes('!settlementDestinationRequired()'));
check('historic unrecognized option cannot validate destination',ui.includes('!this.settlementAllowedOptions(column).some'));
check('channel dates use product version without silently changing edit',ui.includes("this.service.row('PRODUCT_VERSION', versionId)")&&ui.includes('this.editingId() === null && !this.cloning()'));
check('channel temporal coverage guard remains active',read(base+'application/ProductRuleGovernanceService.java').includes('requiredForVersion && !temporal.complete()'));
check('channel version and explicit alignment visible',html.includes('channelVersionHint()')&&html.includes('alignChannelDatesToVersion()'));
check('no automatic Oracle data changes or safety policy override',!ui.includes('ALTER TABLE PDL')&&!validator.includes('canOpen = true'));
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'hf7a3-hf2-'));
function put(rel,content){const target=path.join(temp,rel);fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,content);return target;}
function run(label,command,args){const p=spawnSync(command,args,{encoding:'utf8',timeout:90000});if(p.status!==0){console.error('FAIL | '+label+'\n'+p.stdout+'\n'+p.stderr);failed++;}else{passed++;console.log('PASS | '+label);if(p.stdout)console.log(p.stdout.trim());}}
try {
 const annotation=put('org/springframework/stereotype/Component.java','package org.springframework.stereotype; public @interface Component {}');
 const javaFiles=[annotation, ...[
  'application/ProductBuilderValidationException.java',
  'application/ProductRuleSemanticContract.java',
  'application/ProductPricingSingleRatePolicy.java',
  'application/ProductRuleEvidenceEvaluator.java',
  'application/ProductRuleHierarchyAudit.java',
  'application/ProductBuilderBusinessValidator.java'
 ].map(p=>path.join(root,base,p))];
 const runtime=put('BusinessSmoke.java',`import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
public class BusinessSmoke {
 static int assertions=0;
 static void ok(String s,boolean b){if(!b)throw new AssertionError(s);assertions++;}
 static Map<String,Object> closure(String method,String dest){var r=new LinkedHashMap<String,Object>();r.put("SETTLEMENT_COMPONENT_CODE","COMMISSION");r.put("SETTLEMENT_METHOD_CODE",method);r.put("DESTINATION_CODE",dest);r.put("IS_MANDATORY",1);r.put("SEQUENCE_NO",1);return r;}
 static Map<String,Object> joint(String kind,int min,int max,int signers,String profit){var r=new LinkedHashMap<String,Object>();r.put("OWNERSHIP_TYPE_CODE",kind);r.put("SIGNING_RULE_CODE","ANY_TO_SIGN");r.put("MIN_JOINT_OWNER_COUNT",min);r.put("MAX_JOINT_OWNER_COUNT",max);r.put("MIN_REQUIRED_SIGNER_COUNT",signers);r.put("PROFIT_DISTRIBUTION_CODE",profit);return r;}
 static void bad(ProductBuilderBusinessValidator v,String table,Map<String,Object> data){try{v.validate(table,data);throw new AssertionError("expected rejection");}catch(ProductBuilderValidationException good){assertions++;}}
 public static void main(String[] unused){var v=new ProductBuilderBusinessValidator();var settlement="DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE";var own="DEPOSIT_PRODUCT_JOINT_RULE";
 for(String m:List.of("DEDUCT","WAIVE")){v.validate(settlement,closure(m,null));assertions++;bad(v,settlement,closure(m,"CUSTOMER_ACCOUNT"));}
 for(String m:List.of("PAY","TRANSFER"))bad(v,settlement,closure(m,null));
 v.validate(settlement,closure("PAY","CASH"));assertions++;
 v.validate(settlement,closure("TRANSFER","BANK_ACCOUNT"));assertions++;
 bad(v,settlement,closure("PAY","BANK_ACCOUNT"));bad(v,settlement,closure("TRANSFER","CASH"));bad(v,settlement,closure("UNKNOWN",null));
 for(String m:List.of("SINGLE","INDIVIDUAL","1")){v.validate(own,joint(m,1,1,1,null));assertions++;}
 bad(v,own,joint("SINGLE",1,2,1,null));bad(v,own,joint("SINGLE",1,1,1,"EQUAL"));bad(v,own,joint("JOINT",1,2,1,null));
 v.validate(own,joint("JOINT",2,4,2,"EQUAL"));assertions++;
 System.out.println("HF7A3_HF2_BUSINESS_JAVA_PASS="+assertions);
 }
}`);
 javaFiles.push(runtime);
 // Copy only the unchanged real repository parser declarations and method bodies
 // into a tiny isolated compilation unit; no Spring JDBC or Oracle is invoked.
 const bracketMethod=(src,signature)=>{const at=src.indexOf(signature);if(at<0)throw new Error('Missing Java method '+signature);const brace=src.indexOf('{',at);let depth=0, end=-1;for(let i=brace;i<src.length;i++){if(src[i]==='{')depth++;if(src[i]==='}'){depth--;if(depth===0){end=i+1;break;}}}if(end<0)throw Error('Unterminated '+signature);return src.slice(at,end);};
 const lines=repo.split('\n');
 const regex=lines.filter(x=>x.includes('private static final Pattern IN_CLAUSE')||x.includes('private static final Pattern EQUALITY_CLAUSE')).join('\n');
 const methods=[bracketMethod(repo,'static Map<String, List<SelectOption>> parseCheckOptions('),bracketMethod(repo,'private static SelectOption checkOption('),bracketMethod(repo,'private static List<String> splitValues(')].join('\n').replace('PdlProductBuilderRepository::checkOption','ParserSmoke::checkOption');
 const parser=put('ParserSmoke.java',`import java.util.*;import java.math.*;import java.util.regex.*;
public class ParserSmoke {
 public record SelectOption(Object value,String code,String label) {}
 ${regex}
 ${methods}
 static int assertions=0;
 static void ok(String msg,boolean pass){if(!pass)throw new AssertionError(msg);assertions++;}
 static List<String> codes(Map<String,List<SelectOption>> v,String key){return v.get(key).stream().map(SelectOption::code).toList();}
 public static void main(String[] a){
  char q=(char)34;
  var quoted=parseCheckOptions(List.of(q+"OWNERSHIP_TYPE_CODE"+q+" IN ('INDIVIDUAL','JOINT')"));
  ok("quoted CHECK",codes(quoted,"OWNERSHIP_TYPE_CODE").equals(List.of("INDIVIDUAL","JOINT")));
  var eql=parseCheckOptions(List.of("("+q+"OWNERSHIP_TYPE_CODE"+q+" = 'SINGLE' OR "+q+"OWNERSHIP_TYPE_CODE"+q+" = 'JOINT')"));
  ok("OR equality",codes(eql,"OWNERSHIP_TYPE_CODE").equals(List.of("SINGLE","JOINT")));
  var intersect=parseCheckOptions(List.of("OWNER IN ('SINGLE','JOINT')","OWNER IN ('JOINT')"));
  ok("intersect",codes(intersect,"OWNER").equals(List.of("JOINT")));
  var numeric=parseCheckOptions(List.of(q+"OWNERS"+q+" IN (1,2)",q+"NOTE"+q+" IN ('O''BRIEN','X')"));
  ok("numeric",codes(numeric,"OWNERS").equals(List.of("1","2")));
  ok("escaped SQL apostrophe",codes(numeric,"NOTE").equals(List.of("O'BRIEN","X")));
  System.out.println("HF7A3_HF2_ORACLE_PARSER_JAVA_PASS="+assertions);
 }
}`);
 const cp=path.join(temp,'classes');fs.mkdirSync(cp,{recursive:true});
 run('compile original business Java + isolated original Oracle parser','javac',['-encoding','UTF-8','-d',cp,...javaFiles,parser]);
 if(fs.existsSync(path.join(cp,'ParserSmoke.class'))){run('execute real Oracle CHECK parser','java',['-cp',cp,'ParserSmoke']);run('execute original cross-field validators','java',['-cp',cp,'BusinessSmoke']);}
 // Exercise exact Angular component methods without fetching any services.
 const require=createRequire(import.meta.url);let ts=null;
 const np=spawnSync('npm',['root','-g'],{encoding:'utf8',timeout:10000});
 for(const dir of [path.join(root,'frontend/node_modules/typescript'),...(np.status===0?[path.join(np.stdout.trim(),'typescript')]:[])]){
  try{ts=require(dir);break;}catch{}
 }
 if(ts){
  const transpiled=ts.transpileModule(ui,{fileName:'pdl-table.component.ts',reportDiagnostics:true,compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}});
  check('TypeScript syntax parse (whole component)',!transpiled.diagnostics?.length);
  const src=ts.createSourceFile('ui.ts',ui,ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
  const c=src.statements.find(x=>ts.isClassDeclaration(x)&&x.name?.text==='PdlTableComponent');
  if(!c)throw Error('Angular class missing');
  const names=['isIndividualOwnership','syncJointOwnerFields','settlementDestinationRequired','settlementAllowedOptions','syncSettlementDestination','channelHasPossibleCoverageGap','alignChannelDatesToVersion'];
  const extracted=names.map(n=>{const method=c.members.find(x=>ts.isMethodDeclaration(x)&&x.name.getText(src)===n);if(!method)throw Error('UI method missing '+n);return method.getText(src);}).join('\n');
  const transpile=ts.transpileModule('class TestEditor {\n'+extracted+'\n}\nmodule.exports=TestEditor;',
   {fileName:'ui-test.ts',compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
  const context={module:{exports:{}},exports:{},Validators:{required:()=>true}};vm.runInNewContext(transpile,context);const Editor=context.module.exports;
  const field=(value)=>({value,enabled:true,validators:[],setValue(v){this.value=v;},disable(){this.enabled=false;},enable(){this.enabled=true;},clearValidators(){this.validators=[];},setValidators(x){this.validators=x;},updateValueAndValidity(){}});
  {
   const f=new Editor();f.tableName=()=> 'DEPOSIT_PRODUCT_JOINT_RULE';f.form={controls:{OWNERSHIP_TYPE_CODE:field('INDIVIDUAL'),MIN_JOINT_OWNER_COUNT:field(2),MAX_JOINT_OWNER_COUNT:field(5),MIN_REQUIRED_SIGNER_COUNT:field(3),PROFIT_DISTRIBUTION_CODE:field('EQUAL')}};
   f.syncJointOwnerFields(true);
   check('individual account clears profit distribution',f.form.controls.PROFIT_DISTRIBUTION_CODE.value===null&&!f.form.controls.PROFIT_DISTRIBUTION_CODE.enabled);
   check('individual forces one owner and signer',['MIN_JOINT_OWNER_COUNT','MAX_JOINT_OWNER_COUNT','MIN_REQUIRED_SIGNER_COUNT'].every(k=>f.form.controls[k].value===1&&!f.form.controls[k].enabled));
   f.form.controls.OWNERSHIP_TYPE_CODE.setValue('JOINT');f.syncJointOwnerFields(true);
   check('joint re-enables owner counts and profit rule',f.form.controls.PROFIT_DISTRIBUTION_CODE.enabled&&f.form.controls.MIN_JOINT_OWNER_COUNT.enabled&&f.form.controls.MIN_JOINT_OWNER_COUNT.value===null);
  }
  {
   const dest={name:'DESTINATION_CODE'};const f=new Editor();f.tableName=()=> 'DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE';
   f.form={controls:{SETTLEMENT_METHOD_CODE:field('DEDUCT'),DESTINATION_CODE:field('CASH')}};
   f.descriptor=()=>({columns:[dest]});f.baseOptions=()=>[{code:'CASH',value:'CASH'},{code:'BANK_ACCOUNT',value:'BANK_ACCOUNT'},{code:'CUSTOMER_ACCOUNT',value:'CUSTOMER_ACCOUNT'}];
   f.syncSettlementDestination();check('deduction clears and disables destination',f.form.controls.DESTINATION_CODE.value===null&&!f.form.controls.DESTINATION_CODE.enabled);
   f.form.controls.SETTLEMENT_METHOD_CODE.setValue('PAY');f.syncSettlementDestination();
   check('pay enables and requires a destination',f.form.controls.DESTINATION_CODE.enabled&&f.form.controls.DESTINATION_CODE.validators.length===1);
   f.form.controls.DESTINATION_CODE.setValue('BANK_ACCOUNT');f.syncSettlementDestination();
   check('pay clears incompatible bank account',f.form.controls.DESTINATION_CODE.value===null);
   f.form.controls.SETTLEMENT_METHOD_CODE.setValue('TRANSFER');f.form.controls.DESTINATION_CODE.setValue('CASH');f.syncSettlementDestination();
   check('transfer clears incompatible cash destination',f.form.controls.DESTINATION_CODE.value===null);
   f.form.controls.SETTLEMENT_METHOD_CODE.setValue('WAIVE');f.form.controls.DESTINATION_CODE.setValue('CUSTOMER_ACCOUNT');f.syncSettlementDestination();
   check('waiver clears and disables destination',f.form.controls.DESTINATION_CODE.value===null&&!f.form.controls.DESTINATION_CODE.enabled);
  }
  {
   const f=new Editor();f.tableName=()=> 'PRODUCT_CHANNEL_RULE';let dates={versionId:5,validFrom:'2026-10-14',validTo:'2031-10-16'};
   f.channelVersionScope=()=>dates;f.form={controls:{VALID_FROM:field('2026-10-14'),VALID_TO:field('2031-10-14')}};
   check('user-entered short period warns about possible gap',f.channelHasPossibleCoverageGap());
   f.alignChannelDatesToVersion();check('explicit alignment covers full version interval',!f.channelHasPossibleCoverageGap()&&f.form.controls.VALID_TO.value==='2031-10-16');
  }
 }else console.log('SKIP | dynamic Angular fixture: TypeScript compiler not installed');
}finally{fs.rmSync(temp,{recursive:true,force:true});}
console.log('PDL_R30_HF7A3_HF2_PASS='+passed+' FAIL='+failed);
if(failed)process.exitCode=1;
