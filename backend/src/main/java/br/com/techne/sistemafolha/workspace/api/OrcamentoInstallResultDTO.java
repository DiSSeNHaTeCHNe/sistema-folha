package br.com.techne.sistemafolha.workspace.api;

import java.util.List;

public record OrcamentoInstallResultDTO(
    Long workspaceId,
    Long datasetId,
    List<Long> widgetDefinitionIds
) {}
