#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
test_output="$(mktemp -d)"
trap 'rm -rf "$test_output"' EXIT
if command -v javac >/dev/null 2>&1; then
  test_compiler=(javac)
else
  test_compiler=(java --module jdk.compiler/com.sun.tools.javac.Main)
fi
"${test_compiler[@]}" -d "$test_output" \
  "$project_dir/app/src/main/java/com/sadique/dailyledger/sync/BackupPolicy.java" \
  "$project_dir/app/src/main/java/com/sadique/dailyledger/sync/SnapshotCodec.java" \
  "$project_dir/tests/BackupCoreTest.java"
java -cp "$test_output" BackupCoreTest
