# Quick Task 006: Organograma no submenu Cadastros

**Date:** 2026-07-27  
**Status:** Done

## Description

Mover o item de menu lateral **Organograma** do nível raiz para dentro do submenu **Cadastros**. A rota `/organograma` permanece inalterada.

## Acceptance

- [x] "Organograma" não aparece mais entre os itens de topo do drawer
- [x] "Organograma" aparece no Collapse de Cadastros
- [x] Navegação para `/organograma` continua funcional via o item movido

## Files Changed

- `frontend/src/components/Layout/index.tsx` — item movido de `menuItems` para `cadastroItems`

## Verification

- [x] Grep: `Organograma` só em `cadastroItems`, não em `menuItems`
- [x] Rota `/organograma` inalterada em `frontend/src/routes/index.tsx`
