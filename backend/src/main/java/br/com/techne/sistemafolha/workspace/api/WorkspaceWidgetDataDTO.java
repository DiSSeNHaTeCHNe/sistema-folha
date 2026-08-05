package br.com.techne.sistemafolha.workspace.api;

import java.util.List;
import java.util.Map;

public record WorkspaceWidgetDataDTO(
    String instanceId,
    Long userWidgetDefinitionId,
    String widgetId,
    String tipo,
    boolean semDados,
    boolean invalido,
    String competencia,
    Map<String, String> valores,
    List<Map<String, String>> linhas
) {

    public static WorkspaceWidgetDataDTO semDados(
            String instanceId, Long userWidgetDefinitionId, String widgetId, String tipo, String competencia) {
        return new WorkspaceWidgetDataDTO(
            instanceId, userWidgetDefinitionId, widgetId, tipo, true, false, competencia, Map.of(), List.of());
    }
}
