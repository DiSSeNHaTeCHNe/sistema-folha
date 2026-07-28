-- FCLT-04/FCLT-07: ficha mensal e linhas com snapshot de operadores
DO $$ BEGIN
    CREATE TYPE origem_linha AS ENUM ('FOLHA_ADP', 'CUSTO_FIXO', 'CALCULADO');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS ficha_mensal (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    competencia_inicio DATE NOT NULL,
    competencia_fim DATE NOT NULL,
    decimo_terceiro BOOLEAN NOT NULL DEFAULT FALSE,
    bruto DECIMAL(15, 2) NOT NULL DEFAULT 0,
    liquido DECIMAL(15, 2) NOT NULL DEFAULT 0,
    custo_folha DECIMAL(15, 2) NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ficha_mensal_competencia_funcionario
    ON ficha_mensal (competencia_inicio, competencia_fim, decimo_terceiro, funcionario_id)
    WHERE ativo = TRUE;

CREATE INDEX IF NOT EXISTS idx_ficha_mensal_competencia
    ON ficha_mensal (competencia_inicio, competencia_fim, decimo_terceiro);

CREATE TABLE IF NOT EXISTS ficha_linha (
    id BIGSERIAL PRIMARY KEY,
    ficha_mensal_id BIGINT NOT NULL REFERENCES ficha_mensal(id) ON DELETE CASCADE,
    rubrica_id BIGINT NOT NULL REFERENCES rubricas(id),
    valor DECIMAL(15, 2) NOT NULL,
    origem_linha origem_linha NOT NULL,
    operador_bruto SMALLINT NOT NULL,
    operador_liquido SMALLINT NOT NULL,
    operador_custo SMALLINT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_ficha_linha_ficha_mensal ON ficha_linha (ficha_mensal_id);
CREATE INDEX IF NOT EXISTS idx_ficha_linha_origem ON ficha_linha (origem_linha);

COMMENT ON TABLE ficha_mensal IS 'Totalizadores persistidos por funcionário × competência pós-processamento';
COMMENT ON TABLE ficha_linha IS 'Linhas materializadas da ficha com operadores snapshot e origem';
