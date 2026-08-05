package br.com.techne.sistemafolha.workspace.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
    @NotBlank @Size(max = 120) String nome
) {}
