package br.com.techne.sistemafolha.workspace.api;

public record WorkspaceSummaryDTO(
    Long id,
    String nome,
    int totalWidgets
) {}
