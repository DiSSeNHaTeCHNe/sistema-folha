package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

@Schema(description = "Requisição para atualizar definição de widget")
public record UpdateWidgetDefinitionRequest(
    @NotBlank @Size(max = 120) String nome,
    @NotBlank String tipo,
    @NotEmpty List<@Valid WidgetSourceRef> fontes,
    @Size(max = 2000) String formula,
    Map<String, Object> config
) {}
