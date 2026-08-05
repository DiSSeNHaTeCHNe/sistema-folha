package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resumo de versão de template para diff no frontend")
public record TemplateVersionSummaryDTO(
    Integer versao,
    LocalDateTime dataPublicacao,
    TemplateStructureResumoDTO estruturaResumo
) {}
