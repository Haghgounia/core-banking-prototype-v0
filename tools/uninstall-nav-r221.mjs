/** Undo only NAV-R22.1 edits, without replacing the AppShell or deleting new links. */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const htmlPath = path.join(root,'frontend/src/app/layout/app-shell.component.html');
const cssPath = path.join(root,'frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.scss');
const backupPath = path.join(root,'docs/patch-backups/nav-r221/sidebar-visual-state.json');
const BEGIN = '<!-- R22:SIDEBAR:BEGIN -->';
const END = '<!-- R22:SIDEBAR:END -->';
const STYLE_BEGIN = '/* NAV-R22.1:VISUAL:BEGIN */';
const STYLE_END = '/* NAV-R22.1:VISUAL:END */';
const fail = msg=>{console.error('NAV_R221_UNINSTALL_FAIL: '+msg);process.exit(1);};
const check = (cond,msg)=>{if(!cond)fail(msg);};
const write = (file, content)=>{const tmp=file+`.r221-${process.pid}-tmp`; try {fs.writeFileSync(tmp,content,'utf8');fs.renameSync(tmp,file);}finally{if(fs.existsSync(tmp))fs.unlinkSync(tmp);}};
check(fs.existsSync(htmlPath) && fs.existsSync(cssPath),'AppShell or R22 sidebar stylesheet missing');
const html=fs.readFileSync(htmlPath,'utf8');
const css=fs.readFileSync(cssPath,'utf8');
if(!css.includes(STYLE_BEGIN) && !css.includes(STYLE_END) && !fs.existsSync(backupPath)){
  console.log('NAV_R221_NOT_INSTALLED: no files changed');process.exit(0);
}
check(css.includes(STYLE_BEGIN)&&css.includes(STYLE_END)&&fs.existsSync(backupPath),'partial patch state: manual review required');
check(html.includes(BEGIN)&&html.includes(END),'R22 markers changed; manual review required');
const backup=JSON.parse(fs.readFileSync(backupPath,'utf8'));
check(backup.version==='NAV-R22.1'&&backup.previous,'invalid R22.1 rollback state');
const start=html.indexOf(BEGIN)+BEGIN.length;
const end=html.indexOf(END);
let body=html.slice(start,end);
const rx=/<a\b[^>]*\brouterLink="([^"]+)"[^>]*>[\s\S]*?<\/a>/g;
const originalRoutes=[...body.matchAll(rx)].map(x=>x[1]);
for(const [route,state] of Object.entries(backup.previous)){
 const matches=[...body.matchAll(rx)].filter(x=>x[1]===route);
 check(matches.length===1,`expected one ${route} anchor`);
 const full=matches[0][0];
 const exactTitle=`<span matListItemTitle>${state.newTitle}</span>`;
 const exactTooltip=`matTooltip="${state.newTooltip}"`;
 check(full.includes(exactTitle)&&full.includes(exactTooltip),`the ${route} menu link changed after R22.1; preserve newer edits and review manually`);
 body=body.replace(full,full.replace(exactTitle,`<span matListItemTitle>${state.title}</span>`)
                              .replace(exactTooltip,`matTooltip="${state.tooltip}"`));
}
const styleFrom=css.indexOf(STYLE_BEGIN);
const styleTo=css.indexOf(STYLE_END,styleFrom);
check(styleFrom>0 && styleTo>styleFrom,'style block malformed');
check(css[styleFrom-1]==='\n' && css[styleTo+STYLE_END.length]==='\n','style block moved unexpectedly');
const cssRestored = css.slice(0,styleFrom-1) + css.slice(styleTo+STYLE_END.length+1);
const htmlRestored = html.slice(0,start) + body + html.slice(end);
check(JSON.stringify(originalRoutes)===JSON.stringify([...body.matchAll(rx)].map(x=>x[1])),'rollback changed route order');
check(fs.readFileSync(htmlPath,'utf8')===html&&fs.readFileSync(cssPath,'utf8')===css,'concurrent source edit: no files changed');
try{write(htmlPath,htmlRestored);write(cssPath,cssRestored);}catch(error){
  try{write(htmlPath,html);}catch{}
  try{write(cssPath,css);}catch{}
  fail('rollback write failed: '+error.message);
}
fs.unlinkSync(backupPath);
console.log('NAV_R221_UNINSTALL_PASS');
console.log('Only NAV-R22.1 visible labels/tooltips and added CSS removed; other AppShell links preserved.');
