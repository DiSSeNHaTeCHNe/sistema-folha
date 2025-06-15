package br.com.techne.sistemafolha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FolhaPagamentoDTO(
    Long id,
    Long funcionarioId,
    String funcionarioNome,
    Long rubricaId,
    String rubricaCodigo,
    String rubricaDescricao,
    LocalDate dataInicio,
    LocalDate dataFim,
    BigDecimal valor,
    BigDecimal quantidade,
    BigDecimal baseCalculo
) {} 