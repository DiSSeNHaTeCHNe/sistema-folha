package br.com.techne.sistemafolha.relatorios.api;

import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RelatorioBeneficioDTO(
    Long id,
    Integer mes,
    Integer ano,
    BigDecimal totalBeneficios,
    BigDecimal totalValor,
    RelatorioStatus status,
    LocalDateTime dataProcessamento,
    String erro
) {}
