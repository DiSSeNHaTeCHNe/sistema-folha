package br.com.techne.sistemafolha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BeneficioDTO(
    Long id,
    Long funcionarioId,
    String funcionarioNome,
    String descricao,
    BigDecimal valor,
    LocalDate dataInicio,
    LocalDate dataFim,
    String observacao
) {} 