package br.com.techne.sistemafolha.dashboard.api;

import java.math.BigDecimal;

public record CentroCustoStatsDTO(
    Long id,
    String descricao,
    Long quantidadeFuncionarios,
    BigDecimal valorTotal
) {}
