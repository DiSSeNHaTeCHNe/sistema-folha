package br.com.techne.sistemafolha.folha.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record FichaLinhaDetalheDTO(
    @Schema(description = "Valor bruto da rubrica/lançamento") BigDecimal valor,
    @Schema(description = "Contribuição para o totalizador (valor × operador)") BigDecimal contribuicao,
    @Schema(description = "Origem: FOLHA_ADP, CUSTO_FIXO, CALCULADO ou BENEFICIO (consulta)") String origemLinha,
    @Schema(description = "Código da rubrica ou tipo de benefício") String rubricaCodigo,
    @Schema(description = "Descrição da rubrica ou tipo de benefício") String rubricaDescricao,
    @Schema(description = "Snapshot ficha_linha.porcentagem; null para BENEFICIO")
    BigDecimal porcentagem
) {}
