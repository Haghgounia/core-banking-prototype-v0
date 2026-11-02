'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const ROOT = path.resolve(__dirname, '../../..');
const JAVA = 'backend/src/main/java/com/behsazan/corebanking/iam/s1b/';
const SOURCES = Object.freeze([
  'IamS1bProperties.java','IamS1bConfiguration.java','IamS1bReaderPool.java',
  'IamS1bSecurityConfiguration.java','IamS1bPolicy.java','IamS1bModels.java',
  'IamS1bReadRepository.java','IamS1bReadService.java','IamS1bReadController.java',
  'IamS1bExceptionHandler.java'
]);
const RELEASE = 'IAM-R1.7-S1B-RO.1';
const PROOF = 'backend/src/main/resources/iam-s1b-source-proof.json';
function hash(root=ROOT) {
  const d = crypto.createHash('sha256');
  for(const f of SOURCES) {
    const rel=JAVA+f, buf=fs.readFileSync(path.join(root,rel));
    d.update(rel+'\0'+buf.length+'\0');d.update(buf);d.update('\0');
  }
  for(const rel of ['backend/pom.xml','backend/src/main/resources/application.yml']) {
    const buf=fs.readFileSync(path.join(root,rel));
    d.update(rel+'\0'+buf.length+'\0');d.update(buf);d.update('\0');
  }
  return d.digest('hex');
}
function create(root=ROOT) {
  return {release:RELEASE, sourceSha256:hash(root), defaultEnabled:false,
          endpointCount:4, binding:'jwt-issuer-audience-domain-context', writesEnabled:false};
}
module.exports={ROOT, JAVA, SOURCES, PROOF, RELEASE, hash, create};
