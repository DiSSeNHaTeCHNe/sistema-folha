ALTER TABLE ficha_linha
  ADD COLUMN IF NOT EXISTS porcentagem NUMERIC(7, 4);

COMMENT ON COLUMN ficha_linha.porcentagem IS
  'Snapshot da rubrica.porcentagem no processamento; null tratado como 100 no custo';
