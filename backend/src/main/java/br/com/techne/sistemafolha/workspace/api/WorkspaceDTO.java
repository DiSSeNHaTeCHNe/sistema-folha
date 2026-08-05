package br.com.techne.sistemafolha.workspace.api;

import java.util.List;

public record WorkspaceDTO(
    Long id,
    String nome,
    List<WorkspaceWidgetDTO> widgets
) {}
