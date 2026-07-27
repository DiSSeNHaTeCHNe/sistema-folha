# Modular Monolith Fix — Context

**Gathered:** 2026-07-26  
**Spec:** `_docs/specs/features/modular-monolith-fix/spec.md`  
**Parent:** `_docs/specs/features/modular-monolith/` (Execute T1–T32 done, Independent Verifier FAIL)  
**Status:** Design Approach A locked (2026-07-26) — awaiting design approve → Tasks

---

## Feature Boundary

Follow-up **fix-only** à feature `modular-monolith`: fechar lacunas registradas em `validation.md` (2026-07-26) sem reabrir migração modular, produto ou rewrite FE target. Escopo limitado a (a) honestidade do contrato lint/checklist, (b) enforcement ArchUnit na camada application + ports para offenders conhecidos, (c) teste HTTP/DTO de `GET /auth/acesso`, (d) P2 MockMvc delegação opcional.

**Inclui:** ajustes em `check-modular-compliance.sh` (mensagens/contrato), ArchUnit + refactors backend, testes auth/ACL, correção lint **introduzida** por modular-monolith, documentação de alinhamento AD-004.

**Fora:** zerar ~42 erros ESLint brownfield; novas features; re-migrar domínios já PASS.

---

## Implementation Decisions

### 1. FE lint — Contrato honesto (default validation gap #1)

- **Decisão:** P1 **não** exige ESLint verde em todo o repositório frontend.
- `./diversos/scripts/check-modular-compliance.sh` permanece **fonte de verdade** para gates modulares FE (greps mandatory + build mandatory + lint advisory com mensagem AD-004).
- Corrigir **somente** violações ESLint **introduzidas** por arquivos alterados na migração `modular-monolith`; dívida pré-existente (`api.ts` `any`, hook deps, etc.) fica fora deste fix.
- Success Criteria desta feature **e** nota de re-validação da pai SHALL declarar: conformidade modular FE ≠ `npm run lint` exit 0 global, alinhado a AD-004 (skills FE = target).
- Alternativa rejeitada neste contexto: mass-fix de ~42 erros brownfield (scope creep vs AD-004).

### 2. ArchUnit application-layer + ports (default validation gap #2)

- **Decisão:** P1 — adicionar regra ArchUnit proibindo `..application..` de domínio D depender de `..infrastructure..` de domínio D' (D' ≠ D).
- Refatorar os cinco offenders reportados na validação pai:
  - `beneficios.application.BeneficioMensalService`
  - `beneficios.application.ImportacaoBeneficioMensalService`
  - `folha.application.FolhaPagamentoService`
  - `organograma.application.OrganogramaService`
  - `auth.application.UsuarioService`
- Estratégia preferida: **ports read-only** expostas por Cadastros (ex. consulta funcionário por id/login) com adapter em `cadastros.application`/`cadastros.infrastructure`; consumidores injetam apenas a interface em `cadastros.port`.
- Mesmo domínio continua podendo usar própria infrastructure (ex. `cadastros.application` → `cadastros.infrastructure`).
- Regra ArchUnit existente em domain layer **permanece**; esta fix **complementa**, não relaxa.

### 3. `GET /auth/acesso` — Prova JSON (default validation gap #3)

- **Decisão:** P1 — teste automatizado na borda auth.
- Preferência: MockMvc autenticado **ou** teste unitário de `AuthenticationService` com assert explícito em **todos** os campos distintivos de `AcessoUsuarioDTO` (`temFuncionarioVinculado`, `temNoOrganograma`, `acessoTotal`, `centrosCustoIds`, `motivoNegacao`).
- Cenários mínimos: (1) SEM_FUNCIONARIO — deny all signals; (2) grant parcial com nó — restricted set, `acessoTotal=false`.
- Testes existentes de `OrganogramaAcessoService` **permanecem**; este teste cobre gap HTTP/mapping, não reimplementa lógica ACL.

### 4. Controller MockMvc delegação (default validation gap #4 — P2)

- **Decisão:** P2 opcional — smoke MockMvc em subset (`BeneficioMensalController`, `AuthController` ou equivalente) verificando delegação ao service.
- Não bloqueia PASS da re-validação pai se P1 completo.
- Grep estático (zero Repository em controllers) já PASS; MockMvc é reforço comportamental.

### 5. Approach A (Design locked 2026-07-26)

- Ports: `FuncionarioConsultaPort`, `CadastrosLookupPort`, `UsuarioLookupPort` (+ adapters same-domain).
- Refator P1 inclui também `OrganogramaAcessoService` → `UsuarioLookupPort` (necessário para ArchUnit literal).
- ArchUnit application-layer com **allowlist temporária** `dashboard.application` + `importacao.application` (AD-009).
- Auth prova P1 = unit `AuthenticationService`; MockMvc controllers = P2.
- Parent amendment = edit inline MOD-11 / Success Criteria FE em `modular-monolith/spec.md`.

### Agent's Discretion (remaining)

- Ordem de refator dos consumers (desde que ArchUnit verde ao final).
- Implementação ArchUnit: regras por domínio vs `ArchCondition` única.

### Declined / Undiscussed Gray Areas → Assumptions

| Gray area | Default logged in spec |
| --------- | ---------------------- |
| Zerar lint brownfield inteiro | Out of scope; AD-004 exception (MODFIX-02) |
| Big-bang port para todos cross-domain imports futuros | Só offenders conhecidos + regra preventiva ArchUnit |
| `@SpringBootTest` full stack | Evitar; MockMvc slice / unit preferred |
| Re-run Verifier antes de user commits | Fix assume same uncommitted tree; user commits when ready |

---

## Specific References

- **Validation evidence:** `_docs/specs/features/modular-monolith/validation.md` — Fix Plans 1–3, ranked gaps 1–4.
- **Lessons (candidate, applied as guidance):** L-002 lint AC vs checklist; L-003 ArchUnit application layer; L-004 HTTP JSON proof.
- **AD-004:** Skills FE target; brownfield compliance script advisory lint.
- **Offenders table:** validation.md lines 107–114 (`BeneficioMensalService.java:16`, etc.).

---

## Deferred Ideas

- Feature futura ROADMAP: adequação completa FE às skills target (eslint green + `src/features/` + TanStack Query) — explicitamente fora deste fix.
- Remover allowlist AD-009: ports Folha/Cadastros stats para `dashboard` + `importacao`.
- Empty `centrosCustoIds` → deny em `BeneficioMensalService` (code-review security).
- Remover logs de senha/hash/refresh token em auth.
- ArchUnit rules para `shared`/`config` cross-cutting — só se surgirem violations após application rule; não escopo inicial.
