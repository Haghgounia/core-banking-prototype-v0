/** Non-invasive source-level inventory for HF7-C (not an E2E or runtime proof).
 * Scan direct Java references outside productbuilder without touching Oracle.
 * A missing text reference does NOT prove that a dynamic/inherited integration is absent.
 */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const backend=path.join(root,'backend/src/main/java/com/behsazan/corebanking');
const gov=fs.readFileSync(path.join(backend,'productbuilder/application/ProductRuleGovernanceService.java'),'utf8');
const controls=[...gov.matchAll(/\bc\("([A-Z_]+)",\s*"[^"]+"/g)].map(m=>m[1]);
if(controls.length!==23 || new Set(controls).size!==23) throw Error('Governance catalog unexpectedly changed; review coverage before proceeding');
const java=[];
function visit(directory){for(const entry of fs.readdirSync(directory,{withFileTypes:true})){
 const filename=path.join(directory,entry.name);
 if(entry.isDirectory()){
  if(entry.name==='productbuilder')continue;
  visit(filename);
 }else if(entry.isFile()&&entry.name.endsWith('.java'))java.push(filename);
}}
visit(backend);
const files=java.map(file=>({name:path.relative(root,file).replaceAll(path.sep,'/'),text:fs.readFileSync(file,'utf8')}));
const rows=controls.map(code=>({control:code,directReferences:files.filter(f=>f.text.includes(code)).map(f=>f.name)}));
console.log('HF7-C static source evidence: '+files.length+' non-PDL Java files checked; '+controls.length+' controls inventoried.');
for(const row of rows)console.log(row.control+' | '+(row.directReferences.length?row.directReferences.join(', '):'NO_DIRECT_LITERAL_REFERENCE'));
console.log('HF7_C_DIRECT_REFERENCE_CONTROL_COUNT='+rows.filter(x=>x.directReferences.length).length);
console.log('HF7_C_OPERATIONAL_CERTIFICATION=NOT_PROVEN (no Oracle, channel or E2E execution)');
