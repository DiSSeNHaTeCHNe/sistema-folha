package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Requisição para criar ou atualizar linha de dataset")
public record DatasetRowRequest(
    @NotNull Map<String, Object> valores
) {}
