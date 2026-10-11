/** PB-R22: safe reference UI / Oracle-guard source migration; no Oracle DDL or DML. */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const payload = path.join(root, 'docs', 'patches', 'pb-r22-loan-reference');
const manifest = JSON.parse(fs.readFileSync(path.join(payload, 'manifest.json'), 'utf8'));
const mode = process.argv[2] ?? '--check';
if (!['--check','--apply'].includes(mode)) {
  console.error('Usage: node tools/repair-pb-r22-loan-reference.mjs --check | --apply');
  process.exit(2);
}
const requiredNew = [
  'frontend/src/app/features/product-builder/loan-type-plan-reference.component.ts',
  'frontend/src/app/features/product-builder/loan-type-plan-reference.component.html',
  'frontend/src/app/features/product-builder/loan-type-plan-reference.component.scss'
];
const updates = [];
const errors = [];
for (const file of manifest.patches) {
  const filePath = path.join(root, file.path);
  if (!fs.existsSync(filePath)) {errors.push('MISSING ' + file.path);continue;}
  const original = fs.readFileSync(filePath, 'utf8');
  let result = original;
  // Hunks have nonoverlapping three-line context, apply bottom-up.
  for (const [i, chunk] of [...file.hunks].reverse().entries()) {
    const beforeCount = result.split(chunk.before).length - 1;
    if (result.includes(chunk.after)) {
      // Check already-applied state FIRST: an insertion hunk may preserve its old context.
    } else if (beforeCount === 1) {
      result = result.replace(chunk.before, chunk.after);
    } else {
      errors.push('CONFLICT ' + file.path + ' hunk ' + (file.hunks.length - i) +
        ': expected unique old or previously applied text; review local changes');
    }
  }
  if (result !== original) updates.push({rel:file.path,original,result});
}
for (const rel of requiredNew) {
  const source = path.join(payload,'new-files',rel);
  const dest = path.join(root,rel);
  if (!fs.existsSync(source)) {errors.push('PAYLOAD_MISSING '+rel);continue;}
  const proposed = fs.readFileSync(source,'utf8');
  if (!fs.existsSync(dest)) updates.push({rel,original:null,result:proposed});
  else if (fs.readFileSync(dest,'utf8') !== proposed) errors.push('NEW_FILE_CONFLICT '+rel+' (existing custom file protected)');
}
if (errors.length) {
  errors.forEach(e => console.error('PB_R22_'+e));
  console.error(`PB_R22_MERGE_ABORTED CONFLICTS=${errors.length}; no target files changed.`);
  process.exit(1);
}
for (const u of updates) console.log('NEEDS_PATCH '+u.rel);
if (mode === '--check') {
  console.log(`PB_R22_MERGE_CHECK_OK CHANGES=${updates.length}`);
  process.exit(0);
}
if (!updates.length) {console.log('PB_R22_MERGE_SUCCESS PATCHED=0 (already installed)');process.exit(0);}
const safeStamp = new Date().toISOString().replace(/[:.]/g,'-');
const backupDir = path.join(root,'docs','patch-backups','pb-r22-loan-reference',safeStamp);
const completed = [];
try {
  for (const u of updates) {
    if (u.original !== null) {
      const backup = path.join(backupDir,u.rel);
      fs.mkdirSync(path.dirname(backup),{recursive:true});
      fs.writeFileSync(backup,u.original,'utf8');
      console.log('BACKUP '+path.relative(root,backup));
    }
  }
  for (const u of updates) {
    const target = path.join(root,u.rel);
    fs.mkdirSync(path.dirname(target),{recursive:true});
    const temp = target+'.pb-r22-writing';
    fs.writeFileSync(temp,u.result,'utf8');
    fs.renameSync(temp,target);
    completed.push(u);
    console.log('PATCHED '+u.rel);
  }
  console.log(`PB_R22_MERGE_SUCCESS PATCHED=${completed.length}`);
} catch (error) {
  console.error('PB_R22_IO_FAILURE',error);
  for (const u of completed.reverse()) {
    try {
      if (u.original === null) fs.rmSync(path.join(root,u.rel));
      else fs.writeFileSync(path.join(root,u.rel),u.original,'utf8');
    } catch (restoreError) { console.error('RESTORE_FAILED '+u.rel,restoreError); }
  }
  process.exitCode=1;
}
