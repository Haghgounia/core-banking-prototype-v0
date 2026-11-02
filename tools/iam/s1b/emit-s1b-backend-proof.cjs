#!/usr/bin/env node
'use strict';
const fs=require('node:fs');const path=require('node:path');
const {ROOT,PROOF,create}=require('./s1b-backend-proof.cjs');
const p=path.join(ROOT,PROOF);fs.writeFileSync(p,JSON.stringify(create(),null,2)+'\n');
console.log('IAM_S1B_BUILD_PROOF_EMITTED: '+p);
