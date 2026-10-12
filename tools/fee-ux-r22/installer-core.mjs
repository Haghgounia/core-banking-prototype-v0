import fs from 'node:fs';
import crypto from 'node:crypto';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(currentDirectory, '..', '..');
const manifest = JSON.parse(fs.readFileSync(path.join(currentDirectory,'manifest.json'), 'utf8'));
const digest = bytes => crypto.createHash('sha256').update(bytes).digest('hex');

/** Safe, all-file preflight. Only exact known R2.1/R2.2 file versions can be replaced. */
export function execute(operation = 'install') {
  if (!['install','uninstall'].includes(operation)) throw new Error('Invalid operation');
  const checkOnly = process.argv.includes('--check');
  const wanted = operation === 'install' ? 'updated' : 'base';
  const changes = [];
  const issues = [];
  for (const entry of manifest.files) {
    const file = path.resolve(root, entry.path);
    if (!file.startsWith(root + path.sep)) throw new Error('Unsafe manifest path');
    const payloadPath = path.join(currentDirectory, wanted, entry.filename);
    if (!fs.existsSync(file)) { issues.push(`${entry.path}: missing target`); continue; }
    const actual = digest(fs.readFileSync(file));
    const expectedBase = entry.baseSha256;
    const expectedNew = entry.updatedSha256;
    if (digest(fs.readFileSync(path.join(currentDirectory,'base',entry.filename))) !== expectedBase ||
        digest(fs.readFileSync(path.join(currentDirectory,'updated',entry.filename))) !== expectedNew) {
      issues.push(`${entry.path}: damaged package payload`); continue;
    }
    if (actual === expectedBase || actual === expectedNew) {
      if (actual !== (wanted === 'updated' ? expectedNew : expectedBase)) changes.push({file,relative:entry.path,bytes:fs.readFileSync(payloadPath)});
    } else {
      issues.push(`${entry.path}: source changed since R2.1; refusing to overwrite`);
    }
  }
  if (issues.length) {
    console.error('FEE_R22_'+operation.toUpperCase()+'_PRECHECK_FAIL');
    for (const issue of issues) console.error(' - '+issue);
    console.error('No file has been modified. Rebase R2.2 onto the new FEE sources.');
    process.exitCode=1;
    return;
  }
  if (checkOnly) {
    console.log('FEE_R22_'+operation.toUpperCase()+'_PRECHECK_PASS | '+changes.length+' pending changes | no files written');
    return;
  }
  for (const change of changes) {
    const temp = change.file+'.fee-r22.tmp';
    fs.writeFileSync(temp,change.bytes);
    fs.renameSync(temp,change.file);
    console.log(operation.toUpperCase()+' '+change.relative);
  }
  console.log('FEE_R22_'+operation.toUpperCase()+'_PASS | '+changes.length+' files updated | FEE2/PDL/AppShell untouched');
}
