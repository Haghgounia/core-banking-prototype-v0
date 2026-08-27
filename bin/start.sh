#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_VERSION="$(cat "$ROOT/VERSION")"
JAR="$ROOT/app/core-banking-prototype-$APP_VERSION.jar"
if [ ! -f "$JAR" ]; then
  printf 'ERROR: executable JAR for version %s was not found.\n' "$APP_VERSION" >&2
  printf 'Expected: %s\n' "$JAR" >&2
  printf 'Run ./build-production.sh before starting this source package.\n' >&2
  exit 1
fi
mkdir -p "$ROOT/logs"
printf 'Starting Core Banking Prototype %s...\n' "$APP_VERSION"
printf 'JAR: %s\n' "$JAR"
printf 'Log file: %s\n' "$ROOT/logs/core-banking-prototype.log"
exec java -jar "$JAR" --spring.config.additional-location="optional:file:$ROOT/config/application.yml"
