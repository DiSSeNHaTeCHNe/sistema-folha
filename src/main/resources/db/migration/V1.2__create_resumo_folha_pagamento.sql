CREATE TABLE resumo_folha_pagamento (
    id BIGSERIAL PRIMARY KEY,
    total_empregados INTEGER NOT NULL,
    total_encargos DECIMAL(15,2) NOT NULL,
    total_pagamentos DECIMAL(15,2) NOT NULL,
    total_descontos DECIMAL(15,2) NOT NULL,
    total_liquido DECIMAL(15,2) NOT NULL,
    competencia_inicio DATE NOT NULL,
    competencia_fim DATE NOT NULL,
    data_importacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ativo BOOLEAN DEFAULT TRUE
); 