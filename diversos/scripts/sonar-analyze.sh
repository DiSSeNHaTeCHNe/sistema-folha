#!/usr/bin/env bash
# Compila o backend (binários Java) e envia análise ao SonarQube local (máquina).
# Pré-requisitos:
#   - Sonar UP: ./diversos/scripts/sonar-up.sh
#   - .sonar.env com SONAR_TOKEN (via sonar-setup.sh)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

ENV_FILE="${SONAR_ENV_FILE:-$ROOT/.sonar.env}"
[[ -f "$ENV_FILE" ]] || {
  echo "Arquivo ${ENV_FILE} não encontrado. Rode primeiro: ./diversos/scripts/sonar-setup.sh" >&2
  exit 1
}
# shellcheck disable=SC1090
set -a; source "$ENV_FILE"; set +a

: "${SONAR_HOST_URL:=http://localhost:9000}"
: "${SONAR_TOKEN:?SONAR_TOKEN ausente em ${ENV_FILE}}"
: "${SONAR_PROJECT_KEY:=sistema-folha}"

echo "==> Compilando backend + dependências para análise Java"
(cd backend && mvn -q -DskipTests compile test-compile dependency:copy-dependencies \
  -DoutputDirectory=target/dependency -DincludeScope=compile)
(cd backend && mvn -q dependency:copy-dependencies \
  -DoutputDirectory=target/test-dependency -DincludeScope=test)

echo "==> Testes + JaCoCo (cobertura para o Sonar)"
(cd backend && mvn -q test)
if [[ ! -f backend/target/site/jacoco/jacoco.xml ]]; then
  echo "Aviso: jacoco.xml não gerado — Sonar seguirá sem cobertura Java" >&2
fi

echo "==> Rodando sonar-scanner (Docker)"
# host.docker.internal: Sonar no host, scanner no container
docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -e SONAR_HOST_URL="http://host.docker.internal:9000" \
  -e SONAR_TOKEN="${SONAR_TOKEN}" \
  -v "${ROOT}:/usr/src" \
  -w /usr/src \
  sonarsource/sonar-scanner-cli \
  -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
  -Dsonar.scm.disabled=true \
  -Dsonar.coverage.jacoco.xmlReportPaths=backend/target/site/jacoco/jacoco.xml

echo
echo "Análise enviada. Dashboard: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
