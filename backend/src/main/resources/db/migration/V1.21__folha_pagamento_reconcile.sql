-- Reconciliar colunas de folha_pagamento ausentes no histórico Flyway (schema drift ddl-auto)
ALTER TABLE folha_pagamento ADD COLUMN IF NOT EXISTS rubrica_id BIGINT REFERENCES rubricas(id);
ALTER TABLE folha_pagamento ADD COLUMN IF NOT EXISTS valor DECIMAL(15, 2);
ALTER TABLE folha_pagamento ADD COLUMN IF NOT EXISTS quantidade DECIMAL(15, 4);
ALTER TABLE folha_pagamento ADD COLUMN IF NOT EXISTS base_calculo DECIMAL(15, 2);

UPDATE folha_pagamento
SET valor = valor_total
WHERE valor IS NULL AND valor_total IS NOT NULL;

UPDATE folha_pagamento
SET quantidade = 1
WHERE quantidade IS NULL;

CREATE INDEX IF NOT EXISTS idx_folha_pagamento_rubrica ON folha_pagamento (rubrica_id);

CREATE INDEX IF NOT EXISTS idx_folha_pagamento_competencia_ativo
    ON folha_pagamento (data_inicio, data_fim, decimo_terceiro, ativo)
    WHERE ativo = TRUE;

COMMENT ON COLUMN folha_pagamento.rubrica_id IS 'Rubrica da linha ADP (reconciliado pós V1.0 itens_folha_pagamento)';
