-- FCLT-18: custos fixos Techne por funcionário (INT-2)
CREATE TABLE IF NOT EXISTS funcionario_rubrica_fixa (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    rubrica_id BIGINT NOT NULL REFERENCES rubricas(id),
    valor NUMERIC(15, 2),
    vigencia_inicio DATE NOT NULL,
    vigencia_fim DATE,
    comentario VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    data_atualizacao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_funcionario_rubrica_fixa_funcionario
    ON funcionario_rubrica_fixa (funcionario_id)
    WHERE ativo = TRUE;

CREATE INDEX IF NOT EXISTS idx_funcionario_rubrica_fixa_vigencia
    ON funcionario_rubrica_fixa (funcionario_id, rubrica_id, vigencia_inicio, vigencia_fim)
    WHERE ativo = TRUE;

COMMENT ON TABLE funcionario_rubrica_fixa IS 'Custos fixos Techne por funcionário (INT-2)';
