-- FCLT-01: operadores de rubrica derivados de tipo_rubrica
ALTER TABLE rubricas ADD COLUMN IF NOT EXISTS operador_bruto SMALLINT;
ALTER TABLE rubricas ADD COLUMN IF NOT EXISTS operador_liquido SMALLINT;
ALTER TABLE rubricas ADD COLUMN IF NOT EXISTS operador_custo SMALLINT;

UPDATE rubricas r
SET
    operador_bruto = CASE UPPER(tr.descricao)
        WHEN 'PROVENTO' THEN 1
        WHEN 'DESCONTO' THEN 0
        WHEN 'INFORMATIVO' THEN 0
        ELSE 0
    END,
    operador_liquido = CASE UPPER(tr.descricao)
        WHEN 'PROVENTO' THEN 1
        WHEN 'DESCONTO' THEN -1
        WHEN 'INFORMATIVO' THEN 0
        ELSE 0
    END,
    operador_custo = CASE UPPER(tr.descricao)
        WHEN 'PROVENTO' THEN 1
        WHEN 'DESCONTO' THEN 0
        WHEN 'INFORMATIVO' THEN 0
        ELSE 0
    END
FROM tipo_rubrica tr
WHERE r.tipo_rubrica_id = tr.id
  AND (r.operador_bruto IS NULL OR r.operador_liquido IS NULL OR r.operador_custo IS NULL);

ALTER TABLE rubricas ALTER COLUMN operador_bruto SET NOT NULL;
ALTER TABLE rubricas ALTER COLUMN operador_liquido SET NOT NULL;
ALTER TABLE rubricas ALTER COLUMN operador_custo SET NOT NULL;

COMMENT ON COLUMN rubricas.operador_bruto IS 'Impacto no totalizador bruto: -1, 0 ou +1';
COMMENT ON COLUMN rubricas.operador_liquido IS 'Impacto no totalizador líquido: -1, 0 ou +1';
COMMENT ON COLUMN rubricas.operador_custo IS 'Impacto no totalizador custo folha: -1, 0 ou +1';
