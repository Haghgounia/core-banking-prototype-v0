'use strict';
// CLT-R3D: fast, platform-portable verifier of the analyst preview / zero-write boundary.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(process.argv[2]||path.join(__dirname,'../../..'));
function read(rel){return fs.readFileSync(path.join(root,rel),'utf8');}
const base='frontend/src/app/features/collateral/';
const panel=read(base+'collateral-insurance-panel.component.ts');
const html=read(base+'collateral-insurance-panel.component.html');
const uat=read(base+'collateral-uat-workbench.component.ts');
const uatHtml=read(base+'collateral-uat-workbench.component.html');
const policy=read(base+'clt-r3d-insurance-form-preview-policy.ts');
const reference=read(base+'clt-reference.service.ts');
const checks=[];
function check(name,condition){assert.ok(condition,'CLT_R3D_STATIC_FAIL '+name);checks.push(name);console.log('PASS | '+name);}
check('one production insurance form reused in UAT',uatHtml.includes('<app-collateral-insurance-panel [analystPreview]="true"') && html.includes('data-clt-r3d-analyst-preview'));
check('no fake collateral entity for preview',/collateral:\s*CollateralRow\s*\|\s*null\s*=\s*null/.test(panel) && !/collateral\s*=\s*\{\s*COLLATERAL_ID/.test(uat));
check('same field template is reused for production',html.includes("(click)=\"validate()\"") && html.includes("(click)=\"reviewPreviewSyntax()\""));
check('local GET reference calls not seed',/this\.references\.rows\('INSURANCE_COMPANY', 0, 50\)/.test(panel) && /this\.references\.rows\('COLLATERAL_INSURANCE_TYPE', 0, 50\)/.test(panel));
check('active-only insurers counted explicitly',/row\['STATUS'\] === 'ACTIVE'/.test(panel));
check('incomplete page never becomes false zero active',/page\.items\.length === page\.totalElements/.test(panel) && /\? page\.items\.filter/.test(panel));
check('GET request bound to actual catalogue',reference.includes('this.http.get<CltReadPage>') && !reference.includes('mock-insurers'));
check('preview load never calls per-collateral APIs',/async load\(page = 0\): Promise<void> \{\s*if \(this\.analystPreview\) return;/.test(panel));
check('preview renewal never calls per-collateral API',/async loadRenewals\(\): Promise<void> \{\s*if \(this\.analystPreview\) return;/.test(panel));
check('preview validation never POSTs',/async validate\(\): Promise<void> \{\s*if \(this\.analystPreview\) return;/.test(panel));
check('preview request never POSTs',/async submitRequest\(\): Promise<void> \{\s*if \(this\.analystPreview \|\| !this\.canRequest\(\)\) return;/.test(panel));
check('preview canRequest always false',/return !this\.analystPreview && this\.validInput\(\)/.test(panel));
check('preview reference selectors cannot treat inactive candidates as operational',html.includes('[disabled]="analystPreview"') && !/previewInsurers\(\)\?\.items/.test(html));
check('preview data failures reported not converted to zero',/خواندن منابع بیمه وثیقه ناموفق است/.test(panel) && html.includes('role="alert">{{previewError()}}'));
check('syntax check explicitly not bank validation',html.includes('کنترل شکلی فرم (فقط مرورگر)') && html.includes('به معنی اعتبار بیمه‌نامه'));
check('R3D UAT scenarios do not alter canonical UAT-01..10',uat.includes('const INSURANCE_UAT_R3D') && !read(base+'collateral-r6a-catalog.ts').includes('R3-INS-'));
check('eight independent insurer UAT cases', (uat.match(/id:'R3-INS-\d\d'/g)||[]).length===8);
check('UAT results local and exportable',uat.includes('insuranceResults=signal') && uat.includes('exportInsuranceUat') && uatHtml.includes('دریافت گزارش JSON آزمون بیمه'));
check('no Oracle write in preview component',!panel.includes('this.references.propose(') && !panel.includes('this.references.decide('));
check('no IAM change introduced by R3D',!panel.includes('Iam') && !uat.includes('Iam'));
check('R3D docs clarify non-operational boundary',read('docs/collateral/CLT-R3D-INSURANCE-ANALYST-UAT-FA.md').includes('G-04') && read('docs/collateral/CLT-R3D-INSURANCE-ANALYST-UAT-FA.md').includes('G-06'));

function typescript(){
 const candidates=[path.join(root,'frontend/node_modules/typescript'), 'typescript'];
 for(const candidate of candidates){try{return require(candidate);}catch{}}
 return null;
}
const ts=typescript();
if(ts){
 const script=ts.transpileModule(policy,{fileName:'clt-r3d-insurance-form-preview-policy.ts',
  reportDiagnostics:true,compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}});
 check('pure preview policy TypeScript syntax',!script.diagnostics?.length);
 const sandbox={exports:{}};const vm=require('node:vm');vm.runInNewContext(script.outputText,sandbox,{timeout:1000});
 const validate=sandbox.exports.cltR3dPreviewIssues;
 check('pure preview policy executable',typeof validate==='function');
 const ok={insuranceNumber:'INS-1001',depositNumber:'SAFE-01',insuranceAmount:'100.0001',receivedAmount:'0',
  taxAmount:'',validFrom:'2026-11-07',validTo:'2026-11-08',justification:'Analyst preview only'};
 const issues=delta=>validate({...ok,...delta});
 check('preview accepts valid syntax without claiming eligibility',issues({}).length===0);
 check('reject blank insurance number',issues({insuranceNumber:''}).length===1);
 check('reject whitespace in insurance number',issues({insuranceNumber:'A 123'}).length===1);
 check('reject 21-char insurance number',issues({insuranceNumber:'X'.repeat(21)}).length===1);
 check('reject absent deposit reference',issues({depositNumber:''}).length===1);
 check('reject zero coverage',issues({insuranceAmount:'0.0000'}).length===1);
 check('reject negative amount',issues({insuranceAmount:'-10'}).length===1);
 check('reject five decimal places',issues({insuranceAmount:'100.00001'}).length===1);
 check('allow non-negative received amount',issues({receivedAmount:'0'}).length===0);
 check('reject invalid tax format',issues({taxAmount:'-1'}).length===1);
 check('reject same start/end date',issues({validTo:'2026-11-07'}).length===1);
 check('reject invalid calendar day',issues({validFrom:'2026-02-30'}).length===1);
 check('reject inverted period',issues({validTo:'2026-11-06'}).length===1);
 check('reject inadequate request reason',issues({justification:'short'}).length===1);
}else{
 console.log('CLT_R3D_TYPESCRIPT_UNIT_NOT_RUN; local TypeScript unavailable; run after npm ci');
}
console.log(`CLT_R3D_STATIC_PASS; CHECKS=${checks.length}; PREVIEW_METHODS=GET_ONLY; WRITE_CALLS=NONE; ANALYST_UAT=NOT_RUN`);
