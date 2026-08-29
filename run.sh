#!/usr/bin/env bash
# Runs the Dairy ERP application. Pass optional args, e.g. --init.
# Usage: ./run.sh [args...]
set -euo pipefail
cd "$(dirname "$0")"

JAR=$(ls lib/*.jar 2>/dev/null | head -1)
CP=$(ls lib/*.jar 2>/dev/null | tr '\n' ':')
CP=${CP%:}
if [[ -z "$CP" ]]; then
  echo "ERROR: no jars found in lib/." >&2
  exit 1
fi
if [[ ! -d out ]]; then
  echo "out/ not found. Run ./compile.sh first." >&2
  exit 1
fi

exec java -cp "out:$CP" dairy.erp.Main "$@"