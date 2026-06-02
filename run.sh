#!/usr/bin/env bash
# ============================================================
# run.sh — Launch the Panchayat Management System
# Run from the PanchayatSystem/ directory:
#   ./run.sh
#
# The SQLite database file (panchayat.db) will be created in
# the current directory on first launch.
# ============================================================

SQLITE_JAR=$(ls lib/sqlite-jdbc-*.jar 2>/dev/null | head -1)
if [ -z "$SQLITE_JAR" ]; then
  echo "ERROR: SQLite JDBC jar not found in lib/"
  exit 1
fi

if [ ! -d "out" ] || [ -z "$(ls out 2>/dev/null)" ]; then
  echo "Compiled classes not found. Running build first..."
  ./build.sh
fi

echo "Starting Panchayat Management System..."
java --enable-native-access=ALL-UNNAMED -cp "out:$SQLITE_JAR" panchayat.Main
