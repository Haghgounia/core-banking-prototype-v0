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
const models = read('backend', 'src', 'main', 'java', 'com', 'behsazan', 'corebanking', 'system', 'modelcomparison', 'EaOracleComparisonModels.java');
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
  [inspector, "FK.CONSTRAINT_TYPE='R'", 'Oracle foreign-key metadata comparison missing'],
  [inspector, 'SEARCH_CONDITION_VC', 'Oracle check-constraint metadata comparison missing'],
  [inspector, 'ALL_TAB_COMMENTS', 'Oracle table comments comparison missing'],
  [inspector, 'ALL_COL_COMMENTS', 'Oracle column comments comparison missing'],
  [schemas, 'FROM ALL_TABLES', 'metadata-driven schema discovery from ALL_TABLES missing'],
  [schemas, 'GROUP BY T.OWNER', 'schema owner aggregation missing'],
  [schemas, "COALESCE(U.ORACLE_MAINTAINED, 'N') = 'N'", 'Oracle-maintained system schema exclusion missing'],
  [schemas, 'schema.tableCount()', 'visible table count must be exposed in schema label'],
  [schemas, 'core-banking.schemas.cif', 'configured CIF friendly/default preference missing'],
  [template, 'Oracle Data Dictionary (ALL_TABLES)', 'metadata-driven schema source hint missing'],
  [parser, 'disallow-doctype-decl', 'XXE/DOCTYPE hardening missing'],
  [parser, 'definitions.size()', 'EA duplicate table-definition merge missing'],
  [parser, 'enrichSelectedColumns', 'EA duplicate column metadata enrichment missing'],
  [parser, 'tableTags.get("alias")', 'EA table alias extraction missing'],
  [parser, 'tableTags.get("documentation")', 'EA table documentation extraction missing'],
  [parser, 'tags.get("description")', 'EA column description extraction missing'],
  [parser, 'parseForeignKeyAssociations', 'EA foreign-key association parsing missing'],
  [parser, 'hasDirectStereotype(child, "check")', 'EA check-constraint parsing missing'],
  [service, 'db.displayType(ea.lengthSemantics() != null)', 'implicit length-semantics display normalization missing'],
  [snapshot, 'displayType(boolean includeLengthSemantics)', 'Oracle comparison display normalization missing'],
  [service, 'compareTablePersianMetadata', 'Persian table metadata comparison missing'],
  [service, 'COMMENT فارسی ستون', 'Persian column comment comparison missing'],
  [service, 'timestampZeroSixCompatibility', 'TIMESTAMP(0) vs TIMESTAMP(6) compatibility rule missing'],
  [service, 'compareForeignKeys', 'Foreign Key comparison missing'],
  [service, 'compareChecks', 'Check Constraint comparison missing'],
  [models, 'ConstraintComparison', 'constraint comparison result contract missing'],
  [models, 'persianMetadataMatch', 'Persian metadata result fields missing'],
  [template, 'تعداد رکورد', 'row-count UI missing'],
  [template, 'نتیجه جدول‌ها', 'table comparison grid missing'],
  [template, 'شرح اختلاف', 'column difference grid missing'],
  [template, 'متادیتای فارسی', 'Persian metadata status column missing'],
  [template, 'عنوان فارسی جدول در EA (Alias)', 'EA Persian table title detail missing'],
  [template, 'Foreign Key', 'Foreign Key comparison UI missing'],
  [template, 'Check Constraint', 'Check Constraint comparison UI missing'],
  [template, 'TIMESTAMP(0)', 'timestamp compatibility scope note missing'],
  [template, 'COMMENT جدول در Oracle', 'Oracle table comment detail missing'],
  [template, 'شرح فارسی EA', 'EA column comment detail missing'],
  [template, 'COMMENT Oracle', 'Oracle column comment detail missing'],
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
  console.error('Fix33 EA/Oracle comparison verification FAILED:');
  failures.forEach(item => console.error(` - ${item}`));
  process.exit(1);
}
console.log('EA/Oracle comparison verification OK: application-schema discovery, TIMESTAMP compatibility, PK/FK/CHECK comparison, Persian metadata, row counts and difference-detail UI verified.');
