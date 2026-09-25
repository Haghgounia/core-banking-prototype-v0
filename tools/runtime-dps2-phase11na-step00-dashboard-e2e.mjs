const base=(process.env.CORE_BANKING_BASE_URL||'http://127.0.0.1:8091').replace(/\/$/,'');
async function get(path){const r=await fetch(base+path);const t=await r.text();if(!r.ok)throw new Error(`GET ${path} -> ${r.status}: ${t}`);return t?JSON.parse(t):null;}
const n=v=>Number(v??0);
const key=o=>String(o?.currencyCode??'');
async function main(){
 const before=await get('/api/v1/deposit-accounts/dashboard');
 const search=await get('/api/v1/deposit-accounts?offset=0&limit=200');
 if(n(before?.accounts?.totalAccounts)!==n(search?.total))throw new Error(`DASHBOARD_ACCOUNT_TOTAL_MISMATCH|dashboard=${before?.accounts?.totalAccounts}|search=${search?.total}`);
 const rows=search?.items||[];
 let activeHolds=0;const balances=new Map();let seenTransactions=0;let transactionListTruncated=false;
 for(const row of rows){
   const detail=await get(`/api/v1/deposit-accounts/${row.accountId}`);
   activeHolds+=(detail?.holds||[]).filter(x=>x.holdStatusCode==='ACTIVE').length;
   const b=detail?.balance;if(b){const c=String(b.currencyCode);const a=balances.get(c)||{count:0,ledger:0,available:0,blocked:0,pendingDebit:0,pendingCredit:0};a.count++;a.ledger+=n(b.ledgerBalance);a.available+=n(b.availableBalance);a.blocked+=n(b.blockedAmount);a.pendingDebit+=n(b.pendingDebitAmount);a.pendingCredit+=n(b.pendingCreditAmount);balances.set(c,a);}
   const tx=await get(`/api/v1/deposit-accounts/${row.accountId}/transactions`);const list=tx?.transactions||[];seenTransactions+=list.length;if(list.length>=100)transactionListTruncated=true;
 }
 if(activeHolds!==n(before?.holds?.activeHolds))throw new Error(`DASHBOARD_ACTIVE_HOLD_MISMATCH|dashboard=${before?.holds?.activeHolds}|detail=${activeHolds}`);
 for(const b of before?.balances||[]){const a=balances.get(key(b));if(!a)throw new Error(`DASHBOARD_BALANCE_CURRENCY_MISSING|${key(b)}`);for(const [field,left,right] of [['accountCount',n(b.accountCount),a.count],['ledgerBalance',n(b.ledgerBalance),a.ledger],['availableBalance',n(b.availableBalance),a.available],['blockedAmount',n(b.blockedAmount),a.blocked],['pendingDebitAmount',n(b.pendingDebitAmount),a.pendingDebit],['pendingCreditAmount',n(b.pendingCreditAmount),a.pendingCredit]])if(Math.abs(left-right)>0.0001)throw new Error(`DASHBOARD_BALANCE_MISMATCH|${key(b)}|${field}|dashboard=${left}|detail=${right}`);}
 if(!transactionListTruncated&&seenTransactions!==n(before?.transactions?.totalTransactions))throw new Error(`DASHBOARD_TRANSACTION_TOTAL_MISMATCH|dashboard=${before?.transactions?.totalTransactions}|lists=${seenTransactions}`);
 const after=await get('/api/v1/deposit-accounts/dashboard');
 if(n(after?.accounts?.totalAccounts)!==n(before?.accounts?.totalAccounts)||n(after?.holds?.activeHolds)!==n(before?.holds?.activeHolds)||n(after?.transactions?.totalTransactions)!==n(before?.transactions?.totalTransactions))throw new Error('DASHBOARD_READ_CHANGED_BUSINESS_COUNTS');
 console.log(`PHASE11NA_RUNTIME_ACCOUNTS=${before.accounts.totalAccounts}`);
 console.log(`PHASE11NA_RUNTIME_ACTIVE_HOLDS=${before.holds.activeHolds}`);
 console.log(`PHASE11NA_RUNTIME_TRANSACTIONS=${before.transactions.totalTransactions}`);
 console.log(`PHASE11NA_RUNTIME_BALANCE_CURRENCIES=${(before.balances||[]).length}`);
 console.log('PHASE11NA_RUNTIME_E2E_PASS');
}
main().catch(e=>{console.error('PHASE11NA_RUNTIME_E2E_FAIL');console.error(e);process.exit(1)});
