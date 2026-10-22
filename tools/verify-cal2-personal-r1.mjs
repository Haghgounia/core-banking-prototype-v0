#!/usr/bin/env node
import {existsSync, readFileSync} from 'node:fs';
import {join} from 'node:path';

const root = process.cwd();
const paths = [
  'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/domain/Calendar2PersonalModels.java',
  'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/application/Calendar2PersonalAlertPolicy.java',
  'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/application/Calendar2PersonalService.java',
  'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/oracle/Calendar2PersonalRepository.java',
  'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/web/Calendar2PersonalController.java',
  'backend/src/test/java/com/behsazan/corebanking/calendar2/personal/application/Calendar2PersonalAlertPolicyTest.java',
  'database/oracle/cal2/migrations/CAL2-PERSONAL-ITEMS-R1-OPT-IN.sql',
  'frontend/src/app/features/calendar2-reference/calendar2-personal.models.ts',
  'frontend/src/app/features/calendar2-reference/calendar2-personal.service.ts',
  'frontend/src/app/features/calendar2-reference/calendar2-month-view.component.ts',
  'frontend/src/app/features/calendar2-reference/calendar2-month-view.component.html',
  'frontend/src/app/layout/app-shell.component.ts',
  'frontend/src/app/layout/app-shell.component.html'
];
const errors=[];
for (const name of paths) if (!existsSync(join(root,name))) errors.push(`MISSING: ${name}`);
if (!errors.length) {
  const html = readFileSync(join(root,'frontend/src/app/features/calendar2-reference/calendar2-month-view.component.html'),'utf8');
  const shell = readFileSync(join(root,'frontend/src/app/layout/app-shell.component.html'),'utf8');
  const sql = readFileSync(join(root,'database/oracle/cal2/migrations/CAL2-PERSONAL-ITEMS-R1-OPT-IN.sql'),'utf8');
  const service = readFileSync(join(root,'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/application/Calendar2PersonalService.java'),'utf8');
  const repository = readFileSync(join(root,'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/oracle/Calendar2PersonalRepository.java'),'utf8');
  for (const token of ['savePersonalItem()', 'setPersonalCompleted(', 'selectedPersonalItems()', 'draftAdvance()', 'personalCount(']) {
    if (!html.includes(token)) errors.push(`FORM_MISSING: ${token}`);
  }
  for (const token of ['personalAlertCount()', 'acknowledgePersonalAlert(', 'completePersonalAlert(']) {
    if (!shell.includes(token)) errors.push(`BELL_MISSING: ${token}`);
  }
  if (!sql.includes('DEFINE APPLY_CHANGES = NO')) errors.push('DDL_MUST_DEFAULT_TO_PREVIEW');
  if (!sql.includes('REFERENCES CAL2.CANONICAL_DAY')) errors.push('MISSING_CANONICAL_DAY_FK');
  if (/\b(?:DROP|TRUNCATE|RENAME)\s+TABLE\b/i.test(sql)) errors.push('DESTRUCTIVE_DDL_FOUND');
  if (!service.includes('ZoneId.of(zoneId)') || !service.includes('Calendar2PersonalAlertPolicy.alertKind')) errors.push('SERVER_SIDE_POLICY_MISSING');
  if (!repository.includes('ACTOR_KEY = :actor')) errors.push('ACTOR_SCOPING_MISSING');
}
if (errors.length) {
  console.error('CAL2_PERSONAL_R1_VERIFY_FAIL\n'+errors.join('\n'));
  process.exitCode=1;
} else {
  console.log(`CAL2_PERSONAL_R1_VERIFY_PASS: ${paths.length} required files; server-side policy; month UI; bell; safe DDL`);
}
