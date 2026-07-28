# Diversos

Scripts, coleções de teste manual, utilitários e documentação operacional que **não** fazem parte direta do runtime `frontend/` + `backend/`.

| Pasta | Conteúdo |
|-------|----------|
| `scripts/` | Smoke tests shell (`test-api.sh`, `test-importacao.sh`, `sonar-*.sh`) |
| `sonarqube/` | Docker Compose do SonarQube local (análise estática) |
| `postman/` | Coleções e ambientes Postman |
| `bcrypt-generator/` | Utilitário Maven para gerar hashes bcrypt |
| `db/` | Notas/scripts complementares de banco |
| `relatorios/` | Conhecimento consolidado por domínio (organograma, folha, etc.) |

## SonarQube

```bash
./diversos/scripts/sonar-up.sh                              # sobe Docker
SONAR_USER=admin SONAR_PASSWORD='***' ./diversos/scripts/sonar-setup.sh  # bootstrap
./diversos/scripts/sonar-analyze.sh                         # testes + análise
```

Detalhes: [`sonarqube/README.md`](sonarqube/README.md)

Aplicação principal: `frontend/` (SPA) e `backend/` (API Spring Boot).
