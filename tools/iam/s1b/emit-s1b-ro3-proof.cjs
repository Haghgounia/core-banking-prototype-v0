#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path');
const {ROOT,PROOF,create}=require('./ro3-runtime-proof.cjs');
fs.writeFileSync(path.join(ROOT,PROOF),JSON.stringify(create(),null,2)+'\n');
console.log('IAM_S1B_RO3_BUILD_PROOF_EMITTED');
