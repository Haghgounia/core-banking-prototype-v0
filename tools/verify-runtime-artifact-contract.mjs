import {readFileSync} from 'node:fs';
import {join, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const buildCmd = readFileSync(join(root, 'build-production.cmd'), 'utf8');
const buildSh = readFileSync(join(root, 'build-production.sh'), 'utf8');
const startCmd = readFileSync(join(root, 'bin', 'start.cmd'), 'utf8');
const startSh = readFileSync(join(root, 'bin', 'start.sh'), 'utf8');
const exportCmd = readFileSync(join(root, 'bin', 'export-database.cmd'), 'utf8');
const packageReleaseCmd = readFileSync(join(root, 'package-release.cmd'), 'utf8');

const canonical = 'core-banking-prototype.jar';
const checks = [
  [buildCmd.includes(`set "JAR=%ROOT%app\\${canonical}"`), 'Windows build writes the canonical runtime JAR'],
  [buildCmd.includes('>"%ROOT%app\\BUILD-VERSION" echo %APP_VERSION%'), 'Windows build writes BUILD-VERSION'],
  [buildSh.includes(`JAR="$ROOT/app/${canonical}"`), 'Unix build writes the canonical runtime JAR'],
  [buildSh.includes('> "$ROOT/app/BUILD-VERSION"'), 'Unix build writes BUILD-VERSION'],
  [startCmd.includes(`set "JAR=%ROOT%\\app\\${canonical}"`), 'start.cmd uses only the canonical runtime JAR'],
  [startCmd.includes('if /i not "%BUILT_VERSION%"=="%APP_VERSION%"'), 'start.cmd rejects a stale runtime via BUILD-VERSION'],
  [startSh.includes(`JAR="$ROOT/app/${canonical}"`), 'start.sh uses only the canonical runtime JAR'],
  [startSh.includes('[ "$BUILT_VERSION" != "$APP_VERSION" ]'), 'start.sh rejects a stale runtime via BUILD-VERSION'],
  [exportCmd.includes(`set "JAR=%ROOT%\\app\\${canonical}"`), 'database export uses the canonical runtime JAR'],
  [packageReleaseCmd.includes('--exclude=app/BUILD-VERSION'), 'source packaging excludes BUILD-VERSION so overlay extraction cannot bless a stale JAR'],
  [![buildCmd, buildSh, startCmd, startSh, exportCmd].some((text) => /APP_VERSION[^\r\n]*\.jar|\.jar[^\r\n]*APP_VERSION/.test(text)), 'no active runtime script composes a JAR filename from the release version'],
];

const failed = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failed.length) {
  console.error('Runtime artifact contract verification FAILED:');
  for (const label of failed) console.error(`- ${label}`);
  process.exit(1);
}
console.log(`Runtime artifact contract verification OK: ${checks.length} checks; canonical JAR name and BUILD-VERSION guard are consistent.`);
