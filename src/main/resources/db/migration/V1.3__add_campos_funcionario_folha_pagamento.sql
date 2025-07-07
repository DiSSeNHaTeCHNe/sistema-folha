-- Adicionar campos do funcionário à tabela de folha de pagamento
ALTER TABLE folha_pagamento 
ADD COLUMN cargo_id BIGINT REFERENCES cargos(id),
ADD COLUMN centro_custo_id BIGINT REFERENCES centros_custo(id),
ADD COLUMN linha_negocio_id BIGINT REFERENCES linhas_negocio(id);

-- Criar índices para os novos campos
CREATE INDEX IF NOT EXISTS idx_folha_pagamento_cargo ON folha_pagamento(cargo_id);
CREATE INDEX IF NOT EXISTS idx_folha_pagamento_centro_custo ON folha_pagamento(centro_custo_id);
CREATE INDEX IF NOT EXISTS idx_folha_pagamento_linha_negocio ON folha_pagamento(linha_negocio_id); 