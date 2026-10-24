#!/usr/bin/env node
// Source-only regression checks. Does not connect to Oracle or change any files.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = name => fs.readFileSync(path.join(root, name), 'utf8');
const assert = (ok, message) => { if (!ok) throw Error(message); };
try {
  const menu = read('frontend/src/app/features/organization/organization-menu.component.ts');
  const menuHtml = read('frontend/src/app/features/organization/organization-menu.component.html');
  const shell = read('frontend/src/app/layout/app-shell.component.html');
  const desc = read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
  const levelUi = read('frontend/src/app/features/organization/organization-grade-levels.component.html');
  const levelComponent = read('frontend/src/app/features/organization/organization-grade-levels.component.ts');
  const levelService = read('backend/src/main/java/com/behsazan/corebanking/organization/grade/GradeService.java');
  const refUi = read('frontend/src/app/features/reference-data/presentation/reference-page.component.html');
  const unitPicker = read('frontend/src/app/features/organization/organization-unit-picker.component.ts');
  const map = read('frontend/src/app/features/organization/organization-coordinate-map-picker.component.ts');
  for (const hidden of ['organization-unit-classifications', 'organization-unit-relationship-rules',
    'foreign-exchange-service-profiles', 'organization-unit-geo-coverages']) {
    const menuBlocks = [...menu.matchAll(/resources:\s*\[([\s\S]*?)\]/g)].map(x => x[1]);
    assert(!menuBlocks.some(block => block.includes(`'${hidden}'`)), `${hidden} in menu`);
    assert(desc.includes(`"${hidden}"`), `${hidden} descriptor was removed`);
  }
  assert([...menu.split('readonly groups:')[1].split('readonly gradeTools')[0].matchAll(/title: '[^']+', description:/g)].length === 6, 'ORG menu must have 6 topic groups');
  assert(menuHtml.includes('quickLinks') && menuHtml.includes('toggleGroup(group)'), 'quick access/collapsible groups missing');
  assert(!menuHtml.includes('شبکه') && !shell.includes('شبکه شعب') && !desc.includes('"توسعه شبکه"'), 'obsolete UI wording remains');
  console.log('PASS navigation: 6 groups, quick links, 4 hidden links, no unwanted network wording');

  assert(levelUi.includes('value="درجه‌بندی شعب" readonly'), 'grade scheme is not readonly');
  assert(!levelUi.includes('[(ngModel)]="form.gradeSchemeCode"'), 'grade scheme still editable');
  assert(levelComponent.includes("gradeSchemeCode: 'BRANCH_STANDARD'"), 'Angular does not enforce scheme');
  assert(levelService.includes('"BRANCH_STANDARD".equals(input.gradeSchemeCode())'), 'backend does not enforce scheme');
  console.log('PASS grade scheme fixed in UI and service without introducing approval workflow');

  assert(desc.includes('stringSelect("reasonCode"') && refUi.includes('lifecycleReasonOptions(field)'), 'reason combo missing');
  assert(unitPicker.includes('ControlValueAccessor') && unitPicker.includes("lookup('organization-units'")
    && unitPicker.includes('text, 80'), 'unit picker is not server-side searchable');
  assert(refUi.includes('<app-organization-unit-picker'), 'generic ORG unit selection not searchable');
  assert(refUi.includes('<app-persian-date-input') && refUi.includes('[withTime]="true"'), 'ORG date inputs are not Persian');
  assert(refUi.includes('class="org-technical-details"'), 'technical metadata is not collapsible');
  assert(read('frontend/src/app/features/reference-data/presentation/reference-page.component.scss').includes('.org-page .workspace.editor-open'),
    'ORG-specific editor layout missing');
  console.log('PASS lifecycle reasons, ORG unit search, editor layout');

  for (const token of ['map-picker-backdrop','coordinatesSelected.emit','pointerMove',
    'basemaps.cartocdn.com', 'carto.com/attributions', 'onTileError()'])
    assert(map.includes(token), `map picker missing ${token}`);
  assert(!map.includes('tile.openstreetmap.org'), 'old blocked tile endpoint remains in picker');
  assert(refUi.includes('openLocationCoordinatePicker()') && refUi.includes('applyLocationCoordinates($event)'), 'location map writeback missing');
  console.log('PASS wide coordinate picker, alternative tile provider, manual fallback');

  const physical = [
    ['point-of-service-types','SERVICE_POINT_TYPES'],
    ['points-of-service','SERVICE_POINTS'],
    ['organization-unit-point-of-service-assignments','ORGANIZATION_UNIT_SERVICE_POINT_ASSIGNMENTS'],
    ['point-of-service-contact-points','SERVICE_POINT_CONTACT_POINTS']
  ];
  for (const [resource, table] of physical) {
    assert(desc.includes(`"${resource}"`), `public resource changed unexpectedly: ${resource}`);
    assert(desc.includes(`schemaName, "${table}",`), `physical table mapping missing: ${table}`);
  }
  const ddl = read('database/oracle/org/service-point/ORG-SERVICE-POINT-RENAME-OPT-IN.sql');
  assert(ddl.includes('DEFINE APPLY_CHANGES = NO') && ddl.includes("IF v_mode='NO' THEN"), 'migration is not opt-in');
  assert(!/\b(?:DROP TABLE|DELETE FROM|TRUNCATE TABLE)\b/i.test(ddl), 'migration includes destructive statements');
  for (const [, table] of physical) assert(ddl.includes(table), `migration missing ${table}`);
  console.log('PASS 4 retained service-point table aliases + historical default-no rename migration');
  console.log('ORG_UX_R2_VERIFY_PASS');
} catch (error) {
  console.error('ORG_UX_R2_VERIFY_FAIL:', error.message);
  process.exitCode = 1;
}
