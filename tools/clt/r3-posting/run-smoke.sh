#!/bin/sh
set -eu
ROOT="${1:-.}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
B="$ROOT/backend/src/main/java/com/behsazan/corebanking/collateral"
SMOKE="$ROOT/tools/clt/r3-posting"
mkdir -p "$TMP"
javac --release 21 -encoding UTF-8 -d "$TMP" \
    "$B/workflow/CollateralWorkflowPolicy.java" \
    "$B/workflow/CollateralPostingReadinessPolicy.java" \
    "$SMOKE/R3PostingSmoke.java" "$ROOT/tools/clt/r3-review/JavaSyntaxCheck.java"
java -cp "$TMP" R3PostingSmoke
java -cp "$TMP" JavaSyntaxCheck \
    "$B/workflow/CollateralWorkflowService.java" \
    "$B/workflow/CollateralStageFingerprint.java" \
    "$B/workflow/CollateralWorkflowController.java" \
    "$B/workflow/CollateralPostingReadinessPolicy.java" \
    "$B/workflow/CollateralPostingReadinessService.java" \
    "$ROOT/backend/src/test/java/com/behsazan/corebanking/collateral/workflow/CollateralPostingReadinessPolicyTest.java"
node "$SMOKE/verify-source.cjs" "$ROOT"
