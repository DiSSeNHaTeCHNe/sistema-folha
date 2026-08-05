package br.com.techne.sistemafolha.workspace.domain;

public record WidgetSourceRef(
    WidgetSourceKind kind,
    String ref
) {}
