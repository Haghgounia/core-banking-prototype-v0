import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = relative => fs.readFileSync(path.join(projectRoot, relative), 'utf8');
const assert = (condition, message) => { if (!condition) throw new Error(`FEE2_MENU_NAV_FAIL: ${message}`); };

const shell = read('frontend/src/app/layout/app-shell.component.html');
const fee2Home = read('frontend/src/app/features/fee2/fee2-home.component.html');
const routes = read('frontend/src/app/app.routes.ts');
const breadcrumbs = read('frontend/src/app/shared/ui/app-breadcrumb.component.ts');

assert(!shell.includes('routerLink="/fee2/studio"'), 'the FEE2 Studio shortcut remains in the MAIN sidebar');
assert(shell.includes('routerLink="/fee2"'), 'the New Fee Engine shortcut must remain in the main menu');
assert(shell.includes('routerLink="/fee"'), 'the independent FEE shortcut must remain');
assert(fee2Home.includes('routerLink="/fee2/studio"'), 'the Studio entry inside New Fee Engine must remain');
assert(routes.includes("path: 'fee2/studio'"), 'the FEE2 Studio route was removed');
assert(breadcrumbs.includes("'/fee2/studio'"), 'the FEE2 Studio breadcrumb route was removed');
console.log('FEE2_STUDIO_MAIN_MENU_CLEANUP_PASS');
console.log('Sidebar shortcut removed; FEE / FEE2 links, Studio entry, route and breadcrumb retained.');
console.log('This is a static check only. Run build-production.cmd and verify navigation in the browser.');
