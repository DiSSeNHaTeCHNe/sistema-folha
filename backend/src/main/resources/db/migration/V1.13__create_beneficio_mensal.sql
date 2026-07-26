-- Criação da tabela de benefícios mensais por funcionário e competência
CREATE TABLE IF NOT EXISTS beneficio_mensal (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    tipo_beneficio_id BIGINT NOT NULL REFERENCES tipo_beneficio(id),
    valor DECIMAL(10,2) NOT NULL,
    competencia_inicio DATE NOT NULL,
    competencia_fim DATE NOT NULL,
    observacao VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_beneficio_mensal_competencia
    ON beneficio_mensal (competencia_inicio, competencia_fim);

CREATE INDEX IF NOT EXISTS idx_beneficio_mensal_func_comp
    ON beneficio_mensal (funcionario_id, competencia_inicio);

CREATE UNIQUE INDEX IF NOT EXISTS uk_beneficio_mensal_func_tipo_comp
    ON beneficio_mensal (funcionario_id, tipo_beneficio_id, competencia_inicio);
