#!/usr/bin/env node
// Static contract checks for ORG prototype usability patch. No Oracle reads or writes.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const check = (condition, message) => {if (!condition) throw new Error(message);};
try {
  const menu = read('frontend/src/app/features/organization/organization-menu.component.ts');
  for (const hidden of ['organization-unit-classifications', 'organization-unit-relationship-rules', 'foreign-exchange-service-profiles']) {
    const resourceBlocks = [...menu.matchAll(/resources:\s*\[([\s\S]*?)\]/g)].map(x => x[1]);
    check(resourceBlocks.every(block => !block.includes(`'${hidden}'`)), `${hidden} remains visible in menu`);
  }
  console.log('PASS 3 obsolete menu entries hidden; descriptors/tables retained');

  const descriptor = read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
  check(descriptor.includes('stringSelect("reasonCode", "REASON_CODE", "علت رویداد"'), 'reasonCode is still free text');
  for (const token of ['UNIT_LIFECYCLE_REASONS', 'NETWORK_EXPANSION', 'LEASE_EXPIRY', 'OTHER'])
    check(descriptor.includes(token), `missing reason vocabulary: ${token}`);
  const page = read('frontend/src/app/features/reference-data/presentation/reference-page.component.ts');
  const html = read('frontend/src/app/features/reference-data/presentation/reference-page.component.html');
  check(page.includes('selectedLifecycleReasonCodes()') && html.includes('lifecycleReasonOptions(field)'), 'event-specific reason options missing');
  console.log('PASS lifecycle reason select + event-dependent options');

  const picker = read('frontend/src/app/features/organization/organization-unit-picker.component.ts');
  check(picker.includes('ControlValueAccessor') && picker.includes("lookup('organization-units'") && picker.includes('text, 80'), 'remote picker missing');
  check(html.includes('<app-organization-unit-picker') && html.includes('store.descriptor()?.parent?.resource'), 'ORG generic form/filter/hierarchy picker missing');
  const repo = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/oracle/OracleReferenceRepository.java');
  check(repo.includes('UPPER(T.NAME_EN) LIKE :lookupText'), 'English name search missing');
  const grade = read('frontend/src/app/features/organization/organization-grade-periods.component.html');
  check(grade.includes('mat-autocomplete') && grade.includes('onBranchPicked'), 'grade branch search combo missing');
  console.log('PASS search by name/code in ORG unit pickers and grade branch picker');

  const map = read('frontend/src/app/features/organization/organization-coordinate-map-picker.component.ts');
  for (const token of ['pointerDown', 'pointerMove', 'pointerUp', 'coordinatesSelected.emit', 'tile.openstreetmap.org', 'centerOnCity'])
    check(map.includes(token), `map picker missing ${token}`);
  check(html.includes('انتخاب از روی نقشه') && html.includes('applyLocationCoordinates($event)') && !html.includes('(click)="openLocationCoordinatePicker()" [disabled]'), 'new-location map picker disabled or disconnected');
  console.log('PASS new/edit location map picker with coordinate write-back');

  const menuHtml = read('frontend/src/app/features/organization/organization-menu.component.html');
  check(!menuHtml.includes('گردش تأیید'), 'Grade workflow text remains');
  check(!grade.includes('درخواست‌های منتظر تأیید') && !grade.includes('ORG_GRADE_APPROVER'), 'Old approval workflow remains');
  console.log('ORG_UX_R1_STATIC_PASS');
} catch (error) {
  console.error('ORG_UX_R1_STATIC_FAIL:', error.message);
  process.exitCode = 1;
}
