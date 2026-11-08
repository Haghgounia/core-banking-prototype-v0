'use strict';
// R2: check runtime classes and actual CGLIB compatibility of @Repository bytecode.
// This proof prevents Maven's green unit tests from hiding an unstartable final repository.
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const {execFileSync} = require('node:child_process');
const jarPath = process.argv[2] ? path.resolve(process.argv[2]) : '';
if (!jarPath || !fs.existsSync(jarPath)) { console.error('CLT_UNIFIED_JAR_MISSING'); process.exit(1); }
const entries = new Set(execFileSync('jar',['tf',jarPath],{encoding:'utf8',maxBuffer:16*1024*1024}).split(/\r?\n/));
const checks = [
  ['api-controller','BOOT-INF/classes/com/behsazan/corebanking/collateral/reference/CltUnifiedReferenceController.class'],
  ['oracle-shared-writer','BOOT-INF/classes/com/behsazan/corebanking/collateral/reference/CltUnifiedReferenceRepository.class'],
  ['localhost-security','BOOT-INF/classes/com/behsazan/corebanking/collateral/prototype/CltUnifiedPrototypeSecurityConfiguration.class'],
  ['frontend-ui','BOOT-INF/classes/static/index.html']
];
for (const [name, entry] of checks) {
  if (!entries.has(entry)) { console.error('CLT_UNIFIED_JAR_PROOF_FAIL '+name+' '+entry); process.exit(1); }
  console.log('PASS | '+name);
}
function classFlags(b) {
  if (b.length < 12 || b.readUInt32BE(0) !== 0xcafebabe) throw new Error('INVALID_CLASS_HEADER');
  let p=8;
  const cpCount=b.readUInt16BE(p);p+=2;
  for (let i=1; i<cpCount; i++) {
    const tag=b.readUInt8(p++);
    switch(tag) {
      case 1: { const size=b.readUInt16BE(p);p+=2+size;break; }
      case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18: p+=4;break;
      case 5: case 6:p+=8;i++;break;
      case 7: case 8: case 16: case 19: case 20:p+=2;break;
      case 15:p+=3;break;
      default:throw new Error('INVALID_CONSTANT_POOL_TAG_'+tag);
    }
    if(p>b.length-2) throw new Error('TRUNCATED_CLASS');
  }
  return b.readUInt16BE(p);
}
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'clt-r2-jar-'));
try {
  const entry=checks[1][1];
  execFileSync('jar',['xf',jarPath,entry],{cwd:tmp,windowsHide:true,maxBuffer:8*1024*1024});
  const classFile=path.join(tmp,...entry.split('/'));
  if (!fs.existsSync(classFile)) throw new Error('CLT_REPOSITORY_BYTECODE_MISSING');
  const flags=classFlags(fs.readFileSync(classFile));
  if ((flags & 0x0010)!==0) throw new Error('CLT_UNIFIED_CGLIB_PROXY_INCOMPATIBLE: Repository class is final');
  console.log('PASS | runtime-repository-nonfinal-CGLIB-compatible');
} catch(e) {
  console.error('CLT_UNIFIED_JAR_PROXY_PROOF_FAILED: '+e.message);
  process.exitCode=1;
} finally { fs.rmSync(tmp,{force:true,recursive:true}); }
if (process.exitCode) process.exit(1);
console.log('CLT_UNIFIED_OFFICIAL_JAR_PROOFS_PASS=5/5');
