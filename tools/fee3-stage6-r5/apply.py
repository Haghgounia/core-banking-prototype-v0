from pathlib import Path
import hashlib, json, shutil, sys, datetime
project=Path.cwd()
source=project/'tools/fee3-stage6-r5'
manifest=json.loads((source/'manifest.json').read_text(encoding='utf-8'))
checks=[]
for rel, info in manifest['files'].items():
    target=project/rel
    payload=source/'payload'/rel
    if not target.is_file() or not payload.is_file():
        print('FEE3_R5_MISSING_FILE',rel);sys.exit(2)
    actual=hashlib.sha256(target.read_bytes()).hexdigest()
    if actual not in [info['baseline_sha256'],info['after_sha256']]:
        print('FEE3_R5_BASELINE_MISMATCH',rel,actual);sys.exit(3)
    if hashlib.sha256(payload.read_bytes()).hexdigest()!=info['after_sha256']:
        print('FEE3_R5_PAYLOAD_INVALID',rel);sys.exit(4)
    checks.append((target,payload,actual==info['after_sha256']))
if all(done for _,_,done in checks):
    print('FEE3_R5_ALREADY_APPLIED');sys.exit(0)
stamp=datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
backup=project/'docs/install'/('fee3-r5-backup-'+stamp)
for target,payload,done in checks:
    if done: continue
    dest=backup/target.relative_to(project)
    dest.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(target,dest)
for target,payload,done in checks:
    if not done: shutil.copy2(payload,target)
    print('FEE3_R5_SHA_PASS' if hashlib.sha256(target.read_bytes()).hexdigest()==hashlib.sha256(payload.read_bytes()).hexdigest() else 'FEE3_R5_SHA_FAIL',target.relative_to(project))
print('FEE3_R5_BACKUP',backup)
print('NEXT: bin\\stop.cmd && build-production.cmd; start only after successful build')
