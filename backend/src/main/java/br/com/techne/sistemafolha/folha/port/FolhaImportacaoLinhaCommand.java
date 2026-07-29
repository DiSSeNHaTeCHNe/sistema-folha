package br.com.techne.sistemafolha.folha.port;

import java.math.BigDecimal;

public record FolhaImportacaoLinhaCommand(
    Long funcionarioId,
    Long rubricaId,
    Long cargoId,
    Long centroCustoId,
    Long linhaNegocioId,
    BigDecimal valor,
    BigDecimal quantidade,
    BigDecimal baseCalculo
) {}
