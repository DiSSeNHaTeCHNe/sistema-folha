package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Requisição para criar dataset")
public record CreateDatasetRequest(
    @NotBlank @Size(max = 120) String nome,
    @NotEmpty List<@Valid DatasetFieldSchemaDTO> campos
) {}
