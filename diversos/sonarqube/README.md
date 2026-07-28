# SonarQube local

Servidor de análise estática para o monorepo `sistema-folha` (Java + TypeScript).

## Pré-requisitos

- Docker Desktop (ou Docker Engine + Compose)
- ~2 GB RAM livres para o container SonarQube

## Comandos rápidos

```bash
# 1. Subir SonarQube (cria volumes na primeira execução)
./diversos/scripts/sonar-up.sh

# 2. Bootstrap do projeto + token + MCP Cursor
SONAR_USER=admin SONAR_PASSWORD='***' ./diversos/scripts/sonar-setup.sh

# 3. Análise completa (testes + JaCoCo + scanner)
./diversos/scripts/sonar-analyze.sh
```

UI: http://localhost:9000 — projeto: `sistema-folha`

## Arquivos do projeto

| Arquivo | Função |
|---------|--------|
| `sonar-project.properties` (raiz) | Configuração do scanner (sources, exclusões, JaCoCo) |
| `.sonar.env.example` | Template de token/host (copiar para `.sonar.env`) |
| `.sonarlint/connectedMode.json` | Modo conectado do SonarLint no IDE |
| `diversos/scripts/sonar-*.sh` | Automação (subir, bootstrap, analisar) |

## MCP Cursor

O script `sonar-setup.sh` configura `~/.cursor/mcp.json` com o server `sonarqube`
(`sonarsource/sonarqube-mcp`), apontando para `http://host.docker.internal:9000`.

Reinicie o Cursor após o bootstrap para o MCP ficar disponível.

## Volumes Docker

Os volumes `sonarqube_*` são **externos** e compartilhados entre execuções.
Isso preserva projetos, tokens e histórico mesmo ao recriar o container.

```bash
# Parar sem perder dados
docker compose -f diversos/sonarqube/docker-compose.yml down

# CUIDADO: apaga TODOS os projetos/análises
# docker compose -f diversos/sonarqube/docker-compose.yml down -v
# docker volume rm sonarqube_data sonarqube_extensions sonarqube_logs sonarqube_temp
```
