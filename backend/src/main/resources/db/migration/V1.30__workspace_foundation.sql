CREATE TABLE IF NOT EXISTS workspace (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nome                VARCHAR(120) NOT NULL,
    widgets             JSONB NOT NULL DEFAULT '[]'::jsonb,
    versao_schema       INTEGER NOT NULL DEFAULT 1,
    data_criacao        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workspace_usuario_nome UNIQUE (usuario_id, nome)
);

CREATE INDEX IF NOT EXISTS idx_workspace_usuario
    ON workspace(usuario_id);

CREATE TABLE IF NOT EXISTS workspace_dataset (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nome                VARCHAR(120) NOT NULL,
    schema              JSONB NOT NULL,
    schema_version      INTEGER NOT NULL DEFAULT 1,
    data_criacao        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workspace_dataset_usuario
    ON workspace_dataset(usuario_id);

CREATE TABLE IF NOT EXISTS workspace_dataset_row (
    id                  BIGSERIAL PRIMARY KEY,
    dataset_id          BIGINT NOT NULL REFERENCES workspace_dataset(id) ON DELETE CASCADE,
    valores             JSONB NOT NULL,
    ordem               INTEGER,
    data_criacao        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workspace_dataset_row_dataset
    ON workspace_dataset_row(dataset_id);

CREATE TABLE IF NOT EXISTS workspace_widget_definition (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nome                VARCHAR(120) NOT NULL,
    tipo                VARCHAR(32) NOT NULL,
    fontes              JSONB NOT NULL,
    formula             TEXT,
    config              JSONB NOT NULL DEFAULT '{}'::jsonb,
    invalido            BOOLEAN NOT NULL DEFAULT FALSE,
    data_criacao        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workspace_widget_definition_usuario
    ON workspace_widget_definition(usuario_id);
