-- Criação da tabela de cargos
CREATE TABLE IF NOT EXISTS cargos (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(100) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE
);

-- Criação da tabela de linhas de negócio
CREATE TABLE IF NOT EXISTS linhas_negocio (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Criação da tabela de centros de custo
CREATE TABLE IF NOT EXISTS centros_custo (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    linha_negocio_id BIGINT NOT NULL REFERENCES linhas_negocio(id)
);

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

-- Criação da tabela de folha de pagamento
CREATE TABLE IF NOT EXISTS folha_pagamento (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    valor_total DECIMAL(10,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

-- Criação da tabela de itens da folha de pagamento
CREATE TABLE IF NOT EXISTS itens_folha_pagamento (
    id BIGSERIAL PRIMARY KEY,
    folha_pagamento_id BIGINT NOT NULL REFERENCES folha_pagamento(id),
    rubrica_id BIGINT NOT NULL REFERENCES rubricas(id),
    valor DECIMAL(10,2) NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

-- Criação da tabela de benefícios
CREATE TABLE IF NOT EXISTS beneficios (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id),
    tipo VARCHAR(50) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

-- Criação da tabela de usuários
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);

-- Criação de índices
CREATE INDEX IF NOT EXISTS idx_linhas_negocio_ativo ON linhas_negocio(ativo);
CREATE INDEX IF NOT EXISTS idx_cargos_ativo ON cargos(ativo);
CREATE INDEX IF NOT EXISTS idx_centros_custo_ativo ON centros_custo(ativo);
CREATE INDEX IF NOT EXISTS idx_centros_custo_linha_negocio ON centros_custo(linha_negocio_id);
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

-- Criação de índices para funcionários
CREATE INDEX IF NOT EXISTS idx_funcionarios_cpf ON funcionarios(cpf);
CREATE INDEX IF NOT EXISTS idx_funcionarios_cargo ON funcionarios(cargo_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_centro_custo ON funcionarios(centro_custo_id);

-- Inserção de dados iniciais
INSERT INTO linhas_negocio (id, descricao) VALUES 
(76, 'Corporate'),
(77, 'CronApp'),
(78, 'Educação'),
(79, 'Governo'),
(80, 'Saúde');

INSERT INTO centros_custo (id, descricao, linha_negocio_id) VALUES
(767, 'Suporte - Gov', 79),
(768, 'P&D Evol Gov', 79),
(769, 'Suporte Edu', 78),
(770, 'Serviços Cronapp', 77),
(771, 'Suporte CronApp', 77),
(772, 'Vendas e Pré Vendas Cronapp', 77),
(773, 'P&D Evol Cronapp', 77),
(774, 'P&D Inov Cronapp', 77),
(775, 'RH', 76),
(776, 'Suporte TI', 76),
(777, 'Canais', 76),
(778, 'Marketing Corporativo', 76),
(779, 'P&D Evol LU Edu', 78),
(780, 'Administrativo', 76),
(781, 'CEO e CFO', 76),
(782, 'Conselho Consultivo', 76),
(783, 'Housing e Facilities', 76),
(784, 'Marketing Gov', 79),
(785, 'Marketing Edu', 78),
(786, 'Marketing Cronapp', 77),
(787, 'Infra CronApp', 77),
(788, 'Infra Edu', 78),
(789, 'P&D Inov Gov', 79),
(790, 'Vendas e Pré Vendas Edu', 78),
(791, 'Vendas e Pré Vendas Gov', 79),
(792, 'P&D Inov Edu', 78),
(793, 'P&D Evol Plugin Edu', 78),
(794, 'DevOps', 76),
(795, 'Academy Cronapp', 77),
(796, 'Academy Edu', 78),
(797, 'Academy eSocial', 76),
(798, 'Academy Gov', 79),
(799, 'CO Cronapp', 77),
(800, 'CO Edu', 78),
(801, 'CO Gov', 79),
(802, 'Consultoria Edu', 78),
(803, 'Consultoria eSocial', 76),
(804, 'Consultoria Gov', 79),
(805, 'Controladoria', 76),
(806, 'CQ Edu', 78),
(807, 'CQ eSocial', 76),
(808, 'CQ Gov', 79),
(809, 'CQ Cronapp', 77),
(810, 'CS Edu', 78),
(811, 'CS Gov', 79),
(812, 'Custo Capital Empregado', 76),
(813, 'Customização Edu', 78),
(814, 'Customização Gov', 79),
(815, 'Despesas Company Cron', 77),
(816, 'Despesas Company Edu', 78),
(817, 'Despesas Company Gov', 79),
(818, 'Despesas Corporativas', 76),
(819, 'Diretoria CBO', 76),
(820, 'Diretoria COO', 76),
(821, 'Diretoria Serviços', 76),
(822, 'Infra Gov', 79),
(823, 'P&D eSocial', 76),
(824, 'Plugin Gov', 79),
(825, 'Plugin Inov Edu', 78),
(826, 'Plugin Inov Gov', 79),
(827, 'Produtos Saúde', 80),
(828, 'Suporte eSocial', 76),
(829, 'Sustentação Edu', 78),
(830, 'Sustentação Gov', 79),
(836, 'Suporte Ergon', 80),
(837, 'Administração de Vendas', 76);

INSERT INTO cargos (descricao) VALUES
('Advogado Pleno'),
('Analista Adm Financeiro Jr'),
('Analista Administrativo Financeiro Júnior'),
('Analista de Administração de Pessoal Júnior'),
('Analista de Administração de Pessoal Pleno'),
('Analista de Áudio e Vídeo'),
('Analista de Cargos e Salários'),
('Analista de Carreira e Remuneração'),
('Analista de Chatbot'),
('Analista de Chatbot Pleno'),
('Analista de Compliance'),
('Analista de Controladoria Júnior'),
('Analista de Controladoria Sênior'),
('Analista de Desenvolvimento de Sistemas Pleno'),
('Analista de Desenvolvimento Organizacional Sênior'),
('Analista de DevOps'),
('Analista de DevOps Pleno'),
('Analista de DevOps Sênior'),
('Analista de Documentação'),
('Analista Documentador'),
('Analista Documentador Júnior'),
('Analista de Faturamento'),
('Analista de Infraestrutura'),
('Analista de Infraestrutura Júnior'),
('Analista de Infraestrutura Pleno'),
('Analista de Infraestrutura Sênior'),
('Analista de Java Pleno'),
('Analista Jurídico'),
('Analista de Mídias Digitais'),
('Analista de Mídias Digitais Júnior'),
('Analista de Marketing'),
('Analista de Marketing Júnior'),
('Analista de Marketing Pleno'),
('Analista de Marketing Sênior'),
('Analista de Negócios'),
('Analista de Negócios Pleno'),
('Analista de Negócios Sênior'),
('Analista de Pré-vendas'),
('Analista de Processos e Garantia da Qualidade Sênior'),
('Analista de Projetos'),
('Analista de Projetos Júnior'),
('Analista de Projetos Pleno'),
('Analista de Projetos Sênior'),
('Analista de Qualidade'),
('Analista de Qualidade Júnior'),
('Analista de Qualidade Pleno'),
('Analista de Qualidade Sênior'),
('Analista de Recrutamento e Seleção Pleno'),
('Analista de Recrutamento e Seleção Sênior'),
('Analista de Recursos Humanos Júnior'),
('Analista de Recursos Humanos Pleno'),
('Analista de Recursos Humanos Sênior'),
('Analista de Requisitos'),
('Analista de Segurança Pleno'),
('Analista de Sistemas'),
('Analista de Sistemas Júnior'),
('Analista de Sistemas Pleno'),
('Analista de Sistemas Sênior'),
('Analista de Suporte'),
('Analista de Suporte Júnior'),
('Analista de Suporte Pleno'),
('Analista de Testes'),
('Analista de Testes Júnior'),
('Analista de Testes Pleno'),
('Analista de Testes Sênior'),
('Analista de Tesouraria Pleno'),
('Analista de Treinamento'),
('Analista Desenvolvedor'),
('Analista ERP'),
('Analista ERP Júnior'),
('Analista ERP Pleno'),
('Analista ERP Sênior'),
('Analista Full Stack Sênior'),
('Arquiteto de Sistemas'),
('Arquiteto de Soluções'),
('Arquiteto de Soluções de TI'),
('Assistente Administrativo'),
('Assistente Comercial'),
('Assistente Comercial Pleno'),
('Assistente de Desenvolvimento de Sistemas'),
('Assistente de Documentação'),
('Assistente Executivo'),
('Assistente de Infraestrutura'),
('Assistente de Mídias Digitais'),
('Assistente de Projetos'),
('Assistente de Prospecção'),
('Assistente de Qualidade'),
('Assistente de Sistemas'),
('Assistente de Suporte'),
('Assistente de Vendas'),
('Assistente ERP'),
('Auxiliar Administrativo'),
('Auxiliar de Serviços Gerais'),
('Cientista de Dados'),
('Coordenador Comercial'),
('Coordenador de Administração de Pessoal'),
('Coordenador de Aliança e Sucesso do Cliente'),
('Coordenador de Infraestrutura'),
('Coordenador de Marketing'),
('Coordenador de PMO'),
('Coordenador de Produto'),
('Coordenador de RH'),
('Coordenador de Serviços'),
('Coordenador de Sistemas'),
('Designer Instrucional'),
('Desenvolvedor Full Stack'),
('Desenvolvedor Low-Code Júnior'),
('Desenvolvedor Low-Code Pleno'),
('Diretor'),
('Diretor Comercial'),
('Diretor de Operações'),
('Diretor de Operações e Serviços de TI'),
('Diretor de Serviços'),
('Diretor Financeiro'),
('Especialista ERP'),
('Especialista em Faturamento'),
('Estagiário'),
('Executivo de Contas'),
('Executivo de Pré-vendas'),
('Executivo de Pré-vendas Júnior'),
('Executivo de Vendas'),
('Executivo de Vendas Júnior'),
('Gerente Administrativo Financeiro'),
('Gerente de Canais'),
('Gerente de Contas'),
('Gerente de Infraestrutura'),
('Gerente de Marketing'),
('Gerente de Operações BPO'),
('Gerente de Projetos'),
('Gerente de Projetos Júnior'),
('Gerente de Projetos Pleno'),
('Gerente de Recursos Humanos'),
('Gerente de Serviços'),
('Gerente de Serviços de TI'),
('Gerente de Sistemas'),
('Gerente de Vendas'),
('Gerente Comercial'),
('Gerente Financeiro'),
('Instrutor de Treinamento de Software'),
('Instrutor de Treinamento de Software Sênior'),
('Líder de Desenvolvimento de Produtos'),
('Líder de Projetos'),
('Líder de Testes'),
('Líder Técnico'),
('Oficial de Proteção de Dados'),
('Operador de Testes'),
('Pesquisador'),
('PJ'),
('Presidente'),
('Product Owner'),
('Programador'),
('Programador Júnior'),
('Programador Pleno'),
('Recepcionista'),
('Secretário'),
('Sócio'),
('Sócio Presidente do Conselho'),
('Supervisor de Pré-vendas'),
('Trainee'),
('UX Designer Pleno'),
('UX Designer Sênior'),
('Vendedor'),
('Vice-Presidente de Negócios Cronapp'),
('Vice-Presidente de Negócios Educacionais'),
('Vice-Presidente de Negócios de Governo'),
('Web Designer'),
('Web Designer Sênior'),
('Testador'),
('Diretor de Serviços'),
('Analista Comercial'),
('Analista Financeiro Pleno');

INSERT INTO usuarios (login, senha, nome, email) VALUES
('admin', '$2a$10$4opK407j7af6ysFU7WEJ5Ope/icW4ajjsrquoOCB8N.0nY623CdPC', 'Administrador', 'admin@techne.com.br'); 