import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), 'utf8');
const catalog = read('backend','src','main','java','com','behsazan','corebanking','fee','admin','application','FeeAdminCatalog.java');
const repo = read('backend','src','main','java','com','behsazan','corebanking','fee','admin','oracle','FeeAdminRepository.java');
const controller = read('backend','src','main','java','com','behsazan','corebanking','fee','admin','web','FeeAdminController.java');
const routes = read('frontend','src','app','app.routes.ts');
const home = read('frontend','src','app','features','fee-admin','fee-admin-home.component.html');
const homeTs = read('frontend','src','app','features','fee-admin','fee-admin-home.component.ts');
const table = read('frontend','src','app','features','fee-admin','fee-admin-table.component.html');
const manifest = JSON.parse(read('database','oracle','fee','baseline-1.0','seed','seed_manifest.json'));
const ddlDir = path.join(root,'database','oracle','fee','baseline-1.0','ddl');
const ddlFiles = fs.readdirSync(ddlDir).filter(name => name.endsWith('.sql'));
const ddlTables = ddlFiles.map(name => {
  const text = fs.readFileSync(path.join(ddlDir, name), 'utf8');
  return text.match(/CREATE\s+TABLE\s+FEE\.([A-Z0-9_]+)/i)?.[1]?.toUpperCase() ?? '';
});
const manifestTables = Object.keys(manifest.tables);
const manifestRowSum = Object.values(manifest.tables).reduce((sum, value) => sum + Number(value), 0);
const ddlInstaller = read('database','oracle','fee','install-baseline-1.0-ddl.sql');
const catalogTables = [...catalog.matchAll(/register\("(FEE_[A-Z0-9_]+)"/g)].map(m => m[1]);
const runtimeReadOnly = [...catalog.matchAll(/register\("(FEE_[A-Z0-9_]+)"[^\n]+, false\);/g)].map(m => m[1]);
const checks = [
  [manifest.table_count === 47, `seed manifest table_count must be 47, got ${manifest.table_count}`],
  [manifest.seeded_table_count === 47, `seeded_table_count must be 47, got ${manifest.seeded_table_count}`],
  [manifest.rows_total === 574, `seed rows_total must be 574, got ${manifest.rows_total}`],
  [ddlFiles.length === 47, `source DDL file count must be 47, got ${ddlFiles.length}`],
  [manifestRowSum === 574, `sum of manifest table rows must be 574, got ${manifestRowSum}`],
  [ddlTables.length === 47 && new Set(ddlTables).size === 47 && ddlTables.every(t => manifestTables.includes(t)), '47 source DDL files must map one-to-one to manifest tables'],
  [(ddlInstaller.match(/@@baseline-1\.0\/ddl\//g) ?? []).length === 47, 'DDL installer must invoke all 47 baseline DDL scripts'],
  [catalogTables.length === 47 && new Set(catalogTables).size === 47, `FeeAdminCatalog must register 47 unique tables, got ${catalogTables.length}`],
  [catalogTables.every(t => Object.hasOwn(manifest.tables, t)), 'every catalog table must exist in seed manifest'],
  [runtimeReadOnly.length === 12, `runtime query-only table count must be 12, got ${runtimeReadOnly.length}`],
  [repo.includes('core-banking.schemas.fee:FEE'), 'repository must use configured FEE schema'],
  [repo.includes('FEE_REF_VALUE') && repo.includes('DOMAIN_BY_COLUMN'), 'reference-domain lookup wiring is missing'],
  [repo.includes('SEQ_" + table') || repo.includes('"SEQ_" + table'), 'sequence-based positive ID allocation is missing'],
  [controller.includes('/api/v1/fees/admin'), 'fee admin API base route is missing'],
  [routes.includes("path: 'fee/simulator'") && routes.includes("path: 'fee/tables/:table'"), 'fee admin routes are incomplete'],
  [home.includes('۴۷ فرم کارمزد') && home.includes('Seed پایه'), 'fee admin home coverage labels are missing'],
  [home.includes('نقشه روال تعریف اطلاعات در فرم‌های کارمزد') && homeTs.includes('FEE_ARRANGEMENT_CALC_TERM') && (home.includes('/fee/simulator') || homeTs.includes("route: ['/fee', 'simulator']")), 'fee admin home flow diagram is missing or incomplete'],
  [table.includes('app-persian-date-input'), 'fee forms must use the shared Persian date picker'],
  [table.includes('Runtime / فقط مشاهده'), 'runtime read-only UI marker is missing'],
  [fs.existsSync(path.join(root,'database','oracle','fee','install-baseline-1.0-ddl.sql')), '47-table DDL installer is missing'],
  [fs.existsSync(path.join(root,'database','oracle','fee','install-baseline-1.0-seed.sql')), '574-row seed installer is missing']
];
const failed = checks.filter(([ok]) => !ok).map(([,message]) => message);
if (failed.length) {
  console.error('FEE Baseline 1.0 admin verification FAILED:');
  for (const message of failed) console.error(`- ${message}`);
  process.exit(1);
}
console.log(`FEE Baseline 1.0 admin verification OK: 47 forms/tables, 574 seed rows, ${runtimeReadOnly.length} runtime query-only forms.`);
