package br.com.techne.sistemafolha.workspace.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveWorkspaceLayoutRequest(
    @NotNull @Size(max = 30) List<@Valid WorkspaceWidgetDTO> widgets
) {}
