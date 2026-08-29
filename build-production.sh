#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
APP_VERSION="$(cat "$ROOT/VERSION")"
echo "Building Core Banking Prototype $APP_VERSION..."

node "$ROOT/tools/sync-system-specification.mjs"
node "$ROOT/tools/verify-cif-persisted-grids.mjs"
node "$ROOT/tools/verify-ea-oracle-comparison.mjs"
node "$ROOT/tools/verify-calendar-reference.mjs"
node "$ROOT/tools/verify-calendar-dataset-import.mjs"
node "$ROOT/tools/verify-calendar2-reference.mjs"
node "$ROOT/tools/verify-calendar2-month-view.mjs"
node "$ROOT/tools/verify-calendar2-business-calendar-lookups.mjs"
node "$ROOT/tools/verify-persian-date-picker-current-default.mjs"
node "$ROOT/tools/verify-node-tool-path-portability.mjs"
node "$ROOT/tools/verify-cif-isic2.mjs"
node "$ROOT/tools/verify-pdl-product-builder.mjs"
node "$ROOT/tools/verify-calendar-display-labels.mjs"
node "$ROOT/tools/verify-runtime-artifact-contract.mjs"

cd "$ROOT/backend"
./mvnw -DskipTests compile
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
./mvnw clean package
mkdir -p "$ROOT/app"
JAR="$ROOT/app/core-banking-prototype.jar"
cp "$ROOT/backend/target/core-banking-prototype.jar" "$JAR"
printf '%s\n' "$APP_VERSION" > "$ROOT/app/BUILD-VERSION"
printf '\nBuilt version: %s\nJAR: %s\n' "$APP_VERSION" "$JAR"
