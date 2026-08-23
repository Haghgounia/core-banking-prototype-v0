import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

// fileURLToPath is required on Windows: URL.pathname returns /D:/... and
// path.resolve() would otherwise turn it into D:\\D:\\... .
const scriptFile = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(scriptFile), '..');
const cifDir = path.join(root, 'frontend', 'src', 'app', 'features', 'cif');
const shellTs = fs.readFileSync(path.join(root, 'frontend', 'src', 'app', 'layout', 'app-shell.component.ts'), 'utf8');
const shellHtml = fs.readFileSync(path.join(root, 'frontend', 'src', 'app', 'layout', 'app-shell.component.html'), 'utf8');
const shellScss = fs.readFileSync(path.join(root, 'frontend', 'src', 'app', 'layout', 'app-shell.component.scss'), 'utf8');
const styles = fs.readFileSync(path.join(root, 'frontend', 'src', 'styles.scss'), 'utf8');
const version = fs.readFileSync(path.join(root, 'VERSION'), 'utf8').trim();
const generated = fs.readFileSync(path.join(root, 'frontend', 'src', 'app', 'features', 'system-specification', 'system-version.generated.ts'), 'utf8');

const failures = [];
const htmlFiles = fs.readdirSync(cifDir).filter(name => name.endsWith('.component.html'));
const streamTokens = ['class="records"', 'class="record"', 'class="record-row"', 'class="source-list"', 'class="mini-row"'];
let tableCount = 0;

for (const name of htmlFiles) {
  const text = fs.readFileSync(path.join(cifDir, name), 'utf8');
  for (const token of streamTokens) {
    if (text.includes(token)) failures.push(`${name}: stream renderer remains (${token})`);
  }
  for (const tag of ['table', 'thead', 'tbody', 'tr', 'th', 'td']) {
    const open = (text.match(new RegExp(`<${tag}(?:\\s|>)`, 'g')) ?? []).length;
    const close = (text.match(new RegExp(`</${tag}>`, 'g')) ?? []).length;
    if (open !== close) failures.push(`${name}: unbalanced <${tag}> ${open}/${close}`);
  }
  tableCount += (text.match(/<table class="(?:db-grid|record-table)/g) ?? []).length;
}

const minimumTables = 60;
if (tableCount < minimumTables) failures.push(`CIF persisted-grid table count ${tableCount} is below ${minimumTables}`);

const expectedTables = new Map([
  ['party-contact-address.component.html', 3],
  ['party-financial-employment.component.html', 5],
  ['party-identifiers-documents.component.html', 2],
  ['party-classifications.component.html', 1],
  ['party-relationships.component.html', 3],
  ['party-roles.component.html', 2],
  ['party-kyc-risk.component.html', 4],
  ['party-consents-preferences.component.html', 3],
  ['party-lifecycle-merge.component.html', 2],
  ['party-360.component.html', 40]
]);
for (const [name, minimum] of expectedTables) {
  const text = fs.readFileSync(path.join(cifDir, name), 'utf8');
  const count = (text.match(/<table class="(?:db-grid|record-table)/g) ?? []).length;
  if (count < minimum) failures.push(`${name}: ${count} grids, expected at least ${minimum}`);
}


// Verify fields used by Party360 persisted-grid loops exist on their declared response records.
const modelsText = fs.readFileSync(path.join(cifDir, 'cif.models.ts'), 'utf8');
const party360Text = fs.readFileSync(path.join(cifDir, 'party-360.component.html'), 'utf8');
const interfaceFields = new Map();
for (const match of modelsText.matchAll(/export interface\s+(\w+)\s*\{([^}]*)\}/gs)) {
  interfaceFields.set(match[1], new Set([...match[2].matchAll(/readonly\s+(\w+)\s*:/g)].map(x => x[1])));
}
const mainRecordTypes = new Map(Object.entries({
  names: 'PartyNameRecord', identifiers: 'PartyIdentifierRecord', addresses: 'PartyAddressRecord', contacts: 'ContactPointRecord',
  financialProfiles: 'FinancialProfileRecord', incomeSources: 'PartyIncomeSourceRecord', employments: 'PartyEmploymentRecord', licenses: 'PartyLicenseRecord',
  assetLiabilities: 'PartyAssetLiabilityRecord', classifications: 'PartyClassificationRecord', relationships: 'PartyRelationshipRecord',
  beneficialOwnerships: 'BeneficialOwnershipRecord', authorities: 'PartyAuthorityRecord', roles: 'PartyRoleRecord', customers: 'PartyCustomerRecord',
  kycCases: 'KycCaseRecord', documents: 'PartyDocumentRecord', riskAssessments: 'RiskAssessmentRecord', screenings: 'ScreeningResultRecord',
  externalInquiries: 'ExternalInquiryRecord', consents: 'PartyConsentRecord', communicationPreferences: 'CommunicationPreferenceRecord',
  generalPreferences: 'PartyGeneralPreferenceRecord', statusHistory: 'PartyStatusHistoryRecord', mergeHistory: 'PartyMergeHistoryRecord'
}));
const sourceRecordTypes = new Map(Object.entries({
  productHoldings: 'PartyProductHolding360Record', productRestrictions: 'PartyProductRestriction360Record', operationLimits: 'PartyOperationLimit360Record',
  interactions: 'PartyInteraction360Record', journeyEvents: 'PartyJourneyEvent360Record', complaints: 'PartyComplaint360Record', alerts: 'PartyAlertCase360Record',
  complaintStatusHistory: 'PartyComplaintStatus360Record', segmentMemberships: 'PartySegmentMembership360Record', valueScores: 'PartyValueScore360Record',
  metricSnapshots: 'PartyMetricSnapshot360Record', recommendations: 'PartyRecommendation360Record', organizationOfficers: 'OrganizationOfficer360Record',
  groupMemberships: 'PartyGroupMembership360Record', signatureSpecimens: 'SignatureSpecimen360Record', registrationRequests: 'PartyRegistrationRequest360Record',
  auditEvents: 'AuditEvent360Record'
}));
for (const line of party360Text.split(/\r?\n/)) {
  const loop = line.match(/@for \((row|x) of m\.(?:(source360)\.)?(\w+);/);
  if (!loop) continue;
  const [, variable, sourceMarker, arrayName] = loop;
  const interfaceName = (sourceMarker ? sourceRecordTypes : mainRecordTypes).get(arrayName);
  if (!interfaceName) continue;
  const fields = interfaceFields.get(interfaceName) ?? new Set();
  const used = [...line.matchAll(new RegExp(`\\b${variable}\\.(\\w+)`, 'g'))].map(x => x[1]);
  for (const field of used) {
    if (!fields.has(field)) failures.push(`party-360.component.html: ${arrayName}.${field} is not declared by ${interfaceName}`);
  }
}

if (!shellHtml.includes('[class.collapsed]="sidebarCollapsed()"')) failures.push('Sidebar collapse binding missing');
if (!shellHtml.includes('dir="rtl" autosize')) failures.push('MatSidenav autosize is required for dock-width recalculation');
if ((shellHtml.match(/\(click\)="toggleSidebar\(\)"/g) ?? []).length < 2) failures.push('Sidebar toggle must be available in rail and topbar');
if (!shellTs.includes("localStorage.setItem('core-banking.sidebar.collapsed'")) failures.push('Sidebar state persistence missing');
if (!shellTs.includes('return stored === null ? true')) failures.push('Sidebar must default to compact mode on first use');
if (!shellScss.includes('.sidebar.collapsed { width: 76px; }')) failures.push('Compact sidebar width missing');
if (!shellScss.includes('overflow-x: clip')) failures.push('Content horizontal overflow guard missing');
if (!styles.includes('overflow-x: visible !important')) failures.push('Desktop grid no-horizontal-scroll rule missing');
if (!styles.includes('@media (max-width: 820px)')) failures.push('Small-screen grid fallback missing');
if (!generated.includes(`release: "${version}"`)) failures.push(`Generated system version does not match VERSION (${version})`);

if (failures.length) {
  console.error('CIF persisted-grid verification FAILED:');
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}

console.log(`CIF persisted-grid verification OK: ${tableCount} CIF grids, no stream/card record renderers, dockable sidebar verified.`);
