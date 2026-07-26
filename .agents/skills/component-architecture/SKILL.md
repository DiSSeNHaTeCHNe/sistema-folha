---
name: component-architecture
description: Como estruturar pastas por feature, quando (e quando não) usar composição, memo/useMemo/useCallback, e a checklist de acessibilidade obrigatória em todo componente. Use ao criar qualquer componente ou pasta nova, ao revisar um PR de UI, ou quando o usuário pedir para "organizar"/"refatorar" código de tela — mesmo sem mencionar "arquitetura", "estrutura" ou "acessibilidade" diretamente.
---

> **Status: TARGET (não obrigatório ainda)**  
> Fonte obrigatória atual: `_docs/specs/CONVENTIONS.md`, `STRUCTURE.md`, `TESTING.md` + código em `frontend/src/pages|services|...`.  
> Só aplique esta skill como obrigação quando o ROADMAP/AD liberar a adequação frontend correspondente.  
> Até lá: use esta skill como referência de destino, não como gate de PR.

## O que eu faço

Evito as duas armadilhas mais comuns em projetos React que crescem rápido:
estrutura por tipo de arquivo (`components/`, `hooks/` genéricos que viram
lixeira) e otimização prematura (`memo` em tudo "por garantia").

## Regras

1. **Estrutura por feature, sempre.**

   ```
   src/features/boletos/
     components/       # só usados dentro desta feature
     api/               # hooks de dados desta feature (usa api-client)
     schemas/           # Zod schemas desta feature
     routes/             # se a feature tem rota própria
   src/shared/           # só o que é usado por 2+ features — ver Zona Cinzenta
   ```

   Ver `references/feature-structure.md` para o exemplo completo.

2. **Composição em vez de prop drilling.** Se um componente está repassando
   uma prop 3+ níveis sem usá-la, é sinal de `children`/slot, não de mais
   props. Context só quando composição não resolve (estado realmente global
   à árvore).

3. **`memo`/`useMemo`/`useCallback` só com motivo medido.** Antes de usar,
   o comentário no código precisa dizer *qual* re-render está sendo evitado
   (ex.: `// evita recriar array em cada render, ProdutoList tem 500+ itens`).
   Sem esse comentário, não use — é regra do AGENTS.md, não sugestão.

4. **Padrão de projeto só quando resolve um problema presente.** Não crie
   factory, strategy, HOC etc. antecipando um caso hipotético.

5. **Acessibilidade é obrigatória, não opcional.** Ver
   `references/accessibility-checklist.md` em todo componente interativo
   (formulário, modal, menu, tabela com ação).

## Quando ler cada referência

| Arquivo | Ler quando |
|---|---|
| `references/feature-structure.md` | Criar a primeira pasta de uma feature nova, ou decidir se algo vai para `shared/` |
| `references/accessibility-checklist.md` | Criar componente interativo (form, modal, menu, tabela com ação, dropdown) |
