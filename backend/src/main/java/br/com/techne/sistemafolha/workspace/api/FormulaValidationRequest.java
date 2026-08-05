package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Requisição para validar fórmula de widget sem persistir")
public record FormulaValidationRequest(
    @NotBlank @Size(max = 2000) String formula,
    @NotEmpty List<@Valid WidgetSourceRef> fontes
) {}
