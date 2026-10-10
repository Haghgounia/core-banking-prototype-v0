/**
 * PB-R20 Lite HF3: controlled R18 -> R19 source contract restoration.
 * No dependency on Git or npm. Refuses unfamiliar layouts, backs up originals.
 * Does not touch Oracle, FEE2, Angular or unrelated product-builder files.
 */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const base = 'backend/src/main/java/com/behsazan/corebanking/productbuilder/';
const guardName = base + 'application/ProductGovernanceWriteGuard.java';
const controllerName = base + 'web/ProductBuilderController.java';
const argumentsProvided = process.argv.slice(2);
if (argumentsProvided.some(x => x !== '--check')) {
  console.error('Usage: node tools/repair-pb-r19-contracts.mjs [--check]');
  process.exit(2);
}
const checkOnly = argumentsProvided.includes('--check');
const lf = (s) => s.replace(/\r\n/g, '\n');
const matchExactlyOnce = (text, anchor, label) => {
  const count = text.split(anchor).length - 1;
  if (count !== 1) throw new Error(`${label}: expected exactly one anchor, got ${count}`);
};
const sha256 = (s) => crypto.createHash('sha256').update(s).digest('hex');
const files = [guardName, controllerName];
const original = new Map();
for (const name of files) {
  const absolute = path.join(root, name);
  if (!fs.existsSync(absolute)) throw new Error('Missing required source: ' + name);
  const raw = fs.readFileSync(absolute, 'utf8');
  if (!raw.includes('package com.behsazan.corebanking.productbuilder.'))
    throw new Error('Unexpected Java file: ' + name);
  original.set(name, raw);
}

const NON_DRAFT_STOP_METHOD = `    private void assertNonDraftVersionStopOnly(Map<String, Object> existing, Map<String, Object> changes) {
        Set<String> safeKeys = Set.of("ORIGINATION_STATUS_CODE", "SERVICING_STATUS_CODE",
                "IS_CURRENT", "VERSION_STATUS_CODE", "CHANGE_REASON", "RECORD_STATUS_CODE");
        for (var change : changes.entrySet()) {
            if (same(existing.get(change.getKey()), change.getValue())) continue;
            if (!safeKeys.contains(change.getKey())) {
                throw new ProductBuilderValidationException("نسخه مصوب فقط از مسیر توقف/انقضای ایمن قابل تغییر است؛ برای تغییر قواعد یا تاریخ نسخه جدید بسازید.");
            }
            String oldValue = text(existing.get(change.getKey()));
            String newValue = text(change.getValue());
            switch (change.getKey()) {
                case "ORIGINATION_STATUS_CODE" -> {
                    if (!Set.of("CLOSED", "SUSPENDED").contains(newValue))
                        throw new ProductBuilderValidationException("بازگشایی افتتاح نسخه مصوب از ویرایش عمومی مجاز نیست.");
                }
                case "SERVICING_STATUS_CODE" -> {
                    if (!Set.of("CLOSED", "SUSPENDED").contains(newValue))
                        throw new ProductBuilderValidationException("افزایش مجوز خدمت‌رسانی نسخه مصوب از ویرایش عمومی مجاز نیست.");
                }
                case "IS_CURRENT" -> {
                    if (number(change.getValue()) != 0)
                        throw new ProductBuilderValidationException("فعالسازی مجدد نسخه مصوب باید از گردش کار حاکمیتی انجام شود.");
                }
                case "VERSION_STATUS_CODE" -> {
                    if (!"APPROVED".equals(oldValue) || !"EXPIRED".equals(newValue))
                        throw new ProductBuilderValidationException("از وضعیت مصوب فقط انتقال به «منقضی» در مسیر عمومی مجاز است.");
                }
                case "RECORD_STATUS_CODE" -> {
                    if (!"INACTIVE".equals(newValue))
                        throw new ProductBuilderValidationException("رکورد نسخه مصوب فقط می‌تواند غیرفعال شود.");
                }
                default -> { /* change reason is an explanatory audit field */ }
            }
        }
    }

`;

function guardPatch(input) {
  const text = lf(input);
  const signature = '    private void assertNonDraftVersionStopOnly(Map<String, Object> existing, Map<String, Object> changes) {';
  const invocation = '            assertNonDraftVersionStopOnly(existing, changes);';
  const statusSet = 'Set.of("CLOSED", "SUSPENDED")';
  const present = text.includes(signature);
  if (present) {
    if (!text.includes(invocation) || !text.includes(statusSet))
      throw new Error('Guard is partially modified; manual review required; no files changed');
    return text;
  }
  if (text.includes('assertNonDraftVersionStopOnly'))
    throw new Error('Guard has a noncanonical stop-only method; manual review required');
  const methodAnchor = '    public void assertUpdateAllowed(String table, Map<String, Object> existing, Map<String, Object> changes) {';
  const insertCallBefore = '        Long existingVersionId = resolveVersionId(normalized, existing);';
  const methodStart = text.indexOf(methodAnchor);
  const callPosition = text.indexOf(insertCallBefore, methodStart);
  const endUpdate = text.indexOf('    public void assertDeleteAllowed(', methodStart);
  if (methodStart < 0 || callPosition < 0 || endUpdate < 0 || callPosition > endUpdate)
    throw new Error('Unknown guard update method layout; manual merge required');
  if (!text.slice(methodStart, callPosition).includes('assertProductIdentityMutable(existing, changes);'))
    throw new Error('Product identity guard missing; refuse automatic merge');
  matchExactlyOnce(text, insertCallBefore, 'Guard update call location');
  const insertCall = `        if ("PRODUCT_VERSION".equals(normalized) && !"DRAFT".equals(text(existing.get("VERSION_STATUS_CODE")))) {\n            // An approved version is immutable, but emergency tightening must\n            // remain possible (halt origination, suspend servicing, expire).\n            assertNonDraftVersionStopOnly(existing, changes);\n            return;\n        }\n\n`;
  // Keep the original R19 spacing when the source came from PB-R18.
  const updateAnchor = '\n\n' + insertCallBefore;
  const updatedCall = text.includes(updateAnchor)
    ? text.replace(updateAnchor, '\n' + insertCall + insertCallBefore)
    : text.replace(insertCallBefore, insertCall + insertCallBefore);
  let updated = updatedCall;
  const beforeMethod = '    private void assertProductIdentityMutable(';
  matchExactlyOnce(updated, beforeMethod, 'Guard helper method location');
  updated = updated.replace(beforeMethod, NON_DRAFT_STOP_METHOD + beforeMethod);
  if (!updated.includes(statusSet) || !updated.includes(invocation))
    throw new Error('Guard contract check failed before write');
  return updated;
}

function controllerPatch(input) {
  let text = lf(input);
  const endpoint = '@GetMapping("/versions/{versionId}/rule-governance")';
  if (text.includes(endpoint)) {
    if (!text.includes('governance.report(versionId)') || !text.includes('ProductRuleGovernanceService'))
      throw new Error('Controller has noncanonical endpoint; manual review required');
    return text;
  }
  if (text.includes('/versions/{versionId}/rule-governance'))
    throw new Error('Controller has a different governance route; manual review required');
  if (!text.includes('import com.behsazan.corebanking.productbuilder.application.ProductRuleGovernanceService;')) {
    const importAnchor = 'import com.behsazan.corebanking.productbuilder.application.ProductBuilderService;';
    matchExactlyOnce(text, importAnchor, 'Controller import location');
    text = text.replace(importAnchor, importAnchor + '\nimport com.behsazan.corebanking.productbuilder.application.ProductRuleGovernanceService;');
  }
  const field = '    private final ProductRuleGovernanceService governance;';
  if (!text.includes(field)) {
    const fieldAnchor = '    private final ProductBuilderService service;';
    matchExactlyOnce(text, fieldAnchor, 'Controller field location');
    text = text.replace(fieldAnchor, fieldAnchor + '\n' + field);
  }
  if (!text.includes('this.governance = governance;')) {
    const constructor = 'public ProductBuilderController(ProductBuilderService service) {';
    matchExactlyOnce(text, constructor, 'Controller constructor signature');
    text = text.replace(constructor, 'public ProductBuilderController(ProductBuilderService service, ProductRuleGovernanceService governance) {');
    const marker = '        this.service = service;\n    }';
    matchExactlyOnce(text, marker, 'Controller constructor body');
    text = text.replace(marker, '        this.service = service;\n        this.governance = governance;\n    }');
  }
  if (!text.includes('this.governance = governance;'))
    throw new Error('Governance dependency injection not wired');
  const getCatalog = '    @GetMapping("/catalog")';
  matchExactlyOnce(text, getCatalog, 'Controller endpoint insertion location');
  const method = `    @GetMapping("/versions/{versionId}/rule-governance")\n    ProductRuleGovernanceService.GovernanceReport ruleGovernance(@PathVariable long versionId) {\n        return governance.report(versionId);\n    }\n\n`;
  text = text.replace(getCatalog, method + getCatalog);
  return text;
}

const transformations = new Map([
 [guardName, guardPatch],
 [controllerName, controllerPatch],
]);
const changed = [];
const updates = new Map();
try {
  // Validate every file before touching the filesystem.
  for (const name of files) {
    const oldText = original.get(name);
    const fixed = transformations.get(name)(oldText);
    const normalized = oldText.includes('\r\n') ? fixed.replace(/\n/g, '\r\n') : fixed;
    updates.set(name, normalized);
    if (oldText !== normalized) changed.push(name);
  }
  for (const name of files) {
    console.log((changed.includes(name) ? 'NEEDS_PATCH' : 'ALREADY_OK') + ' ' + name);
  }
  if (checkOnly) {
    console.log(`PB_R20_HF3_CHECK_OK NEEDS_PATCH=${changed.length}`);
    process.exit(0);
  }
  if (!changed.length) {
    console.log('PB_R20_HF3_ALREADY_APPLIED');
    process.exit(0);
  }
  const backupRoot = path.join(root, 'docs', 'patch-backups', 'pb-r20-lite-hf3');
  fs.mkdirSync(backupRoot, {recursive:true});
  for (const name of changed) {
    const before = original.get(name);
    const stamp = sha256(before).slice(0, 12);
    const dest = path.join(backupRoot, path.basename(name) + '.' + stamp + '.java.txt');
    if (!fs.existsSync(dest)) fs.writeFileSync(dest, before, {flag:'wx'});
    else if (fs.readFileSync(dest, 'utf8') !== before) throw new Error('Backup content mismatch: ' + dest);
    console.log('BACKUP ' + path.relative(root,dest));
  }
  const installed = [];
  try {
    for (const name of changed) {
      const dest = path.join(root, name);
      fs.writeFileSync(dest, updates.get(name));
      installed.push(name);
      console.log('PATCHED ' + name);
    }
  } catch (error) {
    for (const name of installed) fs.writeFileSync(path.join(root,name),original.get(name));
    throw error;
  }
  console.log(`PB_R20_HF3_MERGE_SUCCESS PATCHED=${changed.length}`);
} catch (error) {
  console.error('PB_R20_HF3_ABORTED: ' + error.message);
  console.error('No forced overwrite. Compare local source with R19 reference and request manual merge if needed.');
  process.exitCode = 1;
}
