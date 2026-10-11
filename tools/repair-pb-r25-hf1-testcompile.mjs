import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

/** PB-R25 HF1: atomic, idempotent one-file Java testCompile repair. */
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const relative = 'backend/src/test/java/com/behsazan/corebanking/productbuilder/application/LoanProductFinalUxR25Test.java';
const filename = path.join(root, relative);
const isCheck = process.argv.includes('--check');
const isApply = process.argv.includes('--apply');
if (isCheck === isApply) {
  console.error('Usage: node tools/repair-pb-r25-hf1-testcompile.mjs --check | --apply');
  process.exit(2);
}
const before = `        Map<String, Object> invalid = repayment();
        invalid.put("MIN_TERM_DURATION", 2);
        assertThatThrownBy(() -> validator.validate("LOAN_PRODUCT_REPAYMENT_RULE", invalid))
                .isInstanceOf(ProductBuilderValidationException.class);
        invalid = repayment();
        invalid.put("PARTIAL_PREPAYMENT_ALLOWED", 1);
        final Map<String, Object> partial = invalid;`;
const after = `        final Map<String, Object> missingUnit = repayment();
        missingUnit.put("MIN_TERM_DURATION", 2);
        assertThatThrownBy(() -> validator.validate("LOAN_PRODUCT_REPAYMENT_RULE", missingUnit))
                .isInstanceOf(ProductBuilderValidationException.class);
        final Map<String, Object> partial = repayment();
        partial.put("PARTIAL_PREPAYMENT_ALLOWED", 1);`;
try {
  if (!fs.existsSync(filename)) throw new Error(`Required R25 test missing: ${relative}`);
  const original = fs.readFileSync(filename, 'utf8');
  const beforeCount = original.split(before).length - 1;
  const afterCount = original.split(after).length - 1;
  if (afterCount === 1 && beforeCount === 0) {
    console.log('PB_R25_HF1_PREFLIGHT_OK CHANGES=0');
    if (isApply) console.log('PB_R25_HF1_ALREADY_APPLIED');
    process.exit(0);
  }
  if (beforeCount !== 1 || afterCount !== 0) throw new Error('Unexpected R25 test source. No changes made.');
  const updated = original.replace(before, after);
  console.log('PB_R25_HF1_PREFLIGHT_OK CHANGES=1');
  if (!isApply) process.exit(0);
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const backup = path.join(root, 'docs', 'patch-backups', 'pb-r25-hf1-testcompile', timestamp, relative);
  fs.mkdirSync(path.dirname(backup), {recursive: true});
  fs.writeFileSync(backup, original, 'utf8');
  const tmp = `${filename}.pb-r25-hf1.tmp`;
  try {
    fs.writeFileSync(tmp, updated, 'utf8');
    fs.renameSync(tmp, filename);
  } finally {
    if (fs.existsSync(tmp)) fs.unlinkSync(tmp);
  }
  console.log(`PB_R25_HF1_APPLIED FILES=1 BACKUP=${backup}`);
} catch (error) {
  console.error('PB_R25_HF1_REFUSED: ' + (error?.message || error));
  process.exitCode = 1;
}
