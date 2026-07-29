-- FIX3-01: rubrica fixa global (funcionario_id nullable)
ALTER TABLE funcionario_rubrica_fixa
    ALTER COLUMN funcionario_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_funcionario_rubrica_fixa_global_vigencia
    ON funcionario_rubrica_fixa (rubrica_id, vigencia_inicio, vigencia_fim)
    WHERE ativo = TRUE AND funcionario_id IS NULL;

COMMENT ON COLUMN funcionario_rubrica_fixa.funcionario_id IS
    'NULL = fixa global (todos CLT processados na competência); NOT NULL = fixa individual';
