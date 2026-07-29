ALTER TABLE ficha_mensal
  ADD COLUMN IF NOT EXISTS centro_custo_id BIGINT REFERENCES centros_custo(id);

CREATE INDEX IF NOT EXISTS idx_ficha_mensal_centro_custo
  ON ficha_mensal (centro_custo_id);
