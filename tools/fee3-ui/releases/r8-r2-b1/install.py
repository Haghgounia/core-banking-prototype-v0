#!/usr/bin/env python3
"""Install exactly one additive read-only FEE3 R8+R2 verification script.

--preview: no mutation; --apply: add one new tool script after all anchors pass.
Existing R8 historical verifier, manifests, UI, Backend and build files are untouched.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
MANIFEST = HERE / 'manifest.json'
PAYLOAD = HERE / 'payload/verify-r8-r2-integration.py'
DESTINATION = ROOT / 'tools/fee3-ui/verify-r8-r2-integration.py'


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    cli = argparse.ArgumentParser(description=__doc__)
    operation = cli.add_mutually_exclusive_group(required=True)
    operation.add_argument('--preview', action='store_true')
    operation.add_argument('--apply', action='store_true')
    args = cli.parse_args()
    try:
        metadata = json.loads(MANIFEST.read_text(encoding='utf-8'))
        if metadata.get('patchId') != 'FEE3-UI-R8-R2-B1-20261027':
            raise ValueError('unexpected patchId')
        if (ROOT / 'VERSION').read_text(encoding='utf-8').strip() != '0.11.0':
            raise ValueError('unsupported project version')
        for name, expected in metadata['anchors'].items():
            file = ROOT.joinpath(*name.split('/'))
            if not file.is_file() or file.is_symlink() or sha256(file) != expected:
                raise ValueError('anchor mismatch: ' + name)
            print('MATCH | ANCHOR ' + name)
        if not PAYLOAD.is_file() or PAYLOAD.is_symlink():
            raise ValueError('payload not found')
        data = PAYLOAD.read_bytes()
        if hashlib.sha256(data).hexdigest() != metadata['payloadSha256']:
            raise ValueError('payload hash mismatch')
        print('MATCH | PAYLOAD tools/fee3-ui/verify-r8-r2-integration.py')
        # The source gate must pass BEFORE anything is installed.
        spec = importlib.util.spec_from_file_location('fee3_r8r2_gate', PAYLOAD)
        if spec is None or spec.loader is None:
            raise ValueError('cannot load verifier payload')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        results = module.verify(ROOT)
        failed = [name for name, passed, _ in results if not passed]
        if failed:
            raise ValueError('integrated source verification failed: ' + ', '.join(failed))
        print('PASS | R8+R2 integrated sources are byte-exact')
        if DESTINATION.is_symlink() or DESTINATION.parent.is_symlink():
            raise ValueError('symlink target rejected')
        if DESTINATION.exists():
            if not DESTINATION.is_file() or sha256(DESTINATION) != metadata['payloadSha256']:
                raise ValueError('existing integrated verifier differs from patch')
            print('ALREADY_APPLIED | tools/fee3-ui/verify-r8-r2-integration.py')
            print('FEE3_UI_R8_R2_B1_' + ('PREVIEW' if args.preview else 'APPLY') + '_PASS_NO_CHANGES')
            return 0
        if not DESTINATION.parent.is_dir():
            raise ValueError('tools/fee3-ui destination missing')
        print('READY | tools/fee3-ui/verify-r8-r2-integration.py')
        if args.preview:
            print('FEE3_UI_R8_R2_B1_PREVIEW_PASS_NO_CHANGES')
            return 0
        fd, tempname = tempfile.mkstemp(prefix='.fee3-r8r2-', dir=str(DESTINATION.parent))
        try:
            with os.fdopen(fd, 'wb') as handle:
                handle.write(data)
                handle.flush()
                os.fsync(handle.fileno())
            # No pre-existing target is overwritten. No backup is necessary.
            if DESTINATION.exists() or DESTINATION.is_symlink():
                raise ValueError('target created concurrently; refusing overwrite')
            os.replace(tempname, DESTINATION)
            if sha256(DESTINATION) != metadata['payloadSha256']:
                DESTINATION.unlink(missing_ok=True)
                raise ValueError('installed verifier checksum mismatch')
            print('FEE3_UI_R8_R2_B1_APPLIED=1')
            print('FEE3_UI_R8_R2_B1_SAFE_INSTALL_PASS')
            return 0
        finally:
            if os.path.exists(tempname):
                os.unlink(tempname)
    except (OSError, ValueError, TypeError, KeyError, json.JSONDecodeError) as exc:
        print('FEE3_UI_R8_R2_B1_ABORTED_FAIL_CLOSED | ' + str(exc))
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
