package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Instalar template publicado no workspace do usuário")
public record InstallTemplateRequest(
    @NotNull(message = "workspaceId é obrigatório")
    Long workspaceId
) {}
