-- Remover a constraint incorreta que impede múltiplos registros com organograma_ativo = FALSE
ALTER TABLE nos_organograma DROP CONSTRAINT IF EXISTS check_organograma_ativo;

-- Criar índice único parcial que apenas garante que não haja mais de um registro com organograma_ativo = TRUE
-- Índices únicos parciais permitem múltiplos NULL ou FALSE, mas apenas um TRUE
CREATE UNIQUE INDEX idx_unique_organograma_ativo 
ON nos_organograma (organograma_ativo) 
WHERE organograma_ativo = TRUE;

-- Comentário explicativo
COMMENT ON INDEX idx_unique_organograma_ativo IS 'Garante que apenas um nó tenha organograma_ativo = TRUE por vez. Permite múltiplos nós com organograma_ativo = FALSE.';

