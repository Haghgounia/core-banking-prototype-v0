#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path');
const {ROOT,PROOF,create}=require('./ro2-runtime-proof.cjs');
const dst=path.join(ROOT,PROOF);fs.writeFileSync(dst,JSON.stringify(create(),null,2)+'\n');
console.log('IAM_S1B_RO2_BUILD_PROOF_EMITTED');
