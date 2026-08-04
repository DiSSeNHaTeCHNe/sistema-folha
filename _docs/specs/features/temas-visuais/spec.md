# Temas Visuais Selecionáveis — Specification

## Problem Statement

O frontend usa hoje um único tema MUI (azul `#1976d2` padrão, definido inline em
`main.tsx`) e o `index.css` ainda é o boilerplate do Vite — força `background-color: #242424`,
`body { display: flex }` e estiliza `button` globalmente, conflitando com o MUI.
Ao mesmo tempo, o backend já emite relatórios PDF com a identidade Techne
(`#7836FC` / `#3661FC`, AD-015), criando uma incoerência: o relatório sai na marca
da empresa, a interface não.

O estudo em `_docs/estudo-visual/` levantou quatro direções visuais. Esta feature
prepara o frontend para múltiplos temas e entrega as quatro, com o usuário
escolhendo pelo menu do avatar.

## Goals

- [ ] Zero cor fixa (hex/rgba) fora de `src/theme/` no código de aplicação — hoje são 35 ocorrências em 5 arquivos
- [ ] Quatro temas selecionáveis além do atual, todos renderizando Dashboard, Funcionários, Folha de Pagamento, Organograma e Login sem regressão visual
- [ ] Preferência persistida entre recarregamentos de página no mesmo navegador
- [ ] Regra de lint que barra reintrodução de cor fixa em `src/pages/` e `src/components/`

## Out of Scope

| Item | Razão |
| ---- | ----- |
| Persistência da preferência no backend (por usuário) | Decisão do usuário: `localStorage` apenas. Registrado como Deferred Idea no STATE.md. |
| Alternância automática por `prefers-color-scheme` | O seletor é explícito; automação seria comportamento não pedido. |
| Redesenho de layout, hierarquia de telas ou fluxo de navegação | Esta feature troca tokens visuais, não estrutura. |
| Tema aplicado aos PDFs de relatório | Os PDFs têm branding próprio server-side (AD-015). Alinhar cores é decisão separada. |
| Alteração da paleta de marca Techne | As cores vêm do site institucional; mudança de marca é decisão externa. |
| Migração para Tailwind / stack TARGET do `frontend/AGENTS.md` | AD-004: skills FE são TARGET até liberação no ROADMAP. Stack real permanece MUI v7. |

---

## Assumptions & Open Questions

| Assumption / decisão | Escolha | Rationale | Confirmado? |
| --- | --- | --- | --- |
| Onde persistir a preferência | `localStorage`, chave `sistema-folha:tema` | Escopo mínimo, sem migration nem endpoint | y |
| Tema padrão ao final | `techne` | Coerência com o branding dos PDFs (AD-015) | y |
| Onde fica o seletor | Menu do avatar no `AppBar`, abrindo dialog | Reusa o padrão de `AlterarSenhaDialog` | y |
| Quando tokenizar Dashboard e Organograma | Fase 1, integralmente | Fases 2-5 viram só tokens de cor, baratas e seguras | y |
| Paleta de gráficos (Recharts) | Campo custom no tema via `declare module '@mui/material/styles'` | Recharts recebe cores por prop; sem isso os gráficos ignoram o tema | n — decisão técnica do agente |
| Comportamento com `localStorage` indisponível | Cai no tema padrão, sem erro visível | Modo privado, iframe restrito e SSR não podem quebrar a aplicação | n — decisão técnica do agente |
| Valor inválido/desconhecido em `localStorage` | Cai no tema padrão | Impede que dado corrompido ou tema removido trave o boot | n — decisão técnica do agente |
| Fonte Poppins no tema Techne | `@fontsource/poppins`, subset latin, pesos 400/500/600 | Evita dependência de rede externa em produção | n — decisão técnica do agente |
| Migração de usuários já logados | Sem `localStorage` gravado ⇒ tema padrão | Não há como distinguir "nunca escolheu" de "escolheu o padrão" | n — decisão técnica do agente |

**Open questions:** nenhuma — todas resolvidas ou registradas acima.

---

## Implicit-Requirement Dimensions Sweep

Escopo Large. Cada dimensão resolve em requisito ou `N/A` justificado.

| Dimensão | Resolução |
| --- | --- |
| Validação de entrada e limites | TEMA-04 — id lido do `localStorage` validado contra a lista conhecida |
| Falha / falha parcial | TEMA-05 — `localStorage` indisponível ou com erro não quebra o boot |
| Idempotência / retry / duplicata | N/A — trocar de tema é operação idempotente sem efeito remoto |
| Fronteiras de autorização e rate limit | N/A — preferência local, sem chamada de API e sem dado sensível |
| Concorrência / ordenação | N/A — estado local de uma única aba; sincronizar entre abas está fora de escopo |
| Ciclo de vida / expiração do dado | TEMA-06 — a preferência não expira; limpar o `localStorage` volta ao padrão |
| Observabilidade | N/A — sem backend envolvido; falha de storage é silenciosa por decisão |
| Falha de dependência externa | TEMA-16 — fonte Poppins empacotada localmente, sem depender de CDN |
| Integridade de transição de estado | TEMA-07 — troca de tema é atômica: ou aplica inteiro ou mantém o anterior |

---

## User Stories

### P1: Base tokenizada e tema atual preservado ⭐ MVP

**User Story**: Como desenvolvedor do sistema, quero que toda cor venha do tema MUI,
para que trocar de tema mude a aparência de todas as telas sem editar componentes.

**Why P1**: Sem isso, qualquer tema novo quebra em Dashboard e Organograma. É a
fundação das quatro fases seguintes.

**Acceptance Criteria**:

1. WHEN o build roda THEN o sistema SHALL não conter nenhum literal de cor (`#rrggbb`, `#rgb`, `rgba(`, `hsl(`) em `src/pages/` ou `src/components/`
2. WHEN `npm run lint` roda com cor fixa introduzida em `src/pages/` ou `src/components/` THEN o ESLint SHALL falhar com exit code diferente de 0 apontando a linha
3. WHEN o usuário abre o Dashboard com o tema `classico` THEN o sistema SHALL renderizar as mesmas cores de KPI, gráficos e listas que a versão anterior à tokenização
4. WHEN o Dashboard monta um gráfico Recharts THEN o sistema SHALL obter a sequência de cores de `theme.palette.charts`, não de um array literal no componente
5. WHEN o Organograma renderiza nós, arestas e minimap THEN o sistema SHALL usar `theme.palette.primary.main` no lugar do `#1976d2` fixo
6. WHEN a aplicação carrega THEN o `index.css` SHALL conter apenas regras de reset estrutural, sem nenhuma declaração de cor

**Independent Test**: rodar `npm run test`, `npm run lint` e `npm run build` com apenas o tema `classico` registrado; abrir Dashboard e Organograma e comparar com a captura anterior.

---

### P1: Seletor de tema no menu do avatar ⭐ MVP

**User Story**: Como usuário do sistema, quero escolher o tema visual pelo menu do meu
perfil, para que a interface fique do jeito que prefiro.

**Why P1**: É a única forma do usuário exercer a escolha. Sem o seletor, os temas
existem mas ninguém alcança.

**Acceptance Criteria**:

1. WHEN o usuário abre o menu do avatar THEN o sistema SHALL exibir o item "Aparência" acima de "Alterar senha"
2. WHEN o usuário clica em "Aparência" THEN o sistema SHALL abrir um dialog listando todos os temas registrados, cada um com nome, descrição e as amostras de cor
3. WHEN o dialog abre THEN o sistema SHALL marcar visualmente o tema ativo e anunciá-lo via `aria-checked="true"`
4. WHEN o usuário seleciona um tema THEN o sistema SHALL aplicar as cores imediatamente, sem recarregar a página
5. WHEN o usuário seleciona um tema e recarrega a página THEN o sistema SHALL reabrir com o tema escolhido
6. WHEN o usuário fecha o dialog sem selecionar THEN o sistema SHALL manter o tema que estava ativo

**Independent Test**: com dois temas registrados, alternar entre eles pelo dialog, recarregar e confirmar a persistência.

---

### P1: Tema Corporate slate

**User Story**: Como usuário, quero o tema Corporate slate, para ter uma interface densa
e sóbria adequada ao uso diário em tabelas longas.

**Why P1**: Primeira validação real de que a base tokenizada funciona — é o tema mais
próximo do atual, então diferenças revelam tokens faltando.

**Acceptance Criteria**:

1. WHEN o tema `corporate` está ativo THEN o sistema SHALL aplicar `#3B82F6` como `palette.primary.main` e `#0F172A` na sidebar e no `AppBar`
2. WHEN qualquer tela renderiza com `corporate` THEN todo texto sobre superfície SHALL ter contraste mínimo 4.5:1 (WCAG AA)
3. WHEN o Dashboard renderiza com `corporate` THEN os gráficos SHALL usar a paleta `charts` do tema
4. WHEN o tema `corporate` é selecionado THEN o sistema SHALL renderizar Login, Dashboard, Funcionários, Folha de Pagamento e Organograma sem elemento ilegível ou invisível

**Independent Test**: selecionar o tema e percorrer as cinco telas; rodar o teste de contraste sobre os tokens do tema.

---

### P2: Tema Soft neutral

**User Story**: Como usuário, quero o tema Soft neutral, para uma interface mais arejada
e de leitura confortável.

**Why P2**: Não é MVP — é diferenciação estética. Depende da mesma base do P1.

**Acceptance Criteria**:

1. WHEN o tema `soft` está ativo THEN o sistema SHALL aplicar `#1D9E75` como `palette.primary.main` e `#F4F2EC` na sidebar
2. WHEN qualquer tela renderiza com `soft` THEN todo texto sobre superfície SHALL ter contraste mínimo 4.5:1 (WCAG AA)
3. WHEN o tema `soft` é selecionado THEN o sistema SHALL renderizar as cinco telas sem elemento ilegível ou invisível

**Independent Test**: mesmo percurso do Corporate slate.

---

### P2: Tema Indigo dark

**User Story**: Como usuário, quero um tema escuro, para reduzir o cansaço visual em uso
prolongado e em ambientes de pouca luz.

**Why P2**: É o tema de maior risco — `palette.mode: 'dark'` inverte superfícies e expõe
qualquer cor fixa remanescente. Vem depois dos claros de propósito.

**Acceptance Criteria**:

1. WHEN o tema `indigo` está ativo THEN o sistema SHALL definir `palette.mode: 'dark'` e aplicar `#7F77DD` como `palette.primary.main`
2. WHEN qualquer tela renderiza com `indigo` THEN todo texto sobre superfície SHALL ter contraste mínimo 4.5:1 (WCAG AA)
3. WHEN o Dashboard renderiza com `indigo` THEN nenhum card, avatar ou gráfico SHALL apresentar fundo claro com texto claro
4. WHEN o Organograma renderiza com `indigo` THEN os nós, arestas e o minimap SHALL permanecer distinguíveis do fundo escuro
5. WHEN o tema `indigo` é selecionado THEN o sistema SHALL renderizar as cinco telas sem elemento ilegível ou invisível

**Independent Test**: selecionar o tema escuro e percorrer as cinco telas procurando superfícies claras remanescentes.

---

### P1: Tema Techne brand e adoção como padrão

**User Story**: Como empresa, quero que o sistema use a identidade visual da Techne por
padrão, para que a interface seja coerente com os relatórios PDF e com o portfólio.

**Why P1**: Fecha a incoerência entre relatório e interface. Passa a ser o padrão, então
afeta todo usuário sem preferência gravada.

**Acceptance Criteria**:

1. WHEN o tema `techne` está ativo THEN o sistema SHALL aplicar `#7836FC` em `palette.primary.main`, o mesmo valor de `relatorios.branding.primary-color` no `application.yml`
2. WHEN o tema `techne` está ativo THEN o sistema SHALL aplicar `#20284E` na sidebar e no `AppBar` e `#EFF2F7` em `palette.background.default`
3. WHEN o tema `techne` está ativo THEN a tipografia SHALL ser Poppins, carregada localmente via `@fontsource/poppins`, sem requisição a domínio externo
4. WHEN qualquer tela renderiza com `techne` THEN todo texto sobre superfície SHALL ter contraste mínimo 4.5:1 (WCAG AA)
5. WHEN um usuário sem preferência gravada abre o sistema THEN o sistema SHALL aplicar o tema `techne`
6. WHEN um usuário com preferência gravada abre o sistema THEN o sistema SHALL respeitar a preferência gravada, e não o padrão

**Independent Test**: limpar o `localStorage`, recarregar e confirmar Techne; gravar outro tema, recarregar e confirmar que a escolha prevalece.

---

## Edge Cases

- WHEN `localStorage` lança exceção na leitura ou na escrita THEN o sistema SHALL usar o tema padrão e continuar funcionando, sem erro visível ao usuário
- WHEN `localStorage` contém um id de tema desconhecido ou valor não-string THEN o sistema SHALL usar o tema padrão
- WHEN `localStorage` contém string vazia THEN o sistema SHALL usar o tema padrão
- WHEN o dialog de aparência abre em viewport móvel THEN o sistema SHALL manter todos os temas alcançáveis por rolagem, sem corte
- WHEN o usuário navega o dialog apenas por teclado THEN o sistema SHALL permitir selecionar um tema com Tab e Enter/Espaço
- WHEN o tema muda THEN o sistema SHALL preservar a rota atual, o estado dos formulários abertos e o conteúdo do `Outlet`

---

## Requirement Traceability

| ID | Story | Fase | Status |
| --- | --- | --- | --- |
| TEMA-01 | P1: Base tokenizada | 1 | Pending |
| TEMA-02 | P1: Base tokenizada — paleta de gráficos no tema | 1 | Pending |
| TEMA-03 | P1: Base tokenizada — lint anti-cor-fixa | 1 | Pending |
| TEMA-04 | P1: Seletor — validação do id persistido | 1 | Pending |
| TEMA-05 | P1: Seletor — resiliência a `localStorage` indisponível | 1 | Pending |
| TEMA-06 | P1: Seletor — persistência entre recarregamentos | 1 | Pending |
| TEMA-07 | P1: Seletor — troca atômica sem recarregar | 1 | Pending |
| TEMA-08 | P1: Seletor — item "Aparência" no menu do avatar | 1 | Pending |
| TEMA-09 | P1: Seletor — dialog com amostras e a11y | 1 | Pending |
| TEMA-10 | P1: Base tokenizada — `index.css` sem cor | 1 | Pending |
| TEMA-11 | P1: Corporate slate | 2 | Pending |
| TEMA-12 | P2: Soft neutral | 3 | Pending |
| TEMA-13 | P2: Indigo dark | 4 | Pending |
| TEMA-14 | P2: Indigo dark — Organograma legível no escuro | 4 | Pending |
| TEMA-15 | P1: Techne brand | 5 | Pending |
| TEMA-16 | P1: Techne brand — Poppins local | 5 | Pending |
| TEMA-17 | P1: Techne brand — adoção como padrão | 5 | Pending |
| TEMA-18 | Todas — contraste WCAG AA por tema | 2,3,4,5 | Pending |

**Coverage:** 18 requisitos, 18 mapeados para tasks, 0 sem mapeamento.

---

## Success Criteria

- [ ] `grep -rE "#[0-9a-fA-F]{3,6}\b|rgba?\(" frontend/src/pages frontend/src/components` retorna zero ocorrências
- [ ] `npm run lint`, `npm run test` e `npm run build` passam em verde ao final de cada fase
- [ ] Cinco temas selecionáveis (`classico`, `corporate`, `soft`, `indigo`, `techne`), cada um verificado nas cinco telas principais
- [ ] Teste automatizado de contraste cobre todos os pares texto/superfície de todos os temas
- [ ] Cobertura de testes do frontend não regride em relação ao baseline registrado antes da Fase 1
