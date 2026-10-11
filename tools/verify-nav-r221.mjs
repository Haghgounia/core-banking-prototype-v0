/** NAV-R22.1 static contract checks; never modifies project files. */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const check=(ok,message)=>{if(!ok){console.error('NAV_R221_VERIFY_FAIL: '+message);process.exit(1);}};
const html=read('frontend/src/app/layout/app-shell.component.html');
const css=read('frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.scss');
const routes=read('frontend/src/app/app.routes.ts');
const fee2Home=read('frontend/src/app/features/fee2/fee2-home.component.html');
const before=html.split('<!-- R22:SIDEBAR:BEGIN -->')[1];
const body=before?.split('<!-- R22:SIDEBAR:END -->')[0]||'';
check(Boolean(body),'R22 sidebar is not installed');
const links=[...body.matchAll(/<a\b[^>]*\brouterLink="([^"]+)"[^>]*>[\s\S]*?<\/a>/g)];
const get=route=>links.filter(link=>link[1]===route);
for(const [route,title] of [['/fee','مدیریت کارمزد (FEE)'],['/fee2','موتور کارمزد (FEE2)']]){
 const anchors=get(route);
 check(anchors.length===1,`duplicate or missing ${route} route`);
 check(anchors[0][0].includes(`<span matListItemTitle>${title}</span>`),`short title missing for ${route}`);
 check(anchors[0][0].includes('matTooltip='),`tooltip missing for ${route}`);
}
check(get('/fee')[0][0].includes('مدیریت کارمزد — FEE'),'legacy FEE tooltip missing');
check(get('/fee2')[0][0].includes('موتور جدید کارمزد — FEE2')&&get('/fee2')[0][0].includes('موتور کارمزد جدید'),'legacy FEE2 terminology missing');
check(get('/product-builder').length===1,'Product Builder menu link missing');
check(!body.includes('routerLink="/fee2/studio"'),'duplicate studio shortcut present');
check(fee2Home.includes('routerLink="/fee2/studio"')&&routes.includes("path: 'fee2/studio'"),'FEE2 studio accessibility was lost');
check(css.includes('/* NAV-R22.1:VISUAL:BEGIN */')&&css.includes('/* NAV-R22.1:VISUAL:END */'),'responsive style block missing');
check(css.includes('.r22-group:not(.rail)')&&css.includes('white-space: normal !important')&&css.includes('height: auto !important'),
      'wrap styles missing or collapsed rail behavior changed');
check(fs.existsSync(path.join(root,'docs/patch-backups/nav-r221/sidebar-visual-state.json')),'rollback state missing');
console.log('NAV_R221_VERIFY_PASS');
console.log('Short Persian labels, accessible full tooltips, wrap for long menu links, preserved Product Builder and FEE/FEE2 routes (static verification).');
