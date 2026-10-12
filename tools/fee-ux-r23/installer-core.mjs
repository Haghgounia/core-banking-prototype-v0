import fs from 'node:fs';
import crypto from 'node:crypto';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const assets = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(assets, '..', '..');
const manifest = JSON.parse(fs.readFileSync(path.join(assets, 'manifest.json'), 'utf8'));
const digest = bytes => crypto.createHash('sha256').update(bytes).digest('hex');

/** Install/uninstall on the exact live R2.2 sources; no wholesale AppShell or product overwrite. */
export function execute(operation) {
  if (operation !== 'install' && operation !== 'uninstall') throw new Error('Unknown operation');
  const checkOnly = process.argv.includes('--check');
  const desired = operation === 'install' ? 'updated' : 'base';
  const issues = [], changes = [];
  for (const entry of manifest.files) {
    const absolute = path.resolve(root, entry.path);
    if (!absolute.startsWith(root + path.sep)) {issues.push('invalid target path '+entry.path);continue;}
    if (!entry.path.startsWith('frontend/src/app/features/fee-admin/') && entry.path !== 'tools/verify-fee-ux-r22.mjs') {
      issues.push('unexpected target outside FEE UI: '+entry.path);continue;
    }
    const basePayload = path.join(assets, 'base', entry.filename);
    const updatedPayload = path.join(assets, 'updated', entry.filename);
    if (!fs.existsSync(absolute) || !fs.existsSync(basePayload) || !fs.existsSync(updatedPayload)) {
      issues.push('missing project file or patch payload: '+entry.path);continue;
    }
    const baseBytes = fs.readFileSync(basePayload);
    const updatedBytes = fs.readFileSync(updatedPayload);
    if (digest(baseBytes) !== entry.baseSha256 || digest(updatedBytes) !== entry.updatedSha256) {
      issues.push('invalid patch content hash: '+entry.path);continue;
    }
    const currentBytes = fs.readFileSync(absolute);
    const actual = digest(currentBytes);
    if (![entry.baseSha256, entry.updatedSha256].includes(actual)) {
      issues.push('concurrent change detected; refusing to overwrite: '+entry.path);continue;
    }
    const targetHash = desired === 'base' ? entry.baseSha256 : entry.updatedSha256;
    if (actual !== targetHash) changes.push({path: absolute, name:entry.path, before:currentBytes,
      after:desired==='base' ? baseBytes : updatedBytes});
  }
  const phase = `FEE_UX_R23_${operation.toUpperCase()}`;
  if (issues.length) {
    console.error(phase+'_PRECHECK_FAIL');
    for (const issue of issues) console.error(' - '+issue);
    console.error('No target file modified. Obtain a rebased patch if FEE changed in the new version.');
    process.exitCode = 1;
    return;
  }
  if (checkOnly) {
    console.log(phase+'_PRECHECK_PASS | '+changes.length+' pending changes; no files modified');
    return;
  }
  const completed=[];
  try {
    for (const item of changes) {
      const temp = item.path+'.r23.tmp';
      try {fs.writeFileSync(temp, item.after); fs.renameSync(temp,item.path);}
      finally {if (fs.existsSync(temp)) fs.unlinkSync(temp);}
      completed.push(item);
      console.log(operation.toUpperCase()+' '+item.name);
    }
  } catch (error) {
    // Restore only this operation's writes; original files outside this patch remain untouched.
    for (const item of completed.reverse()) fs.writeFileSync(item.path, item.before);
    console.error(phase+'_ROLLBACK_AFTER_WRITE_ERROR',error.message);
    process.exitCode=1; return;
  }
  console.log(phase+'_PASS | '+changes.length+' files updated | Product Builder, FEE2, AppShell, Backend and Oracle untouched');
}
