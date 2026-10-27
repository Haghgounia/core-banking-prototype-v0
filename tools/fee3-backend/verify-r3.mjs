// FEE3 R3 non-mutating source contract verifier. Use Node 20+.
import fs from 'node:fs';
import path from 'node:path';
const root=process.cwd();
const target='backend/src/main/java/com/behsazan/corebanking/fee3/r3';
const required=['Fee3PreviewEngine.java','Fee3SubmissionPolicy.java','Fee3R3WorkflowRepository.java',
    'Fee3R3Service.java','Fee3R3Controller.java','Fee3R3ProblemAdvice.java'];
let tests=0;
function ok(cond,msg){tests++;if(!cond)throw Error(`FEE3_R3_VERIFY_FAILED: ${msg}`);}
let content='';
for(const file of required){
  const p=path.join(root,target,file);ok(fs.existsSync(p),`missing ${file}`);
  const s=fs.readFileSync(p,'utf8');ok(s.includes('class ')||s.includes('record '),`empty ${file}`);
  content+=s+'\n';
}
const service=fs.readFileSync(path.join(root,target,'Fee3R3Service.java'),'utf8');
const engine=fs.readFileSync(path.join(root,target,'Fee3PreviewEngine.java'),'utf8');
const controller=fs.readFileSync(path.join(root,target,'Fee3R3Controller.java'),'utf8');
const repo=fs.readFileSync(path.join(root,target,'Fee3R3WorkflowRepository.java'),'utf8');
ok(service.includes('${FEE3_R3_WORKFLOW_ENABLED:false}'),'workflow fail closed');
ok(service.includes('${FEE3_R3_CHECKER_DECISIONS_ENABLED:false}'),'checker fail closed');
ok(service.includes('${FEE3_R3_UAT_APPROVAL_ENABLED:false}'),'approval fail closed');
ok(service.includes('differentActors('),'maker/checker isolation');
ok(service.includes('allTestsCertified('),'approved test evidence guard');
ok(service.includes('@Transactional'),'transactional workflow');
ok(controller.includes('request.isUserInRole("FEE3_MAKER")'),'real container maker role');
ok(controller.includes('request.isUserInRole("FEE3_CHECKER")'),'real container checker role');
ok(controller.includes('request.getAuthType()'),'container authentication');
ok(!controller.includes('X-FEE3-UAT-Token'),'UAT token not actor identity');
ok(engine.includes('NO_ORACLE_MUTATION'),'no-write preview');
ok(engine.includes('PORTFOLIO_SHARES_MUST_EQUAL_100'),'portfolio total 100');
ok(engine.includes('FORMULA_EXECUTION_FORBIDDEN'),'no arbitrary executable formulas');
ok(repo.includes('SEQ_FCE_CHANGE_REQUEST'),'actual request sequence');
ok(repo.includes('FCE_AUDIT_EVENT'),'audited workflow');
ok(repo.includes('IS_ACTIVE=0'),'no tariff activation');
ok(!repo.includes('PUBLISHED_AT'),'no publication');
ok(!content.includes('executeImmediate('),'no dynamic arbitrary SQL');
ok(fs.existsSync(path.join(root,'tools/fee3-backend/Fee3R3EngineProbe.java')),'engine probe');
console.log(`FEE3_R3_STATIC_PASS=${tests} FAIL=0`);
