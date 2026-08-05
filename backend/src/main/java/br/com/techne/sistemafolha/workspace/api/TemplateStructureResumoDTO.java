package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resumo textual da estrutura de uma versão de template")
public record TemplateStructureResumoDTO(
    List<String> campos,
    List<String> widgets,
    List<String> formulas
) {}
