#!/usr/bin/env python3
"""Fail-closed, additive FEE3 BE-R2 installer. Python 3.11, no external packages."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
from datetime import datetime

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
MANIFEST = HERE / 'manifest.json'
OVERLAY = HERE / 'overlay'

WRAPPERS = {
    'build-production.cmd': (
        b'node "%ROOT%tools\\fee3-backend\\verify-r1.mjs" || exit /b 1',
        b'node "%ROOT%tools\\fee3-backend\\verify-r2.mjs" || exit /b 1',
    ),
    'build-production.sh': (
        b'node "$ROOT/tools/fee3-backend/verify-r1.mjs"',
        b'node "$ROOT/tools/fee3-backend/verify-r2.mjs"',
    ),
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def inside(root, rel):
    p = Path(rel)
    if p.is_absolute() or '..' in p.parts or not p.parts:
        raise ValueError('Unsafe file path: ' + rel)
    candidate = root / p
    if any(x.is_symlink() for x in [candidate, *candidate.parents] if x != root.parent and x != Path('/')):
        # Security fail closed: do not follow symlinked source or destination paths.
        raise ValueError('Symlink in patch path: ' + rel)
    return candidate


def inspect(manifest):
    states = {}
    for rel, info in manifest['files'].items():
        dest = inside(ROOT,rel)
        src = inside(OVERLAY,rel)
        if not src.is_file() or digest(src.read_bytes()) != info['after']:
            states[rel] = ('CONFLICT','overlay missing or checksum mismatch')
        elif dest.is_dir():
            states[rel] = ('CONFLICT','target path is directory')
        elif dest.is_file():
            current = digest(dest.read_bytes())
            if current == info['after']:
                states[rel] = ('ALREADY_APPLIED','')
            elif current == info['before']:
                states[rel] = ('READY','')
            else:
                states[rel] = ('CONFLICT','target differs from known BE-R1 baseline')
        elif info['before'] is None and not dest.exists():
            states[rel] = ('READY','new file')
        else:
            states[rel] = ('CONFLICT','expected baseline file missing')
    for rel, (anchor,addition) in WRAPPERS.items():
        dest = ROOT/rel
        if not dest.is_file() or dest.is_symlink():
            states[rel] = ('CONFLICT','build wrapper missing or symlink')
        else:
            data = dest.read_bytes()
            if addition in data:
                states[rel] = ('ALREADY_APPLIED','')
            elif data.count(anchor)==1:
                states[rel] = ('READY','append verifier only; preserve all other lines')
            else:
                states[rel] = ('CONFLICT','cannot locate unique BE-R1 verifier anchor')
    return states


def wrapper_update(rel, data):
    anchor,addition=WRAPPERS[rel]
    if addition in data:
        return data
    if data.count(anchor)!=1:
        raise ValueError('Unexpected build wrapper: '+rel)
    pos=data.index(anchor)+len(anchor)
    # Add a single verifier line without changing any existing line or its newline style.
    nl = b'\r\n' if b'\r\n' in data else b'\n'
    if data[pos:pos+len(nl)] == nl:
        return data[:pos]+nl+addition+data[pos:]
    raise ValueError('Missing line break after R1 verifier: '+rel)


def atomic_put(dest, data, current=None):
    dest.parent.mkdir(parents=True,exist_ok=True)
    fd,tmp = tempfile.mkstemp(prefix='.fee3-r2-',dir=str(dest.parent))
    try:
        with os.fdopen(fd,'wb') as f:
            f.write(data)
            f.flush()
            os.fsync(f.fileno())
        if current and current.exists():
            shutil.copymode(current,tmp)
        os.replace(tmp,dest)
    finally:
        if os.path.exists(tmp):
            os.unlink(tmp)


def apply(manifest,states):
    pending = [p for p,(s,_) in states.items() if s=='READY']
    if not pending:
        print('FEE3_BE_R2_INSTALL_ALREADY_APPLIED: no files changed')
        return 0
    stamp=datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    backup=ROOT/'docs'/'install'/('fee3-r2-backup-'+stamp)
    backups={}
    original_modes={}
    written=[]
    try:
        for rel in pending:
            dest=ROOT/rel
            if dest.is_file():
                snapshot=backup/rel
                snapshot.parent.mkdir(parents=True,exist_ok=True)
                shutil.copy2(dest,snapshot)
                backups[rel]=snapshot
            else:
                backups[rel]=None
        for rel in pending:
            dest=ROOT/rel
            if rel in WRAPPERS:
                data=wrapper_update(rel,dest.read_bytes())
            else:
                data=(OVERLAY/rel).read_bytes()
            atomic_put(dest,data,dest if dest.is_file() else None)
            written.append(rel)
        after=inspect(manifest)
        bad=[(p,s,reason) for p,(s,reason) in after.items() if s!='ALREADY_APPLIED']
        if bad:
            raise ValueError('Post-install verification failed: '+str(bad))
        print('FEE3_BE_R2_INSTALL_APPLIED='+str(len(pending)))
        print('FEE3_BE_R2_BACKUP='+str(backup))
        print('FEE3_BE_R2_SAFE_INSTALL_PASS')
        return 0
    except Exception as error:
        for rel in reversed(written):
            dest=ROOT/rel
            snap=backups[rel]
            try:
                if snap is None:
                    dest.unlink(missing_ok=True)
                else:
                    atomic_put(dest,snap.read_bytes(),snap)
            except Exception as rollback_error:
                print('ROLLBACK_FAILURE '+rel+': '+str(rollback_error),file=sys.stderr)
        print('FEE3_BE_R2_INSTALL_ABORTED: '+str(error),file=sys.stderr)
        return 3


def main():
    parser=argparse.ArgumentParser(description='Fail-closed FEE3 Backend R2 installation')
    mode=parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--preview',action='store_true')
    mode.add_argument('--apply',action='store_true')
    args=parser.parse_args()
    try:
        manifest=json.loads(MANIFEST.read_text(encoding='utf-8'))
        if manifest.get('schema')!=1 or manifest.get('patchId')!='FEE3-BE-R2-20261026':
            raise ValueError('Unexpected patch manifest')
        states=inspect(manifest)
    except (ValueError, OSError, json.JSONDecodeError) as error:
        print('FEE3_BE_R2_PREFLIGHT_ERROR: '+str(error),file=sys.stderr)
        return 2
    counts={}
    for rel,(status,why) in states.items():
        counts[status]=counts.get(status,0)+1
        print(status+' | '+rel+(' | '+why if why else ''))
    print('FEE3_BE_R2_PREFLIGHT='+str(counts))
    if counts.get('CONFLICT',0):
        print('FEE3_BE_R2_CONFLICT: no project files changed. Reconcile with current source before installing.',file=sys.stderr)
        return 2
    if args.preview:
        print('FEE3_BE_R2_PREVIEW_PASS: no project files changed')
        return 0
    return apply(manifest,states)


if __name__=='__main__':
    raise SystemExit(main())
