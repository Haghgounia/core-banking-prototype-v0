#!/usr/bin/env node
'use strict';
const path = require('node:path');
const {ROOT, verifyJar} = require('./iam-runtime-contract.cjs');
const jar = process.argv[2] ? path.resolve(process.argv[2]) : path.join(ROOT, 'app/core-banking-prototype.jar');
try {
  const proof = verifyJar(jar);
  console.log('IAM_S1A_C1_JAR_PASS: ' + proof.release + '; route and UI verified inside runtime JAR; source fingerprint matched');
} catch (error) {
  console.error('IAM_S1A_C1_JAR_FAILED: ' + error.message);
  console.error('Rebuild with INSTALL-AND-RUN.cmd; do not launch a previous 0.11.0 JAR.');
  process.exitCode = 1;
}
