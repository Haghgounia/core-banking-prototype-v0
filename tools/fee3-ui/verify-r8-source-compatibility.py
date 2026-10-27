#!/usr/bin/env python3
"""Read-only check that R8 did not alter non-FEE3 source paths.
Integrity data is distributed in release/FEE3-UI-R8-MANIFEST.json.
"""
from pathlib import Path
import json,hashlib
root=Path(__file__).resolve().parents[2]
manifest=json.loads((root/'release/FEE3-UI-R8-MANIFEST.json').read_text(encoding='utf-8'))
for path,expect in manifest['file_sha256'].items():
    actual=hashlib.sha256((root/path).read_bytes()).hexdigest()
    if actual!=expect:raise SystemExit('FEE3_R8_INTEGRITY_FAIL '+path)
print(f"FEE3_R8_INTEGRITY_PASS={len(manifest['file_sha256'])}")
