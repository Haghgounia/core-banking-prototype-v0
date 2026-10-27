#!/usr/bin/env python3
"""SHA guarded, idempotent one-file QA transport hotfix; no application or database writes."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
import stat
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
MANIFEST = HERE / 'manifest.json'

def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def safe_path(name: str) -> Path:
    path = Path(name)
    if not name or path.is_absolute() or '..' in path.parts or path.as_posix() != name or '\\' in name:
        raise ValueError('UNSAFE_RELATIVE_PATH')
    node = ROOT
    for part in path.parts:
        node = node / part
        if node.is_symlink():
            raise ValueError('SYMLINK_PATH_FORBIDDEN: ' + name)
    return node

def checks(manifest: dict) -> tuple[dict[str, str], Path, Path]:
    statuses = {}
    for name, expected in manifest['anchors'].items():
        path = safe_path(name)
        statuses['ANCHOR ' + name] = 'MATCH' if path.is_file() and sha256(path) == expected else 'CONFLICT'
    dest = safe_path(manifest['target'])
    src = HERE / 'overlay' / manifest['target']
    if not src.is_file() or src.is_symlink() or sha256(src) != manifest['targetSha256']:
        statuses['PAYLOAD ' + manifest['target']] = 'CONFLICT'
    else:
        statuses['PAYLOAD ' + manifest['target']] = 'MATCH'
    if not dest.is_file():
        statuses[manifest['target']] = 'CONFLICT'
    else:
        current = sha256(dest)
        statuses[manifest['target']] = (
            'ALREADY_APPLIED' if current == manifest['targetSha256'] else
            'READY' if current == manifest['baselineSha256'] else 'CONFLICT'
        )
    return statuses, dest, src

def atomic_replace(dest: Path, content: bytes, expected_before: str) -> None:
    if not dest.is_file() or sha256(dest) != expected_before:
        raise ValueError('CONCURRENT_TARGET_CHANGE')
    mode = stat.S_IMODE(dest.stat().st_mode)
    fd, temporary = tempfile.mkstemp(prefix='.fee3-r4c3-hf1-', dir=str(dest.parent))
    try:
        with os.fdopen(fd, 'wb') as handle:
            handle.write(content)
            handle.flush()
            os.fsync(handle.fileno())
        os.chmod(temporary, mode)
        if sha256(Path(temporary)) != hashlib.sha256(content).hexdigest():
            raise ValueError('TEMPORARY_WRITE_INTEGRITY_FAILURE')
        if sha256(dest) != expected_before:
            raise ValueError('CONCURRENT_TARGET_CHANGE')
        os.replace(temporary, dest)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)

def main() -> int:
    parser = argparse.ArgumentParser()
    actions = parser.add_mutually_exclusive_group(required=True)
    actions.add_argument('--preview', action='store_true')
    actions.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    try:
        manifest = json.loads(MANIFEST.read_text(encoding='utf8'))
        if manifest.get('patchId') != 'FEE3-R4C3-HF1-DECIMAL-JSON-20261027' or manifest.get('schema') != 1 or \
           manifest.get('target') != 'tools/fee3-backend/r4c3/runtime-readonly.py':
            raise ValueError('MANIFEST_IDENTITY_INVALID')
        statuses, dest, src = checks(manifest)
        for name, result in statuses.items():
            print(result + ' | ' + name)
        if 'CONFLICT' in statuses.values():
            print('FEE3_R4C3_HF1_CONFLICT_ABORTED_NO_CHANGES', file=sys.stderr)
            return 2
        if args.preview:
            print('FEE3_R4C3_HF1_PREVIEW_PASS_NO_CHANGES')
            return 0
        modified = 0
        if statuses[manifest['target']] == 'READY':
            atomic_replace(dest, src.read_bytes(), manifest['baselineSha256'])
            modified = 1
        verified, _, _ = checks(manifest)
        if any(x not in ('MATCH','ALREADY_APPLIED') for x in verified.values()):
            raise ValueError('POST_APPLY_VERIFICATION_FAILED')
        print('FEE3_R4C3_HF1_APPLIED=' + str(modified))
        print('FEE3_R4C3_HF1_SAFE_INSTALL_PASS')
        return 0
    except (OSError, ValueError, KeyError, TypeError) as error:
        print('FEE3_R4C3_HF1_INSTALL_ERROR ' + str(error), file=sys.stderr)
        return 3

if __name__ == '__main__':
    sys.exit(main())
