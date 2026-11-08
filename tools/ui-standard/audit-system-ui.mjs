// System UI V1: reproducible source inventory; zero network/DB, no code modifications.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const screens = path.join(root,'frontend','src','app','features');
const normalize=p=>p.replaceAll(path.sep,'/');
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]);}
const files=walk(screens).filter(f=>f.endsWith('.component.html')).sort();
const count=(s,re)=>[...s.matchAll(re)].length;
const rows=files.map(file=>{
 const html=fs.readFileSync(file,'utf8');
 const style=file.replace(/\.html$/,'.scss');
 const css=fs.existsSync(style)?fs.readFileSync(style,'utf8'):'';
 const rel=normalize(path.relative(root,file));
 const domain=normalize(path.relative(screens,file)).split('/')[0];
 const has=(term)=>html.includes(term);
 const matTable=/<table\b[^>]*\bmat-table\b/m.test(html);
 const nativeTable=/<table\b(?![^>]*\bmat-table\b)/m.test(html);
 const paginator=/<mat-paginator\b/i.test(html);
 const dateNative=count(html,/\btype=["'](?:date|time|datetime-local)["']/g);
 const iconButtons=count(html,/<button\b[^>]*\bmat-icon-button\b/g);
 const inputs=count(html,/<(?:input|select|textarea|mat-form-field|app-persian-date-input)\b/g);
 const forms=count(html,/<form\b/g);
 const technicalText=count(html,/(?:\bOracle\b|\bDataSource\b|\bJWT\b)/g);
 const colors=count(css,/(?:#[0-9A-Fa-f]{3,8}\b|rgba?\()/g);
 const tokens=count(css,/var\(--app-/g);
 const CRUD= /(?:\(click\)=\"[^\"]*(?:create|save|remove|edit|delete|startUpdate|startDirectUpdate)[^\"]*\"|\(ngSubmit\))/.test(html);
 const listOrTable = matTable||nativeTable;
 let kind='NAVIGATION_OR_READONLY';
 if (CRUD&&listOrTable) kind='CRUD_OR_AGGREGATE';
 else if(CRUD&&inputs) kind='OPERATIONAL_FORM';
 else if(listOrTable) kind='LIST_OR_REPORT';
 else if(inputs||forms) kind='SINGLE_FORM_OR_TOOL';
 const open=[];
 if(dateNative>0)open.push('NATIVE_DATE_REVIEW');
 if(colors>12&&tokens<3)open.push('HARD_CODED_THEME_REVIEW');
 if(listOrTable&&CRUD&&!paginator)open.push('PAGINATION_CONTRACT_REVIEW');
 if(technicalText>0&&!['database-model-comparison','oracle-ea-xmi-export','iam'].includes(domain))open.push('TECHNICAL_COPY_REVIEW');
 const scope=/<(?:section|main|div)\b[^>]*(?:class\.ui-standard-v1|\bui-standard-v1\b)/s.test(html);
 if(!scope)open.unshift('GLOBAL_UI_SCOPE_MISSING');
 return {domain,component:rel,kind,scope:scope?'APPLIED':'MISSING',material_table:matTable?'YES':'NO',native_table:nativeTable?'YES':'NO',paginator:paginator?'YES':'NO',native_date:dateNative,icon_buttons:iconButtons,technical_tokens:technicalText,css_colors:colors,css_theme_tokens:tokens,additional_review:open.join(';')||'NONE'};
});
const esc=s=>String(s).replaceAll('"','""');
const header=Object.keys(rows[0]);
const csv=[header.join(','),...rows.map(r=>header.map(k=>'"'+esc(r[k])+'"').join(','))].join('\r\n')+'\r\n';
const outfile=path.join(root,'docs','ui-standard','UI-STANDARD-V1-SYSTEM-INVENTORY-2026-11-08.csv');
fs.mkdirSync(path.dirname(outfile),{recursive:true});
fs.writeFileSync(outfile,'\ufeff'+csv,'utf8');
const domains=[...new Set(rows.map(r=>r.domain))].sort();
const result={featureScreens:rows.length,shellTemplate:1,totalAngularTemplates:rows.length+1,domains:domains.length,
 rootsApplied:rows.filter(r=>r.scope==='APPLIED').length,legacyDateScreens:rows.filter(r=>r.native_date>0).length,
 legacyDateFields:rows.reduce((n,r)=>n+r.native_date,0),
 formatReview:rows.filter(r=>r.additional_review.includes('HARD_CODED_THEME_REVIEW')).length,
 paginationContractReview:rows.filter(r=>r.additional_review.includes('PAGINATION_CONTRACT_REVIEW')).length,
 technicalCopyReview:rows.filter(r=>r.additional_review.includes('TECHNICAL_COPY_REVIEW')).length,
 scopedRootMissing:rows.filter(r=>r.scope==='MISSING').length,
 candidateKinds:Object.fromEntries([...new Set(rows.map(r=>r.kind))].sort().map(k=>[k,rows.filter(r=>r.kind===k).length])),
 countByDomain:Object.fromEntries(domains.map(d=>[d,rows.filter(r=>r.domain===d).length]))};
const jsonfile=path.join(root,'docs','ui-standard','UI-STANDARD-V1-SUMMARY-2026-11-08.json');
fs.writeFileSync(jsonfile,JSON.stringify(result,null,2)+'\n');
console.log('SYSTEM_UI_AUDIT='+JSON.stringify(result));
