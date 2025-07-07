package br.com.techne.sistemafolha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResumoFolhaPagamentoDTO(
    Long id,
    Integer totalEmpregados,
    BigDecimal totalEncargos,
    BigDecimal totalPagamentos,
    BigDecimal totalDescontos,
    BigDecimal totalLiquido,
    LocalDate competenciaInicio,
    LocalDate competenciaFim,
    LocalDateTime dataImportacao,
    Boolean ativo
) {} 