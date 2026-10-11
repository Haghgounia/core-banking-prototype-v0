import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const buildFile = join(root, 'build-production.cmd');
const backupFile = join(root, 'docs', 'patch-backups', 'pb-r21-hf5', 'build-production.cmd');
const arg = process.argv[2] ?? '--check';
if (!['--check', '--apply'].includes(arg)) {
  console.error('Usage: node tools\\repair-pb-r21-hf5-maven-wrapper.mjs [--check|--apply]');
  process.exit(2);
}
const data = readFileSync(buildFile);
const text = data.toString('utf8');
const eol = text.includes('\r\n') ? '\r\n' : '\n';
const marker = 'PB-R21-HF5: reuse the Maven executable resolved during the early compile';
const oldLine = 'call mvnw.cmd clean package || exit /b 1';
const earlyLine = 'call mvnw.cmd -DskipTests clean compile || exit /b 1';
const prerequisites = [earlyLine,
  'if exist "%ROOT%frontend\\dist" rmdir /s /q "%ROOT%frontend\\dist"',
  'if exist "%ROOT%backend\\src\\main\\resources\\static" rmdir /s /q "%ROOT%backend\\src\\main\\resources\\static"',
  'if exist "%ROOT%app\\BUILD-DIRTY" del /q "%ROOT%app\\BUILD-DIRTY"'];
function stop(msg) { console.error('PB_R21_HF5_ABORT: ' + msg); process.exit(1); }
if (prerequisites.some(token => !text.includes(token))) stop('Unexpected build layout; no changes were made.');
if (text.includes(marker)) {
  console.log('PB_R21_HF5_ALREADY_APPLIED');
  process.exit(0);
}
if (text.split(oldLine).length !== 2) stop('Expected exactly one packaging invocation; no changes were made.');
if (!(text.indexOf(earlyLine) < text.indexOf(oldLine))) stop('Unexpected Maven build order; no changes were made.');
const replacement = [
  'rem ' + marker,
  'rem Keep the early compile through Maven Wrapper. It resolves __MVNW_CMD__ in this CMD process.',
  'rem A second PowerShell wrapper startup has been observed to crash after Angular production build.',
  'if defined __MVNW_CMD__ (',
  '  if exist "%__MVNW_CMD__%" (',
  '    call "%__MVNW_CMD__%" clean package || exit /b 1',
  '  ) else (',
  '    echo WARNING: Resolved Maven executable is not available. Retrying Maven Wrapper.',
  '    call mvnw.cmd clean package || exit /b 1',
  '  )',
  ') else (',
  '  echo WARNING: Maven Wrapper did not expose a resolved executable. Retrying wrapper.',
  '  call mvnw.cmd clean package || exit /b 1',
  ')',
].join(eol);
if (arg === '--check') {
  console.log('PB_R21_HF5_PATCHABLE: one packaging call, early Java compilation and runtime guards preserved.');
  process.exit(0);
}
if (existsSync(backupFile)) {
  const previous = readFileSync(backupFile);
  if (!previous.equals(data)) stop('An unrelated backup already exists; no changes were made.');
} else {
  mkdirSync(dirname(backupFile), { recursive: true });
  writeFileSync(backupFile, data);
}
writeFileSync(buildFile, text.replace(oldLine, replacement), 'utf8');
console.log('PB_R21_HF5_PATCH_APPLIED');
console.log('Original build script backup: docs\\patch-backups\\pb-r21-hf5\\build-production.cmd');
