package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "DTO para Funcionário")
public record FuncionarioDTO(
    @Schema(description = "Identificador único do funcionário", example = "1")
    Long id,

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome completo do funcionário", example = "João da Silva", required = true)
    String nome,

    @NotBlank(message = "O CPF é obrigatório")
    @Size(min = 11, max = 11, message = "O CPF deve ter 11 dígitos")
    @Schema(description = "CPF do funcionário (apenas números)", example = "12345678900", required = true)
    String cpf,

    @NotNull(message = "A data de admissão é obrigatória")
    @Past(message = "A data de admissão deve ser uma data passada")
    @Schema(description = "Data de admissão do funcionário", example = "2024-01-01", required = true)
    LocalDate dataAdmissao,

    @NotNull(message = "O cargo é obrigatório")
    @Schema(description = "ID do cargo do funcionário", example = "1", required = true)
    Long cargoId,

    @NotNull(message = "O centro de custo é obrigatório")
    @Schema(description = "ID do centro de custo do funcionário", example = "1", required = true)
    Long centroCustoId,

    @Schema(description = "ID externo do funcionário", example = "12345")
    String idExterno,

    @Schema(description = "Indica se o funcionário está ativo", example = "true")
    Boolean ativo
) {} 