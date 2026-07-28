package br.com.techne.sistemafolha.cadastros.api;

/**
 * Filtro tri-state para listagem de funcionários por status de ativação.
 * <p>
 * O service mapeia {@link #ATIVO} → {@code true}, {@link #INATIVO} → {@code false}
 * e {@link #TODOS} → {@code null} na query do repositório.
 */
public enum FuncionarioStatusFiltro {
    /** Apenas funcionários ativos (padrão da API). */
    ATIVO,
    /** Apenas funcionários inativos (soft-delete). */
    INATIVO,
    /** Ativos e inativos. */
    TODOS
}
