import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const requiredFiles = [
  'backend/src/main/java/com/behsazan/corebanking/referencedata/general/descriptor/NameRomanizationDescriptorProvider.java',
  'database/oracle/geo/ddl/04_name_romanization_dictionary.sql',
  'docs/GEO-0.3.71-NAME-ROMANIZATION-CRUD-QA.md'
];

for (const file of requiredFiles) {
  if (!fs.existsSync(path.join(ROOT, file))) throw new Error(`Missing required file: ${file}`);
}

const provider = fs.readFileSync(path.join(ROOT, requiredFiles[0]), 'utf8');
for (const token of [
  'NAME_ROMANIZATION_DICTIONARY', 'NAME_AFFIX_DICTIONARY',
  'name-romanization-dictionary', 'name-affix-dictionary',
  'GOVERNANCE_STATUS_CODE', 'AUTO_FILL_ALLOWED', 'CONTEXT_SENSITIVE'
]) {
  if (!provider.includes(token)) throw new Error(`Descriptor contract missing: ${token}`);
}

const ddl = fs.readFileSync(path.join(ROOT, requiredFiles[1]), 'utf8');
for (const token of [
  'CREATE TABLE GEO.NAME_ROMANIZATION_DICTIONARY',
  'CREATE TABLE GEO.NAME_AFFIX_DICTIONARY',
  'CREATE OR REPLACE FUNCTION GEO.FN_NORMALIZE_NAME',
  'CREATE OR REPLACE PACKAGE GEO.PKG_NAME_ROMANIZATION'
]) {
  if (!ddl.includes(token)) throw new Error(`DDL contract missing: ${token}`);
}

const installer = fs.readFileSync(path.join(ROOT, 'database/oracle/geo/install-ddl.sql'), 'utf8');
if (!installer.includes('@@ddl/04_name_romanization_dictionary.sql')) {
  throw new Error('GEO installer does not include name romanization DDL.');
}

const fieldType = fs.readFileSync(path.join(ROOT, 'backend/src/main/java/com/behsazan/corebanking/referencedata/descriptor/domain/FieldType.java'), 'utf8');
const frontendTypes = fs.readFileSync(path.join(ROOT, 'frontend/src/app/core/models/catalog.model.ts'), 'utf8');
const formHtml = fs.readFileSync(path.join(ROOT, 'frontend/src/app/features/reference-data/presentation/reference-page.component.html'), 'utf8');
if (!fieldType.includes('STRING_SELECT') || !frontendTypes.includes("'STRING_SELECT'") || !formHtml.includes("@case ('STRING_SELECT')")) {
  throw new Error('STRING_SELECT contract is incomplete across backend/frontend.');
}

const service = fs.readFileSync(path.join(ROOT, 'backend/src/main/java/com/behsazan/corebanking/referencedata/management/application/ReferenceService.java'), 'utf8');
for (const token of ['normalizePersianName', 'canonicalEnglishName', 'autoFillAllowed', 'autoApplyAllowed']) {
  if (!service.includes(token)) throw new Error(`Reference governance validation missing: ${token}`);
}

console.log('GEO name romanization CRUD static contract: OK');
