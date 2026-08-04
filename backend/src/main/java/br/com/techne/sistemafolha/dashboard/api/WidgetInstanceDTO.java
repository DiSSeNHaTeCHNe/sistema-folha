package br.com.techne.sistemafolha.dashboard.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record WidgetInstanceDTO(
    @NotBlank String widgetId,
    @NotBlank String instanceId,
    @NotNull @Min(0) Integer ordem,
    @NotNull @Min(1) @Max(12) Integer colSpan,
    @NotNull @Min(1) @Max(3) Integer rowSpan,
    Map<String, Object> config
) {}
