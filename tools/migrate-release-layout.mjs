import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const docsInstall = path.join(root, 'docs', 'install');
fs.mkdirSync(docsInstall, {recursive: true});

const legacyInstallFiles = fs.readdirSync(root).filter(name => /^INSTALL-.*\.txt$/i.test(name));
let moved = 0;
let removed = 0;

for (const name of legacyInstallFiles) {
  const source = path.join(root, name);
  const target = path.join(docsInstall, name);
  if (fs.existsSync(target)) {
    fs.unlinkSync(source);
    removed += 1;
  } else {
    fs.renameSync(source, target);
    moved += 1;
  }
}

if (legacyInstallFiles.length > 0) {
  console.log(`Release layout migration OK: ${moved} INSTALL file(s) moved, ${removed} duplicate root copy/copies removed.`);
} else {
  console.log('Release layout migration OK: no legacy root INSTALL files found.');
}
