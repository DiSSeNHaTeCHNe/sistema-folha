<!--
  Este arquivo é intencionalmente curto (paper ETH Zurich, fev/2026: context file
  grande/genérico piora a taxa de sucesso do agente em -3% e custa +20% a mais;
  só o escrito à mão e mínimo ajuda, +4%). Não repita aqui o que já está no
  README. Conhecimento denso vai para uma skill em .claude/skills/.
-->

# AGENTS.md

## Ambiente
- Node: ver `.nvmrc` (24 LTS, "Krypton"). Fixar também em `engines.node` no `package.json`: `">=24.0.0 <25"`.
<!-- AJUSTAR: assumi npm como gerenciador de pacotes (perfil indica experiência com npm).
     Se o time usa pnpm/yarn, troque os comandos abaixo e o lockfile referenciado. -->
- Gerenciador: npm

## Build & Test
- Instalar: `npm ci`
- Dev server: `npm run dev`
- Build: `npm run build`
- Type-check (sem emitir): `npm run typecheck`
- Lint: `npm run lint`
- Testes unitários (todos): `npm run test`
- Teste unitário único: `npm run test -- <caminho ou nome do teste>`
- E2E: `npm run test:e2e`
- Gerar tipos a partir do OpenAPI do backend: `npm run gen:api-types` (ver skill `api-client`)

## Stack (não repetir detalhes — ver skills quando precisar)
React 19.2 + Vite 8 (Rolldown) + TypeScript 6.0.3 (modo strict) + React Router v7 +
Tailwind CSS + React Hook Form + Zod + TanStack Query v5 + Vitest 4 + Testing
Library + Playwright + MSW.

<!-- AJUSTAR: TypeScript fixado em 6.0.3, não 7.0.x — o typescript-eslint ainda não
     suporta a API do TS 7 (chega só na 7.1, ~out/2026). Reavaliar quando o
     typescript-eslint confirmar suporte ao TS7. -->

## Regras que o agente mais erra
- Proibido `any`. Use `unknown` + narrowing, ou o tipo gerado a partir do OpenAPI.
- Proibido `as` para calar o compilador. Se o tipo não bate, corrija a origem do tipo.
- Só componente de função + hooks. Nunca class component.
- `memo`/`useMemo`/`useCallback` só com motivo medido (comente qual re-render real está sendo evitado). Não usar por reflexo.
- Estrutura por feature em `src/features/<nome>/`. Nunca crie `src/components/`, `src/hooks/` ou `src/services/` genéricos na raiz — isso é estrutura por tipo de arquivo, proibida.
- Toda query/mutation de teste usa `getByRole`/`getByLabelText`/`getByText`. Nunca `data-testid` nem seletor de classe CSS.
- Erro de API é sempre lido como RFC 7807 (`ProblemDetail`: `type`, `title`, `status`, `detail`, `instance`). Nunca inventar shape de erro próprio.
- Listagem paginada usa o tipo único `Page<T>` de `src/lib/api/page.ts` (`content`, `totalElements`, `totalPages`, `number`, `size`). Nunca redeclare esse shape em um hook.
- Valor monetário (BigDecimal serializado) trafega como `string`. Nunca `Number()` para cálculo ou comparação — use a lib de decimal da skill `api-client`.
- Datas em ISO-8601 como vêm do backend (`LocalDate` = `"2026-07-25"`, instante com offset). Não reformate na camada de dados — formate só na borda de UI.
- Token JWT nunca em `localStorage` sem aprovação explícita do usuário nesta conversa.

## Skills disponíveis (carregadas sob demanda — ver `.claude/skills/`)
- `api-client` — camada de acesso a dados: cliente HTTP, erro RFC 7807, paginação, JWT, dinheiro, datas, geração de tipos do OpenAPI.
- `forms-validation` — React Hook Form + Zod, mapeamento de erro de Bean Validation por campo.
- `component-architecture` — composição, estrutura por feature, memo com critério, acessibilidade.
- `testing-a11y` — Vitest/Testing Library/Playwright por papel, MSW.
- `routing-perf` — React Router v7, code splitting, orçamento de bundle.

## Brownfield R3/R4 (AD-004 — TARGET inalterado)

Este frontend é **brownfield**: código em `src/pages/` + `src/services/` com `vi.mock` nos testes de página. A migração para `src/features/` (AD-004 TARGET) **não** faz parte de R4 — não mover pastas nem refatorar estrutura.

- **MSW:** isolado em testes HTTP (`api.test.ts` via `createAuthMswServer()`); **não** há MSW global em `setup.ts`.
- **Playwright:** smoke de login é alvo R4 (T7–T8); script `npm run test:e2e` será adicionado com mock `page.route()` — sem backend real.
- **Vitest:** **184+** casos; page tests continuam mockando services, não MSW.

## Zona cinzenta — pare e pergunte antes de agir
- Adicionar dependência nova.
- Alterar chamada de API ou tipo compartilhado com o backend.
- Mudar rota pública ou regra de autenticação.
- Decidir onde mora código usado por mais de uma feature (candidato a `src/shared/`).
- Trocar TypeScript 6.0.3 → 7.x.
- Definir ou alterar `staleTime`/`cacheTime` do TanStack Query para dado sensível (saldo, status de pagamento, boleto).
- Regenerar tipos do OpenAPI quando o schema do backend mudar de versão major, ou quando um campo obrigatório virar opcional (ou vice-versa).
- Qualquer decisão que precise reformatar/reinterpretar o valor monetário (BigDecimal) fora da lib centralizada.
