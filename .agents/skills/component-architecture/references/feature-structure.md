# Estrutura por feature — exemplo completo

```
src/
  app/                       # bootstrap: providers (QueryClient, Router), main.tsx
  shared/                    # só código usado por 2+ features (ver regra abaixo)
    ui/                      # componentes puramente visuais reusáveis (Button, Input base)
    lib/
  lib/
    api/
      generated/schema.d.ts  # gerado do OpenAPI, não editar à mão
      http-error.ts
      page.ts
      client.ts              # fetch wrapper
  features/
    boletos/
      routes/
        boletos-list-page.tsx
        boleto-detail-page.tsx
      components/
        boleto-status-badge.tsx   # só usado dentro de boletos
      api/
        use-boletos-list.ts
        use-criar-boleto.ts
      schemas/
        criar-boleto.schema.ts
    autenticacao/
      routes/
      api/
      schemas/
```

## Regra para `src/shared/`

Um componente/hook só entra em `shared/` quando **já** é usado por 2+
features — não antecipe. Se hoje só `boletos` usa `StatusBadge`, ele mora em
`features/boletos/components/`. Movê-lo para `shared/` quando `autenticacao`
também precisar é refactor de 1 linha de import, não custo real.

Mover algo para `shared/` é item de Zona Cinzenta no AGENTS.md — avise antes,
porque muda o "dono" conceitual do código.

## Rotas

Cada feature com rota própria expõe seus componentes de página em
`routes/`, que são o que o roteador (skill `routing-perf`) importa via
`React.lazy`. O resto da feature (`components/`, `api/`, `schemas/`) nunca é
importado direto de outra feature — só via `shared/` se promovido.
