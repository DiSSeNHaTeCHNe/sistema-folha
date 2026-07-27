# Quick Task 005: Importação no submenu Cadastros

**Date:** 2026-07-27  
**Status:** Done

## Description

Mover o item de menu lateral **Importação** do nível raiz para dentro do submenu **Cadastros**. A rota `/importacao` permanece inalterada.

## Acceptance

- [x] "Importação" não aparece mais entre os itens de topo do drawer
- [x] "Importação" aparece no Collapse de Cadastros (com os demais cadastros)
- [x] Navegação para `/importacao` continua funcional via o item movido

## Files Changed

- `frontend/src/components/Layout/index.tsx` — item movido de `menuItems` para `cadastroItems`

## Verification

- [x] Grep: `Importação` só em `cadastroItems`, não em `menuItems`
- [x] Rota `/importacao` inalterada em `frontend/src/routes/index.tsx`
