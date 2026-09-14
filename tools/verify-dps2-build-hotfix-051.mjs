import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const exists = relative => fs.existsSync(path.join(root, relative));
const version = read('VERSION').trim();
const atLeast051 = (() => { const [a,b,c] = version.split('.').map(Number); return a > 0 || b > 5 || (b === 5 && c >= 1); })();
const pom = read('backend/pom.xml');
const packageJson = JSON.parse(read('frontend/package.json'));
const auditService = read('backend/src/main/java/com/behsazan/corebanking/deposit/opening/audit/application/DepositOpeningAuditService.java');
const buildCmd = read('build-production.cmd');
const buildSh = read('build-production.sh');

const phase4Support = [
  'backend/src/main/java/com/behsazan/corebanking/deposit/account/domain/DepositAccountModels.java',
  'backend/src/main/java/com/behsazan/corebanking/deposit/account/error/DepositAccountLifecycleException.java',
  'backend/src/main/java/com/behsazan/corebanking/deposit/account/error/DepositAccountNotFoundException.java',
  'backend/src/main/java/com/behsazan/corebanking/deposit/account/application/DepositAccountLifecycleService.java',
  'backend/src/main/java/com/behsazan/corebanking/deposit/account/oracle/DepositAccountRepository.java'
];

const checks = [
  [atLeast051, `VERSION must be 0.5.1 or later, got ${version}`],
  [pom.includes(`<version>${version}-SNAPSHOT</version>`), `backend Maven version is not ${version}-SNAPSHOT`],
  [packageJson.version === version, `frontend version must equal VERSION ${version}, got ${packageJson.version}`],
  [phase4Support.every(exists), 'Phase 4 account lifecycle source package is incomplete'],
  [auditService.includes('import tools.jackson.core.JacksonException;'), 'Phase 5 audit service must use Jackson 3 core package'],
  [auditService.includes('import tools.jackson.databind.json.JsonMapper;'), 'Phase 5 audit service must use Spring Boot 4 / Jackson 3 JsonMapper'],
  [!auditService.includes('com.fasterxml.jackson.core.JsonProcessingException'), 'legacy Jackson 2 core import remains in Phase 5 audit service'],
  [!auditService.includes('com.fasterxml.jackson.databind.ObjectMapper'), 'legacy Jackson 2 databind import remains in Phase 5 audit service'],
  [buildCmd.includes('verify-dps2-deposit-opening-phase5.mjs') && buildSh.includes('verify-dps2-deposit-opening-phase5.mjs'), 'production build does not execute Phase 5 verifier'],
  [buildCmd.includes('verify-dps2-build-hotfix-051.mjs') && buildSh.includes('verify-dps2-build-hotfix-051.mjs'), 'production build does not execute 0.5.1 hotfix verifier'],
  [buildCmd.includes('DepositAccountModels.java') && buildCmd.includes('DepositAccountRepository.java'), 'Windows production build lacks Phase 4 source completeness guard'],
  [buildSh.includes('DepositAccountModels.java') && buildSh.includes('DepositAccountRepository.java'), 'Unix production build lacks Phase 4 source completeness guard'],
  [exists('docs/install/INSTALL-0.5.1-FA.txt') && exists('docs/patches/PATCH-0.5.1-README-FA.txt') && exists('docs/DPS2-0.5.1-BUILD-HOTFIX-QA.md'), '0.5.1 hotfix documentation is incomplete']
];

const failed = checks.filter(([ok]) => !ok).map(([, message]) => message);
if (failed.length) {
  console.error('DPS2 0.5.1 build hotfix verification FAILED:');
  for (const message of failed) console.error(`- ${message}`);
  process.exit(1);
}
console.log('DPS2 0.5.1 build hotfix verification OK: cumulative Phase 4 source completeness, Jackson 3 alignment and production-build guards verified.');
