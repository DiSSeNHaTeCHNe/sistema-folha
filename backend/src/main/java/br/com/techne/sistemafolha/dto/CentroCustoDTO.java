package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para Centro de Custo")
public record CentroCustoDTO(
    @Schema(description = "Identificador único do centro de custo", example = "1")
    Long id,

    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 3, max = 100, message = "A descrição deve ter entre 3 e 100 caracteres")
    @Schema(description = "Descrição do centro de custo", example = "Desenvolvimento", required = true)
    String descricao,

    @Schema(description = "Indica se o centro de custo está ativo", example = "true")
    Boolean ativo,

    @NotNull(message = "A linha de negócio é obrigatória")
    @Schema(description = "ID da linha de negócio associada ao centro de custo", example = "1", required = true)
    Long linhaNegocioId
) {} 