package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Resultado de instalação ou upgrade de template")
public record TemplateInstallResultDTO(
    Long installationId,
    Long templateId,
    Integer versaoInstalada,
    Long workspaceId,
    Long datasetId,
    List<Long> widgetDefinitionIds,
    Map<String, Long> entityMap
) {}
