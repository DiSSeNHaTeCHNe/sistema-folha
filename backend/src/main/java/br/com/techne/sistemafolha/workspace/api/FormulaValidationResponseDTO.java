package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado da validação de fórmula")
public record FormulaValidationResponseDTO(
    boolean valid,
    List<String> errors
) {}
