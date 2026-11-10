#!/bin/sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
SOURCE="$ROOT/backend/src/main/java/com/behsazan/corebanking/collateral"
SMOKE="$ROOT/tools/clt/r3-review"
javac --release 21 -encoding UTF-8 -d "$TMP" \
    "$SOURCE/workflow/CollateralR3WorkflowPolicy.java" \
    "$SOURCE/sponsor/CollateralSponsorPolicy.java" \
    "$SOURCE/insurance/CollateralInsuranceModels.java" \
    "$SOURCE/insurance/CollateralInsurancePolicy.java" \
    "$SOURCE/valuation/CollateralValuationModels.java" \
    "$SOURCE/valuation/CollateralValuationPolicy.java" \
    "$SMOKE/R3ReviewSmoke.java" \
    "$SMOKE/JavaSyntaxCheck.java"
java -cp "$TMP" R3ReviewSmoke
java -cp "$TMP" JavaSyntaxCheck \
    "$SOURCE/workflow/CollateralR3WorkflowService.java" \
    "$SOURCE/workflow/CollateralR3WorkflowPolicy.java" \
    "$SOURCE/sponsor/CollateralSponsorService.java" \
    "$SOURCE/insurance/CollateralInsuranceService.java" \
    "$SOURCE/valuation/CollateralValuationService.java" \
    "$ROOT/backend/src/test/java/com/behsazan/corebanking/collateral/workflow/CollateralR3WorkflowPolicyTest.java"
