package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Dataset completo")
public record DatasetDTO(
    Long id,
    String nome,
    List<DatasetFieldSchemaDTO> campos,
    int schemaVersion,
    long totalLinhas
) {}
