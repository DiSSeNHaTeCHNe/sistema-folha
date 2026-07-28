-- FCLT-15: regime de trabalho explícito — seed CLT
CREATE TABLE IF NOT EXISTS regime_trabalho (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO regime_trabalho (codigo, descricao, ativo)
VALUES ('CLT', 'Consolidação das Leis do Trabalho', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO regime_trabalho (codigo, descricao, ativo)
VALUES
    ('PJ', 'Pessoa Jurídica', FALSE),
    ('ESTAGIO', 'Estágio', FALSE),
    ('AUTONOMO', 'Autônomo', FALSE)
ON CONFLICT (codigo) DO NOTHING;

ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS regime_trabalho_id BIGINT;

UPDATE funcionarios f
SET regime_trabalho_id = r.id
FROM regime_trabalho r
WHERE r.codigo = 'CLT'
  AND f.regime_trabalho_id IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_funcionarios_regime_trabalho'
    ) THEN
        ALTER TABLE funcionarios
            ADD CONSTRAINT fk_funcionarios_regime_trabalho
            FOREIGN KEY (regime_trabalho_id) REFERENCES regime_trabalho(id);
    END IF;
END $$;

-- NOT NULL após backfill
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM funcionarios WHERE regime_trabalho_id IS NULL
    ) THEN
        ALTER TABLE funcionarios ALTER COLUMN regime_trabalho_id SET NOT NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_funcionarios_regime_trabalho
    ON funcionarios (regime_trabalho_id);

COMMENT ON TABLE regime_trabalho IS 'Regime de vínculo do funcionário (Etapa 1: CLT ativo)';
