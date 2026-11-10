#!/bin/sh
set -eu
ROOT="${1:-.}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
B="$ROOT/backend/src/main/java/com/behsazan/corebanking/collateral"
SMOKE="$ROOT/tools/clt/r3-posting"
mkdir -p "$TMP"
javac --release 21 -encoding UTF-8 -d "$TMP" \
    "$B/workflow/CollateralR3WorkflowPolicy.java" \
    "$B/workflow/CollateralR3PostingReadinessPolicy.java" \
    "$SMOKE/R3PostingSmoke.java" "$ROOT/tools/clt/r3-review/JavaSyntaxCheck.java"
java -cp "$TMP" R3PostingSmoke
java -cp "$TMP" JavaSyntaxCheck \
    "$B/workflow/CollateralR3WorkflowService.java" \
    "$B/workflow/CollateralR3StageFingerprint.java" \
    "$B/workflow/CollateralR3WorkflowController.java" \
    "$B/workflow/CollateralR3PostingReadinessPolicy.java" \
    "$B/workflow/CollateralR3PostingReadinessService.java" \
    "$ROOT/backend/src/test/java/com/behsazan/corebanking/collateral/workflow/CollateralR3PostingReadinessPolicyTest.java"
node "$SMOKE/verify-source.cjs" "$ROOT"
