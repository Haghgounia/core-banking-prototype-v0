/**
 * NAV-R22.1 — independent, targeted visual-polish installer.
 * Reads and patches the LIVE R22 AppShell and sidebar stylesheet in place.
 * Does NOT ship replacements for shared files or touch business modules.
 * Unsafe/unexpected source drift aborts before any source-file write.
 */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const htmlPath = path.join(root, 'frontend/src/app/layout/app-shell.component.html');
const cssPath = path.join(root, 'frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.scss');
const backupPath = path.join(root, 'docs/patch-backups/nav-r221/sidebar-visual-state.json');
const fail = message => { console.error('NAV_R221_INSTALL_FAIL: ' + message); process.exit(1); };
const requireTrue = (condition, message) => { if (!condition) fail(message); };
const read = filename => { requireTrue(fs.existsSync(filename), `missing ${path.relative(root, filename)}`); return fs.readFileSync(filename, 'utf8'); };
const atomicWrite = (filename, content) => {
  const tmp = filename + `.nav-r221-${process.pid}-tmp`;
  try { fs.writeFileSync(tmp, content, 'utf8'); fs.renameSync(tmp, filename); }
  finally { if (fs.existsSync(tmp)) fs.unlinkSync(tmp); }
};

const BEGIN = '<!-- R22:SIDEBAR:BEGIN -->';
const END = '<!-- R22:SIDEBAR:END -->';
const CSS_BEGIN = '/* NAV-R22.1:VISUAL:BEGIN */';
const CSS_END = '/* NAV-R22.1:VISUAL:END */';
const CURRENT = {
  '/fee': {old: 'مدیریت کارمزد — FEE', title: 'مدیریت کارمزد (FEE)'},
  '/fee2': {old: 'موتور جدید کارمزد — FEE2', title: 'موتور کارمزد (FEE2)'}
};
const cssExtension = `${CSS_BEGIN}
/* Keep the accordion compact and give projected Material menu labels real room. */
:host { padding-inline: 6px; }
.r22-links { margin-inline-start: 2px; }
/* The group is NOT in rail mode here; do not alter the compact icon-only sidebar. */
:host ::ng-deep .r22-group:not(.rail) .r22-links a[mat-list-item] {
  min-height: 48px;
  height: auto !important;
}
:host ::ng-deep .r22-group:not(.rail) .r22-links a[mat-list-item] .mdc-list-item__content {
  min-width: 0;
}
:host ::ng-deep .r22-group:not(.rail) .r22-links a[mat-list-item] [matListItemTitle],
:host ::ng-deep .r22-group:not(.rail) .r22-links a[mat-list-item] .mdc-list-item__primary-text {
  white-space: normal !important;
  text-overflow: clip !important;
  overflow: visible !important;
  overflow-wrap: break-word;
  line-height: 1.4;
}
${CSS_END}`;
const html = read(htmlPath);
const css = read(cssPath);
requireTrue(html.includes(BEGIN) && html.includes(END), 'R22 sidebar is not installed (or markers are incomplete)');
requireTrue(html.indexOf(BEGIN) < html.indexOf(END), 'R22 sidebar marker order invalid');
const installedStyle = css.includes(CSS_BEGIN) || css.includes(CSS_END);
const installedLabels = Object.values(CURRENT).every(({title}) => html.includes(`<span matListItemTitle>${title}</span>`));
if (installedStyle && installedLabels) {
  requireTrue(css.includes(CSS_BEGIN) && css.includes(CSS_END) && fs.existsSync(backupPath), 'partially installed patch: review manually');
  console.log('NAV_R221_ALREADY_INSTALLED: no files changed');
  process.exit(0);
}
requireTrue(!installedStyle && !installedLabels, 'incomplete R22.1 state: no files changed');
requireTrue(!fs.existsSync(backupPath), 'an earlier backup exists without the complete patch: review manually');
requireTrue(css.includes('.r22-group.rail .r22-links') && css.includes('.r22-links[hidden]'), 'R22 stylesheet changed unexpectedly');
requireTrue(!html.includes('routerLink="/fee2/studio"'), 'FEE2 studio shortcut must not return to main menu');

const before = html.slice(0, html.indexOf(BEGIN) + BEGIN.length);
const body = html.slice(before.length, html.indexOf(END));
const after = html.slice(html.indexOf(END));
const rx = /<a\b[^>]*\brouterLink="([^"]+)"[^>]*>[\s\S]*?<\/a>/g;
const anchors = text => [...text.matchAll(rx)].map(hit => ({route:hit[1], full:hit[0]}));
const oldAnchors = anchors(body);
const originalRoutes = oldAnchors.map(a => a.route);
const productAnchors = oldAnchors.filter(a => a.route.startsWith('/product-builder'));
const previous = {};
let updated = body;
for (const [route, entry] of Object.entries(CURRENT)) {
  const found = oldAnchors.filter(a => a.route === route);
  requireTrue(found.length === 1, `expected one ${route} menu link, found ${found.length}`);
  const full = found[0].full;
  const title = full.match(/<span\s+matListItemTitle>([\s\S]*?)<\/span>/);
  const tooltip = full.match(/\bmatTooltip="([^"]*)"/);
  requireTrue(title && tooltip && title[1] === entry.old, `unexpected ${route} label or tooltip format; no files changed`);
  requireTrue(full.includes('matListItemIcon'), `${route} icon contract changed`);
  const newTooltip = route === '/fee'
    ? tooltip[1]
    : 'موتور جدید کارمزد — FEE2 (موتور کارمزد جدید)';
  // Preserve original labels in the tooltip for existing release verifiers.
  requireTrue(newTooltip.includes(entry.old) && (route !== '/fee2' || newTooltip.includes('موتور کارمزد جدید')),
    `missing legacy tooltip for ${route}`);
  previous[route] = {title:title[1], tooltip:tooltip[1], newTitle:entry.title, newTooltip};
  const changed = full
    .replace(title[0], `<span matListItemTitle>${entry.title}</span>`)
    .replace(tooltip[0], `matTooltip="${newTooltip}"`);
  requireTrue(changed !== full, `no changes prepared for ${route}`);
  requireTrue(updated.includes(full), `${route} anchor changed while preparing patch`);
  updated = updated.replace(full, changed);
}
const nextHtml = before + updated + after;
const nextCss = css + '\n' + cssExtension + '\n';
requireTrue(JSON.stringify(originalRoutes) === JSON.stringify(anchors(updated).map(a=>a.route)), 'navigation route order changed');
requireTrue(JSON.stringify(productAnchors) === JSON.stringify(anchors(updated).filter(a=>a.route.startsWith('/product-builder'))),
  'a Product Builder anchor was changed');
requireTrue(nextHtml.includes('routerLink="/fee"') && nextHtml.includes('routerLink="/fee2"'), 'FEE/FEE2 separation broken');
requireTrue(nextHtml.split(CURRENT['/fee'].title).length === 2 && nextHtml.split(CURRENT['/fee2'].title).length === 2,
  'ambiguous target labels');
requireTrue(read(htmlPath) === html && read(cssPath) === css, 'shared files changed during preflight; retry only after coordinating with product work');
let wroteBackup = false;
try {
  fs.mkdirSync(path.dirname(backupPath), {recursive:true});
  atomicWrite(backupPath, JSON.stringify({version:'NAV-R22.1', previous}, null, 2) + '\n');
  wroteBackup = true;
  atomicWrite(htmlPath, nextHtml);
  atomicWrite(cssPath, nextCss);
} catch (error) {
  try { atomicWrite(htmlPath, html); } catch { /* manual restore from version control */ }
  try { atomicWrite(cssPath, css); } catch { /* manual restore from version control */ }
  if (wroteBackup) { try {fs.unlinkSync(backupPath);} catch {} }
  fail('write failed; attempted to restore originals: ' + error.message);
}
console.log('NAV_R221_INSTALL_PASS');
console.log('Only current AppShell FEE/FEE2 display labels and R22 sidebar styling changed. Route order and Product Builder anchors preserved.');
console.log('For rollback: node tools/uninstall-nav-r221.mjs');
