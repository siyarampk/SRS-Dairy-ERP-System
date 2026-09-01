#!/usr/bin/env bash
# Builds the runnable Dairy ERP jar into dist/ (with a bundled copy of lib/).
# Usage: ./build-jar.sh
set -euo pipefail
cd "$(dirname "$0")"

# 1. Compile sources into ./out
./compile.sh

# 2. Stage dist/ with a self-contained copy of the runtime jars
mkdir -p dist/lib
cp lib/*.jar dist/lib/

# 3. Create manifest (Class-Path entries are resolved relative to the jar's
#    location, i.e. dist/lib/)
printf 'Main-Class: dairy.erp.Main\nClass-Path: lib/sqlite-jdbc-3.45.3.0.jar lib/slf4j-api-1.7.36.jar lib/slf4j-nop-1.7.36.jar\n' > dist/manifest.txt

# 4. Package the jar
jar cfm dist/DairyERP.jar dist/manifest.txt -C out .
rm dist/manifest.txt

echo "Built dist/DairyERP.jar"
echo "Run from the project root:  java -jar dist/DairyERP.jar"
