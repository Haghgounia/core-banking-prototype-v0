#!/usr/bin/env node
/** CAL2 R2-B2 FIX5: Form layout regression checks. Read-only; no Oracle access. */
import {readFileSync} from 'node:fs';
import {join} from 'node:path';
import {fileURLToPath} from 'node:url';
const root = fileURLToPath(new URL('..', import.meta.url));
const read = relative => readFileSync(join(root, relative), 'utf8');
const folder = 'frontend/src/app/features/calendar2-reference/';
const html = read(folder + 'calendar2-month-view.component.html');
const css = read(folder + 'calendar2-month-view.component.scss');
const ts = read(folder + 'calendar2-month-view.component.ts');
const tests = [];
const check = (name, ok) => { tests.push({name, ok: !!ok}); };
const asideStart = html.indexOf('<aside class="day-details');
const asideEnd = html.indexOf('</aside>', asideStart);
const editorStart = html.indexOf('@if (editorOpen())');
const editorEnd = html.indexOf('</form>', editorStart);
const editor = html.slice(editorStart, editorEnd);
check('Editor is outside narrow day-details sidebar', asideStart >= 0 && asideEnd > asideStart && editorStart > asideEnd);
check('Exactly one personal editor form', (html.match(/<form class="personal-editor"/g) ?? []).length === 1);
check('Accessible modal dialog', /class="personal-editor" role="dialog" aria-modal="true" aria-labelledby=/.test(editor) && /id="cal2-personal-editor-title"/.test(editor));
check('Focus trap and automatic entry focus', editor.includes('cdkTrapFocus') && editor.includes('cdkTrapFocusAutoCapture') && editor.includes('cdkFocusInitial') && ts.includes('A11yModule'));
check('Escape and explicit cancel close editor', editor.includes('(keydown.escape)="editorOpen.set(false)"') && editor.includes('>انصراف</button>'));
check('Personal save and new/edit controls remain intact', editor.includes('(ngSubmit)="savePersonalEntry()"') && ts.includes('startPersonalEntry(kind') && ts.includes('editPersonalEntry(item'));
check('Both clock widgets remain shared inputs', (editor.match(/<app-time-input/g) ?? []).length === 2 && editor.includes('label="ساعت شروع کار یا قرار"') && editor.includes('label="ساعت نمایش پیام"'));
check('Short clock hints avoid multi-line fixed Material footers', (editor.match(/hint="قالب ۲۴ ساعته \(HH:mm\)"/g) ?? []).length === 2);
check('Reminder lead-day helper is outside Material subscript', editor.includes('class="personal-editor__help"') && !editor.includes('<mat-hint>'));
check('Independent modal width and scrolling', css.includes('width: min(100%, 640px)') && css.includes('overflow-y: auto') && css.includes('max-height: min(88dvh, 880px)'));
check('Small screens get responsive modal', css.includes('@media (max-width: 600px)') && css.includes('max-height: calc(100dvh - 18px)'));
check('CDK select/clock overlays remain above dialog', css.includes('z-index: 900') && css.includes('z-index: 1;'));
check('Scrollable form keeps save/cancel accessible', css.includes('.personal-editor-modal .personal-editor__buttons') && css.includes('position: sticky;'));
const failed = tests.filter(t => !t.ok);
for (const t of tests) console.log((t.ok ? 'PASS' : 'FAIL') + ' | ' + t.name);
if (failed.length) { console.error('CAL2_R2B2_FORM_LAYOUT_FAIL=' + failed.length); process.exit(1); }
console.log('CAL2_R2B2_FORM_LAYOUT_PASS=' + tests.length + ' FAIL=0');
