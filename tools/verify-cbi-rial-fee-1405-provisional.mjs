import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const dir = path.join(root, 'database', 'oracle', 'fee', 'cbi-rial-1405-provisional');
const req = [
  '00-install-cbi-rial-fee-1405-provisional.sql',
  '01-import-cbi-rial-fee-1405-provisional.sql',
  '02-verify-cbi-rial-fee-1405-provisional.sql',
  '03-finalize-official-reference-template.sql',
  'cbi_rial_fee_1405_provisional_definitions.csv',
  'cbi_rial_fee_1405_provisional_components.csv',
  'cbi_rial_fee_1405_provisional_delta.csv',
  'cbi_rial_fee_1405_provisional_manifest.json',
  'README.md',
];
for (const f of req) {
  const p = path.join(dir, f);
  if (!fs.existsSync(p)) throw new Error(`Missing FIX98 artifact: ${f}`);
}
const manifest = JSON.parse(fs.readFileSync(path.join(dir, 'cbi_rial_fee_1405_provisional_manifest.json'), 'utf8'));
const checks = [
  ['new_tariffs', 152],
  ['source_components', 180],
  ['old_rial_versions_to_archive', 156],
  ['old_electronic_versions_retained', 73],
];
for (const [k,v] of checks) if (manifest[k] !== v) throw new Error(`${k}: ${manifest[k]} != ${v}`);
if (manifest.source_status !== 'PROVISIONAL') throw new Error('source must remain PROVISIONAL until official cover letter is supplied');
if (manifest.classification_code !== 'CBI_1405_RIAL_PROVISIONAL') throw new Error('classification mismatch');
const sql = fs.readFileSync(path.join(dir, '01-import-cbi-rial-fee-1405-provisional.sql'), 'utf8');
const count = (token) => sql.split(token).length - 1;
for (const [token, expected] of [
  ['MERGE INTO FEE_DEFINITION t',152],
  ['MERGE INTO FEE_DEFINITION_VERSION t',152],
  ['MERGE INTO FEE_CALCULATION_RULE t',152],
  ['MERGE INTO FEE_RULE_COMPONENT t',180],
  ['MERGE INTO FEE_CALCULATION_TIER t',15],
  ['MERGE INTO FEE_FEATURE t',9],
]) {
  const actual = count(token);
  if (actual !== expected) throw new Error(`${token}: ${actual} != ${expected}`);
}
if (!sql.includes("STATUS_CODE='SUPERSEDED'")) throw new Error('prior version archival guard missing');
if (!sql.includes("SOURCE_CODE='CBI_FEE_1404_04_35500'")) throw new Error('old regulatory source scope missing');
const archiveBlock = sql.split('Closing only prior non-electronic rial fee definition versions ...')[1]?.split('Upserting 9 source section features')[0] ?? '';
for (const code of ['CBI_ELECTRONIC_SERVICE_FEE_GROUP','CBI_ELECTRONIC_LC_RIAL_FEE_GROUP','CBI_ELECTRONIC_GUARANTEE_RIAL_FEE_GROUP','CBI_ELECTRONIC_BILL_FEE_GROUP']) {
  if (archiveBlock.includes(code)) throw new Error(`electronic fee group must not be archived: ${code}`);
}
const verify = fs.readFileSync(path.join(dir, '02-verify-cbi-rial-fee-1405-provisional.sql'), 'utf8');
for (const token of ['archived prior non-electronic rial versions','retained prior electronic versions','structured appraisal tiers','source calculation components']) {
  if (!verify.includes(token)) throw new Error(`verification assertion missing: ${token}`);
}
console.log('verify-cbi-rial-fee-1405-provisional: OK');
