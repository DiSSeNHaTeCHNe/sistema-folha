package br.com.techne.sistemafolha.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Metadados de API Key (sem secret)")
public record ApiKeyListDTO(
    @Schema(description = "Identificador da API Key")
    Long id,

    @Schema(description = "Nome descritivo")
    String nome,

    @Schema(description = "Prefixo público para identificação")
    String prefixo,

    @Schema(description = "Data de expiração")
    LocalDateTime dataExpiracao,

    @Schema(description = "Indica se a key foi revogada")
    boolean revogado,

    @Schema(description = "Escopo de acesso", example = "READ")
    String escopo,

    @Schema(description = "Último uso registrado")
    LocalDateTime ultimoUsoEm,

    @Schema(description = "Data de criação")
    LocalDateTime dataCriacao
) {}
