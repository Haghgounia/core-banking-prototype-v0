import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const version = fs.readFileSync(path.join(root, 'VERSION'), 'utf8').trim() || 'unknown';
const patchDir = path.join(root, 'docs', 'patches');
const backupRoot = path.join(root, '.upgrade-backup', version, 'legacy-layout');
fs.mkdirSync(patchDir, {recursive: true});

let moved = 0;
let archived = 0;
let removed = 0;

const legacyRootReadme = path.join(root, 'README-FA.txt');
const readmeTarget = path.join(patchDir, 'PATCH-0.3.2-BUILD-FIX1-README-FA.txt');
if (fs.existsSync(legacyRootReadme)) {
  if (!fs.existsSync(readmeTarget)) {
    fs.renameSync(legacyRootReadme, readmeTarget);
    moved += 1;
    console.log('Root layout migration: moved README-FA.txt to docs/patches.');
  } else {
    const source = fs.readFileSync(legacyRootReadme);
    const target = fs.readFileSync(readmeTarget);
    if (source.equals(target)) {
      fs.unlinkSync(legacyRootReadme);
      removed += 1;
      console.log('Root layout migration: removed duplicate README-FA.txt from root.');
    } else {
      fs.mkdirSync(backupRoot, {recursive: true});
      fs.renameSync(legacyRootReadme, path.join(backupRoot, 'README-FA.txt'));
      archived += 1;
      console.log('Root layout migration: archived conflicting legacy README-FA.txt.');
    }
  }
}

for (const rel of ['config/application.yml_', 'backend/src/main/resources/application.yml_']) {
  const source = path.join(root, rel);
  if (!fs.existsSync(source)) continue;
  const target = path.join(backupRoot, rel);
  fs.mkdirSync(path.dirname(target), {recursive: true});
  fs.renameSync(source, target);
  archived += 1;
  console.log(`Root layout migration: archived stale backup ${rel}.`);
}

if (moved + archived + removed === 0) {
  console.log('Root layout migration OK: no stale root/backup files found.');
} else {
  console.log(`Root layout migration OK: moved=${moved}, archived=${archived}, removed=${removed}.`);
}
