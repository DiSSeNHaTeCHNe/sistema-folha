package br.com.techne.sistemafolha.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Requisição para criação de API Key")
public record ApiKeyCreateRequest(
    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    @Schema(description = "Nome descritivo da API Key", example = "Integração MCP")
    String nome,

    @Min(value = 1, message = "diasValidade deve estar entre 1 e 365")
    @Max(value = 365, message = "diasValidade deve estar entre 1 e 365")
    @Schema(description = "Validade em dias (1-365); omitido usa 365", example = "365")
    Integer diasValidade
) {}
