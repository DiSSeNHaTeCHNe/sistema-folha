package br.com.techne.sistemafolha.workspace.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record WorkspaceWidgetDTO(
    @NotBlank String instanceId,
    @NotNull @Min(0) Integer ordem,
    @NotNull @Min(1) @Max(12) Integer colSpan,
    @NotNull @Min(1) @Max(3) Integer rowSpan,
    String widgetId,
    Long userWidgetDefinitionId,
    Map<String, Object> config
) {}
