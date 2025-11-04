-- Adiciona coluna para identificar folhas de 13º salário
ALTER TABLE resumo_folha_pagamento 
ADD COLUMN IF NOT EXISTS decimo_terceiro BOOLEAN DEFAULT FALSE;

-- Atualiza o comentário da tabela
COMMENT ON COLUMN resumo_folha_pagamento.decimo_terceiro IS 'Indica se a folha é referente ao 13º salário';
