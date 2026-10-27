#!/usr/bin/env python3
"""Standalone SHA256 check of QA3-only added files; no mutations."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
manifest_path = ROOT / 'release/FEE3-R3-QA3-MANIFEST.json'
manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
assert manifest['patchId'] == 'FEE3-R3-QA3-20261027'
assert manifest['applicationSourceChanges'] is False
assert manifest['databaseChanges'] is False
assert manifest['replaceExistingFiles'] is False
n = 0
for rel, want in manifest['files'].items():
    assert rel.startswith(('tools/fee3-backend/qualification/r3-qa3/', 'docs/install/FEE3-R3-QA3-')), rel
    actual = ROOT / rel
    if not actual.is_file():
        raise SystemExit('FAIL missing: ' + rel)
    have = hashlib.sha256(actual.read_bytes()).hexdigest()
    if have != want:
        raise SystemExit('FAIL SHA256 mismatch: ' + rel)
    print('PASS | ' + rel)
    n += 1
print('FEE3_R3_QA3_PACKAGE_PASS=' + str(n) + ' FAIL=0')
