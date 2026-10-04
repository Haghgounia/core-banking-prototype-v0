import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'..');
const tsPath=path.join(root,'frontend','src','app','features','four-deposits','deposit-account-operations.component.ts');
const htmlPath=path.join(root,'frontend','src','app','features','four-deposits','deposit-account-operations.component.html');
const buildPath=path.join(root,'build-production.cmd');
const ts=fs.readFileSync(tsPath,'utf8');
const html=fs.readFileSync(htmlPath,'utf8');
const build=fs.readFileSync(buildPath,'utf8');
let pass=0,fail=0;
function chk(name,ok){if(ok){console.log(`PASS | ${name}`);pass++;}else{console.log(`FAIL | ${name}`);fail++;}}
const declarations=[...ts.matchAll(/\b(?:async\s+)?saveAttribute\s*\(/g)];
chk('saveAttribute has exactly one component implementation',declarations.length===1);
chk('canonical async saveAttribute implementation remains',/async\s+saveAttribute\s*\(\)\s*\{/.test(ts));
chk('obsolete saveAttribute-to-saveMaintenance alias removed',!ts.includes('saveAttribute(){return this.saveMaintenance()}'));
chk('saveMaintenance implementation remains',/async\s+saveMaintenance\s*\(/.test(ts));
chk('legacy Wave A template compatibility marker still resolves saveAttribute',html.includes('(click)="saveAttribute()"'));
chk('R10K verifier is wired into production build',build.includes('verify-dps2-account-operations-r10k-angular-duplicate-method-hotfix.mjs'));
console.log('------------------------------------------------------------');
console.log(`DPS2_ACCOUNT_OPERATIONS_R10K_PASS=${pass}`);
console.log(`DPS2_ACCOUNT_OPERATIONS_R10K_FAIL=${fail}`);
if(fail){process.exit(1)}
console.log('DPS2_ACCOUNT_OPERATIONS_R10K_STATIC_PASS');
