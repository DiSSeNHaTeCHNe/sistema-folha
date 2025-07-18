package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "DTO para Associação Funcionário-Organograma")
public record FuncionarioOrganogramaDTO(
    @Schema(description = "Identificador único da associação", example = "1")
    Long id,

    @NotNull(message = "O ID do funcionário é obrigatório")
    @Schema(description = "ID do funcionário", example = "1", required = true)
    Long funcionarioId,

    @Schema(description = "Dados do funcionário")
    FuncionarioDTO funcionario,

    @NotNull(message = "O ID do nó do organograma é obrigatório")
    @Schema(description = "ID do nó do organograma", example = "1", required = true)
    Long noOrganogramaId,

    @Schema(description = "Dados do nó do organograma")
    NoOrganogramaDTO noOrganograma,

    @Schema(description = "Data de criação da associação", example = "2024-01-01T10:00:00")
    LocalDateTime dataCriacao,

    @Schema(description = "Usuário que criou a associação", example = "admin")
    String criadoPor
) {} 