"""Fail-closed additive-only R4C3 installer: hash-guarded, idempotent, no data writes."""
from __future__ import annotations
import argparse,hashlib,json,os,sys,tempfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[3]
MANIFEST=HERE/'manifest.json'
OVERLAY=HERE/'overlay'

def sha(payload:bytes)->str:return hashlib.sha256(payload).hexdigest()

def secure_path(relative:str)->Path:
    p=Path(relative)
    if (not relative or p.is_absolute() or '..' in p.parts or p.as_posix()!=relative or
        '\\' in relative):raise ValueError('Unsafe manifest path')
    current=ROOT
    for part in p.parts:
        current=current/part
        if current.is_symlink():raise ValueError('Symlink target/parent forbidden: '+relative)
    return current

def preflight(manifest):
    status={}
    for name,expected in manifest['anchors'].items():
        f=secure_path(name)
        status['ANCHOR '+name]='MATCH' if f.is_file() and sha(f.read_bytes())==expected else 'CONFLICT'
    for name,expected in manifest['files'].items():
        src=OVERLAY/name
        if src.is_symlink() or not src.is_file() or sha(src.read_bytes())!=expected:
            status[name]='CONFLICT'
            continue
        dest=secure_path(name)
        status[name]='READY' if not dest.exists() else (
            'ALREADY_APPLIED' if dest.is_file() and sha(dest.read_bytes())==expected else 'CONFLICT')
    return status

def atomic_create(target:Path,content:bytes):
    target.parent.mkdir(parents=True,exist_ok=True)
    handle,tmp=tempfile.mkstemp(prefix='.fee3-r4c3-',dir=str(target.parent))
    try:
        with os.fdopen(handle,'wb') as f:
            f.write(content);f.flush();os.fsync(f.fileno())
        if target.exists():raise ValueError('Unexpected target appeared during install')
        os.replace(tmp,target)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)

def main():
    parser=argparse.ArgumentParser()
    group=parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--preview',action='store_true')
    group.add_argument('--apply',action='store_true')
    args=parser.parse_args()
    try:
        manifest=json.loads(MANIFEST.read_text(encoding='utf-8'))
        if manifest.get('patchId')!='FEE3-BE-R4C3-20261027' or manifest.get('schema')!=1:
            raise ValueError('Manifest marker mismatch')
        statuses=preflight(manifest)
        for name,result in statuses.items():print(result+' | '+name)
        if 'CONFLICT' in statuses.values():
            print('FEE3_R4C3_CONFLICT_ABORTED_NO_CHANGES',file=sys.stderr);return 2
        if args.preview:
            print('FEE3_R4C3_PREVIEW_PASS_NO_CHANGES');return 0
        installed=[]
        try:
            for name in manifest['files']:
                if statuses[name]=='READY':
                    target=secure_path(name)
                    atomic_create(target,(OVERLAY/name).read_bytes())
                    installed.append(target)
            post=preflight(manifest)
            if any(s not in ('MATCH','ALREADY_APPLIED') for s in post.values()):
                raise ValueError('Post-install integrity mismatch')
        except Exception:
            for target in reversed(installed):target.unlink(missing_ok=True)
            raise
        print('FEE3_R4C3_APPLIED='+str(len(installed)))
        print('FEE3_R4C3_SAFE_INSTALL_PASS');return 0
    except (OSError,ValueError,KeyError,TypeError) as exc:
        print('FEE3_R4C3_INSTALL_ERROR '+str(exc),file=sys.stderr);return 3

if __name__=='__main__':sys.exit(main())
