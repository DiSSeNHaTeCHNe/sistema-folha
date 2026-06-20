-- Permite múltiplos registros com o mesmo CPF quando inativos; apenas um ativo por CPF.
ALTER TABLE funcionarios DROP CONSTRAINT IF EXISTS funcionarios_cpf_key;

DROP INDEX IF EXISTS idx_funcionarios_cpf;

CREATE UNIQUE INDEX idx_funcionarios_cpf_ativo
ON funcionarios (cpf)
WHERE ativo = true;

COMMENT ON INDEX idx_funcionarios_cpf_ativo IS
    'Garante no máximo um funcionário ativo por CPF. Permite vários inativos com o mesmo CPF (ex.: estágio -> efetivo com nova matrícula).';
