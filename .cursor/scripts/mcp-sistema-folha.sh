#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONFIG="${ROOT}/diversos/openapi/api-to-mcp.yml"
SPEC="${ROOT}/diversos/openapi/sistema-folha-openapi.json"

# Ordem: env já definido (mcp.json) → .cursor/mcp.env → ~/.config/sistema-folha/mcp.env
if [[ -z "${OPENAPI_BEARER_TOKEN:-}" && -z "${SISTEMA_FOLHA_API_KEY:-}" ]]; then
  for ENV_FILE in "${ROOT}/.cursor/mcp.env" "${SISTEMA_FOLHA_MCP_ENV:-${HOME}/.config/sistema-folha/mcp.env}"; do
    if [[ -f "${ENV_FILE}" ]]; then
      # shellcheck disable=SC1090
      source "${ENV_FILE}"
      break
    fi
  done
fi

TOKEN="${OPENAPI_BEARER_TOKEN:-${SISTEMA_FOLHA_API_KEY:-}}"
if [[ -z "${TOKEN}" ]]; then
  cat >&2 <<EOF
[sistema-folha MCP] API key não configurada.

Crie o arquivo (gitignored):
  ${ROOT}/.cursor/mcp.env

Conteúdo:
  SISTEMA_FOLHA_API_KEY=sf_live_...

Depois: Settings → MCP → Refresh no servidor sistema-folha.
EOF
  exit 1
fi

if [[ ! -f "${CONFIG}" ]]; then
  echo "[sistema-folha MCP] Config api-to-mcp não encontrada: ${CONFIG}" >&2
  exit 1
fi

if [[ ! -f "${SPEC}" ]]; then
  echo "[sistema-folha MCP] OpenAPI spec não encontrada: ${SPEC}" >&2
  exit 1
fi

export OPENAPI_BEARER_TOKEN="${TOKEN}"

# Cursor/GUI apps often miss asdf shims in PATH
export PATH="${HOME}/.asdf/shims:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:${PATH}"

exec npx -y @sgaluza/api-to-mcp rest --config "${CONFIG}"
