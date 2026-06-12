#!/usr/bin/env bash
# Audit01 #07 — lint-baseline cap gate (Phase 1).
#
# Purpose:
#   Prevent silent growth of the root lint-baseline.xml. The current entry
#   count is recorded in `.lint-baseline-cap`; this script fails if the
#   live baseline exceeds it. Cap is downward-only by policy (enforced in
#   code review). To raise it intentionally you must edit `.lint-baseline-cap`
#   in the same commit and explain why in the message.
#
# Used by:
#   - hooks/pre-commit.sh         (local pre-commit gate)
#   - .gitlab-ci.yml :: lint-baseline-cap job (CI gate)
#
# See Plans/Audits/audit01/07-lint-baseline-drainage.md.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASELINE="$REPO_ROOT/lint-baseline.xml"
CAP_FILE="$REPO_ROOT/.lint-baseline-cap"

if [ ! -f "$BASELINE" ]; then
    echo "WARN: $BASELINE not found; skipping cap check." >&2
    exit 0
fi
if [ ! -f "$CAP_FILE" ]; then
    echo "ERROR: $CAP_FILE not found." >&2
    exit 1
fi

ENTRIES=$(grep -c 'id="' "$BASELINE" || echo 0)
CAP=$(tr -d '[:space:]' < "$CAP_FILE")

if ! [[ "$CAP" =~ ^[0-9]+$ ]]; then
    echo "ERROR: $CAP_FILE does not contain a single integer (got: '$CAP')." >&2
    exit 1
fi

if [ "$ENTRIES" -gt "$CAP" ]; then
    echo "ERROR: lint-baseline.xml has $ENTRIES entries (cap is $CAP)." >&2
    echo "Either fix the new lint findings or — with justification in the commit message —" >&2
    echo "update .lint-baseline-cap to the new (lower or equal) value." >&2
    exit 1
fi

echo "[lint-baseline-cap] OK: $ENTRIES entries (cap $CAP)."
