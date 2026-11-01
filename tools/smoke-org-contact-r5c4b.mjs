// R5-C4B: two READ-ONLY calls, no DDL/DML; requires a running backend.
const base=(process.env.ORG_R5C4B_BASE_URL ?? 'http://127.0.0.1:8091').replace(/\/$/,'');
try {
  const readiness=await fetch(base+'/api/v1/organization/unit-contacts/readiness');
  const state=await readiness.json();
  if (readiness.status!==200 || state.storageMode!=='UNIT' || state.ready!==true ||
      state.masterPresent!==false || state.unitForeignKeyPresent!==false ||
      state.servicePointForeignKeyPresent!==false || state.rowVersionPresent!==true) {
    throw Error('OUCP readiness mismatch (HTTP '+readiness.status+'): '+JSON.stringify(state));
  }
  const legacy=await fetch(base+'/api/v1/organization/unit-contacts/migration/readiness');
  let detail;try {detail=await legacy.json();} catch {detail=null;}
  if (legacy.status!==410 || detail?.code!=='ORG_V2_MIGRATION_ENDPOINT_RETIRED') {
    throw Error('V2 migration endpoint must return HTTP 410; received HTTP '+legacy.status);
  }
  console.log('ORG_R5C4B_RUNTIME_SMOKE_PASS | OUCP=UNIT | READY=true | V2_API=410 | WRITES_ENABLED='+state.writesEnabled);
} catch(e){console.error('ORG_R5C4B_RUNTIME_SMOKE_FAIL: '+e.message);process.exitCode=1;}
