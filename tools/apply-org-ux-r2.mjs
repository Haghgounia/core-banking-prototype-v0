#!/usr/bin/env node
// Only applies manifest-listed files, after byte-level baseline validation.
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
const patch = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const manifest = JSON.parse(fs.readFileSync(path.join(patch, 'manifest.json'), 'utf8'));
const [projectArg, modeArg] = process.argv.slice(2);
const mode = (modeArg || 'CHECK').toUpperCase();
const fail = message => { console.error('ORG_UX_R2_PRECHECK_FAIL: ' + message); process.exit(1); };
if (!projectArg || !['CHECK','APPLY'].includes(mode)) fail('Usage: apply-org-ux-r2.cmd PROJECT_ROOT CHECK|APPLY');
const root = path.resolve(projectArg);
if (!fs.existsSync(path.join(root, 'VERSION'))) fail('Project root has no VERSION');
if (fs.readFileSync(path.join(root, 'VERSION'), 'utf8').trim() !== manifest.projectVersion) fail('Unexpected project VERSION');
const hash = file => createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const pending = [], already = [];
for (const entry of manifest.files) {
  if (entry.path.startsWith('/') || entry.path.split('/').some(part => part === '..' || part === ''))
    fail('Unsafe manifest path');
  const dst = path.join(root, ...entry.path.split('/'));
  const src = path.join(patch, 'files', ...entry.path.split('/'));
  if (!fs.existsSync(src) || hash(src) !== entry.afterSha256) fail('Patch content mismatch: ' + entry.path);
  const current = fs.existsSync(dst) ? hash(dst) : null;
  if (current === entry.afterSha256) already.push(entry);
  else if (current === entry.beforeSha256) pending.push(entry);
  else fail(`Project file diverged: ${entry.path}. No files changed; send this error instead of force-overwriting.`);
}
console.log(`ORG_UX_R2_PRECHECK_PASS: ${manifest.files.length} files; pending=${pending.length}; already=${already.length}`);
if (mode === 'CHECK') { console.log('CHECK ONLY: no changes.'); process.exit(0); }
if (!pending.length) { console.log('ORG_UX_R2_ALREADY_APPLIED; no changes.'); process.exit(0); }
const backupRoot = path.join(root, 'patch-backups', 'ORG_UX_R2_2026-10-21',
  new Date().toISOString().replaceAll(':', '-'));
// Back up all files before modifying any source file.
for (const entry of pending) {
  const dst = path.join(root, ...entry.path.split('/'));
  if (entry.beforeSha256 !== null) {
    const backup = path.join(backupRoot, ...entry.path.split('/'));
    fs.mkdirSync(path.dirname(backup), {recursive:true});
    fs.copyFileSync(dst, backup);
    if (hash(backup) !== entry.beforeSha256) fail('Backup verification failed: ' + entry.path);
  }
}
let copied = 0;
for (const entry of pending) {
  const dst = path.join(root, ...entry.path.split('/'));
  const src = path.join(patch, 'files', ...entry.path.split('/'));
  const current = fs.existsSync(dst) ? hash(dst) : null;
  if (current !== entry.beforeSha256) fail('Project changed during apply: ' + entry.path);
  fs.mkdirSync(path.dirname(dst), {recursive:true});
  const tmp = `${dst}.org-ux-r2.tmp`;
  fs.copyFileSync(src, tmp);
  if (hash(tmp) !== entry.afterSha256) fail('Temporary content verification failed: ' + entry.path);
  fs.renameSync(tmp, dst);
  copied++;
}
console.log(`ORG_UX_R2_APPLY_PASS: ${copied} files updated. Backups: ${backupRoot}`);
