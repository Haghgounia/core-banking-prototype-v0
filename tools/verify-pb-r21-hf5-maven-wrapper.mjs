import { readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const text = readFileSync(join(root, 'build-production.cmd'), 'utf8');
const checks = [
  ['HF5 repair marker', text.includes('PB-R21-HF5: reuse the Maven executable resolved during the early compile')],
  ['early Java compile preserved', text.includes('call mvnw.cmd -DskipTests clean compile || exit /b 1')],
  ['cached executable detected', text.includes('if defined __MVNW_CMD__ (')],
  ['cached path exists before direct call', text.includes('if exist "%__MVNW_CMD__%" (')],
  ['package launches cached Maven directly', text.includes('call "%__MVNW_CMD__%" clean package || exit /b 1')],
  ['fallback remains when wrapper cache unavailable', text.includes('call mvnw.cmd clean package || exit /b 1')],
  ['Angular production build preserved', text.includes('call npm run build || exit /b 1')],
  ['static asset copy preserved', text.includes('xcopy /e /i /y')],
  ['dirty guard set at beginning', text.includes('>"%ROOT%app\\BUILD-DIRTY" echo BUILD_IN_PROGRESS_OR_FAILED')],
  ['dirty guard cleared only at success', text.includes('if exist "%ROOT%app\\BUILD-DIRTY" del /q "%ROOT%app\\BUILD-DIRTY"')],
  ['canonical JAR remains', text.includes('set "JAR=%ROOT%app\\core-banking-prototype.jar"')],
  ['runtime version marker remains', text.includes('>"%ROOT%app\\BUILD-VERSION" echo %APP_VERSION%')],
];
let failed = 0;
for (const [name, ok] of checks) {
  console.log((ok ? 'PASS' : 'FAIL') + ' | ' + name);
  failed += !ok;
}
console.log(`PB_R21_HF5_STATIC_PASS=${checks.length - failed} FAIL=${failed}`);
process.exitCode = failed ? 1 : 0;
