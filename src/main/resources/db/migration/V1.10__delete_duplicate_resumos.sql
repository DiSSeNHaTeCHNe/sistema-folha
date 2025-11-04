-- V1.10 - Remove registros duplicados em resumo_folha_pagamento
-- Remove fisicamente os registros duplicados, mantendo apenas o mais recente (maior ID) de cada grupo

-- Passo 1: Deletar registros de folha_pagamento relacionados aos resumos duplicados
DELETE FROM folha_pagamento fp
WHERE EXISTS (
    SELECT 1 
    FROM resumo_folha_pagamento r1
    WHERE fp.data_inicio = r1.competencia_inicio
      AND fp.data_fim = r1.competencia_fim
      AND r1.ativo = true
      AND EXISTS (
        SELECT 1 
        FROM resumo_folha_pagamento r2
        WHERE r2.competencia_inicio = r1.competencia_inicio
          AND r2.competencia_fim = r1.competencia_fim
          AND r2.decimo_terceiro = r1.decimo_terceiro
          AND r2.ativo = true
          AND r2.id > r1.id  -- Mantém apenas o registro com maior ID (mais recente)
      )
);

-- Passo 2: Deletar resumos duplicados, mantendo apenas o mais recente de cada grupo
DELETE FROM resumo_folha_pagamento r1
WHERE ativo = true
  AND EXISTS (
    SELECT 1 
    FROM resumo_folha_pagamento r2
    WHERE r2.competencia_inicio = r1.competencia_inicio
      AND r2.competencia_fim = r1.competencia_fim
      AND r2.decimo_terceiro = r1.decimo_terceiro
      AND r2.ativo = true
      AND r2.id > r1.id  -- Mantém o registro com maior ID (mais recente)
  );
