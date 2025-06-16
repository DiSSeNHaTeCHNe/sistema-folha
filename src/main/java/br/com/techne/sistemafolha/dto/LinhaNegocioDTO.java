package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para Linha de Negócio")
public record LinhaNegocioDTO(
    @Schema(description = "Identificador único da linha de negócio", example = "1")
    Long id,

    @NotBlank(message = "O código é obrigatório")
    @Size(min = 3, max = 10, message = "O código deve ter entre 3 e 10 caracteres")
    @Schema(description = "Código único da linha de negócio", example = "TEC", required = true)
    String codigo,

    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 3, max = 100, message = "A descrição deve ter entre 3 e 100 caracteres")
    @Schema(description = "Descrição da linha de negócio", example = "Tecnologia", required = true)
    String descricao,

    @Schema(description = "Indica se a linha de negócio está ativa", example = "true")
    Boolean ativo
) {} 