# Quick Task 002: Organizar pastas do projeto

**Date:** 2026-06-20
**Status:** Done

## Description

Reorganizar o repositório em `front/` (SPA), `back/` (API Spring Boot) e `diversos/` (scripts, Postman, utilitários e relatórios auxiliares).

## Files Changed

- `frontend/` → `front/` (rename)
- `src/`, `pom.xml` → `back/` (move)
- `scripts/`, `postman/`, `bcrypt-generator/`, `db/`, `relatorios/` → `diversos/` (move)
- `Dockerfile`, `.gitignore`, `README.md` — paths atualizados
- `_docs/specs/STRUCTURE.md` + demais specs — paths atualizados
- `diversos/README.md` — índice da pasta auxiliar

## Verification

- [x] `back/pom.xml` + `back/src/` presentes
- [x] `front/package.json` + `front/src/` presentes
- [x] Auxiliares em `diversos/{scripts,postman,bcrypt-generator,db,relatorios}`
- [x] `mvn compile` OK em `back/`
- [x] `npm run build` OK em `front/`
- [x] Dockerfile referencia `front/` e `back/`

## Commit

Pending — user did not request commit.
