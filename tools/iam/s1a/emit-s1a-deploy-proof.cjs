#!/usr/bin/env node
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const {makeProof, distDirectory} = require('./iam-runtime-contract.cjs');
try {
  const proof = makeProof();
  const output = path.join(distDirectory(), 'iam-s1a-deploy.json');
  fs.writeFileSync(output, JSON.stringify(proof, null, 2) + '\n', {encoding: 'utf8', flag: 'w'});
  console.log('IAM_S1A_C1_DEPLOY_PROOF_CREATED: source digest and compiled JS hashes bound');
} catch (error) {
  console.error('IAM_S1A_C1_DEPLOY_PROOF_FAILED: ' + error.message);
  process.exitCode = 1;
}
