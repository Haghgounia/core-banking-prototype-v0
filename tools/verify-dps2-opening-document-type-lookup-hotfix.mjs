import fs from 'node:fs';

const ts = fs.readFileSync(new URL('../frontend/src/app/features/four-deposits/deposit-opening-wizard.component.ts', import.meta.url), 'utf8');
const html = fs.readFileSync(new URL('../frontend/src/app/features/four-deposits/deposit-opening-wizard.component.html', import.meta.url), 'utf8');
const checks = [
  ['reference resource is consumed', ts.includes("referenceLookup('dps2-document-type'")],
  ['document types signal exists', ts.includes('readonly documentTypes=signal<readonly CifLookupOption[]>')],
  ['lookup is loaded during component construction', ts.includes('void this.loadDocumentTypes();')],
  ['document type is a select', html.includes('<mat-select formControlName="documentType"')],
  ['reference options bind code', html.includes('[value]="option.code"')],
  ['free-text document type input removed', !html.includes('<input matInput formControlName="documentType">')],
  ['lookup failure is visible', html.includes('documentTypesError()')],
];
let failed = 0;
for (const [name, ok] of checks) {
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}`);
  if (!ok) failed++;
}
console.log(`DPS2_OPENING_DOCUMENT_TYPE_LOOKUP_PASS=${checks.length-failed}`);
console.log(`DPS2_OPENING_DOCUMENT_TYPE_LOOKUP_FAIL=${failed}`);
if (failed) process.exit(1);
console.log('DPS2_OPENING_DOCUMENT_TYPE_LOOKUP_STATIC_PASS');
