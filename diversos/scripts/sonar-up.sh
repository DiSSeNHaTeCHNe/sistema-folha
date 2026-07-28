#!/usr/bin/env bash
# Sobe o SonarQube local via Docker (cria volumes na primeira execução).
#
# Uso:
#   ./diversos/scripts/sonar-up.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMPOSE_FILE="${ROOT}/diversos/sonarqube/docker-compose.yml"
SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"

for vol in sonarqube_data sonarqube_extensions sonarqube_logs sonarqube_temp; do
  if ! docker volume inspect "$vol" >/dev/null 2>&1; then
    echo "==> Criando volume Docker: ${vol}"
    docker volume create "$vol" >/dev/null
  fi
done

echo "==> Subindo SonarQube (docker compose)"
docker compose -f "$COMPOSE_FILE" up -d

echo "==> Aguardando SonarQube ficar UP em ${SONAR_HOST_URL}"
for i in $(seq 1 60); do
  status="$(curl -sS "${SONAR_HOST_URL}/api/system/status" 2>/dev/null | python3 -c 'import sys,json
try:
  print(json.load(sys.stdin).get("status",""))
except Exception:
  print("")' || true)"
  if [[ "$status" == "UP" ]]; then
    echo "    SonarQube UP (${i}s)"
    echo
    echo "Pronto: ${SONAR_HOST_URL}"
    echo "Próximo passo: SONAR_USER=admin SONAR_PASSWORD='***' ./diversos/scripts/sonar-setup.sh"
    exit 0
  fi
  sleep 5
done

echo "Timeout: SonarQube não respondeu UP em 5 minutos." >&2
echo "Verifique: docker compose -f ${COMPOSE_FILE} logs -f sonarqube" >&2
exit 1
