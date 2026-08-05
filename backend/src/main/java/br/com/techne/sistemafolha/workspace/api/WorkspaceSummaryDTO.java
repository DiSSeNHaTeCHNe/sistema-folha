package br.com.techne.sistemafolha.workspace.api;

import java.time.LocalDateTime;

public record WorkspaceSummaryDTO(
    Long id,
    String nome,
    int totalWidgets,
    LocalDateTime dataAtualizacao
) {}
