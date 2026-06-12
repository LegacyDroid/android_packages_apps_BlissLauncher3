#!/bin/bash

echo '[githooks] formatting files using spotless'
echo

./gradlew spotlessApply

changed_files="$(git diff --name-only)"
echo

# check if there are untracked files
if [[ ! -z "$changed_files" ]] && [[ -n "$changed_files" ]];
then
    echo '[githooks] aborting commit, untracked files found:'
    echo "$changed_files"
    exit 1
else
    echo '[githooks] continuing commit, no untracked files found'
fi

# Audit01 #07 — block commits that grow lint-baseline.xml past the recorded cap.
# See hooks/check-lint-baseline-cap.sh and Plans/Audits/audit01/07-lint-baseline-drainage.md.
if [ -x ./hooks/check-lint-baseline-cap.sh ]; then
    echo '[githooks] checking lint-baseline cap'
    ./hooks/check-lint-baseline-cap.sh || exit 1
fi
