#!/usr/bin/env python3
"""Immutable-source FEE3 UI R8-HF1 + BE-R2 integration integrity gate.

Read-only and strictly deterministic. This is NOT the historical R8-only gate.
It does NOT assert Angular AOT, browser UAT, Oracle safety, or bank approval.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath

R8_MANIFEST = 'release/FEE3-UI-R8-MANIFEST.json'
R2_MANIFEST = 'tools/fee3-backend/releases/r2/manifest.json'
R8_EXPECTED_SHA256 = '0dbbc553f3e6cf44ab2a8500c7b8798b577e487c47d43692481b5a6cd75d4d8a'
R2_EXPECTED_SHA256 = 'e8fd3ebfc664b83904b973d1a8e3cda50a44e607ce201bbc476ebe73b26194ca'
CHANGED_UI = frozenset('frontend/src/app/features/fee3/fee3-workspace.component.' + ext
                       for ext in ('html', 'ts', 'scss'))
EXPECTED_R8_ENTRIES = 12
EXPECTED_R2_ENTRIES = 19


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def safe_file(root: Path, relative: str) -> Path:
    rel = PurePosixPath(relative)
    if (not relative or '\\' in relative or ':' in relative or rel.is_absolute()
            or '..' in rel.parts or '.' in rel.parts):
        raise ValueError('unsafe manifest path')
    return root.joinpath(*rel.parts)


def verify(root: Path) -> list[tuple[str, bool, str]]:
    """Return (test name, pass bool, safe explanation). Never modify files."""
    root = Path(root).resolve()
    checks: list[tuple[str, bool, str]] = []

    def add(name: str, valid: bool, detail: str = '') -> None:
        checks.append((name, bool(valid), detail))

    version_file = root / 'VERSION'
    version = version_file.read_text(encoding='utf-8').strip() if version_file.is_file() else ''
    add('PROJECT_VERSION_0_11_0', version == '0.11.0', version or '(missing)')

    # Pin the manifests themselves; parsing an attacker-modified manifest is not proof.
    r8file, r2file = root / R8_MANIFEST, root / R2_MANIFEST
    r8_hash = sha256(r8file) if r8file.is_file() else '(missing)'
    r2_hash = sha256(r2file) if r2file.is_file() else '(missing)'
    add('HISTORICAL_R8_MANIFEST_UNMODIFIED', r8_hash == R8_EXPECTED_SHA256)
    add('R2_INSTALL_MANIFEST_UNMODIFIED', r2_hash == R2_EXPECTED_SHA256)
    if r8_hash != R8_EXPECTED_SHA256 or r2_hash != R2_EXPECTED_SHA256:
        return checks

    try:
        r8 = json.loads(r8file.read_text(encoding='utf-8'))
        r2 = json.loads(r2file.read_text(encoding='utf-8'))
    except (ValueError, UnicodeError):
        add('MANIFEST_JSON_VALID', False)
        return checks

    r8_files = r8.get('file_sha256', {})
    r2_files = r2.get('files', {})
    valid_contract = (r8.get('release') == 'FEE3-UI-R8-HF1'
                      and r2.get('patchId') == 'FEE3-BE-R2-20261026'
                      and isinstance(r8_files, dict) and len(r8_files) == EXPECTED_R8_ENTRIES
                      and isinstance(r2_files, dict) and len(r2_files) == EXPECTED_R2_ENTRIES
                      and CHANGED_UI.issubset(r8_files.keys())
                      and CHANGED_UI.issubset(r2_files.keys()))
    add('TWO_APPROVED_MANIFEST_SCOPES', valid_contract)
    if not valid_contract:
        return checks

    # The R2 installer must attest that its BEFORE files are precisely the R8
    # release files, and its AFTER hashes must correspond to the installed files.
    transition = all(r2_files[name].get('before') == r8_files[name]
                     for name in CHANGED_UI)
    add('R2_UI_BEFORE_EQUALS_R8_BASELINE_3', transition)

    r8_unchanged = 0
    for rel, expected in sorted(r8_files.items()):
        if rel in CHANGED_UI:
            continue
        path = safe_file(root, rel)
        actual = sha256(path) if path.is_file() else '(missing)'
        match = actual == expected
        add('R8_UNCHANGED_' + rel, match)
        r8_unchanged += int(match)
    add('R8_UNCHANGED_COUNT_9', r8_unchanged == 9)

    r2_count = 0
    r2_ui_count = 0
    for rel, metadata in sorted(r2_files.items()):
        path = safe_file(root, rel)
        after = metadata.get('after')
        actual = sha256(path) if path.is_file() else '(missing)'
        match = actual == after
        add('R2_AFTER_' + rel, match)
        r2_count += int(match)
        if rel in CHANGED_UI:
            r2_ui_count += int(match)
    add('R2_INSTALLED_FULL_BASELINE_19', r2_count == EXPECTED_R2_ENTRIES)
    add('R2_UI_INTEGRATION_FULL_3', r2_ui_count == 3)

    # Verify payload in the original release bundle, if retained in the project:
    # the SHA256 of every overlay must agree with the immutable R2 manifest.
    overlay = root / 'tools/fee3-backend/releases/r2/overlay'
    if overlay.is_dir():
        overlay_ok = 0
        for rel, metadata in sorted(r2_files.items()):
            f = safe_file(overlay, rel)
            match = f.is_file() and sha256(f) == metadata.get('after')
            add('R2_DISTRIBUTION_' + rel, match)
            overlay_ok += int(match)
        add('R2_DISTRIBUTION_19', overlay_ok == EXPECTED_R2_ENTRIES)
    else:
        # Runtime source integrity can be checked even if the distribution
        # overlay has been archived; manifest is pinned and all installed
        # 19 outputs still must agree byte-for-byte.
        add('R2_DISTRIBUTION_ARCHIVED', True, 'overlay unavailable; installed outputs verified')
    return checks


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[2],
                        help='Project root. Default: inferred from installed script path')
    args = parser.parse_args()
    try:
        checks = verify(args.root)
    except (OSError, ValueError, TypeError, KeyError) as exc:
        print('FEE3_R8_R2_INTEGRATION_FAIL_CLOSED=' + type(exc).__name__)
        return 2
    fails = [x for x in checks if not x[1]]
    for name, passed, detail in checks:
        if not passed:
            print('FAIL | ' + name + ((' | ' + detail) if detail else ''))
    if fails:
        print('FEE3_R8_R2_INTEGRATION_FAIL=' + str(len(fails)))
        return 1
    print('PASS | historical R8 manifest and verifier preserved (9/9 unchanged)')
    print('PASS | R2 installer before/after lineage proven (3/3 UI integration)')
    print('PASS | R2 full installed-source baseline verified (19/19)')
    print('FEE3_R8_R2_INTEGRATION_PASS=12 R2_FILES=19 FAIL=0')
    print('SCOPE=Read-only source integrity; NOT Angular AOT, browser UAT, Oracle or financial authorization')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
