import {readFileSync, existsSync} from 'node:fs';
import {join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {dirname} from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const checks = [];
const read = (...parts) => readFileSync(join(root, ...parts), 'utf8');
const expect = (name, condition) => checks.push({name, ok: Boolean(condition)});

const controllerPath = join(root, 'backend/src/main/java/com/behsazan/corebanking/system/modelcomparison/OracleEaXmiExportController.java');
const writerPath = join(root, 'backend/src/main/java/com/behsazan/corebanking/system/modelcomparison/EaOracleXmiWriter.java');
const inspectorPath = join(root, 'backend/src/main/java/com/behsazan/corebanking/system/modelcomparison/OracleEaMetadataInspector.java');
const uiPath = join(root, 'frontend/src/app/features/oracle-ea-xmi-export/oracle-ea-xmi-export.component.html');
const uiTsPath = join(root, 'frontend/src/app/features/oracle-ea-xmi-export/oracle-ea-xmi-export.component.ts');

expect('Backend export controller exists', existsSync(controllerPath));
expect('EA XMI writer exists', existsSync(writerPath));
expect('Oracle metadata inspector exists', existsSync(inspectorPath));
expect('Export form exists', existsSync(uiPath));

const controller = read('backend/src/main/java/com/behsazan/corebanking/system/modelcomparison/OracleEaXmiExportController.java');
const writer = read('backend/src/main/java/com/behsazan/corebanking/system/modelcomparison/EaOracleXmiWriter.java');
const inspector = read('backend/src/main/java/com/behsazan/corebanking/system/modelcomparison/OracleEaMetadataInspector.java');
const uiTs = read('frontend/src/app/features/oracle-ea-xmi-export/oracle-ea-xmi-export.component.ts');
const routes = read('frontend/src/app/app.routes.ts');
const menu = read('frontend/src/app/layout/app-shell.component.html');

expect('Configuration endpoint', controller.includes('@GetMapping("/configuration")'));
expect('Preview endpoint', controller.includes('@GetMapping("/preview")'));
expect('Export endpoint', controller.includes('/export'));
expect('XMI 1.1 marker', writer.includes('xmi.version", "1.1"'));
expect('UML 1.3 namespace', writer.includes('omg.org/UML1.3'));
expect('EA table stereotype', writer.includes('stereotype(document, tableElement, "table")'));
expect('EA column stereotype', writer.includes('stereotype(document, attribute, "column")'));
expect('EA FK association', writer.includes('writeAssociation'));
expect('Stable deterministic IDs', writer.includes('UUID.nameUUIDFromBytes'));
expect('Primary/unique constraints read from Oracle', inspector.includes("CONSTRAINT_TYPE IN ('P','U')"));
expect('Foreign keys read from Oracle', inspector.includes("FK.CONSTRAINT_TYPE='R'"));
expect('Indexes read from Oracle', inspector.includes('ALL_INDEXES'));
expect('Check constraints read from Oracle', inspector.includes('SEARCH_CONDITION_VC'));
expect('Comments read from Oracle', inspector.includes('ALL_TAB_COMMENTS') && inspector.includes('ALL_COL_COMMENTS'));
expect('Virtual-column metadata uses ALL_TAB_COLS', inspector.includes('ALL_TAB_COLS TC') && inspector.includes('TC.VIRTUAL_COLUMN') && !/[^T]C\.VIRTUAL_COLUMN/.test(inspector));
expect('No automatic metadata preview on page initialization', !/loadingConfiguration\.set\(false\);\s*this\.preview\(\)/s.test(uiTs));
expect('Frontend route wired', routes.includes("system/oracle-ea-xmi-export"));
expect('Sidebar menu wired', menu.includes('استخراج Oracle به EA XML'));

const failed = checks.filter(check => !check.ok);
for (const check of checks) console.log(`${check.ok ? 'PASS' : 'FAIL'} - ${check.name}`);
if (failed.length) {
  console.error(`\n${failed.length} verification checks failed.`);
  process.exit(1);
}
console.log(`\nAll ${checks.length} Oracle -> EA XMI exporter checks passed.`);
