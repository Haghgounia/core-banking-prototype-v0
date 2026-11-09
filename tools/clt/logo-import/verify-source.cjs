#!/usr/bin/env node
'use strict';
const fs=require('fs'),path=require('path');
const root=path.resolve(process.argv[2] || path.join(__dirname,'../../..'));
const rd=p=>fs.readFileSync(path.join(root,p),'utf8');
let n=0;
function ok(p,re){if(!re.test(rd(p)))throw Error('CLT_LOGO_CONTRACT_FAIL: '+p+' '+String(re));n++;}
const backend='backend/src/main/java/com/behsazan/corebanking/collateral/reference/';
const front='frontend/src/app/features/collateral/';
ok(backend+'CltInsuranceLogoImportController.java',/@PostMapping\s*\n\s*public CltInsuranceLogoImportService\.ImportResult/);
ok(backend+'CltInsuranceLogoImportController.java',/@GetMapping\("\/status"\)/);
ok(backend+'CltInsuranceLogoImportController.java',/@GetMapping\("\/image\/\{id\}"\)/);
ok(backend+'CltInsuranceLogoImportService.java',/new JdbcTemplate\(dataSource\)/);
ok(backend+'CltInsuranceLogoImportService.java',/RECORD_VERSION=RECORD_VERSION\+1/);
ok(backend+'CltInsuranceLogoImportService.java',/RECORD_VERSION=\? AND LOGO IS NULL/);
ok(backend+'CltInsuranceLogoImportService.java',/MessageDigest\.isEqual\(sha256\(content\), sha256\(readBack\)\)/);
ok(backend+'CltInsuranceLogoImportService.java',/compareAndSet\(false, true\)/);
ok(backend+'CltInsuranceImagePolicy.java',/MAX_BYTES = 1_500_000/);
ok(front+'clt-reference-workspace.component.html',/data-clt-logo-import-once/);
ok(front+'clt-reference-workspace.component.html',/data-clt-business-fields/);
ok(front+'clt-reference-workspace.component.html',/data-clt-audit/);
ok(front+'clt-reference-workspace.component.html',/data-clt-delete-dialog/);
ok(front+'clt-reference-workspace.component.html',/insurer-website/);
ok(front+'clt-reference-workspace.component.html',/insurer-logo/);
ok(front+'clt-reference.service.ts',/importInsuranceLogos\(\)/);
const sources=rd('backend/src/main/resources/clt/insurance-logo-sources.tsv').split(/\r?\n/).filter(x=>x && !x.startsWith('#'));
if(sources.length!==18||sources.some(x=>!x.includes('|https://')))throw Error('CLT_LOGO_SOURCE_COUNT_INVALID');
n++;
console.log('CLT_INAPP_LOGO_SOURCE_CONTRACT_PASS='+n+'/'+n+'; LIVE_ORACLE=NOT_VERIFIED');
