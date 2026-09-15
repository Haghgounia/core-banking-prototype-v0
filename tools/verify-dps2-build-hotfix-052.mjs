import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const exists = relative => fs.existsSync(path.join(root, relative));
const version = read('VERSION').trim();
const versionAtLeast=(actual,minimum)=>{const a=actual.split('.').map(Number),m=minimum.split('.').map(Number);for(let i=0;i<3;i++){if(a[i]>m[i])return true;if(a[i]<m[i])return false}return true};
const pom = read('backend/pom.xml');
const packageJson = JSON.parse(read('frontend/package.json'));
const auditService = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/audit/application/DepositOpeningAuditService.java');
const buildCmd = read('build-production.cmd');
const buildSh = read('build-production.sh');

const immutableEntityPattern = /String requestedEntity\s*=\s*upper\(mutation\.entityName\(\)\);\s*final String entity\s*=\s*blank\(requestedEntity\)\s*\?\s*ROOT_ENTITY\s*:\s*requestedEntity;/s;
const legacyReassignPattern = /String entity\s*=\s*upper\(mutation\.entityName\(\)\);\s*if\s*\(blank\(entity\)\)\s*entity\s*=\s*ROOT_ENTITY;/s;

const checks = [
  [versionAtLeast(version,'0.5.2'), `VERSION must preserve 0.5.2 hotfix or later, got ${version}`],
  [pom.includes(`<version>${version}-SNAPSHOT</version>`), `backend Maven version is not ${version}-SNAPSHOT`],
  [packageJson.version === version, `frontend version must match ${version}, got ${packageJson.version}`],
  [immutableEntityPattern.test(auditService), 'Phase 5 audit mutation entity must be normalized into a final/effectively-final variable before lambda capture'],
  [!legacyReassignPattern.test(auditService), 'legacy reassigned entity local remains and can break javac lambda capture'],
  [auditService.includes('.orElseThrow(() -> validation('), 'expected validation lambda contract is missing'],
  [buildCmd.includes('verify-dps2-build-hotfix-052.mjs') && buildSh.includes('verify-dps2-build-hotfix-052.mjs'), 'production build does not execute 0.5.2 lambda hotfix verifier'],
  [exists('docs/install/INSTALL-0.5.2-FA.txt') && exists('docs/patches/PATCH-0.5.2-README-FA.txt') && exists('docs/DPS2-0.5.2-JAVA-LAMBDA-BUILD-HOTFIX-QA.md'), '0.5.2 hotfix documentation is incomplete']
];

const failed = checks.filter(([ok]) => !ok).map(([, message]) => message);
if (failed.length) {
  console.error('DPS2 0.5.2 Java lambda build hotfix verification FAILED:');
  for (const message of failed) console.error(`- ${message}`);
  process.exit(1);
}
console.log('DPS2 0.5.2 Java lambda build hotfix verification OK: effectively-final lambda capture and release wiring verified.');
