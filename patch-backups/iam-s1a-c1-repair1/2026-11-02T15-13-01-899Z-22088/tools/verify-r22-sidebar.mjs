/** Static, release-safe navigation regression for the R22 independent sidebar patch. */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = file => fs.readFileSync(path.join(root,file), 'utf8');
const assert = (pass, reason) => { if (!pass) { console.error('R22_SIDEBAR_VERIFY_FAIL: ' + reason); process.exit(1); } };
const html = read('frontend/src/app/layout/app-shell.component.html');
const ts = read('frontend/src/app/layout/app-shell.component.ts');
const newTs = read('frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.ts');
const newCss = read('frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.scss');
const routes = read('frontend/src/app/app.routes.ts');
const fee2Home = read('frontend/src/app/features/fee2/fee2-home.component.html');
const sidebar = html.split('<!-- R22:SIDEBAR:BEGIN -->')[1]?.split('<!-- R22:SIDEBAR:END -->')[0] ?? '';

assert(html.includes('<!-- R22:SIDEBAR:BEGIN -->') && html.includes('<!-- R22:SIDEBAR:END -->'), 'install script was not run');
assert(ts.includes("import {SidebarR22GroupComponent} from './sidebar-r22/sidebar-r22-group.component';"), 'missing standalone component import');
assert(/AppBreadcrumbComponent\s*,\s*SidebarR22GroupComponent/.test(ts), 'R22 not registered in AppShell imports');
for (const [id,label] of [
  ['parties','اشخاص و مشتریان'],['products','محصولات و قیمت‌گذاری'],
  ['deposits','سپرده و حساب'],['organization','سازمان و شعب'],
  ['references','داده‌های مرجع'],['technical','مدیریت و ابزارهای فنی']
]) {
  const needle = `groupId="${id}" label="${label}"`;
  assert(html.includes(needle), `sidebar group missing: ${id}`);
  assert(html.split(`groupId="${id}"`).length === 2, `group duplicated: ${id}`);
}
assert(html.includes('class="section-list r22-dashboard"'), 'dashboard not directly reachable');
for (const route of [
 '/dashboard','/cif/parties','/cif/parties/new','/fee','/fee2','/product-builder',
 '/four-deposits','/four-deposits/opening','/four-deposits/batch-opening',
 '/four-deposits/change-management','/four-deposits/runtime-readiness',
 '/four-deposits/account-operations','/four-deposits/operations','/four-deposits/reference-data',
 '/organization','/reference-data','/system/database-model-comparison',
 '/system/oracle-ea-xmi-export','/system-specification'
]) {
  assert(sidebar.includes(`routerLink="${route}"`), `legacy route link removed: ${route}`);
  assert(sidebar.split(`routerLink="${route}"`).length===2, `legacy route duplicated: ${route}`);
}
assert(!html.includes('routerLink="/fee2/studio"'), 'duplicate FEE2 Studio top-level link returned');
assert(fee2Home.includes('routerLink="/fee2/studio"'), 'FEE2 Studio link inside new engine was removed');
assert(routes.includes("path: 'fee2/studio'"), 'FEE2 studio route removed');
for(const [route,label] of [
  ['/cif/parties','جست‌وجو و پرونده اشخاص'],
  ['/fee','مدیریت کارمزد — FEE'],
  ['/fee2','موتور جدید کارمزد — FEE2'],
  ['/four-deposits/reference-data','اطلاعات پایه سپرده'],
  ['/reference-data','اطلاعات پایه عمومی']
]) {
  const match = html.match(new RegExp(`<a[^>]*routerLink="${route}"[^>]*>[\\s\\S]*?<\\/a>`));
  assert(match && match[0].includes(label), `Persian navigation label missing: ${route}`);
}
assert(newTs.includes('takeUntilDestroyed()') && newTs.includes('NavigationEnd'), 'active-group route tracking missing');
assert(newTs.includes('this.manuallyExpanded.set(undefined)'), 'expanded group is not reset on navigation');
assert(newTs.includes("['/fee', '/fee2', '/product-builder']"), 'product routes not recognized');
assert(newTs.includes('[hidden]="!collapsed() && !isExpanded()"'), 'collapsed rail or expanded navigation unavailable');
assert(newTs.includes('[attr.aria-expanded]="isExpanded()"'), 'accordion lacks ARIA expanded state');
assert(newCss.includes('var(--app-primary-soft)') && newCss.includes('var(--app-text)'), 'global theme tokens not used');
console.log('R22_SIDEBAR_VERIFY_PASS');
console.log('6 collapsible groups, direct dashboard, all existing links, preserved FEE/FEE2 separation, active group and collapsed rail verified statically.');
console.log('This is not an Angular compilation or browser test; run build-production.cmd and verify the menu visually.');
