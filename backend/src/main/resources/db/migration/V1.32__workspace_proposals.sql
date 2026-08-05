-- P3: IA proposal staging (propor-e-confirmar)

CREATE TABLE IF NOT EXISTS workspace_ia_proposal (
    id                      BIGSERIAL PRIMARY KEY,
    solicitante_usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    status                  VARCHAR(16) NOT NULL,
    payload                 JSONB NOT NULL,
    data_criacao            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_expiracao          TIMESTAMP NOT NULL,
    data_resolucao          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workspace_ia_proposal_solicitante_status
    ON workspace_ia_proposal(solicitante_usuario_id, status);

CREATE INDEX IF NOT EXISTS idx_workspace_ia_proposal_expiracao
    ON workspace_ia_proposal(data_expiracao)
    WHERE status = 'PENDENTE';
