#!/usr/bin/env bash
#
# Modular monolith compliance checklist (MOD-24, MOD-25, MOD-30).
# Run from repository root: ./diversos/scripts/check-modular-compliance.sh
#
# Note: mvn test uses Mockito inline mock maker and requires JVM self-attach
# (may fail in restricted sandboxes; run on a normal dev shell).
# Exit codes:
#   0 — all mandatory checks passed (modular + backend tests + FE build)
#   1 — one or more mandatory checks failed
#
# Frontend lint runs always; failure is reported in ADVISORY section (pre-existing
# brownfield debt per T19–T22). Modular-specific FE greps are mandatory.
#
set -uo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND="$ROOT/backend"
FRONTEND="$ROOT/frontend"

MODULAR_FAIL=0
BACKEND_FAIL=0
FE_MODULAR_FAIL=0
FE_BUILD_FAIL=0
LINT_FAIL=0

pass() { echo -e "${GREEN}✓${NC} $1"; }
fail() { echo -e "${RED}✗${NC} $1"; }
warn() { echo -e "${YELLOW}!${NC} $1"; }
section() { echo -e "\n${CYAN}=== $1 ===${NC}"; }

grep_zero() {
  local label="$1"
  local pattern="$2"
  local path="$3"
  local extra_args="${4:-}"
  # shellcheck disable=SC2086
  if rg -q $extra_args "$pattern" "$path" 2>/dev/null; then
    fail "$label"
    rg -n $extra_args "$pattern" "$path" 2>/dev/null | head -20 || true
    return 1
  fi
  pass "$label"
  return 0
}

file_exists() {
  local label="$1"
  local file="$2"
  if [[ -f "$file" ]]; then
    pass "$label"
    return 0
  fi
  fail "$label (missing: $file)"
  return 1
}

# --- 1. Modular compliance (grep / static) ---
section "MODULAR COMPLIANCE (mandatory)"

file_exists "BeneficioConsultaPort present" \
  "$BACKEND/src/main/java/br/com/techne/sistemafolha/beneficios/port/BeneficioConsultaPort.java" \
  || MODULAR_FAIL=1

file_exists "OrganogramaAcessoPort present" \
  "$BACKEND/src/main/java/br/com/techne/sistemafolha/organograma/acesso/port/OrganogramaAcessoPort.java" \
  || MODULAR_FAIL=1

file_exists "BeneficioConsultaAdapter present" \
  "$BACKEND/src/main/java/br/com/techne/sistemafolha/beneficios/application/BeneficioConsultaAdapter.java" \
  || MODULAR_FAIL=1

grep_zero "Zero BeneficioRepository (legacy)" \
  '\bBeneficioRepository\b' \
  "$BACKEND/src" \
  || MODULAR_FAIL=1

grep_zero "Zero model.Beneficio entity (legacy)" \
  'model\.Beneficio[^M]|/Beneficio\.java' \
  "$BACKEND/src" \
  || MODULAR_FAIL=1

grep_zero "Zero frontend beneficioService" \
  'beneficioService' \
  "$FRONTEND/src" \
  || MODULAR_FAIL=1

if [[ -f "$FRONTEND/src/services/beneficioService.ts" ]]; then
  fail "beneficioService.ts orphan still exists"
  MODULAR_FAIL=1
else
  pass "beneficioService.ts absent"
fi

if [[ -d "$FRONTEND/src/pages/Example" ]]; then
  fail "pages/Example orphan still exists"
  MODULAR_FAIL=1
else
  pass "pages/Example absent"
fi

if [[ -f "$FRONTEND/src/App.tsx" ]] && ! rg -q "App\.tsx" "$FRONTEND/src/main.tsx" 2>/dev/null; then
  fail "App.tsx orphan (not in main.tsx graph)"
  MODULAR_FAIL=1
else
  pass "App.tsx not orphaned"
fi

file_exists "Flyway V1.14 drop beneficios legado" \
  "$BACKEND/src/main/resources/db/migration/V1.14__drop_beneficios_legado.sql" \
  || MODULAR_FAIL=1

if rg -q '/api/beneficios' "$BACKEND/src/main/java/br/com/techne/sistemafolha/config/SecurityConfig.java" 2>/dev/null; then
  fail "SecurityConfig still has obsolete /api/beneficios matcher"
  MODULAR_FAIL=1
else
  pass "SecurityConfig without /api/beneficios matcher"
fi

if rg -q 'DomainLogging' "$BACKEND/src/main/java/br/com/techne/sistemafolha/organograma/acesso/application/OrganogramaAcessoService.java" 2>/dev/null; then
  pass "ACL service uses domain= structured logging"
else
  fail "OrganogramaAcessoService missing DomainLogging"
  MODULAR_FAIL=1
fi

# --- 2. ArchUnit ---
section "ARCHUNIT (mandatory)"

# Mockito inline mock maker requires JVM self-attach (fails in some sandboxes).
if (cd "$BACKEND" && mvn -q test -Dtest=ModularArchitectureTest 2>&1); then
  pass "ModularArchitectureTest"
else
  fail "ModularArchitectureTest"
  MODULAR_FAIL=1
fi

# --- 3. Backend full test suite ---
section "BACKEND TESTS (mandatory)"

if (cd "$BACKEND" && mvn -q test 2>&1); then
  pass "mvn test (full suite)"
else
  fail "mvn test (full suite)"
  BACKEND_FAIL=1
fi

# --- 4. Frontend modular checks ---
section "FRONTEND MODULAR (mandatory)"

if rg -q "from ['\"].*services/api|from ['\"].*\/api['\"]" "$FRONTEND/src/pages" 2>/dev/null; then
  fail "pages/ import api.ts directly"
  rg -n "from ['\"].*services/api|from ['\"].*\/api['\"]" "$FRONTEND/src/pages" 2>/dev/null | head -20 || true
  FE_MODULAR_FAIL=1
else
  pass "Zero direct api.ts imports in pages/"
fi

if rg -q 'temFuncionarioVinculado' "$FRONTEND/src/types/index.ts" 2>/dev/null \
   && rg -q 'motivoNegacao' "$FRONTEND/src/types/index.ts" 2>/dev/null \
   && rg -q 'centrosCustoIds' "$FRONTEND/src/types/index.ts" 2>/dev/null; then
  pass "AcessoUsuario ACL fields in types/index.ts"
else
  fail "AcessoUsuario ACL fields missing in types/index.ts"
  FE_MODULAR_FAIL=1
fi

if rg -q 'temFuncionarioVinculado' "$FRONTEND/src/contexts/AuthContext.tsx" 2>/dev/null \
   && rg -q 'temNoOrganograma' "$FRONTEND/src/contexts/AuthContext.tsx" 2>/dev/null; then
  pass "AuthContext honors distinct ACL denial signals"
else
  fail "AuthContext missing ACL denial logic"
  FE_MODULAR_FAIL=1
fi

# --- 5. Frontend lint (advisory) ---
section "FRONTEND LINT (advisory — AD-004; modular FE compliance ≠ ESLint verde global)"

echo "AD-004: conformidade modular FE (greps + build acima) ≠ ESLint verde global."
echo "Lint failure here is advisory only when mandatory modular gates pass."

if (cd "$FRONTEND" && npm run lint --silent 2>&1); then
  pass "npm run lint"
else
  warn "npm run lint FAILED (advisory per AD-004; modular FE compliance ≠ ESLint verde global)"
  LINT_FAIL=1
fi

# --- 6. Frontend build (mandatory) ---
section "FRONTEND BUILD (mandatory)"

if (cd "$FRONTEND" && npm run build --silent 2>&1); then
  pass "npm run build"
else
  fail "npm run build"
  FE_BUILD_FAIL=1
fi

# --- Summary ---
section "SUMMARY"

mandatory_ok=0
if [[ $MODULAR_FAIL -eq 0 && $BACKEND_FAIL -eq 0 && $FE_MODULAR_FAIL -eq 0 && $FE_BUILD_FAIL -eq 0 ]]; then
  mandatory_ok=1
  echo -e "${GREEN}Mandatory checks: PASS${NC}"
else
  echo -e "${RED}Mandatory checks: FAIL${NC}"
  [[ $MODULAR_FAIL -ne 0 ]] && echo "  - Modular/ArchUnit static checks failed"
  [[ $BACKEND_FAIL -ne 0 ]] && echo "  - Backend mvn test failed"
  [[ $FE_MODULAR_FAIL -ne 0 ]] && echo "  - Frontend modular greps failed"
  [[ $FE_BUILD_FAIL -ne 0 ]] && echo "  - Frontend build failed"
fi

if [[ $LINT_FAIL -eq 0 ]]; then
  echo -e "${GREEN}Frontend lint: PASS${NC}"
else
  echo -e "${YELLOW}Frontend lint: FAIL (advisory only per AD-004 — modular FE compliance ≠ ESLint verde global)${NC}"
fi

if [[ $mandatory_ok -eq 1 ]]; then
  exit 0
fi
exit 1
