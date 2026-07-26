# Quick Task 002 — Summary

**Completed:** 2026-06-20

## New layout

```text
sistema-folha/
├── frontend/       # React SPA (ex-frontend/)
├── backend/        # Spring Boot (ex-pom.xml + src/ na raiz)
├── diversos/    # scripts, postman, bcrypt-generator, db, relatorios
├── _docs/
├── Dockerfile
└── docker-compose.yml
```

## Comandos atualizados

| Ação | Comando |
|------|---------|
| Backend | `cd back && mvn spring-boot:run` |
| Frontend | `cd front && npm run dev` |
| Testes API | `./diversos/scripts/test-api.sh` |
| Postman | `diversos/postman/*.json` |
| Docker | `docker compose up --build` (raiz) |

## Notas

- `docker-compose.yml` mantém service name `frontend` (apenas label Docker); código está em `frontend/`.
- Specs em `_docs/specs/` atualizados para refletir novos paths.
- Decisão registrada como AD-002 em `STATE.md`.
