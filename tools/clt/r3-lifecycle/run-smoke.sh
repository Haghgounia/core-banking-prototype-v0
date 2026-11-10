#!/bin/sh
set -eu
ROOT="${1:-.}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
BASE="$ROOT/backend/src/main/java/com/behsazan/corebanking/collateral/lifecycle"
javac --release 21 -encoding UTF-8 -d "$TMP" "$BASE/CollateralLifecycleEvidencePolicy.java" "$ROOT/tools/clt/r3-lifecycle/R3LifecycleSmoke.java"
java -cp "$TMP" R3LifecycleSmoke
node "$ROOT/tools/clt/r3-lifecycle/verify-source.cjs" "$ROOT"
