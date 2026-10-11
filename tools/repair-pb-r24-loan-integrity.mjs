import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const patchRoot = path.join(root, 'docs', 'patches', 'pb-r24-loan-integrity');
const manifest = JSON.parse(fs.readFileSync(path.join(patchRoot, 'manifest.json'), 'utf8'));
const apply = process.argv.includes('--apply');
if (!apply && !process.argv.includes('--check')) {
  console.error('Usage: node tools/repair-pb-r24-loan-integrity.mjs --check | --apply');
  process.exit(2);
}
const normalized = rel => {
  const result = path.resolve(root, rel);
  if (!(result.startsWith(root + path.sep) && !path.isAbsolute(rel) && !rel.split(/[\\/]/).includes('..')))
    throw new Error(`Unsafe patch destination: ${rel}`);
  return result;
};
const updates = [];
function occurrences(value, part) { return value.split(part).length - 1; }
function planModified(rel, operations) {
  const file = normalized(rel);
  if (!fs.existsSync(file)) throw new Error(`Baseline missing (install PB-R23 first): ${rel}`);
  const original = fs.readFileSync(file, 'utf8');
  let next = original;
  for (const item of operations) {
    if (!item.before || !item.after || item.before === item.after) throw new Error(`Invalid patch for ${rel}`);
    // A replacement may contain its own original anchor. Check the completed replacement first.
    const beforeCount = occurrences(next, item.before);
    const afterCount = occurrences(next, item.after);
    if (afterCount >= 1 && (beforeCount === 0 || item.after.includes(item.before))) continue;
    if (beforeCount !== 1) throw new Error(`Source mismatch, refusing rewrite: ${rel}`);
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
    if (fs.readFileSync(file, 'utf8') !== next) throw new Error(`Existing target differs; refusing overwrite: ${rel}`);
    return;
  }
  updates.push({rel, file, next, original: null, newFile: true});
}
try {
  for (const [rel, replacements] of Object.entries(manifest.patches)) planModified(rel, replacements);
  for (const rel of manifest.new_files) planNew(rel);
  console.log(`PB_R24_PATCH_PREFLIGHT_OK FILES=${Object.keys(manifest.patches).length + manifest.new_files.length} CHANGES=${updates.length}`);
  if (!apply) process.exit(0);
  if (!updates.length) { console.log('PB_R24_PATCH_ALREADY_APPLIED'); process.exit(0); }
  const now = new Date().toISOString().replace(/[:.]/g, '-');
  const backup = path.join(root, 'docs', 'patch-backups', 'pb-r24-loan-integrity', now);
  // Before ANY modifications, save existing affected sources. External backups are not changed.
  for (const op of updates) {
    if (op.newFile) continue;
    const dest = path.join(backup, op.rel);
    fs.mkdirSync(path.dirname(dest), {recursive: true});
    fs.writeFileSync(dest, op.original, 'utf8');
  }
  for (const op of updates) {
    fs.mkdirSync(path.dirname(op.file), {recursive: true});
    const temporary = op.file + '.pb-r24.tmp';
    fs.writeFileSync(temporary, op.next, 'utf8');
    fs.renameSync(temporary, op.file);
  }
  console.log(`PB_R24_PATCH_APPLIED CHANGES=${updates.length} BACKUPS=${backup}`);
} catch (error) {
  console.error('PB_R24_PATCH_REFUSED: ' + (error?.message || error));
  process.exitCode = 1;
}
