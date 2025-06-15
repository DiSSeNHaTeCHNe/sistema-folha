-- Adiciona coluna ativo nas tabelas
ALTER TABLE funcionario ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE rubrica ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE beneficio ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE folha_pagamento ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- Cria índices para melhorar performance das consultas
CREATE INDEX idx_funcionario_ativo ON funcionario(ativo);
CREATE INDEX idx_rubrica_ativo ON rubrica(ativo);
CREATE INDEX idx_beneficio_ativo ON beneficio(ativo);
CREATE INDEX idx_folha_pagamento_ativo ON folha_pagamento(ativo); 