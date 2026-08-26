#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
APP_VERSION="$(cat "$ROOT/VERSION")"
echo "Building Core Banking Prototype $APP_VERSION..."

node "$ROOT/tools/sync-system-specification.mjs"
node "$ROOT/tools/verify-cif-persisted-grids.mjs"
node "$ROOT/tools/verify-ea-oracle-comparison.mjs"
node "$ROOT/tools/verify-calendar-reference.mjs"
node "$ROOT/tools/verify-calendar-dataset-import.mjs"
node "$ROOT/tools/verify-calendar2-reference.mjs"

rm -f "$ROOT/app/core-banking-prototype.jar" "$ROOT/backend/target/core-banking-prototype.jar"
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
cp "$ROOT/backend/target/core-banking-prototype.jar" "$ROOT/app/core-banking-prototype.jar"
printf '\nBuilt version: %s\nJAR: %s\n' "$APP_VERSION" "$ROOT/app/core-banking-prototype.jar"
