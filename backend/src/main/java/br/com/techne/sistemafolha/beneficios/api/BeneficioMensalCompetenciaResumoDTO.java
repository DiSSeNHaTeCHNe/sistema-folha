package br.com.techne.sistemafolha.beneficios.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Resumo agregado de benefícios mensais por competência")
public record BeneficioMensalCompetenciaResumoDTO(
    @Schema(description = "Primeiro dia da competência", example = "2024-10-01")
    LocalDate competenciaInicio,

    @Schema(description = "Último dia da competência", example = "2024-10-31")
    LocalDate competenciaFim,

    @Schema(description = "Quantidade de funcionários distintos com lançamentos", example = "42")
    long totalFuncionarios,

    @Schema(description = "Soma dos valores de benefícios", example = "15000.00")
    BigDecimal totalBeneficios,

    @Schema(description = "Quantidade de lançamentos", example = "120")
    long qtdLancamentos
) {}
