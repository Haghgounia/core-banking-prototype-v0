#!/usr/bin/env node
'use strict';
/**
 * IAM-S1A C1 HTTP release verification. Tests bytes served by the running process,
 * not just a matching VERSION. No database or mutation requests.
 */
const http = require('node:http');
const path = require('node:path');
const {ROOT, sourceHash, sha, verifyJar} = require('./iam-runtime-contract.cjs');
const origin = process.argv[2] || 'http://127.0.0.1:8091';
const deadlineMs = Number(process.env.IAM_HTTP_TIMEOUT_MS || 120000);
const jarPath = process.argv[3] || path.join(ROOT, 'app/core-banking-prototype.jar');
const timeout = Number.isFinite(deadlineMs) && deadlineMs >= 0 && deadlineMs <= 600000 ? deadlineMs : 120000;
const safeName = name => typeof name === 'string' && /^[a-zA-Z0-9_./-]+\.m?js$/.test(name) && !name.startsWith('/') && !name.includes('..');
function request(resource) {
  return new Promise((resolve, reject) => {
    const target = new URL(resource, origin.endsWith('/') ? origin : origin + '/');
    if (!['127.0.0.1', 'localhost', '[::1]'].includes(target.hostname)) return reject(new Error('LOCALHOST_ONLY'));
    const req = http.get(target, {timeout: 7000, headers: {'Cache-Control':'no-cache'}}, res => {
      if (res.statusCode !== 200) {
        res.resume();
        return reject(new Error('HTTP_' + res.statusCode + '_' + resource));
      }
      const chunks = [];
      let length = 0;
      res.on('data', chunk => {
        length += chunk.length;
        if (length > 24 * 1024 * 1024) { req.destroy(new Error('OVERSIZE_HTTP_ASSET')); return; }
        chunks.push(chunk);
      });
      res.on('end', () => resolve(Buffer.concat(chunks)));
      res.on('error', reject);
    });
    req.on('timeout', () => req.destroy(new Error('HTTP_TIMEOUT')));
    req.on('error', reject);
  });
}
async function check() {
  const jar = verifyJar(jarPath);
  const proof = JSON.parse((await request('iam-s1a-deploy.json')).toString('utf8'));
  if (proof.release !== jar.release || proof.sourceSha256 !== sourceHash() || proof.sourceSha256 !== jar.sourceSha256) {
    throw new Error('HTTP_BUILD_NOT_SAME_SOURCE_AS_JAR');
  }
  for (const [nameKey, hashKey, needle] of [
    ['routeAsset','routeAssetSha256','iam/user-management'],
    ['componentAsset','componentAssetSha256','IAM-S1A']
  ]) {
    const name = proof[nameKey];
    if (!safeName(name) || proof[nameKey] !== jar[nameKey]) throw new Error('HTTP_ASSET_NAME_MISMATCH');
    const asset = await request(name);
    if (sha(asset) !== proof[hashKey] || !asset.includes(Buffer.from(needle))) throw new Error('HTTP_ASSET_STALE: ' + name);
  }
  console.log('IAM_UI_HTTP_PASS: live ' + origin + ' proof, route and component assets match source and JAR');
}
(async () => {
  const started = Date.now();
  let lastError;
  while (true) {
    try { await check(); return; }
    catch (e) { lastError = e; }
    if (Date.now() - started >= timeout) break;
    await new Promise(resolve => setTimeout(resolve, 1200));
  }
  console.error('IAM_UI_HTTP_FAILED: ' + (lastError?.message || 'Unknown error'));
  console.error('Server on port 8091 is not running the freshly built IAM JAR; inspect logs or target folder.');
  process.exitCode = 1;
})();
