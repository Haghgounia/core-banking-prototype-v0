/* Execute the actual Angular mapping TypeScript as CommonJS in a temporary directory.
 * Requires project's typescript dependency (from npm install) or global dev TS.
 * Does not access Oracle, HTTP, or files outside the module sources.
 */
const fs=require('node:fs'),path=require('node:path'),os=require('node:os'),{createRequire}=require('node:module');
const root=path.resolve(__dirname,'../..');
const front=path.join(root,'frontend/src/app/features/fee3');
let ts;
try{ts=require(path.join(root,'frontend/node_modules/typescript'));}catch{try{ts=require('typescript');}catch{console.log('FEE3_BE_R2_MAPPING_SKIP=typescript_not_installed');process.exit(0);}}
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'fee3-r2-map-'));
try {
  for(const name of ['fee3.models','fee3-oracle-mapping']){
    const source=fs.readFileSync(path.join(front,`${name}.ts`),'utf8');
    const parsed=ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS},fileName:`${name}.ts`,reportDiagnostics:true});
    if(parsed.diagnostics?.length)throw new Error('TS syntax invalid: '+name);
    fs.writeFileSync(path.join(tmp,`${name}.js`),parsed.outputText);
  }
  const map=require(path.join(tmp,'fee3-oracle-mapping.js'));
  const src=[{id:44,documentNo:'CBI-1405',documentTitleFa:'مرجع',sourceType:'CBI',validationStatus:'PENDING'}];
  const text=(value)=>JSON.stringify(value);
  const aggregate={item:{CALC_ITEM_ID:42,CALC_CODE:'F3-UAT-42',TITLE_FA:'کارمزد سپرده',MODULE_CODE:'DEPOSIT',OPERATION_CODE:'ACCOUNT_OPENING',AMOUNT_KIND_CODE:'SERVICE_FEE',OWNER_DOMAIN_CODE:'TARIFF',IS_ENABLED:0,RECORD_VERSION:3,SERVICE_VARIANT_CODE:null,DESCRIPTION_FA:'تست'},
    versions:[{version:{TARIFF_VERSION_ID:90,RECORD_VERSION:4,VERSION_NO:1,VALID_FROM:'2026-10-26T00:00:00',STATUS_CODE:'DRAFT',IS_ACTIVE:0,TARIFF_ORIGIN_CODE:'BANK_INTERNAL',BASE_METHOD_CODE:'FORMULA',TAX_MODE_CODE:'UNREVIEWED',ROUNDING_CODE:'HALF_UP',SOURCE_ID:44,FINANCIAL_POLICY_JSON:text({incomeClass:'BANK_INCOME',_fee3UiV1:{contractVersion:1,serviceCurrency:'IRR',feeCurrency:'IRR',context:{cardServiceType:'REISSUE'}}})},
      scopes:[{CHANNEL_CODES_JSON:text(['BRANCH']),CUSTOMER_TYPES_JSON:text(['ALL']),CUSTOMER_GROUPS_JSON:text(['ALL']),PRODUCT_CODES_JSON:text(['ALL']),CONTRACT_TYPES_JSON:text([]),CURRENCY_CODE:'IRR',PRIORITY_NO:100}],
      rules:[{RULE_ORDER:1,METHOD_CODE:'FORMULA',AMOUNT_BASE_CODE:'REQUEST_AMOUNT',RATE_SOURCE_CODE:'REQUEST',RATE_PERCENT:null,FIXED_AMOUNT_IRR:null,COLLATERAL_TYPE_CODE:null,COVERAGE_PERCENT:null,MIN_FEE_IRR:null,MAX_FEE_IRR:null,FORMULA_CODE:null,PARAMETERS_JSON:text({originalConfig:{tiers:[]},_fceEditableExpressionV1:'amount * rate / 100'})}],
      tests:[{TEST_TITLE_FA:'سناریو محلی',REQUEST_JSON:text({amount:1000,simulationOnly:true}),EXPECTED_IRR:null}]}]};
  let count=0;const assert=(v,description)=>{if(!v)throw new Error(description);count++;};
  const item=map.fee3AggregateToItem(aggregate,src),v=item.versions[0];
  assert(item.oracleItemId===42&&item.oracleItemRecordVersion===3,'physical identity');
  assert(item.serviceCurrency==='IRR'&&item.feeCurrency==='IRR','currency context');
  assert(item.context.cardServiceType==='REISSUE','conditional identity');
  assert(v.oracleVersionId===90&&v.oracleVersionRecordVersion===4,'optimistic concurrency');
  assert(v.sourceCode==='CBI-1405','source ID to documentNo');
  assert(!('_fee3UiV1' in v.financialPolicy),'strip transport metadata');
  assert(v.rules[0].formula==='amount * rate / 100','custom formula survived');
  assert(v.rules[0].parameters.originalConfig.tiers.length===0,'rule parameters survived');
  assert(v.tests[0].status===null&&v.tests[0].result===null,'no fabricated bank test');
  const draft=map.fee3ToDraftRequest(item,v,src);
  assert(draft.calcItemId===42&&draft.tariffVersionId===90,'update identity');
  assert(draft.expectedItemRecordVersion===3&&draft.expectedVersionRecordVersion===4,'record versions sent');
  assert(draft.version.sourceId===44,'resolved source');
  assert(draft.rules[0].formulaCode===null&&draft.rules[0].parameters._fceEditableExpressionV1==='amount * rate / 100','custom expression is non-executable metadata');
  assert(draft.tests[0].request.simulationOnly===true,'test remains simulation only');
  let rejected=false;
  item.feeCurrency='USD';try{map.fee3ToDraftRequest(item,v,src);}catch{rejected=true;}
  assert(rejected,'non-IRR monetary fields denied');
  item.feeCurrency='IRR';v.no=2;rejected=false;try{map.fee3ToDraftRequest(item,v,src);}catch{rejected=true;}
  assert(rejected,'unimplemented second version denied');
  const summary=map.fee3SummaryToItem({calcItemId:42,itemRecordVersion:3,code:'F3-UAT-42',titleFa:'کارمزد',moduleCode:'DEPOSIT',operationCode:'ACCOUNT_OPENING',amountKindCode:'SERVICE_FEE',ownerDomainCode:'TARIFF',enabled:false,tariffVersionId:90,latestVersionNo:1,latestStatusCode:'DRAFT',latestValidFrom:'2026-10-26'});
  assert(summary.oracleItemId===42&&summary.versions[0].oracleVersionId===90,'catalog lazy hydration metadata');
  console.log(`FEE3_BE_R2_MAPPING_PASS=${count}`);
}finally{fs.rmSync(tmp,{recursive:true,force:true});}
