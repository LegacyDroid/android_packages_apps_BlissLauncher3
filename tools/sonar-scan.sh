#!/usr/bin/env bash
#
# tools/sonar-scan.sh — manual SonarQube scan against the project's Sonar
# instance. Reads project metadata from the repo-root sonar-project.properties.
#
# Usage:
#   tools/sonar-scan.sh                 # uses defaults
#   SONAR_TOKEN=sqp_xxx tools/sonar-scan.sh
#   tools/sonar-scan.sh -Dsonar.foo=bar # extra args forwarded to sonar-scanner
#
# Token resolution order (first non-empty wins):
#   1. $SONAR_TOKEN environment variable
#   2. ~/.sonar-token  (file mode 600 expected)
#   3. fail with instructions
#
# Why this exists:
#   - The gradle Sonar plugin (sonarqube-gradle-plugin 7.3.0) hits a
#     'fileCollection is null' incompat with AGP 8.13's variant model on
#     :bliss-prefs / :bliss-compat / :animationlib / root, so the standalone
#     sonar-scanner CLI is the working path. sonar-project.properties has
#     'sonar.java.binaries=.sonar-empty', i.e. source-only scan, no compile
#     output needed.
#   - jobzy.fi runs SonarQube Community Build, which doesn't support multi-
#     branch analysis, so we don't pass -Dsonar.branch.name (the scanner
#     would reject it).
set -euo pipefail

SONAR_HOST_URL="${SONAR_HOST_URL:-https://sonarqube.jobzy.fi}"

resolve_token() {
  if [[ -n "${SONAR_TOKEN:-}" ]]; then
    echo "$SONAR_TOKEN"
    return
  fi
  if [[ -r "$HOME/.sonar-token" ]]; then
    cat "$HOME/.sonar-token"
    return
  fi
  echo "error: no token. Set SONAR_TOKEN env var or write the token to ~/.sonar-token (chmod 600)" >&2
  echo "       generate one at: $SONAR_HOST_URL/account/security/" >&2
  exit 1
}

require_scanner() {
  if ! command -v sonar-scanner >/dev/null 2>&1; then
    echo "error: sonar-scanner not found on PATH" >&2
    echo "       install via: curl -sSLo /tmp/ss.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-6.2.1.4610-linux-x64.zip && unzip -q /tmp/ss.zip -d /opt && sudo ln -s /opt/sonar-scanner-6.2.1.4610-linux-x64/bin/sonar-scanner /usr/local/bin/sonar-scanner" >&2
    exit 1
  fi
}

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

require_scanner
TOKEN=$(resolve_token)

exec sonar-scanner \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$TOKEN" \
  "$@"
