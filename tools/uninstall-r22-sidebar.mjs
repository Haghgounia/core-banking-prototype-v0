/** Revert R22 wrappers, preserving any Product Builder links added since installation. */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const htmlPath=path.join(root,'frontend/src/app/layout/app-shell.component.html');
const tsPath=path.join(root,'frontend/src/app/layout/app-shell.component.ts');
const fail = msg=>{ console.error('R22_SIDEBAR_ROLLBACK_FAIL: '+msg); process.exit(1); };
const check=(ok,msg)=>{if(!ok)fail(msg);};
const html=fs.readFileSync(htmlPath,'utf8');
const ts=fs.readFileSync(tsPath,'utf8');
const begin='<!-- R22:SIDEBAR:BEGIN -->';
const end='<!-- R22:SIDEBAR:END -->';
if(!html.includes(begin) && !html.includes(end)){
  console.log('R22_SIDEBAR_NOT_INSTALLED: nothing to roll back');
  process.exit(0);
}
check(html.includes(begin)&&html.includes(end),'incomplete markers');
const start=html.indexOf(begin), stop=html.indexOf(end,start)+end.length;
let region=html.slice(start+begin.length,stop-end.length);
const hrefs=source=>[...source.matchAll(/routerLink="([^"]+)"/g)].map(m=>m[1]);
const oldLinks=hrefs(region);
const groupRx=/<app-sidebar-r22-group\b([^>]*)>\s*(<mat-nav-list\b[^>]*>[\s\S]*?<\/mat-nav-list>)\s*<\/app-sidebar-r22-group>/g;
const groups=[...region.matchAll(groupRx)];
check(groups.length===6,'expected six R22 groups, found '+groups.length);
const expected=new Map([
 ['parties','عملیات'],['products','محصولات و قیمت‌گذاری'],
 ['deposits','سپرده'],['organization','ساختار سازمانی و شبکه شعب'],
 ['references','داده‌های مرجع'],['technical','مدیریت و سیستم']
]);
const replaced=new Set();
region=region.replace(groupRx, (whole,attributes,list)=>{
  const idMatch=attributes.match(/groupId="([^"]+)"/);
  check(Boolean(idMatch), 'groupId missing in R22 wrapper');
  const id=idMatch[1];
  check(expected.has(id) && !replaced.has(id),'unexpected/duplicated group '+id);
  replaced.add(id);
  if (id==='parties') return '<!-- R22:PARTIES:PLACEHOLDER -->'+list;
  return `<div class="menu-section-label">${expected.get(id)}</div>\n    ${list}`;
});
const dashRx=/<mat-nav-list class="section-list r22-dashboard">([\s\S]*?)<\/mat-nav-list>\s*<!-- R22:PARTIES:PLACEHOLDER --><mat-nav-list\b([^>]*)>([\s\S]*?)<\/mat-nav-list>/;
const match=region.match(dashRx);
check(Boolean(match), 'dashboard/Party sections changed, manual rollback required');
region=region.replace(dashRx, (_whole,dashboard,attrs,party) =>
  `<div class="menu-section-label">عملیات</div>\n    <mat-nav-list${attrs}>${dashboard}${party}</mat-nav-list>`);
check(replaced.size===expected.size, 'not all R22 groups were restored');
check(JSON.stringify(hrefs(region))===JSON.stringify(oldLinks),'route order changed during rollback');
const updatedHtml=html.slice(0,start)+region.trimStart().trimEnd()+html.slice(stop);
const importLine="import {SidebarR22GroupComponent} from './sidebar-r22/sidebar-r22-group.component';";
check(ts.includes(importLine),'R22 standalone import missing');
let updatedTs=ts.replace(importLine+'\n','');
check(updatedTs!==ts,'failed to remove standalone import');
check(updatedTs.includes('AppBreadcrumbComponent, SidebarR22GroupComponent'),'R22 component registration missing');
updatedTs=updatedTs.replace('AppBreadcrumbComponent, SidebarR22GroupComponent','AppBreadcrumbComponent');
const write=(filename,content)=>{const temp=filename+'.r22-tmp';fs.writeFileSync(temp,content,'utf8');fs.renameSync(temp,filename);};
write(htmlPath,updatedHtml);
write(tsPath,updatedTs);
console.log('R22_SIDEBAR_ROLLBACK_PASS');
console.log('Restored original flat navigation layout without dropping any current links. R22 standalone files can remain unused.');
