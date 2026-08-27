import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');

const registry = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceRegistry.java');
const service = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/application/Calendar2ReferenceService.java');
const repository = read('backend/src/main/java/com/behsazan/corebanking/calendar2/reference/oracle/Calendar2ReferenceRepository.java');
const template = read('frontend/src/app/features/calendar2-reference/calendar2-reference-page.component.html');

const checks = [
  [registry.includes('lookupText("countryCode", "COUNTRY_CODE", "کشور", "geo-countries"'), 'country is lookup-backed'],
  [registry.includes('lookupText("timeZone", "TIME_ZONE", "منطقه زمانی", "iana-time-zones"'), 'time zone is lookup-backed'],
  [registry.includes('text("organizationId", "ORGANIZATION_ID", "شناسه سازمان"'), 'organization id stays text'],
  [repository.includes('COUNTRY_ISO_CODE AS VALUE_COL') && repository.includes('COUNTRY_NAME AS LABEL_COL') && repository.includes('COUNTRIES'), 'GEO.COUNTRIES country lookup'],
  [repository.includes('IS_ACTIVE = 1') && repository.includes('IS_DEFAULT_COUNTRY DESC'), 'active/default country behavior'],
  [repository.includes('ZoneId.getAvailableZoneIds()') && repository.includes('Asia/Tehran'), 'IANA time-zone lookup'],
  [service.includes('activeCountryCodeExists') && service.includes('ZoneId.of(timeZone)'), 'server-side country/time-zone validation'],
  [template.includes("field.lookupResource === 'geo-countries'") && template.includes("field.lookupResource === 'iana-time-zones'"), 'Angular lookup hints']
];

const failed = checks.filter(([ok]) => !ok).map(([, label]) => label);
if (failed.length) throw new Error(`CAL2 business-calendar lookup verification failed: ${failed.join(', ')}`);
console.log('CAL2 business-calendar lookup verification OK: country from GEO.COUNTRIES, IANA time zones, organization ID remains text.');
