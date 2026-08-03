CREATE TABLE IF NOT EXISTS relatorio (
    id                  BIGSERIAL PRIMARY KEY,
    tipo                VARCHAR(20) NOT NULL,
    mes                 INT NOT NULL CHECK (mes BETWEEN 1 AND 12),
    ano                 INT NOT NULL CHECK (ano BETWEEN 2000 AND 2100),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id),
    total_funcionarios  INT,
    total_folha         NUMERIC(19, 2),
    total_beneficios    NUMERIC(19, 2),
    total_valor         NUMERIC(19, 2),
    erro                VARCHAR(500),
    data_criacao        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_processamento  TIMESTAMP,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_relatorio_usuario_tipo_comp UNIQUE (usuario_id, tipo, mes, ano)
);

CREATE TABLE IF NOT EXISTS relatorio_arquivo (
    relatorio_id    BIGINT PRIMARY KEY REFERENCES relatorio(id) ON DELETE CASCADE,
    pdf_bytes       BYTEA NOT NULL,
    tamanho_bytes   BIGINT NOT NULL,
    data_criacao    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_relatorio_tipo_comp ON relatorio (tipo, ano DESC, mes DESC) WHERE ativo = TRUE;
CREATE INDEX IF NOT EXISTS idx_relatorio_status ON relatorio (status) WHERE ativo = TRUE;
