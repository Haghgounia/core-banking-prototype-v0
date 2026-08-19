#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/app/core-banking-prototype.jar"
if [ ! -f "$JAR" ]; then
  JAR="$ROOT/backend/target/core-banking-prototype.jar"
fi
if [ ! -f "$JAR" ]; then
  printf 'ERROR: core-banking-prototype.jar was not found. Run build-production.sh first.\n' >&2
  exit 1
fi
mkdir -p "$ROOT/logs"
printf 'Log file: %s\n' "$ROOT/logs/core-banking-prototype.log"
exec java -jar "$JAR" --spring.config.additional-location="optional:file:$ROOT/config/application.yml"
