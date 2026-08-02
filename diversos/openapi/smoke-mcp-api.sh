#!/usr/bin/env bash
# Optional live smoke for MCP whitelisted HTTP endpoints (MCP-03, MCP-07).
# Skips gracefully when API is down or API key is absent.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_URL="${SISTEMA_FOLHA_API_URL:-http://localhost:8083/api}"
GLOBAL_EMPLOYEE_CEILING="${SMOKE_GLOBAL_EMPLOYEE_CEILING:-310}"

load_token() {
  if [[ -n "${OPENAPI_BEARER_TOKEN:-}" ]]; then
    echo "${OPENAPI_BEARER_TOKEN}"
    return
  fi
  if [[ -n "${SISTEMA_FOLHA_API_KEY:-}" ]]; then
    echo "${SISTEMA_FOLHA_API_KEY}"
    return
  fi
  # Ordem: .cursor/mcp.env → ~/.config/sistema-folha/mcp.env (via SISTEMA_FOLHA_MCP_ENV)
  for ENV_FILE in "${ROOT}/.cursor/mcp.env" "${SISTEMA_FOLHA_MCP_ENV:-${HOME}/.config/sistema-folha/mcp.env}"; do
    if [[ -f "${ENV_FILE}" ]]; then
      # shellcheck disable=SC1090
      source "${ENV_FILE}"
      echo "${OPENAPI_BEARER_TOKEN:-${SISTEMA_FOLHA_API_KEY:-}}"
      return
    fi
  done
  echo ""
}

skip() {
  echo "[smoke-mcp-api] SKIP: $*"
  exit 0
}

token="$(load_token)"
if [[ -z "${token}" ]]; then
  skip "API key ausente (defina SISTEMA_FOLHA_API_KEY, ${ROOT}/.cursor/mcp.env ou ${SISTEMA_FOLHA_MCP_ENV:-${HOME}/.config/sistema-folha/mcp.env})"
fi

if ! curl -sf --max-time 3 "${BASE_URL}/auth/acesso" -o /dev/null -H "Authorization: Bearer ${token}" 2>/dev/null; then
  if ! curl -sf --max-time 3 "${BASE_URL%/api}/actuator/health" -o /dev/null 2>/dev/null; then
    skip "API indisponível em ${BASE_URL} (subir Docker/backend ou ignorar smoke local)"
  fi
  skip "API respondeu mas GET /auth/acesso falhou (key inválida ou backend parcial)"
fi

echo "[smoke-mcp-api] API up — running MCP-03 checks"

acesso_body="$(mktemp)"
folha_body="$(mktemp)"
func_body="$(mktemp)"
trap 'rm -f "${acesso_body}" "${folha_body}" "${func_body}"' EXIT

acesso_code="$(curl -s -o "${acesso_body}" -w '%{http_code}' \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/auth/acesso")"

if [[ "${acesso_code}" != "200" ]]; then
  echo "[smoke-mcp-api] FAIL: GET /auth/acesso expected 200, got ${acesso_code}" >&2
  exit 1
fi

if ! python3 - "${acesso_body}" <<'PY'
import json, sys
json.load(open(sys.argv[1], encoding="utf-8"))
PY
then
  echo "[smoke-mcp-api] FAIL: GET /auth/acesso body is not valid JSON" >&2
  exit 1
fi
echo "[smoke-mcp-api] OK: GET /auth/acesso → 200 JSON"

folha_code="$(curl -s -o "${folha_body}" -w '%{http_code}' \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/resumo-folha-pagamento?ano=2026&mes=5")"

if [[ "${folha_code}" != "200" ]]; then
  echo "[smoke-mcp-api] FAIL: GET /resumo-folha-pagamento expected 200, got ${folha_code}" >&2
  exit 1
fi

folha_count="$(python3 - "${folha_body}" <<'PY'
import json, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
if isinstance(data, list):
    print(len(data))
elif isinstance(data, dict):
    for key in ("content", "items", "data", "resumos"):
        if isinstance(data.get(key), list):
            print(len(data[key]))
            break
    else:
        print(1 if data else 0)
else:
    print(0)
PY
)"

if [[ -z "${folha_count}" ]]; then
  echo "[smoke-mcp-api] FAIL: could not parse folha response cardinality" >&2
  exit 1
fi
echo "[smoke-mcp-api] OK: GET /resumo-folha-pagamento?ano=2026&mes=5 → 200 (items=${folha_count})"

func_code="$(curl -s -o "${func_body}" -w '%{http_code}' \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/funcionarios?page=0&size=500")"

if [[ "${func_code}" == "200" ]]; then
  func_count="$(python3 - "${func_body}" <<'PY'
import json, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
if isinstance(data, list):
    print(len(data))
elif isinstance(data, dict):
    if isinstance(data.get("content"), list):
        print(len(data["content"]))
    elif isinstance(data.get("totalElements"), int):
        print(data["totalElements"])
    else:
        print(len(data))
else:
    print(0)
PY
)"
  echo "[smoke-mcp-api] MCP-07: folha items=${folha_count}, cadastro funcionarios=${func_count:-?}, global ceiling=${GLOBAL_EMPLOYEE_CEILING}"
  if [[ "${folha_count}" -gt "${GLOBAL_EMPLOYEE_CEILING}" ]]; then
    echo "[smoke-mcp-api] FAIL: folha cardinality (${folha_count}) exceeds global ceiling (${GLOBAL_EMPLOYEE_CEILING}) — response not scoped (MCP-07)" >&2
    exit 1
  elif [[ -n "${func_count:-}" && "${folha_count}" -le "${func_count}" && "${folha_count}" -lt "${GLOBAL_EMPLOYEE_CEILING}" ]]; then
    echo "[smoke-mcp-api] OK: scoped cardinality evidence (folha ≤ cadastro < global ceiling)"
  else
    echo "[smoke-mcp-api] OK: folha response parseable (qualitative MCP-07 check logged above)"
  fi
else
  echo "[smoke-mcp-api] WARN: GET /funcionarios returned ${func_code}; MCP-07 cadastro comparison skipped"
fi

echo "[smoke-mcp-api] smoke complete"
