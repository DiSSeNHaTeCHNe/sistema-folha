-- P2: template marketplace + row audit trail

CREATE TABLE IF NOT EXISTS workspace_template (
    id                      BIGSERIAL PRIMARY KEY,
    publicador_usuario_id   BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nome                    VARCHAR(120) NOT NULL,
    tipo                    VARCHAR(32) NOT NULL,
    organograma_no_id       BIGINT REFERENCES nos_organograma(id),
    ativo                   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_workspace_template_publicador
    ON workspace_template(publicador_usuario_id);

CREATE INDEX IF NOT EXISTS idx_workspace_template_organograma
    ON workspace_template(organograma_no_id);

CREATE TABLE IF NOT EXISTS workspace_template_version (
    id                  BIGSERIAL PRIMARY KEY,
    template_id         BIGINT NOT NULL REFERENCES workspace_template(id) ON DELETE CASCADE,
    versao              INTEGER NOT NULL,
    estrutura           JSONB NOT NULL,
    estrutura_hash      VARCHAR(64) NOT NULL,
    data_publicacao     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workspace_template_version UNIQUE (template_id, versao)
);

CREATE INDEX IF NOT EXISTS idx_workspace_template_version_template
    ON workspace_template_version(template_id);

CREATE TABLE IF NOT EXISTS workspace_template_installation (
    id                      BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    template_id             BIGINT NOT NULL REFERENCES workspace_template(id) ON DELETE CASCADE,
    versao_instalada        INTEGER NOT NULL,
    workspace_id            BIGINT REFERENCES workspace(id) ON DELETE SET NULL,
    dataset_ids             JSONB,
    widget_definition_ids   JSONB,
    data_instalacao         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workspace_template_installation_usuario
    ON workspace_template_installation(usuario_id);

CREATE INDEX IF NOT EXISTS idx_workspace_template_installation_template
    ON workspace_template_installation(template_id);

CREATE TABLE IF NOT EXISTS workspace_dataset_row_audit (
    id                  BIGSERIAL PRIMARY KEY,
    row_id              BIGINT REFERENCES workspace_dataset_row(id) ON DELETE SET NULL,
    autor_usuario_id    BIGINT NOT NULL REFERENCES usuarios(id),
    acao                VARCHAR(16) NOT NULL,
    valores_anteriores  JSONB,
    valores_novos       JSONB,
    data_evento         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workspace_dataset_row_audit_row
    ON workspace_dataset_row_audit(row_id);

CREATE INDEX IF NOT EXISTS idx_workspace_dataset_row_audit_data
    ON workspace_dataset_row_audit(data_evento);
