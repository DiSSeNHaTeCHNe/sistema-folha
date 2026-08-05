package br.com.techne.sistemafolha.workspace.api;

import jakarta.validation.constraints.NotNull;

public record OrcamentoInstallRequest(
    @NotNull Long workspaceId
) {}
