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
(837, 'Administração de Vendas', 76),
(904, 'Administração de Vendas', 79);

INSERT INTO cargos (id, descricao) VALUES
(1, 'Advogado Pleno'),
(2, 'Analista Adm Financeiro Jr'),
(3, 'Analista Administrativo Financeiro Júnior'),
(4, 'Analista de Administração de Pessoal Júnior'),
(5, 'Analista de Administração de Pessoal Pleno'),
(6, 'Analista de Áudio e Vídeo'),
(7, 'Analista de Cargos e Salários'),
(8, 'Analista de Carreira e Remuneração'),
(9, 'Analista de Chatbot'),
(10, 'Analista de Chatbot Pleno'),
(11, 'Analista de Compliance'),
(12, 'Analista de Controladoria Júnior'),
(13, 'Analista de Controladoria Sênior'),
(14, 'Analista de Desenvolvimento de Sistemas Pleno'),
(15, 'Analista de Desenvolvimento Organizacional Sênior'),
(16, 'Analista de DevOps'),
(17, 'Analista de DevOps Pleno'),
(18, 'Analista de DevOps Sênior'),
(19, 'Analista de Documentação'),
(20, 'Analista Documentador'),
(21, 'Analista Documentador Júnior'),
(22, 'Analista de Faturamento'),
(23, 'Analista de Infraestrutura'),
(24, 'Analista de Infraestrutura Júnior'),
(25, 'Analista de Infraestrutura Pleno'),
(26, 'Analista de Infraestrutura Sênior'),
(27, 'Analista de Java Pleno'),
(28, 'Analista Jurídico'),
(29, 'Analista de Mídias Digitais'),
(30, 'Analista de Mídias Digitais Júnior'),
(31, 'Analista de Marketing'),
(32, 'Analista de Marketing Júnior'),
(33, 'Analista de Marketing Pleno'),
(34, 'Analista de Marketing Sênior'),
(35, 'Analista de Negócios'),
(36, 'Analista de Negócios Pleno'),
(37, 'Analista de Negócios Sênior'),
(38, 'Analista de Pré-vendas'),
(39, 'Analista de Processos e Garantia da Qualidade Sênior'),
(40, 'Analista de Projetos'),
(41, 'Analista de Projetos Júnior'),
(42, 'Analista de Projetos Pleno'),
(43, 'Analista de Projetos Sênior'),
(44, 'Analista de Qualidade'),
(45, 'Analista de Qualidade Júnior'),
(46, 'Analista de Qualidade Pleno'),
(47, 'Analista de Qualidade Sênior'),
(48, 'Analista de Recrutamento e Seleção Pleno'),
(49, 'Analista de Recrutamento e Seleção Sênior'),
(50, 'Analista de Recursos Humanos Júnior'),
(51, 'Analista de Recursos Humanos Pleno'),
(52, 'Analista de Recursos Humanos Sênior'),
(53, 'Analista de Requisitos'),
(54, 'Analista de Segurança Pleno'),
(55, 'Analista de Sistemas'),
(56, 'Analista de Sistemas Júnior'),
(57, 'Analista de Sistemas Pleno'),
(58, 'Analista de Sistemas Sênior'),
(59, 'Analista de Suporte'),
(60, 'Analista de Suporte Júnior'),
(61, 'Analista de Suporte Pleno'),
(62, 'Analista de Testes'),
(63, 'Analista de Testes Júnior'),
(64, 'Analista de Testes Pleno'),
(65, 'Analista de Testes Sênior'),
(66, 'Analista de Tesouraria Pleno'),
(67, 'Analista de Treinamento'),
(68, 'Analista Desenvolvedor'),
(69, 'Analista ERP'),
(70, 'Analista ERP Júnior'),
(71, 'Analista ERP Pleno'),
(72, 'Analista ERP Sênior'),
(73, 'Analista Full Stack Sênior'),
(74, 'Arquiteto de Sistemas'),
(75, 'Arquiteto de Soluções'),
(76, 'Arquiteto de Soluções de TI'),
(77, 'Assistente Administrativo'),
(78, 'Assistente Comercial'),
(79, 'Assistente Comercial Pleno'),
(80, 'Assistente de Desenvolvimento de Sistemas'),
(81, 'Assistente de Documentação'),
(82, 'Assistente Executivo'),
(83, 'Assistente de Infraestrutura'),
(84, 'Assistente de Mídias Digitais'),
(85, 'Assistente de Projetos'),
(86, 'Assistente de Prospecção'),
(87, 'Assistente de Qualidade'),
(88, 'Assistente de Sistemas'),
(89, 'Assistente de Suporte'),
(90, 'Assistente de Vendas'),
(91, 'Assistente ERP'),
(92, 'Auxiliar Administrativo'),
(93, 'Auxiliar de Serviços Gerais'),
(94, 'Cientista de Dados'),
(95, 'Coordenador Comercial'),
(96, 'Coordenador de Administração de Pessoal'),
(97, 'Coordenador de Aliança e Sucesso do Cliente'),
(98, 'Coordenador de Infraestrutura'),
(99, 'Coordenador de Marketing'),
(100, 'Coordenador de PMO'),
(101, 'Coordenador de Produto'),
(102, 'Coordenador de RH'),
(103, 'Coordenador de Serviços'),
(104, 'Coordenador de Sistemas'),
(105, 'Designer Instrucional'),
(106, 'Desenvolvedor Full Stack'),
(107, 'Desenvolvedor Low-Code Júnior'),
(108, 'Desenvolvedor Low-Code Pleno'),
(109, 'Diretor'),
(110, 'Diretor Comercial'),
(111, 'Diretor de Operações'),
(112, 'Diretor de Operações e Serviços de TI'),
(113, 'Diretor de Serviços'),
(114, 'Diretor Financeiro'),
(115, 'Especialista ERP'),
(116, 'Especialista em Faturamento'),
(117, 'Estagiário'),
(118, 'Executivo de Contas'),
(119, 'Executivo de Pré-vendas'),
(120, 'Executivo de Pré-vendas Júnior'),
(121, 'Executivo de Vendas'),
(122, 'Executivo de Vendas Júnior'),
(123, 'Gerente Administrativo Financeiro'),
(124, 'Gerente de Canais'),
(125, 'Gerente de Contas'),
(126, 'Gerente de Infraestrutura'),
(127, 'Gerente de Marketing'),
(128, 'Gerente de Operações BPO'),
(129, 'Gerente de Projetos'),
(130, 'Gerente de Projetos Júnior'),
(131, 'Gerente de Projetos Pleno'),
(132, 'Gerente de Recursos Humanos'),
(133, 'Gerente de Serviços'),
(134, 'Gerente de Serviços de TI'),
(135, 'Gerente de Sistemas'),
(136, 'Gerente de Vendas'),
(137, 'Gerente Comercial'),
(138, 'Gerente Financeiro'),
(139, 'Instrutor de Treinamento de Software'),
(140, 'Instrutor de Treinamento de Software Sênior'),
(141, 'Líder de Desenvolvimento de Produtos'),
(142, 'Líder de Projetos'),
(143, 'Líder de Testes'),
(144, 'Líder Técnico'),
(145, 'Oficial de Proteção de Dados'),
(146, 'Operador de Testes'),
(147, 'Pesquisador'),
(148, 'PJ'),
(149, 'Presidente'),
(150, 'Product Owner'),
(151, 'Programador'),
(152, 'Programador Júnior'),
(153, 'Programador Pleno'),
(154, 'Recepcionista'),
(155, 'Secretário'),
(156, 'Sócio'),
(157, 'Sócio Presidente do Conselho'),
(158, 'Supervisor de Pré-vendas'),
(159, 'Trainee'),
(160, 'UX Designer Pleno'),
(161, 'UX Designer Sênior'),
(162, 'Vendedor'),
(163, 'Vice-Presidente de Negócios Cronapp'),
(164, 'Vice-Presidente de Negócios Educacionais'),
(165, 'Vice-Presidente de Negócios de Governo'),
(166, 'Web Designer'),
(167, 'Web Designer Sênior'),
(168, 'Testador'),
(169, 'Diretor de Serviços'),
(170, 'Analista Comercial'),
(171, 'Analista Financeiro Pleno'),
(172, 'Scrum Master Pleno'),
(173, 'Analista de Geração de Leads Pleno'),
(174, 'Joven Aprendiz'),
(175, 'Arquiteto de Qualidade Pleno'),
(176, 'Desenvolver Front End Pleno'),
(177, 'Coordenador(a) QA'),
(178, 'Scrum Master Senior'),
(179, 'Analista de Atendimento Consignado'),
(180, 'Analista de Contratos'),
(181, 'Gerente de Produtos e Soluções'),
(182, 'Gerente de Fabrica de Software'),
(183, 'Gerente de Suporte'),
(184, 'Analista Jurídico Pleno'),
(185, 'Técnico de Suporte'),
(186, 'Analista de Suporte Tecnico'),
(187, 'Analista de Sistemas Fullstack Senior');


INSERT INTO usuarios (login, senha, nome, email) VALUES
('admin', '$2a$10$4opK407j7af6ysFU7WEJ5Ope/icW4ajjsrquoOCB8N.0nY623CdPC', 'Administrador', 'admin@techne.com.br'); 