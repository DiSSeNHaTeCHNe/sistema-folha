# Quick Task 004: Fix TS build Funcionarios

**Date:** 2026-06-20
**Status:** Done

## Description

Corrigir `npm run build` que falhava em `getApiErrorMessage` por uso de `axios.isAxiosError` incompatível com tipagem ESM do projeto.

## Files Changed

- `frontend/src/pages/Funcionarios/index.tsx` — type guard em `error.response` sem import axios

## Verification

- [x] `cd frontend && npm run build` — exit 0

## Commit

Pending.
