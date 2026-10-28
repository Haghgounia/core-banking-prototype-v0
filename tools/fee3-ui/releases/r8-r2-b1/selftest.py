#!/usr/bin/env python3
"""Fixture-based negative source-integrity tests. No project modification."""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import shutil
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
GATE = HERE / 'payload/verify-r8-r2-integration.py'


def load_gate():
    spec = importlib.util.spec_from_file_location('fee3_integrated_verifier', GATE)
    if spec is None or spec.loader is None:
        raise RuntimeError('payload import failed')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def verdict(module, root: Path) -> bool:
    checks = module.verify(root)
    return bool(checks) and all(success for _, success, _ in checks)


def main() -> int:
    module = load_gate()
    manifest = json.loads((ROOT / module.R8_MANIFEST).read_text(encoding='utf-8'))
    r2 = json.loads((ROOT / module.R2_MANIFEST).read_text(encoding='utf-8'))
    paths = set(manifest['file_sha256']) | set(r2['files'])
    paths |= {module.R8_MANIFEST, module.R2_MANIFEST, 'VERSION'}
    with tempfile.TemporaryDirectory(prefix='fee3-r8r2-test-') as work:
        root = Path(work)
        for rel in paths:
            src = ROOT.joinpath(*rel.split('/'))
            dest = root.joinpath(*rel.split('/'))
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dest)
        overlay = ROOT / 'tools/fee3-backend/releases/r2/overlay'
        if overlay.exists():
            for rel in r2['files']:
                src = overlay.joinpath(*rel.split('/'))
                dest = root / 'tools/fee3-backend/releases/r2/overlay' / rel
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, dest)

        def test_case(label: str, fn, expected=True):
            fn()
            actual = verdict(module, root)
            ok = actual == expected
            print(('PASS' if ok else 'FAIL') + ' | ' + label)
            return ok

        count = 0
        passed = 0
        checks = [
            ('unaltered integrated R8+R2 succeeds', None, True),
            ('HTML mutation rejects', 'frontend/src/app/features/fee3/fee3-workspace.component.html', False),
            ('R8-only validation module mutation rejects', 'frontend/src/app/features/fee3/fee3-validation.ts', False),
            ('R2 Backend mutation rejects', 'backend/src/main/java/com/behsazan/corebanking/fee3/domain/Fee3Models.java', False),
            ('R8 manifest tampering rejects', module.R8_MANIFEST, False),
            ('R2 manifest tampering rejects', module.R2_MANIFEST, False),
            ('historical R8 verifier mutation rejects', 'tools/fee3-ui/verify-r8-source-compatibility.py', False),
        ]
        for label, rel, expected in checks:
            count += 1
            if rel is None:
                ok = test_case(label, lambda: None, expected)
            else:
                f = root / rel
                previous = f.read_bytes()
                try:
                    f.write_bytes(previous + b'\n# unexpected mutation\n')
                    ok = test_case(label, lambda: None, expected)
                finally:
                    f.write_bytes(previous)
            passed += int(ok)

        # With only one R8 version restored, the R2 mixed deployment must fail.
        target = 'frontend/src/app/features/fee3/fee3-workspace.component.html'
        f = root / target
        previous = f.read_bytes()
        backups = list(ROOT.glob('docs/install/fee3-r2-backup-*/' + target))
        originals = [p for p in backups if p.is_file()
                     and module.sha256(p) == r2['files'][target]['before']]
        if originals:
            try:
                f.write_bytes(originals[0].read_bytes())
                count += 1
                passed += int(test_case('mixed R8/R2 UI rejects', lambda: None, False))
            finally:
                f.write_bytes(previous)

        # Installed source can be checked when distribution overlay has been archived.
        overlay_copy = root / 'tools/fee3-backend/releases/r2/overlay'
        if overlay_copy.is_dir():
            renamed = root / 'tools/fee3-backend/releases/r2/overlay.archived'
            overlay_copy.rename(renamed)
            try:
                count += 1
                passed += int(test_case('archived overlay accepted with pinned installed sources', lambda: None, True))
            finally:
                renamed.rename(overlay_copy)

    print(f'FEE3_UI_R8_R2_B1_SELFTEST_PASS={passed} FAIL={count-passed}')
    return 0 if passed == count else 1


if __name__ == '__main__':
    raise SystemExit(main())
