-- Criação da tabela de API Keys
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nome VARCHAR(100) NOT NULL,
    prefixo VARCHAR(32) NOT NULL,
    hash_chave VARCHAR(255) NOT NULL,
    escopo VARCHAR(16) NOT NULL DEFAULT 'READ',
    data_expiracao TIMESTAMP NOT NULL,
    revogado BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_uso_em TIMESTAMP,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para lookup e consultas
CREATE UNIQUE INDEX IF NOT EXISTS idx_api_keys_prefixo ON api_keys(prefixo);
CREATE INDEX IF NOT EXISTS idx_api_keys_usuario ON api_keys(usuario_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_expiracao ON api_keys(data_expiracao);
CREATE INDEX IF NOT EXISTS idx_api_keys_revogado ON api_keys(revogado);
