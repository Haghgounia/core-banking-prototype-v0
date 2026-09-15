import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=relative=>fs.readFileSync(path.join(root,relative),'utf8');
const exists=relative=>fs.existsSync(path.join(root,relative));
const version=read('VERSION').trim();
const semverAtLeast=(actual,minimum)=>{const a=actual.split('.').map(Number),b=minimum.split('.').map(Number);for(let i=0;i<3;i++){if((a[i]||0)>(b[i]||0))return true;if((a[i]||0)<(b[i]||0))return false;}return true;};
const pom=read('backend/pom.xml');
const pkg=JSON.parse(read('frontend/package.json'));
const html=read('frontend/src/app/features/cif/party-contact-address.component.html');
const ts=read('frontend/src/app/features/cif/party-contact-address.component.ts');
const service=read('backend/src/main/java/com/behsazan/corebanking/cif/application/CifService.java');
const repo=read('backend/src/main/java/com/behsazan/corebanking/cif/oracle/CifRepository.java');
const phase7=read('tools/verify-dps2-deposit-opening-phase7.mjs');
const buildCmd=read('build-production.cmd');
const buildSh=read('build-production.sh');
const checks=[
 [semverAtLeast(version,'0.7.1'),`VERSION must be >= 0.7.1, got ${version}`],
 [pom.includes(`<version>${version}-SNAPSHOT</version>`)&&pkg.version===version,'backend/frontend version sync is incomplete'],
 [html.includes('formControlName="provinceCode"')&&html.includes('formControlName="countyCode"')&&html.includes('formControlName="districtCode"'),'geography selects must persist codes directly'],
 [html.includes('<mat-option [value]="option.code">{{ option.label }}</mat-option>'),'geography selects must submit reference codes rather than surrogate IDs'],
 [!html.includes('[value]="selectedCountyId()"'),'county UI must not have a second independent display value'],
 [ts.includes("this.provinces().find(item => item.code === code)")&&ts.includes("this.counties().find(item => item.code === code)")&&ts.includes("this.districts().find(item => item.code === code)"),'UI code-to-parent-ID synchronization is incomplete'],
 [ts.includes("!this.addressForm.controls.countyCode.value")&&ts.includes('استان، شهرستان و شهر باید از فهرست‌های جغرافیایی انتخاب شوند'),'client-side Iran address guard is missing'],
 [service.includes('repository.activeCountryCodeExists(r.countryCode())'),'server-side country validation is missing'],
 [service.includes('errors.put("countyCode", "برای نشانی داخل ایران، شهرستان الزامی است.")'),'server-side COUNTY_CODE required guard is missing'],
 [service.includes('repository.activeAddressGeographyPathExists'),'server-side geography hierarchy validation is missing'],
 [repo.includes('activeAddressGeographyPathExists')&&repo.includes('.CITIES C')&&repo.includes('.DISTRICTS D')&&repo.includes('.COUNTIES CNT')&&repo.includes('.PROVINCES P')&&repo.includes('.COUNTRIES CO'),'GEO hierarchy SQL validation is incomplete'],
 [repo.includes('CNT.COUNTY_CODE=:countyCode')&&repo.includes('C.CITY_CODE=:cityCode')&&repo.includes('IS_ACTIVE=1'),'GEO hierarchy code/active checks are incomplete'],
 [phase7.includes("semverAtLeast(version,'0.7.0')"),'Phase 7 historical verifier must be forward-compatible'],
 [buildCmd.includes('verify-cif-address-hotfix-071.mjs')&&buildSh.includes('verify-cif-address-hotfix-071.mjs'),'production builds must execute CIF address hotfix verifier'],
 [exists('docs/CIF-0.7.1-ADDRESS-COUNTY-CODE-HOTFIX-QA.md')&&exists('docs/install/INSTALL-0.7.1-FA.txt')&&exists('docs/patches/PATCH-0.7.1-README-FA.txt'),'0.7.1 QA/install/patch docs are incomplete']
];
const failed=checks.filter(([ok])=>!ok).map(([,msg])=>msg);
if(failed.length){console.error('CIF 0.7.1 address COUNTY_CODE hotfix verification FAILED:');for(const msg of failed)console.error(`- ${msg}`);process.exit(1)}
console.log(`CIF 0.7.1 address COUNTY_CODE hotfix verification OK (${checks.length}/${checks.length}).`);
