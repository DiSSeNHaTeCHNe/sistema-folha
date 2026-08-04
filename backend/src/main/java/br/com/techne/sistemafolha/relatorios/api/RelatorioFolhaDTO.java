package br.com.techne.sistemafolha.relatorios.api;

import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RelatorioFolhaDTO(
    Long id,
    Integer mes,
    Integer ano,
    Integer totalFuncionarios,
    BigDecimal totalFolha,
    BigDecimal totalBeneficios,
    RelatorioStatus status,
    LocalDateTime dataProcessamento,
    String erro,
    LocalDateTime dataCriacao,
    boolean stale
) {}
