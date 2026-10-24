import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = (filename) => fs.readFileSync(path.join(root, filename), 'utf8');
const assert = (condition, description) => {
  if (!condition) { console.error(`ORG_UNIT_LOCATION_R2A_VERIFY_FAIL: ${description}`); process.exit(1); }
};

const src = 'backend/src/main/java/com/behsazan/corebanking/organization/location/';
const service = read(src + 'application/OrganizationUnitLocationCanonicalReviewService.java');
const controller = read(src + 'web/OrganizationUnitLocationCanonicalReviewController.java');
const ui = read('frontend/src/app/features/organization/organization-unit-locations-canonical.component.ts');
const routes = read('frontend/src/app/app.routes.ts');
const routerConfig = read('frontend/src/app/app.config.ts');
const installGuide = read('docs/install/ORG-UNIT-LOCATIONS-R2A-POSTCOMMIT-INSTALL-FA.md');
const menu = read('frontend/src/app/features/organization/organization-menu.component.html');
const quick = read('frontend/src/app/features/organization/organization-menu.component.ts');
const legacy = read(src + 'application/OrganizationUnitLocationReviewService.java');

assert(service.includes('@Transactional(readOnly = true)') && service.includes('LEGACY_BACKFILL_FLAG') &&
  service.includes('ROW_VERSION') && service.includes('DECODE(UL.POSTAL_CODE,P.POSTAL_CODE,0,1)') &&
  service.includes('UL.ORGANIZATION_UNIT_LOCATION_ID') && service.includes("'DRIFT'") &&
  service.includes("'MISSING_LOCATION'") && service.includes("'UNLINKED_ADDRESS'") && service.includes("'AMBIGUOUS_ADDRESS'") &&
  service.includes('P.POSTAL_ADDRESS_ID=UL.LEGACY_POSTAL_ADDRESS_ID') && service.includes('OFFSET :rowOffset ROWS FETCH NEXT :rowLimit ROWS ONLY'), 'canonical read-only and drift comparison incomplete');
const compared = [...service.matchAll(/DECODE\(UL\.[A-Z_]+,[LP]\.[A-Z_]+,0,1\)/g)];
assert(compared.length === 20, `expected exactly 20 exact-value comparisons, got ${compared.length}`);
assert(!/(?:jdbc\.|getJdbcTemplate\(\)\.)update\s*\(|\bINSERT\b|\bMERGE\s+INTO\b|\bDELETE\s+FROM\b|\bALTER\s+TABLE\b/.test(service),
  'R2-A source must never mutate tables');
assert(controller.includes('@GetMapping') && !/@(?:PostMapping|PutMapping|DeleteMapping|PatchMapping)/.test(controller), 'R2-A API is GET-only');
assert(legacy.includes('LEFT JOIN %1$s.POSTAL_ADDRESSES P'), 'legacy projection was removed');
console.log('PASS GET-only canonical snapshot, 20 strict field comparisons and preserved legacy projection');

assert(routes.includes("path: 'organization/unit-locations-canonical'") &&
  routes.indexOf("path: 'organization/unit-locations-canonical'") < routes.indexOf("path: 'organization/:resource'"),
  'canonical route missing or shadowed by catch-all');
for (const token of ["unit-locations-canonical", "legacyBackfillFlag", "sourceStatus", "rowVersion", "postalCode", "geoEntityId", "organizationUnitLocationId", "sourceDriftRows", "ambiguousLegacyAddresses", "hasRisks", "pageCount", "routerLink=\"/organization/unit-locations-review\""]) {
  assert(ui.includes(token), `canonical page missing field or safeguard: ${token}`);
}
assert(menu.includes('routerLink="/organization/unit-locations-canonical"') &&
  quick.includes("resource: 'unit-locations-canonical'"), 'canonical view not available from ORG navigation');
console.log('PASS Angular canonical review, risk badges, standalone routing and legacy cross-check');
assert(routerConfig.includes('withHashLocation()') &&
  installGuide.includes('http://localhost:8091/#/organization/unit-locations-canonical') &&
  installGuide.includes('http://localhost:8091/#/organization/unit-locations-review') &&
  !installGuide.includes('http://localhost:8091/organization/unit-locations-canonical'),
  'browser deep links must include # when Angular uses hash routing');
console.log('PASS hash router and published browser URLs agree');

const ddl = read('database/oracle/org/unit-location-consolidation/02-ORG-UNIT-LOCATIONS-ADD-COLUMNS-OPT-IN.sql');
const dml = read('database/oracle/org/unit-location-consolidation/03-ORG-UNIT-LOCATIONS-BACKFILL-OPT-IN.sql');
assert(/DEFINE\s+APPLY_CHANGES\s*=\s*NO/.test(ddl) && /DEFINE\s+APPLY_CHANGES\s*=\s*NO/.test(dml), 'R1 opt-in defaults changed');
assert(ddl.includes('LEGACY_BACKFILL_FLAG NUMBER(1,0) DEFAULT 0 NOT NULL'), 'canonical schema prerequisites changed');
console.log('PASS R1 DDL and backfill remain opt-in; R2-A adds zero SQL changes');
console.log('ORG_UNIT_LOCATION_R2A_VERIFY_PASS');
