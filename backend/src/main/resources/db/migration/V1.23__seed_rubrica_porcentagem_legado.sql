-- FIX2-17: mapeamento mínimo homologação — Salário Base legado Custo Techne (138,63%).
-- Demais códigos de rubrica permanecem sob carga manual do RH.
UPDATE rubricas
SET porcentagem = 138.63
WHERE codigo = '0010'
  AND ativo = TRUE;
