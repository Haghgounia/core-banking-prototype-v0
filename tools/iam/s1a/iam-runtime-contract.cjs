'use strict';
// IAM-S1A UI.3: deterministic, read-only build / artifact evidence.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const zlib = require('node:zlib');

const RELEASE = 'IAM-S1A-C1';
const ROUTE = 'iam/user-management';
const UI_MARKER = 'IAM-S1A';
const ROOT = path.resolve(__dirname, '../../..');
const SOURCE_FILES = Object.freeze([
  'frontend/src/app/app.config.ts',
  'frontend/src/app/app.routes.ts',
  'frontend/src/app/layout/app-shell.component.html',
  'frontend/src/app/layout/sidebar-r22/sidebar-r22-group.component.ts',
  'frontend/src/app/features/iam/iam-s1a-demo.component.ts',
  'frontend/src/app/features/iam/iam-s1a-demo.component.html',
  'frontend/src/app/features/iam/iam-s1a-demo.component.scss',
  'frontend/src/app/features/iam/iam-s1a-demo.service.ts',
  'frontend/src/app/features/iam/iam-s1a-domain.ts'
]);
const sha = b => crypto.createHash('sha256').update(b).digest('hex');
function assert(condition, message) { if (!condition) throw new Error(message); }
function sourceHash(root = ROOT) {
  const hash = crypto.createHash('sha256');
  for (const rel of SOURCE_FILES) {
    const b = fs.readFileSync(path.join(root, rel));
    hash.update(rel + '\0');
    hash.update(String(b.length) + '\0');
    hash.update(b);
    hash.update('\0');
  }
  return hash.digest('hex');
}
function distDirectory(root = ROOT) {
  const container = path.join(root, 'frontend/dist/core-banking-ui');
  const browser = path.join(container, 'browser');
  return fs.existsSync(browser) ? browser : container;
}
function compiledAssets(dir) {
  const results = [];
  function visit(folder) {
    for (const de of fs.readdirSync(folder, {withFileTypes: true})) {
      const file = path.join(folder, de.name);
      if (de.isDirectory()) visit(file);
      else if (de.isFile() && /\.m?js$/.test(de.name)) {
        results.push({name: path.relative(dir, file).split(path.sep).join('/'), content: fs.readFileSync(file)});
      }
    }
  }
  visit(dir);
  return results.sort((a, b) => a.name.localeCompare(b.name));
}
function makeProof(root = ROOT) {
  const folder = distDirectory(root);
  assert(fs.existsSync(folder), 'IAM_BUILD_DIST_MISSING');
  const assets = compiledAssets(folder);
  const route = assets.find(x => x.content.includes(Buffer.from(ROUTE)));
  const component = assets.find(x => x.content.includes(Buffer.from(UI_MARKER)));
  assert(route, 'IAM_BUILD_ROUTE_NOT_COMPILED');
  assert(component, 'IAM_BUILD_FORM_NOT_COMPILED');
  return {
    release: RELEASE,
    route: '/' + ROUTE,
    sourceSha256: sourceHash(root),
    routeAsset: route.name,
    routeAssetSha256: sha(route.content),
    componentAsset: component.name,
    componentAssetSha256: sha(component.content)
  };
}

// Small ZIP central-directory reader; no third-party package, no shell tools.
// Supports standard Spring Boot JAR ZIP archives (< 4 GiB).
function openJar(jarPath) {
  const fd = fs.openSync(jarPath, 'r');
  const size = fs.fstatSync(fd).size;
  function block(position, length) {
    assert(position >= 0 && length >= 0 && position + length <= size, 'IAM_JAR_ZIP_BOUNDS');
    const out = Buffer.alloc(length);
    let done = 0;
    while (done < length) {
      const n = fs.readSync(fd, out, done, length - done, position + done);
      assert(n > 0, 'IAM_JAR_ZIP_TRUNCATED');
      done += n;
    }
    return out;
  }
  try {
    const length = Math.min(65557, size);
    const tail = block(size - length, length);
    let eocd = -1;
    for (let i = length - 22; i >= 0; i--) {
      if (tail.readUInt32LE(i) === 0x06054b50 && i + 22 + tail.readUInt16LE(i + 20) <= length) {
        eocd = i;
        break;
      }
    }
    assert(eocd >= 0, 'IAM_JAR_ZIP_END_NOT_FOUND');
    const count = tail.readUInt16LE(eocd + 10);
    const offset = tail.readUInt32LE(eocd + 16);
    const span = tail.readUInt32LE(eocd + 12);
    assert(count !== 0xffff && offset !== 0xffffffff && span !== 0xffffffff, 'IAM_JAR_ZIP64_NOT_SUPPORTED');
    const entries = new Map();
    let cursor = offset;
    for (let i = 0; i < count; i++) {
      const hdr = block(cursor, 46);
      assert(hdr.readUInt32LE(0) === 0x02014b50, 'IAM_JAR_ZIP_CENTRAL_DIRECTORY_CORRUPT');
      const method = hdr.readUInt16LE(10);
      const packed = hdr.readUInt32LE(20);
      const unpacked = hdr.readUInt32LE(24);
      const nameLen = hdr.readUInt16LE(28);
      const extraLen = hdr.readUInt16LE(30);
      const commentLen = hdr.readUInt16LE(32);
      const local = hdr.readUInt32LE(42);
      const name = block(cursor + 46, nameLen).toString('utf8');
      entries.set(name, {method, packed, unpacked, local});
      cursor += 46 + nameLen + extraLen + commentLen;
    }
    assert(cursor === offset + span, 'IAM_JAR_ZIP_CENTRAL_DIRECTORY_SIZE_MISMATCH');
    function extract(name) {
      const e = entries.get(name);
      assert(e, 'IAM_JAR_MISSING_ENTRY: ' + name);
      assert(e.packed <= 100 * 1024 * 1024, 'IAM_JAR_ASSET_TOO_LARGE: ' + name);
      const lh = block(e.local, 30);
      assert(lh.readUInt32LE(0) === 0x04034b50, 'IAM_JAR_BAD_LOCAL_HEADER: ' + name);
      const start = e.local + 30 + lh.readUInt16LE(26) + lh.readUInt16LE(28);
      const buf = block(start, e.packed);
      const raw = e.method === 0 ? buf : e.method === 8 ? zlib.inflateRawSync(buf) : null;
      assert(raw && raw.length === e.unpacked, 'IAM_JAR_INVALID_COMPRESSION: ' + name);
      return raw;
    }
    return {entries, extract, close: () => fs.closeSync(fd)};
  } catch (e) { fs.closeSync(fd); throw e; }
}
function verifyJar(jarPath, root = ROOT) {
  assert(fs.existsSync(jarPath), 'IAM_RUNTIME_JAR_MISSING: ' + jarPath);
  const jar = openJar(jarPath);
  try {
    const prefix = 'BOOT-INF/classes/static/';
    assert(jar.entries.has(prefix + 'index.html'), 'IAM_JAR_INDEX_MISSING');
    const proof = JSON.parse(jar.extract(prefix + 'iam-s1a-deploy.json').toString('utf8'));
    assert(proof.release === RELEASE && proof.route === '/' + ROUTE, 'IAM_JAR_DEPLOY_PROOF_WRONG_RELEASE');
    assert(proof.sourceSha256 === sourceHash(root), 'IAM_JAR_SOURCE_DIFFERS_FROM_BUNDLE: rebuild');
    for (const [fileProp, hashProp, needle] of [
      ['routeAsset', 'routeAssetSha256', ROUTE],
      ['componentAsset', 'componentAssetSha256', UI_MARKER]
    ]) {
      const rel = proof[fileProp];
      assert(typeof rel === 'string' && /^[A-Za-z0-9_./-]+\.m?js$/.test(rel) && !rel.includes('..') && !rel.startsWith('/'),
        'IAM_JAR_INVALID_PROOF_FILENAME: ' + fileProp);
      const b = jar.extract(prefix + rel);
      assert(sha(b) === proof[hashProp], 'IAM_JAR_PROOF_HASH_MISMATCH: ' + rel);
      assert(b.includes(Buffer.from(needle)), 'IAM_JAR_ROUTE_OR_FORM_MISSING: ' + rel);
    }
    return {release: proof.release, sourceSha256: proof.sourceSha256, routeAsset: proof.routeAsset, componentAsset: proof.componentAsset};
  } finally { jar.close(); }
}
module.exports = {RELEASE, ROOT, ROUTE, UI_MARKER, SOURCE_FILES, sha, sourceHash, distDirectory, makeProof, verifyJar};
