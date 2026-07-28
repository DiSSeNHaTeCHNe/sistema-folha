package br.com.techne.sistemafolha.cadastros.api;

/**
 * Filtro tri-state para listagem de rubricas por status de ativação.
 * <p>
 * O service mapeia {@link #ATIVO} → {@code true}, {@link #INATIVO} → {@code false}
 * e {@link #TODOS} → {@code null} na query do repositório.
 */
public enum RubricaStatusFiltro {
    /** Apenas rubricas ativas (padrão da API). */
    ATIVO,
    /** Apenas rubricas inativas (soft-delete). */
    INATIVO,
    /** Ativas e inativas. */
    TODOS
}
