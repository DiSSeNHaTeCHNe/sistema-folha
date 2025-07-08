package br.com.techne.sistemafolha.dto;

import java.math.BigDecimal;

public record CargoStatsDTO(
    Long id,
    String descricao,
    Long quantidadeFuncionarios,
    BigDecimal valorMedio,
    BigDecimal valorTotal
) {} 