#!/usr/bin/env node
// ORG-GRADE prototype smoke checks. No Oracle reads/writes.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const load = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const check = (okay, message) => {if (!okay) throw new Error(message);};
const grade = name => load(`backend/src/main/java/com/behsazan/corebanking/organization/grade/${name}.java`);
try {
  const routes = load('frontend/src/app/app.routes.ts');
  const first = routes.indexOf("path: 'organization/grade-levels'");
  const second = routes.indexOf("path: 'organization/grade-periods'");
  const generic = routes.indexOf("path: 'organization/:resource'");
  check(first > 0 && first < second && second < generic, 'Grade routes missing/overridden');
  const controller = grade('GradeController');
  check(controller.includes('service.assign('), 'Direct grade assignment API missing');
  for (const route of ['/submit', '/approve', '/reject', '/pending', '/capabilities'])
    check(!controller.includes(`"${route}"`), `Obsolete workflow route found: ${route}`);
  const actor = grade('GradeActorResolver');
  check(!actor.includes('ORG_GRADE_') && actor.includes('PROTOTYPE'), 'Security role dependency remains');
  const service = grade('GradeService');
  check(service.includes('GradePolicy.plan') && service.includes('repository.lockUnit'), 'Grade overlap/lock policy missing');
  const repository = grade('GradeRepository');
  check(repository.includes("'ACTIVE'") && repository.includes('closePrevious'), 'Direct persistence/history missing');
  const guard = grade('GradeBranchActivationGuard');
  check(!guard.includes('SELECT '), 'Prototype activation unexpectedly blocked');
  const levelUi = load('frontend/src/app/features/organization/organization-grade-levels.component.html');
  const periodUi = load('frontend/src/app/features/organization/organization-grade-periods.component.html');
  check(levelUi.includes('ثبت درجه') && periodUi.includes('ثبت درجه'), 'Forms missing');
  check(periodUi.includes('app-persian-date-input') && !periodUi.includes('درخواست‌های منتظر تأیید'), 'Workflow UI remains');
  check(!levelUi.includes('ORG_GRADE_') && !periodUi.includes('ORG_GRADE_'), 'Old role warning remains');
  const sql = load('database/oracle/org/grade/ORG-GRADE-PROTOTYPE-01-ALLOW-DIRECT-ASSIGN.sql');
  check(sql.includes("'ACTIVE'") && sql.includes('CK_OU_GRADE_PERIODS_STATE'), 'Database compatibility migration missing');
  console.log('ORG_GRADE_PROTOTYPE_STATIC_PASS');

  const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'org-grade-prototype-'));
  try {
    const source = path.join(root, 'backend/src/main/java/com/behsazan/corebanking/organization/grade');
    const smoke = `import java.util.*; import java.time.LocalDate;
import com.behsazan.corebanking.organization.grade.*;
import com.behsazan.corebanking.organization.grade.GradeModels.*;
public class Smoke {
  static Period p(long id,String f,String to){return new Period(id,1,2,"G1","Grade 1",LocalDate.parse(f),to==null?null:LocalDate.parse(to),"CHANGE","ACTIVE",null,"PROTOTYPE",0);}
  static void invalid(Runnable check){try{check.run();throw new Error("Expected validation failure");}catch(IllegalArgumentException expected){}}
  public static void main(String[] args){
    Period old=p(1,"2026-01-01",null);
    if(GradePolicy.plan(p(0,"2026-07-01",null),List.of(old)).closePredecessorId()!=1L)throw new Error("History failure");
    invalid(()->GradePolicy.plan(p(0,"2026-01-01",null),List.of(old)));
    invalid(()->GradePolicy.plan(p(0,"2025-01-01",null),List.of(old)));
    invalid(()->GradePolicy.dates(LocalDate.parse("2026-01-01"),LocalDate.parse("2026-01-01")));
    System.out.println("ORG_GRADE_PROTOTYPE_POLICY_PASS");
  }
}`;
    const target = path.join(temp, 'Smoke.java');
    fs.writeFileSync(target, smoke);
    const javac = spawnSync('javac', ['-encoding','UTF-8','-d',temp,
      path.join(source,'GradeModels.java'),path.join(source,'GradePolicy.java'),target], {encoding:'utf8'});
    if (javac.error?.code === 'ENOENT') console.log('ORG_GRADE_PROTOTYPE_POLICY_SKIP (JDK not available)');
    else {
      check(javac.status === 0, `Grade policy compilation failed: ${javac.stderr}`);
      const run = spawnSync('java',['-cp',temp,'Smoke'],{encoding:'utf8'});
      check(run.status === 0, `Grade policy test failed: ${run.stderr}${run.stdout}`);
      process.stdout.write(run.stdout);
    }
  } finally { fs.rmSync(temp, {recursive:true,force:true}); }
  console.log('ORG_GRADE_PROTOTYPE_VERIFY_PASS');
} catch (error) {
  console.error('ORG_GRADE_PROTOTYPE_VERIFY_FAIL:',error.message);
  process.exitCode = 1;
}
