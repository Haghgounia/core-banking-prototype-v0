/** HF5: read-only source / policy verification. Oracle is NEVER contacted. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const get = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const javaApp = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/';
const javaRepo = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/oracle/';
const migrationName = 'database/oracle/pdl/migrations/0.11.0-r30-hf5-numeric-deposit-type-product-code.sql';
const policy = get(javaApp + 'DepositProductCodePolicy.java');
const service = get(javaApp + 'ProductBuilderService.java');
const repo = get(javaRepo + 'PdlProductBuilderRepository.java');
const migration = get(migrationName);
const ui = get('frontend/src/app/features/product-builder/product-workspace.component.html');
const cmd = get('build-production.cmd');
const sh = get('build-production.sh');
const typePairs = [
  ['CURRENT_NO_INTEREST','2510'],
  ['SAVINGS','3510'],
  ['SHORT_TERM_DEPOSIT','4510'],
  ['LONG_TERM_DEPOSIT','5510'],
  ['CERTIFICATE_OF_DEPOSIT','6510'],
];
let pass = 0, fail = 0;
function check(name, yes) {
  const ok = !!yes;
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}`);
  if (ok) pass++; else fail++;
}
for(const [type,code] of typePairs) {
  check(`${type} numeric policy ${code}`,policy.includes(`"${type}", "${code}"`));
  check(`${type} Oracle mapping ${code}`, migration.includes(`DEPOSIT_TYPE_CODE = '${type}' AND ISSUANCE_TYPE_CODE = '${code}'`));
}
check('Exactly five approved type prefixes', (policy.match(/"[A-Z_]+", "[0-9]{4}"/g)||[]).length===5);
check('No issuance through surrogate DEPOSIT_TYPE_ID',!service.includes('allocateDepositProductCode(depositTypeId') && !repo.includes('WHERE DEPOSIT_TYPE_ID = :'));
check('DEPOSIT_TYPE_CODE is read from persisted version profile',service.includes('String depositTypeCode = text(profile.get("DEPOSIT_TYPE_CODE"))'));
check('Profile type must be coherent with family identity',service.includes('String expectedDepositType = depositIdentity(versionId)[1]') && service.includes('!expectedDepositType.equals(depositTypeCode)'));
check('No numeric family prefix in finalization',!service.includes('allocateDepositProductCode(family);') && service.includes('allocateDepositProductCode(family, depositTypeCode)'));
check('Unsupported correspondent prefix not silently invented',policy.includes('isCorrespondent') && service.includes('numericTypeCode(family)'));
check('Code is four-digit type + four-digit bank year + three-digit serial',policy.includes('return prefix + String.format(Locale.ROOT, "%04d%03d", workingYear, serial);'));
check('Oracle working-year row locked as single issuance clock',repo.includes('int year = activeLoanProductWorkingYear(); // SELECT FOR UPDATE') && repo.includes('SETTING_ID = 1 FOR UPDATE'));
check('New registry is scoped by numeric type and Oracle working year',repo.includes('PRODUCT_DEPOSIT_TYPE_CODE_REGISTRY WHERE ISSUANCE_TYPE_CODE = :typeCode') && repo.includes('AND WORK_YEAR = :workYear'));
check('Serial increments within one type/year from 001 to 999',repo.includes('NVL(MAX(SERIAL_NO), 0)') && repo.includes('for (int serial = maximum + 1; serial <= 999; serial++)'));
check('Full PRODUCT catalog collision check before issuance',repo.includes('WHERE PRODUCT_CODE = :code') && repo.includes('if (used == 0) return new GeneratedDepositCode('));
check('Atomic registry stores type, family, prefix and product code snapshot',repo.includes('INSERT INTO " + schemaName + ".PRODUCT_DEPOSIT_TYPE_CODE_REGISTRY') && repo.includes('PRODUCT_FAMILY_CODE, DEPOSIT_TYPE_CODE, ISSUANCE_TYPE_CODE') && repo.includes('.param("prefix", code.numericTypeCode())'));
check('Old and new registries both enforce family immutability',repo.includes('UNION ALL SELECT PRODUCT_FAMILY_CODE') && service.includes('repository.registeredDepositFamily(id).isPresent()'));
check('Early idempotent return for any existing issued code',service.includes('if (!existingCode.isBlank())\n            return Map.of("productId", productId, "productCode", existingCode, "newlyIssued", false);'));
check('Transaction includes both product code and registry',service.includes('@Transactional\n    public Map<String, Object> finalizeDepositProduct(') && service.includes('repository.registerDepositProductCode(productId, issued, resolvedActor)'));
check('Only approved active and current version can issue',service.includes('LoanCodeIssuanceGate.requireApprovedCurrent(') && service.includes('repository.currentProductVersionCount(productId) != 1L'));
check('Approval governance enforced before numeric serial allocation',service.lastIndexOf('LoanCodeIssuanceGate.requireGovernance(') < service.indexOf('repository.allocateDepositProductCode(family, depositTypeCode)'));
check('Product remains INACTIVE after code issuance',service.includes('Map.of("PRODUCT_CODE", issued.code(), "PRODUCT_STATUS_CODE", "INACTIVE")'));
check('Migration requires old R30 registry and shared year',migration.includes('R30 deposit registry prerequisite') && migration.includes('Bank-controlled working-year prerequisite'));
check('No legacy registry DDL or rewrite in HF5 migration',!migration.includes('DROP TABLE') && !migration.includes('ALTER TABLE PDL.PRODUCT_DEPOSIT_CODE_REGISTRY'));
check('R30 legacy registry remains present in repository',repo.includes('.PRODUCT_DEPOSIT_CODE_REGISTRY WHERE PRODUCT_ID = :product'));
check('Migration validates UQ_PRODUCT_CODE cross-domain uniqueness',migration.includes('CONSTRAINT_NAME = \'UQ_PRODUCT_CODE\' AND STATUS = \'ENABLED\''));
check('New registry unique for numeric type/year/serial',migration.includes('UNIQUE (ISSUANCE_TYPE_CODE, WORK_YEAR, SERIAL_NO)'));
check('New registry unique for product code',migration.includes('UQ_PDL_PDTCR_CODE UNIQUE (PRODUCT_CODE)'));
check('New registry references real product',migration.includes('REFERENCES PDL.PRODUCT(PRODUCT_ID)'));
check('New registry enforces numeric type code shape',migration.includes('CK_PDL_PDTCR_MAPPING CHECK'));
check('New registry enforces persisted code format',migration.includes('CK_PDL_PDTCR_FORMAT CHECK'));
check('No business DML in HF5 migration',!/\b(UPDATE|INSERT\s+INTO|DELETE\s+FROM|MERGE\s+INTO)\b/i.test(migration.split('\n').filter(l=>!l.trim().startsWith('--')).join('\n')));
check('Migration fails closed for incomplete existing table',migration.includes('HF5 numeric registry constraints are incomplete.') && migration.includes('HF5 numeric registry has missing columns.'));
check('Migration never drops or renumbers existing product codes',!migration.includes('UPDATE PDL.PRODUCT SET') && !migration.includes('DROP CONSTRAINT UQ_PRODUCT_CODE'));
check('Stage-six Persian explanation reflects numeric type, not family',ui.includes('Oracle +') && !ui.includes('family + bank working year'));
check('Loan product code policy unchanged',get(javaApp+'LoanProductCodePolicy.java').includes('return code + workingYear + String.format'));
check('HF5 verifier wired to Windows production build',cmd.includes('verify-pdl-r30-hf5-deposit-type-code.mjs'));
check('HF5 verifier wired to Linux production build',sh.includes('verify-pdl-r30-hf5-deposit-type-code.mjs'));

const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-hf5-'));
try {
  const testFile=path.join(tmp,'HF5NumericDepositTypeTest.java');
  fs.writeFileSync(testFile,`import com.behsazan.corebanking.productbuilder.application.*;
public final class HF5NumericDepositTypeTest {
  static int pass=0;
  static void ok(String message,boolean value){if(!value) throw new AssertionError(message); pass++; System.out.println("PASS | "+message);}
  static boolean rejects(Runnable action){try{action.run(); return false;}catch(ProductBuilderValidationException ex){return true;}}
  public static void main(String[] args){
    String[] types={"CURRENT_NO_INTEREST","SAVINGS","SHORT_TERM_DEPOSIT","LONG_TERM_DEPOSIT","CERTIFICATE_OF_DEPOSIT"};
    String[] codes={"2510","3510","4510","5510","6510"};
    for(int i=0;i<types.length;i++){
      String t=types[i], p=codes[i];
      ok("map "+t,DepositProductCodePolicy.numericTypeCode(t).equals(p));
      ok("first "+t,DepositProductCodePolicy.format(t,1405,1).equals(p+"1405001"));
      ok("second "+t,DepositProductCodePolicy.format(t,1405,2).equals(p+"1405002"));
      ok("max "+t,DepositProductCodePolicy.format(t,1405,999).equals(p+"1405999"));
      ok("year reset "+t,DepositProductCodePolicy.format(t,1406,1).equals(p+"1406001"));
    }
    ok("normalizes lowercase",DepositProductCodePolicy.format(" savings ",1405,1).equals("35101405001"));
    ok("fixed length 11",DepositProductCodePolicy.format("SAVINGS",1405,1).length()==11);
    ok("no ref surrogate 8",rejects(()->DepositProductCodePolicy.format("8",1405,1)));
    ok("no numeric legacy business 2",rejects(()->DepositProductCodePolicy.format("2",1405,1)));
    ok("no product family prefix",rejects(()->DepositProductCodePolicy.format("QARD_SAVINGS",1405,1)));
    ok("unknown rejected",rejects(()->DepositProductCodePolicy.format("UNKNOWN",1405,1)));
    ok("null rejected",rejects(()->DepositProductCodePolicy.format(null,1405,1)));
    ok("correspondent nostro rejected",rejects(()->DepositProductCodePolicy.format("NOSTRO_ACCOUNT",1405,1)));
    ok("correspondent vostro rejected",rejects(()->DepositProductCodePolicy.format("VOSTRO_ACCOUNT",1405,1)));
    ok("serial 0 rejected",rejects(()->DepositProductCodePolicy.format("SAVINGS",1405,0)));
    ok("serial 1000 rejected",rejects(()->DepositProductCodePolicy.format("SAVINGS",1405,1000)));
    ok("year 1299 rejected",rejects(()->DepositProductCodePolicy.format("SAVINGS",1299,1)));
    ok("year 1601 rejected",rejects(()->DepositProductCodePolicy.format("SAVINGS",1601,1)));
    ok("loan unchanged",LoanProductCodePolicy.format("10",1405,2).equals("101405002"));
    System.out.println("PDL_R30_HF5_JAVA_PASS="+pass+" FAIL=0");
  }
}`);
  const sources=['DepositProductCodePolicy.java','LoanProductCodePolicy.java','ProductBuilderValidationException.java']
    .map(f=>path.join(root,javaApp,f));
  for(const [command,args] of [['javac',['-encoding','UTF-8','-d',tmp,...sources,testFile]],['java',['-cp',tmp,'HF5NumericDepositTypeTest']]]){
    const result=spawnSync(command,args,{encoding:'utf8'});
    if(result.error||result.status!==0)throw new Error(`${command}: ${result.stderr||result.stdout||result.error}`);
    process.stdout.write(result.stdout||'');
  }
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
console.log(`PDL_R30_HF5_STATIC_PASS=${pass} FAIL=${fail}`);
if(fail)process.exitCode=1;
