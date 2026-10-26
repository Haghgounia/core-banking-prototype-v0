const assert=require('node:assert/strict');
/** Executes tests against ACTUAL R6 TypeScript through TS.transpileModule (not a duplicated calculator). */
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..');
const fdir=path.join(root,'frontend','src','app','features','fee3');
let ts;
try {ts=require(path.join(root,'frontend','node_modules','typescript'));}
catch {
 try {ts=require('typescript');}
 catch {
  if(process.env.FEE3_TYPESCRIPT_MODULE)ts=require(process.env.FEE3_TYPESCRIPT_MODULE);
  else {console.error('Install frontend dependencies (npm ci) or set FEE3_TYPESCRIPT_MODULE to a local TypeScript module.');process.exit(2);}
 }
}
const files=new Map();
function load(file){
 const key=path.resolve(fdir,file+'.ts');
 if(files.has(key))return files.get(key).exports;
 const mod={exports:{}};files.set(key,mod);
 const code=ts.transpileModule(fs.readFileSync(key,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
 const doRequire=id=>id.startsWith('.')?load(path.join(path.dirname(file),id)):require(id);
 new Function('require','module','exports',code)(doRequire,mod,mod.exports);
 return mod.exports;
}
const f=load('fee3-specialized-reference');
const calc=load('fee3-offline-calculator');
const ok=(actual,expected,msg)=>assert.ok(Math.abs(actual-expected)<0.00001,`${msg}: actual ${actual} expected ${expected}`);
const contains=(fn,part)=>assert.throws(fn,e=>e instanceof Error && e.message.includes(part));
let assertions=0;const test=(name,fn)=>{fn();assertions++; console.log('PASS',name)};
const item={module:'GUARANTEE',operation:'GUARANTEE_ISSUE',amountKind:'GUARANTEE_FEE',serviceCurrency:'IRR',feeCurrency:'IRR'};
const rule={order:1,method:'PERCENT',rateSource:'REQUEST',rate:null,formula:'',fixed:null,min:null,max:null,parameters:{originalConfig:{dayBasis:'ACT_ACT'}}};
const version={method:'PERCENT',rules:[rule],rounding:'HALF_UP',taxMode:'EXEMPT',financialPolicy:{}};
const policy=f.blankGuaranteePolicy();policy.portfolioRules=[{collateralType:'CASH_100',rateSource:'EXEMPT',ratePercent:null,coveragePercent:100},
 {collateralType:'PROMISSORY_NOTE',rateSource:'CONFIG',ratePercent:2.25,coveragePercent:120},
 {collateralType:'MOVABLE_PROPERTY',rateSource:'CONFIG',ratePercent:1,coveragePercent:100}];
const rows=[{collateralType:'CASH_100',sharePercent:'15'},{collateralType:'PROMISSORY_NOTE',sharePercent:'50'},{collateralType:'MOVABLE_PROPERTY',sharePercent:'35'}];
const run=(amt=100000000,extra={})=>f.simulateFee3Guarantee(item,version,policy,{amount:amt,portfolio:rows,...extra});
test('Qard leap Jalali year 1403',()=>{let r=f.jalaliContractFraction('2024-03-20','2025-03-21');assert.equal(r.calendarDays,366);ok(r.yearFraction,1,'leap year')});
test('Qard 365-day basis differs from leap',()=>{let r=f.jalaliContractFraction('2024-03-20','2025-03-21','ACT_365');ok(r.yearFraction,366/365,'act365')});
test('Qard 360-day basis',()=>{let r=f.jalaliContractFraction('2024-03-20','2025-03-21','ACT_360');ok(r.yearFraction,366/360,'act360')});
test('Qard crossed years exact sum',()=>{let a=f.jalaliContractFraction('2024-03-01','2025-04-01');let b=f.jalaliContractFraction('2024-03-01','2024-03-20');let c=f.jalaliContractFraction('2024-03-20','2025-03-21');let d=f.jalaliContractFraction('2025-03-21','2025-04-01');ok(a.yearFraction,b.yearFraction+c.yearFraction+d.yearFraction,'crossing');});
test('Qard year fraction never user-derived',()=>{let r=f.jalaliContractFraction('2024-03-20','2025-03-21');let amt=calc.safeReferenceFormula('amount*rate/100*yearFraction',{amount:10000000,rate:18,yearFraction:r.yearFraction});ok(amt,1800000,'qard rate')});
test('Qard invalid reversed dates rejected',()=>contains(()=>f.jalaliContractFraction('2025-03-21','2024-03-20'),'پایان غیرشامل'));
test('Guarantee 3-row allocated sum',()=>{const r=run();assert.equal(r.amount,1475000);assert.equal(r.guaranteePortfolio.shareTotal,100);assert.equal(r.guaranteePortfolio.rows[0].rawFee,0);});
test('Guarantee 3-row coverage sum',()=>{policy.feeBasis=f.GUARANTEE_PORTFOLIO_COVERED;const r=run();assert.equal(r.amount,1700000);policy.feeBasis=f.GUARANTEE_PORTFOLIO_SHARE;});
test('Guarantee 2-row 40/60',()=>{const r=run(100000000,{portfolio:rows.slice(0,2).map((r,i)=>({...r,sharePercent:i===0?'40':'60'}))});assert.equal(r.amount,1350000)});
test('Guarantee 2-row 40/60 covered',()=>{policy.feeBasis=f.GUARANTEE_PORTFOLIO_COVERED;const r=run(100000000,{portfolio:rows.slice(0,2).map((r,i)=>({...r,sharePercent:i===0?'40':'60'}))});assert.equal(r.amount,1620000);policy.feeBasis=f.GUARANTEE_PORTFOLIO_SHARE});
test('Guarantee invalid 95 share blocked',()=>contains(()=>run(100000000,{portfolio:rows.map((r,i)=>({...r,sharePercent:i===2?'30':r.sharePercent}))}),'۱۰۰٪'));
test('Guarantee no rule for collateral blocked',()=>contains(()=>run(100000000,{portfolio:[{collateralType:'GOLD',sharePercent:'100'}]}),'قاعده نرخ'));
test('Guarantee missing rate blocked',()=>{let r=policy.portfolioRules[1];r.ratePercent=null;contains(()=>run(),'نرخ');r.ratePercent=2.25;});
test('Guarantee cash coverage below 100 blocked',()=>{policy.portfolioRules[0].coveragePercent=99;contains(()=>run(),'۱۰۰٪');policy.portfolioRules[0].coveragePercent=100;});
test('Guarantee duplicate tariff rules blocked',()=>{policy.portfolioRules.push({...policy.portfolioRules[1]});contains(()=>run(),'تکراری');policy.portfolioRules.pop();});
test('Guarantee controlled contract rate if REQUEST',()=>{const r=policy.portfolioRules[1];r.rateSource='REQUEST';r.ratePercent=null;contains(()=>run(),'نرخ معتبر');ok(run(100000000,{contractRate:2.25}).amount,1475000,'request rate');r.rateSource='CONFIG';r.ratePercent=2.25;});
test('Guarantee only one tax application',()=>{version.taxMode='TAXABLE';version.financialPolicy.taxPercent=10;let r=run();assert.equal(r.amount,1475000);assert.equal(r.tax,147500);assert.equal(r.total,1622500);version.taxMode='EXEMPT';});
test('Guarantee minimum one time on aggregate',()=>{rule.min=2000000;let r=run();assert.equal(r.amount,2000000);rule.min=null});
test('Guarantee cap one time on aggregate',()=>{rule.max=1200000;let r=run();assert.equal(r.amount,1200000);rule.max=null});
test('Guarantee daily leap ACT/ACT',()=>{rule.method='DAILY';version.method='DAILY';let r=run(100000000,{periodStart:'2024-01-01',periodEndExclusive:'2025-01-01'});assert.equal(r.amount,1475000);version.method='PERCENT';rule.method='PERCENT';});
test('Guarantee DAILY missing period denied',()=>{rule.method='DAILY';version.method='DAILY';contains(()=>run(),'تاریخ');version.method='PERCENT';rule.method='PERCENT'});
test('Guarantee portfolio forbids FIXED',()=>{version.method='FIXED';contains(()=>run(),'سبد وثایق');version.method='PERCENT'});
test('Guarantee formula linear allowed only',()=>{version.method='FORMULA';rule.method='FORMULA';rule.formula='amount * rate / 100';ok(run().amount,1475000,'formula');rule.formula='amount**rate';contains(()=>run(),'فرمول خطی');version.method='PERCENT';rule.method='PERCENT';rule.formula='';});
test('Guarantee single CASH rate, coverage >=100',()=>{let single={collateralType:'CASH_100',feeBasis:f.GUARANTEE_SINGLE_BASE,portfolioRules:[]};let v={...version,rules:[{...rule,rateSource:'RULE',rate:1,fixed:null,method:'PERCENT'}]};let a=f.simulateFee3Guarantee(item,v,single,{amount:100000000,cashCoveragePercent:100});assert.equal(a.amount,1000000);contains(()=>f.simulateFee3Guarantee(item,v,single,{amount:100000000,cashCoveragePercent:80}),'100');});
test('Guarantee single coverage basis 120',()=>{let single={collateralType:'PROMISSORY_NOTE',feeBasis:f.GUARANTEE_COVERED_BASE,portfolioRules:[]};let v={...version,rules:[{...rule,rateSource:'RULE',rate:1,method:'PERCENT'}]};let a=f.simulateFee3Guarantee(item,v,single,{amount:100000000,collateralCoveragePercent:120});assert.equal(a.amount,1200000)});
test('Guarantee portfolio rejects fake other currency',()=>{let j={...item,feeCurrency:'USD'};contains(()=>f.simulateFee3Guarantee(j,version,policy,{amount:100000000,portfolio:rows}),'تبدیل ارزی');});
test('Guarantee portfolio source tax unresolved blocked',()=>{version.taxMode='UNREVIEWED';contains(()=>run(),'مالیات');version.taxMode='EXEMPT'});
test('Guarantee portfolio request malformed source blocked',()=>{let t=policy.portfolioRules[2];t.rateSource='BAD';contains(()=>run(),'منبع نرخ');t.rateSource='CONFIG'});
test('Guarantee portfolio sample amount zero rejected',()=>contains(()=>run(0),'مبلغ تعهد'));
test('Guarantee portfolio no-rate fallback cannot occur',()=>{policy.portfolioRules[1].rateSource='CONFIG';policy.portfolioRules[1].ratePercent=null;contains(()=>run(),'نرخ');policy.portfolioRules[1].ratePercent=2.25});
console.log('FEE3_R6_FINANCIAL_POLICY_TESTS_PASS='+assertions);
