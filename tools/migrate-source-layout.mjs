import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const version = fs.readFileSync(path.join(root, 'VERSION'), 'utf8').trim() || 'unknown';
const backupRoot = path.join(root, '.upgrade-backup', version, 'obsolete-source');

const obsoleteFiles = [
  'backend/src/main/java/com/behsazan/corebanking/productbuilder/application/ProductGovernanceService.java'
];

let archived = 0;
for (const rel of obsoleteFiles) {
  const source = path.join(root, rel);
  if (!fs.existsSync(source)) continue;
  const target = path.join(backupRoot, rel);
  fs.mkdirSync(path.dirname(target), {recursive: true});
  fs.copyFileSync(source, target);
  fs.unlinkSync(source);
  archived += 1;
  console.log(`Source layout migration: archived obsolete file ${rel}`);
}

if (archived === 0) {
  console.log('Source layout migration OK: no obsolete Product Builder governance source found.');
} else {
  console.log(`Source layout migration OK: ${archived} obsolete source file(s) archived under .upgrade-backup\\${version}.`);
}
