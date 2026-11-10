'use strict';
// BUC-09/10 startup regression: @Repository is proxied by Spring AOP and MUST be subclassable.
// Check both Java source and the actual class bytecode inside the bootable runtime JAR.
const fs=require('node:fs');
const path=require('node:path');
const os=require('node:os');
const crypto=require('node:crypto');
const cp=require('node:child_process');
const root=path.resolve(process.argv[2] || path.join(__dirname,'../../../..'));
const sourceOnly=process.argv.includes('--source-only');
const rel='com/behsazan/corebanking/collateral/posting/CollateralPostingRepository';
const src=path.join(root,'backend','src','main','java',...rel.split('/'))+'.java';
const compiled=path.join(root,'backend','target','classes',...rel.split('/'))+'.class';
const jar=path.join(root,'app','core-banking-prototype.jar');
function fail(code){console.error('CLT_POSTING_PROXY_VERIFY_FAIL='+code);process.exit(1);}
function digest(b){return crypto.createHash('sha256').update(b).digest('hex');}
function sourceCheck(){
  if(!fs.existsSync(src))fail('SOURCE_MISSING');
  const text=fs.readFileSync(src,'utf8');
  if(!/@Repository\s+public\s+class\s+CollateralPostingRepository\s*\{/.test(text))
    fail('REPOSITORY_MUST_BE_NONFINAL_FOR_SPRING_AOP');
  const collateral=path.join(root,'backend','src','main','java','com','behsazan','corebanking','collateral');
  function scan(d){
    for(const entry of fs.readdirSync(d,{withFileTypes:true})){
      const p=path.join(d,entry.name);
      if(entry.isDirectory())scan(p);
      else if(entry.isFile() && entry.name.endsWith('.java') &&
        /@Repository\b[\s\S]{0,160}?\bfinal\s+class\s+\w+/.test(fs.readFileSync(p,'utf8')))
        fail('FINAL_REPOSITORY_SOURCE '+path.relative(root,p));
    }
  }
  scan(collateral);
}
// Minimal Java class parser: access_flags is immediately after constant_pool.
function flags(b){
  if(b.length<12 || b.readUInt32BE(0)!==0xCAFEBABE)fail('INVALID_CLASS_MAGIC');
  const count=b.readUInt16BE(8);let pos=10;
  for(let i=1;i<count;i++){
    if(pos>=b.length)fail('TRUNCATED_CONSTANT_POOL');
    const tag=b.readUInt8(pos++);
    if(tag===1){if(pos+2>b.length)fail('TRUNCATED_UTF8');const length=b.readUInt16BE(pos);pos+=2+length;}
    else if(tag===3||tag===4||tag===9||tag===10||tag===11||tag===12||tag===17||tag===18)pos+=4;
    else if(tag===5||tag===6){pos+=8;i++;}
    else if(tag===7||tag===8||tag===16||tag===19||tag===20)pos+=2;
    else if(tag===15)pos+=3;
    else fail('UNSUPPORTED_CONSTANT_POOL_TAG '+tag);
    if(pos>b.length)fail('TRUNCATED_CONSTANT_POOL_DATA');
  }
  if(pos+2>b.length)fail('TRUNCATED_ACCESS_FLAGS');
  return b.readUInt16BE(pos);
}
function bytecodeCheck(bytes,label){
  const f=flags(bytes);
  if((f & 0x0010)!==0)fail('FINAL_BYTECODE '+label);
  if((f & 0x0001)===0)fail('NON_PUBLIC_BYTECODE '+label);
  return digest(bytes);
}
sourceCheck();
if(sourceOnly){console.log('CLT_POSTING_PROXY_SOURCE_PASS=YES; FINAL_REPOSITORIES=0');process.exit(0);}
if(!fs.existsSync(compiled))fail('COMPILED_CLASS_MISSING_BUILD_FIRST');
if(!fs.existsSync(jar))fail('RUNTIME_JAR_MISSING_BUILD_FIRST');
const sourceClassDigest=bytecodeCheck(fs.readFileSync(compiled),'MAVEN_TARGET');
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'clt-posting-proxy-'));
try{
  const member='BOOT-INF/classes/'+rel+'.class';
  const result=cp.spawnSync('jar',['xf',jar,member],{cwd:temp,encoding:'utf8',timeout:30000});
  if(result.error || result.status!==0)fail('JAR_EXTRACT_FAILED '+(result.error&&result.error.message || result.stderr));
  const packed=path.join(temp,...member.split('/'));
  if(!fs.existsSync(packed))fail('RUNTIME_JAR_CLASS_MISSING');
  const runtimeDigest=bytecodeCheck(fs.readFileSync(packed),'RUNTIME_JAR');
  if(sourceClassDigest!==runtimeDigest)fail('RUNTIME_JAR_NOT_SAME_AS_COMPILED_CLASS');
  console.log('CLT_POSTING_PROXY_RUNTIME_PASS=YES; SOURCE_NONFINAL=YES; COMPILED_NONFINAL=YES; JAR_NONFINAL=YES; JAR_MATCH=YES');
}finally{fs.rmSync(temp,{recursive:true,force:true});}
