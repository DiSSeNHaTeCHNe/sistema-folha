# Ajuste do Harness — Context

**Gathered:** 2026-07-26  
**Spec:** `_docs/specs/features/ajuste-harness/spec.md`  
**Status:** Design Draft escrito — aguardando aprovação do usuário (ainda sem Execute)

---

## Feature Boundary

Ajustar o harness de agentes (governança, paths, versionamento Git, skills target vs brownfield, ponteiros de ferramentas) para que sirva de forma confiável a **desenvolver e manter** o sistema-folha.

**Explicitamente fora:** adequação do código de negócio às skills (fase 2, feature separada).  
**Explicitamente fora:** conteúdo transitório em `_docs/temp/` (não considerar, não versionar, não migrar).

---

## Implementation Decisions

### 1. Versionamento Git (escolha: A)

- Versionar o **núcleo do harness**:
  - `AGENTS.md` (raiz)
  - `.agents/` (rules, skills, references)
  - `_docs/specs/` (PROJECT, ROADMAP, STATE, HANDOFF, brownfield, features, quick)
- **Não versionar:** `_docs/temp/`, secrets, `.env*`, scratch, `_old/`
- Ajustar `.gitignore` para deixar de ignorar o núcleo acima; manter ignore de `.specs/` (layout deprecado) e de `_docs/temp/` se `_docs/` deixar de ser ignorado por completo
- Decisão de detalhe do ignore (ignorar `_docs/` inteiro exceto `specs/`, vs versionar só `specs/` e manter resto de `_docs` fora): **Agent's Discretion** na Design — preferência: versionar `_docs/specs/**` e manter `_docs/temp/**` e demais scratch fora do Git

### 2. Skills frontend: target vs brownfield (escolha: B)

- Skills aspiracionais (`api-client`, `forms-validation`, `component-architecture`, `routing-perf`, `testing-a11y`) permanecem como **target state**
- Harness deve **documentar** o brownfield atual em `CONVENTIONS.md` / `STRUCTURE.md` / `TESTING.md` (já existentes; atualizar se necessário)
- Skills target devem declarar explicitamente: **não aplicar como obrigação até item correspondente no ROADMAP** (ou AD futuro)
- Agentes devem preferir brownfield docs + código existente quando target não estiver liberado no ROADMAP

### 3. TLC — ajuste pontual de paths para `_docs` (revisão Q1)

- Permitido **ajuste cirúrgico** em `.agents/skills/tlc-spec-driven/` **somente** para apontar documentação/memória/artefatos para `_docs/specs/` (e layout AD-001)
- Escopo do patch: paths em `SKILL.md`, `references/*.md` relevantes, `scripts/lessons.py` (`STORE_REL` / `RENDER_REL` / mensagens) — trocar `.specs/` → `_docs/specs/` (ou equivalente canônico do projeto)
- **Fora do patch:** mudar o fluxo Specify→Design→Tasks→Execute, Verifier, auto-sizing, lessons rules de negócio, sub-agents mechanics
- Continuar usando TLC como procedimento operacional completo após o path fix
- Symlink `.specs` → `_docs/specs` **não é necessário** se o patch cirúrgico cobrir writers/readers

### 4. Linear e PRD/TDD/SDD (escolha: conforme solicitado / AGENTS.md)

- Seguir `AGENTS.md` e `.agents/rules/linear_issue_management.md` como já definidos:
  - Linear com **key única** por produto quando houver trabalho multiagente / backlog organizado
  - Título `[Frente] Verbo + objeto + contexto`
  - PRD/TDD/SDD como fontes de orientação **quando existirem**; não inventar conteúdo vazio só para “encher pasta” nesta feature
- Rastreio TLC via `_docs/specs/features/[feature]/` + Requirement IDs permanece obrigatório no fluxo spec-driven
- Popular PRD/TDD/SDD é **fase posterior / outra feature** se o usuário pedir — não é P1 deste ajuste de harness

### 5. Ferramentas e AGENTS sob `_docs` (revisão Q2)

- Canônico: **`AGENTS.md` na raiz** apenas
- **Apagar** `_docs/AGENTS.md` — arquivos desse tipo em `_docs/` são **exclusivamente transitórios**; não devem permanecer como cópia de governança
- Regra geral: não versionar nem manter `AGENTS.md` (nem equivalentes de governança permanente) dentro de `_docs/`; transitórios vão em `_docs/temp/` e saem do Git
- Manter: Cursor + Claude apontando para `AGENTS.md` na raiz
- Antigravity: uso eventual — ponteiro para a **raiz**; não recriar `_docs/AGENTS.md`
- Windsurf: remover stub vazio / não manter tooling morto
- Atualizar `.cursorrules` / docs de unificação (paths reais; sem `_D2TLabs`)

### Agent's Discretion

- Forma exata do `.gitignore` (whitelist `_docs/specs` vs un-ignore seletivo de `_docs/`)
- Lista exata de arquivos TLC a tocar no patch de path (mínimo necessário)
- Texto exato dos banners “target / não aplicar até ROADMAP” nas skills FE

### Declined / Undiscussed → Assumptions

| Área | Default | Rationale |
|------|---------|-----------|
| Rigor TLC (Verifier) | Seguir skill TLC sem afrouxar o contrato | Patch só de paths; fluxo intacto |
| Conteúdo mínimo PRD agora | Não criar PRD/TDD/SDD fake | “Conforme solicitado” = AGENTS; pastas vazias não ajudam |
| `_docs/temp` | Ignorar e nunca versionar | Pedido explícito do usuário |
| Symlink `.specs` | Não usar se patch TLC cobrir | Q1 revisado: ajuste cirúrgico preferido |

---

## Specific References

- Relatório: `_docs/RELATORIO_CONFORMIDADE_HARNESS_2026-07-26.md` (diagnóstico; não é contrato)
- AD-001: layout flat `_docs/specs/`
- Usuário: harness pós-brownfield; primeiro ajustar harness; depois adequar projeto; revisar decisões antes de executar

---

## Deferred Ideas

- Feature separada: **adequação do projeto ao harness** (SecurityConfig, JWT, ddl-auto, OpenAPI, testes FE, lazy routes, Zod, limpeza legado benefícios, etc.)
- Popular `_docs/prd|tdd|sdd` com documentos reais de milestone
- Config completa Antigravity quando o usuário for usá-lo de fato
