#!/usr/bin/env bash
# Reproducible pre-merge gate for adequacao-analise-projeto R4.
# Usage: ./diversos/scripts/gate-r4-local.sh [--docker] [--e2e] [--sonar]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

RUN_DOCKER=false
RUN_E2E=false
RUN_SONAR=false

for arg in "$@"; do
  case "$arg" in
    --docker) RUN_DOCKER=true ;;
    --e2e) RUN_E2E=true ;;
    --sonar) RUN_SONAR=true ;;
    -h|--help)
      echo "Usage: $0 [--docker] [--e2e] [--sonar]"
      echo "  (default) mvn test + npm test + JaCoCo thresholds"
      echo "  --docker  Include ImportacaoFolhaAdpIntegrationTest (warn+continue if skipped)"
      echo "  --e2e     Include frontend Playwright smoke"
      echo "  --sonar   Include ./diversos/scripts/sonar-analyze.sh (requires Sonar UP + .sonar.env)"
      exit 0
      ;;
    *)
      echo "Unknown flag: $arg" >&2
      exit 2
      ;;
  esac
done

echo "==> Backend: mvn test"
(cd backend && mvn -q test)

echo "==> JaCoCo thresholds"
bash diversos/scripts/check-jacoco-thresholds.sh

echo "==> Frontend: npm test"
(cd frontend && npm test)

if [[ "$RUN_DOCKER" == true ]]; then
  echo "==> ADP integration (Docker-gated)"
  set +e
  adp_log="$(cd backend && mvn -q test -Dtest=ImportacaoFolhaAdpIntegrationTest 2>&1)"
  adp_exit=$?
  set -e
  echo "$adp_log" | tail -8
  if [[ $adp_exit -ne 0 ]]; then
    echo "ERROR: ImportacaoFolhaAdpIntegrationTest failed (exit $adp_exit)" >&2
    exit "$adp_exit"
  fi
  if echo "$adp_log" | grep -q "Skipped: 1"; then
    echo "WARNING: ADP integration skipped (Docker/Testcontainers unavailable). Continuing." >&2
  fi
fi

if [[ "$RUN_E2E" == true ]]; then
  echo "==> Playwright e2e"
  (cd frontend && npm run test:e2e)
fi

if [[ "$RUN_SONAR" == true ]]; then
  echo "==> Sonar analyze"
  bash diversos/scripts/sonar-analyze.sh
fi

echo
echo "gate-r4-local: all invoked steps passed."
