-- Criação da tabela tipo_rubrica
CREATE TABLE IF NOT EXISTS tipo_rubrica (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(50) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

-- Inserção dos tipos de rubrica
INSERT INTO tipo_rubrica (id, descricao) VALUES 
(1, 'PROVENTO'),
(2, 'DESCONTO'),
(3, 'INFORMATIVO');

-- Adicionar coluna tipo_rubrica_id na tabela rubricas
ALTER TABLE rubricas ADD COLUMN tipo_rubrica_id BIGINT REFERENCES tipo_rubrica(id);

-- Migrar dados existentes da coluna tipo para tipo_rubrica_id
UPDATE rubricas SET tipo_rubrica_id = 1 WHERE tipo = 'PROVENTO';
UPDATE rubricas SET tipo_rubrica_id = 2 WHERE tipo = 'DESCONTO';
UPDATE rubricas SET tipo_rubrica_id = 3 WHERE tipo = 'INFORMATIVO';

-- Tornar a coluna tipo_rubrica_id NOT NULL
ALTER TABLE rubricas ALTER COLUMN tipo_rubrica_id SET NOT NULL;

-- Remover a coluna tipo antiga
ALTER TABLE rubricas DROP COLUMN tipo;

-- Criar índice para a nova coluna
CREATE INDEX IF NOT EXISTS idx_rubricas_tipo_rubrica ON rubricas(tipo_rubrica_id); 