/** R30-HF4: read-only regression for deposit dormancy and closure UI/API contracts. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const get = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const app = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/';
const domain = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/domain/';
const ui = 'frontend/src/app/features/product-builder/';
const reference = get('docs/reference/Unified_Product_Builder_Interactive_Forms_FA.html');
const pdlModelString = reference.match(/const MODEL=(\[[^\n]+\]);/)?.[1];
if (!pdlModelString) throw new Error('Canonical PDL HTML model is missing');
const pdlModel = JSON.parse(pdlModelString);
const vocabulary = [
  ['DEPOSIT_PRODUCT_CLOSURE_RULE', 'CLOSURE_TYPE_CODE', 4],
  ['DEPOSIT_PRODUCT_CLOSURE_RULE', 'BALANCE_DESTINATION_CODE', 4],
  ['DEPOSIT_PRODUCT_CLOSURE_RULE', 'STATUS_CODE', 3],
  ['DEPOSIT_PRODUCT_CLOSURE_PRECHECK', 'CHECK_TYPE_CODE', 4],
  ['DEPOSIT_PRODUCT_CLOSURE_PRECHECK', 'FAILURE_ACTION_CODE', 3],
  ['DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE', 'SETTLEMENT_COMPONENT_CODE', 5],
  ['DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE', 'SETTLEMENT_METHOD_CODE', 4],
  ['DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE', 'DESTINATION_CODE', 4],
  ['DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE', 'APPROVAL_LEVEL_CODE', 4]
];
let staticOk = 0, staticFail = 0;
function assert(description, yes) {
  const accepted = Boolean(yes);
  console.log(`${accepted ? 'PASS' : 'FAIL'} | ${description}`);
  if (accepted) staticOk++; else staticFail++;
}
const policy = get(app + 'DepositClosureReferenceContract.java');
const service = get(app + 'PdlReferenceOptionService.java');
const defaults = get(app + 'DepositRuleDefaults.java');
const create = get(app + 'ProductBuilderService.java');
const validator = get(app + 'ProductBuilderBusinessValidator.java');
const formTs = get(ui + 'pdl-table.component.ts');
const formHtml = get(ui + 'pdl-table.component.html');
const review = get(ui + 'product-workspace.component.html');
const buildCmd = get('build-production.cmd');
const buildSh = get('build-production.sh');
assert('Canonical deposit HTML form model loads', pdlModel.length > 0);
for (const [table, field, expectedCount] of vocabulary) {
  const canonical = pdlModel.find(o => o.name === table)?.columns.find(c => c.name === field)?.options;
  assert(`${table}.${field}: original HTML defines ${expectedCount} approved business options`,
      Array.isArray(canonical) && canonical.length === expectedCount && canonical.every(o => o.length === 2));
  assert(`${table}.${field}: backend explicitly declares controlled vocabulary`,
      policy.includes('"' + table + '.' + field + '"'));
}
assert('Canonical dormancy flag has OFF default', pdlModel.find(o => o.name === 'DEPOSIT_PRODUCT_DORMANCY_RULE')?.columns.find(c => c.name === 'AUTO_REACTIVATION_ALLOWED')?.default === '0');
assert('Reference service intercepts exact closure table+field before generic DPS mapping',
  service.indexOf('DepositClosureReferenceContract.supports(table, name)') > 0 &&
  service.indexOf('DepositClosureReferenceContract.supports(table, name)') < service.indexOf('boolean depositField =') &&
  service.includes('DepositClosureReferenceContract.closureFormOptions(table, name, column.options()'));
assert('PDL CHECKs constrain closure options', policy.includes('if (oracleChecks == null || oracleChecks.isEmpty()) return declared') && policy.includes('for (SelectOption check : oracleChecks)'));
assert('Unmapped numeric surrogate values are not invented', !policy.includes('Integer.parseInt(') && policy.includes('candidates.get(code)'));
assert('Backend validates user selections against actual options', service.includes('validateChangedValues') && service.includes('column.referenceControlled()'));
assert('DPS status reference only used for actual PDL CHECK-backed status', service.includes('"dps-rule-statuses"'));
assert('New dormancy rows default to OFF in Angular before interaction', formTs.includes("column.name === 'AUTO_REACTIVATION_ALLOWED') value = 0"));
assert('Existing NULL dormancy switch also becomes explicit OFF in form', formTs.includes('this.isDormancyAutoField(column) || this.isDepositClosureSwitch(column)'));
assert('Server create fills dormancy OFF before validators and insert', create.includes('DepositRuleDefaults.apply(normalizeTable(table), prepared)') &&
  create.indexOf('DepositRuleDefaults.apply(normalizeTable(table), prepared)') < create.indexOf('referenceOptionService.validateChangedValues(descriptor(table), prepared, null)'));
assert('Backend defaults apply only on create, not generic update', create.match(/DepositRuleDefaults\.apply\(/g)?.length === 1);
assert('Backend default never overwrites explicit 0', defaults.includes('if (supplied == null || supplied instanceof String text && text.isBlank())'));
assert('Closure fields render as boolean switches rather than raw numbers', formHtml.includes('@else if (isDepositClosureSwitch(column))') && formHtml.includes('depositClosureSwitchHint(column)'));
assert('Closure business fields have clarifying Persian descriptions', formTs.includes('DEPOSIT_PRODUCT_CLOSURE_PRECHECK.CHECK_TYPE_CODE') && formTs.includes('DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE.SETTLEMENT_COMPONENT_CODE'));
assert('Closure precheck form describes check / failure action semantics', formHtml.includes('کنترل‌های پیش از خاتمه سپرده') && formTs.includes('اقدام در صورت عدم موفقیت کنترل'));
assert('Settlement subform is linked to actual closure parent', formTs.includes("filterColumn: 'CLOSURE_RULE_ID'"));
assert('Closure validation is server-side for every relevant rule', ['DEPOSIT_PRODUCT_DORMANCY_RULE',
  'DEPOSIT_PRODUCT_CLOSURE_RULE', 'DEPOSIT_PRODUCT_CLOSURE_PRECHECK',
  'DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE', 'DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE']
  .every(table => validator.includes('case "' + table + '" ->')));
assert('Server prevents partial closure unless closure itself is allowed', validator.includes('bool(values, "IS_PARTIAL_CLOSURE_ALLOWED") && !bool(values, "IS_CLOSURE_ALLOWED")'));
assert('Settlement transfer and pay require destination', validator.includes('\"TRANSFER\".equals(method)') && validator.includes('\"PAY\".equals(method)') && validator.includes('destination.isBlank()'));
assert('Issuance review no longer misleadingly says code was previously issued', !review.includes('قبلاً صادر شده و ثابت است') && review.includes('کد سیستمی ثبت‌شده محصول'));
assert('No new HF4 Oracle migration is supplied', !fs.existsSync(path.join(root, 'database/oracle/pdl/migrations/0.11.0-r30-hf4-deposit-closure.sql')));
assert('Windows production build includes HF4 regression', buildCmd.includes('verify-pdl-r30-hf4-deposit-closure.mjs'));
assert('Linux production build includes HF4 regression', buildSh.includes('verify-pdl-r30-hf4-deposit-closure.mjs'));
let compiler;
for (const manifest of [path.join(root, 'frontend/package.json'), '/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript/package.json']) {
  try { compiler = createRequire(manifest)('typescript'); break; } catch { /* no local TS installed */ }
}
if (compiler) {
  const file = compiler.createSourceFile('pdl-table.component.ts', formTs, compiler.ScriptTarget.Latest, true);
  assert('PDL form TypeScript parses without error', file.parseDiagnostics.length === 0);
}
console.log(`PDL_R30_HF4_STATIC_PASS=${staticOk} FAIL=${staticFail}`);
if (staticFail > 0) process.exit(1);

const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'pdl-hf4-'));
try {
  const src = path.join(tmp, 'src');
  const pkg = path.join(src, 'com/behsazan/corebanking/productbuilder/application');
  const annotationDir = path.join(src, 'org/springframework/stereotype');
  const classes = path.join(tmp, 'classes');
  fs.mkdirSync(annotationDir, {recursive: true});
  fs.mkdirSync(pkg, {recursive: true});
  fs.mkdirSync(classes, {recursive: true});
  const stub = path.join(annotationDir, 'Component.java');
  fs.writeFileSync(stub, 'package org.springframework.stereotype; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface Component {}');
  const java = path.join(pkg, 'DepositClosureHF4ContractTest.java');
  fs.writeFileSync(java, `package com.behsazan.corebanking.productbuilder.application;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import java.util.*;
public class DepositClosureHF4ContractTest {
  private static int passed = 0, failed = 0;
  private static final ProductBuilderBusinessValidator CHECK = new ProductBuilderBusinessValidator();
  static Map<String,Object> row(Object... data) {
    Map<String,Object> result = new LinkedHashMap<>();
    for (int i=0; i<data.length; i+=2) result.put((String)data[i], data[i+1]);
    return result;
  }
  static void ok(String name, boolean yes) {
    System.out.println((yes?"PASS":"FAIL")+" | JAVA: "+name);
    if (yes) passed++; else failed++;
  }
  static void valid(String name, String table, Map<String,Object> data) {
    boolean yes; try { CHECK.validate(table, data); yes=true; } catch (RuntimeException err) { yes=false; }
    ok(name, yes);
  }
  static void invalid(String name, String table, Map<String,Object> data) {
    boolean yes; try { CHECK.validate(table, data); yes=false; } catch (ProductBuilderValidationException err) { yes=true; }
    ok(name, yes);
  }
  static List<SelectOption> opts(String table, String field) {
    return DepositClosureReferenceContract.closureFormOptions(table,field,List.of(),List.of());
  }
  static void catalog() {
    String[][] names = {
      {"DEPOSIT_PRODUCT_CLOSURE_RULE","CLOSURE_TYPE_CODE"},
      {"DEPOSIT_PRODUCT_CLOSURE_RULE","BALANCE_DESTINATION_CODE"},
      {"DEPOSIT_PRODUCT_CLOSURE_RULE","STATUS_CODE"},
      {"DEPOSIT_PRODUCT_CLOSURE_PRECHECK","CHECK_TYPE_CODE"},
      {"DEPOSIT_PRODUCT_CLOSURE_PRECHECK","FAILURE_ACTION_CODE"},
      {"DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE","SETTLEMENT_COMPONENT_CODE"},
      {"DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE","SETTLEMENT_METHOD_CODE"},
      {"DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE","DESTINATION_CODE"},
      {"DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE","APPROVAL_LEVEL_CODE"}
    };
    for (String[] item: names) for (SelectOption opt: opts(item[0],item[1]))
      System.out.println("CATALOG\\t"+item[0]+"\\t"+item[1]+"\\t"+opt.code()+"\\t"+opt.label());
  }
  public static void main(String[] args) {
    catalog();
    ok("precheck check types have four choices",opts("DEPOSIT_PRODUCT_CLOSURE_PRECHECK","CHECK_TYPE_CODE").size()==4);
    ok("precheck failure actions have three choices",opts("DEPOSIT_PRODUCT_CLOSURE_PRECHECK","FAILURE_ACTION_CODE").size()==3);
    ok("closure status vocabulary has three choices",opts("DEPOSIT_PRODUCT_CLOSURE_RULE","STATUS_CODE").size()==3);
    ok("closure types have four choices",opts("DEPOSIT_PRODUCT_CLOSURE_RULE","CLOSURE_TYPE_CODE").size()==4);
    ok("settlement components have five choices",opts("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE","SETTLEMENT_COMPONENT_CODE").size()==5);
    ok("settlement methods have four choices",opts("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE","SETTLEMENT_METHOD_CODE").size()==4);
    ok("settlement destinations have four choices",opts("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE","DESTINATION_CODE").size()==4);
    ok("approval levels have four choices",opts("DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE","APPROVAL_LEVEL_CODE").size()==4);
    List<SelectOption> checks=List.of(new SelectOption("BLOCK","BLOCK",""),new SelectOption("UNKNOWN","UNKNOWN",""));
    List<SelectOption> limited=DepositClosureReferenceContract.closureFormOptions("DEPOSIT_PRODUCT_CLOSURE_PRECHECK","FAILURE_ACTION_CODE",checks,List.of());
    ok("Oracle CHECK limits PDL vocabulary",limited.size()==1&&"BLOCK".equals(limited.get(0).code()));
    List<SelectOption> numeric=DepositClosureReferenceContract.closureFormOptions("DEPOSIT_PRODUCT_CLOSURE_RULE","STATUS_CODE",List.of(new SelectOption("2","2","")),List.of(new SelectOption("2","2","فعال")));
    ok("Numeric CHECK works only with exactly matching documented DPS code",numeric.size()==1&&"2".equals(numeric.get(0).code()));
    ok("Unmapped numeric CHECK does not guess semantic meaning",DepositClosureReferenceContract.closureFormOptions("DEPOSIT_PRODUCT_CLOSURE_PRECHECK","CHECK_TYPE_CODE",List.of(new SelectOption("2","2","")),List.of()).isEmpty());
    ok("Unrecognized table and column are excluded",!DepositClosureReferenceContract.supports("DEPOSIT_PRODUCT_OPENING_RULE","CHECK_TYPE_CODE"));
    ok("Missing CHECK list does not substitute unrelated numeric DPS surrogate",opts("DEPOSIT_PRODUCT_CLOSURE_PRECHECK","CHECK_TYPE_CODE").stream().noneMatch(o->"2".equals(o.code())));
    Map<String,Object> dormancy=row("INACTIVITY_PERIOD_VALUE",365,"INACTIVITY_PERIOD_UNIT_CODE","DAY","REACTIVATION_METHOD_CODE","BRANCH");
    DepositRuleDefaults.apply("DEPOSIT_PRODUCT_DORMANCY_RULE",dormancy);
    ok("Dormancy create has initial OFF=0 without toggling",Integer.valueOf(0).equals(dormancy.get("AUTO_REACTIVATION_ALLOWED")));
    valid("Dormancy OFF=0 validates", "DEPOSIT_PRODUCT_DORMANCY_RULE",dormancy);
    Map<String,Object> explicit=row("AUTO_REACTIVATION_ALLOWED",1,"INACTIVITY_PERIOD_VALUE",30,"INACTIVITY_PERIOD_UNIT_CODE","DAY","REACTIVATION_METHOD_CODE","BRANCH");
    DepositRuleDefaults.apply("DEPOSIT_PRODUCT_DORMANCY_RULE",explicit);
    ok("Dormancy true remains true",Integer.valueOf(1).equals(explicit.get("AUTO_REACTIVATION_ALLOWED")));
    valid("Dormancy ON=1 validates", "DEPOSIT_PRODUCT_DORMANCY_RULE",explicit);
    Map<String,Object> warning=new LinkedHashMap<>(dormancy); warning.put("WARNING_PERIOD_VALUE",60); warning.put("WARNING_PERIOD_UNIT_CODE","DAY");
    valid("Dormancy warning period below inactivity validates", "DEPOSIT_PRODUCT_DORMANCY_RULE",warning);
    warning.put("WARNING_PERIOD_VALUE",390);
    invalid("Dormancy warning exceeding inactivity denied", "DEPOSIT_PRODUCT_DORMANCY_RULE",warning);
    warning.remove("WARNING_PERIOD_UNIT_CODE");
    invalid("Dormancy incomplete warning pair denied", "DEPOSIT_PRODUCT_DORMANCY_RULE",warning);
    Map<String,Object> badDorm=new LinkedHashMap<>(dormancy); badDorm.put("INACTIVITY_PERIOD_VALUE",-1);
    invalid("Dormancy negative inactivity denied", "DEPOSIT_PRODUCT_DORMANCY_RULE",badDorm);
    badDorm.put("INACTIVITY_PERIOD_VALUE",365);badDorm.put("AUTO_REACTIVATION_ALLOWED",2);
    invalid("Dormancy invalid numeric switch denied", "DEPOSIT_PRODUCT_DORMANCY_RULE",badDorm);
    Map<String,Object> closure=row("CLOSURE_TYPE_CODE","NORMAL","EFFECTIVE_FROM_DATE","2026-10-13");
    DepositRuleDefaults.apply("DEPOSIT_PRODUCT_CLOSURE_RULE",closure);
    ok("Closure defaults ON=1 and PARTIAL OFF=0",Integer.valueOf(1).equals(closure.get("IS_CLOSURE_ALLOWED"))&&Integer.valueOf(0).equals(closure.get("IS_PARTIAL_CLOSURE_ALLOWED")));
    valid("Closure standard flags accepted", "DEPOSIT_PRODUCT_CLOSURE_RULE",closure);
    Map<String,Object> cp=new LinkedHashMap<>(closure); cp.put("IS_CLOSURE_ALLOWED",0);cp.put("IS_PARTIAL_CLOSURE_ALLOWED",1);
    invalid("Partial closure without closure denied", "DEPOSIT_PRODUCT_CLOSURE_RULE",cp);
    cp=new LinkedHashMap<>(closure);cp.put("MIN_HOLDING_PERIOD_DAYS",-3);
    invalid("Negative closure holding period denied", "DEPOSIT_PRODUCT_CLOSURE_RULE",cp);
    cp=new LinkedHashMap<>(closure);cp.put("EFFECTIVE_TO_DATE","2026-10-12");
    invalid("Closure invalid date interval denied", "DEPOSIT_PRODUCT_CLOSURE_RULE",cp);
    Map<String,Object> precheck=row("CHECK_TYPE_CODE","ACTIVE_HOLD","FAILURE_ACTION_CODE","BLOCK");
    DepositRuleDefaults.apply("DEPOSIT_PRODUCT_CLOSURE_PRECHECK",precheck);
    ok("Precheck defaults mandatory=1, sequence=1",Integer.valueOf(1).equals(precheck.get("IS_MANDATORY"))&&Integer.valueOf(1).equals(precheck.get("SEQUENCE_NO")));
    valid("Precheck contract accepts BLOCK", "DEPOSIT_PRODUCT_CLOSURE_PRECHECK",precheck);
    Map<String,Object> pc=new LinkedHashMap<>(precheck);pc.remove("FAILURE_ACTION_CODE");
    invalid("Precheck requires failure action", "DEPOSIT_PRODUCT_CLOSURE_PRECHECK",pc);
    pc=new LinkedHashMap<>(precheck);pc.put("SEQUENCE_NO",0);
    invalid("Precheck rejects zero sequence", "DEPOSIT_PRODUCT_CLOSURE_PRECHECK",pc);
    pc=new LinkedHashMap<>(precheck);pc.put("IS_MANDATORY",0);
    valid("Precheck explicitly optional accepted", "DEPOSIT_PRODUCT_CLOSURE_PRECHECK",pc);
    DepositRuleDefaults.apply("DEPOSIT_PRODUCT_CLOSURE_PRECHECK",pc);
    ok("Precheck preserve explicit mandatory false",Integer.valueOf(0).equals(pc.get("IS_MANDATORY")));
    Map<String,Object> settlement=row("SETTLEMENT_COMPONENT_CODE","PRINCIPAL","SETTLEMENT_METHOD_CODE","TRANSFER","DESTINATION_CODE","CUSTOMER_ACCOUNT");
    DepositRuleDefaults.apply("DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",settlement);
    valid("Settlement transfer to customer account accepted", "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",settlement);
    Map<String,Object> sm=new LinkedHashMap<>(settlement);sm.remove("DESTINATION_CODE");
    invalid("Transfer without destination rejected", "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",sm);
    sm=new LinkedHashMap<>(settlement);sm.put("SEQUENCE_NO",-1);
    invalid("Settlement negative sequence rejected", "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",sm);
    sm=new LinkedHashMap<>(settlement);sm.put("SETTLEMENT_METHOD_CODE","WAIVE");sm.remove("DESTINATION_CODE");
    valid("Waiver without destination allowed in rule definition", "DEPOSIT_PRODUCT_CLOSURE_SETTLEMENT_RULE",sm);
    Map<String,Object> approval=row("APPROVAL_LEVEL_CODE","BRANCH_MANAGER","MIN_AMOUNT",100,"MAX_AMOUNT",5000);
    DepositRuleDefaults.apply("DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE",approval);
    ok("Approval defaults mandatory and sequence one",Integer.valueOf(1).equals(approval.get("IS_MANDATORY"))&&Integer.valueOf(1).equals(approval.get("APPROVAL_SEQUENCE_NO")));
    valid("Approval amount range accepted", "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE",approval);
    Map<String,Object> ap=new LinkedHashMap<>(approval);ap.put("MAX_AMOUNT",50);
    invalid("Approval reversed amount limits denied", "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE",ap);
    ap=new LinkedHashMap<>(approval);ap.put("APPROVAL_SEQUENCE_NO",0);
    invalid("Approval zero sequence denied", "DEPOSIT_PRODUCT_CLOSURE_APPROVAL_RULE",ap);
    System.out.println("PDL_R30_HF4_JAVA_PASS="+passed+" FAIL="+failed);
    if(failed>0)System.exit(1);
  }
}
`,'utf8');
  const sourceFiles = [
    stub,
    path.join(root, domain, 'ProductBuilderModels.java'),
    path.join(root, app, 'ProductBuilderValidationException.java'),
    path.join(root, app, 'DepositClosureReferenceContract.java'),
    path.join(root, app, 'DepositRuleDefaults.java'),
    path.join(root, app, 'ProductRuleSemanticContract.java'),
    path.join(root, app, 'ProductBuilderBusinessValidator.java'),
    java
  ];
  function run(command, args) {
    const result = spawnSync(command, args, {encoding: 'utf8', timeout: 90000, maxBuffer: 1024 * 1024 * 8});
    if (result.error || result.status !== 0) {
      console.error(result.stdout ?? ''); console.error(result.stderr ?? '');
      throw new Error(`${command} failed (${result.status ?? result.error?.message})`);
    }
    return result.stdout;
  }
  const javac = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME,'bin', process.platform === 'win32' ? 'javac.exe' : 'javac') : 'javac';
  const javacOutput = run(javac, ['-encoding', 'UTF-8', '-d', classes, ...sourceFiles]);
  if (javacOutput.trim()) console.log(javacOutput.trim());
  const javaCmd = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME,'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
  const out = run(javaCmd, ['-Dfile.encoding=UTF-8', '-Dsun.stdout.encoding=UTF-8', '-cp', classes, 'com.behsazan.corebanking.productbuilder.application.DepositClosureHF4ContractTest']);
  const catalog = out.split(/\r?\n/).filter(line => line.startsWith('CATALOG\t'));
  for (const [table, field, expectedCount] of vocabulary) {
    const rows = catalog.filter(line => line.split('\t')[1] === table && line.split('\t')[2] === field);
    const html = pdlModel.find(o => o.name === table)?.columns.find(c => c.name === field)?.options ?? [];
    const javas = rows.map(line => line.split('\t').slice(3));
    assert(`Backend ${table}.${field} exactly matches approved HTML code+Persian-label pairs`,
      javas.length === expectedCount && JSON.stringify(javas) === JSON.stringify(html));
  }
  console.log(`PDL_R30_HF4_CONTRACT_PASS=${vocabulary.length} FAIL=${staticFail}`);
  console.log(out.split(/\r?\n/).filter(line => !line.startsWith('CATALOG\t')).join('\n').trim());
  if (staticFail) process.exitCode = 1;
} finally {
  fs.rmSync(tmp, {recursive: true, force: true});
}
