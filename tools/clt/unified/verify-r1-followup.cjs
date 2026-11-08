'use strict';
// CLT R1: source-only contract guard; never calls HTTP or Oracle and never writes bank data.
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../../..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const checks=[];
function pass(label,ok){checks.push({label,ok:!!ok});if(!ok)console.error('FAIL | '+label);else console.log('PASS | '+label);}
const folder='frontend/src/app/features/collateral/';
const contract=read(folder+'clt-r1-uat-contract.ts');
const component=read(folder+'clt-r1-uat-matrix.component.ts');
const html=component; // Inline template preserves the fixed 86 feature-template baseline.
const css=read(folder+'clt-r1-uat-matrix.component.scss');
const tabs=read(folder+'collateral-uat-workbench.component.html');
const parent=read(folder+'collateral-uat-workbench.component.ts');
const referenceCss=read(folder+'clt-reference-workspace.component.scss');
const catalog=read(folder+'clt-reference-catalog.ts');
const roadmap=read('docs/collateral/CLT-ROADMAP-R1.0-FA.md');
const checkpoint=read('docs/collateral/CLT-R1-17-TABLE-RUNTIME-UAT-CHECKPOINT-2026-11-08-FA.md');
pass('R1 reference catalog has 17 explicit descriptors', (catalog.match(/"table": "[A-Z_]+"/g)||[]).length===17);
pass('R1 UAT tab is wired into existing workbench', tabs.includes('data-clt-r1-uat-tab')&&tabs.includes('<app-clt-r1-uat-matrix />')&&parent.includes('CltR1UatMatrixComponent'));
pass('R1 UI uses 17 descriptor metadata, not invented tables',component.includes('CLT_REFERENCE_TABLES')&&component.includes('readonly tables = CLT_REFERENCE_TABLES'));
pass('All-table scan is explicit user action and read-only',html.includes('data-clt-r1-read-only-scan')&&component.includes('async scanAll()'));
pass('Scan reads through existing reference API',component.includes('this.api.rows(table.table, 0, 1')&&component.includes('this.api.record(table.table, evidence.id)'));
pass('Scan cannot write to reference endpoints',!/this\.api\.(createRecord|updateRecord|deleteRecord|propose|decide)\(/.test(component));
pass('Scan uses bounded 3-table batches',component.includes('start += 3')&&component.includes('this.tables.slice(start, start + 3)'));
pass('Count, ID and version read-back checked',contract.includes('cltR1PageEvidence')&&contract.includes('cltR1DetailMatches')&&component.includes('CLT_R1_INDEPENDENT_GET_MISMATCH'));
pass('Empty table is not treated as failed',contract.includes('count === 0')&&component.includes("'EMPTY'"));
pass('No manual acceptance fabricated by DB read',contract.includes("create: 'NOT_RUN'")&&contract.includes("update: 'NOT_RUN'")&&contract.includes("delete: 'NOT_RUN'")&&contract.includes("rules: 'NOT_RUN'"));
pass('Manual result has 4 independent scenarios plus nonempty evidence',contract.includes('CLT_R1_STEPS.every')&&contract.includes('manual.note.trim().length > 0'));
pass('JSON export contains only row count/status, not record payloads',component.includes('rowCount: this.readState(t.table).count')&& !/\b(record|row)\s*:\s*page\./.test(component));
pass('Imported old scan never claims live Oracle evidence',component.includes('this.probes.set({});')&&component.includes("this.lastScanAt.set('')"));
pass('Firefox actions have enough width for 3 icons',referenceCss.includes('min-width:178px')&&referenceCss.includes('text-overflow:clip'));
pass('New UAT panel uses system theme tokens',css.includes('var(--app-surface)')&&css.includes('var(--app-primary)'));
pass('Fixed R0-R7 roadmap and G-10/15/16/17 blockers documented',roadmap.includes('R0..R7')&&['G-10','G-15','G-16','G-17'].every(x=>checkpoint.includes(x)));
const ok=checks.filter(c=>c.ok).length;
console.log(`CLT_R1_FOLLOWUP_STATIC_${ok===checks.length?'PASS':'FAIL'}=${ok}/${checks.length}; ORACLE_UAT=NOT_RUN`);
process.exitCode=ok===checks.length?0:1;
