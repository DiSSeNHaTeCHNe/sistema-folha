package br.com.techne.sistemafolha.relatorios.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GerarRelatorioRequest(
    @NotNull @Min(1) @Max(12)
    @Schema(description = "Mês da competência (1–12)", example = "6")
    Integer mes,

    @NotNull @Min(2000) @Max(2100)
    @Schema(description = "Ano da competência (2000–2100)", example = "2024")
    Integer ano
) {}
