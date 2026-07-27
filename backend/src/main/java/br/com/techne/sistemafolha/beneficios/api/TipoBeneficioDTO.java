package br.com.techne.sistemafolha.beneficios.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para Tipo de Benefício")
public record TipoBeneficioDTO(
    @Schema(description = "Identificador único do tipo de benefício", example = "1")
    Long id,

    @NotBlank(message = "O código é obrigatório")
    @Size(max = 50, message = "O código deve ter no máximo 50 caracteres")
    @Schema(description = "Código único do tipo de benefício", example = "VALE_REFEICAO", required = true)
    String codigo,

    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres")
    @Schema(description = "Descrição do tipo de benefício", example = "Vale Refeição - Custo Empresa", required = true)
    String descricao,

    @Schema(description = "Indica se o tipo está ativo", example = "true")
    Boolean ativo
) {}
