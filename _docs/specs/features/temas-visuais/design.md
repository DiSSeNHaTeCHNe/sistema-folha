# Temas Visuais — Design

**Spec**: `_docs/specs/features/temas-visuais/spec.md`
**Context**: `_docs/specs/features/temas-visuais/context.md`
**Escopo**: Large — múltiplos componentes, novo padrão de tokens, 5 fases.

---

## Princípio central

O componente nunca conhece uma cor. Ele conhece um **papel** (`primary`, `success`,
`background.paper`, `charts[i]`). O tema resolve papel em cor. Isso é o que torna as
Fases 2-5 baratas: um tema novo é um objeto de tokens, não uma varredura de telas.

Consequência prática: a Fase 1 é a fase difícil e as outras quatro são declarativas.

---

## Arquitetura

```
main.tsx
  └── AppThemeProvider              (contexts/ThemeContext.tsx)
        ├── lê/grava localStorage    (theme/storage.ts)
        ├── criarTema(temaId)        (theme/themes.ts)
        └── <ThemeProvider> + <CssBaseline>
              └── RouterWithAuth
                    └── Layout
                          ├── AppBar → menu avatar → "Aparência"
                          │     └── AparenciaDialog  (components/AparenciaDialog/)
                          └── Outlet → páginas (só consomem useTheme/sx)
```

### Arquivos

| Arquivo | Papel | Fase |
| --- | --- | --- |
| `src/theme/tokens.ts` | Tipo `TokensTema` + `montarTema(tokens)` — fábrica única de temas | 1 |
| `src/theme/themes.ts` | Registro `TEMAS`, `TEMA_IDS`, `TEMA_PADRAO`, `criarTema`, `isTemaId` | 1 |
| `src/theme/storage.ts` | `lerTemaSalvo()` / `gravarTema(id)` com try/catch | 1 |
| `src/theme/augment.d.ts` | `declare module '@mui/material/styles'` — adiciona `palette.charts` e `palette.chrome` | 1 |
| `src/contexts/ThemeContext.tsx` | `AppThemeProvider` + hook `useAppTheme` | 1 |
| `src/components/AparenciaDialog/index.tsx` | Dialog de seleção com amostras | 1 |
| `src/theme.ts` | **Remover** — tema órfão, ninguém importa | 1 |

---

## Extensão da paleta MUI

O MUI não tem slot para sequência de cores de gráfico nem para o "chrome" (sidebar e
AppBar, que não seguem `background.paper`). Ambos são adicionados por augmentation de
módulo, o caminho oficial e type-safe:

```ts
// src/theme/augment.d.ts
declare module '@mui/material/styles' {
  interface Palette {
    charts: string[];
    chrome: { bg: string; fg: string; fgAtivo: string; selecionado: string };
  }
  interface PaletteOptions {
    charts?: string[];
    chrome?: { bg: string; fg: string; fgAtivo: string; selecionado: string };
  }
}
```

`palette.charts` resolve as 8 cores hardcoded em `pieColors` no Dashboard.
`palette.chrome` resolve a sidebar escura sobre conteúdo claro — combinação que
`background.paper` sozinho não expressa.

---

## Fábrica de temas

Um tema = um objeto `TokensTema` + `montarTema`. Os overrides de componente
(`MuiDrawer`, `MuiAppBar`, `MuiCard`, `MuiTableCell`, `MuiListItemButton`) ficam na
fábrica, escritos uma vez. Isso é o que faz um tema novo custar ~20 linhas.

O tema `classico` é a exceção: reproduz exatamente o `createTheme` inline de hoje,
sem passar pela fábrica, para garantir zero regressão visual na Fase 1.

```ts
export function criarTema(id: TemaId): Theme {
  return getTemaDefinicao(id).criar();
}
```

`TemaDefinicao` carrega o que o dialog precisa exibir — `id`, `nome`, `descricao`,
`amostras` — evitando que o componente de UI conheça cores.

---

## Persistência

```ts
const CHAVE = 'sistema-folha:tema';

export function lerTemaSalvo(): TemaId {
  try {
    const v = window.localStorage.getItem(CHAVE);
    return isTemaId(v) ? v : TEMA_PADRAO;
  } catch {
    return TEMA_PADRAO;   // modo privado, iframe restrito, SSR
  }
}
```

`isTemaId` é o type guard que fecha TEMA-04: qualquer valor fora de `TEMA_IDS` — nulo,
string vazia, id de tema removido, dado corrompido — cai no padrão. O `try/catch`
fecha TEMA-05. Nenhum dos dois caminhos loga ou alerta: por decisão, a falha é
silenciosa (spec, dimensão Observabilidade).

---

## Estratégia de tokenização (Fase 1)

Ordem deliberada: **primeiro o mapeamento, depois a substituição**. Trocar cor por
token sem antes decidir o mapa produz escolhas inconsistentes entre arquivos.

| Cor fixa hoje | Papel | Token de destino |
| --- | --- | --- |
| `#f8f9fa` (fundo de página) | superfície da página | `palette.background.default` |
| `#e9ecef` (borda de card) | divisor | `palette.divider` |
| `#e3f2fd` / `#1976d2` (avatar) | acento informativo | `palette.info.light` / `.main` |
| `#e8f5e8` / `#2e7d32` (avatar) | acento de sucesso | `palette.success.light` / `.main` |
| `#fff3e0` / `#f57c00` (avatar) | acento de alerta | `palette.warning.light` / `.main` |
| `#ffebee` / `#c62828` (avatar) | acento de erro | `palette.error.light` / `.main` |
| `pieColors[8]` | sequência categórica | `palette.charts` |
| `#4F46E5` (AreaChart) | acento primário | `palette.primary.main` |
| `#1976d2` (Organograma, 6×) | acento primário | `palette.primary.main` |
| `rgba(0,0,0,0.1)` (sombra) | elevação | `theme.shadows[n]` |
| `#e0e0e0` (borda, Funcionários) | divisor | `palette.divider` |

Recharts recebe cor por prop, não por CSS — daí `useTheme()` no componente e passagem
explícita de `theme.palette.charts[i]`.

---

## Lint anti-regressão (TEMA-03)

Sem isso, a próxima tela escrita reintroduz cor fixa e a dívida volta. Regra
`no-restricted-syntax` no `.eslintrc.json`, escopada a `src/pages/` e
`src/components/`:

```jsonc
{
  "files": ["src/pages/**/*.tsx", "src/components/**/*.tsx"],
  "rules": {
    "no-restricted-syntax": ["error", {
      "selector": "Literal[value=/#[0-9a-fA-F]{3,8}\\b|rgba?\\(|hsla?\\(/]",
      "message": "Cor fixa proibida fora de src/theme/. Use o token do tema (palette.*) ou useTheme()."
    }]
  }
}
```

`src/theme/**` fica de fora — é o único lugar onde cor literal é legítima.

---

## Teste de contraste (TEMA-18)

O critério "contraste ≥ 4.5:1" precisa ser executável, senão vira inspeção manual e
não sobrevive à quarta fase. Helper `src/theme/contraste.ts` implementando a fórmula
de luminância relativa da WCAG 2.1, e um teste parametrizado que percorre
`TEMAS × pares` — cada tema novo entra automaticamente na varredura ao ser
registrado.

Pares verificados por tema: `text.primary`/`background.default`,
`text.primary`/`background.paper`, `text.secondary`/`background.paper`,
`chrome.fg`/`chrome.bg`, `chrome.fgAtivo`/`chrome.selecionado`, e
`primary.contrastText`/`primary.main`.

---

## Impacto no harness de teste

`src/test/renderWithProviders.tsx` envolve hoje `MemoryRouter` + mock de auth. Precisa
envolver também `ThemeProvider`, senão todo componente que chamar `useTheme()` quebra
nos 33 arquivos de teste existentes. Assinatura ganha `temaId?: TemaId` (default
`TEMA_PADRAO`), o que também permite testar uma tela sob um tema específico.

Essa mudança é feita **antes** da tokenização (T3 antes de T5-T7), para que a suíte
continue verde durante a substituição de cores.

---

## Decisões de design registradas

| # | Decisão | Alternativa descartada | Razão |
| --- | --- | --- | --- |
| DD-1 | `palette.charts` por augmentation de módulo | Constante exportada de `theme/` | Constante não muda por tema — anularia o objetivo |
| DD-2 | `palette.chrome` como slot próprio | Reusar `background.paper` na sidebar | Sidebar escura sobre conteúdo claro não é expressável com um slot só |
| DD-3 | `classico` fora da fábrica | Reconstruir `classico` com `montarTema` | Reconstruir arrisca regressão visual justamente na referência de comparação |
| DD-4 | Lint por `no-restricted-syntax` | Plugin externo de design tokens | Zero dependência nova; a regra cabe em 5 linhas |
| DD-5 | Contraste como teste unitário sobre tokens | Verificação visual manual ou axe-core em e2e | Roda em milissegundos e cobre todo tema novo automaticamente |
| DD-6 | Poppins via `@fontsource/poppins` | `<link>` para Google Fonts | Evita dependência de rede externa em produção (TEMA-16) |
| DD-7 | Remover `src/theme.ts` | Manter como fallback | Arquivo órfão que ninguém importa; manter cria ambiguidade sobre a fonte de verdade |

---

## Riscos

| Risco | Mitigação |
| --- | --- |
| Tokenização do Dashboard muda a aparência do tema atual | T5 exige comparação com captura anterior; `classico` preservado fora da fábrica (DD-3) |
| Indigo dark expõe cor fixa esquecida | Lint (TEMA-03) fecha a porta na Fase 1; Fase 4 percorre as 5 telas com checklist |
| Poppins altera métricas de texto e quebra layout denso | Fase 5 verifica Folha de Pagamento, a tela de maior densidade, antes de fechar |
| `renderWithProviders` alterado quebra os 33 testes existentes | T3 é isolada e roda a suíte inteira como gate |
| Cor da marca diverge do manual oficial | Spec ancora `#7836FC` no valor já em produção no `application.yml` (AD-015), não no site |
