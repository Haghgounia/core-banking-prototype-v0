'use strict';
/** Execute installed ACTUAL R6E pure TypeScript policies. No network, DB, secrets or Angular runtime. */
const fs=require('fs'),path=require('path'),assert=require('assert'),vm=require('vm');
const root=path.resolve(process.argv[2]||'.');
let ts;
try{ts=require(path.join(root,'frontend/node_modules/typescript'))}
catch{try{ts=require('typescript')}catch{throw Error('TYPESCRIPT_MISSING_RUN_PROJECT_BUILD_FIRST')}}
const feature=path.join(root,'frontend/src/app/features/collateral');
const cache=new Map();let passed=0;
function test(fn){fn();passed++}
function load(name){
 const file=name.endsWith('.ts')?name:name+'.ts';
 if(cache.has(file))return cache.get(file).exports;
 const source=fs.readFileSync(path.join(feature,file),'utf8');
 const result=ts.transpileModule(source,{fileName:file,reportDiagnostics:true,compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}});
 if(result.diagnostics?.some(d=>d.category===ts.DiagnosticCategory.Error))throw Error('TS_PARSE:'+file);
 const module={exports:{}};cache.set(file,module);
 const sandbox={module,exports:module.exports,require:(relative)=>{
  if(!relative.startsWith('./'))throw Error('UNEXPECTED_IMPORT:'+relative);
  return load(relative.slice(2));
 },Error,Array,Number,String,Set,RegExp,JSON,Object};
 vm.runInNewContext(result.outputText,sandbox,{filename:file,timeout:1100});
 return module.exports;
}
try{
 const report=load('clt-r6e-report-contract');
 const detail=load('clt-r6e-detail-contract');
 const id='9999999999999999999';
 const overview={source:'ORACLE_CLT_READ_ONLY',scope:'JWT_COLLATERAL_BRANCH',branchCode:'BRANCH_TEST',
  totalCollaterals:3,byStatus:[{statusCode:'ACTIVE',count:2},{statusCode:'CLOSED',count:1}],
  activeExposures:[{currencyCode:'IRR',activeAllocationCount:1,activeAllocatedAmount:'20000.5000'}],
  allocationAuthorization:false,evidenceNote:'non-atomic read-only'};
 const events=[{eventId:'8888888888888888888',collateralNumber:id,fromStatus:null,toStatus:'ACTIVE',changedAt:null}];
 const reference={source:'ORACLE_CLT_REFERENCE',scope:'GLOBAL_TECHNICAL_CATALOG',activeTypes:2,activeGroups:5,hqAttestation:false};
 test(()=>assert.doesNotThrow(()=>report.cltAssertOverview(overview)));
 test(()=>assert.doesNotThrow(()=>report.cltAssertEvents(events,20)));
 test(()=>assert.doesNotThrow(()=>report.cltAssertEvents([],20)));
 test(()=>assert.doesNotThrow(()=>report.cltAssertReferenceSummary(reference)));
 const invalidOverview=[{...overview,source:'DEMO'},{...overview,scope:'ALL_BRANCHES'},
  {...overview,allocationAuthorization:true},{...overview,branchCode:''},
  {...overview,totalCollaterals:4},{...overview,totalCollaterals:2**53},
  {...overview,byStatus:[...overview.byStatus,...overview.byStatus]},
  {...overview,byStatus:[{statusCode:'ACTIVE',count:-1}]},
  {...overview,activeExposures:[{currencyCode:'IRR',activeAllocationCount:1,activeAllocatedAmount:100.2}]},
  {...overview,activeExposures:[{currencyCode:'IRR',activeAllocationCount:1,activeAllocatedAmount:'1.12345'}]},
  {...overview,activeExposures:[...overview.activeExposures,...overview.activeExposures]},
  {...overview,activeExposures:[{currencyCode:'IR',activeAllocationCount:1,activeAllocatedAmount:'10'}]},
  {...overview,evidenceNote:null}, null,[],{},
  {...overview,byStatus:[]},{...overview,activeExposures:null}];
 for(const input of invalidOverview)test(()=>assert.throws(()=>report.cltAssertOverview(input),/CLT_R6E_REPORT_CONTRACT_INVALID/));
 for(const input of [null,{},[{...events[0],eventId:Number(id)}],
  [{...events[0],toStatus:null}],
  [{...events[0],collateralNumber:9999999999999999999}],
  [...events,...events],
  [...events,{...events[0],eventId:'123'}],
  [{...events[0],changedAt:123}],
  [{...events[0],fromStatus:'INVALID-STATE'}]])
  test(()=>assert.throws(()=>report.cltAssertEvents(input,1),/CLT_R6E_REPORT_CONTRACT_INVALID/));
 for(const input of [null,{},[],{...reference,source:'LOCAL'},{...reference,hqAttestation:true},
  {...reference,activeGroups:-1},{...reference,activeTypes:1.1},{...reference,scope:'BANK_WIDE'}])
  test(()=>assert.throws(()=>report.cltAssertReferenceSummary(input),/CLT_R6E_REPORT_CONTRACT_INVALID/));
 const specs=detail.CLT_R6E_DETAIL_GETS;
 test(()=>assert.strictEqual(specs.length,11));
 test(()=>assert.strictEqual(new Set(specs.map(x=>x.key)).size,11));
 test(()=>assert.strictEqual(specs[0].key,'collateral-detail'));
 for(const spec of specs){
  test(()=>assert(!/\b(POST|PUT|PATCH|DELETE)\b/.test(spec.suffix)));
  test(()=>assert(detail.cltR6ePath(id,spec).startsWith('/api/v1/collaterals/'+id)));
 }
 test(()=>assert.strictEqual(detail.cltR6eId('۹۹۹۹۹۹۹۹۹۹۹۹۹۹۹۹۹۹۹'),id));
 test(()=>assert.strictEqual(detail.cltR6eId('٩٩٩٩٩٩٩٩٩٩٩٩٩٩٩٩٩٩٩'),id));
 test(()=>assert.strictEqual(detail.cltR6eId('  123  '),'123'));
 for(const bad of ['','0','-1','1.1','0001','12345678901234567890','abc'])
  test(()=>assert.strictEqual(detail.cltR6eId(bad),null));
 test(()=>assert.throws(()=>detail.cltR6ePath('0',specs[0]),/CLT_R6E_INVALID_ID/));
 const page=(src)=>({collateralId:id,collateralNumber:'123',source:src,page:0,size:2,items:[],totalElements:0});
 const fixtures={
  'collateral-detail':{COLLATERAL_ID:id,COLLATERAL_NUMBER:'123'},
  'sponsor-list':{...page('ORACLE_CLT_OWN_BRANCH'),sponsorRequired:false},
  'insurance-list':{...page('ORACLE_CLT_OWN_BRANCH'),insurable:true},
  'valuation-history':page('ORACLE_CLT_OWN_BRANCH_READ_ONLY'),
  'valuation-current':{collateralId:id,eligibleForNewAllocation:false,withinRecordedPeriod:false,blockers:[],unresolvedPolicyGaps:[],timeSource:'ORACLE'},
  'capacity-evidence':{collateralId:id,collateralNumber:'123',eligibleForNewAllocation:false,verifiedFreeCapacity:null,
   evidenceLevel:'READ_ONLY_NON_TRANSACTIONAL_ARITHMETIC_NOT_ALLOCATION_AUTHORIZATION',
   activeAllocationCount:0,allocationBreakdown:[],blockers:[]},
  'allocation-history':[],
  'allocation-readiness':{historyConfigured:true,financialUnallocationEnabled:false,finalReleaseEnabled:false,durableIdempotencyEnabled:false},
  'release-readiness':{evidenceReadsEnabled:true,finalReleaseWritesEnabled:false,legalHoldAttestationIntegrated:false,consumerClearanceIntegrated:false},
  'release-evidence':{snapshot:{collateralId:id,activeAllocationCount:0},releaseAuthorized:false,
   decision:'DENIED_FINAL_RELEASE_NOT_AUTHORIZED',blockers:[]},
  'release-history':[]
 };
 for(const spec of specs){
  const key=spec.key, good=fixtures[key];
  test(()=>assert.strictEqual(detail.cltR6eValid(key,good,id),true,'VALID:'+key));
  test(()=>assert.strictEqual(detail.cltR6eVerdict(key,200,good,id).state,'PASS'));
  test(()=>assert.strictEqual(detail.cltR6eVerdict(key,200,{},id).state,'CONTRACT_DRIFT'));
  for(const [st,state] of [[401,'BLOCKED_AUTH'],[403,'BLOCKED_AUTH'],[503,'READ_DISABLED'],[404,'NOT_FOUND_OR_DENIED'],
   [0,'UNAVAILABLE'],[408,'TIMEOUT'],[504,'TIMEOUT'],[500,'SERVER_ERROR'],[204,'SERVER_ERROR']])
   test(()=>assert.strictEqual(detail.cltR6eVerdict(key,st,null,id).state,state,'STATUS:'+key+':'+st));
 }
 for(const [key,bad] of [
  ['collateral-detail',{COLLATERAL_ID:9999999999999999999,COLLATERAL_NUMBER:'123'}],
  ['collateral-detail',{COLLATERAL_ID:'123',COLLATERAL_NUMBER:'123'}],
  ['sponsor-list',{...fixtures['sponsor-list'],source:'LOCAL_DEMO'}],
  ['sponsor-list',{...fixtures['sponsor-list'],totalElements:-1}],
  ['insurance-list',{...fixtures['insurance-list'],page:1}],
  ['valuation-history',{...fixtures['valuation-history'],source:'ORACLE_CLT_OWN_BRANCH'}],
  ['valuation-current',{...fixtures['valuation-current'],eligibleForNewAllocation:true}],
  ['capacity-evidence',{...fixtures['capacity-evidence'],verifiedFreeCapacity:'5000'}],
  ['capacity-evidence',{...fixtures['capacity-evidence'],eligibleForNewAllocation:true}],
  ['allocation-history',[{collateralId:id,allocationId:Number(id),used:true,allocatedAmount:'12'}]],
  ['allocation-readiness',{...fixtures['allocation-readiness'],financialUnallocationEnabled:true}],
  ['release-readiness',{...fixtures['release-readiness'],finalReleaseWritesEnabled:true}],
  ['release-evidence',{...fixtures['release-evidence'],releaseAuthorized:true}],
  ['release-evidence',{...fixtures['release-evidence'],snapshot:{collateralId:'888',activeAllocationCount:0}}],
  ['release-history',[{statusHistoryId:Number(id),toStatusCode:'RELEASED'}]]
 ])test(()=>assert.strictEqual(detail.cltR6eVerdict(key,200,bad,id).state,'CONTRACT_DRIFT',key));
 test(()=>assert.strictEqual(detail.cltR6eVerdict('allocation-readiness',200,{...fixtures['allocation-readiness'],historyConfigured:false},id).state,'READ_DISABLED'));
 test(()=>assert.strictEqual(detail.cltR6eVerdict('release-readiness',200,{...fixtures['release-readiness'],evidenceReadsEnabled:false},id).state,'READ_DISABLED'));
 const output=detail.cltR6eSafeExport([{key:'collateral-detail',label:'P II TEST',state:'PASS',status:200,message:id}],
  '2026-11-04T10:00:00.000Z');
 const result=JSON.parse(output);
 test(()=>assert.strictEqual(result.financialWrites,false));
 test(()=>assert.strictEqual(result.uatApproval,false));
 test(()=>assert.strictEqual(result.oracleAttestation,false));
 test(()=>assert.strictEqual(result.identityIncluded,false));
 test(()=>assert.strictEqual(result.probes.length,1));
 for(const token of [id,'P II TEST','Authorization','branchCode','partyId','requestUrl','message'])
  test(()=>assert.strictEqual(output.includes(token),false,'LEAK:'+token));
 const ui=fs.readFileSync(path.join(feature,'collateral-uat-workbench.component.html'),'utf8');
 const uat=fs.readFileSync(path.join(feature,'collateral-uat-workbench.component.ts'),'utf8');
 const reports=fs.readFileSync(path.join(feature,'collateral-reports.component.ts'),'utf8');
 test(()=>assert(ui.includes('<app-clt-r6d-readonly-diagnostics />')));
 test(()=>assert(ui.includes('<app-clt-r6e-detail-diagnostics />')));
 test(()=>assert(uat.includes('CltR6eDetailDiagnosticsComponent')));
 for(const mark of ['cltAssertOverview(data)','cltAssertEvents(data,20)','cltAssertReferenceSummary(data)'])
  test(()=>assert(reports.includes(mark),'REPORT_WIRING:'+mark));
 const component=fs.readFileSync(path.join(feature,'clt-r6e-detail-diagnostics.component.ts'),'utf8');
 test(()=>assert(component.includes("if(index===0&&verdict.state!=='PASS')")));
 test(()=>assert(component.includes('this.completedAt.set(new Date().toISOString());')));
 for(const bad of [/\.post\s*\(/i,/\.put\s*\(/i,/\.patch\s*\(/i,/\.delete\s*\(/i,/localStorage/i,/sessionStorage/i,/Authorization\s*:/i])
  test(()=>assert(!bad.test(component),'UNSAFE_DETAIL_DIAGNOSTIC:'+bad));
 for(const rel of ['clt-r6e-detail-diagnostics.component.ts','collateral-uat-workbench.component.ts','collateral-reports.component.ts']){
  const input=fs.readFileSync(path.join(feature,rel),'utf8');
  const parsed=ts.transpileModule(input,{fileName:rel,reportDiagnostics:true,
   compilerOptions:{module:ts.ModuleKind.ES2022,target:ts.ScriptTarget.ES2022,experimentalDecorators:true}});
  test(()=>assert(!(parsed.diagnostics||[]).some(d=>d.category===ts.DiagnosticCategory.Error),'SYNTAX:'+rel));
 }
 const java='backend/src/main/java/com/behsazan/corebanking/collateral/';
 for(const [controller,paths] of Object.entries({
  'registry/CollateralRegistryController.java':['@GetMapping("/{collateralId}")'],
  'sponsor/CollateralSponsorController.java':['@GetMapping("/{collateralId}/sponsors")'],
  'insurance/CollateralInsuranceController.java':['@GetMapping("/{collateralId}/insurance-policies")'],
  'valuation/CollateralValuationController.java':['@GetMapping("/{collateralId}/valuations")','@GetMapping("/{collateralId}/valuations/current-evidence")'],
  'capacity/CollateralCapacityController.java':['@GetMapping("/{collateralId}/capacity")'],
  'unallocation/CollateralUnallocationController.java':['@GetMapping','@GetMapping("/readiness")'],
  'release/CollateralFinalReleaseController.java':['@GetMapping("/readiness")','@GetMapping("/evidence")','@GetMapping("/status-history")']
 })){
  const text=fs.readFileSync(path.join(root,java,controller),'utf8');
  for(const endpoint of paths)test(()=>assert(text.includes(endpoint),'BACKEND_CONTRACT:'+controller+':'+endpoint));
 }
 console.log('CLT_R6E_REPORT_AND_DETAIL_CONTRACT_POLICY_PASS='+passed+'; GET_ONLY=YES; FINANCIAL_WRITES=NONE; ORACLE_EXECUTION=NONE');
}catch(e){console.error('CLT_R6E_POLICY_FAIL:'+e.stack);process.exitCode=1}
