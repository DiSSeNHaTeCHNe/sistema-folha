-- Criação da tabela de nós do organograma
CREATE TABLE IF NOT EXISTS nos_organograma (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    nivel INTEGER NOT NULL DEFAULT 0,
    parent_id BIGINT REFERENCES nos_organograma(id) ON DELETE CASCADE,
    posicao INTEGER NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    organograma_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    
    -- Constraint para garantir que apenas um organograma esteja ativo por vez
    CONSTRAINT check_organograma_ativo UNIQUE (organograma_ativo) DEFERRABLE INITIALLY DEFERRED
);

-- Criação da tabela de associação entre funcionários e nós do organograma
CREATE TABLE IF NOT EXISTS funcionario_organograma (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT NOT NULL REFERENCES funcionarios(id) ON DELETE CASCADE,
    no_organograma_id BIGINT NOT NULL REFERENCES nos_organograma(id) ON DELETE CASCADE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    
    -- Constraint para evitar duplicação da mesma associação
    UNIQUE(funcionario_id, no_organograma_id)
);

-- Criação da tabela de associação entre centros de custo e nós do organograma
CREATE TABLE IF NOT EXISTS centro_custo_organograma (
    id BIGSERIAL PRIMARY KEY,
    centro_custo_id BIGINT NOT NULL REFERENCES centros_custo(id) ON DELETE CASCADE,
    no_organograma_id BIGINT NOT NULL REFERENCES nos_organograma(id) ON DELETE CASCADE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    
    -- Constraint para evitar duplicação da mesma associação
    UNIQUE(centro_custo_id, no_organograma_id)
);

-- Criação de índices para otimizar consultas
CREATE INDEX IF NOT EXISTS idx_nos_organograma_parent ON nos_organograma(parent_id);
CREATE INDEX IF NOT EXISTS idx_nos_organograma_ativo ON nos_organograma(ativo);
CREATE INDEX IF NOT EXISTS idx_nos_organograma_organograma_ativo ON nos_organograma(organograma_ativo);
CREATE INDEX IF NOT EXISTS idx_nos_organograma_nivel ON nos_organograma(nivel);
CREATE INDEX IF NOT EXISTS idx_nos_organograma_posicao ON nos_organograma(posicao);

CREATE INDEX IF NOT EXISTS idx_funcionario_organograma_funcionario ON funcionario_organograma(funcionario_id);
CREATE INDEX IF NOT EXISTS idx_funcionario_organograma_no ON funcionario_organograma(no_organograma_id);

CREATE INDEX IF NOT EXISTS idx_centro_custo_organograma_centro ON centro_custo_organograma(centro_custo_id);
CREATE INDEX IF NOT EXISTS idx_centro_custo_organograma_no ON centro_custo_organograma(no_organograma_id);

-- Função para garantir que apenas um organograma esteja ativo por vez
CREATE OR REPLACE FUNCTION check_single_active_organograma()
RETURNS TRIGGER AS $$
BEGIN
    -- Se estamos tentando ativar um organograma
    IF NEW.organograma_ativo = TRUE THEN
        -- Desativar todos os outros organogramas ativos
        UPDATE nos_organograma 
        SET organograma_ativo = FALSE, data_atualizacao = CURRENT_TIMESTAMP
        WHERE organograma_ativo = TRUE AND id != NEW.id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para executar a função
CREATE TRIGGER trigger_single_active_organograma
    BEFORE INSERT OR UPDATE ON nos_organograma
    FOR EACH ROW
    EXECUTE FUNCTION check_single_active_organograma();

-- Comentários nas tabelas para documentação
COMMENT ON TABLE nos_organograma IS 'Tabela que armazena os nós do organograma da empresa';
COMMENT ON COLUMN nos_organograma.organograma_ativo IS 'Indica se este nó pertence ao organograma atualmente ativo. Apenas um organograma pode estar ativo por vez.';
COMMENT ON COLUMN nos_organograma.nivel IS 'Nível hierárquico do nó na estrutura do organograma (0 = raiz)';
COMMENT ON COLUMN nos_organograma.posicao IS 'Posição do nó entre os irmãos no mesmo nível';

COMMENT ON TABLE funcionario_organograma IS 'Tabela de associação entre funcionários e nós do organograma';
COMMENT ON TABLE centro_custo_organograma IS 'Tabela de associação entre centros de custo e nós do organograma'; 