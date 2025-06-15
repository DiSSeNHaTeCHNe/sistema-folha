-- Criação da tabela de funcionários
CREATE TABLE funcionarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cargo VARCHAR(100) NOT NULL,
    centro_custo VARCHAR(50) NOT NULL,
    linha_negocio VARCHAR(50) NOT NULL,
    id_externo VARCHAR(50) NOT NULL UNIQUE,
    data_admissao VARCHAR(10),
    sexo VARCHAR(1),
    tipo_salario VARCHAR(50),
    funcao VARCHAR(100),
    dep_irrf INTEGER,
    dep_sal_familia INTEGER,
    vinculo VARCHAR(50)
);

-- Criação da tabela de rubricas
CREATE TABLE rubricas (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descricao VARCHAR(255) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    porcentagem DECIMAL(5,2)
);

-- Criação da tabela de folha de pagamento
CREATE TABLE folha_pagamento (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    rubrica_id BIGINT NOT NULL REFERENCES rubricas(id),
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    valor DECIMAL(15,2) NOT NULL,
    quantidade DECIMAL(15,2) NOT NULL,
    base_calculo DECIMAL(15,2)
);

-- Criação da tabela de benefícios
CREATE TABLE beneficios (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(15,2) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    observacao TEXT
);

-- Criação da tabela de usuários
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    centro_custo VARCHAR(50) NOT NULL,
    primeiro_acesso BOOLEAN NOT NULL DEFAULT TRUE
);

-- Criação da tabela de permissões dos usuários
CREATE TABLE usuario_permissoes (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    permissao VARCHAR(50) NOT NULL,
    PRIMARY KEY (usuario_id, permissao)
);

-- Inserção do usuário administrador padrão
-- Senha: admin (deve ser alterada no primeiro acesso)
INSERT INTO usuarios (login, senha, nome, centro_custo, primeiro_acesso)
VALUES ('admin', '$2a$10$X7UrH5YxX5YxX5YxX5YxX.5YxX5YxX5YxX5YxX5YxX5YxX5YxX', 'Administrador', 'ADMIN', true);

-- Inserção das permissões do administrador
INSERT INTO usuario_permissoes (usuario_id, permissao)
VALUES (1, 'ROLE_ADMIN'); 