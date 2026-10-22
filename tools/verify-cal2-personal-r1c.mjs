#!/usr/bin/env node
/** CAL2 R1C semantic ENTRIES contract verifier. Read-only. No Oracle access. */
import {readFileSync, existsSync} from 'node:fs';
import {join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
const root=resolve(fileURLToPath(new URL('..',import.meta.url)));
const paths={
  model:'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/domain/Calendar2PersonalModels.java',
  service:'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/application/Calendar2PersonalService.java',
  repo:'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/oracle/Calendar2PersonalRepository.java',
  controller:'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/web/Calendar2PersonalController.java',
  policy:'backend/src/main/java/com/behsazan/corebanking/calendar2/personal/application/Calendar2PersonalAlertPolicy.java',
  policyTest:'backend/src/test/java/com/behsazan/corebanking/calendar2/personal/application/Calendar2PersonalAlertPolicyTest.java',
  serviceTest:'backend/src/test/java/com/behsazan/corebanking/calendar2/personal/application/Calendar2PersonalServiceR1BTest.java',
  tsModel:'frontend/src/app/features/calendar2-reference/calendar2-personal.models.ts',
  tsApi:'frontend/src/app/features/calendar2-reference/calendar2-personal.service.ts',
  tsView:'frontend/src/app/features/calendar2-reference/calendar2-month-view.component.ts',
  htmlView:'frontend/src/app/features/calendar2-reference/calendar2-month-view.component.html',
  tsShell:'frontend/src/app/layout/app-shell.component.ts',
  htmlShell:'frontend/src/app/layout/app-shell.component.html',
  sqlRename:'database/oracle/cal2/migrations/CAL2-PERSONAL-ENTRIES-R1C-RENAME-OPT-IN.sql',
  sqlVerify:'database/oracle/cal2/migrations/CAL2-PERSONAL-ENTRIES-R1C-VERIFY-READONLY.sql',
  sqlFresh:'database/oracle/cal2/migrations/CAL2-PERSONAL-ENTRIES-R1C-FRESH-OPT-IN.sql',
  sqlGrants:'database/oracle/cal2/migrations/CAL2-PERSONAL-ENTRIES-R1C-APP-GRANTS-OPT-IN.sql',
  build:'build-production.cmd'
};
const errors=[];
const files={};
for(const [key,path] of Object.entries(paths)) {
  if(!existsSync(join(root,path))) errors.push(`MISSING ${path}`);
  else files[key]=readFileSync(join(root,path),'utf8');
}
function need(file,needle,message) {
  if(files[file] && !(typeof needle==='string'?files[file].includes(needle):needle.test(files[file])))
    errors.push(`${file}: ${message}`);
}
function forbid(file,needle,message) {
  if(files[file] && needle.test(files[file])) errors.push(`${file}: ${message}`);
}
need('model','record Entry(', 'domain record Entry');
need('model','long userCalendarEntryId','numeric primary key in record');
need('model','String entryType','semantic type in JSON model');
need('service',/public List<Entry> entries\(/,'service lists entries');
need('service','repository.monthEntries(', 'repository operation renaming');
need('repo','".USER_CALENDAR_ENTRIES"','target table');
need('repo','".SEQ_CAL2_USER_CALENDAR_ENTRIES"','target sequence');
need('repo','I.USER_CALENDAR_ENTRY_ID','numeric id SQL');
need('repo','I.ENTRY_TYPE','semantic type SQL');
need('repo','rs.getLong("USER_CALENDAR_ENTRY_ID")','long mapping');
need('repo','rs.getString("ENTRY_TYPE")','type mapping');
need('repo','WHERE USER_CALENDAR_ENTRY_ID = :id AND ACTOR_KEY = :actor','actor-scope mutations');
need('repo','I.ACTOR_KEY = :actor','actor-scope reads');
need('controller','@GetMapping("/entries")','list endpoint');
need('controller','@PostMapping("/entries")','create endpoint');
need('controller','@PutMapping("/entries/{id}")','update endpoint');
need('controller','@DeleteMapping("/entries/{id}")','delete endpoint');
need('controller','@PatchMapping("/entries/{id}/completion")','completion endpoint');
need('controller','@GetMapping("/alerts")','notification contract unchanged');
need('controller','@PostMapping("/alerts/{id}/acknowledge")','acknowledgment contract unchanged');
need('tsModel','readonly userCalendarEntryId: number','TS numeric identifier');
need('tsModel','readonly entryType: Calendar2PersonalEntryType','TS semantic type');
need('tsApi','`${this.base}/entries`','API path');
need('tsApi','`${this.base}/entries/${encodeURIComponent(id)}`','API instance paths');
need('tsView','personal.entries(','list UI API');
need('tsView','item.userCalendarEntryId','UI identifier references');
need('tsView','item.entryType','UI type references');
need('htmlView','track item.userCalendarEntryId','UI list tracking');
need('htmlView','item.entryType','UI visual type');
need('tsShell','a.userCalendarEntryId','shell alert signature');
need('htmlShell','track alert.userCalendarEntryId','shell alert tracking');
need('sqlRename','DEFINE APPLY_CHANGES = NO','default preview');
need('sqlRename','RENAME USER_CALENDAR_ITEMS TO USER_CALENDAR_ENTRIES','data-preserving table rename');
need('sqlRename','RENAME COLUMN USER_CALENDAR_ITEM_ID TO USER_CALENDAR_ENTRY_ID','key rename');
need('sqlRename','RENAME COLUMN ITEM_TYPE TO ENTRY_TYPE','type rename');
need('sqlRename','RENAME SEQ_CAL2_USER_CALENDAR_ITEMS TO SEQ_CAL2_USER_CALENDAR_ENTRIES','preserve sequence high-water');
need('sqlRename','DEFAULT CAL2.SEQ_CAL2_USER_CALENDAR_ENTRIES.NEXTVAL','default rebind');
need('sqlRename','R1B_ROWS_BEFORE=','before count');
need('sqlRename','R1C_ROWS_AFTER=','after count');
need('sqlRename','DB_OBJECT_DEPENDENCIES=','dependency safety gate');
need('sqlVerify','CAL2_PERSONAL_R1C_READONLY_VERIFY_PASS','read-only contract');
need('sqlVerify','DATA_DEFAULT','manual default verification');
need('sqlFresh','DEFINE APPLY_CHANGES = NO','fresh setup opt-in');
need('sqlFresh','USER_CALENDAR_ENTRY_ID NUMBER(12,0)','fresh numeric key');
need('sqlFresh','SEQ_CAL2_USER_CALENDAR_ENTRIES.NEXTVAL','fresh sequence');
need('sqlFresh','CAL2.USER_CALENDAR_ENTRIES','fresh name');
need('sqlGrants','DEFINE APPLY_CHANGES = NO','grants opt-in');
need('build','node "%ROOT%tools\\verify-cal2-personal-r1c.mjs" || exit /b 1','pre-build gate');
for(const key of ['model','service','repo','controller','policy','policyTest','serviceTest','tsModel','tsApi','tsView','htmlView','tsShell','htmlShell']) {
  forbid(key,/USER_CALENDAR_ITEMS|USER_CALENDAR_ITEM_ID|SEQ_CAL2_USER_CALENDAR_ITEMS|\bITEM_TYPE\b|userCalendarItemId|itemType|Calendar2PersonalItem|\/items(?:\/|\b)/, 'old ITEM contract remains');
}
for(const key of ['sqlRename','sqlVerify']) {
  forbid(key,/^\s*(?:EXECUTE IMMEDIATE\s+['\"](?:DROP|TRUNCATE|DELETE)|(?:DROP TABLE|DROP SEQUENCE|TRUNCATE TABLE|DELETE FROM|UPDATE CAL2\.USER_CALENDAR_))/im, 'destructive/DML operation prohibited');
}
if(errors.length){
 console.error('CAL2_PERSONAL_R1C_VERIFY_FAIL\n'+errors.map(e=>' - '+e).join('\n'));
 process.exitCode=1;
}else{
 console.log(`CAL2_PERSONAL_R1C_VERIFY_PASS: ${Object.keys(paths).length} files; ENTRIES + ENTRY_TYPE + NUMBER(12) + SEQUENCE + API + UI + opt-in read-only gates`);
}
