package br.com.techne.sistemafolha.workspace.domain;

import java.util.Map;

public record WorkspaceWidgetPayload(
    String instanceId,
    Integer ordem,
    Integer colSpan,
    Integer rowSpan,
    String widgetId,
    Long userWidgetDefinitionId,
    Map<String, Object> config
) {}
