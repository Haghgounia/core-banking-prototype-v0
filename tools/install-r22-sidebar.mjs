/**
 * R22 independent sidebar installer.
 * IMPORTANT: it patches the CURRENT app shell in place; never ships an old
 * shared app-shell file or touches Product Builder, FEE, FEE2, API, DB sources.
 * When unexpected source drift is detected it exits BEFORE writing any files.
 */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const shellPath = path.join(root, 'frontend/src/app/layout/app-shell.component.html');
const componentPath = path.join(root, 'frontend/src/app/layout/app-shell.component.ts');
const newComponent = path.join(root, 'frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.ts');
const newStyle = path.join(root, 'frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.scss');
const fail = (message) => { console.error('R22_SIDEBAR_INSTALL_FAIL: ' + message); process.exit(1); };
const assert = (condition, message) => { if (!condition) fail(message); };
const read = (file) => { assert(fs.existsSync(file), `missing ${path.relative(root, file)}`); return fs.readFileSync(file, 'utf8'); };

const originalHtml = read(shellPath);
const originalTs = read(componentPath);
read(newComponent);
read(newStyle);

const MARKER_BEGIN = '<!-- R22:SIDEBAR:BEGIN -->';
const MARKER_END = '<!-- R22:SIDEBAR:END -->';
if (originalHtml.includes(MARKER_BEGIN) || originalHtml.includes(MARKER_END)) {
  assert(originalHtml.includes(MARKER_BEGIN) && originalHtml.includes(MARKER_END), 'partial R22 markers; manual review required');
  assert(originalTs.includes('SidebarR22GroupComponent'), 'R22 sidebar markers present but Angular import missing');
  console.log('R22_SIDEBAR_ALREADY_INSTALLED: no files changed');
  process.exit(0);
}
assert(!originalHtml.includes('app-sidebar-r22-group'), 'R22 component exists without installation markers');
assert(!originalTs.includes('SidebarR22GroupComponent'), 'R22 TS import exists without installation markers');
assert(!originalHtml.includes('routerLink="/fee2/studio"'), 'duplicate FEE2 Studio sidebar link found; run the earlier menu cleanup first');

const SECTION_RX = /<div class="menu-section-label">([^<]+)<\/div>\s*(<mat-nav-list\b[^>]*>[\s\S]*?<\/mat-nav-list>)/g;
const sections = [...originalHtml.matchAll(SECTION_RX)];
const expected = [
  'عملیات', 'محصولات و قیمت‌گذاری', 'چهار سپرده',
  'ساختار سازمانی و شبکه شعب', 'داده‌های مرجع', 'مدیریت و سیستم'
];
for (const label of expected) assert(sections.filter(x => x[1] === label).length === 1, `sidebar section "${label}" is missing or duplicated; no files changed`);
const first = sections.find(x => x[1] === 'عملیات');
const last = sections.find(x => x[1] === 'مدیریت و سیستم');
assert(first.index < last.index, 'unexpected sidebar order');
assert(originalHtml.slice(first.index, last.index + last[0].length).includes('routerLink="/product-builder"'), 'Product Builder navigation missing; no files changed');

const anchorPattern = /<a\b[^>]*routerLink="([^"]+)"[^>]*>[\s\S]*?<\/a>/g;
const anchors = source => [...source.matchAll(anchorPattern)].map(x => ({route:x[1], html:x[0]}));
const originalRoutes = anchors(originalHtml).map(x=>x.route);
const originalProductAnchors = anchors(originalHtml).filter(x=>x.route.startsWith('/product-builder'));

const names = {
  '/cif/parties': 'جست‌وجو و پرونده اشخاص',
  '/cif/parties/new': 'ایجاد شخص جدید',
  '/fee': 'مدیریت کارمزد — FEE',
  '/fee2': 'موتور جدید کارمزد — FEE2',
  '/four-deposits/runtime-readiness': 'آمادگی اجرا',
  '/four-deposits/reference-data': 'اطلاعات پایه سپرده',
  '/reference-data': 'اطلاعات پایه عمومی',
  '/system/database-model-comparison': 'مقایسه مدل داده EA و Oracle',
  '/system/oracle-ea-xmi-export': 'استخراج مدل Oracle به XML'
};
// Maintain compatibility with existing release verifiers and old terminology
// in tooltips, while showing the clearer R22 headings to business users.
const legacyTooltips = {
  '/fee2': 'موتور کارمزد جدید — FEE2',
  '/system/oracle-ea-xmi-export': 'استخراج Oracle به EA XML (استخراج مدل Oracle به XML)'
};
const renameKnownLinks = list => list.replace(anchorPattern, (anchor, route) => {
  const name = names[route];
  if (!name) return anchor;
  assert(anchor.includes('<span matListItemTitle>') && anchor.includes('matTooltip="'), `unrecognized anchor format for ${route}`);
  return anchor.replace(/(<span matListItemTitle>)[\s\S]*?(<\/span>)/, (_, before, after) => before + name + after)
    .replace(/matTooltip="[^"]*"/, `matTooltip="${legacyTooltips[route] ?? name}"`);
});

const startListRx = /^(<mat-nav-list\b[^>]*>)([\s\S]*)(<\/mat-nav-list>)$/;
function navListParts(list) {
  const match = list.match(startListRx);
  assert(Boolean(match), 'unexpected mat-nav-list syntax');
  return {open:match[1], body:match[2], close:match[3]};
}
function wrap(groupId, title, icon, list) {
  return `<app-sidebar-r22-group groupId="${groupId}" label="${title}" icon="${icon}" [collapsed]="sidebarCollapsed()">\n      ${renameKnownLinks(list)}\n    </app-sidebar-r22-group>`;
}
const wrappers = new Map([
  ['محصولات و قیمت‌گذاری', ['products', 'محصولات و قیمت‌گذاری', 'sell']],
  ['چهار سپرده', ['deposits', 'سپرده و حساب', 'account_balance_wallet']],
  ['ساختار سازمانی و شبکه شعب', ['organization', 'سازمان و شعب', 'account_tree']],
  ['داده‌های مرجع', ['references', 'داده‌های مرجع', 'dataset']],
  ['مدیریت و سیستم', ['technical', 'مدیریت و ابزارهای فنی', 'settings']]
]);
const operationsSection = sections.find(x => x[1] === 'عملیات');
const allOperations = navListParts(operationsSection[2]);
const opAnchors = anchors(allOperations.body);
assert(opAnchors.some(x=>x.route === '/dashboard'), 'dashboard navigation missing');
assert(opAnchors.some(x=>x.route === '/cif/parties'), 'Party navigation missing');
assert(opAnchors.some(x=>x.route === '/cif/parties/new'), 'New Party navigation missing');
const dashboardAnchors = opAnchors.filter(x=>x.route === '/dashboard');
const partyAnchors = opAnchors.filter(x=>x.route !== '/dashboard');
assert(dashboardAnchors.length === 1, 'dashboard must be unique');
// Keep all future operations links rather than discarding them; only dashboard is direct.
assert(allOperations.body.replace(anchorPattern, '').trim() === '', 'unexpected operations markup; manual review required');
const directDashboard = `<mat-nav-list class="section-list r22-dashboard">\n      ${dashboardAnchors[0].html}\n    </mat-nav-list>`;
const partiesList = `${allOperations.open}\n      ${partyAnchors.map(x=>x.html).join('\n      ')}\n    ${allOperations.close}`;
const replaceSections = new Map();
replaceSections.set('عملیات', `${directDashboard}\n\n    ${wrap('parties', 'اشخاص و مشتریان', 'groups', partiesList)}`);
for (const [label, [id, title, icon]] of wrappers) {
  const found = sections.find(x => x[1] === label);
  replaceSections.set(label, wrap(id, title, icon, found[2]));
}
let sectionCount = 0;
const originalSlice = originalHtml.slice(first.index, last.index + last[0].length);
const newSlice = originalSlice.replace(SECTION_RX, (whole, label) => {
  if (!replaceSections.has(label)) return whole; // Preserve any future, unrelated sections.
  sectionCount++;
  return replaceSections.get(label);
});
assert(sectionCount === expected.length, 'not all expected sections were transformed');
const updatedHtml = originalHtml.slice(0, first.index) + `${MARKER_BEGIN}\n    ` + newSlice + `\n    ${MARKER_END}` + originalHtml.slice(last.index + last[0].length);

let updatedTs = originalTs;
const importLine = "import {SidebarR22GroupComponent} from './sidebar-r22/sidebar-r22-group.component';";
const breadcrumbImportRx = /import\s*\{\s*AppBreadcrumbComponent\s*\}\s*from\s*['"]\.\.\/shared\/ui\/app-breadcrumb\.component['"];?/;
assert(breadcrumbImportRx.test(updatedTs), 'AppBreadcrumbComponent import moved; no files changed');
updatedTs = updatedTs.replace(breadcrumbImportRx, match => match + '\n' + importLine);
let importsFound = false;
updatedTs = updatedTs.replace(/(\bimports\s*:\s*\[)([\s\S]*?)(\])/, (all, start, content, end) => {
  assert(content.includes('AppBreadcrumbComponent'), 'AppShell Angular imports not recognized; no files changed');
  importsFound = true;
  return start + content.replace(/\bAppBreadcrumbComponent\b/, 'AppBreadcrumbComponent, SidebarR22GroupComponent') + end;
});
assert(importsFound, 'AppShell Angular imports array missing');

const newRoutes = anchors(updatedHtml).map(x=>x.route);
assert(JSON.stringify(originalRoutes) === JSON.stringify(newRoutes), 'sidebar route order changed; no files changed');
const productAfter = anchors(updatedHtml).filter(x=>x.route.startsWith('/product-builder'));
assert(JSON.stringify(productAfter) === JSON.stringify(originalProductAnchors), 'a Product Builder navigation anchor changed; no files changed');
assert(updatedHtml.includes('routerLink="/fee2"') && !updatedHtml.includes('routerLink="/fee2/studio"'), 'FEE2 independent navigation contract changed');
assert(updatedHtml.includes('routerLink="/fee"'), 'FEE independent navigation contract changed');

// Write only after ALL validation has succeeded. No shared file is copied from a ZIP.
const writeAtomic = (filename, contents) => {
  const temp = filename + '.r22-tmp';
  fs.writeFileSync(temp, contents, 'utf8');
  fs.renameSync(temp, filename);
};
assert(read(shellPath) === originalHtml && read(componentPath) === originalTs, 'AppShell changed concurrently during R22 preparation; retry only after coordinating with the Product Builder developer');
try {
  writeAtomic(shellPath, updatedHtml);
  writeAtomic(componentPath, updatedTs);
} catch (error) {
  // Repair a partially written installation rather than leaving the AppShell unusable.
  try { fs.writeFileSync(shellPath, originalHtml, 'utf8'); } catch { /* original backup remains with user */ }
  try { fs.writeFileSync(componentPath, originalTs, 'utf8'); } catch { /* original backup remains with user */ }
  fail('write failed; attempted to restore both originals: ' + error.message);
}
console.log('R22_SIDEBAR_INSTALL_PASS');
console.log(`Preserved ${originalRoutes.length} route links in the same order and ${originalProductAnchors.length} Product Builder anchor(s) byte-for-byte.`);
console.log('Only current app-shell.component.html and app-shell.component.ts were surgically updated; new sidebar code is isolated.');
console.log('Next: node tools\\verify-r22-sidebar.mjs && build-production.cmd');
