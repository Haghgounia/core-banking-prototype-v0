import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const target = path.join(root, 'tools', 'verify-pdl-product-builder.mjs');
const originalBlock = "must(workspace.includes('TARGET_PRODUCT_FAMILIES'), 'Target product-family catalog missing');\nfor (const family of ['CURRENT_ACCOUNT','QARD_SAVINGS','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT','CERTIFICATE_OF_DEPOSIT','NOSTRO_ACCOUNT','VOSTRO_ACCOUNT','RETAIL_LOAN','QARD_HASAN_LOAN']) {\n  must(workspace.includes(family), `Target family missing: ${family}`);\n}\n";
const correctedBlock = "// FIX PB-R24: deposits remain a static catalog; loan families are derived from ACTIVE PDL.LOAN_TYPE groups.\n// RETAIL_LOAN is a legacy persisted family only and must NOT be offered to new products.\nconst staticFamilyBlock = workspace.split('const TARGET_PRODUCT_FAMILIES:')[1]?.split('const LOAN_FAMILY_FROM_GROUP:')[0];\nmust(staticFamilyBlock !== undefined, 'Static deposit family catalog missing');\nfor (const family of ['CURRENT_ACCOUNT','QARD_SAVINGS','SHORT_TERM_DEPOSIT','LONG_TERM_DEPOSIT','CERTIFICATE_OF_DEPOSIT','NOSTRO_ACCOUNT','VOSTRO_ACCOUNT']) {\n  must(staticFamilyBlock.includes(`code: '${family}'`), `Static deposit family missing: ${family}`);\n}\nmust(!staticFamilyBlock.includes(\"domain: 'LOAN'\"), 'Loan family must not be static');\nfor (const family of ['QARD_HASAN_LOAN','EXCHANGE_LOAN','PARTICIPATORY_LOAN','COMMITMENT_LOAN']) {\n  must(workspace.includes(`code: '${family}'`), `Loan group family mapping missing: ${family}`);\n}\nmust(workspace.includes(\"this.service.rows('LOAN_TYPE', {page, size: 200})\"), 'Dynamic loan family source missing');\nmust(workspace.includes(\"String(row['RECORD_STATUS_CODE']).trim().toUpperCase() === 'ACTIVE'\"), 'Inactive loan types must not create loan families');\nmust(!workspace.includes(\"{code: 'RETAIL_LOAN', label:\"), 'Legacy RETAIL_LOAN must not be offered on create');\n";
const wantsApply = process.argv.includes('--apply');
const wantsCheck = process.argv.includes('--check');
if (wantsApply === wantsCheck) {
  console.error('Usage: node tools/repair-pb-r24-hf1-family-verifier.mjs --check | --apply');
  process.exit(2);
}
try {
  if (!fs.existsSync(target)) throw new Error('Existing verifier not found: tools/verify-pdl-product-builder.mjs');
  const source = fs.readFileSync(target, 'utf8');
  const oldCount = source.split(originalBlock).length - 1;
  const newCount = source.split(correctedBlock).length - 1;
  if (newCount === 1 && oldCount === 0) {
    console.log('PB_R24_HF1_ALREADY_APPLIED CHANGES=0');
    process.exit(0);
  }
  if (oldCount !== 1 || newCount !== 0) throw new Error('Source mismatch: refusing to change an unknown verifier version');
  const updated = source.replace(originalBlock, correctedBlock);
  console.log('PB_R24_HF1_PREFLIGHT_OK CHANGES=1');
  if (!wantsApply) process.exit(0);
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const backup = path.join(root, 'docs', 'patch-backups', 'pb-r24-hf1-family-verifier', timestamp, 'verify-pdl-product-builder.mjs');
  fs.mkdirSync(path.dirname(backup), {recursive: true});
  fs.writeFileSync(backup, source, 'utf8');
  const temp = target + '.pb-r24-hf1.tmp';
  fs.writeFileSync(temp, updated, 'utf8');
  fs.renameSync(temp, target);
  console.log('PB_R24_HF1_APPLIED CHANGES=1 BACKUP=' + backup);
} catch(error) {
  console.error('PB_R24_HF1_REFUSED: ' + (error?.message || error));
  process.exitCode=1;
}
