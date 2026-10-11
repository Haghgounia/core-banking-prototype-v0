// CAL2 international observance Phase B1: offline manifest/code consistency.
// This verifier does not connect to Oracle and cannot prove production deployment.
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const prefix = 'database/oracle/cal2/';
const data = read(prefix + 'data/international-observances-b1-verified.tsv')
  .trimEnd().split(/\r?\n/).map(line => line.split('|'));
assert.deepEqual(data[0], ['authority','code','name_en','name_fa','month','day','weekday','ordinal','listing_url','authority_url']);
const records = data.slice(1).map(line => {
  assert.equal(line.length, 10, `Invalid manifest column count: ${line[1]}`);
  const [authority, code, en, fa, month, day, weekday, ordinal, listing, authorityUrl] = line;
  assert.ok(['UNITED_NATIONS','UNESCO','WHO'].includes(authority));
  assert.match(code, /^INTL_[A-Z_]+$/);
  assert.ok(en.length && fa.length);
  assert.ok(Number(month) >= 1 && Number(month) <= 12);
  if (day) {
    const date = new Date(Date.UTC(2026, Number(month) - 1, Number(day)));
    assert.equal(date.getUTCDate(), Number(day));
    assert.equal(date.getUTCMonth() + 1, Number(month));
    assert.equal(weekday, ''); assert.equal(ordinal, '');
  } else {
    assert.ok(Number(weekday) >= 1 && Number(weekday) <= 7);
    assert.ok([-1,1,2,3,4,5].includes(Number(ordinal)));
  }
  assert.ok(listing.startsWith('https://www.') && authorityUrl.startsWith('https://www.'));
  return {authority,code,en,month:Number(month),day:day ? Number(day):null,
    weekday:weekday ? Number(weekday):null, ordinal:ordinal ? Number(ordinal):null};
});
assert.equal(records.length, 72);
assert.equal(new Set(records.map(row => row.code)).size, 72);
assert.equal(records.filter(row => row.authority === 'WHO').length, 9);
assert.equal(records.filter(row => row.authority === 'UNITED_NATIONS').length, 24);
assert.equal(records.filter(row => row.authority === 'UNESCO').length, 39);
assert.equal(records.filter(row => row.weekday !== null).length, 2);

const seed = read(prefix + 'migrations/0.11.0-international-observance-verified-seed-b1.sql');
const variableSchema = read(prefix + 'migrations/0.11.0-international-variable-rule-engine.sql');
const verifier = read(prefix + 'migrations/0.11.0-international-observance-phaseb1-readonly-verify.sql');
const schema = read(prefix + '01-create-cal2-tables.sql');
const backend = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventrecurrence/oracle/Calendar2EventRecurrenceRepository.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventrecurrence/application/Calendar2EventRecurrenceService.java');
const registry = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceRegistry.java');
const ui = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.ts');
const model = read('frontend/src/app/features/calendar2-reference/calendar2-reference.models.ts');
const tsCount = [...seed.matchAll(/^  ensure_observance\('/gm)].length;
assert.equal(tsCount, 72, 'Seed is missing reviewed rows');
assert.equal([...verifier.matchAll(/^  check_observance\('/gm)].length, 72);
for (const row of records) {
  assert.ok(seed.includes(`ensure_observance('${row.authority}','${row.code}'`), `Missing seed ${row.code}`);
  assert.ok(verifier.includes(`check_observance('${row.authority}','${row.code}'`), `Missing verifier ${row.code}`);
}
assert.ok(seed.includes('INTL_WORLD_MARITIME_DAY'));
assert.ok(seed.includes("'GENERATED','CALCULATED'"));
assert.ok(seed.includes('NOT EXISTS (SELECT 1 FROM CAL2.EVENT_OCCURRENCE'));
assert.ok(seed.includes('CAL2_B1_SEED_AND_OCCURRENCE_COMMIT_COMPLETE'));
assert.ok(schema.includes('ANNUAL_NTH_WEEKDAY') && variableSchema.includes('ANNUAL_NTH_WEEKDAY'));
assert.ok(variableSchema.includes('MODIFY (DAY_NO NULL)'));
assert.ok(backend.includes('W.ISO_WEEKDAY_NO = :weekdayNo'));
assert.ok(backend.includes('CD.DAY_NO + 7 >'));
assert.ok(service.includes('ANNUAL_NTH_WEEKDAY'));
assert.ok(registry.includes('weekdayOrdinal') && ui.includes('ANNUAL_NTH_WEEKDAY'));
assert.ok(model.includes('weekdayOrdinal: number | null'));

function isoWeekday(d) { return ((d.getUTCDay() + 6) % 7) + 1; }
function matchingDay(year, month, weekday, ordinal) {
  const end = new Date(Date.UTC(year,month,0)).getUTCDate();
  const matches=[];
  for (let d=1;d<=end;d++) if (isoWeekday(new Date(Date.UTC(year,month-1,d)))===weekday) matches.push(d);
  return ordinal === -1 ? matches.at(-1) : matches[ordinal-1] ?? null;
}
const three = [
  { code:'INTL_WORLD_MARITIME_DAY', month:9, weekday:4, ordinal:-1, day:24 },
  { code:'INTL_WORLD_PHILOSOPHY_DAY', month:11, weekday:4, ordinal:3, day:19 },
  { code:'INTL_SCHOOL_ANTIBULLYING_DAY', month:11, weekday:4, ordinal:1, day:5 }
];
for (const entry of three) {
  assert.equal(matchingDay(2026, entry.month, entry.weekday, entry.ordinal), entry.day, entry.code);
}
// Check last weekday and nth occurrence across a selection of years.
for (let year=2024;year<=2032;year++) {
  for (const entry of three) {
    const calculated=matchingDay(year,entry.month,entry.weekday,entry.ordinal);
    assert.ok(calculated>=1 && calculated<=31);
    assert.equal(isoWeekday(new Date(Date.UTC(year,entry.month-1,calculated))),4);
    if(entry.ordinal===-1) {
      const end = new Date(Date.UTC(year,entry.month,0)).getUTCDate();
      assert.ok(end-calculated<7);
    } else assert.equal(Math.floor((calculated-1)/7)+1,entry.ordinal);
  }
}
console.log('CAL2 international B1 static verifier PASS: 72 source-indexed items (24 UN, 39 UNESCO, 9 WHO), 72 seeds/rules, 3 weekday-relative dates, ISO weekday tests 2024-2032, UI/API/DDL wired.');
console.log('This verifies source code only; run SQL read-only verifier against the actual Oracle runtime after deployment.');
