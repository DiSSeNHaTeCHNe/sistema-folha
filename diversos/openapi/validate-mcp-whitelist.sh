#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
YAML="${YAML:-${SCRIPT_DIR}/api-to-mcp.yml}"
SPEC="${SPEC:-${SCRIPT_DIR}/sistema-folha-openapi.json}"

run_validation() {
  local yaml_path="$1"
  local spec_path="$2"
  python3 - "$yaml_path" "$spec_path" <<'PY'
import json
import re
import sys
from pathlib import Path

yaml_path = Path(sys.argv[1])
spec_path = Path(sys.argv[2])

MANDATORY_IDS = {
    "obterInformacoesAcesso",
    "listarTodos",
    "consultarTotaisPorFuncionario",
    "listarCompetencias",
}

MUTABLE_IDS = {
    "processar",
    "cadastrar",
    "cadastrar_1",
    "cadastrar_2",
    "cadastrar_3",
    "cadastrar_4",
    "cadastrar_5",
    "cadastrar_6",
    "importarFolhaAdp",
    "importarBeneficiosMensais",
    "remover",
    "remover_1",
    "remover_2",
    "remover_3",
    "remover_4",
    "remover_5",
    "remover_6",
    "remover_7",
    "remover_8",
    "remover_9",
    "remover_10",
    "removerComFilhos",
    "atualizar",
    "atualizar_1",
    "atualizar_2",
    "atualizar_3",
    "atualizar_4",
    "atualizar_5",
    "atualizar_6",
    "atualizar_7",
    "atualizar_8",
    "criar",
    "criar_1",
    "criar_2",
    "criar_3",
    "login",
    "logout",
    "refreshToken",
    "alterarSenha",
    "revogar",
    "moverNo",
    "ativarOrganograma",
    "desativarOrganograma",
    "associarFuncionario",
    "desassociarFuncionario",
    "associarCentroCusto",
    "desassociarCentroCusto",
}

FORBIDDEN_PATH_PREFIXES = (
    "/organograma",
    "/importacao",
    "/auth/api-keys",
    "/dashboard",
)


def parse_only_list(text: str) -> list[str]:
    try:
        import yaml  # type: ignore

        data = yaml.safe_load(text)
        only = data.get("options", {}).get("only", [])
        if not isinstance(only, list):
            raise ValueError("options.only must be a list")
        return [str(item) for item in only]
    except ImportError:
        pass

    match = re.search(
        r"(?ms)^options:\s*\n(?:[ \t]+.*\n)*?[ \t]+only:\s*\n((?:[ \t]+-\s+.+\n?)+)",
        text,
    )
    if not match:
        raise ValueError("Could not parse options.only from YAML")

    items = []
    for line in match.group(1).splitlines():
        item_match = re.match(r"^\s+-\s+(.+?)\s*$", line)
        if item_match:
            items.append(item_match.group(1).strip().strip('"').strip("'"))
    if not items:
        raise ValueError("options.only is empty or unparsable")
    return items


def parse_readonly(text: str) -> bool:
    try:
        import yaml  # type: ignore

        data = yaml.safe_load(text)
        return bool(data.get("options", {}).get("readonly", False))
    except ImportError:
        return bool(re.search(r"(?m)^\s*readonly:\s*true\s*$", text))


def load_operation_map(spec_path: Path) -> dict[str, tuple[str, str]]:
    spec = json.loads(spec_path.read_text(encoding="utf-8"))
    mapping: dict[str, tuple[str, str]] = {}
    for path, methods in spec.get("paths", {}).items():
        if not isinstance(methods, dict):
            continue
        for method, operation in methods.items():
            if method.lower() not in {"get", "post", "put", "delete", "patch", "head", "options"}:
                continue
            if not isinstance(operation, dict):
                continue
            op_id = operation.get("operationId")
            if op_id:
                mapping[str(op_id)] = (method.upper(), path)
    return mapping


def fail(message: str) -> None:
    print(message, file=sys.stderr)
    sys.exit(1)


yaml_text = yaml_path.read_text(encoding="utf-8")
if not parse_readonly(yaml_text):
    fail("[validate-mcp-whitelist] options.readonly must be true")

try:
    only_ids = parse_only_list(yaml_text)
except ValueError as exc:
    fail(f"[validate-mcp-whitelist] {exc}")

count = len(only_ids)
if count < 10 or count > 15:
    fail(f"[validate-mcp-whitelist] options.only count must be between 10 and 15 (got {count})")

missing_mandatory = sorted(MANDATORY_IDS - set(only_ids))
if missing_mandatory:
    fail(
        "[validate-mcp-whitelist] missing mandatory operationIds: "
        + ", ".join(missing_mandatory)
    )

mutable_present = sorted(set(only_ids) & MUTABLE_IDS)
if mutable_present:
    fail(
        "[validate-mcp-whitelist] mutable operationIds must not be whitelisted: "
        + ", ".join(mutable_present)
    )

if not spec_path.is_file():
    fail(f"[validate-mcp-whitelist] OpenAPI spec not found: {spec_path}")

operation_map = load_operation_map(spec_path)
spec_ids = set(operation_map)
missing_in_spec = sorted(set(only_ids) - spec_ids)
if missing_in_spec:
    fail(
        "[validate-mcp-whitelist] operationIds missing from OpenAPI spec: "
        + ", ".join(missing_in_spec)
    )

non_get = []
forbidden_paths = []
for op_id in only_ids:
    method, path = operation_map[op_id]
    if method != "GET":
        non_get.append(f"{op_id} ({method} {path})")
    normalized = path if path.startswith("/") else f"/{path}"
    for prefix in FORBIDDEN_PATH_PREFIXES:
        if normalized.startswith(prefix):
            forbidden_paths.append(f"{op_id} ({path})")
            break

if non_get:
    fail(
        "[validate-mcp-whitelist] whitelisted operations must be GET:\n  "
        + "\n  ".join(non_get)
    )

if forbidden_paths:
    fail(
        "[validate-mcp-whitelist] forbidden path prefixes in whitelist:\n  "
        + "\n  ".join(forbidden_paths)
    )

print(f"[validate-mcp-whitelist] OK — {count} operationIds validated against {spec_path.name}")
PY
}

self_test() {
  local tmpdir
  tmpdir="$(mktemp -d)"

  local bad_yaml="${tmpdir}/bad-api-to-mcp.yml"
  cp "${YAML}" "${bad_yaml}"
  printf '\n    - __fictitiousOperationIdXYZ__\n' >> "${bad_yaml}"

  echo "[validate-mcp-whitelist] self-test: negative case (fictitious operationId)"
  set +e
  local bad_output bad_status
  bad_output="$(run_validation "${bad_yaml}" "${SPEC}" 2>&1)"
  bad_status=$?
  set -e

  rm -rf "${tmpdir}"

  if [[ "${bad_status}" -eq 0 ]]; then
    echo "[validate-mcp-whitelist] self-test FAILED: expected non-zero exit for fictitious ID" >&2
    exit 1
  fi
  if [[ "${bad_output}" != *"__fictitiousOperationIdXYZ__"* ]]; then
    echo "[validate-mcp-whitelist] self-test FAILED: stderr must list missing operationId" >&2
    echo "${bad_output}" >&2
    exit 1
  fi
  echo "[validate-mcp-whitelist] self-test: negative case passed"

  echo "[validate-mcp-whitelist] self-test: positive case (production yaml+spec)"
  run_validation "${YAML}" "${SPEC}"
  echo "[validate-mcp-whitelist] self-test: positive case passed"
  echo "[validate-mcp-whitelist] self-test complete (2 assertions)"
}

main() {
  if [[ "${1:-}" == "--self-test" ]]; then
    self_test
    return
  fi

  if [[ ! -f "${YAML}" ]]; then
    echo "[validate-mcp-whitelist] config not found: ${YAML}" >&2
    exit 1
  fi
  if [[ ! -f "${SPEC}" ]]; then
    echo "[validate-mcp-whitelist] spec not found: ${SPEC}" >&2
    exit 1
  fi

  run_validation "${YAML}" "${SPEC}"
}

main "$@"
