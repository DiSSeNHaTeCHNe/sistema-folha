package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "DTO para Benefício Mensal")
public record BeneficioMensalDTO(
    @Schema(description = "Identificador único do lançamento", example = "1")
    Long id,

    @NotNull(message = "O funcionário é obrigatório")
    @Schema(description = "ID do funcionário", example = "10", required = true)
    Long funcionarioId,

    @Schema(description = "Nome do funcionário", example = "João Silva")
    String funcionarioNome,

    @NotNull(message = "O tipo de benefício é obrigatório")
    @Schema(description = "ID do tipo de benefício", example = "2", required = true)
    Long tipoBeneficioId,

    @Schema(description = "Código do tipo de benefício", example = "VALE_REFEICAO")
    String tipoBeneficioCodigo,

    @Schema(description = "Descrição do tipo de benefício", example = "Vale Refeição - Custo Empresa")
    String tipoBeneficioDescricao,

    @Schema(description = "ID do centro de custo do funcionário", example = "3")
    Long centroCustoId,

    @Schema(description = "Descrição do centro de custo do funcionário", example = "TI")
    String centroCustoDescricao,

    @NotNull(message = "O valor é obrigatório")
    @Schema(description = "Valor do benefício", example = "450.00", required = true)
    BigDecimal valor,

    @NotNull(message = "A competência de início é obrigatória")
    @Schema(description = "Primeiro dia da competência", example = "2024-10-01", required = true)
    LocalDate competenciaInicio,

    @NotNull(message = "A competência de fim é obrigatória")
    @Schema(description = "Último dia da competência", example = "2024-10-31", required = true)
    LocalDate competenciaFim,

    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
    @Schema(description = "Observação opcional")
    String observacao
) {}
