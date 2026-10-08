import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const fail = (message) => { console.error(`ORG_LOCATION_FORM_VERIFY_FAIL: ${message}`); process.exit(1); };
const pass = (message) => console.log(`PASS ${message}`);
const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) fail(`missing file: ${relative}`);
  return fs.readFileSync(file, 'utf8');
};

const provider = read('backend/src/main/java/com/behsazan/corebanking/organization/descriptor/OrganizationDescriptorProvider.java');
if (!provider.includes('lookup("geoEntityId", "GEO_ENTITY_ID", "شهر / موجودیت جغرافیایی GEO", "cities"')) fail('ORG.LOCATIONS city lookup is missing');
if (!provider.includes('new SelectOption("WGS84", "WGS 84 (EPSG:4326)")')) fail('WGS84 / EPSG:4326 option is missing');
pass('ORG.LOCATIONS descriptor');

const service = read('backend/src/main/java/com/behsazan/corebanking/organization/location/application/OrganizationLocationLookupService.java');
for (const token of ['PROVINCES p', 'COUNTIES co', 'DISTRICTS d', 'CITIES c', 'p.PROVINCE_ID = :provinceId', 'CITY_ENGLISH_NAME']) {
  if (!service.includes(token)) fail(`location lookup service missing token: ${token}`);
}
pass('province-filtered searchable city lookup');

const controller = read('backend/src/main/java/com/behsazan/corebanking/organization/location/web/OrganizationLocationLookupController.java');
if (!controller.includes('/api/v1/organization/location-lookup') || !controller.includes('@GetMapping("/cities")')) fail('location lookup endpoint missing');
pass('ORG-only location lookup endpoint');

const pageTs = read('frontend/src/app/features/reference-data/presentation/reference-page.component.ts');
for (const token of ['organizationLocationPage', 'locationProvinceControl', 'locationCitySearchControl', 'searchCities', 'locationMapEmbedUrl', 'openLocationMap']) {
  if (!pageTs.includes(token)) fail(`location UI logic missing token: ${token}`);
}
pass('ORG.LOCATIONS search/map UI logic');

const pageHtml = read('frontend/src/app/features/reference-data/presentation/reference-page.component.html');
for (const token of ['استان (فیلتر)', 'نام یا کد شهر را تایپ کنید', 'نمایش روی نقشه', 'OpenStreetMap']) {
  if (!pageHtml.includes(token)) fail(`location UI template missing token: ${token}`);
}
pass('ORG.LOCATIONS search/map template');

console.log('ORG_LOCATION_FORM_VERIFY_PASS');
