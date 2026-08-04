package br.com.techne.sistemafolha.dashboard.domain;

import java.util.Map;

public record WidgetInstancePayload(
    String widgetId,
    String instanceId,
    Integer ordem,
    Integer colSpan,
    Integer rowSpan,
    Map<String, Object> config
) {}
