import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const base = path.join(root, 'database', 'oracle', 'fee', 'cbi-1404');
const read = name => fs.readFileSync(path.join(base, name), 'utf8');
const manifest = JSON.parse(read('cbi_fee_1404_manifest.json'));
const clean = read('cbi_fee_1404_clean.csv');
const review = read('cbi_fee_1404_review.csv');
const sql = read('01-import-cbi-fee-1404.sql');
const verify = read('02-verify-cbi-fee-1404.sql');
const wrapper = read('00-install-cbi-fee-1404.sql');

const count = (text, regex) => (text.match(regex) ?? []).length;
const cleanLines = clean.trimEnd().split(/\r?\n/).length;
const reviewLines = review.trimEnd().split(/\r?\n/).length;
const checks = [
  [manifest.source_sha256 === 'f43842c539427292dd86ce51562b2dbe38367b2cb10642abb164135fc31c9588', 'source SHA256 must match reviewed workbook'],
  [manifest.physical_source_rows === 239, `physical source rows must be 239, got ${manifest.physical_source_rows}`],
  [manifest.logical_tariffs === 229, `logical tariffs must be 229, got ${manifest.logical_tariffs}`],
  [manifest.source_sections === 17, `source sections must be 17, got ${manifest.source_sections}`],
  [manifest.consolidated_continuation_rows === 10, '10 bracket continuation rows must be consolidated'],
  [manifest.official_tiers === 15, 'official appraisal tiers must be 15'],
  [manifest.review_conflict === 3, `numeric source conflicts must be 3, got ${manifest.review_conflict}`],
  [manifest.external_rule === 20, `external rules must be 20, got ${manifest.external_rule}`],
  [cleanLines === 230, `clean CSV must have header + 229 rows, got ${cleanLines}`],
  [reviewLines === 28, `review CSV must have header + 27 rows, got ${reviewLines}`],
  [count(sql, /^MERGE INTO FEE_FEATURE t/gm) === 17, 'SQL must upsert 17 source-section features'],
  [count(sql, /^MERGE INTO FEE_DEFINITION t/gm) === 229, 'SQL must upsert 229 fee definitions'],
  [count(sql, /^MERGE INTO FEE_DEFINITION_VERSION t/gm) === 229, 'SQL must upsert 229 definition versions'],
  [count(sql, /^MERGE INTO FEE_CALCULATION_RULE t/gm) === 233, 'SQL must upsert 233 calculation rules including enriched existing rules'],
  [count(sql, /^MERGE INTO FEE_CALCULATION_TIER t/gm) === 15, 'SQL must upsert 15 appraisal tiers'],
  [count(sql, /^MERGE INTO FEE_INPUT_DEFINITION t/gm) === 10, 'SQL must upsert 10 appraisal inputs'],
  [count(sql, /^MERGE INTO FEE_RULE_COMPONENT t/gm) === 25, 'SQL must upsert 25 appraisal rule components'],
  [!/(^|\n)\s*(DELETE|TRUNCATE|DROP)\s+/i.test(sql), 'official import must not destructively delete/truncate/drop data'],
  [!/(^|\n)\s*COMMIT\s*;/im.test(sql), 'data script must not commit before verification'],
  [sql.includes("t.RATE_VALUE=0.001") && sql.includes("t.RULE_CODE='CREDIT_STAGE2'"), 'CREDIT_STAGE2 must be corrected to 0.001'],
  [sql.includes("'CBI1404_COL_5_4_01'") && sql.includes('نیازمند بازبینی: مبلغ ساختاری 75,000 ریال است ولی متن نوع خدمت 7,500 ریال'), 'row 45 conflict must be preserved as review data'],
  [clean.includes('0.5,0.005') && clean.includes('0.0001,0.000001'), 'percentage normalization must be visible in clean CSV'],
  [verify.includes("assert_eq('CBI1404 active definitions', v, 229)") && verify.includes("assert_eq('CBI1404 appraisal tiers', v, 15)"), 'DB verification must assert definition/tier counts'],
  [wrapper.includes('@@01-import-cbi-fee-1404.sql') && wrapper.includes('@@02-verify-cbi-fee-1404.sql'), 'installer wrapper must run import then verification'],
  [wrapper.indexOf('@@01-import-cbi-fee-1404.sql') < wrapper.indexOf('@@02-verify-cbi-fee-1404.sql') && wrapper.indexOf('@@02-verify-cbi-fee-1404.sql') < wrapper.lastIndexOf('COMMIT;'), 'wrapper must verify before commit'],
  [fs.existsSync(path.join(root, 'database', 'oracle', 'fee', 'install-cbi-fee-1404.sql')), 'top-level CBI import entrypoint is missing'],
];

const failed = checks.filter(([ok]) => !ok).map(([, message]) => message);
if (failed.length) {
  console.error('CBI Fee 1404 clean import verification FAILED:');
  for (const message of failed) console.error(`- ${message}`);
  process.exit(1);
}
console.log('CBI Fee 1404 clean import verification OK: 239 source rows -> 229 logical definitions, 15 tiers, 3 conflicts quarantined, no destructive SQL.');
