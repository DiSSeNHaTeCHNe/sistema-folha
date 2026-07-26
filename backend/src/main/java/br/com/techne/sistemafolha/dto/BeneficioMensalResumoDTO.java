package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resumo de benefícios mensais agrupado por tipo")
public record BeneficioMensalResumoDTO(
    @Schema(description = "Código do tipo de benefício", example = "VALE_REFEICAO")
    String codigo,

    @Schema(description = "Descrição do tipo de benefício", example = "Vale Refeição - Custo Empresa")
    String descricao,

    @Schema(description = "Soma dos valores do tipo na competência", example = "12500.00")
    BigDecimal total,

    @Schema(description = "Quantidade de lançamentos do tipo na competência", example = "42")
    Long qtdLancamentos
) {}
