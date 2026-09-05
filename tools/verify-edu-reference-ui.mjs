import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');

const provider = read('backend/src/main/java/com/behsazan/corebanking/referencedata/general/descriptor/NameRomanizationDescriptorProvider.java');
const menuTs = read('frontend/src/app/features/reference-menu/reference-menu.component.ts');
const menuHtml = read('frontend/src/app/features/reference-menu/reference-menu.component.html');
const hubTs = read('frontend/src/app/features/reference-hub/reference-hub.component.ts');
const pageTs = read('frontend/src/app/features/reference-data/presentation/reference-page.component.ts');
const pageHtml = read('frontend/src/app/features/reference-data/presentation/reference-page.component.html');
const gateway = read('frontend/src/app/features/reference-data/infrastructure/http-reference.gateway.ts');
const controller = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/web/ReferenceController.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/referencedata/management/oracle/OracleReferenceRepository.java');

const checks = [
  ['romanization dictionary is VOCABULARY', provider.includes('"name-romanization-dictionary", "VOCABULARY"')],
  ['name affix dictionary is VOCABULARY', provider.includes('"name-affix-dictionary", "VOCABULARY"')],
  ['vocabulary group exists', menuTs.includes("key: 'VOCABULARY'") && menuHtml.includes("group.key === 'VOCABULARY'")],
  ['vocabulary backward compatibility exists', menuTs.includes("this.catalog.group('VOCABULARY')") && menuTs.includes("this.catalog.group('GENERAL')")],
  ['name conversion tool remains in vocabulary group', menuHtml.includes('تبدیل نام فارسی به انگلیسی') && menuHtml.includes('/reference-data/general/name-romanization-tool')],
  ['general hub count includes vocabulary', hubTs.includes("'VOCABULARY'")],
  ['general hub count includes canonical EDU', hubTs.includes("'EDU_REFERENCE'")],
  ['EDU levels filters exist', pageTs.includes("'edu-education-levels': ['educationSystemCode', 'isSelectable', 'activeFlag']")],
  ['EDU fields filters exist', pageTs.includes("'edu-education-fields': ['parentEducationFieldsId', 'educationSystemCode', 'fieldNodeTypeCode', 'isSelectable', 'activeFlag']")],
  ['EDU field-level filters exist', pageTs.includes("'edu-education-field-levels': ['educationFieldsId', 'educationLevelsId', 'sourceId', 'activeFlag']")],
  ['EDU institution filters exist', pageTs.includes("'edu-education-institutions': ['institutionTypeCode', 'educationSystemCode', 'institutionStatusCode', 'isDegreeGranting', 'activeFlag']")],
  ['EDU source filters exist', pageTs.includes("'edu-education-sources': ['sourceTypeCode', 'sourceYear', 'activeFlag']")],
  ['EDU mapping filters exist', pageTs.includes("'edu-education-source-mappings': ['sourceId', 'entityTypeCode', 'mappingTypeCode', 'matchStatusCode', 'activeFlag']")],
  ['advanced filter UI exists', pageHtml.includes('فیلترهای تخصصی') && pageHtml.includes('[formGroup]="advancedFilterForm"')],
  ['lookup filters render labels', pageHtml.includes('advancedFilterLookupOptions()[field.apiName]') && pageHtml.includes('{{ option.label }}')],
  ['frontend sends filter parameters', gateway.includes('`filter.${field}`')],
  ['backend parses filter parameters', controller.includes('key.startsWith("filter.")')],
  ['repository applies descriptor-safe filters', repository.includes('descriptor.optionalField(entry.getKey())') && repository.includes('queryFilterValue(field, entry.getValue())')]
];

const failed = checks.filter(([, ok]) => !ok).map(([name]) => name);
if (failed.length) {
  throw new Error(`EDU/reference menu verification failed: ${failed.join(', ')}`);
}
console.log(`EDU filters + vocabulary menu static contract: OK (${checks.length} checks)`);
