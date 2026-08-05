package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Linha de dataset")
public record DatasetRowDTO(
    Long id,
    Long datasetId,
    Map<String, Object> valores,
    Integer ordem
) {}
