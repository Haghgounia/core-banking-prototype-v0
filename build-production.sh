#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
node "$ROOT/tools/verify-calendar-reference.mjs"
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
printf '\nBuilt: %s\n' "$ROOT/app/core-banking-prototype.jar"
