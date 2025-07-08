package br.com.techne.sistemafolha.dto;

import java.math.BigDecimal;

public record LinhaNegocioStatsDTO(
    Long id,
    String descricao,
    Long quantidadeFuncionarios,
    BigDecimal valorTotal
) {} 