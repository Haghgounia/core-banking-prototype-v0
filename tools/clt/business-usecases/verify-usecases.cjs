'use strict';
/** Verifies that BUC-14 and BUC-16 implementation is wired to CLT and UI; no DML added. */
const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'../../..');
const java='backend/src/main/java/com/behsazan/corebanking/collateral/';
const ui='frontend/src/app/features/collateral/';
let passed=0;
function ok(condition,label){if(!condition)throw Error('BUC_SOURCE_FAIL '+label);passed++;}
function source(p){return fs.readFileSync(path.join(root,p),'utf8');}
const policy=source(java+'monitoring/CollateralCoverageMonitoringPolicy.java');
const riskRepo=source(java+'monitoring/CollateralCoverageMonitoringRepository.java');
const reportController=source(java+'reporting/CollateralReportingController.java');
const timelineRepo=source(java+'reporting/CollateralTimelineRepository.java');
const timelineService=source(java+'reporting/CollateralTimelineService.java');
const reportHtml=source(ui+'collateral-reports.component.html');
const reportTs=source(ui+'collateral-reports.component.ts');
const reportService=source(ui+'collateral-reports.service.ts');
for(let [text,label] of [
 ['RECORDED_SHORTFALL','recorded shortage'],['FX_REVIEW_REQUIRED','foreign currency'],
 ['VALUATION_REVIEW_REQUIRED','valuation invalid'],['!currentDay.isBefore(from)','from inclusive'],
 ['!currentDay.isAfter(to)','to inclusive'],['newAllocationAuthorized','non authorizing'],
 ['BANK_APPROVED_FACTORS_AND_OBLIGATION_COVERAGE_NOT_ATTESTED','bank factors']])
 ok(policy.includes(text),'POLICY_'+label);
for(let [text,label] of [
 ['CLT.COLLATERAL_ALLOCATION','oracle allocation'],['CLT.COLLATERAL_VALUATION_HISTORY','oracle valuation'],
 ["a.USED = 'YES'",'active only'],['c.BRANCH_CODE = ?','branch bind'],
 ['a.CURRENCY_CODE <> c.CURRENCY_CODE','non matching currencies'],
 ['SUM(CASE WHEN','oracle summation'],['OFFSET ? ROWS FETCH NEXT ? ROWS ONLY','bounded paging']])
 ok(riskRepo.includes(text),'RISK_SQL_'+label);
for(let [text,label] of [
 ['CLT.COLLATERAL_STATUS_HISTORY','status'],['CLT.COLLATERAL_VALUATION_HISTORY','valuation'],
 ['CLT.COLLATERAL_INSURANCE','insurance'],['CLT.COLLATERAL_SPONSOR','sponsor'],
 ['CLT.COLLATERAL_ALLOCATION','allocation'],['CLT.COLLATERAL c','collateral'],
 ['a.UPDATED_AT IS NOT NULL','allocation updated'],['c.BRANCH_CODE = ?','branch bind'],
 ['UNION ALL','unified history'],['OFFSET ? ROWS FETCH NEXT ? ROWS ONLY','history pagination']])
 ok(timelineRepo.includes(text),'HISTORY_SQL_'+label);
ok(timelineService.includes('registry.byRegistration(branch, registrationNumber)'),'AUTH_PARENT_LOOKUP');
ok(reportController.includes('@GetMapping("/coverage-monitor")'),'CONTROLLER_MONITOR');
ok(reportController.includes('@GetMapping("/by-registration/{number}/history")'),'CONTROLLER_HISTORY');
ok(reportController.includes('requireAvailable(authentication)'),'CONTROLLER_AUTH');
ok(reportTs.includes('cltAssertCoverage(data)'),'FRONT_RISK_VERIFY');
ok(reportTs.includes('cltAssertTimeline(data,50)'),'FRONT_HISTORY_VERIFY');
ok(reportService.includes('encodeURIComponent(number)'),'SAFE_URL_NUMBER');
ok(reportHtml.includes("select('coverage')")&&reportHtml.includes("select('history')"),'BOTH_TABS');
ok(reportHtml.includes('nextHistoryPage(1)')&&reportHtml.includes('nextCoveragePage(1)'),'PAGING_BOTH');
ok(!/\b(DELETE|UPDATE|INSERT|MERGE)\s+(?:INTO|FROM|CLT\.)/i.test(riskRepo+timelineRepo),'NO_DML_IN_READ_REPOS');
console.log('BUC14_16_SOURCE_CONTRACT_PASS='+passed+'/'+passed);
