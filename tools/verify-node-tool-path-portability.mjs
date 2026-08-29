import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const toolsDir = path.join(root, 'tools');
const files = fs.readdirSync(toolsDir)
  .filter(name => name.startsWith('verify-') && name.endsWith('.mjs') && name !== 'verify-node-tool-path-portability.mjs')
  .sort();

const failures = [];
for (const name of files) {
  const source = fs.readFileSync(path.join(toolsDir, name), 'utf8');
  if (/import\.meta\.url\)\.pathname/.test(source) || /new URL\([^\n]+import\.meta\.url[^\n]*\)\.pathname/.test(source)) {
    failures.push(`${name}: URL.pathname must not be used for filesystem paths; use fileURLToPath().`);
  }
  if (/process\.cwd\(\)/.test(source)) {
    failures.push(`${name}: verifier root must be derived from import.meta.url, not process.cwd().`);
  }
}

if (failures.length) {
  console.error('Node verifier path-portability verification FAILED:');
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log(`Node verifier path-portability verification OK: ${files.length} verifier scripts are cwd-independent and Windows-safe.`);
