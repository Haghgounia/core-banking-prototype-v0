'use strict';
/** Offline IAM S1A developer verification. No Oracle calls, HTTP, or app writes. */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const crypto = require('node:crypto');
const path = require('node:path');
const os = require('node:os');
const {spawnSync} = require('node:child_process');
const root = path.resolve(__dirname, '../../..');
const src = path.join(root, 'frontend/src/app/features/iam/iam-s1a-domain.ts');
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'iam-s1a-'));
let count = 0;
function check(name, fn) { fn(); count++; process.stdout.write(`PASS ${String(count).padStart(2, '0')} ${name}\n`); }
function expectFail(fn, fragment) {
  assert.throws(fn, e => e instanceof Error && e.message.includes(fragment));
}
const originalSnapshot = path.join(root, 'docs/iam/reviews/R1.5.1/IAM-2026-11-02-ORACLE-DDL-EXPORT.txt');
try {
  const localTsc = path.join(root, 'frontend/node_modules/.bin', process.platform === 'win32' ? 'tsc.cmd' : 'tsc');
  const tsc = fs.existsSync(localTsc) ? localTsc : (process.platform === 'win32' ? 'tsc.cmd' : 'tsc');
  const args = ['--target', 'ES2022', '--module', 'commonjs', '--lib', 'ES2022,DOM',
    '--skipLibCheck', '--strict', '--outDir', tmp, src];
  const res = spawnSync(tsc, args, {encoding: 'utf8', shell: process.platform === 'win32', timeout: 60000});
  if (res.error || res.status !== 0) {
    throw new Error(`TypeScript check failed. Install frontend dependencies before VERIFY.cmd.\n${res.error || ''}\n${res.stdout}\n${res.stderr}`);
  }
  const mod = require(path.join(tmp, 'iam-s1a-domain.js'));
  const {IamS1aDemoStore, IAM_DEMO_CONTEXT, isUuidV4, normalizeIamUsername} = mod;
  const make = (username, externalEntityId, overrides = {}) => ({
    context: 'WORKFORCE', securityDomain: 'DEMO_WF', sourceSystem: 'ORG',
    externalEntityType: 'EMPLOYEE', username, externalEntityId,
    evidenceReference: 'MOCK-REQ-ONE', ...overrides
  });
  check('R1.5.1 DDL evidence digest unchanged', () => {
    assert.equal(crypto.createHash('sha256').update(fs.readFileSync(originalSnapshot)).digest('hex'),
      '932efb3e7a52d16c46a0737badcedfbb33f091df417bf654d2dafe5832f89d94');
  });
  check('username NFKC + lowercase + whitespace policy', () => {
    assert.equal(normalizeIamUsername('  ALI.USER  '), 'ali.user');
    assert.equal(normalizeIamUsername('\uff21\uff2c\uff29'), 'ali');
    expectFail(() => normalizeIamUsername('a b'), 'Username');
    expectFail(() => normalizeIamUsername('ab'), 'Username');
  });
  check('demo seed is fake and status-limited', () => {
    const store = new IamS1aDemoStore();
    assert.equal(store.listAccounts().length, 2);
    assert.equal(store.isDemoAccessAllowed('DEMO-1002', 'CUSTOMER', 'DEMO_CU'), false);
  });
  check('new account cannot be active before link proof', () => {
    const store = new IamS1aDemoStore(false);
    const a = store.createAccount(make('new.user', 'EMP-101'));
    assert.equal(a.status, 'PROVISIONING');
    assert.equal(a.linkStatus, 'UNVERIFIED');
    assert.equal(store.isDemoAccessAllowed(a.accountId, 'WORKFORCE', 'DEMO_WF'), false);
    expectFail(() => store.activateAccount(a.accountId, a.recordVersion), 'requires a verified identity link');
  });
  check('new principal UID is UUIDv4 and unique', () => {
    const store = new IamS1aDemoStore(false);
    const a = store.createAccount(make('user.one', 'EMP-101'));
    const b = store.createAccount(make('user.two', 'EMP-102'));
    assert.equal(isUuidV4(a.principalUid), true);
    assert.equal(isUuidV4(b.principalUid), true);
    assert.notEqual(a.principalUid, b.principalUid);
  });
  check('reject wrong security domain, context and source', () => {
    const store = new IamS1aDemoStore(false);
    expectFail(() => store.createAccount(make('wrong.one', 'EMP-201', {securityDomain: 'DEMO_CU'})), 'Context');
    expectFail(() => store.createAccount(make('wrong.one', 'EMP-201', {sourceSystem: 'CIF'})), 'Context');
    expectFail(() => store.createAccount(make('wrong.one', 'EMP-201', {context: 'CUSTOMER'})), 'Context');
  });
  check('reject normalized duplicate within same domain', () => {
    const store = new IamS1aDemoStore(false);
    store.createAccount(make('Ali.User', 'EMP-301'));
    expectFail(() => store.createAccount(make('ali.user', 'EMP-302')), 'already exists');
    expectFail(() => store.createAccount(make('  ALI.USER ', 'EMP-302')), 'already exists');
  });
  check('same username is permitted in a separate security domain', () => {
    const store = new IamS1aDemoStore(false);
    store.createAccount(make('demo.same', 'EMP-401'));
    const second = store.createAccount(make('demo.same', 'PARTY-401', {
      context: 'CUSTOMER', ...{securityDomain: IAM_DEMO_CONTEXT.CUSTOMER.domain,
        sourceSystem: 'CIF', externalEntityType: 'PARTY'}
    }));
    assert.equal(second.context, 'CUSTOMER');
  });
  check('reject external reference re-link to another principal', () => {
    const store = new IamS1aDemoStore(false);
    store.createAccount(make('one.user', 'EMP-501'));
    expectFail(() => store.createAccount(make('two.user', 'EMP-501')), 'already linked');
  });
  check('rename preserves principal UID, account ID and external link', () => {
    const store = new IamS1aDemoStore(false);
    const before = store.createAccount(make('first.user', 'EMP-601'));
    const after = store.renameAccount(before.accountId, before.recordVersion, 'SECOND.USER');
    assert.equal(before.accountId, after.accountId);
    assert.equal(before.principalUid, after.principalUid);
    assert.equal(before.externalEntityId, after.externalEntityId);
    assert.equal(after.usernameNormalized, 'second.user');
    assert.equal(after.recordVersion, 2);
  });
  check('optimistic version conflict is rejected', () => {
    const store = new IamS1aDemoStore(false);
    const a = store.createAccount(make('version.user', 'EMP-701'));
    store.renameAccount(a.accountId, 1, 'version.new');
    expectFail(() => store.suspendAccount(a.accountId, 1), 'Record version conflict');
  });
  check('mock verification, activation and suspension enforce access decisions', () => {
    const store = new IamS1aDemoStore(false);
    const a = store.createAccount(make('access.user', 'EMP-801'));
    const v = store.simulateExternalProof(a.accountId, 1);
    const active = store.activateAccount(a.accountId, v.recordVersion);
    assert.equal(store.isDemoAccessAllowed(a.accountId, 'WORKFORCE', 'DEMO_WF'), true);
    assert.equal(store.isDemoAccessAllowed(a.accountId, 'CUSTOMER', 'DEMO_CU'), false);
    const suspended = store.suspendAccount(a.accountId, active.recordVersion);
    assert.equal(suspended.status, 'SUSPENDED');
    assert.equal(store.isDemoAccessAllowed(a.accountId, 'WORKFORCE', 'DEMO_WF'), false);
    expectFail(() => store.renameAccount(a.accountId, suspended.recordVersion, 'no.user'), 'Suspended');
  });
  check('invalid UUID factory is rejected', () => {
    const store = new IamS1aDemoStore(false, () => 'not-uuid');
    expectFail(() => store.createAccount(make('uuid.user', 'EMP-901')), 'UUIDv4');
  });
  check('no browser-to-server IAM write integration', () => {
    for (const n of ['iam-s1a-demo.component.ts', 'iam-s1a-demo.service.ts', 'iam-s1a-domain.ts']) {
      const s = fs.readFileSync(path.join(root, 'frontend/src/app/features/iam', n), 'utf8');
      assert.doesNotMatch(s, /HttpClient|fetch\(|XMLHttpRequest|localStorage|sessionStorage|document\.cookie/);
    }
  });
  check('navigation exposes isolated IAM DEV route', () => {
    const routes = fs.readFileSync(path.join(root, 'frontend/src/app/app.routes.ts'), 'utf8');
    const nav = fs.readFileSync(path.join(root, 'frontend/src/app/layout/app-shell.component.html'), 'utf8');
    assert.match(routes, /path: 'iam\/user-management'/);
    assert.match(nav, /routerLink="\/iam\/user-management"/);
  });
  check('IAM direct navigation activates and expands its sidebar group', () => {
    const navState = fs.readFileSync(path.join(root, 'frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.ts'), 'utf8');
    assert.match(navState, /this\.isIn\(url, \['\/iam'\]\)\) return 'iam';/);
    assert.match(navState, /this\.manuallyExpanded\.set\(undefined\)/);
  });
  check('IAM route is addressed using hash routing and breadcrumb is registered', () => {
    const config = fs.readFileSync(path.join(root, 'frontend/src/app/app.config.ts'), 'utf8');
    const crumb = fs.readFileSync(path.join(root, 'frontend/src/app/shared/ui/app-breadcrumb.component.ts'), 'utf8');
    assert.match(config, /provideRouter\(routes, withHashLocation\(\)\)/);
    assert.match(crumb, /'\/iam\/user-management'/);
  });
  process.stdout.write(`IAM_S1A_DEV_QA_PASS=${count}/${count}; NO_ORACLE_IO; NO_HTTP_WRITES; NO_AUTH_CLAIM\n`);
} catch (error) {
  console.error(`IAM_S1A_DEV_QA_FAILED=${count}; ${error.stack || error}`);
  process.exitCode = 1;
} finally {
  fs.rmSync(tmp, {recursive: true, force: true});
}
