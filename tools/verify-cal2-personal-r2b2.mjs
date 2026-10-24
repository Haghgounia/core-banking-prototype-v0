#!/usr/bin/env node
/** Static R2-B2 source and Oracle contract guard. No database calls, no file mutation. */
import {readFileSync, existsSync} from 'node:fs';
import {join,resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
const root=resolve(fileURLToPath(new URL('..',import.meta.url)));
const files={
  repository:'backend/src/main/java/com/behsazan/corebanking/calendar2/personalv2/PersonalV2Repository.java',
  service:'backend/src/main/java/com/behsazan/corebanking/calendar2/personalv2/PersonalV2Service.java',
  model:'backend/src/main/java/com/behsazan/corebanking/calendar2/personalv2/PersonalV2Models.java',
  controller:'backend/src/main/java/com/behsazan/corebanking/calendar2/personalv2/PersonalV2Controller.java',
  scheduler:'backend/src/main/java/com/behsazan/corebanking/calendar2/personalv2/PersonalV2Scheduler.java',
  oldController:'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/web/Calendar2PersonalController.java',
  frontendModels:'frontend/src/app/features/calendar2-reference/calendar2-personal.models.ts',
  frontendService:'frontend/src/app/features/calendar2-reference/calendar2-personal.service.ts',
  frontendView:'frontend/src/app/features/calendar2-reference/calendar2-month-view.component.ts',
  frontendShell:'frontend/src/app/layout/app-shell.component.ts',
  frontendShellHtml:'frontend/src/app/layout/app-shell.component.html',
  config:'backend/src/main/resources/application.yml',
  sql:'database/oracle/cal2/verification/CAL2-R2B2-CONTRACT-VERIFY-READONLY.sql'
};
let bad=0;
function fail(k,msg){console.error('FAIL',k,msg);bad++;}
const src={};
for(const [k,p] of Object.entries(files)) {
  if(!existsSync(join(root,p)))fail(k,`missing ${p}`);
  else src[k]=readFileSync(join(root,p),'utf8');
}
function need(k,part){if(src[k]&&!src[k].includes(part))fail(k,`missing ${part}`)}
function reject(k,rx,msg){if(src[k]&&rx.test(src[k]))fail(k,msg)}
for(const t of ['ENTRIES','RECURRENCE_RULES','OCCURRENCES','REMINDER_RULES','ALERTS','REQUEST_KEYS'])need('repository',`t("${t}")`);
for(const t of ['USER_CALENDAR_ENTRIES','USER_CALENDAR_RECURRENCE_RULES','USER_CALENDAR_OCCURRENCES','USER_CALENDAR_REMINDER_RULES','USER_CALENDAR_ALERTS','USER_CALENDAR_REQUEST_KEYS'])need('sql',t);
for(const s of ['USER_CALENDAR_RECURRENCE_RULE_ID','USER_CALENDAR_REMINDER_RULE_ID','ORIGINAL_SLOT_KEY','OWNER_ISSUER','OWNER_SUBJECT_ID','PLANNED_AT_UTC','NEXT_ELIGIBLE_AT_UTC','REQUEST_SHA256'])need('repository',s);
need('repository','Types.CLOB');
need('service','MessageDigest.getInstance("SHA-256")');
need('service','db.claimIdempotency(');
need('service','db.cancelPendingAlertsForEntry(');
need('service','db.releaseEligibleAlerts(');
need('service','precondition(ifMatch)');
need('service','owner(Principal principal)');
need('service','"LAST_DAY_IF_INVALID"');
need('controller','@RequestMapping("/api/v2/calendar2/personal")');
need('controller','@PostMapping("/alerts/{id}/snooze")');
need('controller','@PatchMapping("/occurrences/{id}/completion")');
need('scheduler','@Scheduled(');
need('scheduler','scheduler-enabled');
need('oldController','legacy-v1-enabled');
need('frontendService',"'/api/v2/calendar2/personal'");
need('frontendService','Idempotency-Key');
need('frontendService','If-Match');
need('frontendService','snooze(alertId');
need('frontendModels','readonly alertId: string');
need('frontendModels','readonly occurrenceId: string');
need('frontendModels','readonly userCalendarEntryId: string');
need('frontendView',"startPersonalEntry(kind");
need('frontendView','draftRecurrence');
need('frontendShell','alert.alertId');
need('frontendShellHtml','track alert.alertId');
need('config','CAL2_PERSONAL_R2B2_DEMO_ENABLED:false');
need('sql','CAL2_R2B2_READONLY_CONTRACT_PASS');
reject('repository',/\b(ACTOR_KEY|ADVANCE_DAYS\s*=\s*:advance|LAST_ALERT_ACK_DATE|FIRST_ADVANCE_ACK)\b/,'legacy R1C data model');
reject('repository',/(?:UPDATE|INSERT INTO|DELETE FROM|MERGE INTO)\s+(?:%s\.)?(?:EVENT_OCCURRENCE|EVENT_RECURRENCE_RULE|BUSINESS_CALENDAR|CALENDAR_DATE)/i,'official or business calendar DML not permitted');
reject('sql',/\b(CREATE TABLE|ALTER TABLE|DROP TABLE|INSERT INTO|UPDATE\s+CAL2|DELETE FROM)\b/i,'read-only check must remain read-only');
if(bad){console.error('CAL2_R2B2_STATIC_VERIFY_FAIL',bad);process.exitCode=1;}
else console.log('CAL2_R2B2_STATIC_VERIFY_PASS: 13 source/config/SQL files; no Oracle writes');
