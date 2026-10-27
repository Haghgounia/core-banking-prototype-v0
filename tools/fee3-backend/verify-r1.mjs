import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'../..');
const base=path.join(root,'backend/src/main/java/com/behsazan/corebanking/fee3');
const paths={
  dto:'domain/Fee3Models.java',controller:'web/Fee3Controller.java',service:'application/Fee3Service.java',
  repo:'oracle/Fee3Repository.java',policy:'application/Fee3DraftPolicy.java',error:'application/Fee3Problem.java',
  advice:'web/Fee3ProblemAdvice.java'
};
const content=Object.fromEntries(Object.entries(paths).map(([k,p])=>[k,fs.readFileSync(path.join(base,p),'utf8')]));
const checks=[];
function requireText(name,where,needle){checks.push([name,where.includes(needle)]);}
function exclude(name,where,needle){checks.push([name,!where.includes(needle)]);}
const merged=Object.values(content).join('\n');
for(const [k,p] of Object.entries(paths))checks.push(['file '+k,fs.existsSync(path.join(base,p))]);
for(const route of ['/readiness','/items','/items/{code}','/references/operations','/references/formulas',
  '/references/sources','/references/codes/{group}','/drafts','/drafts/{itemId}/{versionId}']){
    requireText('route '+route,content.controller,'"'+route+'"');
}
for(const t of ['FCE_AUDIT_EVENT','FCE_CALC_ITEM','FCE_CALC_RULE','FCE_CHANGE_REQUEST','FCE_REF_CODE',
 'FCE_REF_FORMULA','FCE_REF_OPERATION','FCE_REF_SOURCE','FCE_TARIFF_VERSION','FCE_TEST_CASE','FCE_VERSION_SCOPE']){
    requireText('schema table '+t,content.repo,'"'+t+'"');
}
requireText('seed write restriction',content.service,'F3-UAT-[0-9]+');
requireText('draft status required',content.service,'"DRAFT"');
requireText('no actor from HTTP',content.service,'FEE3_UAT_ACTOR:FEE3_UAT');
requireText('authentication UAT token',content.service,'MessageDigest.isEqual');
requireText('audit inserted',content.repo,'UAT_DRAFT_CREATED');
requireText('update audit',content.repo,'UAT_DRAFT_UPDATED');
requireText('pessimistic locking',content.repo,'FOR UPDATE');
requireText('concurrency control',content.repo,'RECORD_VERSION=:expected');
requireText('no delete API',content.controller,'@PutMapping');
exclude('no delete endpoint',content.controller,'@DeleteMapping');
exclude('no publish endpoint',content.controller,'/publish');
exclude('no activate endpoint',content.controller,'/activate');
exclude('no posting integration',merged,'PostJournal');
requireText('server date for tariff',content.repo,"SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran'");
requireText('date floor',content.policy,'isBefore(bankToday)');
requireText('bank default disabled',content.service,'FEE3_DRAFT_WRITES_ENABLED:false');
requireText('UAT token default blank',content.service,'FEE3_UAT_WRITE_TOKEN:');
requireText('schema FEE3',content.repo,'FEE3_SCHEMA:FEE3');
requireText('no seed editing',content.service,'FEE3_SEED_WRITE_DENIED');
requireText('no cross-domain product selection',content.policy,'productScopeAllowed(item)');
requireText('source verification CBI',content.service,'CBI_MANDATORY');
requireText('operation reference validation',content.service,'activeOperation(');
requireText('rule formula reference validation',content.service,'activeFormula(');
requireText('UTC date bug absent',content.repo,"SYSTIMESTAMP AT TIME ZONE 'Asia/Tehran'");
requireText('transactional writes',content.service,'@Transactional\n    public DraftReceipt');
const failed=checks.filter(x=>!x[1]);
for(const [name,ok] of checks) console.log(`${ok?'PASS':'FAIL'} | ${name}`);
console.log(`FEE3_BE_R1_STATIC_PASS=${checks.length-failed.length} FAIL=${failed.length}`);
if(failed.length)process.exit(1);
