#!/usr/bin/env bash
# ============================================================
# build.sh — Compile the Panchayat Management System
# Run from the PanchayatSystem/ directory:
#   chmod +x build.sh && ./build.sh
# ============================================================

set -e  # exit on first error

SQLITE_JAR=$(ls lib/sqlite-jdbc-*.jar 2>/dev/null | head -1)
if [ -z "$SQLITE_JAR" ]; then
  echo "ERROR: SQLite JDBC jar not found in lib/"
  echo "Download from: https://github.com/xerial/sqlite-jdbc/releases"
  exit 1
fi

echo "Using JDBC driver: $SQLITE_JAR"
echo "Compiling sources..."

mkdir -p out

# Find all .java files under src/ and compile them together
find src -name "*.java" | xargs javac -cp "$SQLITE_JAR" -d out

echo ""
echo "✅  Build successful! Run with:  ./run.sh"
