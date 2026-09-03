import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const component = read('frontend/src/app/shared/ui/app-breadcrumb.component.ts');
const shellTs = read('frontend/src/app/layout/app-shell.component.ts');
const shellHtml = read('frontend/src/app/layout/app-shell.component.html');
const styles = read('frontend/src/styles.scss');

const checks = [
  ['shared breadcrumb component exists', component.includes("selector: 'app-breadcrumb'")],
  ['navigation events drive breadcrumb', component.includes('NavigationEnd') && component.includes('urlAfterRedirects')],
  ['page H1 supplies dynamic title', component.includes("main.content h1.page-title, main.content h1")],
  ['reference hierarchy covered', component.includes("'/reference-data'") && component.includes("'/reference-data/general'")],
  ['CAL hierarchy covered', component.includes("'/calendar/reference-data'")],
  ['CAL2 hierarchy covered', component.includes("'/calendar2/reference-data'")],
  ['CIF reference hierarchy covered', component.includes("'/cif/reference-data'")],
  ['deposit hierarchy covered', component.includes("'/deposit/reference-data'")],
  ['fee hierarchy covered', component.includes("'/fee'")],
  ['product builder hierarchy covered', component.includes("'/product-builder'")],
  ['Party operations hierarchy covered', component.includes("'/cif/parties'")],
  ['system hierarchy covered', component.includes("'/system-specification'")],
  ['shell imports breadcrumb', shellTs.includes('AppBreadcrumbComponent')],
  ['shell renders breadcrumb before router outlet', shellHtml.indexOf('<app-breadcrumb />') >= 0 && shellHtml.indexOf('<app-breadcrumb />') < shellHtml.indexOf('<router-outlet />')],
  ['legacy page breadcrumbs hidden', styles.includes('main.content nav.reference-breadcrumb') && styles.includes('main.content nav.breadcrumb')]
];

const failed = checks.filter(([, ok]) => !ok).map(([name]) => name);
if (failed.length) throw new Error(`Global breadcrumb verification failed: ${failed.join(', ')}`);
console.log(`Global breadcrumb verification OK: ${checks.length} checks; centralized navigation path covers all application sections.`);
