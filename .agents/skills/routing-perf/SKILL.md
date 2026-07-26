---
name: routing-perf
description: Configuração do React Router v7 neste projeto, incluindo code splitting e lazy loading por rota e o orçamento de bundle. Use ao criar uma rota nova, ao adicionar uma feature acessível por URL, ou ao revisar por que o bundle cresceu — mesmo que o usuário só peça "adiciona uma tela nova" sem mencionar roteador, lazy ou bundle.
---

> **Status: TARGET (não obrigatório ainda)**  
> Fonte obrigatória atual: `_docs/specs/CONVENTIONS.md`, `STRUCTURE.md`, `TESTING.md` + código em `frontend/src/pages|services|...`.  
> Só aplique esta skill como obrigação quando o ROADMAP/AD liberar a adequação frontend correspondente.  
> Até lá: use esta skill como referência de destino, não como gate de PR.

## O que eu faço

Garanto que toda rota nova entra com code splitting desde o primeiro commit —
não como otimização depois.

## Regras

1. **Toda rota é `React.lazy` por padrão.** Nenhuma página é importada
   estaticamente no roteador raiz, exceto a rota inicial (evita esperar um
   chunk extra para a tela de entrada).

   ```ts
   const BoletosListPage = lazy(() => import('@/features/boletos/routes/boletos-list-page'))
   ```

2. **Rota fica dentro da feature, não no roteador central.** O roteador
   central só monta a árvore de `<Route>` e importa via `lazy`; o componente
   de página mora em `src/features/<feature>/routes/`.
3. **Orçamento de bundle: nenhum chunk de rota passa de 200KB gzip sem
   justificativa.** Ver `references/code-splitting.md` para como medir e o
   que fazer quando estourar.
4. **Rota pública vs. autenticada é decisão de Zona Cinzenta** — não crie
   nem mude guard de rota sem confirmar.

## Quando ler a referência

| Arquivo | Ler quando |
|---|---|
| `references/code-splitting.md` | Criar rota nova, ou quando o build acusar chunk grande |
