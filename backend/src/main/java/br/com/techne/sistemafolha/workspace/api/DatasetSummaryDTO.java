package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo do dataset")
public record DatasetSummaryDTO(
    Long id,
    String nome,
    int schemaVersion,
    long totalLinhas,
    int totalCampos
) {}
