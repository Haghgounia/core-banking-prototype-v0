import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const modelPath = path.join(root, 'backend', 'src', 'main', 'resources', 'cif', 'party-reference', 'party-reference-model.json');
const migrationPath = path.join(root, 'database', 'oracle', 'cif', 'migrations', '0.3.79-fix87-religion-denomination-reference.sql');
const model = JSON.parse(fs.readFileSync(modelPath, 'utf8'));
const migration = fs.readFileSync(migrationPath, 'utf8');
const failures = [];

const identity = model.packages.find(x => x.name === 'Identity and Party');
for (const table of ['REF_RELIGION', 'REF_RELIGIOUS_DENOMINATION']) {
  if (!model.tables[table]) failures.push(`${table} missing from metadata model`);
  if (!identity?.tables?.includes(table)) failures.push(`${table} missing from Identity and Party package`);
}
const religion = model.tables.REF_RELIGION;
const denom = model.tables.REF_RELIGIOUS_DENOMINATION;
if (religion?.pk?.[0] !== 'RELIGION_CODE') failures.push('REF_RELIGION PK must be RELIGION_CODE');
if (denom?.pk?.[0] !== 'DENOMINATION_CODE') failures.push('REF_RELIGIOUS_DENOMINATION PK must be DENOMINATION_CODE');
if (denom?.relation?.field !== 'RELIGION_CODE' || denom?.relation?.target !== 'REF_RELIGION') failures.push('Denomination relation to religion is missing');
if ((religion?.seedRows?.length ?? 0) !== 7) failures.push('REF_RELIGION seed count must be 7');
if ((denom?.seedRows?.length ?? 0) !== 16) failures.push('REF_RELIGIOUS_DENOMINATION seed count must be 16');
for (const token of ['CREATE TABLE CIF.REF_RELIGION', 'CREATE TABLE CIF.REF_RELIGIOUS_DENOMINATION', 'FK_REF_REL_DENOM_RELIGION', 'FK_REF_REL_DENOM_PARENT']) {
  if (!migration.includes(token)) failures.push(`Migration missing ${token}`);
}
if (model.sourceStats?.enabledTableCount !== 105) failures.push(`enabledTableCount must be 105, found ${model.sourceStats?.enabledTableCount}`);

if (failures.length) {
  console.error('CIF religion reference verification FAILED:');
  failures.forEach(x => console.error(` - ${x}`));
  process.exit(1);
}
console.log('CIF religion reference verification OK: 2 forms, 7 religions, 16 denominations, governed relation wired.');
