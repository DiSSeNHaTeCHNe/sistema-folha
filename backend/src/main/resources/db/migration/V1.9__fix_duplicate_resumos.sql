-- V1.9 - Correção de registros duplicados em resumo_folha_pagamento
-- Mantém apenas o resumo mais recente (maior ID) de cada grupo de competência/tipo
-- e desativa os demais

-- Desativa registros duplicados, mantendo apenas o mais recente de cada grupo
UPDATE resumo_folha_pagamento r1
SET ativo = false
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

-- Log de quantos registros foram desativados
-- (Para verificar em produção)
