# Configuração do Banco de Dados

Este diretório contém as configurações do Docker para o banco de dados PostgreSQL do sistema.

## Configurações

- **Imagem**: postgres:15-alpine
- **Porta**: 5433 (mapeada para 5432 no container)
- **Banco de Dados**: sistema_folha
- **Usuário**: postgres
- **Senha**: postgres

## Como executar

Para iniciar o banco de dados:

```bash
cd db
docker-compose up -d
```

Para parar o banco de dados:

```bash
cd db
docker-compose down
```

Para remover os dados e iniciar do zero:

```bash
cd db
docker-compose down -v
docker-compose up -d
```

## Volumes

Os dados do PostgreSQL são persistidos em um volume Docker chamado `postgres_data`.

## Healthcheck

O container inclui um healthcheck que verifica se o PostgreSQL está pronto para aceitar conexões. O check é executado a cada 10 segundos, com timeout de 5 segundos e até 5 tentativas. 