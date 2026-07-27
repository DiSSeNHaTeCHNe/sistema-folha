package br.com.techne.sistemafolha.dashboard.api;

import java.math.BigDecimal;

public record RubricaStatsDTO(
    Long id,
    String codigo,
    String descricao,
    BigDecimal valorTotal,
    Long quantidadeOcorrencias
) {}
