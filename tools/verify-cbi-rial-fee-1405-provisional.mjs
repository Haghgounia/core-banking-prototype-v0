import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const dir = path.join(root, 'database', 'oracle', 'fee', 'cbi-rial-1405-provisional');
const req = [
  '00-install-cbi-rial-fee-1405-provisional.sql',
  '01-import-cbi-rial-fee-1405-provisional.sql',
  '02-verify-cbi-rial-fee-1405-provisional.sql',
  '03-finalize-official-reference-template.sql',
  '04-reconcile-cbi-rial-fee-1405-provisional.sql',
  '04-reconcile-cbi-rial-fee-1405-provisional-core.sql',
  '04-reconcile-cbi-rial-fee-1405-provisional-enforced.sql',
  '05-diagnose-cbi-rial-fee-1405-state.sql',
  'cbi_rial_fee_1405_provisional_definitions.csv',
  'cbi_rial_fee_1405_provisional_components.csv',
  'cbi_rial_fee_1405_provisional_delta.csv',
  'cbi_rial_fee_1405_provisional_manifest.json',
  'README.md',
];
for (const f of req) {
  const p = path.join(dir, f);
  if (!fs.existsSync(p)) throw new Error(`Missing FIX98 artifact: ${f}`);
}
const manifest = JSON.parse(fs.readFileSync(path.join(dir, 'cbi_rial_fee_1405_provisional_manifest.json'), 'utf8'));
const checks = [
  ['new_tariffs', 152],
  ['source_components', 180],
  ['old_rial_versions_to_archive', 156],
  ['old_electronic_versions_retained', 73],
];
for (const [k,v] of checks) if (manifest[k] !== v) throw new Error(`${k}: ${manifest[k]} != ${v}`);
if (manifest.source_status !== 'PROVISIONAL') throw new Error('source must remain PROVISIONAL until official cover letter is supplied');
if (manifest.classification_code !== 'CBI_1405_RIAL_PROVISIONAL') throw new Error('classification mismatch');
const sql = fs.readFileSync(path.join(dir, '01-import-cbi-rial-fee-1405-provisional.sql'), 'utf8');
const count = (token) => sql.split(token).length - 1;
for (const [token, expected] of [
  ['MERGE INTO FEE_DEFINITION t',152],
  ['MERGE INTO FEE_DEFINITION_VERSION t',152],
  ['MERGE INTO FEE_CALCULATION_RULE t',152],
  ['MERGE INTO FEE_RULE_COMPONENT t',180],
  ['MERGE INTO FEE_CALCULATION_TIER t',15],
  ['MERGE INTO FEE_FEATURE t',9],
]) {
  const actual = count(token);
  if (actual !== expected) throw new Error(`${token}: ${actual} != ${expected}`);
}
if (!sql.includes("STATUS_CODE='SUPERSEDED'")) throw new Error('prior version archival guard missing');
if (!sql.includes("SOURCE_CODE='CBI_FEE_1404_04_35500'")) throw new Error('old regulatory source scope missing');
const archiveBlock = sql.split('Closing only prior non-electronic rial fee definition versions ...')[1]?.split('Upserting 9 source section features')[0] ?? '';
for (const code of ['CBI_ELECTRONIC_SERVICE_FEE_GROUP','CBI_ELECTRONIC_LC_RIAL_FEE_GROUP','CBI_ELECTRONIC_GUARANTEE_RIAL_FEE_GROUP','CBI_ELECTRONIC_BILL_FEE_GROUP']) {
  if (archiveBlock.includes(code)) throw new Error(`electronic fee group must not be archived: ${code}`);
}
const verify = fs.readFileSync(path.join(dir, '02-verify-cbi-rial-fee-1405-provisional.sql'), 'utf8');
for (const token of ['archived prior non-electronic rial versions','retained prior electronic versions','structured appraisal tiers','source calculation components']) {
  if (!verify.includes(token)) throw new Error(`verification assertion missing: ${token}`);
}


const installer = fs.readFileSync(path.join(dir, '00-install-cbi-rial-fee-1405-provisional.sql'), 'utf8');
const enforcedName = '@04-reconcile-cbi-rial-fee-1405-provisional-enforced.sql';
if (!installer.includes(enforcedName)) {
  throw new Error('installer must run enforced source-to-Oracle reconciliation before COMMIT');
}
if (installer.indexOf(enforcedName) > installer.indexOf('COMMIT;')) {
  throw new Error('enforced reconciliation must run before COMMIT');
}
if (installer.includes('@04-reconcile-cbi-rial-fee-1405-provisional.sql')) {
  throw new Error('installer must not call transaction-neutral standalone reconciliation wrapper');
}

const reconcile = fs.readFileSync(path.join(dir, '04-reconcile-cbi-rial-fee-1405-provisional.sql'), 'utf8');
const core = fs.readFileSync(path.join(dir, '04-reconcile-cbi-rial-fee-1405-provisional-core.sql'), 'utf8');
const enforced = fs.readFileSync(path.join(dir, '04-reconcile-cbi-rial-fee-1405-provisional-enforced.sql'), 'utf8');
const diagnose = fs.readFileSync(path.join(dir, '05-diagnose-cbi-rial-fee-1405-state.sql'), 'utf8');

// FIX103: standalone diagnostic/reconciliation scripts are transaction-neutral.
if (!reconcile.includes('WHENEVER SQLERROR CONTINUE NONE;')) throw new Error('standalone reconciliation must continue without transaction action');
if (/^\s*(ROLLBACK|COMMIT)\b/im.test(reconcile) || /WHENEVER SQLERROR[^\n]*ROLLBACK/i.test(reconcile)) throw new Error('standalone reconciliation must not commit/rollback caller work');
if (!reconcile.includes('@04-reconcile-cbi-rial-fee-1405-provisional-core.sql')) throw new Error('standalone reconciliation must delegate to core contract');
if (!enforced.includes('WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;')) throw new Error('enforced reconciliation must rollback installer transaction on mismatch');
if (!enforced.includes('@04-reconcile-cbi-rial-fee-1405-provisional-core.sql')) throw new Error('enforced reconciliation must delegate to core contract');
for (const token of ['SERVICE_NAME','CURRENT_SCHEMA','CBI_RIAL_FEE_1405_PROVISIONAL','CBI_1405_RIAL_PROVISIONAL','CBI_FEE_1404_04_35500']) {
  if (!diagnose.includes(token)) throw new Error(`diagnostic contract missing: ${token}`);
}
if (/^\s*(ROLLBACK|COMMIT)\b/im.test(diagnose) || /WHENEVER SQLERROR[^\n]*ROLLBACK/i.test(diagnose)) throw new Error('diagnostic script must be transaction-neutral');

const callCount = (name) => (core.match(new RegExp(`^\\s{2}${name}\\(`, 'gm')) ?? []).length;
for (const [name, expected] of [
  ['check_tariff', 152],
  ['check_component', 180],
  ['check_input', 66],
  ['check_tier', 15],
]) {
  const actual = callCount(name);
  if (actual !== expected) throw new Error(`${name} calls: ${actual} != ${expected}`);
}
for (const token of [
  manifest.xlsx_sha256,
  manifest.pdf_sha256,
  'CONFIG_HASH',
  'SOURCE_COMPONENT_COUNT',
  'INPUT_DEFINITIONS',
  'RAISE_APPLICATION_ERROR(-20260',
  'source-to-Oracle reconciliation OK',
]) {
  if (!core.includes(token)) throw new Error(`reconciliation contract missing: ${token}`);
}

// FIX102 regression guard: SQL/PLSQL string literals generated for reconciliation
// must remain quoted. Python adjacent string literals previously stripped these quotes.
for (const token of [
  "c.REFERENCE_CODE LIKE 'SRC:%'",
  "p_fee_code||'#'||p_sequence",
  "p_fee_code||'#'||p_input_code",
  "p_fee_code||'#TIER'||p_tier_no",
]) {
  if (!core.includes(token)) throw new Error(`reconciliation SQL literal missing or unquoted: ${token}`);
}
for (const bad of [
  'c.REFERENCE_CODE LIKE SRC:%',
  'p_fee_code||#||p_sequence',
  'p_fee_code||#||p_input_code',
  'p_fee_code||#TIER||p_tier_no',
]) {
  if (core.includes(bad)) throw new Error(`invalid unquoted reconciliation SQL token: ${bad}`);
}

// FIX104 regression guard: bind reconciliation buffers to Oracle column types.
// Fixed byte-sized VARCHAR2 locals can overflow on multibyte Persian text.
for (const token of [
  'a_name FEE.FEE_DEFINITION.NAME_FA%TYPE',
  'a_text FEE.FEE_RULE_COMPONENT.CONSTANT_TEXT%TYPE',
  'a_desc FEE.FEE_RULE_COMPONENT.DESCRIPTION%TYPE',
  'a_name FEE.FEE_INPUT_DEFINITION.NAME_FA%TYPE',
  'a_name FEE.FEE_CALCULATION_TIER.TIER_NAME_FA%TYPE',
]) {
  if (!core.includes(token)) throw new Error(`reconciliation anchored buffer missing: ${token}`);
}
for (const bad of [
  'a_name VARCHAR2(250); a_feature',
  'a_text VARCHAR2(500)',
  'a_desc VARCHAR2(1000)',
  'a_name VARCHAR2(200); a_lower',
]) {
  if (core.includes(bad)) throw new Error(`fixed-size reconciliation buffer must not return: ${bad}`);
}

const generator = fs.readFileSync(path.join(root, 'tools', 'generate-cbi-rial-fee-1405-provisional.py'), 'utf8');
for (const token of [
  'WHENEVER SQLERROR CONTINUE NONE;',
  '04-reconcile-cbi-rial-fee-1405-provisional-enforced.sql',
  '04-reconcile-cbi-rial-fee-1405-provisional-core.sql',
  '05-diagnose-cbi-rial-fee-1405-state.sql',
]) {
  if (!generator.includes(token)) throw new Error(`generator transaction-safety guard missing: ${token}`);
}
for (const token of [
  "REFERENCE_CODE LIKE 'SRC:%'",
  "p_fee_code||'#'||p_sequence",
  "p_fee_code||'#'||p_input_code",
  "p_fee_code||'#TIER'||p_tier_no",
]) {
  if (!generator.includes(token)) throw new Error(`generator SQL literal guard missing: ${token}`);
}
for (const token of [
  'FEE.FEE_DEFINITION.NAME_FA%TYPE',
  'FEE.FEE_RULE_COMPONENT.CONSTANT_TEXT%TYPE',
  'FEE.FEE_INPUT_DEFINITION.NAME_FA%TYPE',
  'FEE.FEE_CALCULATION_TIER.TIER_NAME_FA%TYPE',
]) {
  if (!generator.includes(token)) throw new Error(`generator anchored-buffer guard missing: ${token}`);
}

console.log('verify-cbi-rial-fee-1405-provisional: OK');
