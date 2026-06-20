package br.com.techne.sistemafolha.dto;

import java.math.BigDecimal;

public record RubricaStatsDTO(
    Long id,
    String codigo,
    String descricao,
    BigDecimal valorTotal,
    Long quantidadeOcorrencias
) {} 