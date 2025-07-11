-- Criação da tabela de permissões do usuário (que estava faltando)
CREATE TABLE IF NOT EXISTS usuario_permissoes (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    permissao VARCHAR(50) NOT NULL,
    PRIMARY KEY (usuario_id, permissao)
);

-- Adicionar campo funcionario_id na tabela usuarios
ALTER TABLE usuarios ADD COLUMN funcionario_id BIGINT;

-- Adicionar foreign key para funcionario
ALTER TABLE usuarios ADD CONSTRAINT fk_usuario_funcionario 
    FOREIGN KEY (funcionario_id) REFERENCES funcionarios(id);

-- Criar índice para otimizar consultas
CREATE INDEX IF NOT EXISTS idx_usuarios_funcionario ON usuarios(funcionario_id);

-- Remover campo email da tabela usuarios (se existir) já que agora usaremos o funcionario
ALTER TABLE usuarios DROP COLUMN IF EXISTS email;
ALTER TABLE usuarios DROP COLUMN IF EXISTS data_criacao;
ALTER TABLE usuarios DROP COLUMN IF EXISTS data_atualizacao;
ALTER TABLE usuarios DROP COLUMN IF EXISTS criado_por;
ALTER TABLE usuarios DROP COLUMN IF EXISTS atualizado_por;

-- Inserir permissões padrão para o usuário admin existente
INSERT INTO usuario_permissoes (usuario_id, permissao) VALUES 
(1, 'ADMIN'),
(1, 'USER'); 