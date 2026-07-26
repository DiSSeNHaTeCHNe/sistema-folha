---
name: testing-a11y
description: Como escrever teste unitário (Vitest + Testing Library) e E2E (Playwright) neste projeto, sempre consultando por papel/acessibilidade, e como mockar HTTP com MSW usando os mesmos tipos gerados do OpenAPI. Use ao escrever ou revisar qualquer teste, ou ao implementar um componente/feature nova mesmo que o usuário não peça teste explicitamente — todo componente novo precisa de teste antes de ser considerado pronto.
---

> **Status: TARGET (não obrigatório ainda)**  
> Fonte obrigatória atual: `_docs/specs/CONVENTIONS.md`, `STRUCTURE.md`, `TESTING.md` + código em `frontend/src/pages|services|...`.  
> Só aplique esta skill como obrigação quando o ROADMAP/AD liberar a adequação frontend correspondente.  
> Até lá: use esta skill como referência de destino, não como gate de PR.

## O que eu faço

Garanto que todo teste deste projeto consulta o DOM como um usuário real
consultaria (papel, label, texto visível) e que o mock de HTTP não diverge do
contrato real da API.

## Regras

1. **Sempre `getByRole`/`getByLabelText`/`getByText`. Nunca `data-testid` nem
   seletor de classe CSS.** Se um elemento não é alcançável por papel, o
   problema é de acessibilidade do componente (skill
   `component-architecture`), não do teste.
2. **Teste comportamento visível ao usuário, não implementação.** Não testar
   se um `useState` mudou — testar o que aparece na tela depois da interação.
3. **MSW mocka a rede, não a função.** Nenhum teste faz `vi.mock` do hook de
   dados (`use-boletos-list.ts`); o mock fica no nível de `fetch`, via
   handlers do MSW, para que o teste exercite o hook real. Ver
   `references/msw-handlers.md`.
4. **E2E (Playwright) cobre o fluxo completo de uma feature crítica**
   (ex.: criar boleto do zero até confirmação), não cada variação de UI —
   variação fica no teste unitário. Ver `references/playwright-e2e.md`.
5. Componente novo sem teste = feature incompleta, mesmo que não pedido
   explicitamente.

## Quando ler cada referência

| Arquivo | Ler quando |
|---|---|
| `references/msw-handlers.md` | Escrever teste que envolve chamada de API |
| `references/playwright-e2e.md` | Escrever ou revisar teste E2E |
