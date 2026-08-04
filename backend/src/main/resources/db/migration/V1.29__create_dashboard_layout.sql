CREATE TABLE IF NOT EXISTS dashboard_layout (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nome                VARCHAR(100) NOT NULL DEFAULT 'Meu dashboard',
    widgets             JSONB NOT NULL DEFAULT '[]'::jsonb,
    versao_schema       INTEGER NOT NULL DEFAULT 1,
    data_criacao        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_dashboard_layout_usuario
    ON dashboard_layout(usuario_id);
