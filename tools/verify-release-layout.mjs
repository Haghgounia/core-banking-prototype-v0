import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const version = fs.readFileSync(path.join(root, 'VERSION'), 'utf8').trim();
const pkg = JSON.parse(fs.readFileSync(path.join(root, 'frontend', 'package.json'), 'utf8'));
const pom = fs.readFileSync(path.join(root, 'backend', 'pom.xml'), 'utf8');
const generated = fs.readFileSync(path.join(root, 'frontend', 'src', 'app', 'features', 'system-specification', 'system-version.generated.ts'), 'utf8');
const docsInstall = path.join(root, 'docs', 'install');
const rootInstall = fs.readdirSync(root).filter(name => /^INSTALL-.*\.txt$/i.test(name));
const buildCmd = fs.readFileSync(path.join(root, 'build-production.cmd'), 'utf8');
const buildSh = fs.readFileSync(path.join(root, 'build-production.sh'), 'utf8');
const migrationScript = path.join(root, 'tools', 'migrate-release-layout.mjs');
const cmdMigration = buildCmd.indexOf('migrate-release-layout.mjs');
const cmdVerify = buildCmd.indexOf('verify-release-layout.mjs');
const shMigration = buildSh.indexOf('migrate-release-layout.mjs');
const shVerify = buildSh.indexOf('verify-release-layout.mjs');

const checks = [
  [/^\d+\.\d+\.\d+$/.test(version), `VERSION must be semantic only (x.y.z), got ${version}`],
  [!version.includes('prototype-fee-p1'), 'VERSION must not contain prototype-fee-p1'],
  [pkg.version === version, `frontend/package.json version ${pkg.version} must equal VERSION ${version}`],
  [pom.includes(`<version>${version}-SNAPSHOT</version>`), `backend pom must contain ${version}-SNAPSHOT`],
  [generated.includes(version), 'generated system version must match VERSION'],
  [rootInstall.length === 0, `INSTALL files must not remain in root: ${rootInstall.join(', ')}`],
  [fs.existsSync(docsInstall), 'docs/install directory is missing'],
  [fs.existsSync(path.join(docsInstall, `INSTALL-${version}-FA.txt`)), `current install guide docs/install/INSTALL-${version}-FA.txt is missing`],
  [fs.existsSync(migrationScript), 'tools/migrate-release-layout.mjs is missing'],
  [cmdMigration >= 0 && cmdVerify >= 0 && cmdMigration < cmdVerify, 'build-production.cmd must migrate legacy INSTALL files before release-layout verification'],
  [shMigration >= 0 && shVerify >= 0 && shMigration < shVerify, 'build-production.sh must migrate legacy INSTALL files before release-layout verification'],
];

const failed = checks.filter(([ok]) => !ok).map(([, message]) => message);
if (failed.length) {
  console.error('Release layout verification FAILED:');
  for (const message of failed) console.error(`- ${message}`);
  process.exit(1);
}
console.log(`Release layout verification OK: version ${version}; no legacy suffix; INSTALL files under docs/install.`);
