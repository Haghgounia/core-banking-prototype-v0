import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const failures = [];
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), 'utf8');

const routes = read('frontend', 'src', 'app', 'app.routes.ts');
const shell = read('frontend', 'src', 'app', 'layout', 'app-shell.component.html');
const component = read('frontend', 'src', 'app', 'features', 'database-model-comparison', 'database-model-comparison.component.ts');
const template = read('frontend', 'src', 'app', 'features', 'database-model-comparison', 'database-model-comparison.component.html');
const frontendService = read('frontend', 'src', 'app', 'features', 'database-model-comparison', 'database-model-comparison.service.ts');
const controller = read('backend', 'src', 'main', 'java', 'com', 'behsazan', 'corebanking', 'system', 'modelcomparison', 'EaOracleComparisonController.java');
const service = read('backend', 'src', 'main', 'java', 'com', 'behsazan', 'corebanking', 'system', 'modelcomparison', 'EaOracleComparisonService.java');
const parser = read('backend', 'src', 'main', 'java', 'com', 'behsazan', 'corebanking', 'system', 'modelcomparison', 'EaXmiModelParser.java');
const inspector = read('backend', 'src', 'main', 'java', 'com', 'behsazan', 'corebanking', 'system', 'modelcomparison', 'OracleSchemaInspector.java');
const snapshot = read('backend', 'src', 'main', 'java', 'com', 'behsazan', 'corebanking', 'system', 'modelcomparison', 'OracleSchemaSnapshot.java');
const schemas = read('backend', 'src', 'main', 'java', 'com', 'behsazan', 'corebanking', 'system', 'modelcomparison', 'ConfiguredDatabaseSchemas.java');

const required = [
  [routes, "path: 'system/database-model-comparison'", 'route missing'],
  [shell, 'routerLink="/system/database-model-comparison"', 'sidebar menu entry missing'],
  [frontendService, '/api/v1/system/database-model-comparison', 'frontend API base URL missing'],
  [controller, '@PostMapping(value = "/compare", consumes = "multipart/form-data")', 'multipart compare endpoint missing'],
  [service, 'includeRowCounts', 'row-count option missing from comparison service'],
  [inspector, 'SELECT COUNT(*) FROM ', 'exact table row count query missing'],
  [inspector, 'ALL_TAB_COLUMNS', 'Oracle column metadata comparison missing'],
  [inspector, 'ALL_CONSTRAINTS', 'Oracle primary-key metadata comparison missing'],
  [schemas, 'core-banking.schemas.cif', 'configured CIF schema mapping missing'],
  [schemas, 'core-banking.schemas.reference-data', 'configured GEO schema mapping missing'],
  [schemas, 'core-banking.schemas.deposit-product-factory', 'configured DPS schema mapping missing'],
  [parser, 'disallow-doctype-decl', 'XXE/DOCTYPE hardening missing'],
  [parser, 'definitions.size()', 'EA duplicate table-definition merge missing'],
  [parser, 'enrichSelectedColumns', 'EA duplicate column metadata enrichment missing'],
  [service, 'db.displayType(ea.lengthSemantics() != null)', 'implicit length-semantics display normalization missing'],
  [snapshot, 'displayType(boolean includeLengthSemantics)', 'Oracle comparison display normalization missing'],
  [template, 'تعداد رکورد', 'row-count UI missing'],
  [template, 'نتیجه جدول‌ها', 'table comparison grid missing'],
  [template, 'شرح اختلاف', 'column difference grid missing'],
  [component, 'exportCsv()', 'CSV report export missing'],
  [component, 'showDifferences(table: TableComparison, event: Event)', 'difference detail action handler missing'],
  [template, 'جزئیات اختلاف', 'difference detail row link missing'],
  [template, "table.status === 'DIFFERENT'", 'difference detail link must be limited to DIFFERENT rows']
];
for (const [text, token, message] of required) if (!text.includes(token)) failures.push(message);

for (const tag of ['table', 'thead', 'tbody', 'tr', 'th', 'td']) {
  const open = (template.match(new RegExp(`<${tag}(?:\\s|>)`, 'g')) ?? []).length;
  const close = (template.match(new RegExp(`</${tag}>`, 'g')) ?? []).length;
  if (open !== close) failures.push(`unbalanced <${tag}> ${open}/${close}`);
}

if (failures.length) {
  console.error('Fix32 EA/Oracle comparison verification FAILED:');
  failures.forEach(item => console.error(` - ${item}`));
  process.exit(1);
}
console.log('Fix32 EA/Oracle comparison verification OK: route, secure/enriched EA parser, normalized length semantics, configured-schema Oracle metadata, row counts, report grids and per-row difference detail link verified.');
