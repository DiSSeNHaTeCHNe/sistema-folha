package br.com.techne.sistemafolha.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resposta de API Key recém-criada (secret one-shot)")
public record ApiKeyCreatedDTO(
    @Schema(description = "Identificador da API Key")
    Long id,

    @Schema(description = "Nome descritivo")
    String nome,

    @Schema(description = "Prefixo público para identificação")
    String prefixo,

    @Schema(description = "Secret completo — exibido apenas na criação")
    String chave,

    @Schema(description = "Data de expiração")
    LocalDateTime dataExpiracao,

    @Schema(description = "Escopo de acesso", example = "READ")
    String escopo,

    @Schema(description = "Data de criação")
    LocalDateTime dataCriacao
) {}
