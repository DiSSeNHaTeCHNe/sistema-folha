package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para Cargo")
public record CargoDTO(
    @Schema(description = "Identificador único do cargo", example = "1")
    Long id,

    @NotBlank(message = "O código é obrigatório")
    @Size(min = 3, max = 10, message = "O código deve ter entre 3 e 10 caracteres")
    @Schema(description = "Código único do cargo", example = "DEV", required = true)
    String codigo,

    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 3, max = 100, message = "A descrição deve ter entre 3 e 100 caracteres")
    @Schema(description = "Descrição do cargo", example = "Desenvolvedor", required = true)
    String descricao,

    @Schema(description = "Indica se o cargo está ativo", example = "true")
    Boolean ativo,

    @NotNull(message = "A linha de negócio é obrigatória")
    @Schema(description = "ID da linha de negócio associada ao cargo", example = "1", required = true)
    Long linhaNegocioId
) {} 