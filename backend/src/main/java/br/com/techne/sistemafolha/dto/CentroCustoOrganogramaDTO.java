package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "DTO para Associação Centro de Custo-Organograma")
public record CentroCustoOrganogramaDTO(
    @Schema(description = "Identificador único da associação", example = "1")
    Long id,

    @NotNull(message = "O ID do centro de custo é obrigatório")
    @Schema(description = "ID do centro de custo", example = "1", required = true)
    Long centroCustoId,

    @Schema(description = "Dados do centro de custo")
    CentroCustoDTO centroCusto,

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