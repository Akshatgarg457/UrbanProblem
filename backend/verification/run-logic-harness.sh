#!/usr/bin/env bash
# Runs real project logic (ComplaintStatus, ComplaintCategory, AccessRules, RecurrenceService)
# against minimal stand-ins for Spring/JPA types. Needs only JDK 21. It does NOT test Spring wiring,
# JPA queries, JWT, or HTTP. Those require `./mvnw verify`.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
SRC="$ROOT/src/main/java/com/urbanproblems"
mkdir -p "$WORK/src/com/urbanproblems/domain/enums" "$WORK/src/com/urbanproblems/exception" \
         "$WORK/src/com/urbanproblems/security" "$WORK/src/com/urbanproblems/service" "$WORK/out"
cp "$SRC/domain/enums/ComplaintCategory.java" "$SRC/domain/enums/ComplaintStatus.java" "$WORK/src/com/urbanproblems/domain/enums/"
cp "$SRC/exception/ResourceNotFoundException.java" "$WORK/src/com/urbanproblems/exception/"
cp "$SRC/security/AccessRules.java" "$WORK/src/com/urbanproblems/security/"
cp "$SRC/service/RecurrenceService.java" "$WORK/src/com/urbanproblems/service/"
cp "$HERE/harness/HarnessMain.java" "$WORK/src/"
find "$HERE/stubs" "$WORK/src" -name "*.java" > "$WORK/files.txt"
if command -v javac >/dev/null 2>&1; then
  javac -d "$WORK/out" @"$WORK/files.txt"
else
  java -m jdk.compiler/com.sun.tools.javac.Main -d "$WORK/out" @"$WORK/files.txt"
fi
java -cp "$WORK/out" HarnessMain
