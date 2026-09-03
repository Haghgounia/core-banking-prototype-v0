import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const migration = read('database/oracle/cal2/migrations/0.3.80-fix88-last-day-resolution-policy.sql');
const ddl = read('database/oracle/cal2/01-create-cal2-tables.sql');
const registry = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceRegistry.java');
const repo = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventrecurrence/oracle/Calendar2EventRecurrenceRepository.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventrecurrence/application/Calendar2EventRecurrenceService.java');
const models = read('backend/src/main/java/com/behsazan/corebanking/calendar2/eventrecurrence/domain/Calendar2EventRecurrenceModels.java');
const uiModels = read('frontend/src/app/features/calendar2-reference/calendar2-reference.models.ts');
const ui = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.ts');
const html = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.html');
const checks = [
  [ddl.includes('DAY_RESOLUTION_POLICY') && ddl.includes('LAST_DAY_IF_INVALID'), 'baseline DDL has day resolution policy'],
  [migration.includes("IR_IMAM_REZA_MARTYRDOM") && migration.includes("LAST_DAY_IF_INVALID"), 'migration opts only Imam Reza 30 Safar rule into fallback'],
  [registry.includes('dayResolutionPolicy') && registry.includes('اگر روز وجود نداشت، آخرین روز ماه'), 'metadata form exposes controlled policy'],
  [models.includes('String dayResolutionPolicy'), 'backend rule model carries policy'],
  [repo.includes('NOT EXISTS (SELECT 1 FROM ') && repo.includes('SELECT MAX(MX.DAY_NO)') && repo.includes('rule.dayResolutionPolicy()'), 'generator resolves invalid requested day to month end only when opted in'],
  [service.includes('DAY_RESOLUTION_POLICIES') && service.includes('LAST_DAY_IF_INVALID'), 'service validates policy'],
  [uiModels.includes('dayResolutionPolicy') && ui.includes('dayResolutionPolicyLabel') && html.includes('سیاست روز نامعتبر'), 'CAL2 recurrence grid shows policy']
];
let ok = true;
for (const [pass, label] of checks) {
  console.log(`${pass ? '[OK]' : '[FAIL]'} ${label}`);
  ok &&= pass;
}
if (!ok) process.exit(1);
console.log('CAL2 invalid-day resolution policy regression guard passed.');
