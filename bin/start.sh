#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_VERSION="$(cat "$ROOT/VERSION")"
JAR="$ROOT/app/core-banking-prototype.jar"
BUILD_VERSION_FILE="$ROOT/app/BUILD-VERSION"
if [ ! -f "$JAR" ]; then
  printf 'ERROR: executable JAR was not found.\nExpected: %s\n' "$JAR" >&2
  printf 'Run ./build-production.sh before starting this source package.\n' >&2
  exit 1
fi
if [ ! -f "$BUILD_VERSION_FILE" ]; then
  printf 'ERROR: build version marker was not found.\nExpected: %s\n' "$BUILD_VERSION_FILE" >&2
  printf 'Run ./build-production.sh before starting this source package.\n' >&2
  exit 1
fi
BUILT_VERSION="$(cat "$BUILD_VERSION_FILE")"
if [ "$BUILT_VERSION" != "$APP_VERSION" ]; then
  printf 'ERROR: runtime JAR is stale or belongs to another source version.\n' >&2
  printf 'Source version: %s\nBuilt version: %s\n' "$APP_VERSION" "$BUILT_VERSION" >&2
  printf 'Run ./build-production.sh before starting this source package.\n' >&2
  exit 1
fi
mkdir -p "$ROOT/logs"
printf 'Starting Core Banking Prototype %s...\n' "$APP_VERSION"
printf 'JAR: %s\n' "$JAR"
printf 'Log file: %s\n' "$ROOT/logs/core-banking-prototype.log"
exec java -jar "$JAR" --spring.config.additional-location="optional:file:$ROOT/config/application.yml"
