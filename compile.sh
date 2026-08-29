#!/usr/bin/env bash
# Compiles the Dairy ERP sources into ./out without Maven/Gradle.
# Usage: ./compile.sh
set -euo pipefail
cd "$(dirname "$0")"

JAR=$(ls lib/*.jar 2>/dev/null | head -1)
if [[ -z "$JAR" ]]; then
  echo "ERROR: no JDBC jars found in lib/." >&2
  exit 1
fi

CP=$(ls lib/*.jar 2>/dev/null | tr '\n' ':')
CP=${CP%:}

rm -rf out
mkdir -p out

SRC=$(find src -name '*.java')
if [[ -z "$SRC" ]]; then
  echo "No .java sources found under src/." >&2
  exit 1
fi

javac -encoding UTF-8 -cp "$CP" -d out $SRC
echo "Compiled OK -> out/  (classpath: $CP)"