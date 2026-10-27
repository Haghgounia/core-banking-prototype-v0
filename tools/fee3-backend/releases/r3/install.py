"""Safe and idempotent BE-R3 additive installer. No existing application sources overwritten."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
MANIFEST = HERE / 'manifest.json'
OVERLAY = HERE / 'overlay'

def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def path_ok(relative: str) -> bool:
    p = Path(relative)
    return not p.is_absolute() and '..' not in p.parts and relative == p.as_posix()

def safe_target(relative: str) -> Path:
    if not path_ok(relative):
        raise ValueError('Unsafe path in manifest: ' + relative)
    target = ROOT / relative
    check = ROOT
    for part in Path(relative).parts:
        check = check / part
        if check.is_symlink():
            raise ValueError('Symlink target/parent is not allowed: ' + relative)
    return target

def inspect(manifest):
    statuses = {}
    for relative, expected in manifest['anchors'].items():
        target = safe_target(relative)
        if not target.is_file():
            statuses['anchor:' + relative] = ('CONFLICT', 'Missing R2 prerequisite')
        elif sha(target.read_bytes()) != expected:
            statuses['anchor:' + relative] = ('CONFLICT', 'Modified R2 prerequisite')
        else:
            statuses['anchor:' + relative] = ('MATCH', '')
    for relative, expected in manifest['files'].items():
        src = OVERLAY / relative
        if not src.is_file() or src.is_symlink() or sha(src.read_bytes()) != expected:
            statuses[relative] = ('CONFLICT', 'Payload checksum invalid')
            continue
        target = safe_target(relative)
        if not target.exists():
            statuses[relative] = ('READY', '')
        elif target.is_file() and sha(target.read_bytes()) == expected:
            statuses[relative] = ('ALREADY_APPLIED', '')
        else:
            statuses[relative] = ('CONFLICT', 'Target not from R3')
    return statuses

def put_atomic(destination: Path, content: bytes):
    destination.parent.mkdir(parents=True, exist_ok=True)
    fd, temp = tempfile.mkstemp(prefix='.fee3-r3-', dir=str(destination.parent))
    try:
        with os.fdopen(fd, 'wb') as output:
            output.write(content)
            output.flush()
            os.fsync(output.fileno())
        # os.replace is only used after a complete preflight and only for absent files.
        if destination.exists():
            raise ValueError('Target appeared while installing: ' + str(destination))
        os.replace(temp, destination)
    finally:
        if os.path.exists(temp):
            os.unlink(temp)

def main():
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--preview', action='store_true')
    mode.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    try:
        manifest = json.loads(MANIFEST.read_text(encoding='utf-8'))
        if manifest.get('schema') != 1 or manifest.get('patchId') != 'FEE3-BE-R3-20261026':
            raise ValueError('Unexpected manifest')
        statuses = inspect(manifest)
        for name, (status, note) in statuses.items():
            print(status, name, note)
        if any(status == 'CONFLICT' for status, _ in statuses.values()):
            print('FEE3_BE_R3_CONFLICT_ABORTED: nothing changed', file=sys.stderr)
            return 2
        if args.preview:
            print('FEE3_BE_R3_PREVIEW_PASS; no files changed')
            return 0
        installed = []
        try:
            for relative in manifest['files']:
                if statuses[relative][0] != 'READY':
                    continue
                target = safe_target(relative)
                put_atomic(target, (OVERLAY / relative).read_bytes())
                installed.append(target)
            after = inspect(manifest)
            if any(status not in ('MATCH', 'ALREADY_APPLIED') for status, _ in after.values()):
                raise ValueError('Post-install verification failed')
        except Exception:
            for target in reversed(installed):
                target.unlink(missing_ok=True)
            raise
        print('FEE3_BE_R3_APPLIED=' + str(len(installed)))
        print('FEE3_BE_R3_SAFE_INSTALL_PASS')
        return 0
    except (ValueError, OSError, json.JSONDecodeError) as error:
        print('FEE3_BE_R3_INSTALL_FAILED:', error, file=sys.stderr)
        return 3

if __name__ == '__main__':
    sys.exit(main())
