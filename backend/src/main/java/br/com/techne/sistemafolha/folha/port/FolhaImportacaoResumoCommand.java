package br.com.techne.sistemafolha.folha.port;

import java.math.BigDecimal;

public record FolhaImportacaoResumoCommand(
    Integer totalEmpregados,
    BigDecimal totalEncargos,
    BigDecimal totalPagamentos,
    BigDecimal totalDescontos,
    BigDecimal totalLiquido
) {}
