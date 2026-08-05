package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Requisição para atualizar esquema do dataset")
public record UpdateDatasetSchemaRequest(
    @NotEmpty List<@Valid DatasetFieldSchemaDTO> campos,
    @NotNull Integer schemaVersion,
    Boolean confirmarRemocao
) {}
