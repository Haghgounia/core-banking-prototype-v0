// Read-only proof of the reference-data REST contract; no Oracle updates or test fixtures.
const root = (process.env.CORE_BANKING_BASE_URL || 'http://127.0.0.1:8091').replace(/\/+$/, '');
const names = ['LOAN_TYPE','PLAN_TYPE','LOAN_TYPE_PLAN_TYPE','LOAN_USAGE','ECONOMIC_SECTION','ECONOMIC_SUB_SECTION','FACILITY_OPERATION_PARAMETER','LOAN_PRODUCT_COLLATERAL'];
async function GET(path) {
  const r = await fetch(`${root}/api/v1/product-builder${path}`, {method: 'GET', signal: AbortSignal.timeout(12000)});
  if (!r.ok) throw new Error(`HTTP ${r.status} ${path}: ${(await r.text()).slice(0, 220)}`);
  return r.json();
}
try {
  for (const name of names) {
    const desc = await GET(`/tables/${name}/descriptor`);
    if (desc.tableName !== name) throw new Error(`Invalid descriptor ${name}`);
    const rows = await GET(`/tables/${name}/rows?page=0&size=1`);
    if (typeof rows.totalElements !== 'number') throw new Error(`Invalid count ${name}`);
    console.log(`READ_ONLY_PASS ${name}: ${rows.totalElements} total rows`);
  }
  for (const col of ['LOAN_TYPE_ID','PLAN_TYPE_ID']) {
    const options = await GET(`/tables/LOAN_TYPE_PLAN_TYPE/lookups/${col}`);
    if (!Array.isArray(options)) throw new Error(`Invalid lookup ${col}`);
    console.log(`READ_ONLY_PASS ${col}: ${options.length} active options`);
  }
  console.log('PB_R22_LOAN_REFERENCE_RUNTIME_READONLY_PASS');
} catch (error) {
  console.error('PB_R22_LOAN_REFERENCE_RUNTIME_READONLY_FAIL', error);
  process.exitCode = 1;
}
