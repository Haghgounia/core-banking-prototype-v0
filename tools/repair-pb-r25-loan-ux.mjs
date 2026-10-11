import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

/** PB-R25: preflight all hunks, preserve independent edits and back up changed sources. */
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const patchRoot = path.join(root, 'docs', 'patches', 'pb-r25-loan-ux');
const manifest = JSON.parse(fs.readFileSync(path.join(patchRoot, 'manifest.json'), 'utf8'));
const apply = process.argv.includes('--apply');
if (!apply && !process.argv.includes('--check')) {
  console.error('Usage: node tools/repair-pb-r25-loan-ux.mjs --check | --apply');
  process.exit(2);
}
if (manifest.patch_id !== 'PB-R25') throw new Error('Unknown patch manifest.');
const normalized = rel => {
  if (typeof rel !== 'string' || !rel || path.isAbsolute(rel) ||
      rel.split(/[\\/]/).some(part => part === '..' || part === '.'))
    throw new Error(`Unsafe patch destination: ${rel}`);
  const result = path.resolve(root, rel);
  if (!result.startsWith(root + path.sep)) throw new Error(`Unsafe patch destination: ${rel}`);
  return result;
};
const occurrences = (text, part) => text.split(part).length - 1;
const updates = [];
function planModified(rel, operations) {
  const file = normalized(rel);
  if (!fs.existsSync(file)) throw new Error(`Baseline source missing: ${rel}; the full 19:04 source is required.`);
  const original = fs.readFileSync(file, 'utf8');
  let next = original;
  for (const [index, item] of operations.entries()) {
    if (!item.before || !item.after || item.before === item.after)
      throw new Error(`Invalid patch segment ${index + 1} in ${rel}`);
    const beforeCount = occurrences(next, item.before);
    const afterCount = occurrences(next, item.after);
    if (afterCount === 1 && (beforeCount === 0 || item.after.includes(item.before))) continue;
    if (beforeCount !== 1) throw new Error(`Source conflict, refusing to rewrite ${rel}, section ${index + 1}`);
    next = next.replace(item.before, item.after);
  }
  if (next !== original) updates.push({rel, file, next, original, newFile: false});
}
function planNew(rel) {
  const file = normalized(rel);
  const source = path.join(patchRoot, 'new-files', rel);
  if (!fs.existsSync(source)) throw new Error(`Patch payload missing: ${rel}`);
  const next = fs.readFileSync(source, 'utf8');
  if (fs.existsSync(file)) {
    if (fs.readFileSync(file, 'utf8') !== next) throw new Error(`Existing target differs; refusing to overwrite ${rel}`);
    return;
  }
  updates.push({rel, file, next, original: null, newFile: true});
}
try {
  for (const [rel, operations] of Object.entries(manifest.patches)) planModified(rel, operations);
  for (const rel of manifest.new_files) planNew(rel);
  console.log(`PB_R25_PATCH_PREFLIGHT_OK FILES=${Object.keys(manifest.patches).length + manifest.new_files.length} CHANGES=${updates.length}`);
  if (!apply) process.exit(0);
  if (!updates.length) { console.log('PB_R25_PATCH_ALREADY_APPLIED'); process.exit(0); }
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const backup = path.join(root, 'docs', 'patch-backups', 'pb-r25-loan-ux', timestamp);
  for (const item of updates) {
    if (item.newFile) continue;
    const dest = path.join(backup, item.rel);
    fs.mkdirSync(path.dirname(dest), {recursive: true});
    fs.writeFileSync(dest, item.original, 'utf8');
  }
  const written = [];
  try {
    for (const item of updates) {
      fs.mkdirSync(path.dirname(item.file), {recursive: true});
      const temporary = `${item.file}.pb-r25.tmp`;
      try { fs.writeFileSync(temporary, item.next, 'utf8'); fs.renameSync(temporary, item.file); }
      finally { if (fs.existsSync(temporary)) fs.unlinkSync(temporary); }
      written.push(item);
    }
  } catch (error) {
    for (const item of written.reverse()) {
      if (item.newFile) fs.unlinkSync(item.file);
      else fs.writeFileSync(item.file, item.original, 'utf8');
    }
    throw error;
  }
  console.log(`PB_R25_PATCH_APPLIED CHANGES=${updates.length} BACKUPS=${backup}`);
} catch (error) {
  console.error('PB_R25_PATCH_REFUSED: ' + (error?.message || error));
  process.exitCode = 1;
}
