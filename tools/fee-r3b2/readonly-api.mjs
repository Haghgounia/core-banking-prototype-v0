import assert from 'node:assert/strict';

// Read-only UAT; this file must never call POST, PUT, PATCH, DELETE or issue SQL.
const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/+$/,'');
const versionId=Number(process.env.FEE_R3B2_VERSION_ID||'837');
assert(Number.isSafeInteger(versionId)&&versionId>0,'FEE_R3B2_VERSION_ID must be a positive integer');
const api=base+'/api/v1/fees/admin/tables';
let requestCount=0;
async function get(url) {
  const res=await fetch(url,{method:'GET',headers:{Accept:'application/json'},signal:AbortSignal.timeout(15000)});
  requestCount++;
  if (!res.ok) throw new Error('GET '+url+' returned HTTP '+res.status+' '+(await res.text()).slice(0,240));
  return res.json();
}
function q(table,suffix){return api+'/'+encodeURIComponent(table)+'/'+suffix;}
async function scan(table,filterColumn,filterValue){
  const items=[];let total=0;
  for(let page=0;page<100;page++){
    const qs=new URLSearchParams({page:String(page),size:'200',filterColumn,filterValue:String(filterValue)});
    const p=await get(q(table,'rows?'+qs));
    assert(Array.isArray(p.items)&&Number.isInteger(p.totalElements),table+' malformed TablePage');
    total=p.totalElements;items.push(...p.items);
    if(items.length>=total)break;
  }
  assert.equal(items.length,total,'Read-only scan incomplete; >20,000 rows or pagination problem for '+table);
  return items;
}
try {
  const desc=await get(q('FEE_CHARGE_STAGE','descriptor'));
  assert.equal(desc.tableName,'FEE_CHARGE_STAGE');
  const component=desc.columns.find(x=>x.name==='CHARGE_COMPONENT_ID');
  const rule=desc.columns.find(x=>x.name==='CALCULATION_RULE_ID');
  assert.equal(component?.nullable,true,'optional component schema contract');
  assert.equal(rule?.nullable,false,'required calculation rule schema contract');
  const version=await get(q('FEE_DEFINITION_VERSION','rows/'+versionId));
  assert.equal(Number(version.FEE_DEFINITION_VERSION_ID),versionId);
  const stageRows=await scan('FEE_CHARGE_STAGE','FEE_DEFINITION_VERSION_ID',versionId);
  for (const stage of stageRows) {
    assert.equal(Number(stage.FEE_DEFINITION_VERSION_ID),versionId,'stage row leaked from another version');
    const parent=await get(q('FEE_CALCULATION_RULE','rows/'+Number(stage.CALCULATION_RULE_ID)));
    assert.equal(Number(parent.FEE_DEFINITION_VERSION_ID),versionId,'existing stage has cross-version calculation rule');
    if(stage.CHARGE_COMPONENT_ID!=null){
      const comp=await get(q('FEE_CHARGE_COMPONENT','rows/'+Number(stage.CHARGE_COMPONENT_ID)));
      assert.equal(Number(comp.FEE_DEFINITION_VERSION_ID),versionId,'existing stage has cross-version component');
    }
  }
  const readLookups=async(column,parentTable)=>{
    const params=new URLSearchParams({versionId:String(versionId),limit:'1000'});
    const options=await get(q('FEE_CHARGE_STAGE','lookups/'+column+'?'+params));
    assert(Array.isArray(options),'lookup format invalid '+column);
    for(const option of options){
      const entity=await get(q(parentTable,'rows/'+Number(option.value)));
      assert.equal(Number(entity.FEE_DEFINITION_VERSION_ID),versionId,'lookup leaked different owner '+column);
    }
    return options.length;
  };
  const nRules=await readLookups('CALCULATION_RULE_ID','FEE_CALCULATION_RULE');
  const nComponents=await readLookups('CHARGE_COMPONENT_ID','FEE_CHARGE_COMPONENT');
  console.log('FEE_R3B2_READONLY_API_PASS | version='+versionId+'; rules='+nRules+
    '; components='+nComponents+'; stages='+stageRows.length+'; GETs='+requestCount+'; no writes');
} catch(e){
  console.error('FEE_R3B2_READONLY_API_FAIL | '+(e?.message||e));
  process.exitCode=1;
}
