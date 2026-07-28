-- V1.16 - Identifica tipo de folha (normal vs 13º) em cada linha
ALTER TABLE folha_pagamento
    ADD COLUMN IF NOT EXISTS decimo_terceiro BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN folha_pagamento.decimo_terceiro IS 'Indica se a linha pertence à folha de 13º salário da competência';

CREATE INDEX IF NOT EXISTS idx_folha_pagamento_competencia_decimo
    ON folha_pagamento (data_inicio, data_fim, decimo_terceiro)
    WHERE ativo = true;
