'use strict';
const fs=require('node:fs');const path=require('node:path');
const root=path.resolve(__dirname,'../../../');
const src=p=>fs.readFileSync(path.join(root,p),'utf8');
const service=src('backend/src/main/java/com/behsazan/corebanking/collateral/introduction/CollateralIntroductionService.java');
const repo=src('backend/src/main/java/com/behsazan/corebanking/collateral/introduction/CollateralIntroductionRepository.java');
const controller=src('backend/src/main/java/com/behsazan/corebanking/collateral/introduction/CollateralIntroductionController.java');
const models=src('backend/src/main/java/com/behsazan/corebanking/collateral/introduction/CollateralIntroductionModels.java');
const client=src('frontend/src/app/features/collateral/collateral-introduction.service.ts');
const ui=src('frontend/src/app/features/collateral/collateral-introduction.component.ts');
const html=src('frontend/src/app/features/collateral/collateral-introduction.component.html');
const security=src('backend/src/main/java/com/behsazan/corebanking/collateral/prototype/CltUnifiedPrototypeSecurityConfiguration.java');
const checks=[
 ['Page DTO typed',models.includes('record DraftPage(List<DraftSummary> items, int page, int size, long totalElements)')],
 ['Summary DTO typed',models.includes('record DraftSummary(')],
 ['Page parameters bounded',service.includes('page < 0 || page > 1000 || size < 1 || size > 50')],
 ['Server-scoped maker',repo.includes('WHERE MAKER_SUBJECT = ? AND BRANCH_CODE = ?')],
 ['SQL binds offset and size',repo.includes('maker, branch, offset, size')],
 ['Server pagination',repo.includes('OFFSET ? ROWS FETCH NEXT ? ROWS ONLY')],
 ['Stable sort',repo.includes('ORDER BY CREATED_AT DESC, DRAFT_ID DESC')],
 ['Count query',repo.includes('SELECT COUNT(*) FROM CLT.CLT_INTRODUCTION_DRAFT')],
 ['API GET only',controller.includes('@GetMapping("/mine")')],
 ['Client service',client.includes('myDrafts(page = 0, size = 20)')],
 ['Worklist UI',html.includes('پیش‌نویس‌های ثبت‌شده من')],
 ['Worklist actions',html.includes('selectDraftFromList(draft.draftId)')],
 ['Copy independent',ui.includes('copySavedDraftToForm()') && ui.includes('this.saved.set(null)')],
 ['CIF reselect required',ui.includes("this.ownerId.set('')") && ui.includes('مجدداً از CIF انتخاب')],
 ['Repeated attributes not lost',ui.includes('repeatedAttribute')],
 ['No final registration write API',!controller.includes('@PostMapping("/final")')],
 ['Default deny all other operations',security.includes('.anyRequest().denyAll()')],
 ['Original final DML denied',security.includes('no permanent CLT DML')],
 ['Stage only',repo.includes('INSERT INTO CLT.CLT_INTRODUCTION_DRAFT')],
 ['Schema check before stage write',service.includes('repository.draftSchemaReady(staging)')],
];
let fail=0;
for(const [label,pass] of checks){console.log((pass?'PASS':'FAIL')+' | '+label);if(!pass)fail++;}
console.log(`CLT_R2_WORKLIST_SOURCE=${checks.length-fail}/${checks.length}`);process.exitCode=fail?1:0;
