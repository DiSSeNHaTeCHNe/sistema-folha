#!/usr/bin/env bash
# Bootstrap SonarQube: cria o projeto neste servidor local, gera user token e grava .sonar.env
# Pré-requisito: Sonar compartilhado da máquina UP
#   cd ~/devtools/sonarqube && docker compose up -d
#
# Uso:
#   SONAR_USER=admin SONAR_PASSWORD='***' ./diversos/scripts/sonar-setup.sh
#   # ou, se já tiver token:
#   SONAR_TOKEN='***' ./diversos/scripts/sonar-setup.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"
SONAR_PROJECT_KEY="${SONAR_PROJECT_KEY:-sistema-folha}"
SONAR_PROJECT_NAME="${SONAR_PROJECT_NAME:-Sistema Folha}"
ENV_FILE="${SONAR_ENV_FILE:-$ROOT/.sonar.env}"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a; source "$ENV_FILE"; set +a
fi

need() { [[ -n "${!1:-}" ]] || { echo "Erro: defina $1" >&2; exit 1; }; }

api() {
  local method="$1" path="$2"; shift 2
  if [[ -n "${SONAR_TOKEN:-}" ]]; then
    curl -sS -X "$method" -u "${SONAR_TOKEN}:" "$@" "${SONAR_HOST_URL}${path}"
  else
    need SONAR_USER
    need SONAR_PASSWORD
    curl -sS -X "$method" -u "${SONAR_USER}:${SONAR_PASSWORD}" "$@" "${SONAR_HOST_URL}${path}"
  fi
}

echo "==> Checando SonarQube em ${SONAR_HOST_URL}"
status="$(curl -sS "${SONAR_HOST_URL}/api/system/status" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("status",""))')"
[[ "$status" == "UP" ]] || { echo "SonarQube não está UP (status=$status)" >&2; exit 1; }

auth="$(api GET /api/authentication/validate)"
valid="$(python3 -c 'import json,sys; print(json.loads(sys.argv[1]).get("valid"))' "$auth")"
[[ "$valid" == "True" || "$valid" == "true" ]] || {
  echo "Autenticação falhou. Informe SONAR_USER/SONAR_PASSWORD ou SONAR_TOKEN válido." >&2
  exit 1
}
echo "    autenticação OK"

echo "==> Garantindo projeto ${SONAR_PROJECT_KEY}"
exists="$(api GET "/api/components/show?component=${SONAR_PROJECT_KEY}" -o /tmp/sonar-comp.json -w "%{http_code}" || true)"
if [[ "$exists" != "200" ]]; then
  api POST /api/projects/create \
    --data-urlencode "project=${SONAR_PROJECT_KEY}" \
    --data-urlencode "name=${SONAR_PROJECT_NAME}" \
    --data-urlencode "visibility=private" >/dev/null
  echo "    projeto criado"
else
  echo "    projeto já existe"
fi

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "==> Gerando user token para MCP/scanner"
  token_name="sistema-folha-local-$(date +%Y%m%d%H%M%S)"
  # API moderna (SonarQube 10+ / Community Build)
  resp="$(api POST /api/user_tokens/generate \
    --data-urlencode "name=${token_name}" \
    --data-urlencode "type=USER_TOKEN" || true)"
  SONAR_TOKEN="$(python3 -c 'import json,sys
try:
  d=json.loads(sys.argv[1]); print(d.get("token") or "")
except Exception:
  print("")' "$resp")"
  if [[ -z "$SONAR_TOKEN" ]]; then
    # fallback API antiga
    resp="$(api POST /api/user_tokens/generate --data-urlencode "name=${token_name}")"
    SONAR_TOKEN="$(python3 -c 'import json,sys; print(json.loads(sys.argv[1]).get("token",""))' "$resp")"
  fi
  [[ -n "$SONAR_TOKEN" ]] || { echo "Falha ao gerar token. Resposta: $resp" >&2; exit 1; }
  echo "    token gerado: ${token_name}"
else
  echo "==> Reutilizando SONAR_TOKEN já informado"
fi

umask 077
# Valores entre aspas simples para nomes com espaço (ex.: Sistema Folha)
python3 - "$ENV_FILE" "$SONAR_HOST_URL" "$SONAR_PROJECT_KEY" "$SONAR_PROJECT_NAME" "$SONAR_TOKEN" <<'PY'
from pathlib import Path
import sys
env = Path(sys.argv[1])
vals = {
  "SONAR_HOST_URL": sys.argv[2],
  "SONAR_PROJECT_KEY": sys.argv[3],
  "SONAR_PROJECT_NAME": sys.argv[4],
  "SONAR_TOKEN": sys.argv[5],
}
lines = []
for k, v in vals.items():
    escaped = v.replace("'", "'\"'\"'")
    lines.append(f"{k}='{escaped}'")
env.write_text("\n".join(lines) + "\n")
print(f"==> Gravado {env} (não versionar)")
PY

MCP_JSON="${HOME}/.cursor/mcp.json"
echo "==> Atualizando MCP Cursor (${MCP_JSON})"
python3 - <<'PY' "$MCP_JSON" "$SONAR_TOKEN"
import json, os, sys
path, token = sys.argv[1], sys.argv[2]
data = {}
if os.path.exists(path):
    with open(path) as f:
        data = json.load(f)
servers = data.setdefault("mcpServers", {})
servers["sonarqube"] = {
    "command": "docker",
    "args": [
        "run", "-i", "--rm", "--init", "--pull=always",
        "-e", "SONARQUBE_TOKEN",
        "-e", "SONARQUBE_URL",
        "sonarsource/sonarqube-mcp",
    ],
    "env": {
        "SONARQUBE_TOKEN": token,
        "SONARQUBE_URL": "http://host.docker.internal:9000",
    },
}
os.makedirs(os.path.dirname(path), exist_ok=True)
with open(path, "w") as f:
    json.dump(data, f, indent=2)
    f.write("\n")
print("    mcpServers.sonarqube configurado")
PY

echo
echo "Pronto."
echo "  1. Reinicie o Cursor (ou toggle MCP sonarqube em Settings → Tools & MCPs)."
echo "  2. Rode a análise: ./diversos/scripts/sonar-analyze.sh"
echo "  3. UI: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
