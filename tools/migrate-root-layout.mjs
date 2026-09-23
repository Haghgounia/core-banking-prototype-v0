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


const legacyVerifierReadme = path.join(root, 'README.txt');
const verifierReadmeTarget = path.join(patchDir, 'PATCH-0.10.0-FINAL-VERIFIER-DOCKER-FALLBACK-README.txt');
if (fs.existsSync(legacyVerifierReadme)) {
  if (!fs.existsSync(verifierReadmeTarget)) {
    fs.renameSync(legacyVerifierReadme, verifierReadmeTarget);
    moved += 1;
    console.log('Root layout migration: moved legacy Final Verifier README.txt to docs/patches.');
  } else {
    const source = fs.readFileSync(legacyVerifierReadme);
    const target = fs.readFileSync(verifierReadmeTarget);
    if (source.equals(target)) {
      fs.unlinkSync(legacyVerifierReadme);
      removed += 1;
      console.log('Root layout migration: removed duplicate legacy Final Verifier README.txt from root.');
    } else {
      fs.mkdirSync(backupRoot, {recursive: true});
      fs.renameSync(legacyVerifierReadme, path.join(backupRoot, 'README.txt'));
      archived += 1;
      console.log('Root layout migration: archived conflicting legacy README.txt.');
    }
  }
}

for (const name of fs.readdirSync(root)) {
  if (!/^PATCH-LAYOUT-MIGRATION-.*\.txt$/i.test(name)) continue;
  const source = path.join(root, name);
  const target = path.join(patchDir, name);
  if (fs.existsSync(target)) {
    const sourceBytes = fs.readFileSync(source);
    const targetBytes = fs.readFileSync(target);
    if (sourceBytes.equals(targetBytes)) {
      fs.unlinkSync(source);
      removed += 1;
      console.log(`Root layout migration: removed duplicate ${name} from root.`);
    } else {
      fs.mkdirSync(backupRoot, {recursive: true});
      fs.renameSync(source, path.join(backupRoot, name));
      archived += 1;
      console.log(`Root layout migration: archived conflicting ${name}.`);
    }
  } else {
    fs.renameSync(source, target);
    moved += 1;
    console.log(`Root layout migration: moved ${name} to docs/patches.`);
  }
}


const allowedRootFiles = new Set([
  '.gitignore','CHANGELOG.md','README-FA.md','VERSION',
  'build-production.cmd','build-production.sh','package-release.cmd'
]);

for (const name of fs.readdirSync(root)) {
  const source = path.join(root, name);
  if (!fs.statSync(source).isFile()) continue;
  if (/^PATCH-.*\.txt$/i.test(name) || /^PHASE.*-PATCH-MANIFEST\.txt$/i.test(name)) {
    const target = path.join(patchDir, name);
    if (fs.existsSync(target)) {
      const sourceBytes = fs.readFileSync(source);
      const targetBytes = fs.readFileSync(target);
      if (sourceBytes.equals(targetBytes)) {
        fs.unlinkSync(source);
        removed += 1;
        console.log(`Root layout migration: removed duplicate ${name} from root.`);
      } else {
        fs.mkdirSync(backupRoot, {recursive: true});
        fs.renameSync(source, path.join(backupRoot, name));
        archived += 1;
        console.log(`Root layout migration: archived conflicting ${name}.`);
      }
    } else {
      fs.renameSync(source, target);
      moved += 1;
      console.log(`Root layout migration: moved ${name} to docs/patches.`);
    }
    continue;
  }
  if (!allowedRootFiles.has(name) && fs.statSync(source).size === 0) {
    fs.unlinkSync(source);
    removed += 1;
    console.log(`Root layout migration: removed zero-byte root artifact ${name}.`);
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
