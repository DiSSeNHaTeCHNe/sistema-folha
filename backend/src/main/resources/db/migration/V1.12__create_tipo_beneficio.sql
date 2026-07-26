-- Criação da tabela de tipos de benefício mensal
CREATE TABLE IF NOT EXISTS tipo_beneficio (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(200) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tipo_beneficio_ativo ON tipo_beneficio (ativo);

-- Tipos iniciais de benefício (códigos numéricos da planilha real "Relatorio Custo Beneficio Folha")
INSERT INTO tipo_beneficio (codigo, descricao) VALUES
    ('4000', 'Assi Med - UNIMED CE- Custo Empresa'),
    ('4001', 'Assi Med-UNIMED SALV- Custo Empresa'),
    ('4002', 'Assi Med - OMINT MEDI CUSTO EMPRESA'),
    ('4003', 'Assi Medic-GNDI Inter Custo Empresa'),
    ('5322', 'Seguros - Custo Empresa'),
    ('5612', 'Vale Refeição - Custo Empresa'),
    ('5615', 'Vale Alimentação - Custo Empresa'),
    ('5903', 'Vale Transporte - Custo Empresa')
ON CONFLICT (codigo) DO NOTHING;
