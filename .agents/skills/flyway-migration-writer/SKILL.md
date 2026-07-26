---
name: flyway-migration-writer
description: >-
  Cria migração Flyway versionada: nomeia arquivo, escreve DDL/DML idempotente,
  sincroniza entity JPA e valida com flyway migrate. Use ao alterar schema de
  banco (tabela, coluna, índice, constraint) em projetos Java com Flyway.
---

# Flyway — Migration Writer

Workflow genérico para mudanças de schema com Flyway em projetos Spring Boot + JPA.

---

## Antes de codar — calibrar no projeto

1. Ler `AGENTS.md` ou doc de banco do repositório.
2. Localizar pasta de migrations (comum):
   - `src/main/resources/db/migration/`
   - `src/main/resources/flyway/migration/`
3. **Listar arquivos existentes** e identificar próximo número de versão.
4. Confirmar dialeto (PostgreSQL, MySQL, H2) e naming convention do projeto.
5. Verificar se `ddl-auto` está ativo — mesmo assim, **schema versionado vai no Flyway**.

---

## Naming

Formato padrão Flyway:

```text
V{version}__{descricao_snake_case}.sql
```

Exemplos: `V1__init.sql`, `V2__add_users_table.sql`, `V1.12__add_status_column.sql`.

Regras:
- Versão **sempre crescente** — nunca reutilizar número existente.
- Descrição curta, snake_case, verbo + objeto (`add_`, `create_`, `drop_`, `rename_`).
- Migration **já aplicada em ambiente compartilhado não se edita** — crie nova versão corretiva.

---

## Checklist

```text
- [ ] Listar migrations existentes → próximo V{n}
- [ ] Escrever SQL idempotente quando possível
- [ ] Migration de limpeza de dados ANTES de constraint/index (se necessário)
- [ ] Atualizar entity JPA correspondente
- [ ] Atualizar repository/DTO/service se impactados
- [ ] Executar flyway migrate
- [ ] Validar boot da aplicação / testes
```

---

## SQL — padrões por dialeto

### PostgreSQL (preferir idempotência)

```sql
-- Nova coluna
ALTER TABLE items ADD COLUMN IF NOT EXISTS status VARCHAR(50);

-- Nova tabela
CREATE TABLE IF NOT EXISTS items (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índice
CREATE INDEX IF NOT EXISTS idx_items_name ON items (name);

-- Unique parcial
CREATE UNIQUE INDEX IF NOT EXISTS idx_items_name_active
ON items (name) WHERE deleted_at IS NULL;

-- Remover antes de recriar
ALTER TABLE items DROP CONSTRAINT IF EXISTS items_name_key;
DROP INDEX IF EXISTS idx_items_name;
```

### MySQL (idempotência limitada — testar localmente)

```sql
-- Preferir checar information_schema ou usar flyway callbacks do projeto
ALTER TABLE items ADD COLUMN status VARCHAR(50);
```

### DML de correção

Separe limpeza de dados em migration própria **antes** de adicionar constraint:

```sql
-- V{n}__dedupe_items.sql
DELETE FROM items a
USING items b
WHERE a.id > b.id AND a.external_id = b.external_id;
```

Documente o **porquê** em comentário SQL.

---

## Sincronizar JPA entity

Após DDL, alinhar entity ao schema:

```java
@Column(name = "status", nullable = false, length = 50)
private String status;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = false)
private Category category;
```

Checklist:
- [ ] `@Column(name)` = nome físico da coluna
- [ ] `nullable`, `length`, tipos coerentes com DDL
- [ ] FKs com `@JoinColumn(name = "..._id")`
- [ ] Enum: `@Enumerated` ou converter — seguir padrão do projeto

---

## Executar migration

Detectar build tool e plugin Flyway do projeto:

| Build | Comando típico |
|-------|--------------|
| Maven | `mvn flyway:migrate` |
| Gradle | `./gradlew flywayMigrate` |
| Spring Boot auto | sobe com app se `spring.flyway.enabled=true` |

Antes de migrar: banco acessível com credenciais de dev (ver `application.yml`, `.env`, docker compose).

Se falhar:
1. Ler mensagem Flyway (checksum, duplicate version, SQL error).
2. **Não alterar** migration já aplicada fora de dev local.
3. Em dev limpo: drop schema/database e remigrar do zero.

---

## Armadilhas comuns

| Problema | Prevenção |
|----------|-----------|
| Versão duplicada | Listar pasta antes de criar |
| Constraint falha por dados sujos | Migration de cleanup antes |
| Entity desync | Mesmo PR: SQL + entity |
| Editou migration aplicada | Nova migration corretiva |
| Índice unique parcial | Condição `WHERE` explícita no SQL |
| Rename breaking | Migration em 2 passos: add col → backfill → drop old |

---

## Impacto downstream

Após schema, verificar necessidade de alterar:
- Repositories (`@Query`, derived methods)
- Services (regras que usam campo novo)
- DTOs/API (expor ou ocultar campo)
- Testes (fixtures, `@Sql`, Testcontainers)

Se expõe via API → considerar skill `spring-boot-new-endpoint`.
