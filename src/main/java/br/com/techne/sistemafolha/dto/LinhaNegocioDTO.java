package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para Linha de Negócio")
public record LinhaNegocioDTO(
    @Schema(description = "Identificador único da linha de negócio", example = "1")
    Long id,

    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 3, max = 100, message = "A descrição deve ter entre 3 e 100 caracteres")
    @Schema(description = "Descrição da linha de negócio", example = "Tecnologia da Informação", required = true)
    String descricao,

    @Schema(description = "Indica se a linha de negócio está ativa", example = "true")
    Boolean ativo
) {} 