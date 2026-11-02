#!/usr/bin/env node
'use strict';
/** Visibility guard. Source routes/menu and compiled Angular static asset must agree.
 * CLI: node tools/iam/s1a/verify-s1a-visibility.cjs source|dist|static
 * No HTTP requests, no database calls, no writes.
 */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../../..');
const mode = process.argv[2] || 'source';
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const ensure = (condition, message) => assert.ok(condition, message);
function checkSource() {
  const routes = read('frontend/src/app/app.routes.ts');
  const html = read('frontend/src/app/layout/app-shell.component.html');
  const sidebarState = read('frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.ts');
  const component = read('frontend/src/app/features/iam/iam-s1a-demo.component.ts');
  const template = read('frontend/src/app/features/iam/iam-s1a-demo.component.html');
  const hashRouter = read('frontend/src/app/app.config.ts');
  ensure(routes.includes("path: 'iam/user-management'") && routes.includes('IamS1aDemoComponent'), 'IAM_ROUTE_MISSING');
  ensure(html.includes('groupId="iam"') && html.includes('routerLink="/iam/user-management"'), 'IAM_MENU_MISSING');
  ensure(sidebarState.includes("this.isIn(url, ['/iam'])") && sidebarState.includes("return 'iam';"), 'IAM_MENU_AUTOEXPAND_MISSING');
  ensure(hashRouter.includes('withHashLocation()'), 'IAM_HASH_ROUTING_MISSING');
  ensure(component.includes('IamS1aDemoService') && template.includes('IAM-S1A'), 'IAM_COMPONENT_MISSING');
  console.log('IAM_UI_SOURCE_VISIBLE_PASS: route, menu, sidebar active section, component, hash router');
}
function jsFiles(directory) {
  let files = [];
  function scan(dir) {
    for (const item of fs.readdirSync(dir, {withFileTypes: true})) {
      const p = path.join(dir, item.name);
      if (item.isDirectory()) scan(p);
      else if (item.isFile() && /\.(m?js)$/.test(item.name)) files.push(p);
    }
  }
  scan(directory);
  return files;
}
function checkOutput(folder, label) {
  ensure(fs.existsSync(folder), `IAM_${label}_ASSETS_MISSING: ${folder}`);
  const files = jsFiles(folder);
  ensure(files.length > 0, `IAM_${label}_JS_MISSING: ${folder}`);
  let routeFound = false, componentFound = false;
  for (const f of files) {
    const js = fs.readFileSync(f, 'utf8');
    if (js.includes('iam/user-management')) routeFound = true;
    if (js.includes('IAM-S1A')) componentFound = true;
  }
  ensure(routeFound, `IAM_${label}_ROUTE_NOT_IN_BUILT_JS: rebuild Angular`);
  ensure(componentFound, `IAM_${label}_COMPONENT_NOT_IN_BUILT_JS: stale Angular output`);
  console.log(`IAM_UI_${label}_VISIBLE_PASS: ${files.length} JS assets; route and compiled IAM component verified`);
}
try {
  if (mode === 'source') checkSource();
  else if (mode === 'dist') checkOutput(path.join(root, 'frontend/dist/core-banking-ui'), 'DIST');
  else if (mode === 'static') checkOutput(path.join(root, 'backend/src/main/resources/static'), 'STATIC');
  else throw new Error(`Unknown visibility mode: ${mode}`);
} catch (error) {
  console.error(`IAM_UI_${mode.toUpperCase()}_VERIFY_FAIL: ${error.message}`);
  process.exitCode = 1;
}
