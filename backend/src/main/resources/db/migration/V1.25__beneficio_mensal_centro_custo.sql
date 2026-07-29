ALTER TABLE beneficio_mensal
  ADD COLUMN IF NOT EXISTS centro_custo_id BIGINT REFERENCES centros_custo(id);

CREATE INDEX IF NOT EXISTS idx_beneficio_mensal_centro_custo
  ON beneficio_mensal (centro_custo_id);
