#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
APP_VERSION="$(cat "$ROOT/VERSION")"
echo "Building Core Banking Prototype $APP_VERSION..."

node "$ROOT/tools/sync-system-specification.mjs"
node "$ROOT/tools/migrate-release-layout.mjs"
node "$ROOT/tools/migrate-root-layout.mjs"
node "$ROOT/tools/migrate-source-layout.mjs"
node "$ROOT/tools/verify-release-layout.mjs"

for required in \
  "$ROOT/backend/src/main/java/com/behsazan/corebanking/deposit/account/domain/DepositAccountModels.java" \
  "$ROOT/backend/src/main/java/com/behsazan/corebanking/deposit/account/error/DepositAccountLifecycleException.java" \
  "$ROOT/backend/src/main/java/com/behsazan/corebanking/deposit/account/oracle/DepositAccountRepository.java"; do
  if [ ! -f "$required" ]; then
    echo "ERROR: DPS2 Phase 4 account lifecycle source is incomplete: $required" >&2
    echo "Apply the cumulative 0.5.1 patch or use the full package." >&2
    exit 1
  fi
done
node "$ROOT/tools/verify-cif-persisted-grids.mjs"
node "$ROOT/tools/verify-cif-religion-reference.mjs"
node "$ROOT/tools/verify-ea-oracle-comparison.mjs"
node "$ROOT/tools/verify-oracle-ea-xmi-export.mjs"
node "$ROOT/tools/verify-calendar-reference.mjs"
node "$ROOT/tools/verify-calendar-dataset-import.mjs"
node "$ROOT/tools/verify-calendar-month-view.mjs"
node "$ROOT/tools/verify-calendar-form-usability.mjs"
node "$ROOT/tools/verify-calendar-current-year-default.mjs"
node "$ROOT/tools/verify-calendar2-reference.mjs"
node "$ROOT/tools/verify-calendar2-month-view.mjs"
node "$ROOT/tools/verify-calendar2-business-calendar-lookups.mjs"
node "$ROOT/tools/verify-calendar2-business-calendar-day-grid.mjs"
node "$ROOT/tools/verify-calendar2-business-calendar-schedule.mjs"
node "$ROOT/tools/verify-calendar2-day-resolution-policy.mjs"
node "$ROOT/tools/verify-persian-date-picker-current-default.mjs"
node "$ROOT/tools/verify-time-picker.mjs"
node "$ROOT/tools/verify-node-tool-path-portability.mjs"
node "$ROOT/tools/verify-cif-isic2.mjs"
node "$ROOT/tools/verify-pdl-product-builder.mjs"
node "$ROOT/tools/verify-dps2-four-deposits.mjs"
node "$ROOT/tools/verify-dps2-deposit-opening-persistence.mjs"
node "$ROOT/tools/verify-dps2-deposit-opening-phase3.mjs"
node "$ROOT/tools/verify-dps2-deposit-account-phase4.mjs"
node "$ROOT/tools/verify-dps2-deposit-opening-phase5.mjs"
node "$ROOT/tools/verify-dps2-build-hotfix-051.mjs"
node "$ROOT/tools/verify-dps2-build-hotfix-052.mjs"
node "$ROOT/tools/verify-dps2-deposit-opening-phase6.mjs"
node "$ROOT/tools/verify-dps2-deposit-opening-phase7.mjs"
node "$ROOT/tools/verify-dps2-deposit-account-operations-phase8.mjs"
node "$ROOT/tools/verify-dps2-deposit-account-servicing-phase9.mjs"
node "$ROOT/tools/verify-dps2-account-schema-reconciliation-092.mjs"
node "$ROOT/tools/verify-dps2-reference-fk-reconciliation-093.mjs"
node "$ROOT/tools/verify-dps2-opening-operational-v5-phase10.mjs"
node "$ROOT/tools/verify-dps2-opening-operational-v5-phase10b.mjs"
node "$ROOT/tools/verify-dps2-opening-operational-v5-phase10d.mjs"
node "$ROOT/tools/verify-dps2-opening-v5-phase10e10f.mjs"
node "$ROOT/tools/verify-dps2-account-operations-schema-reconciliation-11a.mjs"
node "$ROOT/tools/verify-dps2-account-servicing-core-11b.mjs"
node "$ROOT/tools/verify-dps2-lifecycle-hold-11c.mjs"
node "$ROOT/tools/verify-cif-address-hotfix-071.mjs"
node "$ROOT/tools/verify-fee-admin-baseline.mjs"
node "$ROOT/tools/verify-cbi-fee-1404-import.mjs"
node "$ROOT/tools/verify-cbi-rial-fee-1405-provisional.mjs"
node "$ROOT/tools/verify-geo-name-romanization.mjs"
node "$ROOT/tools/verify-global-breadcrumb.mjs"
node "$ROOT/tools/verify-edu-reference-ui.mjs"
node "$ROOT/tools/verify-calendar-display-labels.mjs"
node "$ROOT/tools/verify-runtime-artifact-contract.mjs"

cd "$ROOT/backend"
sh ./mvnw -DskipTests compile
cd "$ROOT"

rm -f "$ROOT/app/"*.jar "$ROOT/app/BUILD-VERSION" "$ROOT/backend/target/core-banking-prototype.jar"
rm -rf "$ROOT/frontend/dist"

cd "$ROOT/frontend"
npm install
npm run build

rm -rf "$ROOT/backend/src/main/resources/static"
mkdir -p "$ROOT/backend/src/main/resources/static"
if [ -d "$ROOT/frontend/dist/core-banking-ui/browser" ]; then
  cp -R "$ROOT/frontend/dist/core-banking-ui/browser/." "$ROOT/backend/src/main/resources/static/"
else
  cp -R "$ROOT/frontend/dist/core-banking-ui/." "$ROOT/backend/src/main/resources/static/"
fi

cd "$ROOT/backend"
sh ./mvnw clean package
mkdir -p "$ROOT/app"
JAR="$ROOT/app/core-banking-prototype.jar"
cp "$ROOT/backend/target/core-banking-prototype.jar" "$JAR"
printf '%s\n' "$APP_VERSION" > "$ROOT/app/BUILD-VERSION"
printf '\nBuilt version: %s\nJAR: %s\n' "$APP_VERSION" "$JAR"
