package br.com.techne.sistemafolha.folha.port;

import java.math.BigDecimal;

public record FolhaLinhaSnapshot(
    Long funcionarioId,
    Long centroCustoId,
    String centroCustoDescricao,
    Long linhaNegocioId,
    String linhaNegocioDescricao,
    Long cargoId,
    String cargoDescricao,
    Long rubricaId,
    String rubricaCodigo,
    String rubricaDescricao,
    String tipoRubricaDescricao,
    BigDecimal valor
) {}
