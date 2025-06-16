-- Função para atualizar o timestamp de atualização
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.data_atualizacao = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Criação da tabela de linhas de negócio
CREATE TABLE IF NOT EXISTS linhas_negocio (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

CREATE TRIGGER update_linhas_negocio_updated_at
    BEFORE UPDATE ON linhas_negocio
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Criação da tabela de cargos
CREATE TABLE IF NOT EXISTS cargos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    linha_negocio_id BIGINT NOT NULL REFERENCES linhas_negocio(id),
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

CREATE TRIGGER update_cargos_updated_at
    BEFORE UPDATE ON cargos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Criação da tabela de centros de custo
CREATE TABLE IF NOT EXISTS centros_custo (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    linha_negocio_id BIGINT NOT NULL REFERENCES linhas_negocio(id),
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

CREATE TRIGGER update_centros_custo_updated_at
    BEFORE UPDATE ON centros_custo
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Criação da tabela de funcionários
CREATE TABLE IF NOT EXISTS funcionarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    data_admissao DATE NOT NULL,
    cargo_id BIGINT NOT NULL REFERENCES cargos(id),
    centro_custo_id BIGINT NOT NULL REFERENCES centros_custo(id),
    id_externo VARCHAR(50) UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

CREATE TRIGGER update_funcionarios_updated_at
    BEFORE UPDATE ON funcionarios
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Criação da tabela de rubricas
CREATE TABLE IF NOT EXISTS rubricas (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descricao VARCHAR(255) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    porcentagem FLOAT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

CREATE TRIGGER update_rubricas_updated_at
    BEFORE UPDATE ON rubricas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Criação da tabela de folha de pagamento
CREATE TABLE IF NOT EXISTS folha_pagamento (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    rubrica_id BIGINT NOT NULL REFERENCES rubricas(id),
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    valor DECIMAL(15,2) NOT NULL,
    quantidade DECIMAL(15,2) NOT NULL,
    base_calculo DECIMAL(15,2),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

CREATE TRIGGER update_folha_pagamento_updated_at
    BEFORE UPDATE ON folha_pagamento
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Criação da tabela de benefícios
CREATE TABLE IF NOT EXISTS beneficios (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(15,2) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    observacao TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

CREATE TRIGGER update_beneficios_updated_at
    BEFORE UPDATE ON beneficios
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Criação da tabela de usuários
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    centro_custo_id BIGINT REFERENCES centros_custo(id),
    primeiro_acesso BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

CREATE TRIGGER update_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Criação da tabela de permissões dos usuários
CREATE TABLE IF NOT EXISTS usuario_permissoes (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    permissao VARCHAR(50) NOT NULL,
    PRIMARY KEY (usuario_id, permissao)
);

-- Inserção do usuário administrador padrão
-- Senha: admin (deve ser alterada no primeiro acesso)
INSERT INTO linhas_negocio (codigo, descricao, ativo)
VALUES ('ADMIN', 'Linha de Negócio Administrativa', true)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO centros_custo (codigo, descricao, ativo, linha_negocio_id)
VALUES ('ADMIN', 'Centro de Custo Administrativo', true, 
        (SELECT id FROM linhas_negocio WHERE codigo = 'ADMIN'))
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO usuarios (login, senha, nome, centro_custo_id, primeiro_acesso)
VALUES ('admin', '$2a$10$4opK407j7af6ysFU7WEJ5Ope/icW4ajjsrquoOCB8N.0nY623CdPC', 'Administrador',
        (SELECT id FROM centros_custo WHERE codigo = 'ADMIN'), true)
ON CONFLICT (login) DO NOTHING;

-- Inserção das permissões do administrador
INSERT INTO usuario_permissoes (usuario_id, permissao)
VALUES (1, 'ROLE_ADMIN')
ON CONFLICT (usuario_id, permissao) DO NOTHING;

-- Criação de índices para melhor performance
CREATE INDEX IF NOT EXISTS idx_linhas_negocio_ativo ON linhas_negocio(ativo);
CREATE INDEX IF NOT EXISTS idx_cargos_ativo ON cargos(ativo);
CREATE INDEX IF NOT EXISTS idx_cargos_linha_negocio ON cargos(linha_negocio_id);
CREATE INDEX IF NOT EXISTS idx_centros_custo_ativo ON centros_custo(ativo);
CREATE INDEX IF NOT EXISTS idx_funcionarios_cargo ON funcionarios(cargo_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_centro_custo ON funcionarios(centro_custo_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_ativo ON funcionarios(ativo);
CREATE INDEX IF NOT EXISTS idx_rubricas_ativo ON rubricas(ativo);
CREATE INDEX IF NOT EXISTS idx_beneficios_ativo ON beneficios(ativo);
CREATE INDEX IF NOT EXISTS idx_beneficios_funcionario ON beneficios(funcionario_id);
CREATE INDEX IF NOT EXISTS idx_beneficios_data ON beneficios(data_inicio, data_fim);
CREATE INDEX IF NOT EXISTS idx_folha_pagamento_ativo ON folha_pagamento(ativo);
CREATE INDEX IF NOT EXISTS idx_folha_pagamento_funcionario ON folha_pagamento(funcionario_id);
CREATE INDEX IF NOT EXISTS idx_folha_pagamento_data ON folha_pagamento(data_inicio, data_fim);

-- Criação de índices para centros de custo
CREATE INDEX IF NOT EXISTS idx_centros_custo_codigo ON centros_custo(codigo);
CREATE INDEX IF NOT EXISTS idx_centros_custo_linha_negocio ON centros_custo(linha_negocio_id);

-- Criação de índices para funcionários
CREATE INDEX IF NOT EXISTS idx_funcionarios_cpf ON funcionarios(cpf);
CREATE INDEX IF NOT EXISTS idx_funcionarios_cargo ON funcionarios(cargo_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_centro_custo ON funcionarios(centro_custo_id); 